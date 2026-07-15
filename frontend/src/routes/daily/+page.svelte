<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { notifications } from '$lib/notifications.svelte';
	import Icon from '$lib/icons/Icon.svelte';

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
		custom: boolean; // parola scelta manualmente dall'admin
		callout: string | null; // messaggio dell'admin per la giornata
		// stato per-utente
		letterUsed: boolean;
		wordAttemptUsed: boolean;
		won: boolean;
		eliminated: boolean;
		myLetter: string | null;
		wordGuess: string | null;
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
	// Esito della mia lettera: era presente nella parola? (revealedLetters/myLetter sono minuscoli)
	const myLetterCorrect = $derived(
		!!state?.myLetter && (state?.revealedLetters?.includes(state.myLetter) ?? false)
	);

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
	<h1><Icon name="calendar" size={22} title="Parola del Giorno" /> Parola del Giorno</h1>
	<p class="sub">Uno slot lettera + un tentativo parola a testa. Chi indovina vince 10 punti!</p>

	{#if state?.callout}
		<div class="callout" role="status"><Icon name="speech" size={18} /> <span>{state.callout}</span></div>
	{/if}

	{#if state?.custom}
		<p class="custom-word"><Icon name="tools" size={16} /> Parola scelta dall'admin</p>
	{/if}

	<details class="rules">
		<summary><Icon name="book" size={16} /> Come si gioca</summary>
		<ul>
			<li><Icon name="globe" size={16} /> Ogni giorno <strong>una sola parola</strong>, uguale per tutti i colleghi.</li>
			<li><Icon name="letters" size={16} /> Hai <strong>una lettera</strong> e <strong>un tentativo di parola intera</strong> per tutta la giornata.</li>
			<li><Icon name="draw" size={16} /> Gli errori sono <strong>condivisi</strong>: a {state?.maxWrong ?? 6} errori totali la parola è persa per tutti.</li>
			<li><Icon name="party" size={16} /> Indovini la parola → <strong>vinci +10 punti</strong>.</li>
			<li><Icon name="lose" size={16} /> Sbagli il tentativo di parola → <strong>eliminato</strong> per oggi: puoi solo guardare.</li>
			<li><Icon name="sync" size={16} /> Aggiornamento <strong>in tempo reale</strong> quando gli altri giocano.</li>
			<li><Icon name="clock" size={16} /> Nuova parola a <strong>mezzanotte (ora di Roma)</strong>.</li>
		</ul>
	</details>

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
				<div class="banner won"><Icon name="party" size={18} /> Hai vinto! +10 punti!</div>
			{:else}
				<div class="banner won"><Icon name="win" size={18} /> <strong>{state.winner}</strong> ha indovinato la parola: <strong>{state.word}</strong></div>
			{/if}
		{:else if state.status === 'LOST'}
			<div class="banner lost"><Icon name="lose" size={18} /> Parola persa! Era: <strong>{state.word}</strong></div>
			{:else if eliminated}
				<div class="banner lost"><Icon name="lose" size={18} /> Sei stato eliminato: hai sbagliato il tentativo della parola. Puoi solo guardare il resto della giornata.</div>
			{/if}

		<!-- Sezione: la tua lettera -->
		<section class="section">
			<h2>
				{#if state.letterUsed}
					<Icon name="check" size={16} /> Lettera usata
				{:else}
					<Icon name="letters" size={16} /> Scegli la tua lettera
				{/if}
			</h2>
			{#if state.letterUsed && state.myLetter}
				<p class="my-move">
					La tua lettera:
					<span class="chip" class:hit={myLetterCorrect} class:miss={!myLetterCorrect}>
						{state.myLetter.toUpperCase()}
					</span>
					{#if myLetterCorrect}— presente nella parola{:else}— non c'era{/if}
				</p>
			{/if}
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
					{#if state.won}<Icon name="check" size={16} /> Hai indovinato!{:else}<Icon name="cross" size={16} /> Tentativo esaurito{/if}
				{:else}
					<Icon name="speech" size={16} /> Indovina la parola
				{/if}
			</h2>
			{#if state.wordAttemptUsed && state.wordGuess}
				<p class="my-move">
					Hai tentato:
					<span class="chip" class:hit={state.won} class:miss={!state.won}>{state.wordGuess.toUpperCase()}</span>
				</p>
			{/if}
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
	h1 {
		margin: 0;
		font-family: var(--font-display);
		font-size: clamp(0.9rem, 4vw, 1.4rem);
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
		text-align: center;
	}
	.sub { color: var(--muted); margin: 0; text-align: center; }
	.custom-word {
		display: inline-flex;
		align-items: center;
		gap: 0.4rem;
		margin: 0;
		padding: 0.3rem 0.8rem;
		border-radius: 20px;
		border: 1px solid var(--amber);
		background: color-mix(in srgb, var(--amber) 12%, var(--inset));
		color: var(--amber);
		font-family: var(--font-ui);
		font-size: 0.8rem;
		letter-spacing: 0.03em;
	}
	.callout {
		width: 100%;
		display: flex;
		align-items: flex-start;
		gap: 0.6rem;
		padding: 0.7rem 1rem;
		border-radius: 10px;
		border: 1px solid var(--cyan);
		background: color-mix(in srgb, var(--cyan) 10%, var(--panel));
		color: var(--text);
		font-family: var(--font-term);
		font-size: 1rem;
		line-height: 1.4;
		box-shadow: var(--glow-cyan);
	}
	.callout span { white-space: pre-wrap; overflow-wrap: anywhere; flex: 1; }
	.muted { color: var(--muted); }
	.err { color: var(--danger); font-family: var(--font-term); font-size: 1.1rem; }

	/* ── Pannello regole (collassabile) ─────────────────────────────── */
	.rules {
		width: 100%;
		background: var(--panel);
		border: 1px solid var(--line);
		border-radius: 12px;
		padding: 0.5rem 1rem;
	}
	.rules summary {
		cursor: pointer;
		font-family: var(--font-ui);
		text-transform: uppercase;
		letter-spacing: 0.04em;
		font-size: 0.85rem;
		color: var(--cyan);
		padding: 0.4rem 0;
		list-style: none;
		user-select: none;
	}
	.rules summary::-webkit-details-marker { display: none; }
	.rules summary::before {
		content: '▸ ';
		display: inline-block;
		transition: transform 0.15s;
	}
	.rules[open] summary::before {
		content: '▾ ';
	}
	.rules ul {
		margin: 0.4rem 0 0.6rem;
		padding-left: 1.2rem;
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
	}
	.rules li {
		font-family: var(--font-term);
		font-size: 1rem;
		line-height: 1.4;
		color: var(--text);
	}
	.rules strong { color: var(--amber); }
	@media (prefers-reduced-motion: reduce) {
		.rules summary::before { transition: none; }
	}

	.gallows {
		font-family: ui-monospace, monospace;
		font-size: 1rem;
		line-height: 1.15;
		color: var(--green);
		text-shadow: 0 0 8px rgba(61, 255, 154, 0.55);
		background: var(--inset);
		border: 1px solid var(--line);
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
		width: 1.8rem;
		height: 2.4rem;
		display: flex;
		align-items: center;
		justify-content: center;
		font-family: var(--font-display);
		font-size: 1.1rem;
		text-transform: uppercase;
		border-bottom: 4px solid var(--cyan);
	}
	.slot.filled {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.errors { color: var(--muted); margin: 0; font-size: 0.95rem; }
	.wrong { color: var(--danger); letter-spacing: 0.1em; }

	/* Riepilogo delle mosse personali dell'utente (lettera giocata / parola tentata) */
	.my-move {
		color: var(--muted);
		margin: 0 0 0.75rem;
		font-size: 0.95rem;
		display: flex;
		align-items: center;
		gap: 0.4rem;
		flex-wrap: wrap;
	}
	.chip {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		min-width: 1.8rem;
		max-width: 100%;
		padding: 0.15rem 0.55rem;
		border-radius: 4px;
		font-family: var(--font-ui);
		font-weight: 700;
		letter-spacing: 0.06em;
		border: 2px solid var(--line);
		overflow-wrap: anywhere;
	}
	.chip.hit {
		color: var(--green);
		border-color: var(--green);
		background: #10331f;
		box-shadow: 0 0 8px rgba(61, 255, 154, 0.35);
	}
	.chip.miss {
		color: var(--danger);
		border-color: var(--danger);
		background: #3a1420;
	}

	.banner {
		width: 100%;
		padding: 0.65rem 1.2rem;
		border-radius: 8px;
		font-family: var(--font-ui);
		font-size: 1rem;
		font-weight: 700;
		text-align: center;
	}
	.banner.won {
		background: #10331f;
		color: var(--green);
		box-shadow: 0 0 18px rgba(61, 255, 154, 0.35);
	}
	.banner.lost {
		background: #3a1420;
		color: var(--danger);
		box-shadow: 0 0 18px rgba(255, 82, 119, 0.35);
	}

	.section {
		width: 100%;
		background: var(--panel);
		border-radius: 12px;
		padding: 1rem 1.25rem;
		border: 1px solid var(--line);
	}
	h2 {
		margin: 0 0 0.75rem;
		font-family: var(--font-ui);
		text-transform: uppercase;
		letter-spacing: 0.04em;
		font-size: 0.85rem;
		color: var(--cyan);
	}

	.keyboard {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
		justify-content: center;
	}
	.key {
		width: 2.75rem;
		height: 2.75rem;
		border: 2px solid rgba(47, 243, 255, 0.5);
		border-radius: 4px;
		background: transparent;
		color: var(--cyan);
		font-family: var(--font-ui);
		font-size: 0.95rem;
		font-weight: 700;
		cursor: pointer;
		transition: box-shadow 0.12s, border-color 0.12s;
	}
	.key:hover:not(:disabled) { border-color: var(--cyan); box-shadow: var(--glow-cyan); }
	.key.hit {
		background: #10331f;
		border-color: var(--green);
		color: var(--green);
		box-shadow: 0 0 10px rgba(61, 255, 154, 0.4);
	}
	.key.miss {
		background: #3a1420;
		border-color: var(--danger);
		color: var(--danger);
	}
	.key:disabled { cursor: default; opacity: 0.5; }
	.key.hit:disabled, .key.miss:disabled { opacity: 1; }

	.word-form {
		display: flex;
		gap: 0.5rem;
	}
	.word-form input {
		flex: 1;
		padding: 0.55rem;
		border-radius: 4px;
		border: 2px solid var(--line);
		background: var(--inset);
		color: var(--text);
		font-family: var(--font-term);
		font-size: 1.1rem;
	}
	.word-form input:focus { outline: none; border-color: var(--cyan); box-shadow: var(--glow-cyan); }
	.word-form button {
		background: linear-gradient(180deg, var(--accent), #c01e63);
		color: #fff;
		border: 2px solid var(--amber);
		border-radius: 4px;
		padding: 0.55rem 1rem;
		cursor: pointer;
		font-family: var(--font-ui);
		font-weight: 700;
		text-transform: uppercase;
		min-height: 44px;
		box-shadow: 0 0 14px rgba(255, 46, 136, 0.4);
	}
	.word-form button:disabled { opacity: 0.5; cursor: default; }
	.word-err { color: var(--danger); font-family: var(--font-term); margin: 0.4rem 0 0; font-size: 1.05rem; }
	.refresh-hint { color: var(--muted); font-size: 0.78rem; margin: 0; }

	@media (prefers-reduced-motion: reduce) {
		.key { transition: none; }
	}
	@media (max-width: 480px) {
		.key { width: 2.3rem; height: 2.3rem; font-size: 0.85rem; }
		.slot { width: 1.4rem; font-size: 0.95rem; }
	}
</style>
