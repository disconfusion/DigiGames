<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { gameLabel } from '$lib/games/catalog';
	import { showToast } from '$lib/notifications.svelte';
	import Icon from '$lib/icons/Icon.svelte';

	type Power = {
		id: string;
		game: string;
		label: string;
		emoji: string;
		description: string;
		usage: string;
		phase: string;
		cost: number;
		owned: number;
	};

	let balance = $state(0);
	let powers = $state<Power[]>([]);
	let loading = $state(true);
	let error = $state('');
	let busy = $state<Record<string, boolean>>({});

	// Raggruppa i poteri per gioco (predisposto per più giochi in futuro)
	const byGame = $derived.by(() => {
		const m = new Map<string, Power[]>();
		for (const p of powers) {
			if (!m.has(p.game)) m.set(p.game, []);
			m.get(p.game)!.push(p);
		}
		return [...m.entries()];
	});

	async function load() {
		try {
			const r = await api<{ balance: number; powers: Power[] }>('/api/shop/powers');
			balance = r.balance;
			powers = r.powers;
		} catch (e) {
			error = (e as Error).message;
		} finally {
			loading = false;
		}
	}

	onMount(() => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		load();
	});

	async function buy(p: Power) {
		if (busy[p.id] || balance < p.cost) return;
		busy = { ...busy, [p.id]: true };
		try {
			const r = await api<{ message: string; balance: number }>(`/api/shop/powers/${p.id}/buy`, {
				method: 'POST'
			});
			balance = r.balance;
			powers = powers.map((x) => (x.id === p.id ? { ...x, owned: x.owned + 1 } : x));
			showToast(r.message, 'success');
		} catch (e) {
			showToast((e as Error).message, 'error');
		} finally {
			busy = { ...busy, [p.id]: false };
		}
	}
</script>

<div class="shop">
	<div class="head">
		<h1><Icon name="cart" size={22} title="Shop" /> Shop</h1>
		<div class="balance"><Icon name="coin" size={18} title="Token" /> <strong>{balance}</strong> Token</div>
	</div>
	<p class="sub">Spendi i Token guadagnati giocando. I poteri si usano in partita dal Cyberdeck.</p>

	{#if loading}
		<p class="muted">Caricamento…</p>
	{:else if error}
		<p class="err">⚠ {error}</p>
	{:else}
		{#each byGame as [game, list] (game)}
			<section class="game-block">
				<h2>{gameLabel(game)}</h2>
				<div class="grid">
					{#each list as p (p.id)}
						{@const affordable = balance >= p.cost}
						<article class="power" class:owned={p.owned > 0}>
							<div class="emoji"><Icon name={p.id} size={32} title={p.label} /></div>
							<h3>{p.label}</h3>
							<p class="desc">{p.description}</p>
							<p class="usage"><span class="usage-label">Uso:</span> {p.usage}</p>
							<div class="foot">
								<span class="cost" class:cheap={affordable} class:dear={!affordable}><Icon name="coin" size={14} /> {p.cost}</span>
								{#if p.owned > 0}<span class="have">Possiedi {p.owned}</span>{/if}
							</div>
							<button onclick={() => buy(p)} disabled={busy[p.id] || !affordable}>
								{busy[p.id] ? '…' : affordable ? 'Acquista' : 'Token insuff.'}
							</button>
						</article>
					{/each}
				</div>
			</section>
		{/each}
	{/if}
</div>

<style>
	.shop {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}
	.head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		flex-wrap: wrap;
		gap: 0.75rem;
	}
	h1 {
		margin: 0;
		font-family: var(--font-display);
		font-size: clamp(1rem, 4vw, 1.5rem);
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.balance {
		font-family: var(--font-ui);
		font-size: 1rem;
		color: var(--amber);
		border: 1px solid var(--amber);
		border-radius: 999px;
		padding: 0.3rem 0.9rem;
		text-shadow: 0 0 6px rgba(255, 207, 63, 0.5);
		white-space: nowrap;
	}
	.balance strong {
		font-size: 1.15rem;
	}
	.sub {
		color: var(--muted);
		margin: 0 0 0.5rem;
	}
	.muted {
		color: var(--muted);
	}
	.err {
		color: var(--danger);
		font-family: var(--font-term);
		font-size: 1.1rem;
	}
	.game-block h2 {
		font-family: var(--font-display);
		font-size: 0.85rem;
		color: var(--accent);
		text-shadow: var(--glow-mag);
		margin: 0.5rem 0 0.75rem;
	}
	.grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
		gap: 1rem;
	}
	.power {
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
		background: var(--panel);
		border: 1px solid var(--line);
		border-radius: 12px;
		padding: 1rem;
	}
	.power.owned {
		border-color: var(--cyan);
		box-shadow: 0 0 10px rgba(47, 243, 255, 0.18);
	}
	.emoji {
		font-size: 1.8rem;
		line-height: 1;
	}
	h3 {
		margin: 0;
		font-family: var(--font-ui);
		font-size: 0.95rem;
		color: var(--text);
		letter-spacing: 0.02em;
	}
	.desc {
		margin: 0;
		font-size: 0.9rem;
		color: var(--muted);
		flex: 1;
	}
	.usage {
		margin: 0;
		font-size: 0.8rem;
		color: var(--muted);
		opacity: 0.85;
	}
	.usage-label {
		color: var(--cyan);
		text-transform: uppercase;
		font-size: 0.7rem;
		letter-spacing: 0.05em;
	}
	.foot {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
		margin-top: 0.2rem;
	}
	.cost {
		font-family: var(--font-ui);
		font-weight: 700;
	}
	.cost.cheap {
		color: var(--amber);
	}
	.cost.dear {
		color: var(--danger);
		opacity: 0.8;
	}
	.have {
		font-size: 0.78rem;
		color: var(--green);
	}
	button {
		margin-top: 0.2rem;
		padding: 0.55rem 1rem;
		border: 2px solid var(--accent);
		border-radius: 6px;
		background: linear-gradient(180deg, var(--accent), #c01e63);
		color: #fff;
		font-family: var(--font-ui);
		font-weight: 700;
		text-transform: uppercase;
		letter-spacing: 0.04em;
		font-size: 0.8rem;
		cursor: pointer;
		min-height: 40px;
		box-shadow: 0 0 12px rgba(255, 46, 136, 0.35);
		transition: box-shadow 0.15s, opacity 0.15s;
	}
	button:hover:not(:disabled) {
		box-shadow: 0 0 20px rgba(255, 46, 136, 0.6);
	}
	button:disabled {
		opacity: 0.45;
		cursor: not-allowed;
		background: var(--inset);
		border-color: var(--line);
		box-shadow: none;
	}
</style>
