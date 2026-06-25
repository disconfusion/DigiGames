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

	// ── Piazzamento manuale ────────────────────────────────────────────────────
	const FLEET = [5, 4, 3, 3, 2]; // deve corrispondere a BattleshipState.FLEET

	type PlacedShip = { r: number; c: number; len: number; horizontal: boolean };

	let localShips = $state<(PlacedShip | null)[]>([null, null, null, null, null]);
	let selectedIdx = $state<number | null>(0); // indice in FLEET della nave da piazzare
	let localHorizontal = $state(true);
	let hoverR = $state(-1);
	let hoverC = $state(-1);
	let localMode = $state(true); // true = griglia interattiva locale; false = griglia confermata dal server

	const fleetConfirmed = $derived(state?.yourBoard?.some((row) => row.some((c) => c === 'S')) ?? false);
	const allLocalPlaced = $derived(localShips.every((s) => s !== null));
	const unplacedCount = $derived(localShips.filter((s) => s === null).length);

	function shipOccupies(ship: PlacedShip, r: number, c: number): boolean {
		for (let i = 0; i < ship.len; i++) {
			const sr = ship.horizontal ? ship.r : ship.r + i;
			const sc = ship.horizontal ? ship.c + i : ship.c;
			if (sr === r && sc === c) return true;
		}
		return false;
	}

	function computeHoverPreview(): Array<{ r: number; c: number }> {
		if (hoverR < 0 || selectedIdx === null) return [];
		const len = FLEET[selectedIdx];
		const cells: Array<{ r: number; c: number }> = [];
		for (let i = 0; i < len; i++) {
			cells.push({
				r: localHorizontal ? hoverR : hoverR + i,
				c: localHorizontal ? hoverC + i : hoverC
			});
		}
		return cells;
	}

	const hoverPreviewCells = $derived(computeHoverPreview());

	const hoverPreviewValid = $derived(
		hoverPreviewCells.length > 0 &&
			hoverPreviewCells.every(
				({ r, c }) =>
					r >= 0 &&
					r < 10 &&
					c >= 0 &&
					c < 10 &&
					localShips.every((ship, si) => si === selectedIdx || !ship || !shipOccupies(ship, r, c))
			)
	);

	function getLocalCellClass(r: number, c: number): string {
		for (const ship of localShips) {
			if (ship && shipOccupies(ship, r, c)) return 'ship';
		}
		if (hoverPreviewCells.some((p) => p.r === r && p.c === c)) {
			return hoverPreviewValid ? 'preview' : 'preview-invalid';
		}
		return 'water';
	}

	function isPlacedCell(r: number, c: number): boolean {
		return localShips.some((ship) => ship && shipOccupies(ship, r, c));
	}

	function clickLocalCell(r: number, c: number) {
		// Riprendi nave già piazzata
		for (let si = 0; si < localShips.length; si++) {
			const ship = localShips[si];
			if (ship && shipOccupies(ship, r, c)) {
				localShips[si] = null;
				selectedIdx = si;
				localHorizontal = ship.horizontal;
				return;
			}
		}
		// Piazza nave selezionata
		if (selectedIdx === null || !hoverPreviewValid) return;
		localShips[selectedIdx] = { r, c, len: FLEET[selectedIdx], horizontal: localHorizontal };
		const next = localShips.findIndex((s, i) => i !== selectedIdx && s === null);
		selectedIdx = next === -1 ? null : next;
	}

	function confirmPlacement() {
		const ships = localShips.filter(Boolean) as PlacedShip[];
		send({ type: 'place', ships });
		localMode = false;
	}

	function editPlacement() {
		localShips = [null, null, null, null, null];
		selectedIdx = 0;
		localMode = true;
	}

	// ── Azioni ────────────────────────────────────────────────────────────────
	const start = () => send({ type: 'game:start' });

	function doRandomize() {
		localShips = [null, null, null, null, null];
		selectedIdx = null;
		localMode = false;
		send({ type: 'randomize' });
	}

	const doReady = () => send({ type: 'ready' });

	function fire(r: number, c: number) {
		if (!state?.yourTurn || over) return;
		if (state.enemyBoard[r]?.[c] !== '?') return;
		send({ type: 'fire', r, c });
	}

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
		<p class="muted">Nessuna partita in corso.</p>
		<button class="primary" onclick={start}>Inizia</button>
	{:else if over}
		<div class="banner" class:won={over.status === 'WON'} class:lost={over.status !== 'WON'}>
			{#if over.status === 'WON'}🎉 Vittoria!{:else}💥 Sconfitta{/if}
		</div>
		<button class="primary" onclick={start}>Nuova partita</button>
	{:else if inPlacement}
		<div class="phase-head">
			<h3>Disponi la tua flotta</h3>
		</div>

		{#if localMode}
			<!-- Piazzamento manuale interattivo -->
			<div class="fleet-panel">
				<span class="fleet-label">Seleziona nave:</span>
				<div class="fleet-ships">
					{#each FLEET as len, i}
						<button
							class="ship-btn"
							class:selected={selectedIdx === i}
							class:placed={localShips[i] !== null}
							onclick={() => (selectedIdx = i)}
							title={`Nave da ${len} celle`}
						>
							{#each { length: len } as _}
								<span class="seg"></span>
							{/each}
						</button>
					{/each}
				</div>
			</div>

			<div class="orient-row">
				<button
					class="orient-btn"
					class:orient-active={localHorizontal}
					onclick={() => (localHorizontal = true)}>↔ Orizz.</button
				>
				<button
					class="orient-btn"
					class:orient-active={!localHorizontal}
					onclick={() => (localHorizontal = false)}>↕ Vert.</button
				>
			</div>

			<div class="grid-wrap">
				<div class="grid own interactive">
					{#each { length: 10 } as _, r}
						{#each { length: 10 } as _, c}
							{@const cls = getLocalCellClass(r, c)}
							{@const pickable = isPlacedCell(r, c)}
							<button
								class="cell {cls}"
								class:pickable
								onmouseenter={() => {
									hoverR = r;
									hoverC = c;
								}}
								onmouseleave={() => {
									hoverR = -1;
									hoverC = -1;
								}}
								onclick={() => clickLocalCell(r, c)}
								aria-label={`Cella ${r},${c}`}
							></button>
						{/each}
					{/each}
				</div>
			</div>

			<div class="actions">
				<button class="ghost" onclick={doRandomize}>🎲 A caso</button>
				<button class="primary" disabled={!allLocalPlaced} onclick={confirmPlacement}>
					{allLocalPlaced ? '✓ Conferma disposizione' : `Mancano ${unplacedCount} navi`}
				</button>
			</div>

			<p class="hint">
				{#if selectedIdx !== null}
					Clicca griglia per posizionare • Clicca nave piazzata per spostarla
				{:else if allLocalPlaced}
					Tutte le navi piazzate. Premi Conferma.
				{:else}
					Seleziona una nave dalla lista
				{/if}
			</p>
		{:else}
			<!-- Flotta confermata dal server -->
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
				<button class="ghost" onclick={editPlacement} disabled={state.youReady}>✏️ Modifica</button>
				<button class="ghost" onclick={doRandomize} disabled={state.youReady}>🎲 A caso</button>
				<button class="primary" onclick={doReady} disabled={state.youReady || !fleetConfirmed}>
					{state.youReady ? 'Pronto ✓' : 'Pronto'}
				</button>
			</div>

			<p class="hint">
				{#if state.youReady}
					In attesa dell'avversario…
				{:else if fleetConfirmed}
					Flotta posizionata. Premi "Pronto" quando sei sicuro.
				{:else}
					Posizionamento in corso…
				{/if}
				{#if state.enemyReady}
					<span class="ok">Avversario pronto ✓</span>
				{:else}
					<span class="muted">Avversario non ancora pronto.</span>
				{/if}
			</p>
		{/if}
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

	/* ── Selettore flotta ───────────────────────────────────────────────────── */
	.fleet-panel {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.4rem;
	}
	.fleet-label {
		font-size: 0.82rem;
		color: var(--muted);
	}
	.fleet-ships {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		justify-content: center;
	}
	.ship-btn {
		display: flex;
		gap: 2px;
		align-items: center;
		padding: 5px 7px;
		border: 2px solid #334155;
		border-radius: 6px;
		background: var(--panel);
		cursor: pointer;
		transition: border-color 0.12s, background 0.12s, opacity 0.12s;
	}
	.ship-btn.selected {
		border-color: var(--accent);
		background: rgba(59, 130, 246, 0.15);
	}
	.ship-btn.placed:not(.selected) {
		opacity: 0.35;
	}
	.seg {
		display: block;
		width: 12px;
		height: 12px;
		border-radius: 2px;
		background: #64748b;
	}
	.ship-btn.selected .seg {
		background: var(--accent);
	}

	/* ── Toggle orientamento ────────────────────────────────────────────────── */
	.orient-row {
		display: flex;
		gap: 0.4rem;
	}
	.orient-btn {
		padding: 0.25rem 0.75rem;
		border: 1px solid #334155;
		border-radius: 6px;
		background: var(--panel);
		color: var(--muted);
		font-size: 0.9rem;
		cursor: pointer;
		transition: border-color 0.12s, color 0.12s, background 0.12s;
	}
	.orient-btn.orient-active {
		border-color: var(--accent);
		color: var(--accent);
		background: rgba(59, 130, 246, 0.1);
	}

	/* ── Griglie ────────────────────────────────────────────────────────────── */
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
		width: min(90vw, 360px);
		aspect-ratio: 1 / 1;
	}
	.grid.enemy {
		background: #111a2e;
	}
	/* Cursore default sulla griglia propria */
	.grid.own .cell {
		cursor: default;
	}
	/* Griglia interattiva: crosshair per posizionare, pointer per riprendere navi */
	.grid.interactive .cell {
		cursor: crosshair;
	}
	.grid.interactive .cell.pickable {
		cursor: pointer;
	}

	/* ── Celle ──────────────────────────────────────────────────────────────── */
	.cell {
		width: 100%;
		aspect-ratio: 1 / 1;
		border: none;
		border-radius: 3px;
		padding: 0;
		background: #1e293b;
	}
	div.cell {
		cursor: default;
	}
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
	/* Preview piazzamento manuale */
	.cell.preview {
		background: rgba(59, 130, 246, 0.65);
	}
	.cell.preview-invalid {
		background: rgba(220, 38, 38, 0.55);
	}

	/* ── Indicatore turno ───────────────────────────────────────────────────── */
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

	/* ── Azioni e hint ──────────────────────────────────────────────────────── */
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
		font-size: 0.88rem;
	}
	.ok {
		color: #4ade80;
	}

	/* ── Banner fine partita ────────────────────────────────────────────────── */
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

	/* ── Bottoni ────────────────────────────────────────────────────────────── */
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
		.boards {
			flex-direction: column;
			align-items: center;
		}
	}
</style>
