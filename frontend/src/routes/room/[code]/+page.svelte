<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { connectRoom, type RoomEvent, type RoomConnection } from '$lib/ws';
	import { BOARDS } from '$lib/games/registry';
	import { parseAvatar, renderAvatar } from '$lib/avatar';
	import Icon from '$lib/icons/Icon.svelte';

	type RoomView = { code: string; gameSlug: string; hostEmail: string; players: number; maxPlayers: number };
	type UserInfo = { displayName: string; avatar: string | null; companion?: string | null; house?: string | null };

	const SYSTEM = new Set(['player:joined', 'player:left', 'chat']);
	const code: string = page.params.code ?? '';
	const meUsername = auth.session?.username ?? '';

	let conn: RoomConnection | null = null;
	let wsStatus = $state<'connected' | 'reconnecting' | 'closed'>('reconnecting');
	let connected = $state(false);
	let players = $state(0);
	let playerNames = $state<string[]>([]);
	let room = $state<RoomView | null>(null);
	let loadError = $state('');
	let gameEvent = $state<RoomEvent | null>(null);

	let userInfo = $state<Record<string, UserInfo>>({});
	let bubbles = $state<Record<string, { text: string; key: number }>>({});
	let bubbleSeq = 0;

	let log = $state<string[]>([]);
	let chatText = $state('');
	const push = (line: string) => (log = [...log, line]);

	const Board = $derived(room ? BOARDS[room.gameSlug] : undefined);
	const opponents = $derived(playerNames.filter((u) => u !== meUsername));
	const isHost = $derived(!!room && room.hostEmail === meUsername);

	const face = (u: string) => renderAvatar(parseAvatar(userInfo[u]?.avatar ?? null));
	const nameOf = (u: string) => (u === meUsername ? 'Tu' : (userInfo[u]?.displayName ?? u));
	// Mappa username → displayName passata ai board, così mostrano il nome invece dell'username.
	const nameMap = $derived(
		Object.fromEntries(Object.entries(userInfo).map(([u, i]) => [u, i.displayName]))
	);
	const companionOf = (u: string) => userInfo[u]?.companion ?? '';
	const houseOf = (u: string) => userInfo[u]?.house ?? '';

	function addPlayer(u: string) {
		if (u && !playerNames.includes(u)) playerNames = [...playerNames, u];
	}
	function removePlayer(u: string) {
		playerNames = playerNames.filter((p) => p !== u);
	}

	function showBubble(from: string, text: string) {
		const key = ++bubbleSeq;
		bubbles = { ...bubbles, [from]: { text, key } };
		setTimeout(() => {
			if (bubbles[from]?.key === key) {
				const next = { ...bubbles };
				delete next[from];
				bubbles = next;
			}
		}, 4500);
	}

	function handle(e: RoomEvent) {
		switch (e.type) {
			case 'player:joined':
				players = Number(e.players);
				addPlayer(String(e.username));
				if (e.username === meUsername) connected = true;
				push(`▶ ${e.username} è entrato`);
				break;
			case 'player:left':
				players = Number(e.players);
				removePlayer(String(e.username));
				push(`◀ ${e.username} è uscito`);
				break;
			case 'room:members': {
				// Snapshot completo dei membri (inviato al join/refresh): sostituisce la lista locale,
				// così chi entra dopo vede subito tutti i giocatori già presenti.
				const list = Array.isArray(e.players) ? (e.players as string[]) : [];
				playerNames = meUsername && !list.includes(meUsername) ? [...list, meUsername] : list;
				players = playerNames.length;
				break;
			}
			case 'chat':
				push(`${e.from}: ${e.text}`);
				showBubble(String(e.from), String(e.text));
				break;
			case 'room:closed':
				// L'host ha chiuso la stanza: torna alla home.
				leaving = true;
				conn?.close();
				goto('/');
				break;
			default:
				if (e.type === 'error') push(`⚠ ${e.message}`);
				if (!SYSTEM.has(e.type)) gameEvent = e;
		}
	}

	const sendMsg = (msg: Record<string, unknown>) => conn?.send(msg);

	function sendChat(ev: SubmitEvent) {
		ev.preventDefault();
		if (conn && chatText.trim()) {
			conn.send({ type: 'chat', text: chatText });
			chatText = '';
		}
	}

	let leaving = false;

	// Uscita esplicita: libera lo slot lato server (msg "leave") anche a partita in corso,
	// poi chiude il socket e torna alla home.
	function leave() {
		if (leaving) return;
		leaving = true;
		conn?.send({ type: 'leave' });
		conn?.close();
		goto('/');
	}

	// Solo host: chiude la stanza per tutti i partecipanti.
	function closeRoom() {
		if (leaving) return;
		if (!confirm('Chiudere la stanza per tutti i giocatori?')) return;
		leaving = true;
		conn?.send({ type: 'room:close' });
		conn?.close();
		goto('/');
	}

	async function loadAvatars() {
		try {
			const me = await api<{ username: string; displayName: string; avatar: string | null; companion: string | null; house: string | null }>('/api/me');
			const others = await api<{ username: string; displayName: string; avatar: string | null; companion: string | null; house: string | null }[]>(
				'/api/users'
			);
			const map: Record<string, UserInfo> = {};
			map[me.username] = { displayName: me.displayName, avatar: me.avatar, companion: me.companion, house: me.house };
			for (const u of others) map[u.username] = { displayName: u.displayName, avatar: u.avatar, companion: u.companion, house: u.house };
			userInfo = map;
		} catch {
			// avatar non disponibili: si userà il volto di default
		}
	}

	onMount(async () => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		if (meUsername) addPlayer(meUsername);
		const token: string = auth.session.token ?? '';
		await loadAvatars();
		try {
			room = await api<RoomView>(`/api/rooms/${code}`);
		} catch {
			loadError = 'Stanza non trovata o non più disponibile.';
			return;
		}
		conn = connectRoom(code, token, handle, (s) => {
			wsStatus = s;
			if (s === 'reconnecting') push('⟳ Riconnessione in corso…');
			if (s === 'closed') push('✖ Connessione persa. Ricarica la pagina.');
		});
	});
	onDestroy(() => conn?.close());
