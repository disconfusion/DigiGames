<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { GAME_CATALOG, gameLabel } from '$lib/games/catalog';
	import { parseAvatar, renderAvatar } from '$lib/avatar';
	import GameOptions from '$lib/games/GameOptions.svelte';
	import { notifications } from '$lib/notifications.svelte';

	const FEATURES = __WHATSNEW__;
	const SEEN_KEY = 'digiGamesSeenDate';
	const TODAY = new Date().toISOString().slice(0, 10);

	let showWhatsNew = $state(false);

	type RoomView = {
		code: string;
		gameSlug: string;
		hostEmail: string;
		players: number;
		maxPlayers: number;
	};
	type DailyState = {
		masked: string;
		status: 'PLAYING' | 'WON' | 'LOST';
		letterUsed: boolean;
		wordAttemptUsed: boolean;
		won: boolean;
		eliminated: boolean;
	};
	type UserView = { username: string; displayName: string; avatar: string | null; online: boolean };

	let daily = $state<DailyState | null>(null);
	let users = $state<UserView[]>([]);
	let rooms = $state<RoomView[]>([]);
	let error = $state('');

	// Card "Ospita"
	let hostGame = $state('connect4');
	let hostOptions = $state<unknown>(undefined);
	let hosting = $state(false);

	// Card "Invita"
	let inviteGame = $state('connect4');
	let inviteOptions = $state<unknown>(undefined);
	let selected = $state<Set<string>>(new Set());
	let inviting = $state(false);

	// Entra con codice
	let joinCode = $state('');

	const lettersLeft = $derived(daily ? (daily.masked.match(/_/g)?.length ?? 0) : 0);

	const sortedUsers = $derived(
		[...users].sort((a, b) => {
			if (a.online !== b.online) return a.online ? -1 : 1;
			return a.displayName.localeCompare(b.displayName);
		})
	);
	const onlineCount = $derived(users.filter((u) => u.online).length);

	async function load() {
		try {
			daily = await api<DailyState>('/api/daily');
			users = await api<UserView[]>('/api/users');
			await refreshRooms();
		} catch (e) {
			error = (e as Error).message;
		}
	}

	async function refreshRooms() {
		try {
			rooms = await api<RoomView[]>('/api/rooms');
		} catch (e) {
			error = (e as Error).message;
		}
	}

	onMount(() => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		load();
		// Mostra modal novità al primo accesso del giorno
		if (FEATURES.length > 0 && localStorage.getItem(SEEN_KEY) !== TODAY) {
			showWhatsNew = true;
		}
	});

	function closeWhatsNew() {
		localStorage.setItem(SEEN_KEY, TODAY);
		showWhatsNew = false;
	}

	// Aggiorna lista utenti quando qualcuno va online/offline (evento WS)
	let lastPresenceSeen = 0;
	$effect(() => {
		const t = notifications.lastPresenceUpdate;
		if (t > lastPresenceSeen) {
			lastPresenceSeen = t;
			api<UserView[]>('/api/users').then((u) => (users = u)).catch(() => {});
		}
	});

	async function createHost() {
		hosting = true;
		error = '';
		try {
			const r = await api<RoomView>('/api/rooms', {
				method: 'POST',
				body: JSON.stringify({ gameSlug: hostGame, isPrivate: false, options: hostOptions })
			});
			goto(`/room/${r.code}`);
		} catch (e) {
			error = (e as Error).message;
		} finally {
			hosting = false;
		}
	}

	function toggle(username: string) {
		const next = new Set(selected);
		if (next.has(username)) next.delete(username);
		else next.add(username);
		selected = next;
	}

	async function sendInvite() {
		if (selected.size === 0) return;
		inviting = true;
		error = '';
		try {
			const r = await api<{ roomCode: string }>('/api/invites', {
				method: 'POST',
				body: JSON.stringify({
					gameSlug: inviteGame,
					usernames: [...selected],
					options: inviteOptions
				})
			});
			goto(`/room/${r.roomCode}`);
		} catch (e) {
			error = (e as Error).message;
		} finally {
			inviting = false;
		}
	}

	function join(code: string) {
		const c = code.trim().toUpperCase();
		if (c) goto(`/room/${c}`);
	}

	const avatarFace = (u: UserView) => renderAvatar(parseAvatar(u.avatar));
</script>

