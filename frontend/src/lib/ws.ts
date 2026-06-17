import { wsBase } from './config';

export type RoomEvent = {
	type: string;
	[key: string]: unknown;
};

/**
 * Apre la connessione real-time a una stanza e si autentica col JWT
 * (primo messaggio 'hello'). Richiama onEvent per ogni messaggio del server.
 */
export function connectRoom(code: string, token: string, onEvent: (e: RoomEvent) => void): WebSocket {
	const ws = new WebSocket(`${wsBase()}/ws/room/${code}`);
	ws.onopen = () => ws.send(JSON.stringify({ type: 'hello', token }));
	ws.onmessage = (ev) => {
		try {
			onEvent(JSON.parse(ev.data));
		} catch {
			/* ignora payload non-JSON */
		}
	};
	return ws;
}

export function send(ws: WebSocket, msg: Record<string, unknown>) {
	if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg));
}
