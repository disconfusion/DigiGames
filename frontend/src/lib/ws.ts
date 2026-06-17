import { wsBase } from './config';

export type RoomEvent = {
	type: string;
	[key: string]: unknown;
};

export type RoomConnection = {
	send: (msg: Record<string, unknown>) => void;
	close: () => void;
};

const MAX_RETRIES = 6;

/**
 * Apre la connessione real-time a una stanza e si autentica col JWT.
 * Riconnette automaticamente in caso di disconnessione (es. Render sleep),
 * fino a MAX_RETRIES tentativi con backoff esponenziale.
 */
export function connectRoom(
	code: string,
	token: string,
	onEvent: (e: RoomEvent) => void,
	onStatusChange?: (s: 'connected' | 'reconnecting' | 'closed') => void
): RoomConnection {
	let ws: WebSocket | null = null;
	let retries = 0;
	let manually_closed = false;

	function connect() {
		ws = new WebSocket(`${wsBase()}/ws/room/${code}`);

		ws.onopen = () => {
			retries = 0;
			ws!.send(JSON.stringify({ type: 'hello', token }));
			onStatusChange?.('connected');
		};

		ws.onmessage = (ev) => {
			try { onEvent(JSON.parse(ev.data)); } catch { /* ignora payload non-JSON */ }
		};

		ws.onclose = () => {
			if (manually_closed) return;
			if (retries < MAX_RETRIES) {
				retries++;
				onStatusChange?.('reconnecting');
				const delay = Math.min(1000 * 2 ** (retries - 1), 30_000);
				setTimeout(connect, delay);
			} else {
				onStatusChange?.('closed');
			}
		};
	}

	connect();

	return {
		send: (msg) => {
			if (ws?.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg));
		},
		close: () => {
			manually_closed = true;
			ws?.close();
		}
	};
}
