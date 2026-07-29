<script lang="ts">
	import { onMount } from 'svelte';
	import type { BoardProps } from './board';
	import GameResultOverlay from './GameResultOverlay.svelte';
	import Icon from '$lib/icons/Icon.svelte';

	let { send, event, me, names = {} }: BoardProps = $props();

	/* ─────────────────────────────────────────────────────────────────────────
	   Pong realtime. Il server simula a 60 Hz e trasmette uno snapshot per tick
	   (`game:tick`, solo numeri) più uno completo a ogni punto (`game:state`).
	   Qui non si simula nulla: si interpola verso l'ultimo snapshot ricevuto e si
	   invia solo la posizione desiderata della propria racchetta (`aim`).
	   ───────────────────────────────────────────────────────────────────────── */

	type Dims = { w: number; h: number; paddleW: number; paddleH: number; margin: number; ballR: number };
	const DEFAULT_DIMS: Dims = { w: 1000, h: 600, paddleW: 14, paddleH: 110, margin: 28, ballR: 9 };

	/** Colori interfaccia selezionabili (il fosforo del "monitor"). */
	const COLORS = [
		{ label: 'Cyan', hex: '#2ff3ff' },
		{ label: 'Magenta', hex: '#ff2e88' },
		{ label: 'Verde', hex: '#3dff9a' },
		{ label: 'Ambra', hex: '#ffcf3f' },
		{ label: 'Rosso', hex: '#ff5277' },
		{ label: 'Viola', hex: '#b06bff' }
	];
	const COLOR_KEY = 'pong:color';

	let dims = $state<Dims>(DEFAULT_DIMS);
	let seats = $state<Record<string, string>>({});
	let pointsToWin = $state(7);
	let started = $state(false);
	let score = $state<[number, number]>([0, 0]);
	let serveIn = $state(0);
	let rally = $state(0);
	let longestRally = $state(0);
	let ballSpeed = $state(0);
	let serverTps = $state(0);
	let clientFps = $state(0);
	let over = $state<{ winner: string | null; score: [number, number]; longest: number } | null>(null);
	// SPA (ssr=false): localStorage c'è già all'inizializzazione. Il typeof copre il bundle server.
	let color = $state(
		typeof localStorage === 'undefined' ? COLORS[0].hex : (localStorage.getItem(COLOR_KEY) ?? COLORS[0].hex)
	);
	let showPalette = $state(false);

	const mySide = $derived(seats[me.username] ?? null);
	const oppName = $derived.by(() => {
		const other = Object.keys(seats).find((u) => u !== me.username);
		return other ? (names[other] ?? other) : null;
	});
	const myScore = $derived(mySide === 'R' ? score[1] : score[0]);
	const oppScore = $derived(mySide === 'R' ? score[0] : score[1]);

	// Valori autoritativi (ultimo snapshot) e valori disegnati (interpolati): fuori da $state,
	// cambiano 60 volte al secondo e li legge solo il loop di disegno.
	const target = { bx: 500, by: 300, pl: 300, pr: 300 };
	const drawn = { bx: 500, by: 300, pl: 300, pr: 300 };
	/** Scia al fosforo della palla: coordinate campo, dalla più recente. */
	let trail: { x: number; y: number }[] = [];

	let canvas: HTMLCanvasElement | undefined = $state();
	let reduceMotion = false;

	// ── Input ────────────────────────────────────────────────────────────────
	/** Posizione desiderata della racchetta, in unità di campo. */
	let desired = DEFAULT_DIMS.h / 2;
	let lastSent = -1;
	let lastSentAt = 0;
	const AIM_INTERVAL_MS = 33; // ~30 invii/s: bastano, il server insegue il target
	const KEY_SPEED = 900; // unità/s con i tasti
	const keys = new Set<string>();

	function applyDynamic(e: Record<string, unknown>) {
		const b = e.b as number[] | undefined;
		const p = e.p as number[] | undefined;
		const s = e.s as number[] | undefined;
		if (b) {
			target.bx = b[0];
			target.by = b[1];
		}
		if (p) {
			target.pl = p[0];
			target.pr = p[1];
		}
		if (s) score = [s[0], s[1]];
		if (typeof e.serveIn === 'number') serveIn = e.serveIn;
		if (typeof e.rally === 'number') rally = e.rally;
		if (typeof e.speed === 'number') ballSpeed = e.speed;
		if (typeof e.tps === 'number') serverTps = e.tps;
	}

	$effect(() => {
		const e = event;
		if (!e) return;
		if (e.type === 'game:state') {
			const d = e.dims as Dims | undefined;
			if (d) dims = d;
			seats = (e.seats as Record<string, string>) ?? {};
			pointsToWin = Number(e.pointsToWin ?? 7);
			longestRally = Number(e.longestRally ?? 0);
			applyDynamic(e as Record<string, unknown>);
			if (e.status === 'PLAYING') {
				over = null;
				started = true;
				// Nuova partita: riallinea subito il disegno, senza interpolare da posizioni vecchie,
				// e parti dalla posizione reale della TUA racchetta (niente scatto al primo input).
				drawn.bx = target.bx;
				drawn.by = target.by;
				desired = seats[me.username] === 'R' ? target.pr : target.pl;
				lastSent = -1;
				trail = [];
			}
		} else if (e.type === 'game:tick') {
			applyDynamic(e as Record<string, unknown>);
		} else if (e.type === 'game:over') {
			const s = (e.s as number[] | undefined) ?? score;
			over = {
				winner: (e.winner as string | null) ?? null,
				score: [s[0], s[1]],
				longest: Number(e.longestRally ?? longestRally)
			};
		}
	});

	/** Posizione desiderata da puntatore/tocco: converte i px dello schermo in unità di campo. */
	function aimFromPointer(clientY: number) {
		if (!canvas) return;
		const r = canvas.getBoundingClientRect();
		if (r.height === 0) return;
		desired = ((clientY - r.top) / r.height) * dims.h;
	}

	function onPointer(ev: PointerEvent) {
		// Col mouse basta il movimento; su touch solo mentre si trascina (pressure/buttons attivi).
		if (ev.pointerType !== 'mouse' && ev.type === 'pointermove' && ev.buttons === 0) return;
		aimFromPointer(ev.clientY);
	}

	function onKeyDown(ev: KeyboardEvent) {
		const t = ev.target as HTMLElement | null;
		if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA')) return; // non rubare la chat
		if (['ArrowUp', 'ArrowDown', 'w', 'W', 's', 'S'].includes(ev.key)) {
			keys.add(ev.key.toLowerCase());
			ev.preventDefault();
		}
	}

	function onKeyUp(ev: KeyboardEvent) {
		keys.delete(ev.key.toLowerCase());
	}

	const startGame = () => send({ type: 'game:start' });

	// ── Loop di disegno ──────────────────────────────────────────────────────
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

			// FPS client misurati su una finestra di mezzo secondo
			frames++;
			if (now - fpsWindow >= 500) {
				clientFps = Math.round((frames * 1000) / (now - fpsWindow));
				frames = 0;
				fpsWindow = now;
			}

			// Tasti tenuti premuti: muovono la posizione desiderata
			if (keys.size) {
				const up = keys.has('arrowup') || keys.has('w');
				const down = keys.has('arrowdown') || keys.has('s');
				if (up !== down) desired += (down ? 1 : -1) * KEY_SPEED * dt;
			}
			desired = Math.max(0, Math.min(dims.h, desired));

			// Invio dell'intenzione, throttled e solo se cambiata
			if (started && Math.abs(desired - lastSent) > 0.5 && now - lastSentAt >= AIM_INTERVAL_MS) {
				send({ type: 'aim', y: Math.round(desired * 10) / 10 });
				lastSent = desired;
				lastSentAt = now;
			}

			// Interpolazione esponenziale verso lo snapshot autoritativo (time-correct)
			const k = 1 - Math.exp(-dt * 22);
			drawn.bx += (target.bx - drawn.bx) * k;
			drawn.by += (target.by - drawn.by) * k;
			drawn.pl += (target.pl - drawn.pl) * k;
			drawn.pr += (target.pr - drawn.pr) * k;

			if (!reduceMotion) {
				trail.unshift({ x: drawn.bx, y: drawn.by });
				if (trail.length > 9) trail.pop();
			}

			draw(now);
			raf = requestAnimationFrame(loop);
		};
		raf = requestAnimationFrame(loop);

		return () => {
			cancelAnimationFrame(raf);
			window.removeEventListener('keydown', onKeyDown);
			window.removeEventListener('keyup', onKeyUp);
		};
	});

	$effect(() => {
		if (color) localStorage.setItem(COLOR_KEY, color);
	});

	/** Adegua il buffer del canvas alla dimensione CSS × densità dello schermo. */
	function fit(c: HTMLCanvasElement) {
		const dpr = Math.min(window.devicePixelRatio || 1, 2);
		const w = Math.round(c.clientWidth * dpr);
		const h = Math.round(c.clientHeight * dpr);
		if (c.width !== w || c.height !== h) {
			c.width = w;
			c.height = h;
		}
		return dpr;
	}

	function draw(now: number) {
		const c = canvas;
		if (!c) return;
		fit(c);
		const ctx = c.getContext('2d');
		if (!ctx) return;

		const sx = c.width / dims.w; // scala unità campo → pixel del buffer
		const sy = c.height / dims.h;
		const u = (v: number) => v * sx;

		ctx.setTransform(1, 0, 0, 1, 0, 0);
		ctx.clearRect(0, 0, c.width, c.height);

		// Fondo del tubo
		ctx.fillStyle = '#05030d';
		ctx.fillRect(0, 0, c.width, c.height);

		// Punteggio a filigrana, dietro tutto
		ctx.save();
		ctx.globalAlpha = 0.07;
		ctx.fillStyle = color;
		ctx.textAlign = 'center';
		ctx.textBaseline = 'middle';
		ctx.font = `700 ${Math.round(c.height * 0.34)}px 'Press Start 2P', monospace`;
		ctx.fillText(String(score[0]), c.width * 0.3, c.height * 0.3);
		ctx.fillText(String(score[1]), c.width * 0.7, c.height * 0.3);
		ctx.restore();

		// Rete centrale tratteggiata
		ctx.save();
		ctx.strokeStyle = color;
		ctx.globalAlpha = 0.35;
		ctx.lineWidth = Math.max(1, u(3));
		ctx.setLineDash([u(16), u(14)]);
		ctx.beginPath();
		ctx.moveTo(c.width / 2, 0);
		ctx.lineTo(c.width / 2, c.height);
		ctx.stroke();
		ctx.restore();

		// Cornice del campo
		ctx.save();
		ctx.strokeStyle = color;
		ctx.globalAlpha = 0.5;
		ctx.lineWidth = Math.max(1, u(2));
		ctx.strokeRect(u(2), u(2), c.width - u(4), c.height - u(4));
		ctx.restore();

		// Scia al fosforo
		if (trail.length > 1) {
			ctx.save();
			ctx.fillStyle = color;
			trail.forEach((p, i) => {
				if (i === 0) return; // il primo punto è la palla stessa
				const t = 1 - i / trail.length;
				ctx.globalAlpha = 0.14 * t * t;
				ctx.beginPath();
				ctx.arc(p.x * sx, p.y * sy, dims.ballR * sx * (0.35 + 0.45 * t), 0, Math.PI * 2);
				ctx.fill();
			});
			ctx.restore();
		}

		// Racchette + palla, con bagliore
		ctx.save();
		ctx.shadowColor = color;
		ctx.shadowBlur = u(18);
		ctx.fillStyle = color;
		const pw = u(dims.paddleW);
		const ph = dims.paddleH * sy;
		ctx.fillRect(u(dims.margin), drawn.pl * sy - ph / 2, pw, ph);
		ctx.fillRect(c.width - u(dims.margin) - pw, drawn.pr * sy - ph / 2, pw, ph);

		if (started) {
			// Aberrazione cromatica: due copie sfalsate della palla, come su un CRT scarso
			const r = dims.ballR * sx;
			const bx = drawn.bx * sx;
			const by = drawn.by * sy;
			if (!reduceMotion) {
				ctx.globalAlpha = 0.5;
				ctx.fillStyle = '#ff3355';
				ctx.beginPath();
				ctx.arc(bx - u(2), by, r, 0, Math.PI * 2);
				ctx.fill();
				ctx.fillStyle = '#33d0ff';
				ctx.beginPath();
				ctx.arc(bx + u(2), by, r, 0, Math.PI * 2);
				ctx.fill();
				ctx.globalAlpha = 1;
			}
			ctx.fillStyle = '#ffffff';
			ctx.beginPath();
			ctx.arc(bx, by, r, 0, Math.PI * 2);
			ctx.fill();
		}
		ctx.restore();

		// Countdown battuta
		if (serveIn > 0) {
			ctx.save();
			ctx.globalAlpha = 0.85;
			ctx.fillStyle = color;
			ctx.textAlign = 'center';
			ctx.textBaseline = 'middle';
			ctx.font = `700 ${Math.round(c.height * 0.09)}px 'Press Start 2P', monospace`;
			ctx.fillText(String(Math.max(1, Math.ceil(serveIn))), c.width / 2, c.height * 0.32);
			ctx.restore();
		}

		// Scanline + banda di roll: il tocco CRT, disegnate nel buffer così scalano col canvas
		ctx.save();
		ctx.globalAlpha = 0.16;
		ctx.fillStyle = '#000';
		const step = Math.max(2, Math.round(3 * (c.height / 600)));
		for (let y = 0; y < c.height; y += step * 2) ctx.fillRect(0, y, c.width, step);
		ctx.restore();

		if (!reduceMotion) {
			const rollY = ((now / 22) % (c.height + 160)) - 80;
			const g = ctx.createLinearGradient(0, rollY - 60, 0, rollY + 60);
			g.addColorStop(0, 'rgba(255,255,255,0)');
			g.addColorStop(0.5, 'rgba(255,255,255,0.045)');
			g.addColorStop(1, 'rgba(255,255,255,0)');
			ctx.fillStyle = g;
			ctx.fillRect(0, rollY - 60, c.width, 120);
		}

		// Vignettatura
		const vg = ctx.createRadialGradient(
			c.width / 2, c.height / 2, Math.min(c.width, c.height) * 0.3,
			c.width / 2, c.height / 2, Math.max(c.width, c.height) * 0.75
		);
		vg.addColorStop(0, 'rgba(0,0,0,0)');
		vg.addColorStop(1, 'rgba(0,0,0,0.55)');
		ctx.fillStyle = vg;
		ctx.fillRect(0, 0, c.width, c.height);
	}
