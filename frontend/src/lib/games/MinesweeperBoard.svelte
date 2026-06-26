<script lang="ts">
	import type { BoardProps } from './board';
	import GameResultOverlay from './GameResultOverlay.svelte';

	let { send, event, me }: BoardProps = $props();

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
		currentTurn: string | null;
		cells: Cell[][];
	};

	let state = $state<GameState | null>(null);
	let over  = $state<{ status: string } | null>(null);
	let flagMode = $state(false);

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

	const playing   = $derived(state?.status === 'PLAYING');
	const minesLeft = $derived((state?.minesTotal ?? 0) - (state?.flagsUsed ?? 0));
	const isMyTurn  = $derived(state?.currentTurn == null || state.currentTurn === me.username);
	const canPlay   = $derived(playing && isMyTurn);

	function handleCellClick(r: number, c: number) {
		if (!canPlay) return;
		const cell = state!.cells[r][c];
		if (cell.revealed) return;
		if (flagMode) send({ type: 'flag', r, c });
		else send({ type: 'reveal', r, c });
	}

	function handleRightClick(e: MouseEvent, r: number, c: number) {
		e.preventDefault();
		if (!canPlay) return;
		const cell = state!.cells[r][c];
		if (cell.revealed) return;
		send({ type: 'flag', r, c });
	}

	function startGame() {
		send({ type: 'game:start' });
		over = null;
		flagMode = false;
	}

	// Colori adiacenti con glow Retro/CRT: 1=cyan, 2=green, 3=magenta, 4=viola, 5=corallo, 6=teal, 7=rosa, 8=muted
	const ADJ_COLORS: Record<number, string> = {
		1: 'var(--cyan)',    2: 'var(--green)', 3: 'var(--accent)', 4: '#a78bfa',
		5: '#f97362',        6: '#22d3ee',       7: '#f0abfc',       8: 'var(--muted)',
	};
	// Text-shadow glow per ogni numero adiacente
	const ADJ_GLOWS: Record<number, string> = {
		1: '0 0 6px var(--cyan)',    2: '0 0 6px var(--green)', 3: '0 0 6px var(--accent)', 4: '0 0 6px #a78bfa',
		5: '0 0 6px #f97362',        6: '0 0 6px #22d3ee',       7: '0 0 6px #f0abfc',       8: '0 0 6px var(--muted)',
	};
	function adjColor(n: number): string { return ADJ_COLORS[n] ?? 'var(--text)'; }
	function adjGlow(n: number): string  { return ADJ_GLOWS[n]  ?? 'none'; }
</script>

