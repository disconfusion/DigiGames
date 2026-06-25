<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api';
	import { parseAvatar, renderAvatar } from '$lib/avatar';

	type GameStats = { played: number; wins: number };

	type UserRow = {
		username: string;
		displayName: string;
		avatar: string | null;
		wins: number;
		total: number;
		games: Record<string, GameStats>;
	};

	const GAME_LABELS: Record<string, string> = {
		connect4: 'Forza 4',
		battleship: 'Battaglia Navale',
		quiz: 'Quiz',
		hangman: 'Impiccato',
		minesweeper: 'Campo Minato',
		tris: 'Tris',
		dama: 'Dama',
		chess: 'Scacchi'
	};

	const face = (avatar: string | null) => renderAvatar(parseAvatar(avatar));

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
	<h1>★ HIGH SCORES ★</h1>
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
						<tr class:podium={i < 3} class:gold={i === 0} class:silver={i === 1} class:bronze={i === 2}>
							<td class="rank">
								{#if i === 0}🥇{:else if i === 1}🥈{:else if i === 2}🥉{:else}{i + 1}{/if}
							</td>
							<td>
								<div class="player-cell">
									<pre class="mini-face">{face(row.avatar)}</pre>
									<span>
										<span class="name">{row.displayName}</span>
										<span class="uname">@{row.username}</span>
									</span>
								</div>
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
	h1 {
		margin: 0;
		font-family: var(--font-display);
		font-size: clamp(1rem, 4vw, 1.5rem);
		color: var(--amber);
		text-shadow: 0 0 10px var(--amber), 0 0 24px rgba(255, 207, 63, 0.4);
	}
	.sub, .muted { color: var(--muted); margin: 0; }
	.err { color: var(--danger); font-family: var(--font-term); font-size: 1.1rem; }

	.table-wrap {
		width: 100%;
		overflow-x: auto;
	}
	table {
		width: 100%;
		border-collapse: collapse;
		font-size: 0.95rem;
	}
	th {
		text-align: left;
		padding: 0.5rem 0.75rem;
		color: var(--muted);
		font-family: var(--font-ui);
		text-transform: uppercase;
		letter-spacing: 0.04em;
		font-size: 0.72rem;
		border-bottom: 1px solid var(--line);
		white-space: nowrap;
	}
	td {
		padding: 0.55rem 0.75rem;
		border-bottom: 1px solid var(--line);
		vertical-align: middle;
	}
	tr.podium td { background: rgba(255, 207, 63, 0.05); }
	tr:hover td { background: var(--inset); }
	/* Cornici podio: oro / argento / bronzo (bordo sinistro luminoso) */
	tr.gold td:first-child { box-shadow: inset 4px 0 0 #ffcf3f; }
	tr.silver td:first-child { box-shadow: inset 4px 0 0 #cdd6e3; }
	tr.bronze td:first-child { box-shadow: inset 4px 0 0 #d98a4a; }

	.rank { font-size: 1.2rem; text-align: center; width: 2.5rem; }
	.player-cell { display: flex; align-items: center; gap: 0.6rem; }
	.mini-face {
		font-family: ui-monospace, monospace;
		font-size: 0.4rem;
		line-height: 1.05;
		margin: 0;
		color: var(--muted);
	}
	.name { display: block; font-weight: 600; }
	.uname { display: block; font-size: 0.78rem; color: var(--muted); }
	.wins {
		font-family: var(--font-display);
		font-size: 0.85rem;
		color: var(--green);
		text-shadow: 0 0 8px rgba(61, 255, 154, 0.5);
	}
	.rate { color: var(--muted); }
	.game-col { text-align: center; }

	@media (max-width: 640px) {
		.game-col { display: none; }
	}
</style>
