import { browser } from '$app/environment';

export type Session = {
	token: string;
	username: string;
	displayName: string;
	role?: string;
} | null;

const KEY = 'minigames.session';

function load(): Session {
	if (!browser) return null;
	const raw = localStorage.getItem(KEY);
	try {
		return raw ? (JSON.parse(raw) as Session) : null;
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