<div class="ms-wrapper">

	<div class="ms-header">
		<span class="mine-counter" title="Mine rimanenti">💣 {minesLeft}</span>

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

	<!-- Indicatore turno -->
	{#if playing && state?.currentTurn != null}
		{#if isMyTurn}
			<div class="turn-badge my-turn">Tocca a te! Rivela o piazza una bandierina.</div>
		{:else}
			<div class="turn-badge wait">Tocca a <strong>{state.currentTurn}</strong></div>
		{/if}
	{/if}

	<!-- Schermata di fine partita -->
	{#if over || (state && state.status !== 'PLAYING')}
		{@const s = over?.status ?? state?.status}
		<GameResultOverlay
			result={s === 'WON' ? 'win' : 'lose'}
			title={s === 'WON' ? 'CAMPO BONIFICATO' : 'BOOM!'}
			message={s === 'WON' ? 'Tutte le celle sicure rivelate!' : 'Avete colpito una mina.'}
		/>
	{/if}

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
						disabled={cell.revealed || !canPlay}
						onclick={() => handleCellClick(r, c)}
						oncontextmenu={(e) => handleRightClick(e, r, c)}
						aria-label="Cella ({r},{c})"
						role="gridcell"
					>
						{#if cell.revealed}
							{#if cell.mine}
								💣
							{:else if cell.adjacent && cell.adjacent > 0}
								<span style="color: {adjColor(cell.adjacent)}; text-shadow: {adjGlow(cell.adjacent)}; font-weight: 700; font-family: var(--font-term);">
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
	/* ── Campo Minato — tema Retro/CRT/Synthwave ── */
	/* NON ridefinire i token globali: vengono da retro-crt-theme.css */

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

	/* ── Intestazione ── */
	.ms-header {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.6rem;
		width: 100%;
		max-width: 560px;
		justify-content: space-between;
	}

	/* Contatore mine: font terminale stile 7-segmenti */
	.mine-counter {
		font-family: var(--font-display);
		font-size: 0.85rem;
		font-weight: 700;
		color: var(--accent);
		text-shadow: var(--glow-mag);
		min-width: 3.5rem;
		letter-spacing: 0.05em;
	}

	/* Pulsante bandierina */
	.flag-toggle {
		padding: 0.4rem 0.75rem;
		min-height: 44px;
		border-radius: 4px;
		border: 1px solid var(--muted);
		background: var(--panel);
		color: var(--text);
		font-family: var(--font-ui);
		cursor: pointer;
		font-size: 0.8rem;
		transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
	}
	.flag-toggle:hover { border-color: var(--amber); }
	.flag-toggle.active {
		border-color: var(--amber);
		background: #1a1000;
		color: var(--amber);
		box-shadow: 0 0 8px var(--amber);
	}

	/* Pulsante avvia partita */
	.btn-start {
		padding: 0.4rem 0.9rem;
		min-height: 44px;
		border: 1px solid var(--accent);
		border-radius: 4px;
		background: var(--panel);
		color: var(--accent);
		font-family: var(--font-ui);
		font-size: 0.85rem;
		font-weight: 600;
		cursor: pointer;
		text-shadow: var(--glow-mag);
		box-shadow: 0 0 8px color-mix(in srgb, var(--accent) 40%, transparent);
		transition: background 0.15s, box-shadow 0.15s;
	}
	.btn-start:hover {
		background: color-mix(in srgb, var(--accent) 15%, var(--panel));
		box-shadow: 0 0 16px var(--accent);
	}

	/* ── Badge turno ── */
	.turn-badge {
		width: 100%;
		max-width: 560px;
		padding: 0.4rem 0.9rem;
		border-radius: 4px;
		font-family: var(--font-ui);
		font-size: 0.85rem;
		font-weight: 600;
		text-align: center;
	}
	.turn-badge.my-turn {
		background: var(--inset);
		color: var(--cyan);
		border: 1px solid var(--cyan);
		box-shadow: 0 0 8px color-mix(in srgb, var(--cyan) 30%, transparent);
	}
	.turn-badge.wait {
		background: var(--panel);
		color: var(--muted);
		border: 1px solid var(--line);
	}

	/* ── Banner fine partita ── */
	.ms-banner {
		width: 100%;
		max-width: 560px;
		padding: 0.65rem 1rem;
		border-radius: 4px;
		text-align: center;
		font-family: var(--font-ui);
		font-weight: 600;
		font-size: 0.9rem;
		letter-spacing: 0.04em;
	}
	.ms-banner.won {
		background: var(--inset);
		color: var(--green);
		border: 1px solid var(--green);
		box-shadow: 0 0 12px color-mix(in srgb, var(--green) 40%, transparent);
	}
	.ms-banner.lost {
		background: var(--inset);
		color: var(--danger);
		border: 1px solid var(--danger);
		box-shadow: 0 0 12px color-mix(in srgb, var(--danger) 40%, transparent);
	}

	/* ── Griglia celle ── */
	.ms-grid {
		display: grid;
		grid-template-columns: repeat(var(--cols), 1fr);
		gap: 2px;
		width: 100%;
		max-width: 560px;
	}

	/* Cella base (coperta) */
	.ms-cell {
		aspect-ratio: 1 / 1;
		display: flex;
		align-items: center;
		justify-content: center;
		border: none;
		border-radius: 3px;
		background: #241340;
		color: var(--text);
		font-size: clamp(0.6rem, 2.5vw, 1rem);
		font-weight: 700;
		cursor: pointer;
		box-shadow: inset 0 0 0 1px var(--line);
		padding: 0;
		line-height: 1;
		min-width: 0;
		/* Transizione rispetta prefers-reduced-motion (vedi media query) */
		transition: background 0.1s, transform 0.05s, box-shadow 0.1s;
	}

	/* Hover cella coperta */
	.ms-cell:hover:not(:disabled) {
		background: color-mix(in srgb, var(--accent) 20%, #241340);
		box-shadow: inset 0 0 0 1px var(--accent);
		transform: scale(1.06);
	}

	/* Cella rivelata sicura */
	.ms-cell.revealed.safe {
		background: var(--inset);
		box-shadow: inset 0 0 0 1px var(--line);
		cursor: default;
	}

	/* Cella con mina esplosa */
	.ms-cell.revealed.mine {
		background: #3a1420;
		box-shadow: inset 0 0 0 1px var(--accent), 0 0 8px color-mix(in srgb, var(--accent) 50%, transparent);
		cursor: default;
	}

	/* Cella con bandierina */
	.ms-cell.flagged {
		background: #1a1000;
		box-shadow: inset 0 0 0 1px var(--amber);
	}

	.ms-cell:disabled { cursor: default; }
	.ms-cell:disabled:not(.revealed) { opacity: 0.7; }

	/* Messaggio partita non avviata */
	.ms-empty {
		color: var(--muted);
		font-family: var(--font-ui);
		text-align: center;
		padding: 2rem;
		max-width: 400px;
	}

	/* ── Responsive ── */
	@media (max-width: 480px) {
		.ms-header { justify-content: center; }
		.ms-grid { gap: 1px; }
		.ms-cell { border-radius: 2px; }
		.mine-counter { font-size: 0.7rem; }
	}

	/* Rispetta prefers-reduced-motion */
	@media (prefers-reduced-motion: reduce) {
		.ms-cell { transition: none; }
		.ms-cell:hover:not(:disabled) { transform: none; }
		.flag-toggle, .btn-start { transition: none; }
	}
</style>
