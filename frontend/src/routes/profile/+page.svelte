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
	import Icon from '$lib/icons/Icon.svelte';

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

	type Companion = { id: string; name: string; description: string; cost: number; owned: boolean; equipped: boolean };

	let profile = $state<Profile | null>(null);
	let stats = $state<Stats | null>(null);
	let loadError = $state('');

	let companions = $state<Companion[]>([]);
	const ownedCompanions = $derived(companions.filter((c) => c.owned));
	const equippedCompanion = $derived(companions.find((c) => c.equipped)?.id ?? '');

	type House = { id: string; name: string };
	let houseList = $state<House[]>([]);
	let myHouse = $state('');

	async function joinHouse(id: string) {
		try {
			const r = await api<{ mine: string }>(`/api/houses/${id}/join`, { method: 'POST' });
			myHouse = r.mine;
		} catch (e) {
			loadError = (e as Error).message;
		}
	}

	async function equipCompanion(c: Companion) {
		const target = c.equipped ? 'none' : c.id;
		try {
			const r = await api<{ equipped: string }>(`/api/shop/companions/${target}/equip`, { method: 'POST' });
			companions = companions.map((x) => ({ ...x, equipped: x.id === r.equipped }));
		} catch (e) {
			loadError = (e as Error).message;
		}
	}

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
			const cr = await api<{ companions: Companion[] }>('/api/shop/companions');
			companions = cr.companions;
			const hr = await api<{ houses: House[]; mine: string }>('/api/houses');
			houseList = hr.houses;
			myHouse = hr.mine;
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
			profileMsg = 'Profilo aggiornato';
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
			pwMsg = 'Password aggiornata';
			currentPassword = '';
			newPassword = '';
		} catch (err) {
			pwErr = (err as Error).message;
		}
	}
</script>

