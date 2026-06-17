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
					Sei tu — gioca (<span class="c4-disk-inline {colorClass(myColor)}"></span> {colorLabel(myColor)})
				</p>
			{:else}
				<p class="c4-info c4-wait">
					Aspetta il tuo avversario… (tu sei <span class="c4-disk-inline {colorClass(myColor)}"></span> {colorLabel(myColor)})
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
		color: var(--text, #e2e8f0);
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
		font-size: 1rem;
		color: var(--muted, #94a3b8);
		text-align: center;
	}

	.c4-your-turn { color: var(--text, #e2e8f0); font-weight: 600; }

	.c4-banner {
		font-size: 1.3rem;
		font-weight: 700;
		padding: 0.5rem 1.2rem;
		border-radius: 8px;
		text-align: center;
	}
	.c4-win  { background: #14532d; color: #bbf7d0; }
	.c4-lose { background: #7f1d1d; color: #fecaca; }
	.c4-draw { background: #1e3a5f; color: #bae6fd; }

	.c4-btn {
		padding: 0.55rem 1.3rem;
		border: none;
		border-radius: 8px;
		background: var(--accent, #6366f1);
		color: #fff;
		font-size: 1rem;
		font-weight: 600;
		cursor: pointer;
		transition: opacity 0.15s;
	}
	.c4-btn:hover { opacity: 0.85; }

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
		color: var(--accent, #6366f1);
		font-size: 1.2rem;
		cursor: pointer;
		padding: 0.1rem 0;
		border-radius: 4px;
		transition: background 0.15s;
	}
	.c4-col-btn:hover:not(:disabled) { background: #1e293b; }
	.c4-col-btn:disabled { color: transparent; cursor: default; }
	.c4-col-btn-placeholder { /* same size, invisible */ height: 1.8rem; }

	/* ---- Board ---- */
	.c4-board {
		display: grid;
		grid-template-columns: repeat(var(--cols), 1fr);
		gap: 6px;
		background: var(--panel, #1e293b);
		border: 2px solid #334155;
		border-radius: 12px;
		padding: 10px;
		width: 100%;
		max-width: calc(var(--cols) * 64px + (var(--cols) - 1) * 6px + 20px);
		box-sizing: border-box;
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
		transition: background 0.12s;
	}

	.c4-disk.empty  { background: var(--bg, #0f172a); border: 2px solid #334155; }
	.c4-disk.red    { background: radial-gradient(circle at 35% 35%, #f87171, #dc2626); }
	.c4-disk.yellow { background: radial-gradient(circle at 35% 35%, #fde047, #ca8a04); }

	/* ---- Disco inline (legenda / turno) ---- */
	.c4-disk-inline {
		display: inline-block;
		width: 0.9em;
		height: 0.9em;
		border-radius: 50%;
		vertical-align: middle;
		margin: 0 0.15em;
	}
	.c4-disk-inline.red    { background: #dc2626; }
	.c4-disk-inline.yellow { background: #ca8a04; }
	.c4-disk-inline.empty  { background: #334155; }

	/* ---- Legenda giocatori ---- */
	.c4-legend {
		display: flex;
		gap: 1.5rem;
		flex-wrap: wrap;
		justify-content: center;
		font-size: 0.9rem;
		color: var(--muted, #94a3b8);
	}
	.c4-player { display: flex; align-items: center; gap: 0.3rem; }
	.c4-player.c4-active { color: var(--text, #e2e8f0); font-weight: 600; }
	.c4-arrow { color: var(--accent, #6366f1); font-size: 0.8rem; }

	/* ---- Responsive ---- */
	@media (max-width: 480px) {
		.c4-board {
			gap: 4px;
			padding: 7px;
		}
		.c4-col-buttons {
			gap: 4px;
		}
		.c4-banner { font-size: 1.1rem; }
	}
</style>
