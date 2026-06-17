<script lang="ts">
	import type { BoardProps } from './board';

	let { send, event, me }: BoardProps = $props();

	type Cell = { color: 'W' | 'B'; king: boolean } | null;
	type GameStatus = 'PLAYING' | 'WON';

	interface GameState {
		board: Cell[][];
		currentTurn: string | null;
		seats: Record<string, string>;
		status: GameStatus;
		winner: string | null;
		mustContinue: number[] | null;
	}

	let state = $state<GameState | null>(null);
	let over = $state<{ winner: string | null } | null>(null);
	let sel = $state<{ r: number; c: number } | null>(null);

	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			state = e as unknown as GameState;
			sel = null;
			if ((e as { status?: string }).status === 'PLAYING') over = null;
		} else if (e.type === 'game:over') {
			over = { winner: (e as { winner?: string | null }).winner ?? null };
		}
	});

	const myColor = $derived(state?.seats?.[me.username] ?? null);
	const isMyTurn = $derived(state?.currentTurn === me.username);
	const isPlaying = $derived(state?.status === 'PLAYING');
	const mustPos = $derived(state?.mustContinue ?? null);

	function cellAt(r: number, c: number): Cell {
		return state?.board?.[r]?.[c] ?? null;
	}

	function mine(cell: Cell): boolean {
		return !!cell && cell.color === myColor;
	}

	function clickCell(r: number, c: number) {
		if (!isPlaying || !isMyTurn || !state) return;
		const cell = cellAt(r, c);
		// Se devo continuare la cattura, posso selezionare solo quel pezzo
		if (mustPos && (mustPos[0] !== r || mustPos[1] !== c) && mine(cell)) return;

		if (mine(cell)) {
			sel = { r, c };
			return;
		}
		if (sel && !cell) {
			send({ type: 'move', from: { r: sel.r, c: sel.c }, to: { r, c } });
			sel = null;
		}
	}

	const startGame = () => send({ type: 'game:start' });

	const ROWS = [0, 1, 2, 3, 4, 5, 6, 7];
</script>

<div class="dama">
	<div class="header">
		{#if !state}
			<p class="info">In attesa del secondo giocatore…</p>
			<button class="btn" onclick={startGame}>Inizia partita</button>
		{:else if over}
			{#if over.winner === me.username}
				<div class="banner win">Hai vinto!</div>
			{:else}
				<div class="banner lose">Hai perso!</div>
			{/if}
			<button class="btn" onclick={startGame}>Nuova partita</button>
		{:else if isPlaying}
			{#if isMyTurn}
				<p class="info turn">
					Tocca a te ({myColor === 'W' ? 'bianco' : 'nero'}){#if mustPos}
						— continua la cattura!{/if}
				</p>
			{:else}
				<p class="info wait">Aspetta l'avversario…</p>
			{/if}
		{/if}
	</div>

	{#if state}
		<div class="grid">
			{#each ROWS as r (r)}
				{#each ROWS as c (c)}
					{@const dark = (r + c) % 2 === 1}
					{@const cell = cellAt(r, c)}
					<button
						class="sq"
						class:dark
						class:light={!dark}
						class:selected={sel?.r === r && sel?.c === c}
						class:must={mustPos && mustPos[0] === r && mustPos[1] === c}
						disabled={!dark || !isPlaying || !isMyTurn}
						onclick={() => clickCell(r, c)}
						aria-label="Casa {r},{c}"
					>
						{#if cell}
							<span class="piece" class:white={cell.color === 'W'} class:black={cell.color === 'B'}>
								{cell.king ? '♛' : ''}
							</span>
						{/if}
					</button>
				{/each}
			{/each}
		</div>

		<div class="legend">
			{#each Object.entries(state.seats) as [uname, color] (uname)}
				<span class="player" class:active={state.currentTurn === uname && isPlaying}>
					<span class="dot" class:white={color === 'W'} class:black={color === 'B'}></span>
					{uname === me.username ? 'Tu' : uname}
				</span>
			{/each}
		</div>
	{/if}
</div>

<style>
	.dama {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
		padding: 1rem;
	}
	.header {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
		min-height: 3rem;
	}
	.info {
		margin: 0;
		color: var(--muted);
		text-align: center;
	}
	.turn {
		color: var(--text);
		font-weight: 600;
	}
	.banner {
		font-size: 1.3rem;
		font-weight: 700;
		padding: 0.5rem 1.2rem;
		border-radius: 8px;
	}
	.win {
		background: #14532d;
		color: #bbf7d0;
	}
	.lose {
		background: #7f1d1d;
		color: #fecaca;
	}
	.btn {
		padding: 0.55rem 1.3rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: #fff;
		font-weight: 600;
		cursor: pointer;
	}
	.grid {
		display: grid;
		grid-template-columns: repeat(8, 1fr);
		width: min(92vw, 480px);
		aspect-ratio: 1;
		border: 3px solid #1e293b;
		border-radius: 6px;
		overflow: hidden;
	}
	.sq {
		border: none;
		padding: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		aspect-ratio: 1;
	}
	.sq.light {
		background: #e7d3b1;
	}
	.sq.dark {
		background: #7c5a3a;
	}
	.sq:disabled {
		cursor: default;
	}
	.sq.selected {
		outline: 3px solid #6366f1;
		outline-offset: -3px;
	}
	.sq.must {
		outline: 3px solid #f59e0b;
		outline-offset: -3px;
	}
	.piece {
		width: 72%;
		height: 72%;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 1.1rem;
		line-height: 1;
	}
	.piece.white {
		background: radial-gradient(circle at 35% 35%, #fefefe, #cbd5e1);
		border: 2px solid #94a3b8;
		color: #b45309;
	}
	.piece.black {
		background: radial-gradient(circle at 35% 35%, #475569, #1e293b);
		border: 2px solid #0f172a;
		color: #fbbf24;
	}
	.legend {
		display: flex;
		gap: 1.5rem;
		flex-wrap: wrap;
		justify-content: center;
		font-size: 0.9rem;
		color: var(--muted);
	}
	.player {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}
	.player.active {
		color: var(--text);
		font-weight: 600;
	}
	.dot {
		width: 0.9rem;
		height: 0.9rem;
		border-radius: 50%;
	}
	.dot.white {
		background: #e2e8f0;
	}
	.dot.black {
		background: #334155;
	}
</style>
