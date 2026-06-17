<script lang="ts">
	import { onMount } from 'svelte';
	import type { RoomEvent } from '$lib/ws';
	import type { HangmanGameState } from './hangman';

	let {
		send,
		event,
		me
	}: {
		send: (msg: Record<string, unknown>) => void;
		event: RoomEvent | null;
		me: { email: string; displayName: string };
	} = $props();

	let state = $state<HangmanGameState | null>(null);
	let over = $state<{ status: string; word: string } | null>(null);

	// Reagisce agli snapshot di gioco (l'ultimo è sempre autoritativo).
	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			state = e as unknown as HangmanGameState;
			if ((e as { status?: string }).status === 'PLAYING') over = null;
		} else if (e.type === 'game:over') {
			over = { status: String(e.status), word: String(e.word) };
		}
	});

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

	const playing = $derived(state?.status === 'PLAYING');
	const used = $derived(new Set([...(state?.guessed ?? []), ...(state?.wrong ?? [])]));
	const frame = $derived(FRAMES[Math.min(state?.wrongCount ?? 0, FRAMES.length - 1)]);

	function tryLetter(l: string) {
		if (playing && !used.has(l)) send({ type: 'guess', letter: l });
	}
	const startGame = () => send({ type: 'game:start' });

	onMount(() => {
		const handler = (e: KeyboardEvent) => {
			const k = e.key.toLowerCase();
			if (k.length === 1 && k >= 'a' && k <= 'z') tryLetter(k);
		};
		window.addEventListener('keydown', handler);
		return () => window.removeEventListener('keydown', handler);
	});
</script>

<div class="hangman">
	{#if !state}
		<p class="muted">Nessuna partita in corso.</p>
		<button class="start" onclick={startGame}>Inizia partita</button>
	{:else}
		<pre class="gallows">{frame}</pre>

		<div class="word" aria-label="parola">
			{#each state.masked.split('') as ch, i (i)}
				<span class="slot" class:filled={ch !== '_'}>{ch === '_' ? '' : ch}</span>
			{/each}
		</div>

		<p class="errors">
			Errori: {state.wrongCount}/{state.maxWrong}
			{#if state.wrong.length}— <span class="wrong">{state.wrong.join(' ').toUpperCase()}</span>{/if}
		</p>

		{#if over}
			<div class="result" class:won={over.status === 'WON'} class:lost={over.status === 'LOST'}>
				{#if over.status === 'WON'}🎉 Indovinata!{:else}💀 Impiccato! La parola era <strong>{over.word}</strong>{/if}
			</div>
			<button class="start" onclick={startGame}>Nuova parola</button>
		{/if}

		<div class="keyboard">
			{#each ALPHABET as l (l)}
				<button
					class="key"
					class:hit={state.guessed.includes(l)}
					class:miss={state.wrong.includes(l)}
					disabled={!playing || used.has(l)}
					onclick={() => tryLetter(l)}
				>
					{l.toUpperCase()}
				</button>
			{/each}
		</div>
	{/if}
</div>

<style>
	.hangman {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
	}
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
	.slot.filled {
		border-bottom-color: var(--accent);
	}
	.errors {
		color: var(--muted);
		margin: 0;
	}
	.wrong {
		color: #f87171;
		letter-spacing: 0.1em;
	}
	.result {
		font-size: 1.1rem;
		padding: 0.6rem 1rem;
		border-radius: 8px;
		text-align: center;
	}
	.result.won {
		background: #14532d;
		color: #bbf7d0;
	}
	.result.lost {
		background: #7f1d1d;
		color: #fecaca;
	}
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
	.key.hit {
		background: #14532d;
		border-color: #16a34a;
		color: #bbf7d0;
	}
	.key.miss {
		background: #7f1d1d;
		border-color: #dc2626;
		color: #fecaca;
		opacity: 0.85;
	}
	.start {
		padding: 0.6rem 1.2rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: white;
		font-size: 1rem;
		cursor: pointer;
	}
	@media (max-width: 480px) {
		.key {
			width: 2rem;
			height: 2rem;
			font-size: 0.9rem;
		}
		.slot {
			width: 1.3rem;
			font-size: 1.2rem;
		}
	}
</style>
