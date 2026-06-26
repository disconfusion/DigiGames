<script lang="ts">
	import PixelIcon from './PixelIcon.svelte';
	import { SPRITES } from './sprites';

	let {
		name,
		size = 24,
		title = ''
	}: { name: string; size?: number; title?: string } = $props();

	const sprite = $derived(SPRITES[name]);
</script>

{#if sprite}
	<PixelIcon
		frames={sprite.frames}
		palette={sprite.palette}
		grid={sprite.grid ?? 16}
		fps={sprite.fps ?? 4}
		motion={sprite.motion ?? ''}
		{size}
		title={title || name}
	/>
{/if}
