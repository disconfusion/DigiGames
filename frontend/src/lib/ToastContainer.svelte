<script lang="ts">
	import { fly, fade } from 'svelte/transition';
	import { notifications, dismissToast } from '$lib/notifications.svelte';
	import Icon from '$lib/icons/Icon.svelte';

	// Icona pixel in base al tipo di toast
	const KIND_ICON: Record<string, string> = {
		invite: 'mail',
		success: 'check',
		error: 'cross',
		info: 'speech'
	};
</script>

<div class="toast-stack" aria-live="polite" aria-atomic="false">
	{#each notifications.toasts as t (t.id)}
		<div
			class="toast {t.kind}"
			role="status"
			in:fly={{ x: 40, duration: 220 }}
			out:fade={{ duration: 160 }}
		>
			<Icon name={KIND_ICON[t.kind] ?? 'speech'} size={18} />
			<span class="msg">{t.message}</span>
			<button class="close" onclick={() => dismissToast(t.id)} aria-label="Chiudi notifica">✕</button>
		</div>
	{/each}
</div>

<style>
	.toast-stack {
		position: fixed;
		top: 1rem;
		right: 1rem;
		z-index: 10001; /* sopra scanline (9998) e modali (10000) */
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
		max-width: min(92vw, 360px);
		pointer-events: none;
	}

	.toast {
		pointer-events: auto;
		display: flex;
		align-items: center;
		gap: 0.6rem;
		padding: 0.7rem 0.9rem;
		border-radius: 10px;
		background: var(--panel);
		border: 1px solid var(--line);
		border-left-width: 4px;
		color: var(--text);
		font-family: var(--font-term, monospace);
		font-size: 1rem;
		line-height: 1.35;
		letter-spacing: 0.02em;
		box-shadow: 0 4px 18px rgba(0, 0, 0, 0.5);
	}

	/* Varianti per tipo */
	.toast.invite {
		border-left-color: var(--accent);
		box-shadow: 0 4px 18px rgba(0, 0, 0, 0.5), 0 0 12px color-mix(in srgb, var(--accent) 35%, transparent);
	}
	.toast.success {
		border-left-color: var(--green);
		box-shadow: 0 4px 18px rgba(0, 0, 0, 0.5), 0 0 12px color-mix(in srgb, var(--green) 30%, transparent);
	}
	.toast.info {
		border-left-color: var(--cyan);
		box-shadow: 0 4px 18px rgba(0, 0, 0, 0.5), 0 0 12px color-mix(in srgb, var(--cyan) 30%, transparent);
	}
	.toast.error {
		border-left-color: var(--danger);
		box-shadow: 0 4px 18px rgba(0, 0, 0, 0.5), 0 0 12px color-mix(in srgb, var(--danger) 35%, transparent);
	}

	.msg {
		flex: 1;
	}

	.close {
		flex-shrink: 0;
		background: none;
		border: none;
		color: var(--muted);
		font-size: 0.9rem;
		line-height: 1;
		cursor: pointer;
		padding: 0.2rem;
		transition: color 0.12s;
	}
	.close:hover {
		color: var(--text);
	}

	@media (max-width: 640px) {
		.toast-stack {
			top: auto;
			bottom: 1rem;
			left: 0.8rem;
			right: 0.8rem;
			max-width: none;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.close {
			transition: none;
		}
	}
</style>
