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
	/* ── Contenitore principale ── */
	.tris {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
		padding: 1rem;
	}

	/* ── Header con stato partita ── */
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
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		font-size: 0.85rem;
		text-align: center;
		letter-spacing: 0.05em;
	}

	.turn {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}

	/* ── Banner risultato ── */
	.banner {
		font-family: var(--font-display, 'Press Start 2P', monospace);
		font-size: 0.9rem;
		padding: 0.6rem 1.4rem;
		border-radius: 4px;
		text-align: center;
		letter-spacing: 0.05em;
	}

	.win {
		background: color-mix(in srgb, var(--green) 15%, var(--bg));
		color: var(--green);
		border: 1px solid var(--green);
		text-shadow: 0 0 8px var(--green), 0 0 16px var(--green);
	}

	.lose {
		background: color-mix(in srgb, var(--danger) 15%, var(--bg));
		color: var(--danger);
		border: 1px solid var(--danger);
		text-shadow: 0 0 8px var(--danger);
	}

	.draw {
		background: color-mix(in srgb, var(--amber) 15%, var(--bg));
		color: var(--amber);
		border: 1px solid var(--amber);
		text-shadow: 0 0 8px var(--amber);
	}

	/* ── Pulsante azione ── */
	.btn {
		min-height: 44px;
		padding: 0.55rem 1.4rem;
		border: 2px solid var(--accent);
		border-radius: 4px;
		background: color-mix(in srgb, var(--accent) 12%, var(--panel));
		color: var(--accent);
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		font-size: 0.8rem;
		font-weight: 700;
		letter-spacing: 0.08em;
		text-transform: uppercase;
		cursor: pointer;
		text-shadow: var(--glow-mag);
		box-shadow: 0 0 8px color-mix(in srgb, var(--accent) 40%, transparent);
		transition: background 0.15s, box-shadow 0.15s;
	}

	.btn:hover:not(:disabled) {
		background: color-mix(in srgb, var(--accent) 22%, var(--panel));
		box-shadow: 0 0 14px color-mix(in srgb, var(--accent) 60%, transparent);
	}

	/* ── Griglia 3×3 ── */
	.grid {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 6px;
		/* Gabbia con bordo e glow cyan */
		background: var(--line);
		padding: 6px;
		border-radius: 6px;
		border: 2px solid var(--cyan);
		box-shadow:
			0 0 10px color-mix(in srgb, var(--cyan) 50%, transparent),
			inset 0 0 8px color-mix(in srgb, var(--cyan) 20%, transparent);
	}

	/* ── Cella singola ── */
	.cell {
		width: 5.5rem;
		height: 5.5rem;
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
		/* Font display per i glifi X/O */
		font-family: var(--font-display, 'Press Start 2P', monospace);
		font-size: 2rem;
		line-height: 1;
		/* Fondo scuro della cella */
		background: var(--bg);
		border: 1px solid color-mix(in srgb, var(--cyan) 35%, transparent);
		border-radius: 4px;
		cursor: pointer;
		color: var(--muted);
		transition: border-color 0.15s, background 0.15s;
	}

	.cell:not(:disabled):not(.x):not(.o):hover {
		border-color: var(--cyan);
		background: color-mix(in srgb, var(--cyan) 8%, var(--bg));
	}

	.cell:disabled {
		cursor: default;
	}

	/* Glifo X — var(--cyan) con glow cyan */
	.cell.x {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
		border-color: color-mix(in srgb, var(--cyan) 50%, transparent);
	}

	/* Glifo O — var(--accent) con glow magenta */
	.cell.o {
		color: var(--accent);
		text-shadow: var(--glow-mag);
		border-color: color-mix(in srgb, var(--accent) 50%, transparent);
	}

	/* ── Legenda giocatori ── */
	.legend {
		display: flex;
		gap: 1.5rem;
		flex-wrap: wrap;
		justify-content: center;
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		font-size: 0.75rem;
		color: var(--muted);
		letter-spacing: 0.05em;
	}

	.player {
		display: flex;
		align-items: center;
		gap: 0.35rem;
	}

	.player.active {
		color: var(--text);
	}

	/* Giocatore attivo: strong (il simbolo) eredita il glow dal colore */
	.player.active strong {
		text-shadow: var(--glow-cyan);
	}

	/* ── Ridotto moto per accessibilità ── */
	@media (prefers-reduced-motion: reduce) {
		.btn,
		.cell {
			transition: none;
		}
	}

	/* ── Responsive mobile ── */
	@media (max-width: 480px) {
		.cell {
			width: 4.5rem;
			height: 4.5rem;
			font-size: 1.6rem;
		}

		.banner {
			font-size: 0.75rem;
		}
	}
</style>
