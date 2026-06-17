<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { BoardProps } from './board';

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
</script>

<div class="chess">
	<div class="header">
		{#if !state}
			<p class="info">In attesa del secondo giocatore…</p>
			<button class="btn" onclick={startGame}>Inizia partita</button>
		{:else if over}
			<div class="banner {overClass}">{overMessage(over)}</div>
			<button class="btn" onclick={startGame}>Nuova partita</button>
		{:else if isPlaying}
			<p class="info" class:turn={isMyTurn}>
				{turnLabel} · sei il {myColorLabel}
				{#if state.inCheck}<span class="check-badge">SCACCO!</span>{/if}
			</p>
		{/if}
	</div>

	{#if state}
		{#if state.clock?.timed}
			<div class="clock" class:running={topRunning} class:low={topClock < 30000}>
				⏱ {fmt(topClock)} <span class="clock-who">avversario</span>
			</div>
		{/if}

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

		{#if state.clock?.timed}
			<div class="clock" class:running={bottomRunning} class:low={bottomClock < 30000}>
				⏱ {fmt(bottomClock)} <span class="clock-who">tu</span>
			</div>
		{/if}

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
	.chess {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.85rem;
		padding: 0.5rem;
	}
	.header {
		min-height: 2.5rem;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
	}
	.info {
		margin: 0;
		color: var(--muted);
	}
	.info.turn {
		color: var(--text);
		font-weight: 600;
	}
	.check-badge {
		background: #7f1d1d;
		color: #fecaca;
		padding: 0.1rem 0.5rem;
		border-radius: 6px;
		font-size: 0.8rem;
		margin-left: 0.4rem;
	}
	.banner {
		font-size: 1.2rem;
		font-weight: 700;
		padding: 0.5rem 1.1rem;
		border-radius: 8px;
	}
	.win { background: #14532d; color: #bbf7d0; }
	.lose { background: #7f1d1d; color: #fecaca; }
	.draw { background: #1e3a5f; color: #bae6fd; }
	.btn {
		padding: 0.55rem 1.3rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: #fff;
		font-weight: 600;
		cursor: pointer;
	}
	.grid {
		display: grid;
		grid-template-columns: repeat(8, 1fr);
		width: min(92vw, 460px);
		aspect-ratio: 1;
		border: 3px solid #1e293b;
		border-radius: 4px;
		overflow: hidden;
	}
	.sq {
		position: relative;
		border: none;
		padding: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		aspect-ratio: 1;
	}
	.sq.light { background: #e7d3b1; }
	.sq.dark { background: #7c5a3a; }
	.sq:disabled { cursor: default; }
	.sq.sel { outline: 3px solid #6366f1; outline-offset: -3px; }
	.piece {
		font-size: clamp(1.3rem, 6vw, 2rem);
		line-height: 1;
	}
	.piece.white {
		color: #f8fafc;
		text-shadow: 0 0 2px #000, 0 0 2px #000;
	}
	.piece.black {
		color: #0f172a;
		text-shadow: 0 0 1px #94a3b8;
	}
	.hint {
		position: absolute;
		width: 28%;
		height: 28%;
		border-radius: 50%;
		background: rgba(99, 102, 241, 0.7);
	}
	.hint.capture {
		width: 86%;
		height: 86%;
		background: transparent;
		border: 4px solid rgba(99, 102, 241, 0.7);
		border-radius: 50%;
	}
	.promo {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		background: var(--panel);
		padding: 0.5rem 0.8rem;
		border-radius: 10px;
		color: var(--muted);
	}
	.promo-btn {
		width: 2.6rem;
		height: 2.6rem;
		border: 1px solid #334155;
		border-radius: 8px;
		background: #7c5a3a;
		cursor: pointer;
	}
	.clock {
		font-family: ui-monospace, monospace;
		font-size: 1.2rem;
		font-weight: 700;
		background: #1e293b;
		border: 1px solid #334155;
		border-radius: 8px;
		padding: 0.3rem 0.8rem;
		color: var(--muted);
	}
	.clock.running {
		color: var(--text);
		border-color: var(--accent);
	}
	.clock.running.low {
		color: #fecaca;
		border-color: #ef4444;
		animation: blink 1s steps(2, start) infinite;
	}
	.clock-who {
		font-size: 0.75rem;
		font-weight: 400;
		opacity: 0.7;
	}
	@keyframes blink {
		50% {
			opacity: 0.55;
		}
	}
	.controls {
		display: flex;
		gap: 0.6rem;
	}
	.ctrl {
		padding: 0.45rem 1rem;
		border: 1px solid #334155;
		border-radius: 8px;
		background: #1e293b;
		color: var(--text);
		cursor: pointer;
		font-size: 0.9rem;
	}
	.ctrl:disabled {
		opacity: 0.6;
		cursor: default;
	}
	.ctrl.resign {
		background: #7f1d1d;
		border-color: #991b1b;
		color: #fecaca;
	}
	.draw-offer {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		background: #1e3a5f;
		border: 1px solid #3b82f6;
		border-radius: 8px;
		padding: 0.4rem 0.8rem;
		font-size: 0.9rem;
	}
	.mini {
		padding: 0.3rem 0.7rem;
		border: none;
		border-radius: 6px;
		cursor: pointer;
		color: white;
		font-size: 0.82rem;
	}
	.mini.accept {
		background: #16a34a;
	}
	.mini.decline {
		background: #475569;
	}
</style>
