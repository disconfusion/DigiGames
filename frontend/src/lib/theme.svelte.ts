/**
 * Il lato oscuro di DigiGames: DigiCasinò.
 *
 * Due "lati" dello stesso sito sullo stesso account: Token, companion, poteri e accessori
 * sono condivisi, cambiano solo la veste (tema casinò in `casino-theme.css`) e i giochi
 * proposti (vedi `side` in `games/catalog.ts`). Il lato si alterna scrivendo 777 fuori dai
 * campi di testo oppure con un doppio tocco sul logo, ed entrando in un tavolo del casinò.
 *
 * Come lo zoom, la scelta è del dispositivo e sta in `localStorage`; `app.html` applica
 * `data-theme` prima del primo disegno, qui si allinea lo stato reattivo.
 */
import { showToast } from '$lib/notifications.svelte';

const STORAGE_KEY = 'digigames:side';

export type Side = 'arcade' | 'casino';

export const theme = $state<{ side: Side }>({ side: 'arcade' });

export const brandName = (side: Side) => (side === 'casino' ? 'DigiCasinò' : 'DigiGames');

function apply(side: Side) {
	if (typeof document === 'undefined') return;
	if (side === 'casino') document.documentElement.dataset.theme = 'casino';
	else delete document.documentElement.dataset.theme;
}

/** Legge il lato salvato all'avvio dell'app. */
export function initSide() {
	let saved: string | null = null;
	try {
		saved = localStorage.getItem(STORAGE_KEY);
	} catch {
		/* niente storage: si parte dalla sala giochi */
	}
	theme.side = saved === 'casino' ? 'casino' : 'arcade';
	apply(theme.side);
}

/** Passa al lato indicato, con sfarfallio CRT e un saluto. Nessun effetto se ci si è già. */
export function switchSide(side: Side) {
	if (theme.side === side) return;
	theme.side = side;
	apply(side);
	try {
		localStorage.setItem(STORAGE_KEY, side);
	} catch {
		/* storage negato (finestra privata): il lato vale per questa sessione */
	}
	flicker();
	showToast(
		side === 'casino' ? 'Benvenuto nel lato oscuro: DigiCasinò' : 'Bentornato su DigiGames',
		'success',
		3000
	);
}

export const toggleSide = () => switchSide(theme.side === 'casino' ? 'arcade' : 'casino');

/** Sfarfallio delle scanline al cambio di lato (classe su <html>, keyframes in casino-theme.css). */
function flicker() {
	const root = document.documentElement;
	root.classList.remove('side-switch');
	void root.offsetWidth; // riavvia l'animazione se il cambio è ravvicinato
	root.classList.add('side-switch');
	setTimeout(() => root.classList.remove('side-switch'), 700);
}

// ── Trigger ────────────────────────────────────────────────────────────────

const SECRET = '777';
const SECRET_WINDOW_MS = 1500;
let typed = '';
let lastKeyAt = 0;

/**
 * true quando il tasto completa il codice segreto. Ignora campi di testo (chat, form),
 * scorciatoie con modificatori e ripetizioni da tasto tenuto premuto.
 */
export function isSecretCode(e: KeyboardEvent): boolean {
	if (e.ctrlKey || e.metaKey || e.altKey || e.repeat) return false;
	const t = e.target as HTMLElement | null;
	if (t && (t.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(t.tagName))) return false;
	const now = Date.now();
	if (e.key.length !== 1 || now - lastKeyAt > SECRET_WINDOW_MS) typed = '';
	lastKeyAt = now;
	if (e.key.length !== 1) return false;
	typed = (typed + e.key).slice(-SECRET.length);
	if (typed !== SECRET) return false;
	typed = '';
	return true;
}

const DOUBLE_TAP_MS = 350;
let lastTapAt = 0;

/**
 * true al secondo tocco/click ravvicinato. Non si usa `dblclick`: su iOS Safari non è
 * affidabile al tocco, mentre `pointerup` arriva uguale da mouse, dito e penna.
 */
export function isDoubleTap(e: PointerEvent): boolean {
	if (e.button !== 0) return false;
	const now = Date.now();
	const hit = now - lastTapAt < DOUBLE_TAP_MS;
	lastTapAt = hit ? 0 : now;
	return hit;
}
