<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { BoardProps } from './board';
	import GameResultOverlay from './GameResultOverlay.svelte';

	let { send, event, me }: BoardProps = $props();

	type LegalMove = { fr: number; fc: number; tr: number; tc: number; promotion: string | null };
	type GameStatus =
		| 'PLAYING'
		| 'CHECKMATE'
		| 'STALEMATE'
		| 'DRAW_INSUFFICIENT'
		| 'DRAW_REPETITION'
		| 'DRAW_AGREED'
		| 'RESIGNED'
		| 'TIMEOUT';
	type Clock = { timed: boolean; white?: number; black?: number };

	interface GameState {
		board: (string | null)[][];
		currentTurn: string | null;
		seats: Record<string, string>; // email -> "W" | "B"
		status: GameStatus;
		winner: string | null; // "W" | "B" | null
		inCheck: boolean;
		legalMoves: LegalMove[];
		drawOfferBy: string | null; // "W" | "B" | null
		clock: Clock;
		captured: { W: string[]; B: string[] };
	}

	let state = $state<GameState | null>(null);
	let over = $state<{ status: GameStatus; winner: string | null } | null>(null);
	let sel = $state<{ r: number; c: number } | null>(null);
	let pendingPromo = $state<{ from: { r: number; c: number }; to: { r: number; c: number } } | null>(null);

	// Orologio: valori visualizzati (ticchettano localmente tra una mossa e l'altra)
	let dispWhite = $state(0);
	let dispBlack = $state(0);
	let lastTick = 0;
	let claimed = false;

	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			state = e as unknown as GameState;
			sel = null;
			pendingPromo = null;
			const c = (e as unknown as GameState).clock;
			if (c?.timed) {
				dispWhite = c.white ?? 0;
				dispBlack = c.black ?? 0;
				lastTick = Date.now();
				claimed = false;
			}
			if ((e as { status?: string }).status === 'PLAYING') over = null;
		} else if (e.type === 'game:over') {
			over = {
				status: String((e as { status?: string }).status) as GameStatus,
				winner: (e as { winner?: string | null }).winner ?? null
			};
		}
	});

	const turnColor = $derived(state ? (state.seats[state.currentTurn ?? ''] ?? null) : null);

	onMount(() => {
		const id = setInterval(() => {
			if (!state || !state.clock?.timed || state.status !== 'PLAYING') return;
			const now = Date.now();
			const delta = now - lastTick;
			lastTick = now;
			if (turnColor === 'W') dispWhite = Math.max(0, dispWhite - delta);
			else if (turnColor === 'B') dispBlack = Math.max(0, dispBlack - delta);
			const running = turnColor === 'W' ? dispWhite : dispBlack;
			if (running <= 0 && !claimed) {
				claimed = true;
				send({ type: 'clock:claim' });
			}
		}, 250);
		return () => clearInterval(id);
	});
	onDestroy(() => {});

	function fmt(ms: number): string {
		const s = Math.max(0, Math.ceil(ms / 1000));
		return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
	}

	const GLYPH: Record<string, string> = {
		K: '♚', Q: '♛', R: '♜', B: '♝', N: '♞', P: '♟'
	};

	const myColor = $derived(state?.seats?.[me.username] ?? 'W');
	const isMyTurn = $derived(state?.currentTurn === me.username);
	const isPlaying = $derived(state?.status === 'PLAYING');

	// Orientamento: il nero vede la scacchiera ruotata.
	const rowOrder = $derived(myColor === 'B' ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7]);
	const colOrder = $derived(myColor === 'B' ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7]);

	function pieceAt(r: number, c: number): string | null {
		return state?.board?.[r]?.[c] ?? null;
	}
	function mine(p: string | null): boolean {
		return !!p && p[0] === myColor;
	}

	const targets = $derived.by(() => {
		const set = new Set<string>();
		if (sel && state) {
			for (const m of state.legalMoves) {
				if (m.fr === sel.r && m.fc === sel.c) set.add(`${m.tr},${m.tc}`);
			}
		}
		return set;
	});

	function clickCell(r: number, c: number) {
		if (!isPlaying || !isMyTurn || !state || pendingPromo) return;
		const p = pieceAt(r, c);
		if (mine(p)) {
			sel = { r, c };
			return;
		}
		if (sel && targets.has(`${r},${c}`)) {
			const matching = state.legalMoves.filter(
				(m) => m.fr === sel!.r && m.fc === sel!.c && m.tr === r && m.tc === c
			);
			if (matching.some((m) => m.promotion)) {
				pendingPromo = { from: { ...sel }, to: { r, c } };
			} else {
				send({ type: 'move', from: sel, to: { r, c } });
				sel = null;
			}
		}
	}

	function choosePromo(piece: string) {
		if (!pendingPromo) return;
		send({ type: 'move', from: pendingPromo.from, to: pendingPromo.to, promotion: piece });
		pendingPromo = null;
		sel = null;
	}

	const startGame = () => send({ type: 'game:start' });

	function resign() {
		if (confirm('Vuoi davvero abbandonare la partita?')) send({ type: 'resign' });
	}
	const offerDraw = () => send({ type: 'draw:offer' });
	const acceptDraw = () => send({ type: 'draw:accept' });
	const declineDraw = () => send({ type: 'draw:decline' });

	const turnLabel = $derived(state?.currentTurn === me.username ? 'Tocca a te' : "Tocca all'avversario");
	const myColorLabel = $derived(myColor === 'W' ? 'Bianco' : 'Nero');

	// Offerta di patta ricevuta dall'avversario?
	const incomingDraw = $derived(!!state?.drawOfferBy && state.drawOfferBy !== myColor && isPlaying);
	const myDrawPending = $derived(state?.drawOfferBy === myColor && isPlaying);

	function overMessage(o: { status: GameStatus; winner: string | null }): string {
		const iWon = o.winner === myColor;
		switch (o.status) {
			case 'CHECKMATE':
				return iWon ? 'Scacco matto — hai vinto!' : 'Scacco matto — hai perso';
			case 'RESIGNED':
				return iWon ? 'L\'avversario ha abbandonato — hai vinto!' : 'Hai abbandonato';
			case 'TIMEOUT':
				return iWon ? 'Tempo scaduto per l\'avversario — hai vinto!' : 'Tempo scaduto — hai perso';
			case 'STALEMATE':
				return 'Stallo — patta!';
			case 'DRAW_INSUFFICIENT':
				return 'Patta — materiale insufficiente';
			case 'DRAW_REPETITION':
				return 'Patta — ripetizione di posizione';
			case 'DRAW_AGREED':
				return 'Patta concordata';
			default:
				return 'Partita terminata';
		}
	}
	const overClass = $derived(
		over ? (over.winner === myColor ? 'win' : over.winner ? 'lose' : 'draw') : ''
	);

	// Orologi orientati: in basso il mio, in alto l'avversario
	const topClock = $derived(myColor === 'W' ? dispBlack : dispWhite);
	const bottomClock = $derived(myColor === 'W' ? dispWhite : dispBlack);
	const topRunning = $derived(turnColor != null && turnColor !== myColor && isPlaying);
	const bottomRunning = $derived(turnColor === myColor && isPlaying);

	// Pezzi mangiati. captured[X] = pezzi catturati dal colore X (cioè pezzi avversari).
	const oppColor = $derived(myColor === 'W' ? 'B' : 'W');
	const myCaptured = $derived(state?.captured?.[myColor as 'W' | 'B'] ?? []); // pezzi avversari che ho preso
	const oppCaptured = $derived(state?.captured?.[oppColor as 'W' | 'B'] ?? []); // miei pezzi presi dall'avversario
