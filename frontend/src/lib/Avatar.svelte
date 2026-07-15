<script lang="ts">
	import { parseAvatar, renderAvatar } from '$lib/avatar';
	import Icon from '$lib/icons/Icon.svelte';

	/** Accessorio equipaggiato: id sprite + slot ("testa" | "occhi" | "bocca"). */
	type Accessory = { id: string; slot: string };

	let {
		avatar = null,
		accessories = [],
		fontSize = 16,
		bg = true
	}: {
		avatar?: string | null;
		accessories?: Accessory[];
		fontSize?: number;
		bg?: boolean;
	} = $props();

	const face = $derived(renderAvatar(parseAvatar(avatar)));
	// Un accessorio per slot (l'ultimo vince se per errore ce ne fossero due).
	const bySlot = $derived(
		Object.fromEntries(accessories.map((a) => [a.slot, a.id])) as Record<string, string>
	);
	// L'icona dell'accessorio è dimensionata sul volto (~box largo 7 char).
	const accSize = $derived(Math.round(fontSize * 2.3));
</script>

<span class="avatar" class:bg style:--fs="{fontSize}px">
	<pre class="face">{face}</pre>
	{#if bySlot.testa}
		<span class="acc slot-testa"><Icon name={bySlot.testa} size={accSize} /></span>
	{/if}
	{#if bySlot.occhi}
		<span class="acc slot-occhi"><Icon name={bySlot.occhi} size={accSize} /></span>
	{/if}
	{#if bySlot.bocca}
		<span class="acc slot-bocca"><Icon name={bySlot.bocca} size={accSize} /></span>
	{/if}
</span>

<style>
	.avatar {
		position: relative;
		display: inline-block;
		line-height: 0;
	}
	.face {
		margin: 0;
		font-family: ui-monospace, monospace;
		font-size: var(--fs);
		line-height: 1.15;
	}
	.avatar.bg .face {
		background: #0f172a;
		padding: 0.75rem 1rem;
		border-radius: 8px;
	}
	.acc {
		position: absolute;
		left: 50%;
		display: inline-flex;
		pointer-events: none;
		z-index: 1;
	}
	/* Posizioni relative al riquadro del volto (approssimate, robuste su tutte le taglie). */
	.slot-testa {
		top: 0;
		transform: translate(-50%, -58%);
	}
	.slot-occhi {
		top: 40%;
		transform: translate(-50%, -50%);
	}
	.slot-bocca {
		top: 72%;
		transform: translate(-50%, -50%);
	}
</style>
