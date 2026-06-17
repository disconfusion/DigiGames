<script lang="ts">
	import favicon from '$lib/assets/favicon.svg';
	import { auth, logout } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';

	let { children } = $props();

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
	main {
		max-width: 900px;
		margin: 0 auto;
		padding: 1.5rem 1rem;
	}
</style>