<h1>Area personale</h1>
{#if loadError}<p class="err"><Icon name="warning" size={14} /> {loadError}</p>{/if}

{#if profile}
	<p class="sub">@{profile.username}</p>

	<div class="profile-grid">
	<section class="panel">
		<h2><Icon name="smiley" size={18} title="Avatar" /> Avatar</h2>
		<div class="avatar-builder">
			<div class="avatar-stage">
				<pre class="avatar-preview">{preview}</pre>
				{#if equippedCompanion}
					<div class="orbit"><div class="orbit-pos"><Icon name={equippedCompanion} size={28} title="Companion" /></div></div>
				{/if}
			</div>
			<div class="controls">
				<div class="ctrl">
					<button onclick={() => cycle('hat', HATS.length, -1)}><Icon name="arrow_left" size={16} title="Precedente" /></button>
					<span>Cappello</span>
					<button onclick={() => cycle('hat', HATS.length, 1)}><Icon name="arrow_right" size={16} title="Successivo" /></button>
				</div>
				<div class="ctrl">
					<button onclick={() => cycle('eyes', EYES.length, -1)}><Icon name="arrow_left" size={16} title="Precedente" /></button>
					<span>Occhi</span>
					<button onclick={() => cycle('eyes', EYES.length, 1)}><Icon name="arrow_right" size={16} title="Successivo" /></button>
				</div>
				<div class="ctrl">
					<button onclick={() => cycle('nose', NOSES.length, -1)}><Icon name="arrow_left" size={16} title="Precedente" /></button>
					<span>Naso</span>
					<button onclick={() => cycle('nose', NOSES.length, 1)}><Icon name="arrow_right" size={16} title="Successivo" /></button>
				</div>
				<div class="ctrl">
					<button onclick={() => cycle('mouth', MOUTHS.length, -1)}><Icon name="arrow_left" size={16} title="Precedente" /></button>
					<span>Bocca</span>
					<button onclick={() => cycle('mouth', MOUTHS.length, 1)}><Icon name="arrow_right" size={16} title="Successivo" /></button>
				</div>
			</div>
		</div>
	</section>

	<section class="panel">
		<h2><Icon name="person" size={18} title="Nome" /> Nome visualizzato</h2>
		<div class="row">
			<input bind:value={displayName} maxlength="40" placeholder="Nome visualizzato" />
			<button onclick={saveProfile}>Salva profilo</button>
		</div>
		{#if profileMsg}<p class="ok"><Icon name="check" size={14} /> {profileMsg}</p>{/if}
		{#if profileErr}<p class="err"><Icon name="warning" size={14} /> {profileErr}</p>{/if}
		<p class="hint">Salva profilo aggiorna anche l'avatar.</p>
	</section>

	<section class="panel">
		<h2><Icon name="lock" size={18} title="Password" /> Cambia password</h2>
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
		{#if pwMsg}<p class="ok"><Icon name="check" size={14} /> {pwMsg}</p>{/if}
		{#if pwErr}<p class="err"><Icon name="warning" size={14} /> {pwErr}</p>{/if}
	</section>

	<section class="panel">
		<h2><Icon name={equippedCompanion || 'leone'} size={18} title="Companion" /> Companion</h2>
		{#if ownedCompanions.length === 0}
			<p class="hint">Non possiedi companion. Compratene uno nello <a href="/shop">shop</a>!</p>
		{:else}
			<div class="companion-grid">
				{#each ownedCompanions as c (c.id)}
					<button class="comp" class:on={c.equipped} onclick={() => equipCompanion(c)} title={c.name}>
						<Icon name={c.id} size={40} title={c.name} />
						<span class="comp-name">{c.name}</span>
						{#if c.equipped}<span class="comp-tag">ON</span>{/if}
					</button>
				{/each}
			</div>
			<p class="hint">Clicca per equipaggiare/togliere. Appare nell'header, attorno all'avatar e nelle stanze.</p>
		{/if}
	</section>

	<section class="panel">
		<h2><Icon name={myHouse || 'grifondoro'} size={18} title="Casata" /> Casata</h2>
		<div class="house-grid">
			{#each houseList as h (h.id)}
				<button class="house" class:on={myHouse === h.id} onclick={() => joinHouse(h.id)} title={h.name}>
					<Icon name={h.id} size={44} title={h.name} />
					<span class="house-name">{h.name}</span>
				</button>
			{/each}
		</div>
		<p class="hint">Scegli la tua casata: lo stemma appare nel profilo, nell'header, in classifica e nelle stanze. La <a href="/leaderboard">Classifica</a> ha la sezione Casate.</p>
	</section>

	{#if stats}
		<section class="panel stats-panel">
			<h2><Icon name="chart" size={18} title="Statistiche" /> Le tue statistiche</h2>
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
	</div>
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
	/* Dashboard responsive: su schermi larghi i pannelli avatar/nome/password
	   si affiancano (auto-fit), le statistiche restano a tutta larghezza sotto.
	   Su mobile collassa naturalmente a una colonna. */
	.profile-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
		gap: 1.25rem;
		align-items: start;
	}
	.profile-grid .panel {
		margin-bottom: 0;
	}
	.stats-panel {
		grid-column: 1 / -1;
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
		padding: 0;
		display: flex;
		align-items: center;
		justify-content: center;
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
		display: flex;
		align-items: center;
		gap: 0.35rem;
	}
	.err {
		color: #f87171;
		margin: 0.5rem 0 0;
		display: flex;
		align-items: center;
		gap: 0.35rem;
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

	/* Companion che orbita attorno all'avatar */
	.avatar-stage {
		position: relative;
		display: inline-flex;
	}
	.orbit {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 0;
		height: 0;
		pointer-events: none;
		animation: orbit 7s linear infinite;
	}
	.orbit-pos {
		position: absolute;
		transform: translate(-50%, -50%) translateY(-58px);
	}
	@keyframes orbit {
		to {
			transform: rotate(360deg);
		}
	}

	/* Griglia companion posseduti */
	.companion-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(82px, 1fr));
		gap: 0.5rem;
	}
	.comp {
		position: relative;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.25rem;
		padding: 0.5rem 0.3rem;
		background: #0f172a;
		border: 2px solid #334155;
		border-radius: 8px;
		color: var(--muted);
		cursor: pointer;
	}
	.comp.on {
		border-color: var(--cyan);
		color: var(--text);
		box-shadow: var(--glow-cyan);
	}
	.comp-name {
		font-size: 0.65rem;
		text-align: center;
		line-height: 1.1;
	}
	.comp-tag {
		position: absolute;
		top: 0.2rem;
		right: 0.25rem;
		font-size: 0.55rem;
		font-weight: 700;
		color: var(--cyan);
	}

	/* Scelta casata */
	.house-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(90px, 1fr));
		gap: 0.5rem;
	}
	.house {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.3rem;
		padding: 0.6rem 0.3rem;
		background: #0f172a;
		border: 2px solid #334155;
		border-radius: 10px;
		color: var(--muted);
		cursor: pointer;
	}
	.house.on {
		border-color: var(--amber);
		color: var(--text);
		box-shadow: 0 0 10px rgba(255, 207, 63, 0.45);
	}
	.house-name {
		font-size: 0.7rem;
		font-family: var(--font-ui);
		letter-spacing: 0.02em;
	}

	@media (prefers-reduced-motion: reduce) {
		.orbit {
			animation: none;
		}
	}
</style>
