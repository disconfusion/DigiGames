export type ToastKind = 'info' | 'success' | 'invite' | 'error';
/** Azione cliccabile dentro un toast (es. accetta/rifiuta un invito senza aprire /inviti). */
export type ToastAction = {
	label: string;
	/** primary = pulsante pieno d'accento, ghost = contorno neutro. */
	style?: 'primary' | 'ghost';
	run: () => void | Promise<void>;
};
export type Toast = { id: number; message: string; kind: ToastKind; actions?: ToastAction[] };

// Stato condiviso per notifiche real-time (badge nav + trigger refresh pagine + coda toast).
export const notifications = $state({
	inviteCount: 0,
	lastInvite: 0,
	lastDailyUpdate: 0,
	lastPresenceUpdate: 0,
	lastGift: 0,
	toasts: [] as Toast[]
});

let toastSeq = 0;

/** Accoda un toast; auto-dismiss dopo `duration` ms (0 = persistente). Ritorna l'id. */
export function showToast(
	message: string,
	kind: ToastKind = 'info',
	duration = 5000,
	actions?: ToastAction[]
): number {
	const id = ++toastSeq;
	notifications.toasts.push({ id, message, kind, actions });
	// Limita a 4 toast simultanei (rimuove i più vecchi)
	if (notifications.toasts.length > 4) notifications.toasts.shift();
	if (duration > 0) setTimeout(() => dismissToast(id), duration);
	return id;
}

export function dismissToast(id: number) {
	const i = notifications.toasts.findIndex((t) => t.id === id);
	if (i >= 0) notifications.toasts.splice(i, 1);
}

export function onInviteReceived() {
	notifications.inviteCount++;
	notifications.lastInvite = Date.now();
}

export function setInviteCount(n: number) {
	notifications.inviteCount = n;
}

/** Invito accettato/rifiutato (anche dal toast): fa ricaricare la lista in /invites. */
export function onInviteResolved() {
	notifications.lastInvite = Date.now();
}

export function onDailyUpdate() {
	notifications.lastDailyUpdate = Date.now();
}

export function onPresenceUpdate() {
	notifications.lastPresenceUpdate = Date.now();
}

export function onGiftReceived() {
	notifications.lastGift = Date.now();
}
