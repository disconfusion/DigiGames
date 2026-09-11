<script lang="ts">
	import {
		MAX_SCALE,
		MIN_SCALE,
		SCALE_STEP,
		resetScale,
		scalePercent,
		stepScale,
		uiScale
	} from '$lib/uiScale.svelte';

	const atMin = $derived(uiScale.value <= MIN_SCALE);
	const atMax = $derived(uiScale.value >= MAX_SCALE);
</script>

<div class="zoom" role="group" aria-label="Dimensione dell'interfaccia">
	<button
		class="zoom-btn"
		onclick={() => stepScale(-SCALE_STEP)}
		disabled={atMin}
		title="Rimpicciolisci l'interfaccia"
		aria-label="Rimpicciolisci l'interfaccia"
	>
		A<span class="sign">−</span>
	</button>
	<button
		class="zoom-value"
		onclick={resetScale}
		title="Dimensione dell'interfaccia: clicca per tornare al 100%"
		aria-label="Dimensione attuale {scalePercent()} per cento, clicca per tornare al 100%"
	>
		{scalePercent()}%
	</button>
	<button
		class="zoom-btn"
		onclick={() => stepScale(SCALE_STEP)}
		disabled={atMax}
		title="Ingrandisci l'interfaccia"
		aria-label="Ingrandisci l'interfaccia"
	>
		A<span class="sign">+</span>
	</button>
</div>

<style>
	.zoom {
		display: inline-flex;
		align-items: stretch;
		border: 1px solid var(--line);
		border-radius: 6px;
		background: var(--panel);
		overflow: hidden;
	}
	.zoom-btn,
	.zoom-value {
		border: 0;
		background: transparent;
		color: var(--muted);
		font-family: var(--font-term, monospace);
		cursor: pointer;
		padding: 0 0.45rem;
		min-height: 1.9rem;
		line-height: 1;
		transition: color 0.12s, background 0.12s;
	}
	.zoom-btn {
		font-size: 0.85rem;
		font-weight: 700;
	}
	.zoom-btn .sign {
		font-size: 0.95em;
	}
	.zoom-value {
		font-size: 0.72rem;
		min-width: 3.1rem;
		border-left: 1px solid var(--line);
		border-right: 1px solid var(--line);
	}
	.zoom-btn:hover:not(:disabled),
	.zoom-value:hover {
		color: var(--cyan);
		background: rgba(47, 243, 255, 0.1);
	}
	.zoom-btn:disabled {
		opacity: 0.35;
		cursor: default;
	}

	/* Su schermo stretto restano i due pulsanti: la percentuale è il primo lusso da tagliare. */
	@media (max-width: 640px) {
		.zoom-value {
			display: none;
		}
	}
</style>
