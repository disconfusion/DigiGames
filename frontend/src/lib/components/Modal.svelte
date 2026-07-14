<script lang="ts">
	import type { Snippet } from 'svelte';
	import Icon from '$lib/icons/Icon.svelte';

	let {
		title,
		icon,
		onClose,
		children,
		actions
	}: {
		title: string;
		icon?: string;
		onClose: () => void;
		children: Snippet;
		actions?: Snippet;
	} = $props();

	// Chiusura sempre possibile anche se il contenuto è troppo alto per lo schermo.
	function onKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') onClose();
	}
</script>

<svelte:window onkeydown={onKeydown} />

<div class="modal-backdrop" role="presentation" onclick={onClose}>
	<div class="modal" role="dialog" aria-modal="true" onclick={(e) => e.stopPropagation()}>
		<div class="modal-head">
			<h2>{#if icon}<Icon name={icon} size={20} title={title} />{/if} {title}</h2>
			<button class="x" onclick={onClose} aria-label="Chiudi">✕</button>
		</div>
		<div class="modal-body">
			{@render children()}
		</div>
		{#if actions}
			<div class="modal-actions">{@render actions()}</div>
		{/if}
	</div>
</div>

<style>
	.modal-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.6);
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 1rem;
		/* sopra le scanline globali (z-index 9998 in retro-crt-theme.css) */
		z-index: 10000;
	}
	.modal {
		background: var(--panel);
		border-radius: 14px;
		padding: 1.25rem;
		/* safe-area mobile: evita che bordo/azioni finiscano sotto la barra/tacca del telefono */
		padding-bottom: max(1.25rem, env(safe-area-inset-bottom));
		width: 100%;
		max-width: 460px;
		border: 2px solid var(--accent);
		box-shadow: var(--glow-mag);
		box-sizing: border-box;
		/* Responsive: mai più alta del viewport; head + azioni restano visibili, il corpo scrolla */
		max-height: 90vh;
		max-height: 90dvh;
		display: flex;
		flex-direction: column;
	}
	.modal-head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 0.75rem;
		flex-shrink: 0;
	}
	.modal-head h2 {
		margin: 0;
		display: inline-flex;
		align-items: center;
		gap: 0.4rem;
		font-family: var(--font-display);
		font-size: 0.9rem;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.x {
		background: none;
		border: none;
		color: var(--muted);
		font-size: 1.1rem;
		cursor: pointer;
		line-height: 1;
		padding: 0;
	}
	/* Corpo scrollabile dentro la modale ad altezza limitata */
	.modal-body {
		overflow-y: auto;
		min-height: 0;
	}
	.modal-actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.6rem;
		flex-shrink: 0;
		margin-top: 1rem;
	}
</style>
