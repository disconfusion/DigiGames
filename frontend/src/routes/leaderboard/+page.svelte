<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api';
	import { parseAvatar, renderAvatar } from '$lib/avatar';
	import Icon from '$lib/icons/Icon.svelte';

	type GameStats = { played: number; wins: number };

	type UserRow = {
		username: string;
		displayName: string;
		avatar: string | null;
		house: string | null;
		points: number;
		wins: number;
		draws: number;
		total: number;
		games: Record<string, GameStats>;
	};

	type HouseRow = { id: string; name: string; points: number; members: number };

	const GAME_LABELS: Record<string, string> = {
		connect4: 'Forza 4',
		battleship: 'Battaglia Navale',
		quiz: 'Quiz',
		hangman: 'Impiccato',
		minesweeper: 'Campo Minato',
		tris: 'Tris',
		dama: 'Dama',
		chess: 'Scacchi',
		pong: 'Pong',
		battlecity: 'Battle City'
	};

	const face = (avatar: string | null) => renderAvatar(parseAvatar(avatar));

	const HOUSE_NAMES: Record<string, string> = {
		grifondoro: 'Grifondoro',
		serpeverde: 'Serpeverde',
		corvonero: 'Corvonero',
		tassorosso: 'Tassorosso'
	};

	let rows = $state<UserRow[]>([]);
	let houses = $state<HouseRow[]>([]);
	let tab = $state<'players' | 'houses'>('players');
	let loading = $state(true);
	let error = $state('');

	onMount(async () => {
		try {
			rows = await api<UserRow[]>('/api/leaderboard');
			houses = await api<HouseRow[]>('/api/houses/standings');
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
	<div class="lb-tabs">
		<button class:active={tab === 'players'} onclick={() => (tab = 'players')}>Giocatori</button>
		<button class:active={tab === 'houses'} onclick={() => (tab = 'houses')}>Casate</button>
	</div>

	{#if loading}
		<p class="muted">Caricamento…</p>
	{:else if error}
		<p class="err">⚠ {error}</p>
	{:else if tab === 'players'}
		<p class="sub">Punti cumulativi (vittoria 3 · pareggio 1 · sconfitta 0)</p>
		{#if rows.length === 0}
			<p class="muted">Nessuna partita completata ancora. Iniziate a giocare!</p>
		{:else}
			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>#</th>
							<th>Giocatore</th>
							<th>Punti</th>
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
									{#if i === 0}<Icon name="gold" size={26} title="1º" />{:else if i === 1}<Icon name="silver" size={26} title="2º" />{:else if i === 2}<Icon name="bronze" size={26} title="3º" />{:else}{i + 1}{/if}
								</td>
								<td>
									<div class="player-cell">
										<pre class="mini-face">{face(row.avatar)}</pre>
										<span>
											<span class="name">{#if row.house}<Icon name={row.house} size={16} title={HOUSE_NAMES[row.house] ?? ''} /> {/if}{row.displayName}</span>
											<span class="uname">@{row.username}</span>
										</span>
									</div>
								</td>
								<td class="points">{row.points}</td>
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
	{:else}
		<p class="sub">Classifica Casate — somma dei punti di tutti i membri.</p>
		<div class="table-wrap">
			<table>
				<thead>
					<tr><th>#</th><th>Casata</th><th>Punti</th><th>Membri</th></tr>
				</thead>
				<tbody>
					{#each houses as h, i (h.id)}
						<tr class:podium={i < 3} class:gold={i === 0} class:silver={i === 1} class:bronze={i === 2}>
							<td class="rank">
								{#if i === 0}<Icon name="gold" size={26} title="1º" />{:else if i === 1}<Icon name="silver" size={26} title="2º" />{:else if i === 2}<Icon name="bronze" size={26} title="3º" />{:else}{i + 1}{/if}
							</td>
							<td>
								<div class="player-cell">
									<Icon name={h.id} size={34} title={h.name} />
									<span class="name">{h.name}</span>
								</div>
							</td>
							<td class="points">{h.points}</td>
							<td class="total">{h.members}</td>
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
	.lb-tabs {
		display: flex;
		gap: 0.4rem;
	}
	.lb-tabs button {
		padding: 0.4rem 1.1rem;
		border: 1px solid var(--line);
		border-radius: 999px;
		background: var(--panel);
		color: var(--muted);
		cursor: pointer;
		font-family: var(--font-ui);
		font-size: 0.8rem;
		text-transform: uppercase;
		letter-spacing: 0.04em;
	}
	.lb-tabs button.active {
		border-color: var(--amber);
		color: var(--amber);
		text-shadow: 0 0 6px rgba(255, 207, 63, 0.5);
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
	.points {
		font-family: var(--font-display);
		font-size: 0.95rem;
		color: var(--amber);
		text-shadow: 0 0 8px rgba(255, 207, 63, 0.55);
	}
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
