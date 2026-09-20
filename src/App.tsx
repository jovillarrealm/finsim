import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { exampleScenario, type Scenario } from './domain/scenario';
import { simulateLoan } from './engine/loan';
import LoanForm from './features/loan/LoanForm';
import LoanSummary from './features/loan/LoanSummary';
import MonthlyTable from './features/loan/MonthlyTable';
import ScenarioComparison from './features/loan/ScenarioComparison';
import RateConverter from './features/rates/RateConverter';
import Reconciliation from './features/reconciliation/Reconciliation';
import { loadScenario, parseScenario, saveScenario, serializeScenario } from './persistence/scenario';
import './styles.css';

function workspace(scenario: Scenario) {
  return { scenario, original: simulateLoan({ ...scenario, events: [] }), modified: simulateLoan(scenario) };
}

export default function App() {
  const [initial] = useState(() => {
    try {
      const saved = typeof window === 'undefined' ? null : loadScenario();
      return { workspace: workspace(saved ?? exampleScenario), notice: saved ? 'Escenario guardado recuperado.' : '' };
    } catch (error) {
      return { workspace: workspace(exampleScenario), notice: error instanceof Error ? error.message : 'No se pudo cargar el escenario.' };
    }
  });
  const [current, setCurrent] = useState(initial.workspace);
  const [notice, setNotice] = useState(initial.notice);
  const [error, setError] = useState('');
  const [page, setPage] = useState('loan');
  const [tableMode, setTableMode] = useState('modified');
  const [selectedMonth, setSelectedMonth] = useState(1);
  const [pendingEvents, setPendingEvents] = useState(false);
  const pendingEventsRef = useRef(false);
  pendingEventsRef.current = pendingEvents;
  useEffect(() => {
    if (!pendingEvents) return;
    const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ''; };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [pendingEvents]);
  const fileInput = useRef<HTMLInputElement>(null);
  function applyScenario(scenario: Scenario) {
    try { setCurrent(workspace(scenario)); setError(''); setNotice('Escenario actualizado. Guarda para conservarlo en este navegador.'); return true; }
    catch (failure) { setError(failure instanceof Error ? failure.message : 'No se pudo calcular. Conservamos el escenario anterior.'); return false; }
  }
  function save() {
    try { saveScenario(current.scenario); setNotice('Escenario guardado en este navegador.'); setError(''); }
    catch (failure) { setError(failure instanceof Error ? failure.message : 'No se pudo guardar.'); }
  }
  function exportFile() {
    const url = URL.createObjectURL(new Blob([serializeScenario(current.scenario)], { type: 'application/json' }));
    const anchor = document.createElement('a');
    anchor.href = url; anchor.download = 'finsim-escenario.json'; anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    setNotice('Copia JSON exportada.');
  }
  async function importFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    try {
      if (file.size > 2_000_000) throw new Error('El archivo supera el máximo de 2 MB.');
      const content = await file.text();
      if (pendingEventsRef.current) throw new Error('Aplica o descarta los cambios de meses antes de importar.');
      const imported = workspace(parseScenario(content));
      setCurrent(imported); setError(''); setNotice('Escenario importado. Guarda para conservarlo en este navegador.');
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'No se pudo importar. Conservamos el escenario anterior.'); }
  }
  return <div className="shell">
    <header className="app-header"><a href="#" className="brand" onClick={event => { event.preventDefault(); setPage('loan'); }}>finsim<span className="brand-dot">.</span></a><span className="badge">Tu laboratorio financiero</span></header>
    <main>
      <section className="hero"><div><p className="eyebrow">MENOS INCERTIDUMBRE. MÁS PERSPECTIVA.</p><h1>Decide con<br /><span>los números claros.</span></h1><p className="muted">Explora tu préstamo, cambia un mes y entiende qué pasa con tu dinero.</p></div><div className="hero-note"><span className="hero-symbol" aria-hidden="true">↗</span><p>Un pequeño cambio hoy.<br /><strong>Una historia distinta mañana.</strong></p><span className="muted">{current.scenario.currency} · pagos mensuales · tasa fija</span></div></section>
      <nav className="tabs" aria-label="Herramientas financieras">
        {[['loan', 'Explorar préstamo'], ['rates', 'Convertir tasas'], ['reconcile', 'Comparar extracto']].map(([id, label]) => <button key={id} aria-current={page === id ? 'page' : undefined} onClick={() => setPage(id)}>{label}</button>)}
      </nav>
      <div className="scenario-toolbar"><div><p className="eyebrow">ESCENARIO ACTUAL</p><strong>{current.scenario.name}</strong></div><div className="toolbar-actions">
        <button className="button ghost" disabled={pendingEvents} onClick={() => applyScenario(exampleScenario)}>Usar ejemplo</button>
        <button className="button ghost" disabled={pendingEvents} onClick={() => fileInput.current?.click()}>Importar</button>
        <input className="sr-only" aria-label="Archivo de escenario" ref={fileInput} disabled={pendingEvents} type="file" accept="application/json,.json" onChange={importFile} />
        <button className="button ghost" disabled={pendingEvents} onClick={exportFile}>Exportar</button><button className="button" disabled={pendingEvents} onClick={save}>Guardar escenario</button>
      </div></div>
      {pendingEvents && <p role="status" className="status-line">Hay cambios de meses pendientes. Aplica o descarta el lote en «Editar meses» antes de guardar, exportar o reemplazar el escenario.</p>}
      {notice && <p role="status" className="status-line">{notice}</p>}{error && <p role="alert" className="error note">{error}</p>}
      <div hidden={page !== 'loan'}>
        <LoanForm scenario={current.scenario} onChange={applyScenario} />
        <LoanSummary currency={current.scenario.currency} result={current.modified} />
        <ScenarioComparison scenario={current.scenario} original={current.original} modified={current.modified}
          onChange={applyScenario} onPendingChange={setPendingEvents} selectedMonth={selectedMonth} onSelectMonth={setSelectedMonth} />
        <div className="table-choice field"><label htmlFor="table-mode">Cronograma que quieres consultar</label><select id="table-mode" value={tableMode} onChange={event => setTableMode(event.target.value)}><option value="modified">Escenario modificado</option><option value="original">Escenario original · sin eventos</option></select></div>
        <MonthlyTable currency={current.scenario.currency} result={tableMode === 'modified' ? current.modified : current.original} onSelectMonth={month => {
          setSelectedMonth(month);
          requestAnimationFrame(() => {
            const heading = document.getElementById('events-title');
            heading?.focus(); heading?.scrollIntoView({ block: 'center' });
          });
        }} />
      </div>
      <div hidden={page !== 'rates'}><RateConverter /></div>
      <div hidden={page !== 'reconcile'}><Reconciliation currency={current.scenario.currency} result={current.original} /></div>
      <footer className="app-footer"><p><strong>Un espacio para explorar, no una promesa de tu banco.</strong> Modelo educativo mensual. Los extras se aplican después del pago habitual; primero seguros, luego intereses y capital. Las omisiones no generan penalidades ni intereses sobre cargos pendientes.</p><p>Los escenarios se guardan solo en este navegador. Exporta una copia para conservarlos o llevarlos a otro dispositivo.</p></footer>
    </main>
  </div>;
}
