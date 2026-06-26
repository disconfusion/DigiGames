<script lang="ts">
	import { scale } from 'svelte/transition';
	import type { Snippet } from 'svelte';
	import Icon from '$lib/icons/Icon.svelte';

	export type GameResult = 'win' | 'lose' | 'draw';

	let {
		result,
		title,
		message = '',
		playAgainLabel = 'Nuova partita',
		onPlayAgain,
		children
	}: {
		result: GameResult;
		title?: string;
		message?: string;
		playAgainLabel?: string;
		onPlayAgain?: () => void;
		children?: Snippet;
	} = $props();

	const DEFAULT_TITLE: Record<GameResult, string> = {
		win: 'HAI VINTO',
		lose: 'HAI PERSO',
		draw: 'PAREGGIO'
	};
</script>

<div class="result {result}" role="status" aria-live="assertive">
	<div class="card" in:scale={{ duration: 320, start: 0.8 }}>
		<div class="icon" aria-hidden="true"><Icon name={result} size={64} /></div>
		<h2 class="title">{title ?? DEFAULT_TITLE[result]}</h2>
		{#if message}<p class="msg">{message}</p>{/if}
		{#if children}
			<div class="extra">{@render children()}</div>
		{/if}
		{#if onPlayAgain}
			<button class="again" onclick={onPlayAgain}>{playAgainLabel}</button>
		{/if}
	</div>
</div>

<style>
	.result {
		display: flex;
		justify-content: center;
		width: 100%;
	}

	.card {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.7rem;
		padding: 1.3rem 1.8rem;
		border-radius: 14px;
		border: 2px solid var(--line);
		background: color-mix(in srgb, var(--inset) 85%, transparent);
		text-align: center;
		max-width: 92vw;
	}

	.icon {
		line-height: 0;
		animation: pop-in 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
	}

	.title {
		margin: 0;
		font-family: var(--font-display);
		font-size: clamp(1.2rem, 5vw, 2rem);
		letter-spacing: 0.06em;
		line-height: 1.2;
	}

	.msg {
		margin: 0;
		font-family: var(--font-term, monospace);
		font-size: 1.05rem;
		color: var(--muted);
		letter-spacing: 0.02em;
	}

	.extra {
		width: 100%;
	}

	/* ── Varianti per esito ────────────────────────────────────────── */
	.win .card {
		border-color: var(--green);
		box-shadow: 0 0 22px color-mix(in srgb, var(--green) 35%, transparent);
	}
	.win .title {
		color: var(--green);
		animation: glow-pulse-green 1.6s ease-in-out infinite;
	}

	.lose .card {
		border-color: var(--danger);
		box-shadow: 0 0 22px color-mix(in srgb, var(--danger) 30%, transparent);
		animation: shake 0.5s ease-in-out both;
	}
	.lose .title {
		color: var(--danger);
		text-shadow: 0 0 10px color-mix(in srgb, var(--danger) 55%, transparent);
	}

	.draw .card {
		border-color: var(--amber);
		box-shadow: 0 0 22px color-mix(in srgb, var(--amber) 28%, transparent);
	}
	.draw .title {
		color: var(--amber);
		text-shadow: 0 0 10px color-mix(in srgb, var(--amber) 50%, transparent);
	}

	/* ── Bottone "gioca ancora" ────────────────────────────────────── */
	.again {
		margin-top: 0.3rem;
		min-height: 44px;
		padding: 0.55rem 1.5rem;
		border: 2px solid var(--accent);
		border-radius: 6px;
		background: color-mix(in srgb, var(--accent) 18%, var(--panel));
		color: var(--accent);
		font-family: var(--font-ui);
		font-size: 0.8rem;
		font-weight: 700;
		letter-spacing: 0.06em;
		text-transform: uppercase;
		cursor: pointer;
		text-shadow: var(--glow-mag);
		box-shadow: 0 0 10px color-mix(in srgb, var(--accent) 30%, transparent);
		transition: background 0.15s, box-shadow 0.15s;
	}
	.again:hover {
		background: color-mix(in srgb, var(--accent) 32%, var(--panel));
		box-shadow: 0 0 18px color-mix(in srgb, var(--accent) 55%, transparent);
	}

	/* ── Animazioni ────────────────────────────────────────────────── */
	@keyframes pop-in {
		from { transform: scale(0); opacity: 0; }
		to { transform: scale(1); opacity: 1; }
	}
	@keyframes glow-pulse-green {
		0%, 100% { text-shadow: 0 0 8px color-mix(in srgb, var(--green) 45%, transparent); }
		50% { text-shadow: 0 0 18px var(--green), 0 0 30px color-mix(in srgb, var(--green) 60%, transparent); }
	}
	@keyframes shake {
		0%, 100% { transform: translateX(0); }
		20% { transform: translateX(-8px); }
		40% { transform: translateX(8px); }
		60% { transform: translateX(-5px); }
		80% { transform: translateX(5px); }
	}

	@media (prefers-reduced-motion: reduce) {
		.icon,
		.win .title,
		.lose .card,
		.card {
			animation: none !important;
		}
		.again {
			transition: none;
		}
	}
</style>