{#if showWhatsNew}
	<div class="modal-backdrop" role="presentation" onclick={closeWhatsNew}>
		<div class="modal" role="dialog" aria-modal="true" onclick={(e) => e.stopPropagation()}>
			<div class="modal-head">
				<h2>🚀 Novità</h2>
				<button class="x" onclick={closeWhatsNew} aria-label="Chiudi">✕</button>
			</div>
			<ul class="features">
				{#each FEATURES as f (f)}
					<li>{f}</li>
				{/each}
			</ul>
			<div class="modal-actions">
				<button class="ok" onclick={closeWhatsNew}>Capito!</button>
			</div>
		</div>
	</div>
{/if}

<h1 class="title">SELECT YOUR GAME</h1>
<div class="arcade-strip">
	<span class="oneup">1UP</span>
	<span class="hi">HIGH SCORE</span>
	<span class="online">{onlineCount} ONLINE</span>
</div>
{#if error}<p class="error">{error}</p>{/if}

<div class="cards">
	<!-- Card 1: Ospita partita -->
	<section class="card fade" style="--delay: 0ms">
		<div class="card-icon">🏠</div>
		<h2>Ospita partita</h2>
		<p class="muted">Apri una stanza pubblica: chiunque può entrare.</p>
		<select bind:value={hostGame}>
			{#each GAME_CATALOG as g (g.slug)}
				<option value={g.slug}>{g.emoji} {g.label}</option>
			{/each}
		</select>
		<GameOptions game={hostGame} bind:options={hostOptions} />
		<button onclick={createHost} disabled={hosting}>
			{hosting ? 'Creo…' : 'Crea partita pubblica'}
		</button>
	</section>

	<!-- Card 2 (centrale): Parola del Giorno -->
	<section class="card center fade" style="--delay: 80ms">
		<div class="card-icon">🗓</div>
		<h2>Parola del Giorno</h2>
		{#if !daily}
			<p class="muted">Caricamento…</p>
		{:else if daily.eliminated && daily.status === 'PLAYING'}
			<div class="badge danger pulse">☠️ Sei stato eliminato</div>
			<p class="muted">Puoi solo guardare il resto della giornata.</p>
		{:else if daily.status === 'WON'}
			<div class="badge ok pulse">{daily.won ? '🎉 Hai indovinato!' : '🏆 Parola trovata'}</div>
		{:else if daily.status === 'LOST'}
			<div class="badge danger">💀 Parola persa</div>
		{:else}
			<div class="big-number">{lettersLeft}</div>
			<p class="muted">lettere mancanti</p>
			<div class="moves">
				{#if !daily.letterUsed}
					<span class="badge ok">Lettera disponibile</span>
				{:else}
					<span class="badge dim">Lettera usata</span>
				{/if}
				{#if !daily.wordAttemptUsed}
					<span class="badge ok">Tentativo parola</span>
				{:else}
					<span class="badge dim">Tentativo usato</span>
				{/if}
			</div>
			{#if daily.letterUsed && daily.wordAttemptUsed}
				<div class="badge dim pulse">Mosse finite per oggi</div>
			{/if}
		{/if}
		<button class="primary" onclick={() => goto('/daily')}>Vai alla parola</button>
	</section>

	<!-- Card 3: Invita un amico -->
	<section class="card fade" style="--delay: 160ms">
		<div class="card-icon">✉️</div>
		<h2>Invita un amico</h2>
		<p class="muted">Scegli un gioco e invita una o più persone.</p>
		<p class="muted small">🟢 {onlineCount} online ora</p>
		<select bind:value={inviteGame}>
			{#each GAME_CATALOG as g (g.slug)}
				<option value={g.slug}>{g.emoji} {g.label}</option>
			{/each}
		</select>
		<GameOptions game={inviteGame} bind:options={inviteOptions} />
		<div class="users">
			{#if users.length === 0}
				<p class="muted small">Nessun altro utente iscritto.</p>
			{:else}
				{#each sortedUsers as u (u.username)}
					<button
						class="user"
						class:on={selected.has(u.username)}
						onclick={() => toggle(u.username)}
						type="button"
					>
						<span class="presence" class:online={u.online} title={u.online ? 'Online' : 'Offline'}></span>
						<pre class="mini-face">{avatarFace(u)}</pre>
						<span class="uname">{u.displayName}</span>
						{#if selected.has(u.username)}<span class="check">✓</span>{/if}
					</button>
				{/each}
			{/if}
		</div>
		<button onclick={sendInvite} disabled={inviting || selected.size === 0}>
			{inviting ? 'Invio…' : `Invita (${selected.size})`}
		</button>
	</section>
</div>

<!-- Entra con codice -->
<section class="panel">
	<h3>Entra con un codice</h3>
	<div class="row">
		<input placeholder="ABC123" bind:value={joinCode} maxlength="6" />
		<button class="alt" onclick={() => join(joinCode)}>Entra</button>
	</div>
</section>

<!-- Stanze pubbliche -->
<section class="panel">
	<div class="row between">
		<h3>Stanze pubbliche</h3>
		<button class="link" onclick={refreshRooms}>↻ Aggiorna</button>
	</div>
	{#if rooms.length === 0}
		<p class="muted">Nessuna stanza aperta. Creane una qui sopra!</p>
	{:else}
		<ul class="rooms">
			{#each rooms as r (r.code)}
				<li>
					<div>
						<strong>{gameLabel(r.gameSlug)}</strong>
						<span class="muted">· {r.code} · {r.players}/{r.maxPlayers} · host {r.hostEmail}</span>
					</div>
					<button class="alt" onclick={() => join(r.code)} disabled={r.players >= r.maxPlayers}>
						{r.players >= r.maxPlayers ? 'Piena' : 'Entra'}
					</button>
				</li>
			{/each}
		</ul>
	{/if}
</section>

<style>
	.title {
		text-align: center;
		margin: 0.5rem 0 0.6rem;
		font-family: var(--font-display);
		font-size: clamp(1rem, 4vw, 1.6rem);
		color: var(--amber);
		text-shadow: 0 0 10px var(--amber), 0 0 24px rgba(255, 207, 63, 0.4);
	}
	.arcade-strip {
		display: flex;
		justify-content: center;
		gap: 1.5rem;
		flex-wrap: wrap;
		font-family: var(--font-display);
		font-size: 0.62rem;
		margin: 0 0 1.5rem;
	}
	.arcade-strip .oneup {
		color: var(--green);
		text-shadow: 0 0 8px var(--green);
	}
	.arcade-strip .hi {
		color: var(--amber);
		text-shadow: 0 0 8px var(--amber);
	}
	.arcade-strip .online {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.error {
		color: var(--danger);
		font-family: var(--font-term);
		font-size: 1.1rem;
		text-align: center;
	}
	.cards {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 1.25rem;
		align-items: start;
		margin-bottom: 1.5rem;
	}
	.card {
		background: var(--panel);
		border-radius: 16px;
		padding: 1.5rem 1.25rem;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.75rem;
		text-align: center;
		border: 1px solid var(--line);
	}
	.card.center {
		border: 2px solid var(--accent);
		box-shadow: 0 0 0 1px var(--accent), 0 0 30px rgba(255, 46, 136, 0.4);
		transform: scale(1.03);
	}
	.card-icon {
		font-size: 2.2rem;
	}
	h2 {
		margin: 0;
		font-family: var(--font-display);
		font-size: 0.85rem;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
		line-height: 1.4;
	}
	.card.center h2 {
		color: var(--accent);
		text-shadow: var(--glow-mag);
	}
	.muted {
		color: var(--muted);
		margin: 0;
		font-size: 0.9rem;
	}
	.muted.small {
		font-size: 0.8rem;
	}
	.big-number {
		font-family: var(--font-display);
		font-size: 2.6rem;
		color: var(--accent);
		text-shadow: var(--glow-mag);
		line-height: 1;
	}
	select,
	input {
		width: 100%;
		padding: 0.55rem;
		border-radius: 4px;
		border: 2px solid var(--line);
		background: var(--inset);
		color: var(--text);
		font-size: 0.95rem;
	}
	button {
		width: 100%;
		padding: 0.6rem 1rem;
		border: 2px solid rgba(47, 243, 255, 0.5);
		border-radius: 4px;
		background: transparent;
		color: var(--cyan);
		font-family: var(--font-ui);
		font-weight: 700;
		text-transform: uppercase;
		letter-spacing: 0.04em;
		cursor: pointer;
		font-size: 0.9rem;
		margin-top: auto;
		min-height: 44px;
		transition: box-shadow 0.12s, border-color 0.12s;
	}
	button:hover {
		border-color: var(--cyan);
		box-shadow: var(--glow-cyan);
	}
	button.primary,
	.card.center button {
		background: linear-gradient(180deg, var(--accent), #c01e63);
		border-color: var(--amber);
		color: #fff;
		box-shadow: 0 0 18px rgba(255, 46, 136, 0.45);
	}
	button.primary:hover,
	.card.center button:hover {
		box-shadow: 0 0 26px rgba(255, 46, 136, 0.7);
	}
	button:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.moves {
		display: flex;
		gap: 0.4rem;
		flex-wrap: wrap;
		justify-content: center;
	}
	.badge {
		padding: 0.3rem 0.7rem;
		border-radius: 20px;
		font-size: 0.82rem;
		font-weight: 600;
	}
	.badge.ok {
		background: #10331f;
		color: var(--green);
		box-shadow: 0 0 10px rgba(61, 255, 154, 0.3);
	}
	.badge.danger {
		background: #3a1420;
		color: var(--danger);
		box-shadow: 0 0 10px rgba(255, 82, 119, 0.3);
	}
	.badge.dim {
		background: var(--inset);
		color: var(--muted);
		border: 1px solid var(--line);
	}
	.users {
		width: 100%;
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
		max-height: 220px;
		overflow-y: auto;
	}
	.user {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		width: 100%;
		margin: 0;
		padding: 0.4rem 0.6rem;
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 8px;
		color: var(--text);
		cursor: pointer;
		text-align: left;
		text-transform: none;
		font-family: var(--font-term);
		letter-spacing: normal;
		min-height: 44px;
	}
	.user.on {
		border-color: var(--accent);
		background: #2a0f33;
		box-shadow: 0 0 10px rgba(255, 46, 136, 0.3);
	}
	.user:hover {
		border-color: var(--cyan);
		box-shadow: none;
	}
	.presence {
		width: 0.6rem;
		height: 0.6rem;
		border-radius: 50%;
		background: var(--muted);
		flex: 0 0 auto;
	}
	.presence.online {
		background: var(--green);
		box-shadow: 0 0 6px var(--green);
	}
	.mini-face {
		font-family: ui-monospace, monospace;
		font-size: 0.45rem;
		line-height: 1.1;
		margin: 0;
		color: var(--muted);
	}
	.uname {
		flex: 1;
		font-size: 0.9rem;
	}
	.check {
		color: var(--green);
		font-weight: 700;
		text-shadow: 0 0 6px var(--green);
	}
	.panel {
		background: var(--panel);
		padding: 1rem 1.25rem;
		border-radius: 12px;
		margin-bottom: 1.25rem;
		border: 1px solid var(--line);
	}
	h3 {
		font-family: var(--font-display);
		font-size: 0.8rem;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
		margin: 0 0 0.75rem;
	}
	.row {
		display: flex;
		gap: 0.75rem;
		align-items: center;
		flex-wrap: wrap;
	}
	.row.between {
		justify-content: space-between;
	}
	.row input {
		flex: 1;
		width: auto;
		min-width: 8rem;
	}
	.alt {
		width: auto;
		margin-top: 0;
		background: linear-gradient(180deg, var(--accent), #c01e63);
		border-color: var(--amber);
		color: #fff;
		box-shadow: 0 0 14px rgba(255, 46, 136, 0.4);
	}
	.link {
		width: auto;
		min-height: 0;
		margin-top: 0;
		background: none;
		border: none;
		color: var(--muted);
		box-shadow: none;
	}
	.link:hover {
		color: var(--cyan);
		border: none;
		box-shadow: none;
	}
	.rooms {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}
	.rooms li {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 0.75rem;
		padding: 0.6rem 0.75rem;
		background: var(--inset);
		border-left: 4px solid var(--cyan);
		border-radius: 8px;
		flex-wrap: wrap;
	}
	.fade {
		opacity: 0;
		transform: translateY(8px);
		animation: fadein 0.45s ease forwards;
		animation-delay: var(--delay);
	}
	@keyframes fadein {
		to {
			opacity: 1;
			transform: translateY(0) scale(var(--s, 1));
		}
	}
	.card.center.fade {
		--s: 1.03;
		transform: translateY(8px) scale(1.03);
	}
	.pulse {
		animation: pulse 1.6s ease-in-out infinite;
	}
	@keyframes pulse {
		0%,
		100% {
			opacity: 1;
		}
		50% {
			opacity: 0.5;
		}
	}
	@media (max-width: 760px) {
		.cards {
			grid-template-columns: 1fr;
		}
		.card.center {
			transform: none;
		}
		.card.center.fade {
			transform: translateY(8px);
			--s: 1;
		}
	}
	.modal-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.6);
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 1rem;
		z-index: 10000;
	}
	.modal {
		background: var(--panel);
		border-radius: 14px;
		padding: 1.25rem;
		width: 100%;
		max-width: 460px;
		border: 2px solid var(--accent);
		box-shadow: var(--glow-mag);
	}
	.modal-head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 0.75rem;
	}
	.modal-head h2 {
		margin: 0;
		font-family: var(--font-display);
		font-size: 0.9rem;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.x {
		background: none;
		border: none;
		color: var(--muted);
		font-size: 1.1rem;
		cursor: pointer;
		width: auto;
		margin: 0;
		padding: 0;
	}
	.features {
		margin: 0 0 1rem 1.25rem;
		padding: 0;
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
		color: var(--text);
		font-family: var(--font-term);
		font-size: 1.1rem;
	}
	.modal-actions {
		display: flex;
		justify-content: flex-end;
	}
	.ok {
		width: auto;
		margin: 0;
		padding: 0.55rem 1.4rem;
		background: linear-gradient(180deg, var(--accent), #c01e63);
		border: 2px solid var(--amber);
		border-radius: 4px;
		color: #fff;
		cursor: pointer;
		box-shadow: 0 0 18px rgba(255, 46, 136, 0.45);
	}
</style>
