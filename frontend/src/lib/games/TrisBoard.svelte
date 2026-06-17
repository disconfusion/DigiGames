<script lang="ts">
	import type { BoardProps } from './board';

	let { send, event, me }: BoardProps = $props();

	type GameStatus = 'PLAYING' | 'WON' | 'DRAW';

	interface GameState {
		board: (string | null)[][]; // 3x3, null | "X" | "O"
		currentTurn: string | null;
		seats: Record<string, string>; // email -> "X" | "O"
		status: GameStatus;
		winner: string | null;
	}

	let state = $state<GameState | null>(null);
	let over = $state<{ status: GameStatus; winner: string | null } | null>(null);

	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			state = e as unknown as GameState;
			if ((e as { status?: string }).status === 'PLAYING') over = null;
		} else if (e.type === 'game:over') {
			over = {
				status: String((e as { status?: string }).status) as GameStatus,
				winner: (e as { winner?: string | null }).winner ?? null
			};
		}
	});

	const myMark = $derived(state?.seats?.[me.username] ?? null);
	const isMyTurn = $derived(state?.currentTurn === me.username);
	const isPlaying = $derived(state?.status === 'PLAYING');

	const flat = $derived(state?.board ? state.board.flat() : Array(9).fill(null));

	function play(pos: number) {
		if (!isPlaying || !isMyTurn) return;
		if (flat[pos]) return;
		send({ type: 'move', pos });
	}

	const startGame = () => send({ type: 'game:start' });
</script>

<div class="tris">
	<div class="header">
		{#if !state}
			<p class="info">In attesa del secondo giocatore…</p>
			<button class="btn" onclick={startGame}>Inizia partita</button>
		{:else if over}
			{#if over.status === 'WON'}
				{#if over.winner === me.username}
					<div class="banner win">Hai vinto!</div>
				{:else}
					<div class="banner lose">Hai perso!</div>
				{/if}
			{:else}
				<div class="banner draw">Pareggio!</div>
			{/if}
			<button class="btn" onclick={startGame}>Nuova partita</button>
		{:else if isPlaying}
			{#if isMyTurn}
				<p class="info turn">Tocca a te — sei <strong>{myMark}</strong></p>
			{:else}
				<p class="info wait">Aspetta l'avversario (tu sei <strong>{myMark}</strong>)</p>
			{/if}
		{/if}
	</div>

	<div class="grid">
		{#each flat as cell, pos (pos)}
			<button
				class="cell"
				class:x={cell === 'X'}
				class:o={cell === 'O'}
				disabled={!isPlaying || !isMyTurn || !!cell || !!over}
				onclick={() => play(pos)}
				aria-label="Casella {pos + 1}{cell ? ': ' + cell : ''}"
			>
				{cell ?? ''}
			</button>
		{/each}
	</div>

	{#if state}
		<div class="legend">
			{#each Object.entries(state.seats) as [uname, mark] (uname)}
				<span class="player" class:active={state.currentTurn === uname && isPlaying}>
					<strong>{mark}</strong>
					{uname === me.username ? 'Tu' : uname}
				</span>
			{/each}
		</div>
	{/if}
</div>

<style>
	.tris {
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
	.draw {
		background: #1e3a5f;
		color: #bae6fd;
	}
	.btn {
		padding: 0.55rem 1.3rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: #fff;
		font-size: 1rem;
		font-weight: 600;
		cursor: pointer;
	}
	.grid {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 8px;
		background: #334155;
		padding: 8px;
		border-radius: 12px;
	}
	.cell {
		width: 5.5rem;
		height: 5.5rem;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 3rem;
		font-weight: 800;
		background: var(--bg);
		border: none;
		border-radius: 8px;
		cursor: pointer;
		color: var(--text);
		line-height: 1;
	}
	.cell:disabled {
		cursor: default;
	}
	.cell.x {
		color: #60a5fa;
	}
	.cell.o {
		color: #f87171;
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
		gap: 0.35rem;
	}
	.player.active {
		color: var(--text);
		font-weight: 600;
	}
	@media (max-width: 480px) {
		.cell {
			width: 4.2rem;
			height: 4.2rem;
			font-size: 2.4rem;
		}
	}
</style>