</script>

<div class="pong">
	<div class="topbar">
		<div class="scoreline" aria-label="Punteggio">
			<span class="who">{mySide ? 'Tu' : '—'}</span>
			<strong class="pts" style:color>{myScore}</strong>
			<span class="dash">:</span>
			<strong class="pts">{oppScore}</strong>
			<span class="who">{oppName ?? 'avversario'}</span>
			<span class="to">primo a {pointsToWin}</span>
		</div>

		<div class="hud" aria-live="off">
			<span class="chip" title="Fotogrammi al secondo disegnati dal browser">
				<em>FPS</em> {clientFps}
			</span>
			<span class="chip" title="Tick al secondo simulati dal server (autorità)">
				<em>SRV</em> {serverTps}
			</span>
			<span class="chip" title="Velocità della palla in unità di campo al secondo">
				<em>VEL</em> {ballSpeed}
			</span>
			<span class="chip" title="Colpi nello scambio in corso">
				<em>RALLY</em> {rally}
			</span>
			<button
				class="chip picker"
				onclick={() => (showPalette = !showPalette)}
				aria-expanded={showPalette}
				title="Colore del monitor"
			>
				<span class="dot" style:background={color}></span> COLORE
			</button>
		</div>
	</div>

	{#if showPalette}
		<div class="palette">
			{#each COLORS as c (c.hex)}
				<button
					class="swatch"
					class:on={color === c.hex}
					style:background={c.hex}
					title={c.label}
					aria-label="Colore {c.label}"
					onclick={() => (color = c.hex)}
				></button>
			{/each}
			<label class="custom">
				<input type="color" bind:value={color} aria-label="Colore personalizzato" />
				<span>personalizzato</span>
			</label>
		</div>
	{/if}

	<div class="stage">
		<canvas
			bind:this={canvas}
			onpointermove={onPointer}
			onpointerdown={onPointer}
			aria-label="Campo da Pong"
		></canvas>
		{#if !started}
			<div class="pre">
				<p class="info">Pong 1v1 — muovi la racchetta col mouse, il dito o i tasti W/S · ↑/↓</p>
				<button class="btn" onclick={startGame}>Inizia partita</button>
			</div>
		{/if}
	</div>

	{#if over}
		<GameResultOverlay
			result={over.winner === me.username ? 'win' : 'lose'}
			message="{over.score[0]} - {over.score[1]} · scambio più lungo: {over.longest} colpi"
			onPlayAgain={startGame}
		/>
	{/if}

	<p class="hint">
		<Icon name="bolt" size={14} /> Il server simula a 60&nbsp;Hz ed è l'unica autorità: dal tuo
		browser parte solo la posizione della racchetta.
	</p>
</div>

<style>
	.pong {
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

	.scoreline {
		display: flex;
		align-items: baseline;
		gap: 0.45rem;
		font-family: var(--font-ui, sans-serif);
		font-size: 0.85rem;
		color: var(--muted);
		letter-spacing: 0.06em;
		text-transform: uppercase;
	}
	.pts {
		font-family: var(--font-display, monospace);
		font-size: 1.5rem;
		color: var(--text);
	}
	.dash {
		color: var(--muted);
	}
	.to {
		padding-left: 0.4rem;
		font-size: 0.7rem;
		opacity: 0.8;
	}

	.hud {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
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
	.picker {
		cursor: pointer;
	}
	.dot {
		width: 10px;
		height: 10px;
		border-radius: 50%;
		box-shadow: 0 0 8px currentColor;
	}

	.palette {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		flex-wrap: wrap;
		padding: 0.4rem 0.5rem;
		border: 1px solid var(--line);
		border-radius: 8px;
		background: var(--inset);
	}
	.swatch {
		width: 26px;
		height: 26px;
		border-radius: 6px;
		border: 2px solid transparent;
		cursor: pointer;
	}
	.swatch.on {
		border-color: var(--text);
		box-shadow: 0 0 10px currentColor;
	}
	.custom {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		font-family: var(--font-term, monospace);
		font-size: 0.85rem;
		color: var(--muted);
	}
	.custom input {
		width: 30px;
		height: 26px;
		padding: 0;
		border: 1px solid var(--line);
		border-radius: 6px;
		background: none;
		cursor: pointer;
	}

	.stage {
		position: relative;
		width: 100%;
		aspect-ratio: 5 / 3;
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
		touch-action: none; /* il trascinamento muove la racchetta, non la pagina */
		cursor: none;
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
		background: rgba(5, 3, 13, 0.72);
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
		.scoreline {
			font-size: 0.75rem;
		}
		.pts {
			font-size: 1.2rem;
		}
		.chip {
			font-size: 0.8rem;
		}
	}
</style>