</script>

<div class="head">
	<h1>Stanza {code}</h1>
	<div class="actions">
		{#if isHost}
			<button class="close-room" onclick={closeRoom}>Chiudi stanza</button>
		{/if}
		<button class="leave" onclick={leave}>Esci</button>
	</div>
</div>

{#if loadError}
	<p class="error">{loadError}</p>
	<button onclick={() => goto('/')}>Torna alla home</button>
{:else}
	<p class="status">
		{#if connected}<Icon name="online" size={12} /> connesso{:else}<Icon name="wait" size={12} /> connessione…{/if} · giocatori: {players}/{room?.maxPlayers ?? '?'}
	</p>

	{#snippet seat(username: string, side: 'left' | 'right')}
		<div class="seat {side}" class:me-seat={username === meUsername}>
			<span class="pnum">{side === 'left' ? 'P1' : 'P2'}</span>
			<div class="avatar-box">
				{#if bubbles[username]}
					<div class="bubble {side}">{bubbles[username].text}</div>
				{/if}
				<pre class="face">{face(username)}</pre>
				{#if houseOf(username)}
					<div class="seat-house"><Icon name={houseOf(username)} size={22} title="Casata" /></div>
				{/if}
				{#if companionOf(username)}
					<div class="seat-companion"><Icon name={companionOf(username)} size={26} title="Companion" /></div>
				{/if}
			</div>
			<span class="seat-name" class:me={username === meUsername}>{nameOf(username)}</span>
		</div>
	{/snippet}

	<div class="arena">
		<div class="rail">
			{@render seat(meUsername, 'left')}
		</div>

		<section class="panel board-panel">
			{#if Board && auth.session}
				<Board send={sendMsg} event={gameEvent} me={auth.session} names={nameMap} />
			{:else}
				<p class="muted">Gioco "{room?.gameSlug}" non disponibile.</p>
			{/if}
		</section>

		<div class="rail">
			{#if opponents.length === 0}
				<div class="seat right empty">
					<div class="avatar-box dim"><pre class="face">{renderAvatar(parseAvatar(null))}</pre></div>
					<span class="seat-name">In attesa…</span>
				</div>
			{:else}
				{#each opponents as o (o)}
					{@render seat(o, 'right')}
				{/each}
			{/if}
		</div>
	</div>

	<section class="panel term-panel">
		<h2>&gt; EVENTI &amp; CHAT</h2>
		<ul class="log">
			{#each log as line, i (i)}
				<li>{line}</li>
			{/each}
			<li class="cursor-line">&gt;&nbsp;<span class="caret">▮</span></li>
		</ul>
		<form onsubmit={sendChat}>
			<input placeholder="Scrivi un messaggio…" bind:value={chatText} disabled={!connected} />
			<button type="submit" disabled={!connected}>Invia</button>
		</form>
	</section>
{/if}

<style>
	.head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		flex-wrap: wrap;
		gap: 0.5rem;
	}
	.head h1 {
		font-family: var(--font-display);
		font-size: clamp(0.85rem, 3.5vw, 1.25rem);
		text-transform: uppercase;
		letter-spacing: 0.08em;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.actions {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
	}
	.leave,
	.close-room {
		background: transparent;
		color: var(--danger);
		border: 2px solid var(--danger);
		border-radius: 4px;
		padding: 0.5rem 0.9rem;
		font-family: var(--font-ui);
		font-weight: 700;
		text-transform: uppercase;
		letter-spacing: 0.05em;
		cursor: pointer;
		min-height: 44px;
		transition: box-shadow 0.12s;
	}
	.close-room {
		color: var(--amber);
		border-color: var(--amber);
	}
	.leave:hover {
		box-shadow: 0 0 14px rgba(255, 82, 119, 0.5);
	}
	.close-room:hover {
		box-shadow: 0 0 14px rgba(255, 207, 63, 0.5);
	}
	@media (prefers-reduced-motion: reduce) {
		.leave,
		.close-room {
			transition: none;
		}
	}
	.status {
		color: var(--muted);
		font-family: var(--font-term);
		font-size: 1.05rem;
	}
	.arena {
		display: flex;
		align-items: flex-start;
		justify-content: center;
		gap: 0.75rem;
		margin-bottom: 1.25rem;
	}
	.rail {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		padding-top: 1rem;
	}
	.board-panel {
		flex: 1;
		min-width: 0;
		margin: 0;
	}
	.seat {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.3rem;
		width: 5.5rem;
	}
	.pnum {
		font-family: var(--font-display);
		font-size: 0.6rem;
		color: var(--amber);
		text-shadow: 0 0 8px var(--amber);
	}
	.me-seat .pnum {
		color: var(--accent);
		text-shadow: var(--glow-mag);
	}
	.avatar-box {
		position: relative;
		background: var(--panel);
		border: 2px solid var(--amber);
		border-radius: 12px;
		padding: 0.4rem 0.5rem;
		box-shadow: 0 0 12px rgba(255, 207, 63, 0.3);
	}
	.me-seat .avatar-box {
		border-color: var(--accent);
		box-shadow: 0 0 12px rgba(255, 46, 136, 0.4);
	}
	.avatar-box.dim {
		border-color: var(--line);
		box-shadow: none;
		opacity: 0.4;
	}
	.seat-companion {
		position: absolute;
		right: -10px;
		bottom: -10px;
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 8px;
		padding: 1px 2px;
		line-height: 0;
	}
	.seat-house {
		position: absolute;
		left: -10px;
		top: -10px;
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 8px;
		padding: 1px 2px;
		line-height: 0;
	}
	.face {
		font-family: ui-monospace, monospace;
		font-size: 0.6rem;
		line-height: 1.05;
		margin: 0;
		color: var(--text);
	}
	.seat-name {
		font-size: 0.8rem;
		color: var(--muted);
		max-width: 5.5rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.seat-name.me {
		color: var(--accent);
		font-weight: 600;
	}
	.bubble {
		position: absolute;
		bottom: 100%;
		margin-bottom: 8px;
		left: 50%;
		transform: translateX(-50%);
		background: #e2e8f0;
		color: #0f172a;
		padding: 0.4rem 0.6rem;
		border-radius: 10px;
		font-size: 0.78rem;
		min-width: 4rem;
		max-width: 11rem;
		width: max-content;
		text-align: center;
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
		z-index: 5;
		animation: pop 0.18s ease-out;
	}
	.bubble::after {
		content: '';
		position: absolute;
		top: 100%;
		left: 50%;
		transform: translateX(-50%);
		border: 7px solid transparent;
		border-top-color: #e2e8f0;
	}
	@keyframes pop {
		from {
			opacity: 0;
			transform: translateX(-50%) scale(0.8);
		}
		to {
			opacity: 1;
			transform: translateX(-50%) scale(1);
		}
	}
	.panel {
		background: var(--panel);
		padding: 1rem 1.25rem;
		border-radius: 12px;
		margin-bottom: 1.25rem;
		border: 1px solid var(--line);
	}
	h2 {
		font-family: var(--font-display);
		font-size: 0.8rem;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
		margin: 0 0 0.5rem;
	}
	/* Pannello eventi+chat in stile terminale CRT */
	.term-panel .log {
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 6px;
		padding: 0.6rem 0.8rem;
	}
	.log {
		list-style: none;
		margin: 0 0 0.75rem;
		padding: 0;
		max-height: 200px;
		overflow-y: auto;
		display: flex;
		flex-direction: column;
		gap: 0.2rem;
		font-family: var(--font-term);
		font-size: 1.05rem;
		color: var(--green);
	}
	.cursor-line {
		color: var(--cyan);
	}
	.caret {
		animation: blink 1.05s steps(1) infinite;
	}
	@keyframes blink {
		50% {
			opacity: 0;
		}
	}
	form {
		display: flex;
		gap: 0.5rem;
	}
	form input {
		flex: 1;
		padding: 0.55rem;
		border-radius: 4px;
		border: 2px solid var(--line);
		background: var(--inset);
		color: var(--text);
		font-family: var(--font-term);
		font-size: 1.1rem;
	}
	form input:focus {
		outline: none;
		border-color: var(--cyan);
		box-shadow: var(--glow-cyan);
	}
	form button {
		background: linear-gradient(180deg, var(--accent), #c01e63);
		color: #fff;
		border: 2px solid var(--amber);
		border-radius: 4px;
		padding: 0.55rem 1rem;
		font-family: var(--font-ui);
		font-weight: 700;
		text-transform: uppercase;
		cursor: pointer;
		min-height: 44px;
		box-shadow: 0 0 14px rgba(255, 46, 136, 0.4);
	}
	button:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.muted {
		color: var(--muted);
	}
	.error {
		color: var(--danger);
		font-family: var(--font-term);
		font-size: 1.1rem;
	}
	@media (prefers-reduced-motion: reduce) {
		.caret {
			animation: none;
		}
	}
	@media (max-width: 680px) {
		.arena {
			flex-direction: column;
			align-items: center;
		}
		.rail {
			flex-direction: row;
			padding-top: 0;
		}
		.board-panel {
			width: 100%;
			order: -1;
		}
	}
</style>
