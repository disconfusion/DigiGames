<script lang="ts">
	import Icon from '$lib/icons/Icon.svelte';
	import { GAME_CATALOG } from './catalog';

	type Item = { slug: string; label: string };

	let {
		value = $bindable(),
		items = GAME_CATALOG as Item[],
		iconSize = 34
	}: { value: string; items?: Item[]; iconSize?: number } = $props();
</script>

<div class="picker" role="radiogroup">
	{#each items as g (g.slug)}
		<button
			type="button"
			class="opt"
			class:on={value === g.slug}
			role="radio"
			aria-checked={value === g.slug}
			onclick={() => (value = g.slug)}
		>
			<Icon name={g.slug} size={iconSize} title={g.label} />
			<span class="lbl">{g.label}</span>
		</button>
	{/each}
</div>

<style>
	.picker {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 0.4rem;
		width: 100%;
	}
	.opt {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: flex-start;
		gap: 0.3rem;
		margin: 0;
		padding: 0.5rem 0.2rem;
		min-height: 70px;
		background: var(--inset);
		border: 2px solid var(--line);
		border-radius: 8px;
		color: var(--muted);
		cursor: pointer;
		text-transform: none;
		letter-spacing: 0.01em;
		font-family: var(--font-ui);
		transition: border-color 0.12s, box-shadow 0.12s, color 0.12s, background 0.12s;
	}
	.opt .lbl {
		font-size: 0.62rem;
		line-height: 1.15;
		text-align: center;
	}
	.opt:hover {
		border-color: var(--cyan);
		color: var(--text);
		box-shadow: var(--glow-cyan);
	}
	.opt.on {
		border-color: var(--accent);
		color: var(--text);
		background: #2a0f33;
		box-shadow: 0 0 12px rgba(255, 46, 136, 0.45);
	}
	@media (max-width: 760px) {
		.picker {
			grid-template-columns: repeat(3, 1fr);
		}
	}
</style>
