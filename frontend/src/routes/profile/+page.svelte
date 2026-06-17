<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api';
	import { auth, setSession } from '$lib/auth.svelte';
	import {
		EYES,
		NOSES,
		MOUTHS,
		HATS,
		parseAvatar,
		serializeAvatar,
		renderAvatar,
		type AvatarSpec
	} from '$lib/avatar';

	type Profile = { username: string; displayName: string; avatar: string | null; role: string };
	type AuthResponse = { token: string; username: string; displayName: string; role: string };
	type GameStat = { game: string; played: number; wins: number; losses: number; draws: number };
	type Stats = {
		total: number;
		wins: number;
		losses: number;
		draws: number;
		games: GameStat[];
		recent: { game: string; result: string; playedAt: string }[];
	};

	const GAME_LABELS: Record<string, string> = {
		connect4: 'Forza 4',
		hangman: 'Impiccato',
		quiz: 'Quiz',
		battleship: 'Battaglia navale',
		minesweeper: 'Campo minato',
		tris: 'Tris',
		dama: 'Dama',
		chess: 'Scacchi',
		daily: 'Parola del Giorno'
	};
	const gameLabel = (g: string) => GAME_LABELS[g] ?? g;

	let profile = $state<Profile | null>(null);
	let stats = $state<Stats | null>(null);
	let loadError = $state('');

	const winRate = $derived(stats && stats.total > 0 ? Math.round((stats.wins / stats.total) * 100) : 0);

	// Avatar
	let spec = $state<AvatarSpec>({ eyes: 0, nose: 0, mouth: 0, hat: 0 });
	const preview = $derived(renderAvatar(spec));

	// Form displayName
	let displayName = $state('');
	let profileMsg = $state('');
	let profileErr = $state('');

	// Form password
	let currentPassword = $state('');
	let newPassword = $state('');
	let pwMsg = $state('');
	let pwErr = $state('');

	function cycle(field: keyof AvatarSpec, len: number, dir: number) {
		spec[field] = (spec[field] + dir + len) % len;
	}

	async function load() {
		try {
			profile = await api<Profile>('/api/me');
			displayName = profile.displayName;
			spec = parseAvatar(profile.avatar);
			stats = await api<Stats>('/api/me/stats');
		} catch (e) {
			loadError = (e as Error).message;
		}
	}

	onMount(() => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		load();
	});

	async function saveProfile() {
		profileMsg = '';
		profileErr = '';
		try {
			const res = await api<AuthResponse>('/api/me/profile', {
				method: 'PUT',
				body: JSON.stringify({ displayName, avatar: serializeAvatar(spec) })
			});
			// Aggiorna la sessione col nuovo token/displayName/ruolo
			setSession({
				token: res.token,
				username: res.username,
				displayName: res.displayName,
				role: res.role
			});
			profileMsg = '✓ Profilo aggiornato';
		} catch (e) {
			profileErr = (e as Error).message;
		}
	}

	async function changePassword(e: SubmitEvent) {
		e.preventDefault();
		pwMsg = '';
		pwErr = '';
		try {
			await api('/api/me/password', {
				method: 'PUT',
				body: JSON.stringify({ currentPassword, newPassword })
			});
			pwMsg = '✓ Password aggiornata';
			currentPassword = '';
			newPassword = '';
		} catch (err) {
			pwErr = (err as Error).message;
		}
	}
</script>

