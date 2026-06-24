<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { gameLabel } from '$lib/games/catalog';
	import { notifications, setInviteCount } from '$lib/notifications.svelte';

	type Invite = {
		id: number;
		fromDisplayName: string;
		fromUsername: string;
		gameSlug: string;
		roomCode: string;
		createdAt: string;
	};

	let invites = $state<Invite[]>([]);
	let error = $state('');
	let loading = $state(true);

	async function load() {
		try {
			invites = await api<Invite[]>('/api/invites');
			setInviteCount(invites.length);
		} catch (e) {
			error = (e as Error).message;
		} finally {
			loading = false;
		}
	}

	onMount(() => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		load();
	});

	// Ricarica lista quando arriva un nuovo invito via WS
	$effect(() => {
		if (notifications.lastInvite) load();
	});

	async function accept(inv: Invite) {
		try {
			const r = await api<{ roomCode: string }>(`/api/invites/${inv.id}/accept`, { method: 'POST' });
			goto(`/room/${r.roomCode}`);
		} catch (e) {
			error = (e as Error).message;
		}
	}

	async function decline(inv: Invite) {
		try {
			await api(`/api/invites/${inv.id}/decline`, { method: 'POST' });
			invites = invites.filter((i) => i.id !== inv.id);
		} catch (e) {
			error = (e as Error).message;
		}
	}
</script>

<h1>📨 Inviti ricevuti</h1>
{#if error}<p class="error">{error}</p>{/if}

{#if loading}
	<p class="muted">Caricamento…</p>
{:else if invites.length === 0}
	<p class="muted">Nessun invito in attesa.</p>
{:else}
	<ul class="list">
		{#each invites as inv (inv.id)}
			<li>
				<div class="info">
					<strong>{inv.fromDisplayName}</strong> ti invita a giocare a
					<strong>{gameLabel(inv.gameSlug)}</strong>
					<span class="muted">· stanza {inv.roomCode}</span>
				</div>
				<div class="actions">
					<button class="accept" onclick={() => accept(inv)}>Accetta</button>
					<button class="decline" onclick={() => decline(inv)}>Rifiuta</button>
				</div>
			</li>
		{/each}
	</ul>
{/if}

<style>
	.muted {
		color: var(--muted);
	}
	.error {
		color: #f87171;
	}
	.list {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
	}
	.list li {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 0.75rem;
		padding: 0.8rem 1rem;
		background: var(--panel);
		border-radius: 10px;
		flex-wrap: wrap;
	}
	.actions {
		display: flex;
		gap: 0.5rem;
	}
	button {
		padding: 0.5rem 1rem;
		border: none;
		border-radius: 8px;
		cursor: pointer;
		color: white;
		font-size: 0.9rem;
	}
	.accept {
		background: #16a34a;
	}
	.decline {
		background: #475569;
	}
</style>
