<script lang="ts">
	import type { BoardProps } from './board';

	let { send, event, me }: BoardProps = $props();

	// ----------------------------------------------------------------
	// Tipi
	// ----------------------------------------------------------------

	type Cell = {
		revealed: boolean;
		flagged: boolean;
		adjacent?: number;
		mine?: boolean;
	};

	type GameState = {
		rows: number;
		cols: number;
		minesTotal: number;
		flagsUsed: number;
		status: 'PLAYING' | 'WON' | 'LOST';
		cells: Cell[][];
	};

	// ----------------------------------------------------------------
	// Stato reattivo
	// ----------------------------------------------------------------

	let state = $state<GameState | null>(null);
	let over  = $state<{ status: string } | null>(null);
	let flagMode = $state(false); // modalità bandierina per mobile

	// ----------------------------------------------------------------
	// Reazione agli eventi (snapshot autoritativi)
	// ----------------------------------------------------------------

	$effect(() => {
		const e = event;
		if (!e) return;

		if (e.type === 'game:state') {
			state = e as unknown as GameState;
			if ((e as { status?: string }).status === 'PLAYING') over = null;
		} else if (e.type === 'game:over') {
			over = { status: String((e as { status?: string }).status) };
		}
	});

	// ----------------------------------------------------------------
	// Derived
	// ----------------------------------------------------------------

	const playing     = $derived(state?.status === 'PLAYING');
	const minesLeft   = $derived((state?.minesTotal ?? 0) - (state?.flagsUsed ?? 0));

	// ----------------------------------------------------------------
	// Azioni
	// ----------------------------------------------------------------

	function handleCellClick(r: number, c: number) {
		if (!playing) return;
		const cell = state!.cells[r][c];
		if (cell.revealed) return;

		if (flagMode) {
			send({ type: 'flag', r, c });
		} else {
			send({ type: 'reveal', r, c });
		}
	}

	function handleRightClick(e: MouseEvent, r: number, c: number) {
		e.preventDefault();
		if (!playing) return;
		const cell = state!.cells[r][c];
		if (cell.revealed) return;
		send({ type: 'flag', r, c });
	}

	function startGame() {
		send({ type: 'game:start' });
		over = null;
		flagMode = false;
	}

	// ----------------------------------------------------------------
	// Colori numeri adiacenti (classico minesweeper)
	// ----------------------------------------------------------------

	const ADJ_COLORS: Record<number, string> = {
		1: '#3b82f6', // blu
		2: '#22c55e', // verde
		3: '#ef4444', // rosso
		4: '#7c3aed', // viola
		5: '#dc2626', // rosso scuro
		6: '#06b6d4', // cyan
		7: '#ec4899', // rosa
		8: '#94a3b8', // grigio
	};

	function adjColor(n: number): string {
		return ADJ_COLORS[n] ?? '#e2e8f0';
	}
</script>

