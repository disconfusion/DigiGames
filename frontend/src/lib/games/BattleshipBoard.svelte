<script lang="ts">
	import type { BoardProps } from './board';
	import GameResultOverlay from './GameResultOverlay.svelte';

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
		fleetSize?: number;
		yourSunk?: number;
		enemySunk?: number;
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

	// ── Monitoraggio flotte (derivato dalle board + conteggi affondamenti dal server) ──
	function countCells(board: Cell[][] | undefined, v: string): number {
		if (!board) return 0;
		let n = 0;
		for (const row of board) for (const cell of row) if (cell === v) n++;
		return n;
	}
	const fleetSize = $derived(state?.fleetSize ?? 5);
	const enemySunk = $derived(state?.enemySunk ?? 0); // navi nemiche affondate da te
	const yourSunk = $derived(state?.yourSunk ?? 0); // tue navi affondate
	const myHits = $derived(countCells(state?.enemyBoard, 'X')); // tuoi colpi a segno
	const myMisses = $derived(countCells(state?.enemyBoard, 'O')); // tuoi colpi mancati
	const hitsTaken = $derived(countCells(state?.yourBoard, 'X')); // tue celle colpite

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
		<GameResultOverlay
			result={over.status === 'WON' ? 'win' : 'lose'}
			onPlayAgain={start}
		/>
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
			<div class="board-col own-col">
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
				<div class="stats own-stats">
					<span class="stat" class:danger={yourSunk > 0}>🚢 Affondate {yourSunk}/{fleetSize}</span>
					<span class="stat">🎯 Colpita {hitsTaken}×</span>
				</div>
			</div>

			<div class="board-col enemy-col">
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
				<div class="stats enemy-stats">
					<span class="stat" class:good={enemySunk > 0}>🔥 Affondate {enemySunk}/{fleetSize}</span>
					<span class="stat">🎯 A segno {myHits}</span>
					<span class="stat muted-stat">💧 Mancati {myMisses}</span>
				</div>
			</div>
		</div>
	{/if}
</div>

