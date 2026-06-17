<script lang="ts">
	import type { BoardProps } from './board';

	let { send, event, me }: BoardProps = $props();

	type Phase = 'PLACEMENT' | 'BATTLE';
	type Cell = string; // "~" | "S" | "X" | "O" | "?"

	type BattleshipState = {
		phase: Phase;
		status: 'PLAYING' | 'WON' | 'LOST';
		yourTurn: boolean;
		youReady: boolean;
		enemyReady: boolean;
		winner: string | null;
		yourBoard: Cell[][];
		enemyBoard: Cell[][];
	};

	let state = $state<BattleshipState | null>(null);
	let over = $state<{ status: string; winner: string | null } | null>(null);

	// Reagisce agli snapshot personalizzati ricevuti dal server (l'ultimo è autoritativo).
	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			state = e as unknown as BattleshipState;
			if ((e as { status?: string }).status === 'PLAYING') over = null;
		} else if (e.type === 'game:over') {
			over = { status: String(e.status), winner: (e.winner as string) ?? null };
		}
	});

	const phase = $derived(state?.phase ?? null);
	const inBattle = $derived(phase === 'BATTLE');
	const inPlacement = $derived(phase === 'PLACEMENT');

	const start = () => send({ type: 'game:start' });
	const randomize = () => send({ type: 'randomize' });
	const ready = () => send({ type: 'ready' });

	function fire(r: number, c: number) {
		if (!state?.yourTurn || over) return;
		if (state.enemyBoard[r]?.[c] !== '?') return; // già colpita o non sparabile
		send({ type: 'fire', r, c });
	}

	// Etichette di stato testuali per accessibilità / chiarezza.
	function cellTitle(cell: Cell, enemy: boolean): string {
		if (enemy) {
			if (cell === 'X') return 'Colpito';
			if (cell === 'O') return 'Mancato';
			return 'Sconosciuto';
		}
		if (cell === 'S') return 'Nave';
		if (cell === 'X') return 'Nave colpita';
		if (cell === 'O') return 'Colpo mancato';
		return 'Acqua';
	}
</script>

