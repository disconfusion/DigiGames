<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { connectRoom, send, type RoomEvent } from '$lib/ws';
	import { BOARDS } from '$lib/games/registry';

	type RoomView = { code: string; gameSlug: string; players: number; maxPlayers: number };

	const SYSTEM = new Set(['player:joined', 'player:left', 'chat']);
	const code = page.params.code;

	let ws: WebSocket | null = null;
	let connected = $state(false);
	let players = $state(0);
	let room = $state<RoomView | null>(null);
	let loadError = $state('');
	let gameEvent = $state<RoomEvent | null>(null);

	let log = $state<string[]>([]);
	let chatText = $state('');
	const push = (line: string) => (log = [...log, line]);

	const Board = $derived(room ? BOARDS[room.gameSlug] : undefined);

	function handle(e: RoomEvent) {
		switch (e.type) {
			case 'player:joined':
				players = Number(e.players);
				if (e.email === auth.session?.email) connected = true;
				push(`▶ ${e.email} è entrato`);
				break;
			case 'player:left':
				players = Number(e.players);
				push(`◀ ${e.email} è uscito`);
				break;
			case 'chat':
				push(`${e.from}: ${e.text}`);
				break;
			default:
				// Evento di gioco (game:state, game:over, error): inoltralo al tabellone.
				if (e.type === 'error') push(`⚠ ${e.message}`);
				if (!SYSTEM.has(e.type)) gameEvent = e;
		}
	}

	const sendMsg = (msg: Record<string, unknown>) => ws && send(ws, msg);

	function sendChat(ev: SubmitEvent) {
		ev.preventDefault();
		if (ws && chatText.trim()) {
			send(ws, { type: 'chat', text: chatText });
			chatText = '';
		}
	}

	function leave() {
		ws?.close();
		goto('/lobby');
	}

	onMount(async () => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		try {
			room = await api<RoomView>(`/api/rooms/${code}`);
		} catch {
			loadError = 'Stanza non trovata o non più disponibile.';
			return;
		}
		ws = connectRoom(code, auth.session.token, handle);
	});
	onDestroy(() => ws?.close());
</script>

<div class="head">
	<h1>Stanza {code}</h1>
	<button class="leave" onclick={leave}>Esci</button>
</div>

{#if loadError}
	<p class="error">{loadError}</p>
	<button onclick={() => goto('/lobby')}>Torna alla lobby</button>
{:else}
	<p class="status">
		{connected ? '🟢 connesso' : '🟡 connessione…'} · giocatori: {players}/{room?.maxPlayers ?? '?'}
	</p>

	<section class="panel">
		{#if Board && auth.session}
			<Board send={sendMsg} event={gameEvent} me={auth.session} />
		{:else}
			<p class="muted">Gioco "{room?.gameSlug}" non disponibile.</p>
		{/if}
	</section>

	<section class="panel">
		<h2>Eventi & chat</h2>
		<ul class="log">
			{#each log as line, i (i)}
				<li>{line}</li>
			{/each}
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
	.leave {
		background: #475569;
		color: white;
		border: none;
		border-radius: 8px;
		padding: 0.5rem 0.9rem;
		cursor: pointer;
	}
	.status {
		color: var(--muted);
	}
	.panel {
		background: var(--panel);
		padding: 1rem 1.25rem;
		border-radius: 12px;
		margin-bottom: 1.25rem;
	}
	h2 {
		font-size: 1.05rem;
		margin: 0 0 0.5rem;
	}
	.log {
		list-style: none;
		margin: 0 0 0.75rem;
		padding: 0;
		max-height: 200px;
		overflow-y: auto;
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
		font-size: 0.9rem;
	}
	form {
		display: flex;
		gap: 0.5rem;
	}
	form input {
		flex: 1;
		padding: 0.55rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
	}
	form button {
		background: var(--accent);
		color: white;
		border: none;
		border-radius: 8px;
		padding: 0.55rem 1rem;
		cursor: pointer;
	}
	button:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.muted {
		color: var(--muted);
	}
	.error {
		color: #f87171;
	}
</style>
