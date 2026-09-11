/**
 * Zoom dell'interfaccia, scelto dall'utente e ricordato dal browser.
 *
 * Tutto il sito è dimensionato in `rem` e nessuno fissa la base su `html`: basta quindi
 * cambiare il font-size della radice per scalare testi, spazi, icone e contenitori insieme,
 * senza toccare un solo componente. È il rimedio a monitor grandi con testi piccoli.
 *
 * La preferenza sta in `localStorage` (è del dispositivo, non dell'account: lo stesso utente
 * può volere una scala diversa sul portatile e sul monitor grande). Per evitare lo scatto
 * visibile al caricamento, la scala viene applicata anche da uno script inline in `app.html`
 * prima del primo disegno: qui la si riapplica solo per allineare lo stato reattivo.
 */

const STORAGE_KEY = 'digigames:ui-scale';

export const MIN_SCALE = 0.8;
export const MAX_SCALE = 1.6;
export const SCALE_STEP = 0.1;

export const uiScale = $state({ value: 1 });

/** Arrotonda al decimo: evita che i passi accumulino 0.7999999. */
function normalize(value: number): number {
	if (!Number.isFinite(value)) return 1;
	return Math.min(MAX_SCALE, Math.max(MIN_SCALE, Math.round(value * 10) / 10));
}

/**
 * La scala è una percentuale del default del browser, non un valore in pixel: chi ha già
 * caratteri grandi nelle preferenze del browser se li tiene, moltiplicati per questo fattore.
 * Scrivere "16px" glieli cancellerebbe.
 */
function apply(value: number) {
	if (typeof document === 'undefined') return;
	document.documentElement.style.fontSize = `${(value * 100).toFixed(1)}%`;
}

export function setScale(value: number) {
	const next = normalize(value);
	uiScale.value = next;
	apply(next);
	try {
		localStorage.setItem(STORAGE_KEY, String(next));
	} catch {
		/* storage negato (finestra privata): lo zoom vale per questa sessione */
	}
}

/** Un passo avanti o indietro sulla scala. */
export function stepScale(delta: number) {
	setScale(uiScale.value + delta);
}

export function resetScale() {
	setScale(1);
}

/** Legge la preferenza salvata all'avvio dell'app. */
export function initScale() {
	let saved = 1;
	try {
		saved = Number(localStorage.getItem(STORAGE_KEY));
	} catch {
		/* niente storage: si parte da 1 */
	}
	const value = normalize(saved || 1);
	uiScale.value = value;
	apply(value);
}

export const scalePercent = () => Math.round(uiScale.value * 100);
