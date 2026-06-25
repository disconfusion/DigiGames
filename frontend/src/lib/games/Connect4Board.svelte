<script lang="ts">
	import type { BoardProps } from './board';

	let { send, event, me }: BoardProps = $props();

	// ---- Tipi ----
	type GameStatus = 'PLAYING' | 'WON' | 'DRAW';

	interface GameState {
		board: (string | null)[][];   // 6 righe x 7 colonne, null | "R" | "Y"
		currentTurn: string | null;   // email del giocatore di turno
		seats: Record<string, string>; // email → "R" | "Y"
		status: GameStatus;
		winner: string | null;
	}

	// ---- Stato locale ----
	let state = $state<GameState | null>(null);
	let over  = $state<{ status: GameStatus; winner: string | null } | null>(null);

	// ---- Reazione agli eventi WebSocket ----
	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			state = e as unknown as GameState;
			if ((e as { status?: string }).status === 'PLAYING') over = null;
		} else if (e.type === 'game:over') {
			over = {
				status: String((e as { status?: string }).status) as GameStatus,
				winner: (e as { winner?: string | null }).winner ?? null,
			};
		}
	});

	// ---- Derivati ----
	const myColor   = $derived(state?.seats?.[me.username] ?? null);
	const isMyTurn  = $derived(state?.currentTurn === me.username);
	const isPlaying = $derived(state?.status === 'PLAYING');

	// ---- Azioni ----
	function drop(col: number) {
		if (!isPlaying || !isMyTurn) return;
		send({ type: 'move', col });
	}

	function startGame() {
		send({ type: 'game:start' });
	}

	// ---- Helpers colore ----
	function colorClass(cell: string | null): string {
		if (cell === 'R') return 'red';
		if (cell === 'Y') return 'yellow';
		return 'empty';
	}

	function colorLabel(color: string | null): string {
		if (color === 'R') return 'Rosso';
		if (color === 'Y') return 'Giallo';
		return '';
	}

	const ROWS = 6;
	const COLS = 7;
	const EMPTY_BOARD: (null)[][] = Array.from({ length: ROWS }, () => Array(COLS).fill(null));

	const displayBoard = $derived(state?.board ?? EMPTY_BOARD);
</script>

