import { browser } from '$app/environment';

export type Session = {
	token: string;
	username: string;
	displayName: string;
	role?: string;
} | null;

const KEY = 'minigames.session';

function isExpired(token: string): boolean {
	try {
		const payload = JSON.parse(atob(token.split('.')[1]));
		return payload.exp * 1000 < Date.now();
	} catch {
		return true;
	}
}

function load(): Session {
	if (!browser) return null;
	const raw = localStorage.getItem(KEY);
	try {
		const s = raw ? (JSON.parse(raw) as Session) : null;
		if (s && isExpired(s.token)) {
			localStorage.removeItem(KEY);
			return null;
		}
		return s;
	} catch {
		return null;
	}
}

// Stato reattivo condiviso (Svelte 5 runes).
export const auth = $state<{ session: Session }>({ session: load() });

export function setSession(s: Session) {
	auth.session = s;
	if (browser) {
		if (s) localStorage.setItem(KEY, JSON.stringify(s));
		else localStorage.removeItem(KEY);
	}
}

export function logout() {
	setSession(null);
}
