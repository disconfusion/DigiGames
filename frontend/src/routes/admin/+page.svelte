<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { gameLabel } from '$lib/games/catalog';

	type Bug = {
		id: number;
		username: string;
		displayName: string;
		game: string;
		description: string;
		createdAt: string;
	};
	type AdminUser = { username: string; displayName: string; role: string };

	let bugs = $state<Bug[]>([]);
	let users = $state<AdminUser[]>([]);
	let error = $state('');
	let notice = $state('');

	function flash(msg: string) {
		notice = msg;
		setTimeout(() => (notice = ''), 2500);
	}

	async function load() {
		try {
			bugs = await api<Bug[]>('/api/bugs');
			users = await api<AdminUser[]>('/api/admin/users');
		} catch (e) {
			error = (e as Error).message;
		}
	}

	onMount(() => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		if (auth.session.role !== 'admin') {
			goto('/');
			return;
		}
		load();
	});

	async function deleteBug(id: number) {
		try {
			await api(`/api/bugs/${id}`, { method: 'DELETE' });
			bugs = bugs.filter((b) => b.id !== id);
			flash('Bug eliminato');
		} catch (e) {
			error = (e as Error).message;
		}
	}

	async function resetStats(u: AdminUser) {
		if (!confirm(`Azzerare le statistiche di ${u.displayName}?`)) return;
		try {
			const r = await api<{ message: string }>(`/api/admin/users/${u.username}/reset-stats`, {
				method: 'POST'
			});
			flash(r.message);
		} catch (e) {
			error = (e as Error).message;
		}
	}

	async function dailyReset(u: AdminUser) {
		try {
			const r = await api<{ message: string }>(`/api/admin/users/${u.username}/daily-reset`, {
				method: 'POST'
			});
			flash(r.message);
		} catch (e) {
			error = (e as Error).message;
		}
	}

	async function resetPassword(u: AdminUser) {
		const np = prompt(`Nuova password per ${u.username} (min 6):`);
		if (!np) return;
		try {
			const r = await api<{ message: string }>(`/api/admin/users/${u.username}/reset-password`, {
				method: 'POST',
				body: JSON.stringify({ newPassword: np })
			});
			flash(r.message);
		} catch (e) {
			error = (e as Error).message;
		}
	}
</script>

<h1>🛠 Pannello Admin</h1>
{#if error}<p class="err">{error}</p>{/if}
{#if notice}<p class="ok">{notice}</p>{/if}

<section class="panel">
	<h2>🐞 Segnalazioni bug ({bugs.length})</h2>
	{#if bugs.length === 0}
		<p class="muted">Nessuna segnalazione.</p>
	{:else}
		<ul class="bugs">
			{#each bugs as b (b.id)}
				<li>
					<div class="bug-meta">
						<strong>{b.displayName}</strong>
						<span class="muted">@{b.username} · {gameLabel(b.game)} · {b.createdAt.slice(0, 16).replace('T', ' ')}</span>
					</div>
					<p class="bug-desc">{b.description}</p>
					<button class="danger" onclick={() => deleteBug(b.id)}>Elimina</button>
				</li>
			{/each}
		</ul>
	{/if}
</section>

<section class="panel">
	<h2>👥 Utenti ({users.length})</h2>
	<ul class="users">
		{#each users as u (u.username)}
			<li>
				<div>
					<strong>{u.displayName}</strong>
					<span class="muted">@{u.username}{u.role === 'admin' ? ' · admin' : ''}</span>
				</div>
				<div class="actions">
					<button onclick={() => dailyReset(u)}>Reset parola del giorno</button>
					<button onclick={() => resetStats(u)}>Azzera stats</button>
					<button onclick={() => resetPassword(u)}>Reset password</button>
				</div>
			</li>
		{/each}
	</ul>
</section>

<style>
	.panel {
		background: var(--panel);
		padding: 1rem 1.25rem;
		border-radius: 12px;
		margin-bottom: 1.25rem;
	}
	h2 {
		font-size: 1.05rem;
		margin: 0 0 0.75rem;
	}
	.muted {
		color: var(--muted);
		font-size: 0.85rem;
	}
	.err {
		color: #f87171;
	}
	.ok {
		color: #4ade80;
	}
	.bugs,
	.users {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
	}
	.bugs li {
		background: #0f172a;
		border-radius: 8px;
		padding: 0.7rem 0.9rem;
	}
	.bug-meta {
		display: flex;
		flex-direction: column;
	}
	.bug-desc {
		margin: 0.5rem 0;
		white-space: pre-wrap;
	}
	.users li {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 0.75rem;
		background: #0f172a;
		border-radius: 8px;
		padding: 0.6rem 0.9rem;
		flex-wrap: wrap;
	}
	.actions {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
	}
	button {
		padding: 0.45rem 0.9rem;
		border: 1px solid #334155;
		border-radius: 8px;
		background: #1e293b;
		color: var(--text);
		cursor: pointer;
		font-size: 0.85rem;
	}
	button.danger {
		background: #7f1d1d;
		border-color: #991b1b;
		color: #fecaca;
		margin-top: 0.4rem;
	}
</style>
