<script lang="ts">
	import { onMount } from 'svelte';
	import type { RoomEvent } from '$lib/ws';
	import { gallows, type HangmanGameState } from './hangman';
	import GameResultOverlay from './GameResultOverlay.svelte';
	import Icon from '$lib/icons/Icon.svelte';

	let {
		send,
		event,
		me,
		names
	}: {
		send: (msg: Record<string, unknown>) => void;
		event: RoomEvent | null;
		me: { username: string; displayName: string };
		names?: Record<string, string>;
	} = $props();

	let state = $state<HangmanGameState | null>(null);
	let over = $state<{ status: string; word: string } | null>(null);

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
	const VOWELS = new Set(['a', 'e', 'i', 'o', 'u']);

	const playing  = $derived(state?.status === 'PLAYING');
	const used     = $derived(new Set([...(state?.guessed ?? []), ...(state?.wrong ?? [])]));
	const frame    = $derived(gallows(state?.wrongCount ?? 0, state?.accessories ?? [], state?.status === 'LOST'));
	const isMyTurn = $derived(state?.currentTurn == null || state.currentTurn === me.username);
	// Nome leggibile del giocatore di turno (displayName se disponibile, altrimenti username).
	const turnName = $derived(
		state?.currentTurn ? (names?.[state.currentTurn] ?? state.currentTurn) : ''
	);

	const vowelLimit      = $derived(state?.maxVowels ?? 0);
	const vowelBudgetOver  = $derived(vowelLimit > 0 && (state?.vowelsCalled ?? 0) >= vowelLimit);

	const letterLimit     = $derived(state?.lettersPerPlayer ?? 0);
	const myLettersUsed    = $derived(state?.lettersUsed?.[me.username] ?? 0);
	const letterBudgetOver = $derived(letterLimit > 0 && myLettersUsed >= letterLimit);

	const amEliminated = $derived((state?.eliminated ?? []).includes(me.username));
	const canPlay  = $derived(playing && isMyTurn && !letterBudgetOver && !amEliminated);
	// Tentativo parola: consentito solo nel proprio turno, finché gioco in corso e non eliminato.
	const canGuessWord = $derived(playing && isMyTurn && !amEliminated);

	// Esito personale: se ho perso pur essendo stato indovinato da altri, resta una sconfitta.
	const iWon = $derived(over?.status === 'WON' && !amEliminated);

	let wordGuess = $state('');

	function blocked(l: string): boolean {
		return used.has(l) || (VOWELS.has(l) && vowelBudgetOver);
	}

	function tryLetter(l: string) {
		if (canPlay && !blocked(l)) send({ type: 'guess', letter: l });
	}

	function submitWord(e: Event) {
		e.preventDefault();
		const w = wordGuess.trim();
		if (!w || !canGuessWord) return;
		if (!confirm(`Tentare la parola "${w}"? Se è sbagliata verrai eliminato dalla partita.`)) return;
		send({ type: 'guessWord', word: w });
		wordGuess = '';
	}
	const startGame = () => send({ type: 'game:start' });

	onMount(() => {
		const handler = (e: KeyboardEvent) => {
			// Fix bug: non intercettare se il focus è su un input/textarea
			const tag = (e.target as HTMLElement).tagName;
			if (tag === 'INPUT' || tag === 'TEXTAREA') return;
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

		<!-- Limiti regole personalizzate -->
		{#if vowelLimit > 0 || letterLimit > 0}
			<p class="limits">
				{#if vowelLimit > 0}<span class="chip" class:exhausted={vowelBudgetOver}><Icon name="letters" size={14} /> Vocali: {state.vowelsCalled}/{vowelLimit}</span>{/if}
				{#if letterLimit > 0}<span class="chip" class:exhausted={letterBudgetOver}><Icon name="hand" size={14} /> Tue lettere: {myLettersUsed}/{letterLimit}</span>{/if}
			</p>
		{/if}

		<!-- Indicatore turno -->
		{#if playing && state.currentTurn != null}
			{#if isMyTurn}
				<div class="turn-badge my-turn">Tocca a te!</div>
			{:else}
				<div class="turn-badge wait">Tocca a <strong>{turnName}</strong></div>
			{/if}
		{/if}

		<!-- Tentativo parola intera — rischioso: se sbagliato = eliminazione diretta -->
		{#if canGuessWord}
			<form class="word-guess" onsubmit={submitWord}>
				<input
					class="word-input"
					type="text"
					bind:value={wordGuess}
					placeholder="Indovina la parola…"
					autocomplete="off"
					autocapitalize="none"
					spellcheck="false"
					aria-label="Indovina l'intera parola"
				/>
				<button class="risk" type="submit" disabled={!wordGuess.trim()}>Rischia</button>
			</form>
			<p class="hint">Puoi tentare la parola nel tuo turno. Se sbagli, sei eliminato.</p>
		{:else if amEliminated && playing}
			<div class="eliminated-banner">
				<Icon name="cross" size={16} /> Sei stato eliminato — attendi la fine della partita.
			</div>
		{/if}

		{#if over}
			<GameResultOverlay
				result={iWon ? 'win' : 'lose'}
				title={iWon ? 'INDOVINATA' : amEliminated ? 'ELIMINATO' : 'IMPICCATO'}
				message={iWon ? '' : `La parola era: ${over.word.toUpperCase()}`}
				playAgainLabel="Nuova parola"
				onPlayAgain={startGame}
			/>
		{/if}

		<div class="keyboard">
			{#each ALPHABET as l (l)}
				<button
					class="key"
					class:hit={state.guessed.includes(l)}
					class:miss={state.wrong.includes(l)}
					class:vowel={VOWELS.has(l)}
					disabled={!canPlay || blocked(l)}
					onclick={() => tryLetter(l)}
				>
					{l.toUpperCase()}
				</button>
			{/each}
		</div>
	{/if}
</div>

<style>
	/* === Layout principale === */
	.hangman {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
	}

	/* === Patibolo ASCII — verde neon con glow === */
	.gallows {
		font-family: var(--font-term), ui-monospace, monospace;
		font-size: 1.1rem;
		line-height: 1.15;
		color: var(--green);
		text-shadow: var(--glow-cyan);
		background: var(--inset);
		border: 1px solid var(--line);
		padding: 0.75rem 1rem;
		border-radius: 8px;
		margin: 0;
	}

	/* === Parola da indovinare — slot con bordo cyan === */
	.word {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		justify-content: center;
	}

	.slot {
		width: 2rem;
		height: 2.75rem;
		min-width: 44px; /* hit target mobile */
		display: flex;
		align-items: center;
		justify-content: center;
		font-family: var(--font-display), monospace;
		font-size: 1.2rem;
		font-weight: 700;
		text-transform: uppercase;
		color: var(--text);
		border-bottom: 4px solid var(--cyan);
	}

	.slot.filled {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
		border-bottom-color: var(--accent);
	}

	/* === Errori e lettere sbagliate === */
	.errors {
		color: var(--muted);
		margin: 0;
		font-family: var(--font-ui), sans-serif;
		font-size: 0.85rem;
		letter-spacing: 0.05em;
	}

	.wrong {
		color: var(--danger);
		letter-spacing: 0.15em;
		text-shadow: 0 0 6px var(--danger);
	}

	/* === Chip limiti regole === */
	.limits {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		justify-content: center;
		margin: 0;
	}

	.chip {
		background: var(--panel);
		border: 1px solid var(--line);
		color: var(--muted);
		border-radius: 20px;
		padding: 0.25rem 0.7rem;
		font-size: 0.82rem;
		font-family: var(--font-ui), sans-serif;
	}

	.chip.exhausted {
		background: color-mix(in srgb, var(--danger) 20%, var(--inset));
		border-color: var(--danger);
		color: var(--text);
		text-shadow: 0 0 4px var(--danger);
	}

	/* === Indicatore turno === */
	.turn-badge {
		padding: 0.4rem 0.9rem;
		border-radius: 20px;
		font-size: 0.85rem;
		font-family: var(--font-ui), sans-serif;
		font-weight: 600;
		letter-spacing: 0.04em;
		min-height: 44px;
		display: flex;
		align-items: center;
	}

	.turn-badge.my-turn {
		background: color-mix(in srgb, var(--cyan) 15%, var(--inset));
		color: var(--cyan);
		border: 1px solid var(--cyan);
		text-shadow: var(--glow-cyan);
	}

	.turn-badge.wait {
		background: var(--panel);
		color: var(--muted);
		border: 1px solid var(--line);
	}

	/* === Tentativo parola intera === */
	.word-guess {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		justify-content: center;
		width: 100%;
		max-width: 420px;
	}

	.word-input {
		flex: 1 1 200px;
		min-height: 44px;
		padding: 0.5rem 0.8rem;
		border: 1px solid var(--accent);
		border-radius: 6px;
		background: var(--inset);
		color: var(--text);
		font-family: var(--font-ui), sans-serif;
		font-size: 0.95rem;
		text-transform: uppercase;
		letter-spacing: 0.05em;
	}

	.word-input:focus {
		outline: none;
		box-shadow: var(--glow-mag);
	}

	.risk {
		min-height: 44px;
		padding: 0 1.1rem;
		border: 2px solid var(--danger);
		border-radius: 6px;
		background: transparent;
		color: var(--danger);
		font-family: var(--font-ui), sans-serif;
		font-weight: 700;
		letter-spacing: 0.05em;
		cursor: pointer;
		transition: background 0.15s, box-shadow 0.15s;
	}

	.risk:not(:disabled):hover {
		background: color-mix(in srgb, var(--danger) 20%, transparent);
		box-shadow: 0 0 8px color-mix(in srgb, var(--danger) 50%, transparent);
	}

	.risk:disabled {
		opacity: 0.4;
		cursor: not-allowed;
	}

	.hint {
		color: var(--muted);
		font-family: var(--font-ui), sans-serif;
		font-size: 0.78rem;
		margin: 0;
		text-align: center;
	}

	.eliminated-banner {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		padding: 0.5rem 1rem;
		border: 1px solid var(--danger);
		border-radius: 8px;
		background: color-mix(in srgb, var(--danger) 15%, var(--inset));
		color: var(--danger);
		font-family: var(--font-ui), sans-serif;
		font-size: 0.85rem;
		text-shadow: 0 0 6px var(--danger);
	}

	/* === Risultato partita === */
	.result {
		font-family: var(--font-display), monospace;
		font-size: 0.95rem;
		padding: 0.6rem 1rem;
		border-radius: 8px;
		text-align: center;
		letter-spacing: 0.03em;
	}

	.result.won {
		background: color-mix(in srgb, var(--green) 15%, var(--inset));
		color: var(--green);
		border: 1px solid var(--green);
		text-shadow: 0 0 8px var(--green);
	}

	.result.lost {
		background: color-mix(in srgb, var(--danger) 15%, var(--inset));
		color: var(--danger);
		border: 1px solid var(--danger);
		text-shadow: 0 0 8px var(--danger);
	}

	/* === Tasto messaggio iniziale === */
	.muted {
		color: var(--muted);
		font-family: var(--font-ui), sans-serif;
	}

	/* === Tastiera lettere — stile ghost neon === */
	.keyboard {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		justify-content: center;
		max-width: 520px;
	}

	.key {
		width: 2.75rem;
		height: 2.75rem;
		min-width: 44px;
		min-height: 44px;
		border: 1px solid var(--cyan);
		border-radius: 6px;
		background: transparent;
		color: var(--cyan);
		font-family: var(--font-ui), sans-serif;
		font-size: 0.9rem;
		font-weight: 700;
		letter-spacing: 0.03em;
		cursor: pointer;
		transition: background 0.1s, box-shadow 0.1s;
	}

	/* Vocali: bordo magenta per distinguerle */
	.key.vowel {
		border-color: var(--accent);
		color: var(--accent);
	}

	/* Lettera indovinata → verde neon */
	.key.hit {
		background: color-mix(in srgb, var(--green) 20%, var(--inset));
		border-color: var(--green);
		color: var(--green);
		text-shadow: 0 0 6px var(--green);
		box-shadow: 0 0 8px color-mix(in srgb, var(--green) 40%, transparent);
		cursor: default;
	}

	/* Lettera sbagliata → rosso/magenta spento */
	.key.miss {
		background: color-mix(in srgb, var(--danger) 15%, var(--inset));
		border-color: var(--danger);
		color: var(--danger);
		opacity: 0.7;
		cursor: default;
	}

	/* Tasto disabilitato ma non usato (es. turno bloccato) */
	.key:disabled:not(.hit):not(.miss) {
		opacity: 0.35;
		cursor: not-allowed;
	}

	/* Hover solo se abilitato e non già usato */
	@media (hover: hover) {
		.key:not(:disabled):not(.hit):not(.miss):hover {
			background: color-mix(in srgb, var(--cyan) 15%, var(--inset));
			box-shadow: 0 0 8px color-mix(in srgb, var(--cyan) 50%, transparent);
		}
	}

	/* === Pulsante avvio === */
	.start {
		padding: 0.6rem 1.4rem;
		border: 2px solid var(--accent);
		border-radius: 8px;
		background: transparent;
		color: var(--accent);
		font-family: var(--font-ui), sans-serif;
		font-size: 0.95rem;
		font-weight: 700;
		letter-spacing: 0.05em;
		cursor: pointer;
		text-shadow: var(--glow-mag);
		transition: background 0.15s, box-shadow 0.15s;
		min-height: 44px;
	}

	.start:hover {
		background: color-mix(in srgb, var(--accent) 20%, transparent);
		box-shadow: var(--glow-mag);
	}

	/* === Responsive mobile === */
	@media (max-width: 480px) {
		.key { width: 2.5rem; height: 2.5rem; font-size: 0.8rem; }
		.slot { width: 1.6rem; min-width: 36px; font-size: 1rem; }
		.gallows { font-size: 0.9rem; }
	}

	/* === Riduzione movimento per accessibilità === */
	@media (prefers-reduced-motion: reduce) {
		.key,
		.start,
		.risk {
			transition: none;
		}
	}
</style>
