// In dev il backend Quarkus gira su :8085; in prod il frontend e' servito dallo
// stesso origin di Quarkus, quindi le chiamate sono relative.
export const API_BASE = import.meta.env.DEV ? 'http://localhost:8085' : '';

export function wsBase(): string {
	if (import.meta.env.DEV) return 'ws://localhost:8085';
	return location.origin.replace(/^http/, 'ws');
}
