<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import Markdown from '$lib/Markdown.svelte';

	let content = $state('');
	let loading = $state(true);
	let error = $state('');

	onMount(async () => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		try {
			const r = await api<{ content: string }>('/api/roadmap');
			content = r.content;
		} catch (e) {
			error = (e as Error).message;
		} finally {
			loading = false;
		}
	});
</script>

<h1>🗺 Roadmap</h1>

{#if loading}
	<p class="muted">Caricamento…</p>
{:else if error}
	<p class="error">{error}</p>
{:else if !content.trim()}
	<p class="muted">Nessuna roadmap disponibile al momento.</p>
{:else}
	<div class="roadmap-content"><Markdown source={content} /></div>
{/if}

<style>
	h1 {
		margin: 0.5rem 0 1.25rem;
	}
	.muted {
		color: var(--muted);
	}
	.error {
		color: #f87171;
	}
	.roadmap-content {
		background: var(--panel);
		padding: 1.5rem 1.75rem;
		border-radius: 12px;
		border: 1px solid var(--line);
		max-width: 1000px;
	}
</style>
