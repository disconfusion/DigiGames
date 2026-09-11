<script lang="ts">
	import '$lib/retro-crt-theme.css';
	import favicon from '$lib/assets/favicon.svg';
	import { onDestroy } from 'svelte';
	import { auth, logout } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { api } from '$lib/api';
	import { GAME_CATALOG } from '$lib/games/catalog';
	import { connectNotify, type NotifyConnection } from '$lib/ws';
	import { notifications, onInviteReceived, onInviteResolved, setInviteCount, onDailyUpdate, onPresenceUpdate, onGiftReceived, showToast } from '$lib/notifications.svelte';
	import { wallet, setBalance, refreshBalance } from '$lib/wallet.svelte';
	import ToastContainer from '$lib/ToastContainer.svelte';
	import GamePicker from '$lib/games/GamePicker.svelte';
	import Icon from '$lib/icons/Icon.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import ZoomControl from '$lib/components/ZoomControl.svelte';
	import { initScale } from '$lib/uiScale.svelte';

	// Etichetta gioco da slug (per i toast invito)
	const gameLabel = (slug: unknown): string =>
		GAME_CATALOG.find((g) => g.slug === slug)?.label ?? 'una partita';

	// Aree selezionabili nella segnalazione bug: "Generale" + tutti i giochi
	const BUG_AREAS = [{ slug: 'generale', label: 'Generale / Altro' }, ...GAME_CATALOG];

	let { children } = $props();

	let pingTimer: ReturnType<typeof setInterval> | undefined;
	let notifyWs: NotifyConnection | undefined;

	const isAdmin = $derived(auth.session?.role === 'admin');

	// Pagina corrente → evidenziazione del link attivo nella tab-strip
	const path = $derived(page.url.pathname);
	const isActive = (href: string) => (href === '/' ? path === '/' : path.startsWith(href));

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

	// Modale suggerimento feature/gioco
	let showSuggest = $state(false);
	let suggestKind = $state<'feature' | 'game'>('feature');
	let suggestDesc = $state('');
	let suggestMsg = $state('');
	let suggestErr = $state('');
	let suggestSending = $state(false);

	function openSuggest() {
		suggestKind = 'feature';
		suggestDesc = '';
		suggestMsg = '';
		suggestErr = '';
		showSuggest = true;
	}

	async function submitSuggest() {
		if (!suggestDesc.trim()) return;
		suggestSending = true;
		suggestErr = '';
		try {
			await api('/api/suggestions', {
				method: 'POST',
				body: JSON.stringify({ kind: suggestKind, description: suggestDesc })
			});
			suggestMsg = '✓ Grazie per il suggerimento!';
			suggestDesc = '';
			setTimeout(() => (showSuggest = false), 1200);
		} catch (e) {
			suggestErr = (e as Error).message;
		} finally {
			suggestSending = false;
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

	let companion = $state('');
	let companionTint = $state('');
	let house = $state('');
	async function fetchCompanion() {
		try {
			const r = await api<{ companion: string | null; companionTint: string | null; house: string | null }>(
				'/api/me'
			);
			companion = r.companion ?? '';
			companionTint = r.companionTint ?? '';
			house = r.house ?? '';
		} catch { /* silenzioso */ }
	}

	/** Accetta l'invito direttamente dal toast ed entra nella stanza. */
	async function acceptInvite(inviteId: number, roomCode: string) {
		try {
			const r = await api<{ roomCode: string }>(`/api/invites/${inviteId}/accept`, { method: 'POST' });
			setInviteCount(Math.max(0, notifications.inviteCount - 1));
			onInviteResolved();
			goto(`/room/${r.roomCode ?? roomCode}`);
		} catch (e) {
			showToast((e as Error).message, 'error');
		}
	}

	/** Rifiuta l'invito dal toast (nessun cambio di pagina). */
	async function declineInvite(inviteId: number) {
		try {
			await api(`/api/invites/${inviteId}/decline`, { method: 'POST' });
			setInviteCount(Math.max(0, notifications.inviteCount - 1));
			onInviteResolved();
		} catch (e) {
			showToast((e as Error).message, 'error');
		}
	}

	// Zoom dell'interfaccia scelto sul dispositivo: `app.html` lo applica già prima del primo
	// disegno, qui si allinea lo stato reattivo del controllo in header.
	$effect(() => {
		initScale();
	});

	$effect(() => {
		if (auth.session) {
			// Heartbeat presenza + refresh saldo Token / companion ogni 30s.
			// Il saldo arriva anche in push sul WS ("tokens"): il polling è solo la rete di sicurezza.
			ping();
			refreshBalance();
			fetchCompanion();
			pingTimer = setInterval(() => {
				ping();
				refreshBalance();
				fetchCompanion();
			}, 30_000);
			// Fetch count iniziale + canale WS notifiche
			fetchInviteCount();
			notifyWs = connectNotify(
				auth.session.token,
				(msg) => {
					if (msg.type === 'invite') {
						onInviteReceived();
						const from = typeof msg.from === 'string' ? msg.from : null;
						const inviteId = typeof msg.inviteId === 'number' ? msg.inviteId : null;
						const roomCode = typeof msg.roomCode === 'string' ? msg.roomCode : '';
						const text = from
							? `${from} ti ha invitato a ${gameLabel(msg.game)}`
							: 'Hai ricevuto un nuovo invito';
						// Con inviteId il toast è persistente e decidibile sul posto; senza (vecchi
						// client/server) resta il toast informativo con auto-dismiss.
						showToast(
							text,
							'invite',
							inviteId ? 0 : 5000,
							inviteId
								? [
										{ label: 'Accetta', style: 'primary', run: () => acceptInvite(inviteId, roomCode) },
										{ label: 'Rifiuta', style: 'ghost', run: () => declineInvite(inviteId) }
									]
								: undefined
						);
					} else if (msg.type === 'tokens') {
						// Saldo aggiornato dal server (vittoria, acquisto, regalo): badge reattivo
						if (typeof msg.balance === 'number') setBalance(msg.balance);
					} else if (msg.type === 'daily:update') {
						onDailyUpdate();
						showToast('La parola del giorno è stata aggiornata', 'info');
					} else if (msg.type === 'presence:update') {
						// Aggiorna solo lo stato presenze (niente toast: troppo frequente)
						onPresenceUpdate();
					} else if (msg.type === 'gift') {
						onGiftReceived();
						const from = typeof msg.from === 'string' && msg.from ? msg.from : null;
						showToast(from ? `${from} ti ha mandato un regalo!` : 'Hai ricevuto un regalo!', 'success');
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
			setBalance(0);
			companion = '';
			house = '';
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
	<div class="bar-top">
		<a class="brand" href="/"><Icon name="gamepad" size={18} title="DigiGames" /> DigiGames</a>
		{#if auth.session}
			<div class="actions">
				<ZoomControl />
				<a class="token-badge" href="/shop" title="Vai allo shop"><Icon name="coin" size={16} title="Token" /> {wallet.balance}</a>
				<a class="who" href="/profile" class:active={isActive('/profile')} title="Area personale">
					{#if house}<Icon name={house} size={16} title="Casata" />{/if}
					{#if companion}<Icon name={companion} size={16} title="Companion" tint={companionTint} />{/if}
					<span class="who-name">{auth.session.displayName}</span>
				</a>
				<button class="icon-btn suggest" onclick={openSuggest} title="Suggerisci feature/gioco" aria-label="Suggerisci feature o gioco"><Icon name="pencil" size={16} /></button>
				<button class="icon-btn bug" onclick={openBug} title="Segnala bug" aria-label="Segnala bug"><Icon name="bug" size={16} /></button>
				<button class="icon-btn" onclick={doLogout} title="Esci">Esci</button>
			</div>
		{:else}
			<div class="actions">
				<ZoomControl />
				<a class="login-link" href="/login">Accedi</a>
			</div>
		{/if}
	</div>
	{#if auth.session}
		<nav class="tab-strip">
			<a href="/" class:active={isActive('/')}>Home</a>
			<a href="/daily" class:active={isActive('/daily')}>Parola del Giorno</a>
			<a href="/leaderboard" class:active={isActive('/leaderboard')}>Classifica</a>
			<a href="/shop" class:active={isActive('/shop')}><Icon name="cart" size={14} title="Shop" /> Shop</a>
			<a href="/roadmap" class:active={isActive('/roadmap')}>Roadmap</a>
			<a class="invites" href="/invites" class:active={isActive('/invites')}>
				Inviti
				{#if notifications.inviteCount > 0}<span class="nav-badge">{notifications.inviteCount}</span>{/if}
			</a>
			{#if isAdmin}<a href="/admin" class:active={isActive('/admin')}><Icon name="tools" size={14} title="Admin" /> Admin</a>{/if}
		</nav>
	{/if}
</header>

<main>
	{@render children()}
</main>

<ToastContainer />

{#if showBug}
	<Modal title="Segnala un bug" icon="bug" onClose={() => (showBug = false)}>
		<div class="field">
			<span class="field-label">Gioco / area interessata</span>
			<GamePicker bind:value={bugGame} items={BUG_AREAS} iconSize={28} />
		</div>
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
		{#snippet actions()}
			<button class="cancel" onclick={() => (showBug = false)}>Annulla</button>
			<button class="send" onclick={submitBug} disabled={bugSending || !bugDesc.trim()}>
				{bugSending ? 'Invio…' : 'Invia segnalazione'}
			</button>
		{/snippet}
	</Modal>
{/if}

{#if showSuggest}
	<Modal title="Suggerisci feature/gioco" icon="pencil" onClose={() => (showSuggest = false)}>
		<label class="field">
			Tipo di suggerimento
			<select bind:value={suggestKind}>
				<option value="feature">Nuova feature</option>
				<option value="game">Nuovo gioco</option>
			</select>
		</label>
		<label class="field">
			La tua idea
			<textarea
				bind:value={suggestDesc}
				rows="5"
				maxlength="2000"
				placeholder="Descrivi la feature o il gioco che vorresti…"
			></textarea>
		</label>
		{#if suggestErr}<p class="bug-err">{suggestErr}</p>{/if}
		{#if suggestMsg}<p class="bug-ok">{suggestMsg}</p>{/if}
		{#snippet actions()}
			<button class="cancel" onclick={() => (showSuggest = false)}>Annulla</button>
			<button class="send" onclick={submitSuggest} disabled={suggestSending || !suggestDesc.trim()}>
				{suggestSending ? 'Invio…' : 'Invia suggerimento'}
			</button>
		{/snippet}
	</Modal>
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
	/* ── Header a due righe: brand+azioni / tab-strip ── */
	header {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		padding: 0.6rem 1rem 0;
	}
	.bar-top {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		flex-wrap: wrap;
	}
	.brand {
		font-weight: 700;
		font-size: 1.2rem;
		color: var(--text);
		text-decoration: none;
		display: inline-flex;
		align-items: center;
		gap: 0.4rem;
	}
	.actions {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}
	.token-badge {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		padding: 0.25rem 0.65rem;
		border-radius: 999px;
		background: var(--inset);
		border: 1px solid var(--amber);
		color: var(--amber);
		font-family: var(--font-ui);
		font-weight: 700;
		font-size: 0.8rem;
		white-space: nowrap;
		text-decoration: none;
		text-shadow: 0 0 6px rgba(255, 207, 63, 0.5);
	}
	.token-badge:hover {
		box-shadow: 0 0 10px rgba(255, 207, 63, 0.45);
	}
	.who {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		padding: 0.22rem 0.6rem;
		border-radius: 999px;
		border: 1px solid var(--line);
		background: var(--inset);
		color: var(--muted);
		text-decoration: none;
	}
	.who:hover,
	.who.active {
		border-color: var(--cyan);
		color: var(--text);
		box-shadow: 0 0 8px color-mix(in srgb, var(--cyan) 30%, transparent);
	}
	.icon-btn {
		background: none;
		border: 1px solid transparent;
		color: var(--muted);
		cursor: pointer;
		font: inherit;
		padding: 0.25rem 0.5rem;
		border-radius: 8px;
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
	}
	.icon-btn:hover {
		color: var(--text);
		border-color: var(--line);
	}
	.icon-btn.bug {
		color: var(--amber);
	}
	.icon-btn.suggest {
		color: var(--cyan);
	}
	.login-link {
		color: var(--cyan);
		text-decoration: none;
	}

	/* Striscia di tab della navigazione */
	.tab-strip {
		display: flex;
		align-items: stretch;
		gap: 0.15rem;
		border-top: 1px solid var(--line);
		overflow-x: auto;
		scrollbar-width: none;
	}
	.tab-strip::-webkit-scrollbar {
		display: none;
	}
	.tab-strip a {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		padding: 0.55rem 0.8rem;
		color: var(--muted);
		text-decoration: none;
		white-space: nowrap;
		border-bottom: 2px solid transparent;
	}
	.tab-strip a:hover {
		color: var(--text);
	}
	.tab-strip a.active {
		color: var(--cyan);
		border-bottom-color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.invites {
		position: relative;
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
		max-width: 60rem;
		margin: 0 auto;
		padding: 1.5rem 1rem;
		width: 100%;
	}
	/* Su schermi grandi diamo più respiro orizzontale (evita card strette e troppo alte) */
	@media (min-width: 1200px) {
		main {
			max-width: 68.75rem;
		}
	}
	@media (min-width: 1500px) {
		main {
			max-width: 80rem;
		}
	}
	@media (min-width: 1920px) {
		main {
			max-width: 90rem;
		}
	}
	.bug {
		color: var(--amber);
	}
	@media (max-width: 640px) {
		header {
			padding: 0.5rem 0.6rem 0;
		}
		.brand {
			font-size: 1rem;
		}
		.actions {
			gap: 0.35rem;
		}
		.who-name {
			display: inline-block;
			max-width: 6rem;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
		.tab-strip a {
			padding: 0.5rem 0.6rem;
		}
		main {
			padding: 1rem 0.8rem;
		}
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
	.cancel,
	.send {
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
