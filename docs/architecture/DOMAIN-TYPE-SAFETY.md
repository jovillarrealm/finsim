# Arquitectura de Dominio: Seguridad Tipada en Value Objects y Modelos Sellados

## 1. Diagnóstico: Fricción por Obsesión Primitiva en el Motor

En la implementación inicial del simulador (`Scenario.kt`, `Loan.kt`, `Rates.kt`), el intercambio de datos entre la persistencia, la interfaz de usuario y el motor de amortización depende casi exclusivamente de tipos primitivos no restringidos (`String`, `Int`):

1. **Importes monetarios como cadenas desnudas (`String`)**:
   - Campos como `principal = "20000000.00"`, `payment = "1500000.00"` o `event.amount = "500000.00"` carecen de etiqueta de divisa y de garantía de precisión a nivel de compilación.
   - En el bucle mensual de `Loan.kt` (hasta 1.200 iteraciones), el motor ejecuta conversiones defensivas repetitivas (`Decimal.parse(...)` y `money(...)`) para cada evento, cuota, abono y saldo pendiente.
   - Se corre el riesgo inadvertido de procesar importes con más de dos decimales (violando la regla estricta de [financial-conventions.md](file:///C:/Users/Lenovo/Documents/finsim/docs/financial-conventions.md)) o mezclar divisas incompatibles sin validación estática.

2. **Índices mensuales como enteros desprotegidos (`Int`)**:
   - Meses, plazos y frecuencias se representan como enteros arbitrarios (`month: Int`, `termMonths: Int`, `every: Int`).
   - El motor debe verificar repetidamente rangos (`month in 1..MAX_MONTHS`, `endMonth >= month`, `every in 1..MAX_MONTHS`). Un error de permutación posicional en una llamada de función (p. ej. invertir `month` y `every`) pasa inadvertido para el compilador.

3. **Tasas como cadenas no calificadas (`Rate.value: String`)**:
   - Una cadena `"0.18"` no distingue intrínsecamente si representa una tasa efectiva anual (E.A.), efectiva mensual o nominal anual sin inspeccionar estructuras secundarias.
   - En conversiones de tasa (`convertRate`), se recurre a cadenas mágicas (`target = "monthly"`, `"annual"`, `"nominal"`), delegando fallos a excepciones en tiempo de ejecución.

4. **Identificadores como cadenas vaciables (`id: String`)**:
   - `Insurance.id` y `LoanEvent.id` son cadenas genéricas susceptibles de estar en blanco o colisionar con nombres o importes.

5. **Sobrecarga semántica en seguros (`Insurance.kind: String`, `value: String`)**:
   - Si `kind == "fixed"`, `value` representa un importe monetario absoluto en la divisa del préstamo.
   - Si `kind == "percentage"`, `value` representa una fracción de tasa mensual aplicada sobre el saldo de apertura.
   - La falta de polimorfismo sellado obliga a bifurcaciones por comparación de cadenas en cada periodo.

---

## 2. El Módulo `DomainTypeSafety`

El módulo `DomainTypeSafety` profundiza el diseño del núcleo financiero: concentra las reglas de redondeo, restricciones de escala, garantías de divisa y proyecciones temporales detrás de un conjunto de value objects inmutables.

- **Categoría de dependencia**: En proceso (*in-process*). Cálculo puro y estado en memoria sin operaciones de entrada/salida.
- **Costura (*seam*)**:
  - **Costura externa de ingestión**: Punto de entrada donde los datos crudos (JSON, almacenamiento local o formularios de interfaz) se validan y se adaptan a los tipos del dominio mediante un adaptador dedicado (`ScenarioAdapter`).
  - **Costura interna del motor**: La función del motor `simulateTypedLoan` expone una interfaz estrictamente tipada sobre `TypedScenario`. Dentro de la costura interna, no existe ninguna verificación defensiva de parsing ni redondeo ad-hoc.
- **Superficie de prueba (*test surface*)**: La interfaz es la superficie de prueba. Las pruebas unitarias ejercitan los invariantes directamente en los constructores y operadores de los value objects, liberando al motor de pruebas redundantes de cadenas malformadas.

---

## 3. Especificación de la Interfaz

### 3.1. `Money` (Valor Monetario Tipado)

```kotlin
data class Money(val cents: Long, val currency: Currency = Currency.COP) : Comparable<Money>
```

- **Parámetros**:
  - `cents`: Entero de 64 bits que almacena la cantidad exacta de centavos.
  - `currency`: Etiqueta explícita de divisa (`COP`, `ARS`, `USD`, `EUR`).
- **Invariantes**:
  - `cents >= 0`: Cumple la regla de [financial-conventions.md](file:///C:/Users/Lenovo/Documents/finsim/docs/financial-conventions.md): *"Nunca perdonar capital ni producir saldos negativos"*.
  - `cents <= MAX_CENTS` ($10^{17}$ centavos, equivalente a $1.000.000.000.000.000,00$ en la divisa seleccionada).
  - Escala fija de 2 decimales: El constructor de ingestión `Money.parse(str, currency)` rechaza cadenas con más de 2 decimales (*"Los importes ingresados o importados deben tener como máximo dos decimales; rechazar mayor precisión con una explicación"*).
  - Coherencia de divisa: Toda operación binaria (`+`, `-`, `compareTo`) exige `this.currency == other.currency`.
- **Operaciones**:
  - `operator fun plus(other: Money): Money`
  - `operator fun minus(other: Money): Money`: Lanza excepción si `other > this`, imposibilitando la generación de saldos de deuda negativos.
  - `fun minusOrZero(other: Money): Money`: Sustracción segura con piso en cero para amortizaciones parciales.
  - `fun timesFraction(fraction: Decimal): Money`: Multiplicación por fracción de 40 dígitos de precisión y redondeo de mitad hacia arriba (*half-up*) al centavo más próximo.
  - `fun toFormattedString(): String`: Representación estándar con dos decimales (`"100.00"`).

### 3.2. `MonthIndex` (Índice de Periodo)

```kotlin
@JvmInline
value class MonthIndex(val value: Int) : Comparable<MonthIndex>
```

- **Parámetros**:
  - `value`: Entero positivo que representa el periodo mensual relativo al inicio del préstamo.
- **Invariantes**:
  - `value in 1..MAX_MONTHS` ($1 \le value \le 1.200$).
- **Operaciones**:
  - `fun next(): MonthIndex?`: Retorna el periodo siguiente o `null` si se alcanza el horizonte máximo.
  - `operator fun plus(months: Int): MonthIndex`
  - `operator fun minus(other: MonthIndex): Int`
  - `fun toDate(originationYear: Int, originationMonth: Int): String`: Oculta la proyección calendárica YYYY-MM detrás de la interfaz.

### 3.3. `RateValue` (Jerarquía Sellada de Tasas)

```kotlin
sealed class RateValue {
    abstract val fractionDecimal: Decimal
    abstract val effectiveMonthlyDecimal: Decimal

    data class EffectiveMonthly(override val fractionDecimal: Decimal) : RateValue()
    data class EffectiveAnnual(override val fractionDecimal: Decimal) : RateValue()
    data class NominalAnnual(override val fractionDecimal: Decimal, val frequency: Int) : RateValue()

    fun toEffectiveMonthly(): EffectiveMonthly
    fun toEffectiveAnnual(): EffectiveAnnual
    fun toNominalAnnual(frequency: Int): NominalAnnual
}
```

- **Invariantes**:
  - `fractionDecimal >= 0`: Tasas no negativas en el modelo.
  - `frequency >= 1` en tasas nominales.
- **Comportamiento Oculto**:
  - Aritmética decimal de 40 dígitos mediante exponenciación `(1 + i)^{1/12} - 1` precalculada y encapsulada.
  - La propiedad `effectiveMonthlyDecimal` entrega la tasa mensual lista para el bucle de simulación sin coste de recálculo repetitivo.

### 3.4. `EventId` (Identificador de Dominio)

```kotlin
@JvmInline
value class EventId(val value: String)
```

- **Invariantes**:
  - `value.isNotBlank()`: Rechaza cadenas vacías o compuestas exclusivamente de espacios.

### 3.5. Modelo Sellado de Seguros y Eventos

```kotlin
sealed class InsuranceCharge {
    data class Fixed(val amount: Money) : InsuranceCharge()
    data class Percentage(val monthlyRateFraction: Decimal) : InsuranceCharge()
}

data class TypedInsurance(
    val id: EventId,
    val name: String,
    val charge: InsuranceCharge,
    val endsAtPayoff: Boolean = true,
    val endMonth: MonthIndex? = null
)

sealed class TypedLoanEvent {
    abstract val id: EventId
    abstract val month: MonthIndex

    data class Extra(override val id: EventId, override val month: MonthIndex, val amount: Money) : TypedLoanEvent()
    data class Recurring(override val id: EventId, override val month: MonthIndex, val endMonth: MonthIndex, val everyMonths: Int, val amount: Money) : TypedLoanEvent()
    data class Missed(override val id: EventId, override val month: MonthIndex) : TypedLoanEvent()
    data class Catchup(override val id: EventId, override val month: MonthIndex, val amount: Money) : TypedLoanEvent()
}
```

---

## 4. Ejemplo de Uso: Dominio y Motor Simplificado

### 4.1. Construcción del Escenario en la Costura de Ingestión

```kotlin
val scenario = TypedScenario(
    currency = Currency.COP,
    name = "Crédito Libre Inversión",
    principal = Money.parse("20000000.00", Currency.COP),
    startDate = "2026-10",
    termMonths = 36,
    rate = RateValue.EffectiveAnnual.of("0.18"),
    insurance = listOf(
        TypedInsurance(
            id = EventId("life"),
            name = "Seguro de vida",
            charge = InsuranceCharge.Percentage(Decimal.parse("0.0005")),
            endsAtPayoff = true
        )
    ),
    events = listOf(
        TypedLoanEvent.Extra(
            id = EventId("abono-1"),
            month = MonthIndex(6),
            amount = Money.parse("2000000.00", Currency.COP)
        )
    )
)
```

### 4.2. Internos del Motor de Simulación (`simulateTypedLoan`)

Observe cómo la presencia de tipos invariantes en la interfaz elimina completamente las comprobaciones y conversiones defensivas:

```kotlin
fun simulateTypedLoan(scenario: TypedScenario): TypedLoanResult {
    val rate = scenario.rate.effectiveMonthlyDecimal
    var balance = scenario.principal
    val curr = scenario.currency

    // Cálculo de cuota base sin verificaciones de tipo o parseos de cadena
    val base: Money = calculateBasePayment(scenario, rate)

    var interest = Money.zero(curr)
    var insurance = Money.zero(curr)
    var overdue = Money.zero(curr)

    for (m in 1..MAX_MONTHS) {
        val month = MonthIndex(m)

        // Generación de intereses con redondeo half-up integrado en Money
        val opening = balance
        val currentInterest = opening.timesFraction(rate)

        // Seguros polimórficos sin comprobaciones "fixed" vs "percentage"
        var currentInsurance = Money.zero(curr)
        for (item in activeInsurance) {
            val charge = when (val c = item.charge) {
                is InsuranceCharge.Fixed -> c.amount
                is InsuranceCharge.Percentage -> opening.timesFraction(c.monthlyRateFraction)
            }
            currentInsurance = currentInsurance + charge
        }

        // Operaciones aritméticas directas entre Money
        interest = interest + currentInterest
        insurance = insurance + currentInsurance

        // Asignación de pagos: tipos garantizan no-negatividad
        val scheduled = allocate(scheduledOffer)
        val extra = allocate(extraOffer)

        // Totales acumulados directamente sin desarmar a cadenas
        totals.principal = totals.principal + scheduled.principal + extra.principal
        totals.interest = totals.interest + scheduled.interest + extra.interest
    }

    return TypedLoanResult(...)
}
```

---

## 5. Lo que la Implementación Oculta Detrás de la Costura

1. **Aritmética de punto fijo en centavos (`Long`)**: La representación de `Money` en centavos enteros permite sumas, restas y comparaciones en $O(1)$ sin asignación de objetos `BigInt` o cadenas intermedias en el bucle principal.
2. **Algoritmo de redondeo mitad hacia arriba (*Half-Up*)**: La multiplicación `timesFraction` coordina internamente los 40 dígitos de `Decimal` con el redondeo exacto al segundo decimal, aislando a los consumidores de las convenciones de truncamiento.
3. **Fórmulas de equivalencia de tasas de interés**: El cálculo exponencial de tasas efectivas y nominales queda encapsulado dentro de `RateValue`; el motor solo consume la propiedad `effectiveMonthlyDecimal`.
4. **Cálculo de proyección temporal calendárica**: La traducción de `MonthIndex` a formato `"YYYY-MM"` se resuelve en `month.toDate(year, month)` sin manipular subcadenas en cada fila de amortización.

---

## 6. Estrategia de Dependencias y Adaptadores

### 6.1. Clasificación de Dependencias
- **Categoría**: *In-process*.
- El módulo no realiza llamadas a red, disco ni interactúa con la interfaz gráfica. Se prueba directamente cruzando su interfaz sin necesidad de fakes ni mocks.

### 6.2. Adaptadores en la Costura
Para preservar la compatibilidad binaria y de persistencia JSON con el resto del proyecto sin romper interfaces existentes:
- **`ScenarioAdapter.toTyped(scenario: Scenario): TypedScenario`**:
  - Actúa en la costura de ingestión deserializada.
  - Valida el esquema sin tipar y construye los value objects garantizados.
- **`ScenarioAdapter.toUntyped(typed: TypedLoanResult): LoanResult`**:
  - Actúa en la costura de salida hacia serializadores JSON y la interfaz de usuario web/Compose.
  - Proyecta los value objects a cadenas formateadas esperadas por los contratos visuales.

---

## 7. Compensaciones (*Trade-offs*)

### 7.1. Dónde el Apalancamiento (*Leverage*) es Alto
- **Bucle de simulación del motor**:
  - Desaparecen más de 40 llamadas a `Decimal.parse` y `money(...)` por mes en 1.200 periodos.
  - El motor queda expresado en términos puros del negocio financiero: cuota, intereses, capital amortizado, atrasos y liquidación.
- **Localidad de los invariantes de redondeo**:
  - Un cambio en la política de redondeo o formato monetario se modifica únicamente en el método `timesFraction` o `toFormattedString` de `Money`, propagándose inmediatamente a todas las cuotas, seguros y tablas.
- **Superficie de pruebas**:
  - Las pruebas de valor extremo (cadenas con 3 decimales, importes negativos, meses mayores a 1200, tasas negativas) se prueban una sola vez en `DomainTypesTest`. El motor queda exento de probar combinaciones sintácticas inválidas.

### 7.2. Dónde el Apalancamiento es Delgado
- **Costura de persistencia / Serialización**:
  - Al cruzar con `kotlinx.serialization` para almacenamiento en disco o JSON, los value objects deben mapearse hacia y desde tipos primitivos (o requerir serializadores personalizados).
  - El costo de adaptación en la costura externa es el precio necesario para aislar la pureza y velocidad del motor en el interior.
- **Diferencias firmadas en conciliación bancaria**:
  - En el módulo de reconciliación (`Reconciliation.kt`), las diferencias entre el extracto bancario y el simulador pueden ser negativas (p. ej. `"-5.20"`). El invariante `cents >= 0` de `Money` impide usar `Money` para representar variaciones relativas firmadas sin definir un tipo complementario (`SignedMoney` o `DecimalDiff`).

---

## 8. Verificación y Resultados de Prueba

El módulo fue implementado y verificado en Kotlin Multiplatform bajo Gradle:
- **Archivos creados**:
  - `kmp/shared/src/commonMain/kotlin/com/finsim/domain/types/DomainTypes.kt`
  - `kmp/shared/src/commonMain/kotlin/com/finsim/domain/types/TypedEngine.kt`
  - `kmp/shared/src/commonTest/kotlin/com/finsim/domain/types/DomainTypesTest.kt`
- **Comando de verificación ejecutado**:
  ```bash
  gradle :shared:jvmTest
  ```
- **Resultado**:
  - 100% de pruebas pasadas satisfactoriamente (`BUILD SUCCESSFUL in 10s`).
  - Paridad estricta bit a bit comprobada entre `simulateLoan` y `simulateTypedLoan` a lo largo de todo el corpus de prueba de préstamos (escenario ejemplo, multidivisa COP/USD/EUR/ARS, perfiles de comisión prepago de Argentina, omisiones y recuperaciones de cuota).
