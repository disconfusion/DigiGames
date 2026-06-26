<script lang="ts">
	import type { BoardProps } from './board';
	import GameResultOverlay from './GameResultOverlay.svelte';

	let { send, event, me }: BoardProps = $props();

	type Phase = 'PLACEMENT' | 'BATTLE';
	type Cell = string; // "~" | "S" | "X" | "O" | "?"
	type Power = { id: string; label: string; emoji: string; usage: string; phase: string; owned: number };

	type BattleshipState = {
		phase: Phase;
		status: 'PLAYING' | 'WON' | 'LOST';
		yourTurn: boolean;
		youReady: boolean;
		enemyReady: boolean;
		winner: string | null;
		yourBoard: Cell[][];
		enemyBoard: Cell[][];
		cols?: number;
		yourRows?: number;
		enemyRows?: number;
		yourFleet?: number;
		enemyFleet?: number;
		yourSunk?: number;
		enemySunk?: number;
		decoys?: number[][];
		powers?: Power[];
	};

	let state = $state<BattleshipState | null>(null);
	let over = $state<{ status: string; winner: string | null } | null>(null);

	// Animazioni colpo (esplosione/schizzo) — solo celle appena cambiate.
	let flashes = $state<Record<string, 'hit' | 'miss'>>({});
	let prevSnapshot: BattleshipState | null = null;

	// Poteri / cyberdeck
	let activePower = $state<Power | null>(null);
	let moveFrom = $state<{ r: number; c: number } | null>(null); // sposta barca: nave sorgente
	let moveHorizontal = $state(true);
	let radarReveal = $state<Record<string, 'ship' | 'water'>>({}); // overlay radar temporaneo
	let torpedoFx = $state<{ r: number; c: number; level: number } | null>(null); // alone prossimità

	function detectFlashes(prev: BattleshipState | null, next: BattleshipState) {
		if (!prev) return;
		const add: Record<string, 'hit' | 'miss'> = {};
		const scan = (pb: Cell[][] | undefined, nb: Cell[][] | undefined, side: 'own' | 'enemy') => {
			if (!pb || !nb) return;
			for (let r = 0; r < nb.length; r++) {
				for (let c = 0; c < nb[r].length; c++) {
					const after = nb[r][c];
					if (after === pb[r]?.[c]) continue;
					if (after === 'X') add[`${side}-${r}-${c}`] = 'hit';
					else if (after === 'O') add[`${side}-${r}-${c}`] = 'miss';
				}
			}
		};
		scan(prev.yourBoard, next.yourBoard, 'own');
		scan(prev.enemyBoard, next.enemyBoard, 'enemy');
		const keys = Object.keys(add);
		if (keys.length === 0) return;
		flashes = { ...flashes, ...add };
		setTimeout(() => {
			const copy = { ...flashes };
			for (const k of keys) delete copy[k];
			flashes = copy;
		}, 650);
	}

	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			const next = e as unknown as BattleshipState;
			detectFlashes(prevSnapshot, next);
			prevSnapshot = next;
			state = next;
			if ((e as { status?: string }).status === 'PLAYING') over = null;
		} else if (e.type === 'game:over') {
			over = { status: String(e.status), winner: (e.winner as string) ?? null };
		} else if (e.type === 'power:torpedo') {
			const prox = Number((e as { proximity?: number }).proximity ?? -1);
			const level = prox <= 0 ? 0 : prox === 1 ? 1 : prox === 2 ? 2 : 3;
			torpedoFx = { r: Number((e as { r?: number }).r), c: Number((e as { c?: number }).c), level };
			setTimeout(() => (torpedoFx = null), 2600);
		} else if (e.type === 'power:radar') {
			const cells = ((e as { cells?: number[][] }).cells ?? []) as number[][];
			const rev: Record<string, 'ship' | 'water'> = {};
			for (const cell of cells) rev[`${cell[0]}-${cell[1]}`] = cell[2] ? 'ship' : 'water';
			radarReveal = rev;
			setTimeout(() => (radarReveal = {}), 4000);
		}
	});

	const phase = $derived(state?.phase ?? null);
	const inBattle = $derived(phase === 'BATTLE');
	const inPlacement = $derived(phase === 'PLACEMENT');
	const cols = $derived(state?.cols ?? 10);

	function countCells(board: Cell[][] | undefined, v: string): number {
		if (!board) return 0;
		let n = 0;
		for (const row of board) for (const cell of row) if (cell === v) n++;
		return n;
	}
	const yourFleet = $derived(state?.yourFleet ?? 5);
	const enemyFleet = $derived(state?.enemyFleet ?? 5);
	const enemySunk = $derived(state?.enemySunk ?? 0);
	const yourSunk = $derived(state?.yourSunk ?? 0);
	const myHits = $derived(countCells(state?.enemyBoard, 'X'));
	const myMisses = $derived(countCells(state?.enemyBoard, 'O'));
	const hitsTaken = $derived(countCells(state?.yourBoard, 'X'));

	// ── Cyberdeck ──
	const ownPowers = $derived((state?.powers ?? []).filter((p) => p.owned > 0));
	const decoySet = $derived(new Set((state?.decoys ?? []).map(([r, c]) => `${r}-${c}`)));
	const enemyTargeting = $derived(!!activePower && (activePower.id === 'bs_torpedo' || activePower.id === 'bs_radar'));
	const ownTargeting = $derived(
		!!activePower && (activePower.id === 'bs_extend_ship' || activePower.id === 'bs_decoy' || activePower.id === 'bs_move_ship')
	);

	function usePower(id: string, params: Record<string, unknown>) {
		send({ type: 'power:use', powerId: id, ...params });
	}
	function clearPower() {
		activePower = null;
		moveFrom = null;
	}
	function selectPower(p: Power) {
		if (activePower?.id === p.id) {
			clearPower();
			return;
		}
		// Poteri ad attivazione immediata (nessun bersaglio)
		if (p.id === 'bs_expand_board') {
			usePower('bs_expand_board', {});
			clearPower();
			return;
		}
		if (p.id === 'bs_extra_ship') {
			usePower('bs_extra_ship', {});
			clearPower();
			return;
		}
		activePower = p;
		moveFrom = null;
	}

	// ── Piazzamento manuale (board base 10x10) ──
	const FLEET = [5, 4, 3, 3, 2];
	type PlacedShip = { r: number; c: number; len: number; horizontal: boolean };
	let localShips = $state<(PlacedShip | null)[]>([null, null, null, null, null]);
	let selectedIdx = $state<number | null>(0);
	let localHorizontal = $state(true);
	let hoverR = $state(-1);
	let hoverC = $state(-1);
	let localMode = $state(true);

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
			cells.push({ r: localHorizontal ? hoverR : hoverR + i, c: localHorizontal ? hoverC + i : hoverC });
		}
		return cells;
	}
	const hoverPreviewCells = $derived(computeHoverPreview());
	const hoverPreviewValid = $derived(
		hoverPreviewCells.length > 0 &&
			hoverPreviewCells.every(
				({ r, c }) =>
					r >= 0 && r < 10 && c >= 0 && c < 10 &&
					localShips.every((ship, si) => si === selectedIdx || !ship || !shipOccupies(ship, r, c))
			)
	);
	function getLocalCellClass(r: number, c: number): string {
		for (const ship of localShips) if (ship && shipOccupies(ship, r, c)) return 'ship';
		if (hoverPreviewCells.some((p) => p.r === r && p.c === c)) return hoverPreviewValid ? 'preview' : 'preview-invalid';
		return 'water';
	}
	function isPlacedCell(r: number, c: number): boolean {
		return localShips.some((ship) => ship && shipOccupies(ship, r, c));
	}
	function clickLocalCell(r: number, c: number) {
		for (let si = 0; si < localShips.length; si++) {
			const ship = localShips[si];
			if (ship && shipOccupies(ship, r, c)) {
				localShips[si] = null;
				selectedIdx = si;
				localHorizontal = ship.horizontal;
				return;
			}
		}
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

	// ── Azioni ──
	const start = () => send({ type: 'game:start' });
	function doRandomize() {
		localShips = [null, null, null, null, null];
		selectedIdx = null;
		localMode = false;
		send({ type: 'randomize' });
	}
	const doReady = () => send({ type: 'ready' });

	function enemyCellClick(r: number, c: number) {
		if (over) return;
		if (enemyTargeting && activePower) {
			usePower(activePower.id, { r, c });
			clearPower();
			return;
		}
		if (!state?.yourTurn) return;
		if (state.enemyBoard[r]?.[c] !== '?') return;
		send({ type: 'fire', r, c });
	}

	function ownCellClick(r: number, c: number) {
		if (!activePower) return;
		if (activePower.id === 'bs_extend_ship') {
			usePower('bs_extend_ship', { r, c });
			clearPower();
		} else if (activePower.id === 'bs_decoy') {
			usePower('bs_decoy', { r, c });
			clearPower();
		} else if (activePower.id === 'bs_move_ship') {
			if (!moveFrom) {
				moveFrom = { r, c };
			} else {
				usePower('bs_move_ship', { fromR: moveFrom.r, fromC: moveFrom.c, toR: r, toC: c, horizontal: moveHorizontal });
				clearPower();
			}
		}
	}

	function cellTitle(cell: Cell, enemy: boolean): string {
		if (enemy) return cell === 'X' ? 'Colpito' : cell === 'O' ? 'Mancato' : 'Sconosciuto';
		return cell === 'S' ? 'Nave' : cell === 'X' ? 'Nave colpita' : cell === 'O' ? 'Colpo mancato' : 'Acqua';
	}
</script>

<div class="battleship">
	{#if !state}
		<p class="muted">Nessuna partita in corso.</p>
		<button class="primary" onclick={start}>Inizia</button>
	{:else if over}
		<GameResultOverlay result={over.status === 'WON' ? 'win' : 'lose'} onPlayAgain={start} />
	{:else if inPlacement}
		<div class="phase-head"><h3>Disponi la tua flotta</h3></div>

		{#if localMode}
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
							{#each { length: len } as _}<span class="seg"></span>{/each}
						</button>
					{/each}
				</div>
			</div>

			<div class="orient-row">
				<button class="orient-btn" class:orient-active={localHorizontal} onclick={() => (localHorizontal = true)}>↔ Orizz.</button>
				<button class="orient-btn" class:orient-active={!localHorizontal} onclick={() => (localHorizontal = false)}>↕ Vert.</button>
			</div>

			<div class="grid-wrap">
				<div class="grid own interactive" style="--cols: 10">
					{#each { length: 10 } as _, r}
						{#each { length: 10 } as _, c}
							{@const cls = getLocalCellClass(r, c)}
							<button
								class="cell {cls}"
								class:pickable={isPlacedCell(r, c)}
								onmouseenter={() => { hoverR = r; hoverC = c; }}
								onmouseleave={() => { hoverR = -1; hoverC = -1; }}
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
				{#if selectedIdx !== null}Clicca griglia per posizionare • Clicca nave piazzata per spostarla
				{:else if allLocalPlaced}Tutte le navi piazzate. Premi Conferma.
				{:else}Seleziona una nave dalla lista{/if}
			</p>
		{:else}
			<div class="grid-wrap">
				<div class="grid own" style="--cols: {cols}">
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
				{#if state.youReady}In attesa dell'avversario…
				{:else if fleetConfirmed}Flotta posizionata. Premi "Pronto" quando sei sicuro.
				{:else}Posizionamento in corso…{/if}
				{#if state.enemyReady}<span class="ok">Avversario pronto ✓</span>{:else}<span class="muted">Avversario non ancora pronto.</span>{/if}
			</p>
		{/if}
	{:else if inBattle}
		<div class="turn" class:active={state.yourTurn}>
			{state.yourTurn ? '🎯 Tocca a te' : '⏳ Turno avversario'}
		</div>

		<!-- Cyberdeck: poteri usabili -->
		<div class="cyberdeck">
			<span class="deck-label">⚡ CYBERDECK</span>
			{#if ownPowers.length === 0}
				<span class="deck-empty">Nessun potere — compra dallo <a href="/shop">shop</a></span>
			{:else}
				<div class="deck-slots">
					{#each ownPowers as p (p.id)}
						<button class="slot" class:active={activePower?.id === p.id} onclick={() => selectPower(p)} title={p.usage}>
							<span class="slot-emoji">{p.emoji}</span>
							<span class="slot-name">{p.label}</span>
							<span class="slot-qty">×{p.owned}</span>
						</button>
					{/each}
				</div>
			{/if}
		</div>

		<!-- HUD di guida quando un potere è attivo -->
		{#if activePower}
			<div class="hud">
				<div class="hud-icon">{activePower.emoji}</div>
				<div class="hud-body">
					<strong>{activePower.label}</strong>
					<p>
						{activePower.usage}
						{#if activePower.id === 'bs_move_ship'}
							{moveFrom ? ' — ora scegli la NUOVA posizione (testa della nave).' : ' — scegli prima la nave da spostare.'}
						{/if}
					</p>
					{#if activePower.id === 'bs_move_ship'}
						<div class="hud-orient">
							<button class:on={moveHorizontal} onclick={() => (moveHorizontal = true)}>↔ Orizz.</button>
							<button class:on={!moveHorizontal} onclick={() => (moveHorizontal = false)}>↕ Vert.</button>
						</div>
					{/if}
				</div>
				<button class="hud-cancel" onclick={clearPower}>✕ Annulla</button>
			</div>
		{/if}

		<div class="boards">
			<div class="board-col own-col">
				<h4>La tua flotta {#if ownTargeting}<span class="target-tag">bersaglio</span>{/if}</h4>
				<div class="grid own" class:targetable={ownTargeting} style="--cols: {cols}">
					{#each state.yourBoard as row, r (r)}
						{#each row as cell, c (c)}
							{@const key = `${r}-${c}`}
							<button
								class="cell {cell === 'S' ? 'ship' : cell === 'X' ? 'hit' : cell === 'O' ? 'miss' : 'water'}"
								class:just-hit={flashes[`own-${key}`] === 'hit'}
								class:just-miss={flashes[`own-${key}`] === 'miss'}
								class:decoy={decoySet.has(key)}
								class:movesrc={moveFrom?.r === r && moveFrom?.c === c}
								disabled={!ownTargeting}
								onclick={() => ownCellClick(r, c)}
								title={cellTitle(cell, false)}
							>{#if decoySet.has(key)}<span class="decoy-mark">🪤</span>{/if}</button>
						{/each}
					{/each}
				</div>
				<div class="stats own-stats">
					<span class="stat" class:danger={yourSunk > 0}>🚢 Affondate {yourSunk}/{yourFleet}</span>
					<span class="stat">🎯 Colpita {hitsTaken}×</span>
				</div>
			</div>

			<div class="board-col enemy-col">
				<h4>Avversario {#if enemyTargeting}<span class="target-tag">bersaglio</span>{/if}</h4>
				<div class="grid enemy" class:targetable={enemyTargeting} style="--cols: {cols}">
					{#each state.enemyBoard as row, r (r)}
						{#each row as cell, c (c)}
							{@const key = `${r}-${c}`}
							<button
								class="cell {cell === 'X' ? 'hit' : cell === 'O' ? 'miss' : 'unknown'}"
								class:just-hit={flashes[`enemy-${key}`] === 'hit'}
								class:just-miss={flashes[`enemy-${key}`] === 'miss'}
								class:radar-ship={radarReveal[key] === 'ship'}
								class:radar-water={radarReveal[key] === 'water'}
								class:tfx0={torpedoFx?.r === r && torpedoFx?.c === c && torpedoFx?.level ===0}
								class:tfx1={torpedoFx?.r === r && torpedoFx?.c === c && torpedoFx?.level ===1}
								class:tfx2={torpedoFx?.r === r && torpedoFx?.c === c && torpedoFx?.level ===2}
								class:tfx3={torpedoFx?.r === r && torpedoFx?.c === c && torpedoFx?.level ===3}
								disabled={cell !== '?' || (!enemyTargeting && !state.yourTurn)}
								onclick={() => enemyCellClick(r, c)}
								title={cellTitle(cell, true)}
								aria-label={`Spara in ${r},${c}`}
							></button>
						{/each}
					{/each}
				</div>
				<div class="stats enemy-stats">
					<span class="stat" class:good={enemySunk > 0}>🔥 Affondate {enemySunk}/{enemyFleet}</span>
					<span class="stat">🎯 A segno {myHits}</span>
					<span class="stat muted-stat">💧 Mancati {myMisses}</span>
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
		font-family: var(--font-ui, sans-serif);
		width: 100%;
	}
	.phase-head { text-align: center; }
	.phase-head h3 {
		margin: 0;
		font-family: var(--font-display, sans-serif);
		font-size: 0.85rem;
		letter-spacing: 0.08em;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}

	/* Selettore flotta (placement) */
	.fleet-panel { display: flex; flex-direction: column; align-items: center; gap: 0.4rem; }
	.fleet-label { font-size: 0.75rem; font-family: var(--font-term, monospace); letter-spacing: 0.06em; color: var(--muted); }
	.fleet-ships { display: flex; gap: 0.5rem; flex-wrap: wrap; justify-content: center; }
	.ship-btn {
		display: flex; gap: 2px; align-items: center; padding: 5px 7px; min-height: 44px;
		border: 2px solid var(--line); border-radius: 4px; background: var(--panel); cursor: pointer;
		transition: border-color 0.12s, background 0.12s, opacity 0.12s, box-shadow 0.12s;
	}
	.ship-btn.selected { border-color: var(--cyan); background: rgba(47, 243, 255, 0.1); box-shadow: var(--glow-cyan); }
	.ship-btn.placed:not(.selected) { opacity: 0.35; }
	.seg { display: block; width: 12px; height: 12px; border-radius: 2px; background: var(--muted); }
	.ship-btn.selected .seg { background: var(--cyan); box-shadow: var(--glow-cyan); }

	.orient-row { display: flex; gap: 0.4rem; }
	.orient-btn {
		padding: 0.35rem 0.9rem; min-height: 44px; border: 1px solid var(--line); border-radius: 4px;
		background: var(--panel); color: var(--muted); font-family: var(--font-term, monospace);
		font-size: 1rem; cursor: pointer; transition: border-color 0.12s, color 0.12s, background 0.12s, box-shadow 0.12s;
	}
	.orient-btn.orient-active { border-color: var(--cyan); color: var(--cyan); background: rgba(47, 243, 255, 0.08); box-shadow: var(--glow-cyan); }

	/* Griglie */
	.boards { display: flex; flex-wrap: wrap; gap: 1.5rem; justify-content: center; align-items: flex-start; width: 100%; }
	.board-col { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
	.board-col h4 {
		margin: 0; font-family: var(--font-term, monospace); font-size: 1.1rem; letter-spacing: 0.04em;
		color: var(--muted); font-weight: 600; display: flex; align-items: center; gap: 0.4rem;
	}
	.own-col h4 { color: var(--cyan); text-shadow: var(--glow-cyan); }
	.enemy-col h4 { color: var(--accent); text-shadow: var(--glow-mag); }
	.target-tag {
		font-family: var(--font-ui); font-size: 0.6rem; text-transform: uppercase; letter-spacing: 0.08em;
		background: var(--amber); color: #1a1030; border-radius: 4px; padding: 0.1rem 0.35rem; animation: tag-pulse 1s ease-in-out infinite;
	}
	@keyframes tag-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

	.grid {
		display: grid;
		grid-template-columns: repeat(var(--cols, 10), 1fr);
		gap: 2px; background: var(--bg); padding: 4px; border-radius: 6px;
		border: 1px solid var(--cyan); box-shadow: 0 0 8px rgba(47, 243, 255, 0.18);
		width: min(90vw, 360px);
	}
	.grid.enemy { background: var(--inset); border-color: var(--accent); box-shadow: 0 0 8px rgba(255, 46, 136, 0.18); }
	.grid.own .cell { cursor: default; }
	.grid.interactive .cell { cursor: crosshair; }
	.grid.interactive .cell.pickable { cursor: pointer; }
	.grid.targetable { box-shadow: 0 0 14px var(--amber); border-color: var(--amber); }
	.grid.targetable .cell:not(:disabled) { cursor: crosshair; }

	.cell {
		width: 100%; aspect-ratio: 1 / 1; border: none; border-radius: 2px; padding: 0;
		background: #10243a; transition: background 0.1s, box-shadow 0.1s; position: relative;
	}
	button.cell:disabled { cursor: not-allowed; }
	.cell.water { background: #10243a; }
	.cell.unknown { background: #1a2a44; }
	@media (hover: hover) {
		button.cell.unknown:not(:disabled):hover { background: var(--accent); box-shadow: var(--glow-mag); }
		.grid.targetable button.cell:not(:disabled):hover { background: var(--amber); box-shadow: 0 0 10px var(--amber); }
	}
	.cell.ship { background: #3a6b8c; box-shadow: inset 0 0 4px rgba(47, 243, 255, 0.3); }
	.cell.hit { background: var(--accent); box-shadow: var(--glow-mag); }
	.cell.hit::after {
		content: 'X'; position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
		font-family: var(--font-term, monospace); font-size: clamp(0.55rem, 1.5vw, 0.85rem); color: #fff; line-height: 1;
	}
	.cell.miss { background: #475569; }
	.cell.miss::after { content: ''; position: absolute; inset: 38%; border-radius: 50%; background: var(--bg); }
	.cell.preview { background: rgba(47, 243, 255, 0.55); box-shadow: var(--glow-cyan); }
	.cell.preview-invalid { background: rgba(255, 82, 119, 0.55); box-shadow: 0 0 6px rgba(255, 82, 119, 0.5); }

	/* Esca (propria) */
	.cell.decoy { box-shadow: inset 0 0 0 2px var(--amber); }
	.decoy-mark { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: clamp(0.5rem, 1.4vw, 0.8rem); }
	/* Nave sorgente selezionata per lo spostamento */
	.cell.movesrc { box-shadow: 0 0 0 2px var(--cyan), var(--glow-cyan); }

	/* Radar reveal (temporaneo) */
	.cell.radar-ship::before, .cell.radar-water::before {
		content: ''; position: absolute; inset: 30%; border-radius: 50%; animation: radar-blip 0.3s ease-out;
	}
	.cell.radar-ship::before { background: var(--danger); box-shadow: 0 0 8px var(--danger); }
	.cell.radar-water::before { background: var(--cyan); opacity: 0.7; }
	@keyframes radar-blip { from { transform: scale(0); } to { transform: scale(1); } }

	/* Alone siluro di prossimità */
	.cell.tfx0, .cell.tfx1, .cell.tfx2, .cell.tfx3 { z-index: 2; }
	.cell.tfx0 { box-shadow: 0 0 16px 6px var(--accent); }
	.cell.tfx1 { box-shadow: 0 0 16px 6px #3dff9a; }
	.cell.tfx2 { box-shadow: 0 0 16px 6px #ffcf3f; }
	.cell.tfx3 { box-shadow: 0 0 16px 6px #ff5277; }

	/* Cyberdeck */
	.cyberdeck {
		width: 100%; max-width: 760px; display: flex; flex-direction: column; gap: 0.5rem;
		background: var(--panel); border: 1px solid var(--line); border-radius: 10px; padding: 0.6rem 0.8rem;
	}
	.deck-label { font-family: var(--font-display, monospace); font-size: 0.62rem; color: var(--cyan); text-shadow: var(--glow-cyan); letter-spacing: 0.08em; }
	.deck-empty { color: var(--muted); font-size: 0.9rem; }
	.deck-empty a { color: var(--cyan); }
	.deck-slots { display: flex; gap: 0.5rem; flex-wrap: wrap; }
	.slot {
		display: flex; flex-direction: column; align-items: center; gap: 0.1rem; min-width: 4.5rem;
		padding: 0.4rem 0.5rem; border: 1px solid var(--line); border-radius: 8px; background: var(--inset);
		color: var(--text); cursor: pointer; transition: border-color 0.12s, box-shadow 0.12s;
	}
	.slot:hover { border-color: var(--cyan); }
	.slot.active { border-color: var(--amber); box-shadow: 0 0 12px rgba(255, 207, 63, 0.5); }
	.slot-emoji { font-size: 1.3rem; line-height: 1; }
	.slot-name { font-size: 0.62rem; text-align: center; color: var(--muted); }
	.slot-qty { font-size: 0.7rem; font-weight: 700; color: var(--amber); }

	/* HUD guida */
	.hud {
		width: 100%; max-width: 760px; display: flex; align-items: center; gap: 0.75rem;
		background: linear-gradient(180deg, rgba(255, 207, 63, 0.12), transparent);
		border: 1px solid var(--amber); border-radius: 10px; padding: 0.6rem 0.8rem;
	}
	.hud-icon { font-size: 1.8rem; line-height: 1; }
	.hud-body { flex: 1; }
	.hud-body strong { color: var(--amber); font-family: var(--font-ui); font-size: 0.9rem; }
	.hud-body p { margin: 0.15rem 0 0; font-size: 0.85rem; color: var(--text); font-family: var(--font-term, monospace); }
	.hud-orient { display: flex; gap: 0.4rem; margin-top: 0.4rem; }
	.hud-orient button {
		padding: 0.25rem 0.6rem; border: 1px solid var(--line); border-radius: 4px; background: var(--inset);
		color: var(--muted); cursor: pointer; font-size: 0.8rem;
	}
	.hud-orient button.on { border-color: var(--cyan); color: var(--cyan); box-shadow: var(--glow-cyan); }
	.hud-cancel {
		align-self: flex-start; padding: 0.35rem 0.7rem; border: 1px solid var(--danger); border-radius: 4px;
		background: transparent; color: var(--danger); cursor: pointer; font-size: 0.78rem; white-space: nowrap;
	}

	/* Turno */
	.turn {
		font-family: var(--font-term, monospace); font-size: 1.1rem; letter-spacing: 0.05em;
		padding: 0.5rem 1.2rem; border-radius: 4px; border: 1px solid var(--line); background: var(--panel);
		color: var(--muted); transition: background 0.2s, color 0.2s, box-shadow 0.2s;
	}
	.turn.active { background: var(--accent); border-color: var(--accent); color: #fff; box-shadow: var(--glow-mag); }

	.actions { display: flex; gap: 0.75rem; flex-wrap: wrap; justify-content: center; }
	.hint {
		color: var(--muted); text-align: center; margin: 0; display: flex; gap: 0.5rem; flex-wrap: wrap;
		justify-content: center; font-family: var(--font-term, monospace); font-size: 0.95rem; letter-spacing: 0.03em;
	}
	.ok { color: var(--green); text-shadow: 0 0 6px rgba(61, 255, 154, 0.5); }

	.stats { display: flex; gap: 0.5rem 0.9rem; flex-wrap: wrap; justify-content: center; font-family: var(--font-term, monospace); font-size: 0.95rem; letter-spacing: 0.03em; }
	.stat { color: var(--muted); white-space: nowrap; }
	.stat.good { color: var(--green); text-shadow: 0 0 6px rgba(61, 255, 154, 0.45); }
	.stat.danger { color: var(--danger); text-shadow: 0 0 6px rgba(255, 82, 119, 0.45); }
	.stat.muted-stat { opacity: 0.7; }

	.primary {
		padding: 0.6rem 1.2rem; min-height: 44px; border: 2px solid var(--accent); border-radius: 4px;
		background: var(--accent); color: #fff; font-family: var(--font-ui, sans-serif); font-size: 0.85rem;
		font-weight: 700; letter-spacing: 0.05em; cursor: pointer; transition: box-shadow 0.15s, opacity 0.15s;
	}
	.primary:not(:disabled):hover { box-shadow: var(--glow-mag); }
	.primary:disabled { opacity: 0.5; cursor: not-allowed; }
	.ghost {
		padding: 0.6rem 1.2rem; min-height: 44px; border: 1px solid var(--line); border-radius: 4px;
		background: var(--panel); color: var(--text); font-family: var(--font-ui, sans-serif); font-size: 0.85rem;
		font-weight: 600; letter-spacing: 0.04em; cursor: pointer; transition: border-color 0.15s, box-shadow 0.15s, opacity 0.15s;
	}
	.ghost:not(:disabled):hover { border-color: var(--cyan); box-shadow: var(--glow-cyan); }
	.ghost:disabled { opacity: 0.4; cursor: not-allowed; }
	.muted { color: var(--muted); font-family: var(--font-term, monospace); }

	@media (prefers-reduced-motion: reduce) {
		.cell, .ship-btn, .orient-btn, .turn, .primary, .ghost { transition: none; }
		.cell.just-hit { animation: none; }
		.cell.just-hit::before, .cell.just-miss::before { display: none; }
		.target-tag { animation: none; }
	}

	/* Animazioni colpo (esplosione/schizzo) */
	.cell.just-hit { animation: hit-pop 0.5s ease-out; z-index: 2; }
	.cell.just-hit::before {
		content: ''; position: absolute; inset: -4px; border-radius: 50%;
		background: radial-gradient(circle, rgba(255, 207, 63, 0.95) 0%, rgba(255, 46, 136, 0.7) 45%, transparent 72%);
		animation: shockwave 0.55s ease-out forwards; pointer-events: none; z-index: 3;
	}
	.cell.just-miss::before {
		content: ''; position: absolute; inset: 26%; border-radius: 50%; border: 2px solid rgba(47, 243, 255, 0.9);
		animation: splash 0.6s ease-out forwards; pointer-events: none; z-index: 3;
	}
	@keyframes hit-pop { 0% { transform: scale(1); } 28% { transform: scale(1.35); box-shadow: 0 0 18px 6px var(--amber); } 100% { transform: scale(1); } }
	@keyframes shockwave { 0% { transform: scale(0.2); opacity: 1; } 100% { transform: scale(2.4); opacity: 0; } }
	@keyframes splash { 0% { transform: scale(0.3); opacity: 0.95; } 100% { transform: scale(2.3); opacity: 0; } }

	@media (max-width: 720px) {
		.boards { flex-direction: column; align-items: center; }
	}
</style>