</script>

<div class="chess">
	<div class="header">
		{#if !state}
			<p class="info">In attesa del secondo giocatore…</p>
			<button class="btn" onclick={startGame}>Inizia partita</button>
		{:else if over}
			<GameResultOverlay
				result={(overClass || 'draw') as 'win' | 'lose' | 'draw'}
				message={overMessage(over)}
				onPlayAgain={startGame}
			/>
		{:else if isPlaying}
			<p class="info" class:turn={isMyTurn}>
				{turnLabel} · sei il {myColorLabel}
				{#if state.inCheck}<span class="check-badge">SCACCO!</span>{/if}
			</p>
		{/if}
	</div>

	{#if state}
		<div class="player-bar">
			{#if state.clock?.timed}
				<div class="clock" class:running={topRunning} class:low={topClock < 30000}>
					⏱ {fmt(topClock)} <span class="clock-who">avversario</span>
				</div>
			{:else}
				<span class="clock-who">avversario</span>
			{/if}
			{#if oppCaptured.length}
				<div class="captured">
					{#each oppCaptured as t, i (i)}
						<span class="cap" class:white={myColor === 'W'} class:black={myColor === 'B'}>{GLYPH[t]}</span>
					{/each}
				</div>
			{/if}
		</div>

		<div class="grid">
			{#each rowOrder as r (r)}
				{#each colOrder as c (c)}
					{@const piece = pieceAt(r, c)}
					{@const dark = (r + c) % 2 === 1}
					{@const isSel = sel?.r === r && sel?.c === c}
					{@const isTarget = targets.has(`${r},${c}`)}
					<button
						class="sq"
						class:dark
						class:light={!dark}
						class:sel={isSel}
						disabled={!isPlaying || !isMyTurn || !!over}
						onclick={() => clickCell(r, c)}
						aria-label="Casa {r},{c}"
					>
						{#if piece}
							<span class="piece" class:white={piece[0] === 'W'} class:black={piece[0] === 'B'}>
								{GLYPH[piece[1]]}
							</span>
						{/if}
						{#if isTarget}<span class="hint" class:capture={!!piece}></span>{/if}
					</button>
				{/each}
			{/each}
		</div>

		<div class="player-bar">
			{#if state.clock?.timed}
				<div class="clock" class:running={bottomRunning} class:low={bottomClock < 30000}>
					⏱ {fmt(bottomClock)} <span class="clock-who">tu</span>
				</div>
			{:else}
				<span class="clock-who">tu</span>
			{/if}
			{#if myCaptured.length}
				<div class="captured">
					{#each myCaptured as t, i (i)}
						<span class="cap" class:white={oppColor === 'W'} class:black={oppColor === 'B'}>{GLYPH[t]}</span>
					{/each}
				</div>
			{/if}
		</div>

		{#if pendingPromo}
			<div class="promo">
				<span>Promuovi a:</span>
				{#each ['Q', 'R', 'B', 'N'] as pc (pc)}
					<button class="promo-btn" onclick={() => choosePromo(pc)}>
						<span class="piece" class:white={myColor === 'W'} class:black={myColor === 'B'}>
							{GLYPH[pc]}
						</span>
					</button>
				{/each}
			</div>
		{/if}

		{#if incomingDraw}
			<div class="draw-offer">
				<span>L'avversario offre patta</span>
				<button class="mini accept" onclick={acceptDraw}>Accetta</button>
				<button class="mini decline" onclick={declineDraw}>Rifiuta</button>
			</div>
		{/if}

		{#if isPlaying && !pendingPromo}
			<div class="controls">
				<button class="ctrl" onclick={offerDraw} disabled={myDrawPending}>
					{myDrawPending ? 'Patta offerta…' : 'Offri patta'}
				</button>
				<button class="ctrl resign" onclick={resign}>Abbandona</button>
			</div>
		{/if}
	{/if}
</div>

<style>
	/* ── Contenitore principale ── */
	.chess {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.85rem;
		padding: 0.5rem;
	}

	/* ── Header / info turno ── */
	.header {
		min-height: 2.5rem;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
	}
	.info {
		margin: 0;
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		font-size: 0.85rem;
		color: var(--muted);
		letter-spacing: 0.05em;
	}
	.info.turn {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
		font-weight: 600;
	}

	/* ── Badge scacco ── */
	.check-badge {
		background: var(--danger);
		color: var(--bg);
		padding: 0.1rem 0.5rem;
		border-radius: 4px;
		font-family: var(--font-display, 'Press Start 2P', monospace);
		font-size: 0.65rem;
		margin-left: 0.4rem;
		text-shadow: none;
		box-shadow: 0 0 8px var(--danger);
	}

	/* ── Banner fine partita ── */
	.banner {
		font-family: var(--font-display, 'Press Start 2P', monospace);
		font-size: 0.85rem;
		font-weight: 700;
		padding: 0.6rem 1.2rem;
		border-radius: 6px;
		letter-spacing: 0.05em;
		text-align: center;
	}
	.win  { background: color-mix(in srgb, var(--green) 18%, var(--inset)); color: var(--green);  border: 1px solid var(--green);  box-shadow: 0 0 10px color-mix(in srgb, var(--green) 40%, transparent); }
	.lose { background: color-mix(in srgb, var(--danger) 18%, var(--inset)); color: var(--danger); border: 1px solid var(--danger); box-shadow: 0 0 10px color-mix(in srgb, var(--danger) 40%, transparent); }
	.draw { background: color-mix(in srgb, var(--amber) 18%, var(--inset)); color: var(--amber);  border: 1px solid var(--amber);  box-shadow: 0 0 10px color-mix(in srgb, var(--amber) 40%, transparent); }

	/* ── Bottone primario ── */
	.btn {
		min-height: 44px;
		padding: 0.55rem 1.4rem;
		border: 1px solid var(--accent);
		border-radius: 6px;
		background: color-mix(in srgb, var(--accent) 18%, var(--panel));
		color: var(--accent);
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		font-size: 0.82rem;
		font-weight: 700;
		letter-spacing: 0.08em;
		cursor: pointer;
		text-shadow: var(--glow-mag);
		box-shadow: 0 0 8px color-mix(in srgb, var(--accent) 35%, transparent);
		transition: box-shadow 0.2s;
	}
	.btn:hover {
		box-shadow: 0 0 16px color-mix(in srgb, var(--accent) 60%, transparent);
	}

	/* ── Griglia scacchiera ── */
	.grid {
		display: grid;
		grid-template-columns: repeat(8, 1fr);
		width: min(92vw, 460px);
		aspect-ratio: 1;
		/* Bordo neon cyan come da specifica */
		border: 3px solid var(--cyan);
		box-shadow: 0 0 12px color-mix(in srgb, var(--cyan) 50%, transparent),
		            0 0 24px color-mix(in srgb, var(--cyan) 25%, transparent);
		border-radius: 4px;
		overflow: hidden;
	}

	/* ── Caselle ── */
	.sq {
		position: relative;
		border: none;
		padding: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		aspect-ratio: 1;
		/* Hit target ≥44px garantito dalla dimensione della griglia (460/8≈57px) */
	}
	/* Casella chiara → viola scuro, casella scura → quasi-nero */
	.sq.light { background: #241340; }
	.sq.dark  { background: var(--inset); }
	.sq:disabled { cursor: default; }

	/* Selezione → outline neon cyan */
	.sq.sel {
		outline: 3px solid var(--cyan);
		outline-offset: -3px;
		box-shadow: inset 0 0 10px color-mix(in srgb, var(--cyan) 30%, transparent);
	}

	/* ── Pezzi ── */
	.piece {
		font-size: clamp(1.3rem, 6vw, 2rem);
		line-height: 1;
	}
	/* Pezzi bianchi → neon cyan */
	.piece.white {
		color: var(--cyan);
		text-shadow: 0 0 10px var(--cyan), 0 0 4px var(--cyan);
	}
	/* Pezzi neri → neon magenta */
	.piece.black {
		color: var(--accent);
		text-shadow: var(--glow-mag), 0 0 4px var(--accent);
	}

	/* ── Suggerimenti mosse legali ── */
	/* Punto centrale per mossa libera */
	.hint {
		position: absolute;
		width: 28%;
		height: 28%;
		border-radius: 50%;
		background: color-mix(in srgb, var(--cyan) 65%, transparent);
		box-shadow: 0 0 6px var(--cyan);
		pointer-events: none;
	}
	/* Anello per cattura */
	.hint.capture {
		width: 86%;
		height: 86%;
		background: transparent;
		border: 4px solid color-mix(in srgb, var(--cyan) 70%, transparent);
		box-shadow: 0 0 6px var(--cyan);
		border-radius: 50%;
	}

	/* ── Promozione pedone ── */
	.promo {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		background: var(--panel);
		border: 1px solid var(--line);
		padding: 0.5rem 0.8rem;
		border-radius: 8px;
		color: var(--muted);
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		font-size: 0.8rem;
	}
	.promo-btn {
		width: 2.6rem;
		height: 2.6rem;
		min-height: 44px;
		min-width: 44px;
		border: 1px solid var(--line);
		border-radius: 6px;
		background: var(--inset);
		cursor: pointer;
		transition: border-color 0.15s, box-shadow 0.15s;
	}
	.promo-btn:hover {
		border-color: var(--cyan);
		box-shadow: 0 0 8px color-mix(in srgb, var(--cyan) 40%, transparent);
	}

	/* ── Orologi ── */
	.clock {
		font-family: ui-monospace, 'VT323', monospace;
		font-size: 1.25rem;
		font-weight: 700;
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 6px;
		padding: 0.3rem 0.8rem;
		color: var(--muted);
		letter-spacing: 0.06em;
		transition: border-color 0.3s, color 0.3s, box-shadow 0.3s;
	}
	/* Orologio in corso → bordo + testo neon */
	.clock.running {
		color: var(--text);
		border-color: var(--cyan);
		box-shadow: 0 0 8px color-mix(in srgb, var(--cyan) 40%, transparent);
	}
	/* Tempo basso (< 30s) → lampeggio rosso */
	.clock.running.low {
		color: var(--danger);
		border-color: var(--danger);
		box-shadow: 0 0 8px color-mix(in srgb, var(--danger) 50%, transparent);
		animation: blink-danger 1s steps(2, start) infinite;
	}
	@media (prefers-reduced-motion: reduce) {
		.clock.running.low {
			animation: none;
			/* Usa solo colore statico per indicare urgenza */
			border-color: var(--danger);
			color: var(--danger);
		}
	}
	.clock-who {
		font-size: 0.72rem;
		font-weight: 400;
		opacity: 0.65;
		font-family: var(--font-ui, 'Orbitron', sans-serif);
	}

	/* ── Barra giocatori ── */
	.player-bar {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		width: min(92vw, 460px);
		flex-wrap: wrap;
	}

	/* ── Pezzi catturati ── */
	.captured {
		display: flex;
		flex-wrap: wrap;
		gap: 0.05rem;
		font-size: 1.1rem;
		line-height: 1;
	}
	/* Pezzi catturati di colore bianco → cyan */
	.cap.white {
		color: var(--cyan);
		text-shadow: 0 0 6px var(--cyan);
	}
	/* Pezzi catturati di colore nero → accent sfumato */
	.cap.black {
		color: var(--muted);
	}

	/* ── Animazione lampeggio pericolo ── */
	@keyframes blink-danger {
		50% { opacity: 0.45; }
	}

	/* ── Controlli partita ── */
	.controls {
		display: flex;
		gap: 0.6rem;
		flex-wrap: wrap;
		justify-content: center;
	}
	.ctrl {
		min-height: 44px;
		padding: 0.45rem 1rem;
		border: 1px solid var(--line);
		border-radius: 6px;
		background: var(--panel);
		color: var(--text);
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		font-size: 0.8rem;
		letter-spacing: 0.04em;
		cursor: pointer;
		transition: border-color 0.2s, box-shadow 0.2s;
	}
	.ctrl:hover:not(:disabled) {
		border-color: var(--cyan);
		box-shadow: 0 0 8px color-mix(in srgb, var(--cyan) 35%, transparent);
	}
	.ctrl:disabled {
		opacity: 0.5;
		cursor: default;
	}
	/* Bottone abbandona → tono danger */
	.ctrl.resign {
		background: color-mix(in srgb, var(--danger) 15%, var(--panel));
		border-color: var(--danger);
		color: var(--danger);
		box-shadow: 0 0 6px color-mix(in srgb, var(--danger) 30%, transparent);
	}
	.ctrl.resign:hover:not(:disabled) {
		box-shadow: 0 0 14px color-mix(in srgb, var(--danger) 55%, transparent);
	}

	/* ── Offerta di patta ── */
	.draw-offer {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		background: color-mix(in srgb, var(--amber) 12%, var(--inset));
		border: 1px solid var(--amber);
		border-radius: 8px;
		padding: 0.4rem 0.8rem;
		font-size: 0.85rem;
		color: var(--amber);
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		box-shadow: 0 0 8px color-mix(in srgb, var(--amber) 25%, transparent);
	}
	.mini {
		min-height: 44px;
		padding: 0.35rem 0.75rem;
		border: 1px solid transparent;
		border-radius: 6px;
		cursor: pointer;
		font-family: var(--font-ui, 'Orbitron', sans-serif);
		font-size: 0.78rem;
		font-weight: 700;
		letter-spacing: 0.04em;
		transition: box-shadow 0.2s;
	}
	.mini.accept {
		background: color-mix(in srgb, var(--green) 20%, var(--panel));
		border-color: var(--green);
		color: var(--green);
	}
	.mini.accept:hover {
		box-shadow: 0 0 10px color-mix(in srgb, var(--green) 45%, transparent);
	}
	.mini.decline {
		background: color-mix(in srgb, var(--muted) 20%, var(--panel));
		border-color: var(--muted);
		color: var(--muted);
	}
	.mini.decline:hover {
		border-color: var(--text);
		color: var(--text);
	}
</style>
