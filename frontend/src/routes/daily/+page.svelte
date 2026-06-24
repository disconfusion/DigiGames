<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { notifications } from '$lib/notifications.svelte';

	type DailyState = {
		// stato condiviso
		masked: string;
		wrongLetters: string[];
		revealedLetters: string[];
		wrongCount: number;
		maxWrong: number;
		status: 'PLAYING' | 'WON' | 'LOST';
		winner: string | null;
		word: string | null;
		// stato per-utente
		letterUsed: boolean;
		wordAttemptUsed: boolean;
		won: boolean;
		eliminated: boolean;
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
	let loadError = $state('');
	let loading = $state(true);
	let busy = $state(false);
	let wordInput = $state('');
	let wordError = $state('');

	const used = $derived(
		new Set([...(state?.wrongLetters ?? []), ...(state?.revealedLetters ?? [])])
	);
	const frame = $derived(FRAMES[Math.min(state?.wrongCount ?? 0, FRAMES.length - 1)]);
	const playing = $derived(state?.status === 'PLAYING');
	const eliminated = $derived(state?.eliminated ?? false);
	const canGuessLetter = $derived(playing && !eliminated && !(state?.letterUsed));
	const canGuessWord = $derived(playing && !eliminated && !(state?.wordAttemptUsed));
	const me = $derived(auth.session?.username ?? '');

	async function load() {
		try {
			state = await api<DailyState>('/api/daily');
		} catch (e) {
			loadError = (e as Error).message;
		}
	}

	onMount(async () => {
		if (!auth.session) { goto('/login'); return; }
		await load();
		loading = false;
	});

	// Aggiorna quando un altro utente fa una mossa (evento WS broadcast)
	let lastSeen = 0;
	$effect(() => {
		const t = notifications.lastDailyUpdate;
		if (t > lastSeen && !busy) { lastSeen = t; load(); }
	});

	async function guessLetter(letter: string) {
		if (!canGuessLetter || busy || used.has(letter)) return;
		busy = true;
		try {
			state = await api<DailyState>('/api/daily/letter', {
				method: 'POST',
				body: JSON.stringify({ letter })
			});
		} catch (e) {
			loadError = (e as Error).message;
		} finally {
			busy = false;
		}
	}

	async function guessWord(e: SubmitEvent) {
		e.preventDefault();
		if (!canGuessWord || busy || !wordInput.trim()) return;
		busy = true;
		wordError = '';
		try {
			const prev = state?.winner;
			state = await api<DailyState>('/api/daily/word', {
				method: 'POST',
				body: JSON.stringify({ word: wordInput.trim() })
			});
			if (state?.status !== 'WON' || state.winner === prev) {
				wordError = '✗ Parola sbagliata. Sei stato eliminato dalla parola di oggi.';
			}
			wordInput = '';
		} catch (e2) {
			loadError = (e2 as Error).message;
		} finally {
			busy = false;
		}
	}
</script>

<div class="daily">
	<h1>🗓 Parola del Giorno</h1>
	<p class="sub">Uno slot lettera + un tentativo parola a testa. Chi indovina vince 10 punti!</p>

	{#if loading}
		<p class="muted">Caricamento…</p>
	{:else if loadError}
		<p class="err">⚠ {loadError}</p>
	{:else if state}
		<pre class="gallows">{frame}</pre>

		<!-- Parola mascherata -->
		<div class="word">
			{#each state.masked.split('') as ch, i (i)}
				<span class="slot" class:filled={ch !== '_'}>{ch === '_' ? '' : ch}</span>
			{/each}
		</div>

		<!-- Errori condivisi -->
		<p class="errors">
			Errori condivisi: {state.wrongCount}/{state.maxWrong}
			{#if state.wrongLetters.length}
				— <span class="wrong">{state.wrongLetters.join(' ').toUpperCase()}</span>
			{/if}
		</p>

		<!-- Banner fine partita -->
		{#if state.status === 'WON'}
			{#if state.winner === me || state.won}
				<div class="banner won">🎉 Hai vinto! +10 punti!</div>
			{:else}
				<div class="banner won">🏆 <strong>{state.winner}</strong> ha indovinato la parola: <strong>{state.word}</strong></div>
			{/if}
		{:else if state.status === 'LOST'}
			<div class="banner lost">💀 Parola persa! Era: <strong>{state.word}</strong></div>
			{:else if eliminated}
				<div class="banner lost">☠️ Sei stato eliminato: hai sbagliato il tentativo della parola. Puoi solo guardare il resto della giornata.</div>
			{/if}

		<!-- Sezione: la tua lettera -->
		<section class="section">
			<h2>
				{#if state.letterUsed}
					✅ Lettera usata
				{:else}
					🔤 Scegli la tua lettera
				{/if}
			</h2>
			<div class="keyboard">
				{#each ALPHABET as l (l)}
					<button
						class="key"
						class:hit={state.revealedLetters.includes(l)}
						class:miss={state.wrongLetters.includes(l)}
						disabled={!canGuessLetter || busy || used.has(l)}
						onclick={() => guessLetter(l)}
					>
						{l.toUpperCase()}
					</button>
				{/each}
			</div>
		</section>

		<!-- Sezione: indovina la parola -->
		<section class="section">
			<h2>
				{#if state.wordAttemptUsed}
					{#if state.won}✅ Hai indovinato!{:else}❌ Tentativo esaurito{/if}
				{:else}
					💬 Indovina la parola
				{/if}
			</h2>
			{#if !state.wordAttemptUsed && playing}
				<form class="word-form" onsubmit={guessWord}>
					<input
						placeholder="Scrivi la parola…"
						bind:value={wordInput}
						disabled={busy}
						autocomplete="off"
					/>
					<button type="submit" disabled={busy || !wordInput.trim()}>Invia</button>
				</form>
				{#if wordError}
					<p class="word-err">{wordError}</p>
				{/if}
			{/if}
		</section>

		<p class="refresh-hint">Aggiornamento in tempo reale</p>
	{/if}
</div>

<style>
	.daily {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1.2rem;
		max-width: 600px;
		margin: 0 auto;
	}
	h1 { margin: 0; }
	.sub { color: var(--muted); margin: 0; text-align: center; }
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
	.errors { color: var(--muted); margin: 0; font-size: 0.9rem; }
	.wrong { color: #f87171; letter-spacing: 0.1em; }

	.banner {
		width: 100%;
		padding: 0.65rem 1.2rem;
		border-radius: 8px;
		font-size: 1rem;
		font-weight: 600;
		text-align: center;
	}
	.banner.won { background: #14532d; color: #bbf7d0; }
	.banner.lost { background: #7f1d1d; color: #fecaca; }

	.section {
		width: 100%;
		background: var(--panel);
		border-radius: 12px;
		padding: 1rem 1.25rem;
	}
	h2 { margin: 0 0 0.75rem; font-size: 1rem; }

	.keyboard {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
		justify-content: center;
	}
	.key {
		width: 2.2rem;
		height: 2.2rem;
		border: 1px solid #334155;
		border-radius: 8px;
		background: #0f172a;
		color: var(--text);
		font-size: 0.9rem;
		font-weight: 600;
		cursor: pointer;
	}
	.key.hit { background: #14532d; border-color: #16a34a; color: #bbf7d0; }
	.key.miss { background: #7f1d1d; border-color: #dc2626; color: #fecaca; opacity: 0.85; }
	.key:disabled { cursor: default; opacity: 0.5; }
	.key.hit:disabled, .key.miss:disabled { opacity: 1; }

	.word-form {
		display: flex;
		gap: 0.5rem;
	}
	.word-form input {
		flex: 1;
		padding: 0.55rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font-size: 1rem;
	}
	.word-form button {
		background: var(--accent);
		color: white;
		border: none;
		border-radius: 8px;
		padding: 0.55rem 1rem;
		cursor: pointer;
		font-weight: 600;
	}
	.word-form button:disabled { opacity: 0.5; cursor: default; }
	.word-err { color: #f87171; margin: 0.4rem 0 0; font-size: 0.9rem; }
	.refresh-hint { color: #475569; font-size: 0.78rem; margin: 0; }

	@media (max-width: 480px) {
		.key { width: 1.9rem; height: 1.9rem; font-size: 0.8rem; }
		.slot { width: 1.3rem; font-size: 1.2rem; }
	}
</style>
