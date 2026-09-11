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
		type AvatarSpec
	} from '$lib/avatar';
	import Icon from '$lib/icons/Icon.svelte';
	import Avatar from '$lib/Avatar.svelte';
	import { showToast } from '$lib/notifications.svelte';
	import { setBalance } from '$lib/wallet.svelte';

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
		pong: 'Pong',
		battlecity: 'Battle City',
		poker: 'Poker',
		daily: 'Parola del Giorno'
	};
	const gameLabel = (g: string) => GAME_LABELS[g] ?? g;

	type Companion = {
		id: string;
		name: string;
		description: string;
		cost: number;
		owned: boolean;
		equipped: boolean;
		tintable?: boolean;
		tint?: string;
		/** Forme alternative oltre a "base" (es. il pipistrello → vampiro) */
		forms?: string[];
		form?: string;
	};
	type Accessory = { id: string; name: string; description: string; slot: string; cost: number; owned: boolean; equipped: boolean };

	let profile = $state<Profile | null>(null);
	let stats = $state<Stats | null>(null);
	let loadError = $state('');

	let companions = $state<Companion[]>([]);
	const ownedCompanions = $derived(companions.filter((c) => c.owned));
	/** Sprite da disegnare: id nudo, o "<id>_<forma>" se trasformato (come fa il backend). */
	const spriteOf = (c: Companion) =>
		!c.form || c.form === 'base' ? c.id : `${c.id}_${c.form}`;
	const equipped = $derived(companions.find((c) => c.equipped));
	const equippedCompanion = $derived(equipped ? spriteOf(equipped) : '');
	const equippedTint = $derived(equipped?.tint ?? '');
	/** Companion posseduti con almeno una forma alternativa: hanno il tasto trasformazione. */
	const shapeShifters = $derived(companions.filter((c) => c.owned && (c.forms?.length ?? 0) > 0));

	// Trasformazione in corso per companion (mostra la nuvola di fumo)
	let morphing = $state<Record<string, boolean>>({});
	const reduceMotion = () =>
		typeof window !== 'undefined' &&
		(window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false);

	/**
	 * Passa alla forma alternativa (o torna alla base). Lo swap della sprite avviene a metà
	 * animazione, così il companion "esce dal fumo" già trasformato.
	 */
	async function toggleForm(c: Companion) {
		if (morphing[c.id]) return;
		const next = (c.form ?? 'base') === 'base' ? (c.forms?.[0] ?? 'base') : 'base';
		const still = reduceMotion();
		morphing = { ...morphing, [c.id]: true };
		try {
			const r = await api<{ form: string }>(`/api/shop/companions/${c.id}/form`, {
				method: 'POST',
				body: JSON.stringify({ form: next })
			});
			const apply = () => {
				companions = companions.map((x) => (x.id === c.id ? { ...x, form: r.form } : x));
			};
			if (still) apply();
			else setTimeout(apply, 320);
		} catch (e) {
			showToast((e as Error).message, 'error');
		} finally {
			const end = () => (morphing = { ...morphing, [c.id]: false });
			if (still) end();
			else setTimeout(end, 900);
		}
	}

	let accessories = $state<Accessory[]>([]);
	const ownedAccessories = $derived(accessories.filter((a) => a.owned));
	const equippedAccessories = $derived(
		accessories.filter((a) => a.equipped).map((a) => ({ id: a.id, slot: a.slot }))
	);

	// #4 — varietà nell'orbita dei companion: velocità, verso, raggio, pulse e **traiettoria**.
	// I companion "generici" prendono una variante circolare scelta in modo deterministico dall'id
	// (stesso companion = stessa orbita); quelli che volano hanno un percorso proprio.
	type Orbit = {
		dur: string;
		dir: string;
		radius: string;
		bob: string;
		path: 'circle' | 'zigzag' | 'ellipse' | 'swoop';
	};

	const ORBIT_VARIANTS: Orbit[] = [
		{ dur: '7s', dir: 'normal', radius: '-58px', bob: '2.6s', path: 'circle' },
		{ dur: '5s', dir: 'reverse', radius: '-64px', bob: '1.9s', path: 'circle' },
		{ dur: '9s', dir: 'normal', radius: '-52px', bob: '3.2s', path: 'circle' },
		{ dur: '6s', dir: 'reverse', radius: '-60px', bob: '2.2s', path: 'circle' }
	];

	/** Traiettorie speciali: zig-zag da insetto, ellisse planata, picchiata avanti e indietro. */
	const ORBIT_PATHS: Record<string, Orbit> = {
		zigzag: { dur: '4.2s', dir: 'normal', radius: '-60px', bob: '0.3s', path: 'zigzag' },
		ellipse: { dur: '7.5s', dir: 'normal', radius: '-66px', bob: '2.4s', path: 'ellipse' },
		swoop: { dur: '6s', dir: 'reverse', radius: '-62px', bob: '1.6s', path: 'swoop' }
	};

	/** Chi vola si muove come vola: api e vespe a scatti, farfalla in planata, pipistrello a picchiate. */
	const ORBIT_BY_COMPANION: Record<string, keyof typeof ORBIT_PATHS> = {
		ape: 'zigzag',
		vespa: 'zigzag',
		calabrone: 'zigzag',
		scarabeo: 'zigzag',
		farfalla: 'ellipse',
		pipistrello: 'swoop',
		batman: 'swoop',
		gondola: 'ellipse',
		frog: 'swoop'
	};

	function orbitHash(id: string): number {
		let h = 0;
		for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) | 0;
		return Math.abs(h);
	}

	const orbit = $derived.by<Orbit>(() => {
		const id = equipped?.id ?? '';
		const named = ORBIT_BY_COMPANION[id];
		return named ? ORBIT_PATHS[named] : ORBIT_VARIANTS[orbitHash(id) % ORBIT_VARIANTS.length];
	});

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

	async function equipAccessory(a: Accessory) {
		try {
			const r = await api<{ equipped: { id: string; slot: string }[] }>(
				`/api/shop/accessories/${a.id}/equip`,
				{ method: 'POST' }
			);
			const eq = new Set(r.equipped.map((e) => e.id));
			accessories = accessories.map((x) => ({ ...x, equipped: eq.has(x.id) }));
		} catch (e) {
			loadError = (e as Error).message;
		}
	}

	// ── Regala a un collega (dono user→user) ─────────────────────────────────
	type GiftUser = { username: string; displayName: string };
	type GiftItem = { id: string; name?: string; label?: string; slot?: string; cost: number; ownedByRecipient?: boolean };
	type GiftOptions = { balance: number; companions: GiftItem[]; accessories: GiftItem[]; powers: GiftItem[] };

	let giftUsers = $state<GiftUser[]>([]);
	let giftTo = $state('');
	let giftKind = $state<'tokens' | 'power' | 'companion' | 'accessory'>('tokens');
	let giftAmount = $state(50);
	let giftOptions = $state<GiftOptions | null>(null);
	let giftPowerId = $state('');
	let giftCompanionId = $state('');
	let giftAccessoryId = $state('');
	let giftBusy = $state(false);

	async function loadGiftOptions(to: string) {
		try {
			giftOptions = await api<GiftOptions>(`/api/gifts/options?to=${encodeURIComponent(to)}`);
			giftPowerId = giftOptions.powers[0]?.id ?? '';
			giftCompanionId = giftOptions.companions.find((c) => !c.ownedByRecipient)?.id ?? '';
			giftAccessoryId = giftOptions.accessories.find((a) => !a.ownedByRecipient)?.id ?? '';
		} catch (e) {
			showToast((e as Error).message, 'error');
		}
	}

	$effect(() => {
		if (giftTo) loadGiftOptions(giftTo);
	});

	// Il cosmetico selezionato è già posseduto dal destinatario? (blocca l'invio lato UI)
	const giftBlocked = $derived.by(() => {
		if (!giftOptions) return false;
		if (giftKind === 'companion')
			return giftOptions.companions.find((c) => c.id === giftCompanionId)?.ownedByRecipient ?? false;
		if (giftKind === 'accessory')
			return giftOptions.accessories.find((a) => a.id === giftAccessoryId)?.ownedByRecipient ?? false;
		return false;
	});

	async function sendGift() {
		if (!giftTo || giftBusy || giftBlocked) return;
		const body =
			giftKind === 'tokens'
				? { toUsername: giftTo, type: 'tokens', amount: giftAmount }
				: giftKind === 'power'
					? { toUsername: giftTo, type: 'power', id: giftPowerId }
					: giftKind === 'companion'
						? { toUsername: giftTo, type: 'companion', id: giftCompanionId }
						: { toUsername: giftTo, type: 'accessory', id: giftAccessoryId };
		giftBusy = true;
		try {
			const r = await api<{ message: string; balance: number }>('/api/gifts/send', {
				method: 'POST',
				body: JSON.stringify(body)
			});
			showToast(r.message, 'success');
			if (giftOptions) giftOptions.balance = r.balance;
			setBalance(r.balance); // badge in header allineato all'istante
			loadGiftOptions(giftTo); // aggiorna i flag "già posseduto"
		} catch (e) {
			showToast((e as Error).message, 'error');
		} finally {
			giftBusy = false;
		}
	}

	const winRate = $derived(stats && stats.total > 0 ? Math.round((stats.wins / stats.total) * 100) : 0);

	// Avatar
	let spec = $state<AvatarSpec>({ eyes: 0, nose: 0, mouth: 0, hat: 0 });

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
			const ar = await api<{ accessories: Accessory[] }>('/api/shop/accessories');
			accessories = ar.accessories;
			const hr = await api<{ houses: House[]; mine: string }>('/api/houses');
			houseList = hr.houses;
			myHouse = hr.mine;
			giftUsers = await api<GiftUser[]>('/api/users');
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
				<Avatar avatar={serializeAvatar(spec)} accessories={equippedAccessories} fontSize={18} />
				{#if equippedCompanion}
					<div class="orbit-shape" class:ellipse={orbit.path === 'ellipse'}>
						<div class="orbit" style:--orbit-dur={orbit.dur} style:animation-direction={orbit.dir}>
							<div
								class="orbit-pos"
								class:swoop={orbit.path === 'swoop'}
								style:--orbit-radius={orbit.radius}
							>
								<!-- annulla la rotazione dell'orbita: senza questo lo schiacciamento
								     dell'ellisse arriverebbe all'icona come deformazione (shear) -->
								<div class="orbit-fix" style:--orbit-dur={orbit.dur}>
									<div
										class="orbit-bob"
										class:zigzag={orbit.path === 'zigzag'}
										style:--bob-dur={orbit.bob}
									>
										<Icon name={equippedCompanion} size={28} title="Companion" tint={equippedTint} />
									</div>
								</div>
							</div>
						</div>
					</div>
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
		<h2><Icon name={equippedCompanion || 'leone'} size={18} title="Companion" tint={equippedTint} /> Companion</h2>
		{#if ownedCompanions.length === 0}
			<p class="hint">Non possiedi companion. Compratene uno nello <a href="/shop">shop</a>!</p>
		{:else}
			<div class="companion-grid">
				{#each ownedCompanions as c (c.id)}
					<button class="comp" class:on={c.equipped} onclick={() => equipCompanion(c)} title={c.name}>
						<Icon name={spriteOf(c)} size={40} title={c.name} tint={c.tint ?? ''} />
						<span class="comp-name">{c.name}</span>
						{#if c.equipped}<span class="comp-tag">ON</span>{/if}
					</button>
				{/each}
			</div>
			<p class="hint">Clicca per equipaggiare/togliere. Appare nell'header, attorno all'avatar e nelle stanze.</p>
			{#if shapeShifters.length}
				<div class="morph-row">
					{#each shapeShifters as c (c.id)}
						{@const transformed = (c.form ?? 'base') !== 'base'}
						<button
							class="morph"
							class:on={transformed}
							disabled={morphing[c.id]}
							onclick={() => toggleForm(c)}
						>
							<span class="morph-icon">
								<Icon name={spriteOf(c)} size={26} title={c.name} tint={c.tint ?? ''} />
								{#if morphing[c.id]}
									<span class="smoke" aria-hidden="true">
										{#each [0, 1, 2, 3, 4, 5, 6] as p (p)}<span class="puff p{p}"></span>{/each}
									</span>
								{/if}
							</span>
							{transformed ? `Torna ${c.name.toLowerCase()}` : `Trasforma in ${c.forms?.[0]}`}
						</button>
					{/each}
				</div>
				<p class="hint">
					La forma resta anche fuori da qui: header, stanze e classifica mostrano quella scelta.
				</p>
			{/if}
		{/if}
	</section>

	<section class="panel">
		<h2><Icon name="acc_corona" size={18} title="Accessori" /> Accessori</h2>
		{#if ownedAccessories.length === 0}
			<p class="hint">Non possiedi accessori. Compratene nello <a href="/shop">shop</a>! Si indossano sul volto: uno per slot (testa/occhi/bocca), combinabili.</p>
		{:else}
			<div class="companion-grid">
				{#each ownedAccessories as a (a.id)}
					<button class="comp" class:on={a.equipped} onclick={() => equipAccessory(a)} title={a.name}>
						<Icon name={a.id} size={40} title={a.name} />
						<span class="comp-name">{a.name}</span>
						{#if a.equipped}<span class="comp-tag">ON</span>{/if}
					</button>
				{/each}
			</div>
			<p class="hint">Clicca per indossare/togliere. Uno per slot: puoi combinare testa + occhi + bocca.</p>
		{/if}
	</section>

	<section class="panel">
		<h2><Icon name="party" size={18} title="Regala" /> Regala a un collega</h2>
		<div class="gift-form">
			<label>Destinatario
				<select bind:value={giftTo}>
					<option value="" disabled>Scegli un collega…</option>
					{#each giftUsers as u (u.username)}
						<option value={u.username}>{u.displayName} (@{u.username})</option>
					{/each}
				</select>
			</label>
			{#if giftTo && giftOptions}
				<p class="gift-balance"><Icon name="coin" size={14} /> Saldo: <strong>{giftOptions.balance}</strong> Token</p>
				<label>Cosa regalare
					<select bind:value={giftKind}>
						<option value="tokens">Token</option>
						<option value="power">Potere (consumabile)</option>
						<option value="companion">Companion</option>
						<option value="accessory">Accessorio</option>
					</select>
				</label>
				{#if giftKind === 'tokens'}
					<label>Quantità Token
						<input type="number" min="1" bind:value={giftAmount} />
					</label>
				{:else if giftKind === 'power'}
					<label>Potere
						<select bind:value={giftPowerId}>
							{#each giftOptions.powers as p (p.id)}
								<option value={p.id}>{p.label} — {p.cost} Token</option>
							{/each}
						</select>
					</label>
				{:else if giftKind === 'companion'}
					<label>Companion
						<select bind:value={giftCompanionId}>
							{#each giftOptions.companions as c (c.id)}
								<option value={c.id} disabled={c.ownedByRecipient}>
									{c.name} — {c.cost} Token{c.ownedByRecipient ? ' (già posseduto)' : ''}
								</option>
							{/each}
						</select>
					</label>
				{:else}
					<label>Accessorio
						<select bind:value={giftAccessoryId}>
							{#each giftOptions.accessories as a (a.id)}
								<option value={a.id} disabled={a.ownedByRecipient}>
									{a.name} — {a.cost} Token{a.ownedByRecipient ? ' (già posseduto)' : ''}
								</option>
							{/each}
						</select>
					</label>
				{/if}
				{#if giftBlocked}
					<p class="err"><Icon name="warning" size={14} /> Il destinatario possiede già questo oggetto.</p>
				{/if}
				<button onclick={sendGift} disabled={giftBusy || giftBlocked}>
					{giftBusy ? '…' : 'Invia regalo'}
				</button>
			{/if}
		</div>
		<p class="hint">Paghi tu con i tuoi Token: i Token si trasferiscono dal tuo saldo, i cosmetici/poteri si comprano in regalo. Il collega li vede alla prossima apertura.</p>
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
	/* Contenitore della traiettoria: schiaccia l'orbita quando il percorso è ellittico */
	.orbit-shape {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 0;
		height: 0;
		pointer-events: none;
	}
	.orbit-shape.ellipse {
		transform: scaleY(0.6);
	}
	/* L'icona non deve risultare schiacciata: si annulla prima la rotazione (.orbit-fix, stessa
	   durata in verso opposto) e poi la scala del contenitore. Il pulse qui non serve: nella
	   farfalla il movimento è già dato dalle ali. */
	.orbit-shape.ellipse .orbit-fix {
		animation: orbit var(--orbit-dur, 7s) linear infinite reverse;
	}
	.orbit-shape.ellipse .orbit-bob {
		animation: none;
		transform: scaleY(1.667);
	}
	.orbit {
		position: absolute;
		left: 0;
		top: 0;
		width: 0;
		height: 0;
		pointer-events: none;
		/* durata e verso variano per companion (custom props / animation-direction inline) */
		animation: orbit var(--orbit-dur, 7s) linear infinite;
	}
	.orbit-pos {
		position: absolute;
		/* il raggio varia per companion */
		transform: translate(-50%, -50%) translateY(var(--orbit-radius, -58px));
	}
	/* Picchiata: il raggio si accorcia e si allunga lungo il giro (pipistrello, gondola) */
	.orbit-pos.swoop {
		animation: orbit-swoop 3s ease-in-out infinite;
	}
	/* Pulse proprio del companion, con ritmo variabile per companion */
	.orbit-bob {
		animation: companion-bob var(--bob-dur, 2.6s) ease-in-out infinite;
	}
	/* Volo a scatti da insetto: sali-scendi rapido perpendicolare all'orbita */
	.orbit-bob.zigzag {
		animation: orbit-zigzag var(--bob-dur, 0.3s) steps(2, end) infinite;
	}
	@keyframes orbit {
		to {
			transform: rotate(360deg);
		}
	}
	@keyframes orbit-zigzag {
		0% {
			transform: translateY(-5px);
		}
		50% {
			transform: translateY(5px);
		}
		100% {
			transform: translateY(-5px);
		}
	}
	@keyframes orbit-swoop {
		0%,
		100% {
			transform: translate(-50%, -50%) translateY(var(--orbit-radius, -58px));
		}
		50% {
			transform: translate(-50%, -50%) translateY(calc(var(--orbit-radius, -58px) * 0.45));
		}
	}
	@keyframes companion-bob {
		0%,
		100% {
			transform: scale(1);
		}
		50% {
			transform: scale(1.18);
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

	/* Trasformazione companion (pipistrello → vampiro) con nuvola di fumo */
	.morph-row {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		margin-top: 0.6rem;
	}
	.morph {
		display: inline-flex;
		align-items: center;
		gap: 0.45rem;
		padding: 0.35rem 0.7rem;
		background: #0f172a;
		border: 2px solid #334155;
		border-radius: 999px;
		color: var(--muted);
		font-family: var(--font-ui, sans-serif);
		font-size: 0.72rem;
		letter-spacing: 0.05em;
		text-transform: uppercase;
		cursor: pointer;
	}
	.morph:hover:not(:disabled) {
		color: var(--text);
		border-color: var(--accent);
	}
	.morph.on {
		border-color: var(--danger);
		color: var(--text);
		box-shadow: 0 0 12px color-mix(in srgb, var(--danger) 40%, transparent);
	}
	.morph:disabled {
		cursor: progress;
	}
	.morph-icon {
		position: relative;
		display: inline-flex;
		width: 26px;
		height: 26px;
		align-items: center;
		justify-content: center;
	}

	.smoke {
		position: absolute;
		inset: -6px;
		pointer-events: none;
	}
	.puff {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 11px;
		height: 11px;
		border-radius: 3px;
		background: #e2e8f0;
		box-shadow: 0 0 6px rgba(226, 232, 240, 0.55);
		opacity: 0;
		animation: puff 0.9s ease-out forwards;
	}
	/* direzioni diverse per ogni sbuffo (pixel look: quadratini, non cerchi) */
	.puff.p0 { --dx: -18px; --dy: -13px; animation-delay: 0s; }
	.puff.p1 { --dx: 16px;  --dy: -15px; animation-delay: 0.05s; }
	.puff.p2 { --dx: -16px; --dy: 10px;  animation-delay: 0.1s; }
	.puff.p3 { --dx: 18px;  --dy: 8px;   animation-delay: 0.15s; }
	.puff.p4 { --dx: 0px;   --dy: -20px; animation-delay: 0.2s; }
	.puff.p5 { --dx: -8px;  --dy: 16px;  animation-delay: 0.25s; }
	.puff.p6 { --dx: 9px;   --dy: 17px;  animation-delay: 0.3s; }

	@keyframes puff {
		0% {
			opacity: 1;
			transform: translate(-50%, -50%) scale(0.35);
		}
		55% {
			opacity: 0.7;
		}
		100% {
			opacity: 0;
			transform: translate(calc(-50% + var(--dx)), calc(-50% + var(--dy))) scale(2.2);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.puff {
			animation: none;
			opacity: 0;
		}
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

	/* Form regali user→user */
	.gift-form {
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
		max-width: 24rem;
	}
	.gift-form label {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
		font-size: 0.85rem;
		color: var(--muted);
	}
	.gift-form select,
	.gift-form input {
		padding: 0.5rem 0.6rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font-size: 0.95rem;
	}
	.gift-form button {
		align-self: flex-start;
	}
	.gift-balance {
		margin: 0;
		font-size: 0.85rem;
		color: var(--amber);
		display: flex;
		align-items: center;
		gap: 0.3rem;
	}

	@media (prefers-reduced-motion: reduce) {
		.orbit,
		.orbit-bob,
		.orbit-bob.zigzag,
		.orbit-pos.swoop,
		.orbit-shape.ellipse .orbit-fix {
			animation: none;
		}
	}
</style>
