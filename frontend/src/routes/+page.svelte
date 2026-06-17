<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { GAME_CATALOG } from '$lib/games/catalog';
	import { parseAvatar, renderAvatar } from '$lib/avatar';

	type RoomView = { code: string };
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
	let error = $state('');

	// Card "Ospita"
	let hostGame = $state('connect4');
	let hosting = $state(false);

	// Card "Invita"
	let inviteGame = $state('connect4');
	let selected = $state<Set<string>>(new Set());
	let inviting = $state(false);

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
	});

	async function createHost() {
		hosting = true;
		error = '';
		try {
			const r = await api<RoomView>('/api/rooms', {
				method: 'POST',
				body: JSON.stringify({ gameSlug: hostGame, isPrivate: false })
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
				body: JSON.stringify({ gameSlug: inviteGame, usernames: [...selected] })
			});
			goto(`/room/${r.roomCode}`);
		} catch (e) {
			error = (e as Error).message;
		} finally {
			inviting = false;
		}
	}

	function avatarFace(u: UserView): string {
		return renderAvatar(parseAvatar(u.avatar));
	}
</script>

<h1 class="title">🎮 DigiGames</h1>
{#if error}<p class="error">{error}</p>{/if}

<div class="cards">
	<!-- Card 1: Ospita partita -->
	<section class="card fade" style="--delay: 0ms">
		<div class="card-icon">🏠</div>
		<h2>Ospita partita</h2>
		<p class="muted">Apri una stanza pubblica: chiunque può entrare dalla lobby.</p>
		<select bind:value={hostGame}>
			{#each GAME_CATALOG as g (g.slug)}
				<option value={g.slug}>{g.emoji} {g.label}</option>
			{/each}
		</select>
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
		<p class="muted">Scegli un gioco e invita una o più persone iscritte.</p>
		<p class="muted small">🟢 {onlineCount} online ora</p>
		<select bind:value={inviteGame}>
			{#each GAME_CATALOG as g (g.slug)}
				<option value={g.slug}>{g.emoji} {g.label}</option>
			{/each}
		</select>
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

<style>
	.title {
		text-align: center;
		margin: 0.5rem 0 1.5rem;
	}
	.error {
		color: #f87171;
		text-align: center;
	}
	.cards {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 1.25rem;
		align-items: start;
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
		border: 1px solid #334155;
	}
	.card.center {
		border-color: var(--accent);
		box-shadow: 0 0 0 1px var(--accent), 0 8px 30px rgba(99, 102, 241, 0.25);
		transform: scale(1.03);
	}
	.card-icon {
		font-size: 2.2rem;
	}
	h2 {
		margin: 0;
		font-size: 1.15rem;
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
		font-size: 3rem;
		font-weight: 800;
		color: var(--accent);
		line-height: 1;
	}
	select {
		width: 100%;
		padding: 0.55rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font-size: 0.95rem;
	}
	button {
		width: 100%;
		padding: 0.6rem 1rem;
		border: none;
		border-radius: 8px;
		background: #475569;
		color: white;
		cursor: pointer;
		font-size: 0.95rem;
		margin-top: auto;
	}
	button.primary,
	.card.center button {
		background: var(--accent);
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
		background: #14532d;
		color: #bbf7d0;
	}
	.badge.danger {
		background: #7f1d1d;
		color: #fecaca;
	}
	.badge.dim {
		background: #1e293b;
		color: var(--muted);
		border: 1px solid #334155;
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
		background: #0f172a;
		border: 1px solid #334155;
		border-radius: 8px;
		color: var(--text);
		cursor: pointer;
		text-align: left;
	}
	.user.on {
		border-color: var(--accent);
		background: #1e3a5f;
	}
	.presence {
		width: 0.6rem;
		height: 0.6rem;
		border-radius: 50%;
		background: #475569;
		flex: 0 0 auto;
	}
	.presence.online {
		background: #22c55e;
		box-shadow: 0 0 6px #22c55e;
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
		color: #4ade80;
		font-weight: 700;
	}
	.fade {
		opacity: 0;
		transform: translateY(8px);
		animation: fadein 0.45s ease forwards;
		animation-delay: var(--delay);
	}
	.card.center.fade {
		transform: translateY(8px) scale(1.03);
	}
	@keyframes fadein {
		to {
			opacity: 1;
			transform: translateY(0) scale(var(--s, 1));
		}
	}
	.card.center.fade {
		--s: 1.03;
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
</style>
