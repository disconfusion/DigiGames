<script lang="ts">
	import { onMount } from 'svelte';
	import { toRects, type Palette } from './pixel';

	let {
		frames,
		palette,
		grid = 16,
		size = 24,
		fps = 4,
		motion = '',
		title = ''
	}: {
		/** Uno o più fotogrammi (mappe pixel). Se >1 vengono ciclati a `fps`. */
		frames: string[][];
		palette: Palette;
		grid?: number;
		size?: number;
		fps?: number;
		/** Classe di movimento dell'intera icona: bob | swing | pulse | shake | float | spin. */
		motion?: string;
		title?: string;
	} = $props();

	let frame = $state(0);

	onMount(() => {
		if (frames.length < 2) return;
		// Niente animazione a fotogrammi se l'utente preferisce meno movimento.
		if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return;
		const id = setInterval(() => {
			frame = (frame + 1) % frames.length;
		}, 1000 / fps);
		return () => clearInterval(id);
	});

	const cells = $derived(toRects(frames[frame] ?? frames[0], palette));
</script>

<svg
	class="pix {motion}"
	width={size}
	height={size}
	viewBox="0 0 {grid} {grid}"
	shape-rendering="crispEdges"
	role="img"
	aria-label={title}
>
	{#each cells as c (c.x + ':' + c.y)}
		<rect x={c.x} y={c.y} width="1.02" height="1.02" style="fill:{c.fill}" />
	{/each}
</svg>

<style>
	.pix {
		display: inline-block;
		vertical-align: middle;
		overflow: visible;
	}
	/* Origine al centro del disegno per le trasformazioni */
	.bob,
	.pulse,
	.shake,
	.float,
	.spin {
		transform-box: fill-box;
		transform-origin: center;
	}
	.swing {
		transform-box: fill-box;
		transform-origin: 50% 8%;
	}

	.bob {
		animation: bob 1.8s ease-in-out infinite;
	}
	.swing {
		animation: swing 2.6s ease-in-out infinite;
	}
	.pulse {
		animation: pulse 1.8s ease-in-out infinite;
	}
	.shake {
		animation: shake 0.6s ease-in-out infinite;
	}
	.float {
		animation: float 3.2s ease-in-out infinite;
	}
	.spin {
		animation: spin 6s linear infinite;
	}

	@keyframes bob {
		0%, 100% { transform: translateY(0); }
		50% { transform: translateY(-1.4px); }
	}
	@keyframes swing {
		0%, 100% { transform: rotate(-6deg); }
		50% { transform: rotate(6deg); }
	}
	@keyframes pulse {
		0%, 100% { transform: scale(1); }
		50% { transform: scale(1.09); }
	}
	@keyframes shake {
		0%, 100% { transform: translateX(0); }
		25% { transform: translateX(-1.2px); }
		75% { transform: translateX(1.2px); }
	}
	@keyframes float {
		0%, 100% { transform: translateY(0) rotate(-2deg); }
		50% { transform: translateY(-1.2px) rotate(2deg); }
	}
	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	@media (prefers-reduced-motion: reduce) {
		.pix {
			animation: none !important;
		}
	}
</style>
