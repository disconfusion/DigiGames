import { API_BASE } from './config';
import { auth } from './auth.svelte';

/** Fetch JSON con base URL e Bearer token automatici. Lancia Error con il messaggio del server. */
export async function api<T = unknown>(path: string, opts: RequestInit = {}): Promise<T> {
	const headers: Record<string, string> = {
		'Content-Type': 'application/json',
		...((opts.headers as Record<string, string>) ?? {})
	};
	if (auth.session?.token) headers['Authorization'] = `Bearer ${auth.session.token}`;

	const res = await fetch(API_BASE + path, { ...opts, headers });
	const text = await res.text();
	const body = text ? JSON.parse(text) : null;
	if (!res.ok) {
		throw new Error(body?.message ?? `Errore ${res.status}`);
	}
	return body as T;
}
