<script lang="ts">
	import PixelIcon from './PixelIcon.svelte';
	import { SPRITES } from './sprites';

	let {
		name,
		size = 24,
		title = '',
		tint = ''
	}: {
		name: string;
		size?: number;
		title?: string;
		/** Colore scelto dall'utente: ricolora le chiavi `tintKeys` della sprite (es. scarabeo). */
		tint?: string;
	} = $props();

	const sprite = $derived(SPRITES[name]);

	// Palette effettiva: quella della sprite, con le chiavi ricolorabili sostituite dal tint.
	const palette = $derived.by(() => {
		if (!sprite) return {};
		if (!tint || !sprite.tintKeys?.length) return sprite.palette;
		const p = { ...sprite.palette };
		for (const k of sprite.tintKeys) p[k] = tint;
		return p;
	});
</script>

{#if sprite}
	<PixelIcon
		frames={sprite.frames}
		{palette}
		grid={sprite.grid ?? 16}
		fps={sprite.fps ?? 4}
		motion={sprite.motion ?? ''}
		{size}
		title={title || name}
	/>
{/if}
