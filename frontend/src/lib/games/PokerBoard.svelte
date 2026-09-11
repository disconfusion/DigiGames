<script lang="ts">
	import type { BoardProps } from './board';
	import GameResultOverlay from './GameResultOverlay.svelte';
	import Icon from '$lib/icons/Icon.svelte';
	import {
		rankLabel,
		suitSprite,
		isRedSuit,
		STREET_LABELS,
		BOT_LEVEL_LABELS,
		type Card,
		type PokerAward,
		type PokerGameState,
		type PokerMove,
		type PokerOver,
		type PokerSeat
	} from './poker';

	let { send, event, me }: BoardProps = $props();

	/** Durata del turno lato server: qui serve solo a disegnare la barra che si svuota. */
	const TURN_TOTAL_MS = 30_000;

	let table = $state<PokerGameState | null>(null);
	let over = $state<PokerOver | null>(null);
	let error = $state('');
	let raiseTo = $state(0);
	let deadline = $state(0);
	let now = $state(Date.now());
	let showLog = $state(false);

	$effect(() => {
		const id = setInterval(() => (now = Date.now()), 250);
		return () => clearInterval(id);
	});

	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			const next = e as unknown as PokerGameState;
			table = next;
			if (next.status === 'PLAYING') over = null;
			deadline = next.turnMs > 0 ? Date.now() + next.turnMs : 0;
			// Il cursore del rilancio riparte dal minimo a ogni aggiornamento del tavolo.
			raiseTo = next.you && next.you.minRaiseTo > 0 ? next.you.minRaiseTo : 0;
			error = '';
		} else if (e.type === 'game:over') {
			over = e as unknown as PokerOver;
			deadline = 0;
		} else if (e.type === 'error') {
			error = String((e as { message?: string }).message ?? '');
		}
	});

	const you = $derived(table?.you ?? null);
	const isMyTurn = $derived(!!you?.yourTurn);
	const legal = $derived<PokerMove[]>(you?.legal ?? []);
	const seats = $derived<PokerSeat[]>(table?.seats ?? []);
	const board = $derived<Card[]>(table?.board ?? []);
	const lastHand = $derived(table?.lastHand ?? null);
	const potNow = $derived(
		table ? (table.phase === 'HAND_OVER' && lastHand ? lastHand.pot : table.pot) : 0
	);
	const secondsLeft = $derived(deadline > 0 ? Math.max(0, Math.ceil((deadline - now) / 1000)) : 0);
	const timerPct = $derived(
		deadline > 0 ? Math.max(0, Math.min(100, ((deadline - now) / TURN_TOTAL_MS) * 100)) : 0
	);
	const canRaise = $derived(legal.includes('raise') && !!you && you.maxRaiseTo > you.minRaiseTo);
	/** Fiches che il rilancio aggiunge a quelle già puntate: è quello che si sfila dallo stack. */
	const raiseCost = $derived(you ? Math.max(0, raiseTo - (seatOf(you.seat)?.bet ?? 0)) : 0);
	const isAllInRaise = $derived(!!you && raiseTo >= you.maxRaiseTo);

	function seatOf(id: string | null | undefined): PokerSeat | undefined {
		return id ? seats.find((s) => s.id === id) : undefined;
	}

	function clamp(v: number, min: number, max: number): number {
		return Math.max(min, Math.min(max, v));
	}

	function act(move: PokerMove, amount = 0) {
		if (!isMyTurn) return;
		send({ type: 'action', move, amount });
	}

	function preset(kind: 'half' | 'pot' | 'allin') {
		if (!you || !table) return;
		const target =
			kind === 'allin'
				? you.maxRaiseTo
				: table.currentBet + Math.round(table.pot * (kind === 'half' ? 0.5 : 1));
		raiseTo = clamp(target, you.minRaiseTo, you.maxRaiseTo);
	}

	const startGame = () => send({ type: 'game:start' });

	/** Etichetta del posto: il nome vero per gli umani, quello di fantasia per l'IA. */
	function seatLabel(s: PokerSeat): string {
		return s.you ? `${s.name} (tu)` : s.name;
	}

	function awardLine(): string {
		if (!lastHand || lastHand.awards.length === 0) return '';
		return lastHand.awards
			.map((a: PokerAward) => `${a.name} vince ${a.amount}${a.hand ? ` con ${a.hand.toLowerCase()}` : ''}`)
			.join(' · ');
	}