<div class="battleship">
	{#if !state}
		<!-- Nessuna partita: il server valida che ci siano 2 giocatori. -->
		<p class="muted">Nessuna partita in corso.</p>
		<button class="primary" onclick={start}>Inizia</button>
	{:else if over}
		<!-- Fine partita -->
		<div class="banner" class:won={over.status === 'WON'} class:lost={over.status !== 'WON'}>
			{#if over.status === 'WON'}🎉 Vittoria!{:else}💥 Sconfitta{/if}
		</div>
		<button class="primary" onclick={start}>Nuova partita</button>
	{:else if inPlacement}
		<!-- Fase di piazzamento -->
		<div class="phase-head">
			<h3>Disponi la tua flotta</h3>
			<p class="muted">Navi: 5, 4, 3, 3, 2</p>
		</div>

		<div class="grid-wrap">
			<div class="grid own">
				{#each state.yourBoard as row, r (r)}
					{#each row as cell, c (c)}
						<div
							class="cell {cell === 'S' ? 'ship' : cell === 'X' ? 'hit' : cell === 'O' ? 'miss' : 'water'}"
							title={cellTitle(cell, false)}
						></div>
					{/each}
				{/each}
			</div>
		</div>

		<div class="actions">
			<button class="ghost" onclick={randomize} disabled={state.youReady}>Disponi a caso</button>
			<button class="primary" onclick={ready} disabled={state.youReady}>
				{state.youReady ? 'Pronto ✓' : 'Pronto'}
			</button>
		</div>

		<p class="hint">
			{#if state.youReady}
				In attesa dell'avversario…
			{:else}
				Premi "Disponi a caso" e poi "Pronto".
			{/if}
			{#if state.enemyReady}
				<span class="ok">Avversario pronto ✓</span>
			{:else}
				<span class="muted">Avversario non ancora pronto.</span>
			{/if}
		</p>
	{:else if inBattle}
		<!-- Fase di battaglia -->
		<div class="turn" class:active={state.yourTurn}>
			{state.yourTurn ? '🎯 Tocca a te' : '⏳ Turno avversario'}
		</div>

		<div class="boards">
			<div class="board-col">
				<h4>La tua flotta</h4>
				<div class="grid own">
					{#each state.yourBoard as row, r (r)}
						{#each row as cell, c (c)}
							<div
								class="cell {cell === 'S' ? 'ship' : cell === 'X' ? 'hit' : cell === 'O' ? 'miss' : 'water'}"
								title={cellTitle(cell, false)}
							></div>
						{/each}
					{/each}
				</div>
			</div>

			<div class="board-col">
				<h4>Avversario</h4>
				<div class="grid enemy">
					{#each state.enemyBoard as row, r (r)}
						{#each row as cell, c (c)}
							<button
								class="cell {cell === 'X' ? 'hit' : cell === 'O' ? 'miss' : 'unknown'}"
								disabled={cell !== '?' || !state.yourTurn}
								onclick={() => fire(r, c)}
								title={cellTitle(cell, true)}
								aria-label={`Spara in ${r},${c}`}
							></button>
						{/each}
					{/each}
				</div>
			</div>
		</div>
	{/if}
</div>

<style>
	.battleship {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
		color: var(--text);
	}
	.phase-head {
		text-align: center;
	}
	.phase-head h3 {
		margin: 0;
	}
	.boards {
		display: flex;
		flex-wrap: wrap;
		gap: 1.5rem;
		justify-content: center;
		width: 100%;
	}
	.board-col {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
	}
	.board-col h4 {
		margin: 0;
		color: var(--muted);
		font-weight: 600;
	}
	.grid {
		display: grid;
		grid-template-columns: repeat(10, 1fr);
		gap: 2px;
		background: #0b1220;
		padding: 4px;
		border-radius: 8px;
		/* Responsive: la griglia si adatta alla larghezza disponibile. */
		width: min(90vw, 360px);
		aspect-ratio: 1 / 1;
	}
	.grid.enemy {
		background: #111a2e;
	}
	.cell {
		width: 100%;
		aspect-ratio: 1 / 1;
		border: none;
		border-radius: 3px;
		padding: 0;
		background: #1e293b;
	}
	/* Celle non interattive (board propria) */
	div.cell {
		cursor: default;
	}
	/* Celle interattive (board nemica) */
	button.cell {
		cursor: pointer;
	}
	button.cell:disabled {
		cursor: not-allowed;
	}
	.cell.water {
		background: #1e293b;
	}
	.cell.unknown {
		background: #243a5e;
	}
	button.cell.unknown:not(:disabled):hover {
		background: var(--accent);
	}
	.cell.ship {
		background: #64748b;
	}
	.cell.hit {
		background: #dc2626;
	}
	.cell.miss {
		background: #475569;
		position: relative;
	}
	.cell.miss::after {
		content: '';
		position: absolute;
		inset: 38%;
		border-radius: 50%;
		background: #0b1220;
	}
	.turn {
		font-size: 1.05rem;
		font-weight: 700;
		padding: 0.5rem 1rem;
		border-radius: 8px;
		background: var(--panel);
		color: var(--muted);
	}
	.turn.active {
		background: var(--accent);
		color: white;
	}
	.actions {
		display: flex;
		gap: 0.75rem;
		flex-wrap: wrap;
		justify-content: center;
	}
	.hint {
		color: var(--muted);
		text-align: center;
		margin: 0;
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		justify-content: center;
	}
	.ok {
		color: #4ade80;
	}
	.banner {
		font-size: 1.3rem;
		font-weight: 700;
		padding: 0.8rem 1.5rem;
		border-radius: 8px;
		text-align: center;
	}
	.banner.won {
		background: #14532d;
		color: #bbf7d0;
	}
	.banner.lost {
		background: #7f1d1d;
		color: #fecaca;
	}
	.primary {
		padding: 0.6rem 1.2rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: white;
		font-size: 1rem;
		font-weight: 600;
		cursor: pointer;
	}
	.primary:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}
	.ghost {
		padding: 0.6rem 1.2rem;
		border: 1px solid #334155;
		border-radius: 8px;
		background: var(--panel);
		color: var(--text);
		font-size: 1rem;
		font-weight: 600;
		cursor: pointer;
	}
	.ghost:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}
	@media (max-width: 720px) {
		/* Su mobile le due griglie si impilano. */
		.boards {
			flex-direction: column;
			align-items: center;
		}
	}
</style>
