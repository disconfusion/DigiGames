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
	/* ── Animazioni ────────────────────────────────────────────────── */
	@keyframes lampeggia-pericolo {
		0%, 100% { color: var(--danger); text-shadow: 0 0 8px var(--danger); }
		50%       { color: var(--amber);  text-shadow: none; }
	}

	/* ── Layout principale ─────────────────────────────────────────── */
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

	/* ── Testi informativi ─────────────────────────────────────────── */
	.info {
		margin: 0;
		font-family: var(--font-term);
		font-size: 1.1rem;
		color: var(--muted);
		text-align: center;
		letter-spacing: 0.04em;
	}

	.turn {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}

	.wait {
		color: var(--muted);
	}

	/* ── Banner vittoria / sconfitta ───────────────────────────────── */
	.banner {
		font-family: var(--font-display);
		font-size: 0.85rem;
		letter-spacing: 0.05em;
		padding: 0.6rem 1.4rem;
		border-radius: 4px;
		text-align: center;
	}

	.win {
		background: color-mix(in srgb, var(--green) 15%, var(--inset));
		color: var(--green);
		border: 1px solid var(--green);
		text-shadow: 0 0 10px var(--green);
		box-shadow: 0 0 12px color-mix(in srgb, var(--green) 35%, transparent);
	}

	.lose {
		background: color-mix(in srgb, var(--danger) 15%, var(--inset));
		color: var(--danger);
		border: 1px solid var(--danger);
		text-shadow: 0 0 10px var(--danger);
		box-shadow: 0 0 12px color-mix(in srgb, var(--danger) 35%, transparent);
	}

	/* ── Bottone azione ────────────────────────────────────────────── */
	.btn {
		min-height: 44px;
		padding: 0.55rem 1.4rem;
		border: 2px solid var(--accent);
		border-radius: 4px;
		background: color-mix(in srgb, var(--accent) 18%, var(--panel));
		color: var(--accent);
		font-family: var(--font-ui);
		font-size: 0.78rem;
		font-weight: 700;
		letter-spacing: 0.06em;
		text-transform: uppercase;
		cursor: pointer;
		text-shadow: var(--glow-mag);
		box-shadow: 0 0 10px color-mix(in srgb, var(--accent) 30%, transparent);
		transition: background 0.15s, box-shadow 0.15s;
	}

	.btn:hover {
		background: color-mix(in srgb, var(--accent) 32%, var(--panel));
		box-shadow: 0 0 18px color-mix(in srgb, var(--accent) 55%, transparent);
	}

	/* ── Griglia / tavola ──────────────────────────────────────────── */
	.grid {
		display: grid;
		grid-template-columns: repeat(8, 1fr);
		width: min(92vw, 480px);
		aspect-ratio: 1;
		/* Bordo tavola con glow verde */
		border: 3px solid var(--green);
		border-radius: 4px;
		box-shadow: 0 0 16px color-mix(in srgb, var(--green) 45%, transparent),
		            inset 0 0 8px color-mix(in srgb, var(--green) 15%, transparent);
		overflow: hidden;
	}

	/* ── Caselle ───────────────────────────────────────────────────── */
	.sq {
		border: none;
		padding: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		aspect-ratio: 1;
		/* Garantisce hit-target ≥44px su mobile (griglia 8×8 su min 92vw) */
		min-width: 0;
		min-height: 0;
	}

	/* Casella chiara — viola scuro */
	.sq.light {
		background: #241340;
	}

	/* Casella scura — quasi nero viola */
	.sq.dark {
		background: var(--inset);
	}

	.sq:disabled {
		cursor: default;
	}

	/* Selezione pezzo attivo */
	.sq.selected {
		outline: 3px solid var(--cyan);
		outline-offset: -3px;
		box-shadow: inset 0 0 10px color-mix(in srgb, var(--cyan) 40%, transparent);
	}

	/* Pezzo obbligato a continuare la cattura */
	.sq.must {
		outline: 3px solid var(--amber);
		outline-offset: -3px;
		box-shadow: inset 0 0 10px color-mix(in srgb, var(--amber) 35%, transparent);
	}

	/* ── Pedine ────────────────────────────────────────────────────── */
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

	/* Pedina "bianco" → cyan con glow */
	.piece.white {
		background: radial-gradient(circle at 35% 35%, color-mix(in srgb, var(--cyan) 80%, #fff), color-mix(in srgb, var(--cyan) 40%, var(--panel)));
		border: 2px solid var(--cyan);
		color: var(--bg);
		box-shadow: 0 0 10px var(--cyan), inset 0 0 6px color-mix(in srgb, var(--cyan) 50%, transparent);
		text-shadow: 0 0 6px var(--cyan);
	}

	/* Pedina "nero" → magenta con glow */
	.piece.black {
		background: radial-gradient(circle at 35% 35%, color-mix(in srgb, var(--accent) 70%, #fff), color-mix(in srgb, var(--accent) 35%, var(--panel)));
		border: 2px solid var(--accent);
		color: var(--bg);
		box-shadow: 0 0 10px var(--accent), inset 0 0 6px color-mix(in srgb, var(--accent) 50%, transparent);
		text-shadow: var(--glow-mag);
	}

	/* ── Legenda giocatori ─────────────────────────────────────────── */
	.legend {
		display: flex;
		gap: 1.5rem;
		flex-wrap: wrap;
		justify-content: center;
		font-family: var(--font-term);
		font-size: 1rem;
		color: var(--muted);
		letter-spacing: 0.03em;
	}

	.player {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}

	.player.active {
		color: var(--text);
	}

	/* Pallino colore in legenda */
	.dot {
		width: 0.9rem;
		height: 0.9rem;
		border-radius: 50%;
	}

	.dot.white {
		background: var(--cyan);
		box-shadow: 0 0 6px var(--cyan);
	}

	.dot.black {
		background: var(--accent);
		box-shadow: 0 0 6px var(--accent);
	}

	/* ── Riduzione movimento (accessibilità) ───────────────────────── */
	@media (prefers-reduced-motion: reduce) {
		@keyframes lampeggia-pericolo {
			0%, 100% { color: var(--danger); text-shadow: none; }
		}
		.btn {
			transition: none;
		}
	}
</style>