<style>
	/* ── Contenitore principale ─────────────────────────────────────────────── */
	.battleship {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
		color: var(--text);
		font-family: var(--font-ui, sans-serif);
	}
	.phase-head {
		text-align: center;
	}
	.phase-head h3 {
		margin: 0;
		font-family: var(--font-display, sans-serif);
		font-size: 0.85rem;
		letter-spacing: 0.08em;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}

	/* ── Selettore flotta ───────────────────────────────────────────────────── */
	.fleet-panel {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.4rem;
	}
	.fleet-label {
		font-size: 0.75rem;
		font-family: var(--font-term, monospace);
		letter-spacing: 0.06em;
		color: var(--muted);
	}
	.fleet-ships {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		justify-content: center;
	}
	/* Bottone-nave: bordo cyan inattivo, cyan pieno se selezionato */
	.ship-btn {
		display: flex;
		gap: 2px;
		align-items: center;
		padding: 5px 7px;
		min-height: 44px; /* hit target mobile */
		border: 2px solid var(--line);
		border-radius: 4px;
		background: var(--panel);
		cursor: pointer;
		transition: border-color 0.12s, background 0.12s, opacity 0.12s, box-shadow 0.12s;
	}
	.ship-btn.selected {
		border-color: var(--cyan);
		background: rgba(47, 243, 255, 0.1);
		box-shadow: var(--glow-cyan);
	}
	.ship-btn.placed:not(.selected) {
		opacity: 0.35;
	}
	/* Segmento nave: cyan neutro, vivido se selezionato */
	.seg {
		display: block;
		width: 12px;
		height: 12px;
		border-radius: 2px;
		background: var(--muted);
	}
	.ship-btn.selected .seg {
		background: var(--cyan);
		box-shadow: var(--glow-cyan);
	}

	/* ── Toggle orientamento ────────────────────────────────────────────────── */
	.orient-row {
		display: flex;
		gap: 0.4rem;
	}
	.orient-btn {
		padding: 0.35rem 0.9rem;
		min-height: 44px;
		border: 1px solid var(--line);
		border-radius: 4px;
		background: var(--panel);
		color: var(--muted);
		font-family: var(--font-term, monospace);
		font-size: 1rem;
		cursor: pointer;
		transition: border-color 0.12s, color 0.12s, background 0.12s, box-shadow 0.12s;
	}
	.orient-btn.orient-active {
		border-color: var(--cyan);
		color: var(--cyan);
		background: rgba(47, 243, 255, 0.08);
		box-shadow: var(--glow-cyan);
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
	/* Titolo flotta propria: cyan; nemica: magenta */
	.board-col h4 {
		margin: 0;
		font-family: var(--font-term, monospace);
		font-size: 1.1rem;
		letter-spacing: 0.04em;
		color: var(--muted);
		font-weight: 600;
	}
	.own-col h4 {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.enemy-col h4 {
		color: var(--accent);
		text-shadow: var(--glow-mag);
	}
	/* Griglia propria: bordo cyan */
	.grid {
		display: grid;
		grid-template-columns: repeat(10, 1fr);
		gap: 2px;
		background: var(--bg);
		padding: 4px;
		border-radius: 6px;
		border: 1px solid var(--cyan);
		box-shadow: 0 0 8px rgba(47, 243, 255, 0.18);
		width: min(90vw, 360px);
		aspect-ratio: 1 / 1;
	}
	/* Griglia nemica: bordo magenta */
	.grid.enemy {
		background: var(--inset);
		border-color: var(--accent);
		box-shadow: 0 0 8px rgba(255, 46, 136, 0.18);
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
		border-radius: 2px;
		padding: 0;
		background: #10243a; /* acqua propria */
		transition: background 0.1s, box-shadow 0.1s;
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
	/* Acqua propria */
	.cell.water {
		background: #10243a;
	}
	/* Cella nemica ignota */
	.cell.unknown {
		background: #1a2a44;
	}
	/* Hover cella nemica tiro disponibile */
	@media (hover: hover) {
		button.cell.unknown:not(:disabled):hover {
			background: var(--accent);
			box-shadow: var(--glow-mag);
		}
	}
	/* Nave (flotta propria): blu-acciaio con glow cyan */
	.cell.ship {
		background: #3a6b8c;
		box-shadow: inset 0 0 4px rgba(47, 243, 255, 0.3);
	}
	/* Colpito: magenta + glow — mostra "X" via pseudo-elemento */
	.cell.hit {
		background: var(--accent);
		box-shadow: var(--glow-mag);
		position: relative;
	}
	.cell.hit::after {
		content: 'X';
		position: absolute;
		inset: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		font-family: var(--font-term, monospace);
		font-size: clamp(0.55rem, 1.5vw, 0.85rem);
		color: #fff;
		line-height: 1;
	}
	/* Mancato: grigio scuro con pallino centrale */
	.cell.miss {
		background: #475569;
		position: relative;
	}
	.cell.miss::after {
		content: '';
		position: absolute;
		inset: 38%;
		border-radius: 50%;
		background: var(--bg);
	}
	/* Preview piazzamento: cyan valido, rosso se invalido */
	.cell.preview {
		background: rgba(47, 243, 255, 0.55);
		box-shadow: var(--glow-cyan);
	}
	.cell.preview-invalid {
		background: rgba(255, 82, 119, 0.55);
		box-shadow: 0 0 6px rgba(255, 82, 119, 0.5);
	}

	/* ── Indicatore turno ───────────────────────────────────────────────────── */
	.turn {
		font-family: var(--font-term, monospace);
		font-size: 1.1rem;
		letter-spacing: 0.05em;
		padding: 0.5rem 1.2rem;
		border-radius: 4px;
		border: 1px solid var(--line);
		background: var(--panel);
		color: var(--muted);
		transition: background 0.2s, color 0.2s, box-shadow 0.2s;
	}
	.turn.active {
		background: var(--accent);
		border-color: var(--accent);
		color: #fff;
		box-shadow: var(--glow-mag);
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
		font-family: var(--font-term, monospace);
		font-size: 0.95rem;
		letter-spacing: 0.03em;
	}
	.ok {
		color: var(--green);
		text-shadow: 0 0 6px rgba(61, 255, 154, 0.5);
	}

	/* ── Monitoraggio flotte (affondamenti / colpi) ─────────────────────────── */
	.stats {
		display: flex;
		gap: 0.5rem 0.9rem;
		flex-wrap: wrap;
		justify-content: center;
		font-family: var(--font-term, monospace);
		font-size: 0.95rem;
		letter-spacing: 0.03em;
	}
	.stat {
		color: var(--muted);
		white-space: nowrap;
	}
	.stat.good {
		color: var(--green);
		text-shadow: 0 0 6px rgba(61, 255, 154, 0.45);
	}
	.stat.danger {
		color: var(--danger);
		text-shadow: 0 0 6px rgba(255, 82, 119, 0.45);
	}
	.stat.muted-stat {
		opacity: 0.7;
	}

	/* ── Banner fine partita ────────────────────────────────────────────────── */
	.banner {
		font-family: var(--font-display, sans-serif);
		font-size: 0.9rem;
		letter-spacing: 0.08em;
		padding: 0.8rem 1.5rem;
		border-radius: 6px;
		text-align: center;
		border: 2px solid transparent;
	}
	.banner.won {
		background: rgba(61, 255, 154, 0.12);
		border-color: var(--green);
		color: var(--green);
		text-shadow: 0 0 10px rgba(61, 255, 154, 0.6);
		box-shadow: 0 0 16px rgba(61, 255, 154, 0.2);
	}
	.banner.lost {
		background: rgba(255, 82, 119, 0.12);
		border-color: var(--danger);
		color: var(--danger);
		text-shadow: 0 0 10px rgba(255, 82, 119, 0.6);
		box-shadow: 0 0 16px rgba(255, 82, 119, 0.2);
	}

	/* ── Bottoni ────────────────────────────────────────────────────────────── */
	.primary {
		padding: 0.6rem 1.2rem;
		min-height: 44px;
		border: 2px solid var(--accent);
		border-radius: 4px;
		background: var(--accent);
		color: #fff;
		font-family: var(--font-ui, sans-serif);
		font-size: 0.85rem;
		font-weight: 700;
		letter-spacing: 0.05em;
		cursor: pointer;
		transition: box-shadow 0.15s, opacity 0.15s;
	}
	.primary:not(:disabled):hover {
		box-shadow: var(--glow-mag);
	}
	.primary:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}
	.ghost {
		padding: 0.6rem 1.2rem;
		min-height: 44px;
		border: 1px solid var(--line);
		border-radius: 4px;
		background: var(--panel);
		color: var(--text);
		font-family: var(--font-ui, sans-serif);
		font-size: 0.85rem;
		font-weight: 600;
		letter-spacing: 0.04em;
		cursor: pointer;
		transition: border-color 0.15s, box-shadow 0.15s, opacity 0.15s;
	}
	.ghost:not(:disabled):hover {
		border-color: var(--cyan);
		box-shadow: var(--glow-cyan);
	}
	.ghost:disabled {
		opacity: 0.4;
		cursor: not-allowed;
	}

	/* ── Testo muted generico ───────────────────────────────────────────────── */
	.muted {
		color: var(--muted);
		font-family: var(--font-term, monospace);
	}

	/* ── Rispetto prefers-reduced-motion ────────────────────────────────────── */
	@media (prefers-reduced-motion: reduce) {
		.cell,
		.ship-btn,
		.orient-btn,
		.turn,
		.primary,
		.ghost {
			transition: none;
		}
	}

	@media (max-width: 720px) {
		.boards {
			flex-direction: column;
			align-items: center;
		}
	}
</style>
