import { api } from '$lib/api';

/**
 * Saldo Token condiviso fra header, shop e regali.
 * Reattivo: il backend pubblica `{type:"tokens",balance}` sul canale `/ws/notify` a ogni
 * variazione (vittoria, acquisto, regalo) e il layout chiama `setBalance` — niente attese
 * del polling. `refreshBalance` resta come rete di sicurezza (login, riconnessione WS).
 */
export const wallet = $state({ balance: 0 });

export function setBalance(n: number) {
	wallet.balance = n;
}

export async function refreshBalance() {
	try {
		const r = await api<{ balance: number }>('/api/tokens');
		wallet.balance = r.balance;
	} catch {
		/* silenzioso */
	}
}
