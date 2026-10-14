# 09 — Mostrar alcance por país y procedencia de indicadores

**What to build:** Entender si se observa una cotización, una simulación educativa o un perfil contrastado, con alcance explícito para Colombia y Argentina.

**Blocked by:** 06

**Status:** ready-for-agent

- [ ] La interfaz distingue cotización transcrita, simulación bajo supuestos y perfil contrastado; fuente y fecha pueden completarse progresivamente, sin bloquear la aritmética disponible.
- [ ] CFT/VTUA publicados se guardan con nombre, unidad, base de impuestos y procedencia cuando se conocen. No se usan como interés, no alteran cuotas transcritas ni se recalculan sumando pagos.
- [ ] Cambiar país/producto revisa convenciones, cargos y perfil dependientes. Moneda no selecciona país y seleccionar país no acredita fidelidad contractual.
- [ ] La matriz de cobertura identifica país, producto, moneda, convención, versión y límites. Hay casos verificables de cotización y simulación educativa para Colombia y Argentina, incluidos productos dentro del alcance.
- [ ] Solo se etiqueta un perfil como contrastado si existe cronograma completo independiente, referencia fechada, comprobación de cargos/reglas y discrepancias documentadas. Primera cuota, tasa o indicador aislado no habilitan esa etiqueta.
- [ ] Se prueban rechazo de CFT/VTUA como tasa, falta de procedencia, cambio de país/producto y bloqueo de una etiqueta contractual sin evidencia suficiente. La ausencia de perfiles contrastados permanece explícita.
- [ ] No se automatizan impuestos, no se introducen porcentajes universales argentinos y no se activa prepago. Las fuentes requieren comprobar aplicabilidad/vigencia antes de liberar cualquier perfil contractual; el pendiente de precancelación de la herramienta anterior conserva su alcance separado.
- [ ] Guardar/retomar conserva procedencia, indicadores y versión de reglas, sin promover estimaciones a cotizaciones confirmadas.
