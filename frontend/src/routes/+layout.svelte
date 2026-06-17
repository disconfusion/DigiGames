<script lang="ts">
	import favicon from '$lib/assets/favicon.svg';
	import { onDestroy } from 'svelte';
	import { auth, logout } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api';

	let { children } = $props();

	let inviteCount = $state(0);
	let timer: ReturnType<typeof setInterval> | undefined;

	async function refreshInvites() {
		if (!auth.session) {
			inviteCount = 0;
			return;
		}
		try {
			const r = await api<{ count: number }>('/api/invites/count');
			inviteCount = r.count;
		} catch {
			// silenzioso
		}
	}

	$effect(() => {
		// (Ri)avvia il polling quando cambia lo stato di login
		if (auth.session && !timer) {
			refreshInvites();
			timer = setInterval(refreshInvites, 15_000);
		} else if (!auth.session && timer) {
			clearInterval(timer);
			timer = undefined;
			inviteCount = 0;
		}
	});

	onDestroy(() => clearInterval(timer));

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
	<nav>
		{#if auth.session}
			<a class="who" href="/profile">{auth.session.displayName}</a>
			<a href="/lobby">Lobby</a>
			<a href="/daily">Parola del Giorno</a>
			<a href="/leaderboard">Classifica</a>
			<a class="invites" href="/invites">
				Inviti
				{#if inviteCount > 0}<span class="nav-badge">{inviteCount}</span>{/if}
			</a>
			<button class="link" onclick={doLogout}>Esci</button>
		{:else}
			<a href="/login">Accedi</a>
		{/if}
	</nav>
</header>

<main>
	{@render children()}
</main>

<style>
	:global(:root) {
		--bg: #0f172a;
		--panel: #1e293b;
		--accent: #6366f1;
		--text: #e2e8f0;
		--muted: #94a3b8;
	}
	:global(body) {
		margin: 0;
		font-family: system-ui, sans-serif;
		background: var(--bg);
		color: var(--text);
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
	}
</style>
