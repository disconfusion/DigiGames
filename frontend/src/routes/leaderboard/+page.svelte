<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api';

	type GameStats = { played: number; wins: number };

	type UserRow = {
		username: string;
		displayName: string;
		wins: number;
		total: number;
		games: Record<string, GameStats>;
	};

	const GAME_LABELS: Record<string, string> = {
		connect4: 'Forza 4',
		battleship: 'Battaglia Navale',
		quiz: 'Quiz',
		hangman: 'Impiccato',
		minesweeper: 'Campo Minato'
	};

	let rows = $state<UserRow[]>([]);
	let loading = $state(true);
	let error = $state('');

	onMount(async () => {
		try {
			rows = await api<UserRow[]>('/api/leaderboard');
		} catch (e) {
			error = (e as Error).message;
		} finally {
			loading = false;
		}
	});

	function winRate(wins: number, total: number): string {
		if (total === 0) return '—';
		return Math.round((wins / total) * 100) + '%';
	}
</script>

<div class="lb">
	<h1>🏆 Classifica Globale</h1>
	<p class="sub">Vittorie cumulative su tutte le partite completate</p>

	{#if loading}
		<p class="muted">Caricamento…</p>
	{:else if error}
		<p class="err">⚠ {error}</p>
	{:else if rows.length === 0}
		<p class="muted">Nessuna partita completata ancora. Iniziate a giocare!</p>
	{:else}
		<div class="table-wrap">
			<table>
				<thead>
					<tr>
						<th>#</th>
						<th>Giocatore</th>
						<th>Vittorie</th>
						<th>Partite</th>
						<th>% Vinte</th>
						{#each Object.keys(GAME_LABELS) as slug}
							<th class="game-col">{GAME_LABELS[slug]}</th>
						{/each}
					</tr>
				</thead>
				<tbody>
					{#each rows as row, i (row.username)}
						<tr class:podium={i < 3}>
							<td class="rank">
								{#if i === 0}🥇{:else if i === 1}🥈{:else if i === 2}🥉{:else}{i + 1}{/if}
							</td>
							<td>
								<span class="name">{row.displayName}</span>
								<span class="uname">@{row.username}</span>
							</td>
							<td class="wins">{row.wins}</td>
							<td class="total">{row.total}</td>
							<td class="rate">{winRate(row.wins, row.total)}</td>
							{#each Object.keys(GAME_LABELS) as slug}
								{@const gs = row.games[slug]}
								<td class="game-col">
									{#if gs}
										{gs.wins}/{gs.played}
									{:else}
										—
									{/if}
								</td>
							{/each}
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</div>

<style>
	.lb {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
	}
	h1 { margin: 0; }
	.sub, .muted { color: var(--muted); margin: 0; }
	.err { color: #f87171; }

	.table-wrap {
		width: 100%;
		overflow-x: auto;
	}
	table {
		width: 100%;
		border-collapse: collapse;
		font-size: 0.9rem;
	}
	th {
		text-align: left;
		padding: 0.5rem 0.75rem;
		color: var(--muted);
		border-bottom: 1px solid #334155;
		white-space: nowrap;
	}
	td {
		padding: 0.55rem 0.75rem;
		border-bottom: 1px solid #1e293b;
		vertical-align: middle;
	}
	tr.podium td { background: #1a2540; }
	tr:hover td { background: #1e293b; }

	.rank { font-size: 1.1rem; text-align: center; width: 2.5rem; }
	.name { display: block; font-weight: 600; }
	.uname { display: block; font-size: 0.78rem; color: var(--muted); }
	.wins { font-weight: 700; color: #86efac; }
	.rate { color: var(--muted); }
	.game-col { text-align: center; }

	@media (max-width: 640px) {
		.game-col { display: none; }
	}
</style>
