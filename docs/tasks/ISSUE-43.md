# Issue #43 — verificación Android de recorridos financieros

- Responsable: /root/issue43_implement; coordinadora /root.
- Estado: reservada; implementación y después revisión independiente pendientes, ambos Luna/max.
- Rama: codex/spec37-issue43.
- Worktree previsto: C:/Users/Lenovo/.codex/worktrees/spec37-issue43/finsim.
- Base: registro de reserva posterior a a289b1b en codex/spec37-verification.
- Fuente canónica: https://github.com/jovillarrealm/finsim/issues/43 y #37 con comentarios; #39 cerrada.
- Alcance: perfil Android invocable de préstamo, condiciones/seguros, eventos, conversor, conciliación y ofertas; teclado/foco/scroll/objetivos según densidad, identidad de APK/dispositivo y evidencia por criterio.
- Límites: no intervenir el teléfono del usuario con estado sin guardar, instalar, reiniciar ni resetear ADB por defecto. Corridas móviles pueden quedar documentadas sin bloquear código/revisión. Reutilizar herramientas existentes, sin asumir serial ni píxeles de un teléfono. No atribuir al emulador inferencia Gemma. Solo propia ficha y slot pesado autorizado por coordinación.
- Siguiente paso: confirmar base y rama, leer fuentes/contexto, preparar automatización y comprobaciones ligeras de sus contratos; distinguir entrega de código de aceptación en dispositivo.
