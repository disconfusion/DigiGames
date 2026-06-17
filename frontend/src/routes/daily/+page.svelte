<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';

	type DailyState = {
		masked: string;
		wrong: string[];
		guessed: string[];
		wrongCount: number;
		maxWrong: number;
		status: 'PLAYING' | 'WON' | 'LOST';
		word: string | null;
	};

	const ALPHABET = 'abcdefghijklmnopqrstuvwxyz'.split('');

	const FRAMES = [
		' +---+\n |   |\n     |\n     |\n     |\n     |\n=======',
		' +---+\n |   |\n O   |\n     |\n     |\n     |\n=======',
		' +---+\n |   |\n O   |\n |   |\n     |\n     |\n=======',
		' +---+\n |   |\n O   |\n/|   |\n     |\n     |\n=======',
		' +---+\n |   |\n O   |\n/|\\  |\n     |\n     |\n=======',
		' +---+\n |   |\n O   |\n/|\\  |\n/    |\n     |\n=======',
		' +---+\n |   |\n O   |\n/|\\  |\n/ \\  |\n     |\n======='
	];

	let state = $state<DailyState | null>(null);
	let error = $state('');
	let loading = $state(true);
	let guessing = $state(false);

	const used = $derived(new Set([...(state?.guessed ?? []), ...(state?.wrong ?? [])]));
	const frame = $derived(FRAMES[Math.min(state?.wrongCount ?? 0, FRAMES.length - 1)]);
	const playing = $derived(state?.status === 'PLAYING');

	onMount(async () => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		try {
			state = await api<DailyState>('/api/daily');
		} catch (e) {
			error = (e as Error).message;
		} finally {
			loading = false;
		}
	});

	async function guess(letter: string) {
		if (!playing || guessing || used.has(letter)) return;
		guessing = true;
		try {
			state = await api<DailyState>('/api/daily/guess', {
				method: 'POST',
				body: JSON.stringify({ letter })
			});
		} catch (e) {
			error = (e as Error).message;
		} finally {
			guessing = false;
		}
	}

	function handleKeydown(e: KeyboardEvent) {
		const k = e.key.toLowerCase();
		if (k.length === 1 && k >= 'a' && k <= 'z') guess(k);
	}
</script>

<svelte:window onkeydown={handleKeydown} />

<div class="daily">
	<h1>🗓 Impiccato del Giorno</h1>
	<p class="sub">Una parola al giorno — vale solo per te!</p>

	{#if loading}
		<p class="muted">Caricamento…</p>
	{:else if error}
		<p class="err">⚠ {error}</p>
	{:else if state}
		<pre class="gallows">{frame}</pre>

		<div class="word">
			{#each state.masked.split('') as ch, i (i)}
				<span class="slot" class:filled={ch !== '_'}>{ch === '_' ? '' : ch}</span>
			{/each}
		</div>

		<p class="errors">
			Errori: {state.wrongCount}/{state.maxWrong}
			{#if state.wrong.length}
				— <span class="wrong">{state.wrong.join(' ').toUpperCase()}</span>
			{/if}
		</p>

		{#if state.status === 'WON'}
			<div class="banner won">🎉 Complimenti! Hai indovinato la parola di oggi!</div>
		{:else if state.status === 'LOST'}
			<div class="banner lost">
				💀 Sei stato impiccato! La parola era <strong>{state.word}</strong>.
			</div>
		{/if}

		<div class="keyboard">
			{#each ALPHABET as l (l)}
				<button
					class="key"
					class:hit={state.guessed.includes(l)}
					class:miss={state.wrong.includes(l)}
					disabled={!playing || guessing || used.has(l)}
					onclick={() => guess(l)}
				>
					{l.toUpperCase()}
				</button>
			{/each}
		</div>
	{/if}
</div>

<style>
	.daily {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
	}
	h1 { margin: 0; }
	.sub { color: var(--muted); margin: 0; }
	.muted { color: var(--muted); }
	.err { color: #f87171; }

	.gallows {
		font-family: ui-monospace, monospace;
		font-size: 1rem;
		line-height: 1.15;
		background: #0f172a;
		padding: 0.75rem 1rem;
		border-radius: 8px;
		margin: 0;
	}
	.word {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		justify-content: center;
	}
	.slot {
		width: 1.6rem;
		height: 2.2rem;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 1.5rem;
		font-weight: 700;
		text-transform: uppercase;
		border-bottom: 3px solid #475569;
	}
	.slot.filled { border-bottom-color: var(--accent); }
	.errors { color: var(--muted); margin: 0; }
	.wrong { color: #f87171; letter-spacing: 0.1em; }

	.banner {
		padding: 0.65rem 1.2rem;
		border-radius: 8px;
		font-size: 1rem;
		font-weight: 600;
		text-align: center;
	}
	.banner.won { background: #14532d; color: #bbf7d0; }
	.banner.lost { background: #7f1d1d; color: #fecaca; }

	.keyboard {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		justify-content: center;
		max-width: 520px;
	}
	.key {
		width: 2.4rem;
		height: 2.4rem;
		border: 1px solid #334155;
		border-radius: 8px;
		background: #1e293b;
		color: var(--text);
		font-size: 1rem;
		font-weight: 600;
		cursor: pointer;
	}
	.key.hit { background: #14532d; border-color: #16a34a; color: #bbf7d0; }
	.key.miss { background: #7f1d1d; border-color: #dc2626; color: #fecaca; opacity: 0.85; }
	.key:disabled { cursor: default; }

	@media (max-width: 480px) {
		.key { width: 2rem; height: 2rem; font-size: 0.9rem; }
		.slot { width: 1.3rem; font-size: 1.2rem; }
	}
</style>
