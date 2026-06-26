<script lang="ts">
	import '$lib/retro-crt-theme.css';
	import favicon from '$lib/assets/favicon.svg';
	import { onDestroy } from 'svelte';
	import { auth, logout } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api';
	import { GAME_CATALOG } from '$lib/games/catalog';
	import { connectNotify, type NotifyConnection } from '$lib/ws';
	import { notifications, onInviteReceived, setInviteCount, onDailyUpdate, onPresenceUpdate, showToast } from '$lib/notifications.svelte';
	import ToastContainer from '$lib/ToastContainer.svelte';

	// Etichetta gioco da slug (per i toast invito)
	const gameLabel = (slug: unknown): string =>
		GAME_CATALOG.find((g) => g.slug === slug)?.label ?? 'una partita';

	let { children } = $props();

	let pingTimer: ReturnType<typeof setInterval> | undefined;
	let notifyWs: NotifyConnection | undefined;

	const isAdmin = $derived(auth.session?.role === 'admin');

	let menuOpen = $state(false);

	// Modale segnalazione bug
	let showBug = $state(false);
	let bugGame = $state('generale');
	let bugDesc = $state('');
	let bugMsg = $state('');
	let bugErr = $state('');
	let bugSending = $state(false);

	function openBug() {
		bugGame = 'generale';
		bugDesc = '';
		bugMsg = '';
		bugErr = '';
		showBug = true;
	}

	async function submitBug() {
		if (!bugDesc.trim()) return;
		bugSending = true;
		bugErr = '';
		try {
			await api('/api/bugs', {
				method: 'POST',
				body: JSON.stringify({ game: bugGame, description: bugDesc })
			});
			bugMsg = '✓ Grazie per la segnalazione!';
			bugDesc = '';
			setTimeout(() => (showBug = false), 1200);
		} catch (e) {
			bugErr = (e as Error).message;
		} finally {
			bugSending = false;
		}
	}

	async function fetchInviteCount() {
		try {
			const r = await api<{ count: number }>('/api/invites/count');
			setInviteCount(r.count);
		} catch { /* silenzioso */ }
	}

	async function ping() {
		try {
			await api('/api/presence/ping', { method: 'POST' });
		} catch { /* silenzioso */ }
	}

	$effect(() => {
		if (auth.session) {
			// Heartbeat presenza ogni 30s
			ping();
			pingTimer = setInterval(ping, 30_000);
			// Fetch count iniziale + canale WS notifiche
			fetchInviteCount();
			notifyWs = connectNotify(
				auth.session.token,
				(msg) => {
					if (msg.type === 'invite') {
						onInviteReceived();
						const from = typeof msg.from === 'string' ? msg.from : null;
						showToast(
							from
								? `📨 ${from} ti ha invitato a ${gameLabel(msg.game)}`
								: '📨 Hai ricevuto un nuovo invito',
							'invite'
						);
					} else if (msg.type === 'daily:update') {
						onDailyUpdate();
						showToast('🗓 La parola del giorno è stata aggiornata', 'info');
					} else if (msg.type === 'presence:update') {
						// Aggiorna solo lo stato presenze (niente toast: troppo frequente)
						onPresenceUpdate();
					}
				},
				() => fetchInviteCount()
			);
		} else {
			clearInterval(pingTimer);
			pingTimer = undefined;
			notifyWs?.close();
			notifyWs = undefined;
			setInviteCount(0);
		}
	});

	onDestroy(() => {
		clearInterval(pingTimer);
		notifyWs?.close();
	});

	function doLogout() {
		logout();
		goto('/login');
	}
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>DigiGames</title>
</svelte:head>

