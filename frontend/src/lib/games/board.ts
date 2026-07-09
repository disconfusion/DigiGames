import type { RoomEvent } from '$lib/ws';

/**
 * Contratto comune a tutti i tabelloni di gioco. Il componente reagisce agli snapshot
 * `game:state` / `game:over` ricevuti via `event` (l'ultimo è autoritativo) e invia
 * azioni col `send`. Il server è sempre l'autorità sullo stato.
 */
export type BoardProps = {
	send: (msg: Record<string, unknown>) => void;
	event: RoomEvent | null;
	me: { username: string; displayName: string };
	/** Mappa username → displayName degli utenti in stanza, per mostrare i nomi al posto degli username. */
	names?: Record<string, string>;
};