<h1>Area personale</h1>
{#if loadError}<p class="err">⚠ {loadError}</p>{/if}

{#if profile}
	<p class="sub">@{profile.username}</p>

	<section class="panel">
		<h2>🙂 Avatar</h2>
		<div class="avatar-builder">
			<pre class="avatar-preview">{preview}</pre>
			<div class="controls">
				<div class="ctrl">
					<button onclick={() => cycle('hat', HATS.length, -1)}>◀</button>
					<span>Cappello</span>
					<button onclick={() => cycle('hat', HATS.length, 1)}>▶</button>
				</div>
				<div class="ctrl">
					<button onclick={() => cycle('eyes', EYES.length, -1)}>◀</button>
					<span>Occhi</span>
					<button onclick={() => cycle('eyes', EYES.length, 1)}>▶</button>
				</div>
				<div class="ctrl">
					<button onclick={() => cycle('nose', NOSES.length, -1)}>◀</button>
					<span>Naso</span>
					<button onclick={() => cycle('nose', NOSES.length, 1)}>▶</button>
				</div>
				<div class="ctrl">
					<button onclick={() => cycle('mouth', MOUTHS.length, -1)}>◀</button>
					<span>Bocca</span>
					<button onclick={() => cycle('mouth', MOUTHS.length, 1)}>▶</button>
				</div>
			</div>
		</div>
	</section>

	<section class="panel">
		<h2>👤 Nome visualizzato</h2>
		<div class="row">
			<input bind:value={displayName} maxlength="40" placeholder="Nome visualizzato" />
			<button onclick={saveProfile}>Salva profilo</button>
		</div>
		{#if profileMsg}<p class="ok">{profileMsg}</p>{/if}
		{#if profileErr}<p class="err">{profileErr}</p>{/if}
		<p class="hint">Salva profilo aggiorna anche l'avatar.</p>
	</section>

	<section class="panel">
		<h2>🔒 Cambia password</h2>
		<form onsubmit={changePassword} class="pw-form">
			<input
				type="password"
				bind:value={currentPassword}
				placeholder="Password attuale"
				autocomplete="current-password"
			/>
			<input
				type="password"
				bind:value={newPassword}
				placeholder="Nuova password (min 6)"
				autocomplete="new-password"
			/>
			<button type="submit" disabled={!currentPassword || newPassword.length < 6}>Aggiorna password</button>
		</form>
		{#if pwMsg}<p class="ok">{pwMsg}</p>{/if}
		{#if pwErr}<p class="err">{pwErr}</p>{/if}
	</section>

	{#if stats}
		<section class="panel">
			<h2>📊 Le tue statistiche</h2>
			{#if stats.total === 0}
				<p class="hint">Nessuna partita registrata. Gioca qualcosa dalla home!</p>
			{:else}
				<div class="summary">
					<div class="stat"><span class="num">{stats.total}</span> partite</div>
					<div class="stat win"><span class="num">{stats.wins}</span> vinte</div>
					<div class="stat lose"><span class="num">{stats.losses}</span> perse</div>
					<div class="stat draw"><span class="num">{stats.draws}</span> pari</div>
					<div class="stat"><span class="num">{winRate}%</span> win rate</div>
				</div>

				<h3>Win rate per gioco</h3>
				<div class="bars">
					{#each stats.games as g (g.game)}
						<div class="bar-row">
							<span class="bar-label">{gameLabel(g.game)}</span>
							<div class="bar-track">
								<div
									class="bar-fill"
									style="width: {g.played > 0 ? Math.round((g.wins / g.played) * 100) : 0}%"
								></div>
							</div>
							<span class="bar-val">{g.wins}/{g.played}</span>
						</div>
					{/each}
				</div>

				<h3>Ultime partite</h3>
				<div class="timeline">
					{#each stats.recent as r, i (i)}
						<span
							class="dot"
							class:win={r.result === 'WIN'}
							class:lose={r.result === 'LOSE'}
							class:draw={r.result === 'DRAW'}
							title="{gameLabel(r.game)} — {r.result}"
						></span>
					{/each}
				</div>
			{/if}
		</section>
	{/if}
{/if}

<style>
	.sub {
		color: var(--muted);
		margin: 0 0 1rem;
	}
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
	.avatar-builder {
		display: flex;
		gap: 1.5rem;
		align-items: center;
		flex-wrap: wrap;
	}
	.avatar-preview {
		font-family: ui-monospace, monospace;
		font-size: 1.1rem;
		line-height: 1.15;
		background: #0f172a;
		padding: 0.75rem 1rem;
		border-radius: 8px;
		margin: 0;
	}
	.controls {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}
	.ctrl {
		display: flex;
		align-items: center;
		gap: 0.6rem;
	}
	.ctrl span {
		min-width: 5.5rem;
		text-align: center;
		color: var(--muted);
	}
	.ctrl button {
		width: 2.2rem;
		height: 2.2rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		cursor: pointer;
	}
	.row {
		display: flex;
		gap: 0.75rem;
		flex-wrap: wrap;
	}
	input {
		padding: 0.55rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font-size: 1rem;
	}
	.row input {
		flex: 1;
		min-width: 12rem;
	}
	.pw-form {
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
		max-width: 22rem;
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
	.ok {
		color: #4ade80;
		margin: 0.5rem 0 0;
	}
	.err {
		color: #f87171;
		margin: 0.5rem 0 0;
	}
	.hint {
		color: var(--muted);
		font-size: 0.8rem;
		margin: 0.5rem 0 0;
	}
	h3 {
		font-size: 0.95rem;
		margin: 1.25rem 0 0.6rem;
		color: var(--muted);
	}
	.summary {
		display: flex;
		gap: 0.75rem;
		flex-wrap: wrap;
	}
	.stat {
		background: #0f172a;
		border: 1px solid #334155;
		border-radius: 8px;
		padding: 0.5rem 0.9rem;
		font-size: 0.85rem;
		color: var(--muted);
	}
	.stat .num {
		font-size: 1.3rem;
		font-weight: 700;
		color: var(--text);
		margin-right: 0.3rem;
	}
	.stat.win .num {
		color: #4ade80;
	}
	.stat.lose .num {
		color: #f87171;
	}
	.stat.draw .num {
		color: #60a5fa;
	}
	.bars {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}
	.bar-row {
		display: flex;
		align-items: center;
		gap: 0.6rem;
	}
	.bar-label {
		flex: 0 0 9rem;
		font-size: 0.85rem;
	}
	.bar-track {
		flex: 1;
		height: 0.9rem;
		background: #0f172a;
		border-radius: 6px;
		overflow: hidden;
	}
	.bar-fill {
		height: 100%;
		background: linear-gradient(90deg, #22c55e, #4ade80);
		border-radius: 6px;
		transition: width 0.3s;
	}
	.bar-val {
		flex: 0 0 3rem;
		text-align: right;
		font-size: 0.8rem;
		color: var(--muted);
	}
	.timeline {
		display: flex;
		gap: 0.3rem;
		flex-wrap: wrap;
	}
	.dot {
		width: 1rem;
		height: 1rem;
		border-radius: 50%;
		background: #334155;
	}
	.dot.win {
		background: #22c55e;
	}
	.dot.lose {
		background: #ef4444;
	}
	.dot.draw {
		background: #3b82f6;
	}
</style>
