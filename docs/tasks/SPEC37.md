# Spec #37 — coordinación de implementación y verificación

- Responsable: /root, sesión coordinadora.
- Fuente de verdad: https://github.com/jovillarrealm/finsim/issues/37 y sus comentarios/sub-issues.
- Rama de integración: `codex/spec37-verification`.
- Worktree de integración: `C:/Users/Lenovo/.codex/worktrees/spec37-verification/finsim`.
- Base publicada: `839c69c6e8f7c114424acb6f3fa9b7aae91063a2`.
- Estado: en curso, publicación de tickets autorizada por el propietario el 2026-10-20.
- Método autorizado: dos subagentes secuenciales por ticket, implementador y revisor de Standards/Spec; integración a cargo de /root.

## Acuerdos operativos

- Los tickets y sus dependencias se consultan y registran en GitHub; este archivo es continuidad auxiliar, no una spec paralela.
- El corpus privado contiene dos PDFs y un TXT ampliado. Los documentos y la referencia permanecen fuera de Git.
- La falta de teléfono permite omitir corridas móviles con motivo, criterios y procedimiento de reanudación. No convierte esas capacidades en aprobadas ni bloquea desarrollo o revisión.
- Cada implementador parte de la punta integrada. Cada revisión usa como punto fijo el commit de integración al asignar el ticket. Los dos ejes de revisión se mantienen separados, aunque el revisor los ejecute secuencialmente por instrucción del propietario.
- No incorporar los cambios ajenos sin commit de otros checkouts. No cerrar antecedentes por existencia de avances locales.

## Traspaso actual

- Once tickets #38–#48 publicados con etiqueta `ready-for-agent`, sub-issues y dependencias nativas verificadas. Autorización/corpus actualizados en el comentario de #37; columna19 moneda confirmada por el propietario en #37#issuecomment-6294154389.
- #38 integrada y publicada en `ea7e1fe62dc3e5f30510d1cabde0f92c047e3fbf`, cerrada tras Standards0/Spec0. Matriz canónica422criterios y34referencias; ambos hallazgos iniciales corregidos y verificados por el mismo revisor. Informe #38#issuecomment-6301803848.
- #36 revisada (Standards 0 / Spec 0), integrada en `8ef5fe3ce71ffe4433342e56a4c452a7b84e2054` y cerrada en GitHub. Evidencia: 115 pruebas shared JVM + 5 host y 13 recorridos de navegador aprobados; sin cambios de producción. Informe publicado en #36#issuecomment-6301672098. Rama de integración publicada; todavía no integrada en main.
- Base: `:shared:jvmTest` 115 y `:composeApp:testDebugUnitTest` 8 pruebas pasaron, sin fallos. Build Wasm de producción pasó. Estas comprobaciones describen la base publicada, no acreditan las entregas nuevas.
- #34 integrada y publicada en `804997d635645fdb1cebac651518ef677819673e`: AGP9.1.1/Gradle9.3.1/Kotlin2.4.21/compileSdk37, Compose1.9.3 conservado. Standards0; Spec conserva dos criterios parciales (launch bloqueado por AVD/ADB y actualización firmada no ejecutada). #34 permanece abierta, con reanudación en #43/#44; no son fallos observados del APK. Pruebas115shared+5host, releasefirmado, Wasm y13browserPASS; artefactos/logs/XML preservados externamente.
- El prerrequisito de compilación compatible de #32 ya está integrado, por eso se levanta su arista de bloqueo por #34 sin cerrar ésta ni certificar ejecución móvil. /root/issue32_implement retoma su misma rama/worktree desde preparación parcial `06189148ece02951d43408b14ef56aab8b1245f9`, incorporando la integración. #45 posee ahora el slot Gradle y los contratos de revisión/mapeo; #32 prepara runtime sin invadirlos.
- Siguiente paso: terminar/revisar la matriz #38, integrar; continuar la frontera de dependencias. Las corridas de teléfono siguen separadas de la posibilidad de progresar/revisar código.
- Referencia privada: ambos PDFs inspeccionados; trece pares del TXT contrastados con el PDF hipotecario y saldos del resumen de cuenta comprobados independientemente. Inferencia real no ejecutada.

## Reservas de la frontera tras #38

- #39: /root/issue39_implement y después revisor independiente por asignar, Luna/max. Rama `codex/spec37-issue39`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue39/finsim`. Perfil financiero y evidencia; comparte sólo adición de scripts npm con #34, resolver preservando ambas entregas.
- #45: /root/issue45_implement y después revisor independiente por asignar, Luna/max. Rama `codex/spec37-issue45`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue45/finsim`. Referencia privada y revisión/mapeo de importación; no implementar runtime ni tocar la referencia financiera del motor.
- Ambas reservas parten de este registro después de integrar #38. #34 conserva el único slot de compilación pesada hasta finalizar; las siguientes tareas coordinan sus corridas con /root.
- Slot actual: #34 terminó y AVD se detuvo; #39 confirmó17casosJVM y pasó a revisión; #45 ejecuta red/green KMP secuencial. #32 espera para compilar hasta liberación explícita.
