<script lang="ts">
	import { onMount } from 'svelte';
	import type { BoardProps } from './board';
	import GameResultOverlay from './GameResultOverlay.svelte';
	import Icon from '$lib/icons/Icon.svelte';
	import { TANK_SHAPES, TANK_COLORS, EAGLE, POWERUP_SPRITES, POWERUP_LABELS } from './battlecity';

	let { send, event, me, names = {} }: BoardProps = $props();

	/* ─────────────────────────────────────────────────────────────────────────
	   Battle City. Il server simula a 60 Hz ed è l'unica autorità: da qui parte
	   solo la direzione premuta ("input") e lo sparo ("shoot"). Il canvas disegna
	   l'ultimo snapshot con un filo di interpolazione per assorbire il jitter.
	   ───────────────────────────────────────────────────────────────────────── */

	type Dir = 'UP' | 'RIGHT' | 'DOWN' | 'LEFT';
	type TankView = {
		id: number;
		x: number;
		y: number;
		dir: Dir;
		player: boolean;
		owner?: string;
		power?: number;
		kind?: string;
		hp?: number;
		shield?: boolean;
	};
	type BulletView = { id: number; x: number; y: number; dir: Dir; player: boolean };
	type PowerView = { kind: string; x: number; y: number };
	type Stats = Record<string, { lives: number; score: number; power: number; alive: boolean }>;
	type Dims = { field: number; cell: number; tile: number; tank: number; bullet: number };

	const DEFAULT_DIMS: Dims = { field: 208, cell: 8, tile: 16, tank: 16, bullet: 4 };

	let dims = $state<Dims>(DEFAULT_DIMS);
	let grid = $state<string[]>([]);
	let mode = $state<'COOP' | 'DUEL'>('COOP');
	let level = $state(1);
	let started = $state(false);
	let stats = $state<Stats>({});
	let enemiesLeft = $state(0);
	let frozen = $state(false);
	let shovel = $state(false);
	let serverTps = $state(0);
	let clientFps = $state(0);
	let over = $state<{ status: string; winner: string | null; level: number; stats: Stats } | null>(null);
	let lastPowerUp = $state('');

	// ── Construction Mode: la mappa la disegnano i giocatori, ognuno la propria metà ──
	type BuildView = {
		grid: string[];
		middle: number;
		reservedTop: number;
		reservedBottom: number;
		zones: Record<string, number>;
		ready: Record<string, boolean>;
		everyoneReady: boolean;
	};
	let build = $state<BuildView | null>(null);
	let brush = $state<'.' | 'B' | 'S' | 'W' | 'T' | 'I'>('B');
	let mapName = $state('');
	let painting = false;

	const TILE_KINDS: { key: '.' | 'B' | 'S' | 'W' | 'T' | 'I'; label: string; color: string }[] = [
		{ key: '.', label: 'Vuoto', color: '#0b0b16' },
		{ key: 'B', label: 'Mattoni', color: '#c56a45' },
		{ key: 'S', label: 'Acciaio', color: '#b9c2d0' },
		{ key: 'W', label: 'Acqua', color: '#1b4fd8' },
		{ key: 'T', label: 'Cespugli', color: '#2fa84f' },
		{ key: 'I', label: 'Ghiaccio', color: '#cfe8ff' }
	];
	const TILE_COLOR: Record<string, string> = Object.fromEntries(
		TILE_KINDS.map((t) => [t.key, t.color])
	);

	/** La cella è nella mia metà (e non è riservata)? Solo lì posso dipingere. */
	function mine(row: number, col: number): boolean {
		if (!build) return false;
		if (row < build.reservedTop || row >= build.grid.length - build.reservedBottom) return false;
		if (col === build.middle) return false;
		const zone = build.zones[me.username];
		if (zone === undefined || zone < 0) return true; // da soli si disegna tutto
		return zone === 0 ? col < build.middle : col > build.middle;
	}

	function paint(row: number, col: number) {
		if (!mine(row, col)) return;
		if (build && build.grid[row][col] === brush) return; // già così: niente traffico inutile
		send({ type: 'build:paint', row, col, tile: brush });
	}

	const openBuilder = () => send({ type: 'build:open' });
	const buildTool = (tool: 'random' | 'clear' | 'mirror') => send({ type: 'build:tool', tool });
	const toggleReady = () =>
		send({ type: 'build:ready', ready: !(build?.ready[me.username] ?? false) });

	function saveMap() {
		const name = mapName.trim();
		if (!name) return;
		send({ type: 'build:save', name });
		mapName = '';
	}

	// Valori disegnati: fuori da $state, li legge solo il loop di disegno
	let tanks: TankView[] = [];
	let bullets: BulletView[] = [];
	let powerups: PowerView[] = [];
	/** Posizioni interpolate per id, così i tank non "saltano" fra due snapshot. */
	const drawn = new Map<number, { x: number; y: number }>();
	/** Esplosioni in corso: {x, y, nato, grande} */
	let booms: { x: number; y: number; at: number; big: boolean }[] = [];

	let canvas: HTMLCanvasElement | undefined = $state();
	let reduceMotion = false;

	const myStats = $derived(stats[me.username]);
	const others = $derived(Object.keys(stats).filter((u) => u !== me.username));
	const oppName = $derived(others.length ? (names[others[0]] ?? others[0]) : null);

	// ── Ricezione degli snapshot ─────────────────────────────────────────────
	function applyDynamic(e: Record<string, unknown>) {
		if (Array.isArray(e.tanks)) tanks = e.tanks as TankView[];
		if (Array.isArray(e.bullets)) bullets = e.bullets as BulletView[];
		if (Array.isArray(e.powerups)) powerups = e.powerups as PowerView[];
		if (e.stats) stats = e.stats as Stats;
		if (typeof e.enemiesLeft === 'number') enemiesLeft = e.enemiesLeft;
		if (typeof e.frozen === 'boolean') frozen = e.frozen;
		if (typeof e.shovel === 'boolean') shovel = e.shovel;
		if (typeof e.tps === 'number') serverTps = e.tps;
		// Eventi effimeri: esplosioni e bonus raccolti
		if (Array.isArray(e.events)) {
			for (const ev of e.events as { type: string; x: number; y: number; by?: string }[]) {
				if (ev.type === 'explosion') booms.push({ x: ev.x, y: ev.y, at: performance.now(), big: true });
				else if (ev.type.startsWith('bullet:') || ev.type === 'hit:armor' || ev.type === 'hit:shield')
					booms.push({ x: ev.x, y: ev.y, at: performance.now(), big: false });
				else if (ev.type === 'powerup:take') lastPowerUp = POWERUP_LABELS[ev.by ?? ''] ?? '';
				else if (ev.type === 'base:destroyed')
					booms.push({ x: ev.x, y: ev.y, at: performance.now(), big: true });
			}
			if (booms.length > 60) booms = booms.slice(-60);
		}
		// Tank scomparsi: libera le posizioni interpolate
		if (drawn.size > 40) {
			const live = new Set(tanks.map((t) => t.id));
			for (const id of [...drawn.keys()]) if (!live.has(id)) drawn.delete(id);
		}
	}

	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			const d = e.dims as Dims | undefined;
			if (d) dims = d;
			if (Array.isArray(e.grid)) grid = e.grid as string[];
			if (typeof e.mode === 'string') mode = e.mode as 'COOP' | 'DUEL';
			if (typeof e.level === 'number') level = e.level;
			applyDynamic(e as Record<string, unknown>);
			if (e.status === 'PLAYING') {
				over = null;
				started = true;
				build = null; // la costruzione è finita: si gioca
			}
		} else if (e.type === 'game:build') {
			build = e as unknown as BuildView;
		} else if (e.type === 'game:tick') {
			applyDynamic(e as Record<string, unknown>);
		} else if (e.type === 'game:over') {
			over = {
				status: String(e.status),
				winner: (e.winner as string | null) ?? null,
				level: Number(e.level ?? level),
				stats: (e.stats as Stats) ?? stats
			};
		}
	});

	// ── Input ────────────────────────────────────────────────────────────────
	const KEY_DIR: Record<string, Dir> = {
		arrowup: 'UP',
		w: 'UP',
		arrowdown: 'DOWN',
		s: 'DOWN',
		arrowleft: 'LEFT',
		a: 'LEFT',
		arrowright: 'RIGHT',
		d: 'RIGHT'
	};
	/** Tasti direzione premuti, in ordine: l'ultimo premuto comanda. */
	let held: Dir[] = [];
	let sentDir: Dir | null = null;
	let sentMoving = false;

	/** Invia solo al cambio di stato: il server ricorda direzione e movimento. */
	function pushInput() {
		const dir = held.length ? held[held.length - 1] : null;
		const moving = dir !== null;
		if (dir === sentDir && moving === sentMoving) return;
		sentDir = dir;
		sentMoving = moving;
		send({ type: 'input', dir: dir ?? (sentDir ?? 'UP'), moving });
	}

	function pressDir(d: Dir) {
		if (!held.includes(d)) held = [...held, d];
		pushInput();
	}

	function releaseDir(d: Dir) {
		held = held.filter((x) => x !== d);
		pushInput();
	}

	const fire = () => send({ type: 'shoot' });

	function onKeyDown(ev: KeyboardEvent) {
		const t = ev.target as HTMLElement | null;
		if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA')) return; // non rubare la chat
		const k = ev.key.toLowerCase();
		if (KEY_DIR[k]) {
			ev.preventDefault();
			pressDir(KEY_DIR[k]);
		} else if (k === ' ' || k === 'enter') {
			ev.preventDefault();
			fire();
		}
	}

	function onKeyUp(ev: KeyboardEvent) {
		const k = ev.key.toLowerCase();
		if (KEY_DIR[k]) releaseDir(KEY_DIR[k]);
	}

	const startGame = () => send({ type: 'game:start' });

	// ── Disegno ──────────────────────────────────────────────────────────────
	onMount(() => {
		reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
		window.addEventListener('keydown', onKeyDown);
		window.addEventListener('keyup', onKeyUp);

		let raf = 0;
		let last = performance.now();
		let frames = 0;
		let fpsWindow = last;

		const loop = (now: number) => {
			const dt = Math.min((now - last) / 1000, 0.1);
			last = now;
			frames++;
			if (now - fpsWindow >= 500) {
				clientFps = Math.round((frames * 1000) / (now - fpsWindow));
				frames = 0;
				fpsWindow = now;
			}
			draw(now, dt);
			raf = requestAnimationFrame(loop);
		};
		raf = requestAnimationFrame(loop);

		return () => {
			cancelAnimationFrame(raf);
			window.removeEventListener('keydown', onKeyDown);
			window.removeEventListener('keyup', onKeyUp);
		};
	});

	function fit(c: HTMLCanvasElement) {
		const dpr = Math.min(window.devicePixelRatio || 1, 2);
		const w = Math.round(c.clientWidth * dpr);
		if (c.width !== w || c.height !== w) {
			c.width = w;
			c.height = w; // campo quadrato
		}
	}

	/** Disegna una mappa di pixel (16×16) in un rettangolo del canvas. */
	function drawSprite(
		ctx: CanvasRenderingContext2D,
		map: string[],
		palette: Record<string, string>,
		x: number,
		y: number,
		size: number
	) {
		const n = map.length;
		const px = size / n;
		for (let r = 0; r < n; r++) {
			const row = map[r];
			for (let c = 0; c < row.length; c++) {
				const ch = row[c];
				if (ch === '.' || ch === ' ') continue;
				const fill = palette[ch];
				if (!fill) continue;
				ctx.fillStyle = fill;
				ctx.fillRect(x + c * px, y + r * px, px + 0.5, px + 0.5);
			}
		}
	}

	function draw(now: number, dt: number) {
		const c = canvas;
		if (!c) return;
		fit(c);
		const ctx = c.getContext('2d');
		if (!ctx) return;
		const s = c.width / dims.field; // scala unità campo → pixel canvas
		const u = (v: number) => v * s;

		ctx.setTransform(1, 0, 0, 1, 0, 0);
		ctx.fillStyle = '#05030d';
		ctx.fillRect(0, 0, c.width, c.height);

		// Muri e terreno
		const cell = dims.cell;
		for (let r = 0; r < grid.length; r++) {
			const row = grid[r];
			for (let col = 0; col < row.length; col++) {
				const ch = row[col];
				if (ch === '.') continue;
				const x = u(col * cell);
				const y = u(r * cell);
				const w = u(cell) + 0.5;
				if (ch === 'B') {
					// mattoni: due file di laterizi per cella
					ctx.fillStyle = '#8b3a2a';
					ctx.fillRect(x, y, w, w);
					ctx.fillStyle = '#c56a45';
					ctx.fillRect(x, y, w, w / 2 - 0.5);
					ctx.fillStyle = '#5a2418';
					ctx.fillRect(x + w / 2 - 0.5, y, 1, w);
				} else if (ch === 'S') {
					ctx.fillStyle = '#8b93a5';
					ctx.fillRect(x, y, w, w);
					ctx.fillStyle = '#d9e2f0';
					ctx.fillRect(x, y, w / 2, w / 2);
				} else if (ch === 'W') {
					const t = reduceMotion ? 0 : Math.sin(now / 320 + r + col) * 0.5 + 0.5;
					ctx.fillStyle = t > 0.5 ? '#1b4fd8' : '#1233a0';
					ctx.fillRect(x, y, w, w);
				} else if (ch === 'I') {
					ctx.fillStyle = '#cfe8ff';
					ctx.fillRect(x, y, w, w);
					ctx.fillStyle = '#eaf6ff';
					ctx.fillRect(x, y, w / 2, w / 2);
				} else if (ch === 'E') {
					// l'aquila copre 2×2 celle: la disegno una volta sola, sull'angolo
					const isTopLeft = grid[r - 1]?.[col] !== 'E' && row[col - 1] !== 'E';
					if (isTopLeft) drawSprite(ctx, EAGLE.map, EAGLE.palette, x, y, u(cell * 2));
				}
			}
		}

		// Power-up (lampeggiano)
		for (const p of powerups) {
			if (!reduceMotion && Math.floor(now / 220) % 2 === 0) continue;
			const sprite = POWERUP_SPRITES[p.kind];
			if (sprite) drawSprite(ctx, sprite.map, sprite.palette, u(p.x), u(p.y), u(dims.tile));
		}

		// Tank, con interpolazione leggera verso l'ultima posizione nota
		const k = 1 - Math.exp(-dt * 26);
		for (const t of tanks) {
			const prev = drawn.get(t.id) ?? { x: t.x, y: t.y };
			const nx = prev.x + (t.x - prev.x) * k;
			const ny = prev.y + (t.y - prev.y) * k;
			drawn.set(t.id, { x: nx, y: ny });

			const colorKey = t.player
				? t.owner === me.username
					? 'P1'
					: 'P2'
				: (t.kind ?? 'BASIC');
			const col = TANK_COLORS[colorKey] ?? TANK_COLORS.BASIC;
			// I corazzati sbiadiscono man mano che incassano colpi
			const palette =
				!t.player && t.kind === 'ARMOR' && (t.hp ?? 4) < 4
					? { B: '#7a5aa8', C: col.C }
					: { B: col.B, C: col.C };
			drawSprite(ctx, TANK_SHAPES[t.dir] ?? TANK_SHAPES.UP, palette, u(nx), u(ny), u(dims.tank));

			if (t.shield) {
				ctx.save();
				ctx.strokeStyle = '#2ff3ff';
				ctx.globalAlpha = reduceMotion ? 0.8 : 0.5 + 0.5 * Math.sin(now / 90);
				ctx.lineWidth = Math.max(1, u(1.5));
				ctx.strokeRect(u(nx) - 1, u(ny) - 1, u(dims.tank) + 2, u(dims.tank) + 2);
				ctx.restore();
			}
			// Chi sono io: puntino sopra la torretta
			if (t.player && t.owner === me.username) {
				ctx.fillStyle = '#ffffff';
				ctx.fillRect(u(nx + dims.tank / 2 - 1), u(ny - 3), u(2), u(2));
			}
		}

		// Proiettili
		for (const b of bullets) {
			ctx.fillStyle = b.player ? '#ffffff' : '#ffd9d9';
			ctx.fillRect(u(b.x), u(b.y), u(dims.bullet), u(dims.bullet));
		}

		// Esplosioni
		booms = booms.filter((b) => now - b.at < (b.big ? 420 : 200));
		for (const b of booms) {
			const life = (now - b.at) / (b.big ? 420 : 200);
			const rad = u((b.big ? 14 : 6) * (0.4 + life));
			ctx.save();
			ctx.globalAlpha = 1 - life;
			ctx.fillStyle = life < 0.5 ? '#ffe89a' : '#ff5277';
			ctx.beginPath();
			ctx.arc(u(b.x) + u(2), u(b.y) + u(2), rad, 0, Math.PI * 2);
			ctx.fill();
			ctx.restore();
		}

		// Cespugli sopra tutto: nascondono i tank, come nell'originale
		for (let r = 0; r < grid.length; r++) {
			const row = grid[r];
			for (let col = 0; col < row.length; col++) {
				if (row[col] !== 'T') continue;
				const x = u(col * cell);
				const y = u(r * cell);
				const w = u(cell) + 0.5;
				ctx.fillStyle = '#1f7a3a';
				ctx.fillRect(x, y, w, w);
				ctx.fillStyle = '#2fa84f';
				ctx.fillRect(x, y, w / 2, w / 2);
				ctx.fillStyle = '#14532b';
				ctx.fillRect(x + w / 2, y + w / 2, w / 2, w / 2);
			}
		}

		// Nemici congelati: velo azzurro
		if (frozen) {
			ctx.fillStyle = 'rgba(47, 243, 255, 0.10)';
			ctx.fillRect(0, 0, c.width, c.height);
		}

		// CRT: scanline, banda di roll e vignettatura
		ctx.save();
		ctx.globalAlpha = 0.16;
		ctx.fillStyle = '#000';
		const step = Math.max(2, Math.round(3 * (c.height / 600)));
		for (let y = 0; y < c.height; y += step * 2) ctx.fillRect(0, y, c.width, step);
		ctx.restore();

		if (!reduceMotion) {
			const rollY = ((now / 26) % (c.height + 160)) - 80;
			const g = ctx.createLinearGradient(0, rollY - 60, 0, rollY + 60);
			g.addColorStop(0, 'rgba(255,255,255,0)');
			g.addColorStop(0.5, 'rgba(255,255,255,0.04)');
			g.addColorStop(1, 'rgba(255,255,255,0)');
			ctx.fillStyle = g;
			ctx.fillRect(0, rollY - 60, c.width, 120);
		}

		const vg = ctx.createRadialGradient(
			c.width / 2, c.height / 2, Math.min(c.width, c.height) * 0.34,
			c.width / 2, c.height / 2, Math.max(c.width, c.height) * 0.75
		);
		vg.addColorStop(0, 'rgba(0,0,0,0)');
		vg.addColorStop(1, 'rgba(0,0,0,0.5)');
		ctx.fillStyle = vg;
		ctx.fillRect(0, 0, c.width, c.height);
	}
