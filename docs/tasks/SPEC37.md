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
- #38 tiene matriz canónica de 422 criterios en GitHub. Revisión: Standards sin hallazgos; dos enlaces de evidencia por corregir en Spec. El mismo implementador corrige antes de devolverla al mismo revisor.
- #36 revisada (Standards 0 / Spec 0), integrada en `8ef5fe3ce71ffe4433342e56a4c452a7b84e2054` y cerrada en GitHub. Evidencia: 115 pruebas shared JVM + 5 host y 13 recorridos de navegador aprobados; sin cambios de producción. Informe publicado en #36#issuecomment-6301672098. Rama de integración publicada; todavía no integrada en main.
- Base: `:shared:jvmTest` 115 y `:composeApp:testDebugUnitTest` 8 pruebas pasaron, sin fallos. Build Wasm de producción pasó. Estas comprobaciones describen la base publicada, no acreditan las entregas nuevas.
- #32 encontró incompatibilidad de metadata Kotlin: runtime oficial requiere 2.4, base tiene2.2. Preparación parcial conservada en `06189148ece02951d43408b14ef56aab8b1245f9`; no acredita runtime real. #34 implementa el prerrequisito AGP9/Kotlin2.4.21 manteniendo Compose1.9.3 si resulta compatible. Una sola compilación pesada a la vez por memoria disponible.
- Siguiente paso: terminar/revisar la matriz #38, integrar; continuar la frontera de dependencias. Las corridas de teléfono siguen separadas de la posibilidad de progresar/revisar código.
- Referencia privada: ambos PDFs inspeccionados; trece pares del TXT contrastados con el PDF hipotecario y saldos del resumen de cuenta comprobados independientemente. Inferencia real no ejecutada.