<header>
	<a class="brand" href="/">🎮 DigiGames</a>
	{#if auth.session}
		<button
			class="menu-toggle"
			onclick={() => (menuOpen = !menuOpen)}
			aria-label="Apri/chiudi menu"
			aria-expanded={menuOpen}
		>
			{menuOpen ? '✕' : '☰'}
		</button>
	{/if}
	<nav class:open={menuOpen} onclick={() => (menuOpen = false)}>
		{#if auth.session}
			<a class="who" href="/profile">{auth.session.displayName}</a>
			<a href="/">Home</a>
			<a href="/daily">Parola del Giorno</a>
			<a href="/leaderboard">Classifica</a>
			<a href="/roadmap">Roadmap</a>
			<a class="invites" href="/invites">
				Inviti
				{#if notifications.inviteCount > 0}<span class="nav-badge">{notifications.inviteCount}</span>{/if}
			</a>
			{#if isAdmin}<a href="/admin">🛠 Admin</a>{/if}
			<button class="link bug" onclick={openBug}>🐞 Segnala bug</button>
			<button class="link" onclick={doLogout}>Esci</button>
		{:else}
			<a href="/login">Accedi</a>
		{/if}
	</nav>
</header>

<main>
	{@render children()}
</main>

<ToastContainer />

{#if showBug}
	<div
		class="modal-backdrop"
		role="presentation"
		onclick={() => (showBug = false)}
	>
		<div class="modal" role="dialog" aria-modal="true" onclick={(e) => e.stopPropagation()}>
			<div class="modal-head">
				<h2>🐞 Segnala un bug</h2>
				<button class="x" onclick={() => (showBug = false)} aria-label="Chiudi">✕</button>
			</div>
			<label class="field">
				Gioco / area interessata
				<select bind:value={bugGame}>
					<option value="generale">Generale / Altro</option>
					{#each GAME_CATALOG as g (g.slug)}
						<option value={g.slug}>{g.emoji} {g.label}</option>
					{/each}
				</select>
			</label>
			<label class="field">
				Descrizione del problema
				<textarea
					bind:value={bugDesc}
					rows="5"
					maxlength="2000"
					placeholder="Cosa è successo? Come riprodurlo?"
				></textarea>
			</label>
			{#if bugErr}<p class="bug-err">{bugErr}</p>{/if}
			{#if bugMsg}<p class="bug-ok">{bugMsg}</p>{/if}
			<div class="modal-actions">
				<button class="cancel" onclick={() => (showBug = false)}>Annulla</button>
				<button class="send" onclick={submitBug} disabled={bugSending || !bugDesc.trim()}>
					{bugSending ? 'Invio…' : 'Invia segnalazione'}
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	/* Token palette/font/scanline definiti in $lib/retro-crt-theme.css (import nel <script>). */
	:global(*, *::before, *::after) {
		box-sizing: border-box;
	}
	:global(html) {
		/* Scrollbar a tema (Firefox) */
		scrollbar-width: thin;
		scrollbar-color: var(--line) transparent;
	}
	:global(body) {
		margin: 0;
		color: var(--text);
		overflow-x: hidden;
		-webkit-text-size-adjust: 100%;
	}
	/* Scrollbar a tema (WebKit/Chromium) */
	:global(::-webkit-scrollbar) {
		width: 10px;
		height: 10px;
	}
	:global(::-webkit-scrollbar-track) {
		background: var(--bg);
	}
	:global(::-webkit-scrollbar-thumb) {
		background: #334155;
		border-radius: 8px;
		border: 2px solid var(--bg);
	}
	:global(::-webkit-scrollbar-thumb:hover) {
		background: var(--accent);
	}
	:global(::-webkit-scrollbar-corner) {
		background: var(--bg);
	}
	header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		padding: 0.75rem 1rem;
		background: var(--panel);
		flex-wrap: wrap;
	}
	.brand {
		font-weight: 700;
		font-size: 1.2rem;
		color: var(--text);
		text-decoration: none;
	}
	nav {
		display: flex;
		align-items: center;
		gap: 1rem;
	}
	.menu-toggle {
		display: none;
		background: none;
		border: none;
		color: var(--text);
		font-size: 1.5rem;
		line-height: 1;
		cursor: pointer;
		padding: 0.25rem 0.5rem;
	}
	nav a,
	.who {
		color: var(--muted);
		text-decoration: none;
	}
	nav a:hover {
		color: var(--text);
	}
	.link {
		background: none;
		border: none;
		color: var(--muted);
		cursor: pointer;
		font: inherit;
		padding: 0;
	}
	.link:hover {
		color: var(--text);
	}
	.invites {
		position: relative;
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
	}
	.nav-badge {
		background: #ef4444;
		color: white;
		font-size: 0.72rem;
		font-weight: 700;
		min-width: 1.1rem;
		height: 1.1rem;
		padding: 0 0.3rem;
		border-radius: 999px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
	}
	main {
		max-width: 900px;
		margin: 0 auto;
		padding: 1.5rem 1rem;
		width: 100%;
	}
	/* Su schermi grandi diamo più respiro orizzontale (evita card strette e troppo alte) */
	@media (min-width: 1500px) {
		main {
			max-width: 1120px;
		}
	}
	@media (min-width: 2000px) {
		main {
			max-width: 1320px;
		}
	}
	.bug {
		color: var(--amber);
	}
	@media (max-width: 640px) {
		header {
			padding: 0.6rem 0.8rem;
		}
		.brand {
			font-size: 1.05rem;
		}
		.menu-toggle {
			display: block;
		}
		/* La nav diventa un menu a tendina a tutta larghezza */
		nav {
			display: none;
			order: 3;
			width: 100%;
			flex-direction: column;
			align-items: stretch;
			gap: 0.25rem;
			margin-top: 0.5rem;
		}
		nav.open {
			display: flex;
		}
		nav a,
		nav button {
			padding: 0.6rem 0.4rem;
			border-radius: 8px;
			width: 100%;
			text-align: left;
		}
		nav a:hover,
		nav button:hover {
			background: var(--inset);
		}
		main {
			padding: 1rem 0.8rem;
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
		/* sopra le scanline globali (z-index 9998 in retro-crt-theme.css) */
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
		font-size: 1.15rem;
	}
	.x {
		background: none;
		border: none;
		color: var(--muted);
		font-size: 1.1rem;
		cursor: pointer;
	}
	.field {
		display: flex;
		flex-direction: column;
		gap: 0.3rem;
		font-size: 0.9rem;
		color: var(--muted);
		margin-bottom: 0.75rem;
	}
	.field select,
	.field textarea {
		padding: 0.55rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font: inherit;
		resize: vertical;
	}
	.modal-actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.6rem;
	}
	.modal-actions button {
		padding: 0.55rem 1.1rem;
		border: none;
		border-radius: 8px;
		cursor: pointer;
		font-size: 0.95rem;
	}
	.cancel {
		background: var(--line);
		color: var(--text);
	}
	.send {
		background: var(--accent);
		color: white;
	}
	.send:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.bug-err {
		color: var(--danger);
		margin: 0 0 0.5rem;
	}
	.bug-ok {
		color: var(--green);
		margin: 0 0 0.5rem;
	}
</style>
