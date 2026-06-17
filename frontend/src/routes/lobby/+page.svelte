<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api';
	import { auth } from '$lib/auth.svelte';

	type RoomView = {
		code: string;
		gameSlug: string;
		hostEmail: string;
		status: string;
		players: number;
		maxPlayers: number;
		isPrivate: boolean;
	};

	const GAMES = [
		{ slug: 'connect4', label: 'Forza 4' },
		{ slug: 'hangman', label: 'Impiccato' },
		{ slug: 'quiz', label: 'Quiz' },
		{ slug: 'battleship', label: 'Battaglia navale' },
		{ slug: 'minesweeper', label: 'Campo minato' }
	];
	const labelOf = (slug: string) => GAMES.find((g) => g.slug === slug)?.label ?? slug;

	let rooms = $state<RoomView[]>([]);
	let error = $state('');
	let newGame = $state('connect4');
	let isPrivate = $state(false);
	let joinCode = $state('');

	async function refresh() {
		try {
			rooms = await api<RoomView[]>('/api/rooms');
		} catch (e) {
			error = e instanceof Error ? e.message : 'Errore';
		}
	}

	async function create() {
		try {
			const r = await api<RoomView>('/api/rooms', {
				method: 'POST',
				body: JSON.stringify({ gameSlug: newGame, isPrivate })
			});
			goto(`/room/${r.code}`);
		} catch (e) {
			error = e instanceof Error ? e.message : 'Errore';
		}
	}

	function join(code: string) {
		const c = code.trim().toUpperCase();
		if (c) goto(`/room/${c}`);
	}

	onMount(() => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		refresh();
	});
</script>

<h1>Lobby</h1>
{#if error}<p class="error">{error}</p>{/if}

<section class="panel">
	<h2>Crea una partita</h2>
	<div class="row">
		<select bind:value={newGame}>
			{#each GAMES as g (g.slug)}
				<option value={g.slug}>{g.label}</option>
			{/each}
		</select>
		<label class="check">
			<input type="checkbox" bind:checked={isPrivate} /> Privata (solo su invito)
		</label>
		<button onclick={create}>Crea</button>
	</div>
</section>

<section class="panel">
	<h2>Entra con codice</h2>
	<div class="row">
		<input placeholder="ABC123" bind:value={joinCode} maxlength="6" />
		<button onclick={() => join(joinCode)}>Entra</button>
	</div>
</section>

<section class="panel">
	<div class="row between">
		<h2>Stanze pubbliche</h2>
		<button class="link" onclick={refresh}>↻ Aggiorna</button>
	</div>
	{#if rooms.length === 0}
		<p class="muted">Nessuna stanza aperta. Creane una!</p>
	{:else}
		<ul class="rooms">
			{#each rooms as r (r.code)}
				<li>
					<div>
						<strong>{labelOf(r.gameSlug)}</strong>
						<span class="muted">· {r.code} · {r.players}/{r.maxPlayers} · host {r.hostEmail}</span>
					</div>
					<button onclick={() => join(r.code)} disabled={r.players >= r.maxPlayers}>
						{r.players >= r.maxPlayers ? 'Piena' : 'Entra'}
					</button>
				</li>
			{/each}
		</ul>
	{/if}
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
	.row {
		display: flex;
		gap: 0.75rem;
		align-items: center;
		flex-wrap: wrap;
	}
	.row.between {
		justify-content: space-between;
	}
	select,
	input {
		padding: 0.55rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font-size: 1rem;
	}
	button {
		padding: 0.55rem 1rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: white;
		cursor: pointer;
		font-size: 0.95rem;
	}
	button:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.link {
		background: none;
		color: var(--muted);
	}
	.check {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		color: var(--muted);
		font-size: 0.9rem;
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
		background: #0f172a;
		border-radius: 8px;
		flex-wrap: wrap;
	}
	.muted {
		color: var(--muted);
		font-size: 0.9rem;
	}
	.error {
		color: #f87171;
	}
</style>