<div class="ms-wrapper">

	<!-- Header: contatore mine + toggle flag mode + nuova partita -->
	<div class="ms-header">
		<span class="mine-counter" title="Mine rimanenti">
			💣 {minesLeft}
		</span>

		{#if state}
			<button
				class="flag-toggle"
				class:active={flagMode}
				onclick={() => (flagMode = !flagMode)}
				title="Modalità bandierina (anche tasto destro)"
			>
				🚩 {flagMode ? 'Bandierina ON' : 'Bandierina OFF'}
			</button>
		{/if}

		<button class="btn-start" onclick={startGame}>
			{state ? '🔄 Nuova partita' : '▶ Inizia partita'}
		</button>
	</div>

	<!-- Banner WON / LOST -->
	{#if over || (state && state.status !== 'PLAYING')}
		{@const s = over?.status ?? state?.status}
		<div class="ms-banner" class:won={s === 'WON'} class:lost={s === 'LOST'}>
			{#if s === 'WON'}
				🎉 Avete vinto! Tutte le celle sicure sono state rivelate!
			{:else}
				💥 Boom! Avete colpito una mina. Partita persa.
			{/if}
		</div>
	{/if}

	<!-- Griglia -->
	{#if state}
		<div
			class="ms-grid"
			style="--cols: {state.cols};"
			role="grid"
			aria-label="Campo Minato {state.rows}×{state.cols}"
		>
			{#each state.cells as row, r (r)}
				{#each row as cell, c (c)}
					<button
						class="ms-cell"
						class:revealed={cell.revealed}
						class:flagged={cell.flagged}
						class:mine={cell.revealed && cell.mine}
						class:safe={cell.revealed && !cell.mine}
						disabled={cell.revealed || !playing}
						onclick={() => handleCellClick(r, c)}
						oncontextmenu={(e) => handleRightClick(e, r, c)}
						aria-label="Cella ({r},{c})"
						role="gridcell"
					>
						{#if cell.revealed}
							{#if cell.mine}
								💣
							{:else if cell.adjacent && cell.adjacent > 0}
								<span style="color: {adjColor(cell.adjacent)}; font-weight: 700;">
									{cell.adjacent}
								</span>
							{/if}
						{:else if cell.flagged}
							🚩
						{/if}
					</button>
				{/each}
			{/each}
		</div>
	{:else}
		<p class="ms-empty">Nessuna partita in corso. Premi "Inizia partita" per cominciare.</p>
	{/if}

</div>

<style>
	/* ---- Variabili tema (dark) ---- */
	:root {
		--bg:     #0f172a;
		--panel:  #1e293b;
		--accent: #6366f1;
		--text:   #e2e8f0;
		--muted:  #94a3b8;
	}

	.ms-wrapper {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.75rem;
		padding: 1rem;
		color: var(--text);
		background: var(--bg);
		min-height: 100%;
		box-sizing: border-box;
	}

	/* ---- Header ---- */
	.ms-header {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.6rem;
		width: 100%;
		max-width: 560px;
		justify-content: space-between;
	}

	.mine-counter {
		font-size: 1.2rem;
		font-weight: 700;
		min-width: 3.5rem;
	}

	.flag-toggle {
		padding: 0.35rem 0.7rem;
		border-radius: 6px;
		border: 2px solid var(--muted);
		background: var(--panel);
		color: var(--text);
		cursor: pointer;
		font-size: 0.85rem;
		transition: border-color 0.15s, background 0.15s;
	}
	.flag-toggle.active {
		border-color: #f59e0b;
		background: #451a03;
		color: #fde68a;
	}

	.btn-start {
		padding: 0.4rem 0.9rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: #fff;
		font-size: 0.9rem;
		font-weight: 600;
		cursor: pointer;
		transition: opacity 0.15s;
	}
	.btn-start:hover {
		opacity: 0.85;
	}

	/* ---- Banner ---- */
	.ms-banner {
		width: 100%;
		max-width: 560px;
		padding: 0.65rem 1rem;
		border-radius: 8px;
		text-align: center;
		font-weight: 600;
		font-size: 1rem;
	}
	.ms-banner.won  { background: #14532d; color: #bbf7d0; }
	.ms-banner.lost { background: #7f1d1d; color: #fecaca; }

	/* ---- Griglia ---- */
	.ms-grid {
		display: grid;
		grid-template-columns: repeat(var(--cols), 1fr);
		gap: 2px;
		width: 100%;
		max-width: 560px;
		/* celle quadrate: aspect-ratio non funziona su grid col, usiamo padding-bottom trick via JS */
	}

	/* ---- Cella ---- */
	.ms-cell {
		aspect-ratio: 1 / 1;
		display: flex;
		align-items: center;
		justify-content: center;
		border: none;
		border-radius: 4px;
		background: var(--panel);
		color: var(--text);
		font-size: clamp(0.6rem, 2.5vw, 1rem);
		font-weight: 700;
		cursor: pointer;
		transition: background 0.1s, transform 0.05s;
		box-shadow: inset 0 0 0 1px #334155;
		padding: 0;
		line-height: 1;
		min-width: 0;
	}

	.ms-cell:hover:not(:disabled) {
		background: #334155;
		transform: scale(1.08);
	}

	/* Cella rivelata (sicura) */
	.ms-cell.revealed.safe {
		background: #0f172a;
		box-shadow: inset 0 0 0 1px #1e293b;
		cursor: default;
	}

	/* Cella con mina (solo dopo game over) */
	.ms-cell.revealed.mine {
		background: #7f1d1d;
		box-shadow: inset 0 0 0 1px #dc2626;
		cursor: default;
	}

	/* Cella flaggata (coperta) */
	.ms-cell.flagged {
		background: #1c1917;
		box-shadow: inset 0 0 0 1px #f59e0b;
	}

	/* Disabilitata (già rivelata o partita finita) */
	.ms-cell:disabled {
		cursor: default;
	}
	.ms-cell:disabled:not(.revealed) {
		opacity: 0.7;
	}

	/* ---- Empty state ---- */
	.ms-empty {
		color: var(--muted);
		text-align: center;
		padding: 2rem;
		max-width: 400px;
	}

	/* ---- Responsive ---- */
	@media (max-width: 480px) {
		.ms-header {
			justify-content: center;
		}
		.ms-grid {
			gap: 1px;
		}
		.ms-cell {
			border-radius: 2px;
		}
	}
</style>
