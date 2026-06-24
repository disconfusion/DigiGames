// Stato condiviso per notifiche real-time (badge nav + trigger refresh pagine).
export const notifications = $state({ inviteCount: 0, lastInvite: 0 });

export function onInviteReceived() {
	notifications.inviteCount++;
	notifications.lastInvite = Date.now();
}

export function setInviteCount(n: number) {
	notifications.inviteCount = n;
}