</script>

<div class="bc">
	<div class="topbar">
		<div class="scoreline">
			<span class="chip lives" title="Vite">
				<Icon name="battlecity" size={14} />
				{myStats?.lives ?? 0}
			</span>
			<span class="chip" title="Punti">{myStats?.score ?? 0} pt</span>
			{#if mode === 'COOP'}
				<span class="chip" title="Nemici ancora da abbattere">
					<Icon name="target" size={14} /> {enemiesLeft}
				</span>
				<span class="chip" title="Livello">LIV {level}</span>
			{:else}
				<span class="chip duel" title="Colpi (primo a 3 vince)">
					DUELLO {myStats?.score ?? 0} : {stats[others[0]]?.score ?? 0}
					{#if oppName}<em>{oppName}</em>{/if}
				</span>
			{/if}
			{#if (myStats?.power ?? 1) > 1}
				<span class="chip star" title="Cannone potenziato">
					<Icon name="bolt" size={14} /> ×{myStats?.power}
				</span>
			{/if}
			{#if frozen}<span class="chip frozen">Nemici congelati</span>{/if}
			{#if shovel}<span class="chip shovel">Base in acciaio</span>{/if}
		</div>
		<div class="hud">
			<span class="chip"><em>FPS</em> {clientFps}</span>
			<span class="chip" title="Tick al secondo simulati dal server"><em>SRV</em> {serverTps}</span>
		</div>
	</div>

	<div class="stage">
		<canvas bind:this={canvas} aria-label="Campo di Battle City"></canvas>
		{#if !started && !build}
			<div class="pre">
				<p class="info">
					Muovi con le frecce o WASD, spara con <strong>spazio</strong>.
					{#if mode === 'DUEL'}Duello: primo a 3 colpi.{:else}Difendi l'aquila e abbatti i 20 tank.{/if}
				</p>
				<div class="pre-actions">
					<button class="btn" onclick={startGame}>Inizia partita</button>
					<button class="btn ghost" onclick={openBuilder}>Disegna la mappa</button>
				</div>
			</div>
		{/if}
	</div>

	{#if build}
		<!-- Construction Mode: griglia condivisa, ognuno dipinge la propria metà in tempo reale -->
		<section class="builder">
			<div class="builder-head">
				<strong>Costruzione mappa</strong>
				<span class="muted">
					{#if (build.zones[me.username] ?? -1) < 0}
						Disegni tutto il campo
					{:else if build.zones[me.username] === 0}
						La tua metà è quella <b>sinistra</b>
					{:else}
						La tua metà è quella <b>destra</b>
					{/if}
					· colonna centrale libera, righe di comparsa e base intoccabili
				</span>
			</div>

			<div class="palette" role="group" aria-label="Materiali">
				{#each TILE_KINDS as t (t.key)}
					<button
						class="swatch"
						class:on={brush === t.key}
						style:background={t.color}
						title={t.label}
						aria-label={t.label}
						onclick={() => (brush = t.key)}
					></button>
				{/each}
				<span class="brush-name">{TILE_KINDS.find((t) => t.key === brush)?.label}</span>
			</div>

			<div
				class="grid"
				role="group"
				aria-label="Griglia della mappa"
				style:--cols={build.grid.length}
				onpointerdown={() => (painting = true)}
				onpointerup={() => (painting = false)}
				onpointerleave={() => (painting = false)}
				onpointercancel={() => (painting = false)}
			>
				{#each build.grid as row, r (r)}
					{#each row.split('') as ch, c (c)}
						{@const editable = mine(r, c)}
						<button
							class="cell"
							class:editable
							class:middle={c === build.middle}
							style:background={TILE_COLOR[ch] ?? '#0b0b16'}
							aria-label="riga {r + 1} colonna {c + 1}"
							disabled={!editable}
							onpointerdown={() => paint(r, c)}
							onpointerenter={() => painting && paint(r, c)}
						></button>
					{/each}
				{/each}
			</div>

			<div class="builder-tools">
				<button class="btn ghost" onclick={() => buildTool('random')}>Bozza casuale</button>
				<button class="btn ghost" onclick={() => buildTool('mirror')}>Specchia</button>
				<button class="btn ghost" onclick={() => buildTool('clear')}>Svuota</button>
				<button class="btn" class:on={build.ready[me.username]} onclick={toggleReady}>
					{build.ready[me.username] ? 'Pronto ✓' : 'Sono pronto'}
				</button>
			</div>

			<div class="builder-status">
				{#each Object.keys(build.ready) as u (u)}
					<span class="chip" class:frozen={build.ready[u]}>
						{u === me.username ? 'Tu' : (names[u] ?? u)}: {build.ready[u] ? 'pronto' : 'sta disegnando'}
					</span>
				{/each}
			</div>

			<div class="builder-save">
				<input
					class="name-input"
					type="text"
					maxlength="60"
					placeholder="Nome della mappa (per rigiocarla)"
					bind:value={mapName}
				/>
				<button class="btn ghost" disabled={!mapName.trim()} onclick={saveMap}>Salva</button>
				<button class="btn" onclick={startGame}>
					{build.everyoneReady ? 'Giocate su questa mappa' : 'Gioca comunque'}
				</button>
			</div>
		</section>
	{/if}

	{#if lastPowerUp}
		<p class="pickup"><Icon name="party" size={14} /> {lastPowerUp}</p>
	{/if}

	<!-- Comandi per il tocco: croce direzionale + fuoco -->
	<div class="touch" aria-hidden="false">
		<div class="dpad">
			{#each [{ d: 'UP', label: '▲', cls: 'up' }, { d: 'LEFT', label: '◀', cls: 'left' }, { d: 'RIGHT', label: '▶', cls: 'right' }, { d: 'DOWN', label: '▼', cls: 'down' }] as b (b.d)}
				<button
					class="pad {b.cls}"
					aria-label="Muovi {b.d}"
					onpointerdown={(e) => { e.preventDefault(); pressDir(b.d as Dir); }}
					onpointerup={() => releaseDir(b.d as Dir)}
					onpointerleave={() => releaseDir(b.d as Dir)}
					onpointercancel={() => releaseDir(b.d as Dir)}
				>{b.label}</button>
			{/each}
		</div>
		<button class="fire" aria-label="Spara" onpointerdown={(e) => { e.preventDefault(); fire(); }}>
			FUOCO
		</button>
	</div>

	{#if over}
		<GameResultOverlay
			result={over.status === 'WON'
				? mode === 'DUEL'
					? over.winner === me.username
						? 'win'
						: 'lose'
					: 'win'
				: 'lose'}
			message={mode === 'DUEL'
				? over.winner === me.username
					? 'Duello vinto!'
					: 'Duello perso'
				: over.status === 'WON'
					? `Livello ${over.level} completato!`
					: 'Base perduta'}
			playAgainLabel={mode === 'COOP' && over.status === 'WON' ? 'Livello successivo' : 'Riprova'}
			onPlayAgain={startGame}
		/>
	{/if}

	<p class="hint">
		<Icon name="bolt" size={14} /> Il server simula a 60&nbsp;Hz: dal tuo browser parte solo la
		direzione e lo sparo.
	</p>
</div>

<style>
	.bc {
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
		width: 100%;
	}

	.topbar {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
	}
	.scoreline,
	.hud {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
		align-items: center;
	}
	.chip {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		padding: 0.22rem 0.5rem;
		border: 1px solid var(--line);
		border-radius: 6px;
		background: var(--inset);
		color: var(--text);
		font-family: var(--font-term, monospace);
		font-size: 0.9rem;
		line-height: 1.2;
	}
	.chip em {
		font-style: normal;
		color: var(--muted);
		font-size: 0.75rem;
		letter-spacing: 0.08em;
	}
	.chip.star {
		border-color: var(--amber);
		color: var(--amber);
	}
	.chip.frozen {
		border-color: var(--cyan);
		color: var(--cyan);
	}
	.chip.shovel {
		border-color: var(--muted);
	}
	.chip.duel {
		border-color: var(--accent);
	}

	.stage {
		position: relative;
		width: 100%;
		max-width: 560px;
		margin: 0 auto;
		aspect-ratio: 1 / 1;
		border: 2px solid var(--line);
		border-radius: 12px;
		overflow: hidden;
		background: #05030d;
		box-shadow: inset 0 0 60px rgba(0, 0, 0, 0.8);
	}
	canvas {
		display: block;
		width: 100%;
		height: 100%;
		image-rendering: pixelated;
		touch-action: none;
	}

	.pre {
		position: absolute;
		inset: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 0.8rem;
		padding: 1rem;
		text-align: center;
		background: rgba(5, 3, 13, 0.75);
	}
	.info {
		margin: 0;
		font-family: var(--font-term, monospace);
		color: var(--text);
	}
	.btn {
		padding: 0.6rem 1.2rem;
		border: 1px solid var(--accent);
		border-radius: 8px;
		background: var(--accent);
		color: #0b0b12;
		font-family: var(--font-ui, sans-serif);
		font-weight: 700;
		letter-spacing: 0.08em;
		text-transform: uppercase;
		cursor: pointer;
	}

	/* Construction Mode */
	.builder {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		padding: 0.6rem;
		border: 1px solid var(--line);
		border-radius: 10px;
		background: var(--inset);
	}
	.builder-head {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		align-items: baseline;
		font-family: var(--font-ui, sans-serif);
		font-size: 0.85rem;
	}
	.muted {
		color: var(--muted);
		font-size: 0.78rem;
	}
	.palette {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		flex-wrap: wrap;
	}
	.swatch {
		width: 26px;
		height: 26px;
		border-radius: 5px;
		border: 2px solid var(--line);
		cursor: pointer;
		padding: 0;
	}
	.swatch.on {
		border-color: var(--text);
		box-shadow: 0 0 8px currentColor;
	}
	.brush-name {
		font-family: var(--font-term, monospace);
		font-size: 0.85rem;
		color: var(--muted);
	}
	.grid {
		display: grid;
		grid-template-columns: repeat(var(--cols, 13), 1fr);
		gap: 1px;
		width: 100%;
		max-width: 420px;
		margin: 0 auto;
		aspect-ratio: 1 / 1;
		background: var(--line);
		touch-action: none;
	}
	.cell {
		border: none;
		padding: 0;
		cursor: not-allowed;
		opacity: 0.45;
	}
	.cell.editable {
		cursor: crosshair;
		opacity: 1;
	}
	.cell.middle {
		box-shadow: inset 0 0 0 1px var(--cyan);
	}
	.builder-tools,
	.builder-status,
	.builder-save {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		align-items: center;
	}
	.name-input {
		flex: 1 1 12rem;
		padding: 0.4rem 0.55rem;
		border-radius: 8px;
		border: 1px solid var(--line);
		background: #0f172a;
		color: var(--text);
		font-family: var(--font-term, monospace);
	}
	.btn.ghost {
		background: transparent;
		border-color: var(--line);
		color: var(--muted);
	}
	.btn.ghost:hover:not(:disabled) {
		color: var(--text);
		border-color: var(--accent);
	}
	.btn.on {
		background: var(--green);
		border-color: var(--green);
	}
	.btn:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.pre-actions {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		justify-content: center;
	}

	.pickup {
		margin: 0;
		display: flex;
		align-items: center;
		gap: 0.4rem;
		font-family: var(--font-term, monospace);
		font-size: 0.9rem;
		color: var(--amber);
	}

	/* Comandi touch: nascosti dove c'è un puntatore fine (mouse) */
	.touch {
		display: none;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		user-select: none;
	}
	.dpad {
		display: grid;
		grid-template-columns: repeat(3, 46px);
		grid-template-rows: repeat(3, 46px);
		gap: 0.2rem;
	}
	.pad {
		border: 1px solid var(--line);
		border-radius: 8px;
		background: var(--inset);
		color: var(--text);
		font-size: 1.1rem;
		cursor: pointer;
	}
	.pad.up { grid-area: 1 / 2; }
	.pad.left { grid-area: 2 / 1; }
	.pad.right { grid-area: 2 / 3; }
	.pad.down { grid-area: 3 / 2; }
	.fire {
		width: 92px;
		height: 92px;
		border-radius: 50%;
		border: 2px solid var(--accent);
		background: color-mix(in srgb, var(--accent) 22%, transparent);
		color: var(--text);
		font-family: var(--font-ui, sans-serif);
		font-weight: 700;
		letter-spacing: 0.06em;
		cursor: pointer;
	}
	@media (pointer: coarse) {
		.touch {
			display: flex;
		}
	}

	.hint {
		margin: 0;
		display: flex;
		align-items: center;
		gap: 0.4rem;
		font-family: var(--font-term, monospace);
		font-size: 0.85rem;
		color: var(--muted);
	}

	@media (max-width: 640px) {
		.chip {
			font-size: 0.8rem;
		}
	}
</style>