<div class="c4-wrap">

	<!-- Intestazione: stato del gioco -->
	<div class="c4-header">
		{#if !state}
			<p class="c4-info">In attesa del secondo giocatore…</p>
			<button class="c4-btn" onclick={startGame}>Inizia partita</button>
		{:else if over}
			{#if over.status === 'WON'}
				{#if over.winner === me.username}
					<div class="c4-banner c4-win">Hai vinto!</div>
				{:else}
					<div class="c4-banner c4-lose">Hai perso!</div>
				{/if}
			{:else}
				<div class="c4-banner c4-draw">Pareggio!</div>
			{/if}
			<button class="c4-btn" onclick={startGame}>Nuova partita</button>
		{:else if isPlaying}
			{#if isMyTurn}
				<p class="c4-info c4-your-turn">
					▶ TOCCA A TE (<span class="c4-disk-inline {colorClass(myColor)}"></span> {colorLabel(myColor)})
				</p>
			{:else}
				<p class="c4-info c4-wait">
					Aspetta l'avversario… (tu sei <span class="c4-disk-inline {colorClass(myColor)}"></span> {colorLabel(myColor)})
				</p>
			{/if}
		{/if}
	</div>

	<!-- Griglia: frecce clic colonne -->
	{#if state && !over}
		<div class="c4-col-buttons" style="--cols: {COLS}">
			{#each Array(COLS) as _, col (col)}
				<button
					class="c4-col-btn"
					onclick={() => drop(col)}
					disabled={!isPlaying || !isMyTurn}
					aria-label="Inserisci in colonna {col + 1}"
				>▼</button>
			{/each}
		</div>
	{:else}
		<div class="c4-col-buttons" style="--cols: {COLS}">
			{#each Array(COLS) as _, col (col)}
				<div class="c4-col-btn-placeholder"></div>
			{/each}
		</div>
	{/if}

	<!-- Board -->
	<div class="c4-board" style="--cols: {COLS}; --rows: {ROWS}">
		{#each displayBoard as row, r (r)}
			{#each row as cell, c (c)}
				<button
					class="c4-cell"
					onclick={() => drop(c)}
					disabled={!isPlaying || !isMyTurn || !!over}
					aria-label="Riga {r + 1} colonna {c + 1}{cell ? ': ' + colorLabel(cell) : ''}"
				>
					<span class="c4-disk {colorClass(cell)}"></span>
				</button>
			{/each}
		{/each}
	</div>

	<!-- Legenda giocatori -->
	{#if state}
		<div class="c4-legend">
			{#each Object.entries(state.seats) as [uname, color] (uname)}
				<span class="c4-player" class:c4-active={state.currentTurn === uname && isPlaying}>
					<span class="c4-disk-inline {colorClass(color)}"></span>
					{uname === me.username ? 'Tu' : uname}
					{#if state.currentTurn === uname && isPlaying}<span class="c4-arrow">◄</span>{/if}
				</span>
			{/each}
		</div>
	{/if}
</div>

<style>
	/* ---- Layout ---- */
	.c4-wrap {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.75rem;
		padding: 1rem;
		width: 100%;
		box-sizing: border-box;
		color: var(--text);
	}

	/* ---- Intestazione ---- */
	.c4-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
		min-height: 3rem;
	}

	.c4-info {
		margin: 0;
		font-family: var(--font-term);
		font-size: 1.15rem;
		color: var(--muted);
		text-align: center;
	}

	/* "▶ TOCCA A TE" — font display ambra con glow */
	.c4-your-turn {
		font-family: var(--font-display);
		font-size: 0.7rem;
		color: var(--amber);
		text-shadow: 0 0 8px var(--amber), 0 0 16px var(--amber);
		letter-spacing: 0.04em;
		text-transform: uppercase;
		animation: c4-pulse 1.4s ease-in-out infinite;
	}

	@keyframes c4-pulse {
		0%, 100% { opacity: 1; }
		50%       { opacity: 0.65; }
	}

	@media (prefers-reduced-motion: reduce) {
		.c4-your-turn { animation: none; }
	}

	/* Banner esito */
	.c4-banner {
		font-family: var(--font-display);
		font-size: 0.9rem;
		padding: 0.55rem 1.4rem;
		border-radius: 6px;
		text-align: center;
		letter-spacing: 0.05em;
		text-transform: uppercase;
	}
	.c4-win {
		background: #0d2b1a;
		color: var(--green);
		border: 1px solid var(--green);
		text-shadow: 0 0 10px var(--green);
		box-shadow: 0 0 14px rgba(61, 255, 154, 0.3);
	}
	.c4-lose {
		background: #2b0d15;
		color: var(--danger);
		border: 1px solid var(--danger);
		text-shadow: 0 0 10px var(--danger);
		box-shadow: 0 0 14px rgba(255, 82, 119, 0.3);
	}
	.c4-draw {
		background: var(--inset);
		color: var(--cyan);
		border: 1px solid var(--cyan);
		text-shadow: 0 0 10px var(--cyan);
		box-shadow: 0 0 14px rgba(47, 243, 255, 0.25);
	}

	/* Bottone azione */
	.c4-btn {
		padding: 0.6rem 1.4rem;
		min-height: 44px;
		border: 2px solid var(--accent);
		border-radius: 6px;
		background: transparent;
		color: var(--accent);
		font-family: var(--font-ui);
		font-size: 0.75rem;
		font-weight: 700;
		letter-spacing: 0.1em;
		text-transform: uppercase;
		cursor: pointer;
		box-shadow: 0 0 10px rgba(255, 46, 136, 0.35), inset 0 0 8px rgba(255, 46, 136, 0.08);
		transition: box-shadow 0.2s, background 0.2s;
	}
	.c4-btn:hover {
		background: rgba(255, 46, 136, 0.12);
		box-shadow: var(--glow-mag);
	}

	/* ---- Frecce colonna ---- */
	.c4-col-buttons {
		display: grid;
		grid-template-columns: repeat(var(--cols), 1fr);
		gap: 4px;
		width: 100%;
		max-width: calc(var(--cols) * 64px + (var(--cols) - 1) * 4px);
	}

	.c4-col-btn {
		background: transparent;
		border: none;
		color: var(--cyan);
		font-size: 1.2rem;
		cursor: pointer;
		padding: 0.1rem 0;
		min-height: 44px;
		border-radius: 4px;
		text-shadow: 0 0 6px var(--cyan);
		transition: color 0.15s, text-shadow 0.15s;
	}
	.c4-col-btn:hover:not(:disabled) {
		color: var(--accent);
		text-shadow: var(--glow-mag);
	}
	.c4-col-btn:disabled { color: transparent; cursor: default; }
	.c4-col-btn-placeholder { height: 44px; }

	/* ---- Board ---- */
	.c4-board {
		display: grid;
		grid-template-columns: repeat(var(--cols), 1fr);
		gap: 6px;
		background: linear-gradient(180deg, #1d2bb0, #142080);
		border: 3px solid var(--cyan);
		border-radius: 12px;
		padding: 10px;
		width: 100%;
		max-width: calc(var(--cols) * 64px + (var(--cols) - 1) * 6px + 20px);
		box-sizing: border-box;
		box-shadow: 0 0 26px rgba(47, 243, 255, 0.27);
	}

	.c4-cell {
		background: transparent;
		border: none;
		padding: 0;
		cursor: pointer;
		border-radius: 50%;
		aspect-ratio: 1;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.c4-cell:disabled { cursor: default; }

	/* ---- Dischi ---- */
	.c4-disk {
		width: 100%;
		height: 100%;
		border-radius: 50%;
		display: block;
		transition: box-shadow 0.15s;
	}

	/* Foro vuoto: fondo --bg con inset shadow */
	.c4-disk.empty {
		background: var(--bg);
		box-shadow: inset 0 0 8px #000;
	}

	/* P1 = --accent (magenta), P2 = --amber */
	.c4-disk.red {
		background: radial-gradient(circle at 35% 35%, #ff6eb0, var(--accent));
		box-shadow: 0 0 12px var(--accent);
	}
	.c4-disk.yellow {
		background: radial-gradient(circle at 35% 35%, #ffe080, var(--amber));
		box-shadow: 0 0 12px var(--amber);
	}

	/* ---- Disco inline (legenda / turno) ---- */
	.c4-disk-inline {
		display: inline-block;
		width: 0.9em;
		height: 0.9em;
		border-radius: 50%;
		vertical-align: middle;
		margin: 0 0.15em;
	}
	.c4-disk-inline.red    { background: var(--accent); box-shadow: 0 0 6px var(--accent); }
	.c4-disk-inline.yellow { background: var(--amber);  box-shadow: 0 0 6px var(--amber); }
	.c4-disk-inline.empty  { background: var(--line); }

	/* ---- Legenda giocatori ---- */
	.c4-legend {
		display: flex;
		gap: 1.5rem;
		flex-wrap: wrap;
		justify-content: center;
		font-family: var(--font-term);
		font-size: 1.05rem;
		color: var(--muted);
	}
	.c4-player { display: flex; align-items: center; gap: 0.3rem; }
	.c4-player.c4-active { color: var(--text); }
	.c4-arrow {
		color: var(--cyan);
		font-size: 0.85rem;
		text-shadow: 0 0 6px var(--cyan);
	}

	/* ---- Responsive ---- */
	@media (max-width: 480px) {
		.c4-board {
			gap: 4px;
			padding: 7px;
		}
		.c4-col-buttons {
			gap: 4px;
		}
		.c4-banner { font-size: 0.7rem; }
	}
</style>