</script>

<div class="poker">
	{#if over}
		<GameResultOverlay
			result={over.status === 'WON' ? 'win' : 'lose'}
			title={over.status === 'WON' ? 'TAVOLO TUO' : 'FUORI DAL TAVOLO'}
			message={over.winnerName ? `Vince ${over.winnerName}` : ''}
			onPlayAgain={startGame}
			playAgainLabel="Nuovo tavolo"
		>
			<div class="over">
				{#if over.tokensWon > 0}
					<p class="tokens win">
						<Icon name="coin" size={16} /> +{over.tokensWon} Token dal montepremi
					</p>
				{:else if over.tokensLost > 0}
					<p class="tokens lost">
						<Icon name="coin" size={16} /> -{over.tokensLost} Token di buyin
					</p>
				{/if}
				<ol class="ranking">
					{#each over.ranking as row (row.id)}
						<li class:me={row.id === me.username}>
							<span class="place">{row.place}°</span>
							<Icon name={row.bot ? 'bolt' : 'person'} size={14} />
							<span class="who">{row.name}</span>
							<span class="chips">{row.chips}</span>
						</li>
					{/each}
				</ol>
			</div>
		</GameResultOverlay>
	{:else if !table}
		<div class="lobby">
			<h3>Texas Hold'em</h3>
			<p class="hint">
				Sit &amp; Go a eliminazione: si gioca fino all'ultimo stack in piedi, con i bui che
				raddoppiano ogni 8 mani. Quanti posti sono umani e quanti dell'IA lo ha deciso chi ha
				creato la stanza.
			</p>
			<button class="btn primary" onclick={startGame}>Inizia partita</button>
		</div>
	{:else}
		<div class="head">
			<span class="tag">Mano {table.handNo}</span>
			<span class="tag">{STREET_LABELS[table.street] ?? table.street}</span>
			<span class="tag">Bui {table.smallBlind}/{table.bigBlind}</span>
			{#if table.buyin > 0}
				<span class="tag prize">
					<Icon name="coin" size={13} /> Montepremi {table.pool}
				</span>
			{:else}
				<span class="tag friendly">Amichevole (solo fiches)</span>
			{/if}
			{#if seats.some((s) => s.bot)}
				<span class="tag">IA {BOT_LEVEL_LABELS[table.botLevel] ?? table.botLevel}</span>
			{/if}
		</div>

		<div class="felt">
			<div class="pot">
				<span class="pot-label">Piatto</span>
				<span class="pot-value">{potNow}</span>
			</div>
			<div class="community">
				{#each [0, 1, 2, 3, 4] as slot (slot)}
					{#if board[slot] !== undefined}
						{@const c = board[slot]}
						<span class="card" class:red={isRedSuit(c)}>
							<span class="rank">{rankLabel(c)}</span>
							<Icon name={suitSprite(c)} size={16} />
						</span>
					{:else}
						<span class="card slot" aria-hidden="true"></span>
					{/if}
				{/each}
			</div>
			{#if table.phase === 'HAND_OVER' && lastHand}
				<p class="hand-result">
					{awardLine()}
					{#if lastHand.showdown}<span class="showdown-tag">showdown</span>{/if}
				</p>
			{/if}
		</div>

		<div class="seats">
			{#each seats as s (s.id)}
				<div
					class="seat"
					class:active={table.actor === s.id}
					class:folded={s.folded && !s.out}
					class:out={s.out}
					class:mine={s.you}
				>
					<div class="seat-head">
						<Icon name={s.bot ? 'bolt' : 'person'} size={14} title={s.bot ? 'Avversario IA' : 'Giocatore'} />
						<span class="seat-name">{seatLabel(s)}</span>
						{#if s.dealer}<span class="badge dealer" title="Bottone">D</span>{/if}
						{#if s.allIn}<span class="badge allin">all-in</span>{/if}
						{#if s.out}<span class="badge gone">fuori</span>{/if}
					</div>

					<div class="seat-cards">
						{#if s.cards && s.cards.length > 0}
							{#each s.cards as c (c)}
								<span class="card small" class:red={isRedSuit(c)}>
									<span class="rank">{rankLabel(c)}</span>
									<Icon name={suitSprite(c)} size={12} />
								</span>
							{/each}
						{:else if s.hasCards}
							<span class="card small back" aria-label="Carta coperta"></span>
							<span class="card small back" aria-label="Carta coperta"></span>
						{:else}
							<span class="card small slot" aria-hidden="true"></span>
							<span class="card small slot" aria-hidden="true"></span>
						{/if}
					</div>

					<div class="seat-foot">
						<span class="stack">{s.chips} fiches</span>
						{#if s.bet > 0}<span class="bet">punta {s.bet}</span>{/if}
					</div>

					{#if table.actor === s.id && deadline > 0}
						<div class="timer" aria-hidden="true">
							<div class="timer-fill" style="width: {timerPct}%"></div>
						</div>
					{/if}
				</div>
			{/each}
		</div>

		{#if error}
			<p class="error" role="alert"><Icon name="warning" size={14} /> {error}</p>
		{/if}

		{#if you && isMyTurn}
			<div class="actions">
				<div class="turn-line">
					<Icon name="hourglass" size={14} />
					<span>Tocca a te{#if secondsLeft > 0} · {secondsLeft}s{/if}</span>
				</div>
				<div class="buttons">
					{#if legal.includes('check')}
						<button class="btn" onclick={() => act('check')}>Bussa</button>
					{/if}
					{#if legal.includes('call')}
						<button class="btn primary" onclick={() => act('call')}>
							Chiama {you.call}{#if you.call >= you.chips} (all-in){/if}
						</button>
					{/if}
					{#if legal.includes('fold')}
						<button class="btn danger" onclick={() => act('fold')}>Passo</button>
					{/if}
				</div>
				{#if canRaise}
					<div class="raise">
						<div class="raise-row">
							<label for="raiseTo">Rilancia a</label>
							<input
								id="raiseTo"
								type="range"
								min={you.minRaiseTo}
								max={you.maxRaiseTo}
								step="1"
								bind:value={raiseTo}
							/>
							<input
								class="raise-num"
								type="number"
								min={you.minRaiseTo}
								max={you.maxRaiseTo}
								bind:value={raiseTo}
								aria-label="Puntata totale del rilancio"
							/>
						</div>
						<div class="raise-presets">
							<button class="chip-btn" onclick={() => preset('half')}>½ piatto</button>
							<button class="chip-btn" onclick={() => preset('pot')}>Piatto</button>
							<button class="chip-btn" onclick={() => preset('allin')}>All-in</button>
						</div>
						<button
							class="btn accent"
							onclick={() => act('raise', clamp(raiseTo, you.minRaiseTo, you.maxRaiseTo))}
						>
							{isAllInRaise ? 'All-in' : 'Rilancia'} a {raiseTo} <span class="cost">(-{raiseCost})</span>
						</button>
					</div>
				{/if}
			</div>
		{:else if you && table.phase === 'BETTING'}
			<p class="waiting">
				<Icon name="wait" size={14} />
				{seatOf(table.actor)?.name ?? 'Il tavolo'} sta decidendo…
			</p>
		{:else if table.phase === 'HAND_OVER'}
			<p class="waiting"><Icon name="clock" size={14} /> Prossima mano in arrivo…</p>
		{/if}

		<div class="log-box">
			<button class="log-toggle" onclick={() => (showLog = !showLog)} aria-expanded={showLog}>
				<Icon name="letters" size={13} /> {showLog ? 'Nascondi' : 'Mostra'} cronaca del tavolo
			</button>
			{#if showLog}
				<ul class="log">
					{#each table.log as line, i (i)}
						<li>{line}</li>
					{/each}
				</ul>
			{/if}
		</div>
	{/if}
</div>

<style>
	.poker {
		/* Scala del tavolo in un posto solo. Tutto in rem, quindi segue anche lo zoom globale
		   dell'interfaccia (font-size della radice) senza calcoli aggiuntivi. */
		--fs-xs: 0.78rem;
		--fs-sm: 0.88rem;
		--fs-md: 1rem;
		--fs-lg: 1.2rem;
		--fs-xl: 2rem;
		--card-w: 3.8rem;
		--card-h: 5.3rem;
		--card-w-sm: 2.6rem;
		--card-h-sm: 3.6rem;

		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.8rem;
		width: 100%;
		color: var(--text);
		font-family: var(--font-ui, sans-serif);
	}

	/* Lobby */
	.lobby {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.7rem;
		text-align: center;
		max-width: 42rem;
	}
	.lobby h3 {
		margin: 0;
		font-family: var(--font-display, sans-serif);
		font-size: var(--fs-lg);
		letter-spacing: 0.08em;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.hint {
		margin: 0;
		font-size: var(--fs-md);
		line-height: 1.5;
		color: var(--muted);
	}

	/* Intestazione */
	.head {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
		justify-content: center;
	}
	.tag {
		display: inline-flex;
		align-items: center;
		gap: 0.25rem;
		padding: 0.15rem 0.45rem;
		border: 1px solid var(--line);
		border-radius: 4px;
		background: var(--panel);
		font-family: var(--font-term, monospace);
		font-size: var(--fs-sm);
		letter-spacing: 0.04em;
		color: var(--muted);
	}
	.tag.prize {
		border-color: var(--amber);
		color: var(--amber);
	}
	.tag.friendly {
		border-color: var(--green);
		color: var(--green);
	}

	/* Tavolo */
	.felt {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.6rem;
		width: min(100%, 32rem);
		padding: 0.9rem 0.7rem;
		border: 1px solid var(--cyan);
		border-radius: 12px;
		background: var(--inset);
		box-shadow: 0 0 10px rgba(47, 243, 255, 0.15);
	}
	.pot {
		display: flex;
		align-items: baseline;
		gap: 0.4rem;
	}
	.pot-label {
		font-family: var(--font-term, monospace);
		font-size: var(--fs-sm);
		letter-spacing: 0.08em;
		text-transform: uppercase;
		color: var(--muted);
	}
	.pot-value {
		font-family: var(--font-display, sans-serif);
		font-size: var(--fs-xl);
		color: var(--amber);
		text-shadow: 0 0 8px rgba(255, 184, 41, 0.45);
	}
	.community {
		display: flex;
		gap: 0.3rem;
		flex-wrap: wrap;
		justify-content: center;
	}
	.hand-result {
		margin: 0;
		text-align: center;
		font-size: var(--fs-md);
		color: var(--text);
	}
	.showdown-tag {
		margin-left: 0.35rem;
		padding: 0.05rem 0.3rem;
		border-radius: 4px;
		background: var(--accent);
		color: #1a1030;
		font-size: var(--fs-xs);
		text-transform: uppercase;
		letter-spacing: 0.08em;
	}

	/* Carte */
	.card {
		display: inline-flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 1px;
		width: var(--card-w);
		height: var(--card-h);
		border: 1px solid var(--line);
		border-radius: 5px;
		background: #e9e9f2;
		color: #16161f;
	}
	.card .rank {
		font-family: var(--font-term, monospace);
		font-size: var(--fs-lg);
		font-weight: 700;
		line-height: 1;
	}
	.card.red .rank {
		color: #b9123c;
	}
	.card.slot {
		background: transparent;
		border-style: dashed;
		opacity: 0.4;
	}
	.card.back {
		/* Dorso: trama a righe come le carte del mazzo, nessun contenuto leggibile. */
		background: repeating-linear-gradient(
			45deg,
			var(--accent) 0 3px,
			var(--inset) 3px 6px
		);
		border-color: var(--accent);
	}
	.card.small {
		width: var(--card-w-sm);
		height: var(--card-h-sm);
	}
	.card.small .rank {
		font-size: var(--fs-md);
	}

	/* Posti */
	.seats {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(10.5rem, 1fr));
		gap: 0.5rem;
		width: min(100%, 68rem);
	}
	.seat {
		display: flex;
		flex-direction: column;
		gap: 0.35rem;
		padding: 0.5rem;
		border: 1px solid var(--line);
		border-radius: 8px;
		background: var(--panel);
		transition: border-color 0.15s, box-shadow 0.15s, opacity 0.15s;
	}
	.seat.mine {
		border-color: var(--cyan);
	}
	.seat.active {
		border-color: var(--amber);
		box-shadow: 0 0 10px rgba(255, 184, 41, 0.3);
	}
	.seat.folded {
		opacity: 0.5;
	}
	.seat.out {
		opacity: 0.35;
	}
	.seat.out .seat-name {
		text-decoration: line-through;
	}
	.seat-head {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		flex-wrap: wrap;
	}
	.seat-name {
		font-family: var(--font-term, monospace);
		font-size: var(--fs-md);
		font-weight: 600;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		max-width: 9rem;
	}
	.badge {
		padding: 0.05rem 0.3rem;
		border-radius: 4px;
		font-size: var(--fs-xs);
		text-transform: uppercase;
		letter-spacing: 0.06em;
	}
	.badge.dealer {
		background: var(--cyan);
		color: #06121a;
		font-weight: 700;
	}
	.badge.allin {
		background: var(--danger);
		color: #fff;
	}
	.badge.gone {
		background: var(--line);
		color: var(--muted);
	}
	.seat-cards {
		display: flex;
		gap: 0.25rem;
	}
	.seat-foot {
		display: flex;
		flex-wrap: wrap;
		gap: 0.3rem;
		font-family: var(--font-term, monospace);
		font-size: var(--fs-sm);
		color: var(--muted);
	}
	.bet {
		color: var(--amber);
	}
	.timer {
		height: 3px;
		border-radius: 2px;
		background: var(--inset);
		overflow: hidden;
	}
	.timer-fill {
		height: 100%;
		background: var(--amber);
		transition: width 0.25s linear;
	}

	/* Azioni */
	.actions {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		align-items: center;
		width: min(100%, 40rem);
		padding: 0.7rem;
		border: 1px solid var(--amber);
		border-radius: 10px;
		background: var(--panel);
	}
	.turn-line {
		display: flex;
		align-items: center;
		gap: 0.3rem;
		font-family: var(--font-term, monospace);
		font-size: var(--fs-md);
		color: var(--amber);
	}
	.buttons {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		justify-content: center;
		width: 100%;
	}
	.btn {
		min-height: 44px;
		padding: 0.4rem 0.9rem;
		border: 1px solid var(--line);
		border-radius: 6px;
		background: var(--inset);
		color: var(--text);
		font-family: var(--font-term, monospace);
		font-size: var(--fs-md);
		cursor: pointer;
		transition: border-color 0.12s, color 0.12s, box-shadow 0.12s;
	}
	.btn:hover {
		border-color: var(--cyan);
	}
	.btn.primary {
		border-color: var(--cyan);
		color: var(--cyan);
	}
	.btn.accent {
		border-color: var(--accent);
		color: var(--accent);
		width: 100%;
	}
	.btn.danger {
		border-color: var(--danger);
		color: var(--danger);
	}
	.cost {
		color: var(--muted);
		font-size: var(--fs-sm);
	}

	.raise {
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
		width: 100%;
	}
	.raise-row {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		flex-wrap: wrap;
	}
	.raise-row label {
		font-family: var(--font-term, monospace);
		font-size: var(--fs-sm);
		color: var(--muted);
	}
	.raise-row input[type='range'] {
		flex: 1 1 8rem;
		min-width: 6rem;
		accent-color: var(--accent);
	}
	.raise-num {
		width: 5.5rem;
		min-height: 44px;
		padding: 0.25rem 0.4rem;
		border: 1px solid var(--line);
		border-radius: 5px;
		background: var(--inset);
		color: var(--text);
		font-family: var(--font-term, monospace);
	}
	.raise-presets {
		display: flex;
		gap: 0.3rem;
		flex-wrap: wrap;
	}
	.chip-btn {
		min-height: 36px;
		padding: 0.25rem 0.6rem;
		border: 1px solid var(--line);
		border-radius: 999px;
		background: var(--inset);
		color: var(--muted);
		font-family: var(--font-term, monospace);
		font-size: var(--fs-sm);
		cursor: pointer;
	}
	.chip-btn:hover {
		border-color: var(--accent);
		color: var(--accent);
	}

	.waiting,
	.error {
		display: flex;
		align-items: center;
		gap: 0.3rem;
		margin: 0;
		font-family: var(--font-term, monospace);
		font-size: var(--fs-md);
		color: var(--muted);
	}
	.error {
		color: var(--danger);
	}

	/* Cronaca */
	.log-box {
		width: min(100%, 40rem);
	}
	.log-toggle {
		display: flex;
		align-items: center;
		gap: 0.3rem;
		width: 100%;
		min-height: 36px;
		padding: 0.3rem 0.5rem;
		border: 1px dashed var(--line);
		border-radius: 6px;
		background: transparent;
		color: var(--muted);
		font-family: var(--font-term, monospace);
		font-size: var(--fs-sm);
		cursor: pointer;
	}
	.log {
		list-style: none;
		margin: 0.4rem 0 0;
		padding: 0.5rem;
		border: 1px solid var(--line);
		border-radius: 6px;
		background: var(--inset);
		font-family: var(--font-term, monospace);
		font-size: var(--fs-sm);
		color: var(--muted);
		display: flex;
		flex-direction: column;
		gap: 0.2rem;
		max-height: 11rem;
		overflow-y: auto;
	}

	/* Esito */
	.over {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		align-items: center;
	}
	.tokens {
		display: flex;
		align-items: center;
		gap: 0.3rem;
		margin: 0;
		font-family: var(--font-term, monospace);
		font-size: var(--fs-md);
	}
	.tokens.win {
		color: var(--green);
	}
	.tokens.lost {
		color: var(--danger);
	}
	.ranking {
		list-style: none;
		margin: 0;
		padding: 0;
		display: flex;
		flex-direction: column;
		gap: 0.2rem;
		font-family: var(--font-term, monospace);
		font-size: var(--fs-sm);
	}
	.ranking li {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		color: var(--muted);
	}
	.ranking li.me {
		color: var(--cyan);
	}
	.place {
		width: 1.6rem;
		text-align: right;
	}
	.who {
		flex: 1 1 auto;
	}

	@media (max-width: 420px) {
		.poker {
			--card-w: 2.7rem;
			--card-h: 3.8rem;
			--card-w-sm: 2.3rem;
			--card-h-sm: 3.2rem;
		}
		.seats {
			grid-template-columns: repeat(auto-fit, minmax(9.5rem, 1fr));
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.timer-fill {
			transition: none;
		}
	}
</style>
