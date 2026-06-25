<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api';
	import { setSession, type Session } from '$lib/auth.svelte';

	let mode = $state<'login' | 'register'>('login');
	let username = $state('');
	let password = $state('');
	let displayName = $state('');
	let error = $state('');
	let loading = $state(false);

	async function submit(e: SubmitEvent) {
		e.preventDefault();
		error = '';
		loading = true;
		try {
			const path = mode === 'login' ? '/api/auth/login' : '/api/auth/register';
			const payload =
				mode === 'login' ? { username, password } : { username, password, displayName };
			const res = await api<NonNullable<Session>>(path, {
				method: 'POST',
				body: JSON.stringify(payload)
			});
			setSession(res);
			goto('/');
		} catch (err) {
			error = err instanceof Error ? err.message : 'Errore';
		} finally {
			loading = false;
		}
	}
</script>

<!-- Griglia synthwave in prospettiva (puramente decorativa) -->
<div class="synthwave" aria-hidden="true">
	<div class="grid-floor"></div>
</div>

<div class="login-stage">
	<div class="brand-logo"><span class="digi">DIGI</span><span class="games">GAMES</span></div>
	<p class="subtitle">▶ PLAYER LOGIN <span class="caret">_</span></p>

	<div class="card">
		<h1>{mode === 'login' ? 'INSERT COIN' : 'NEW PLAYER'}</h1>

		<form onsubmit={submit}>
		{#if mode === 'register'}
			<label>
				Nome visualizzato
				<input bind:value={displayName} required maxlength="40" />
			</label>
		{/if}
		<label>
			Username
			<input type="text" bind:value={username} required autocomplete="username" minlength="3" maxlength="30" />
		</label>
		<label>
			Password
			<input
				type="password"
				bind:value={password}
				required
				minlength="8"
				autocomplete={mode === 'login' ? 'current-password' : 'new-password'}
			/>
		</label>

		{#if error}<p class="error">{error}</p>{/if}

		<button type="submit" disabled={loading}>
			{loading ? '…' : mode === 'login' ? '▶ PRESS START' : 'CREA ACCOUNT'}
		</button>
	</form>

	<p class="switch">
		{#if mode === 'login'}
			Non hai un account?
			<button class="link" onclick={() => ((mode = 'register'), (error = ''))}>Registrati</button>
		{:else}
			Hai già un account?
			<button class="link" onclick={() => ((mode = 'login'), (error = ''))}>Accedi</button>
		{/if}
	</p>
	</div>
</div>

<style>
	/* ---- Griglia synthwave in prospettiva (decorativa, dietro la card) ---- */
	.synthwave {
		position: fixed;
		inset: 0;
		overflow: hidden;
		pointer-events: none;
		z-index: 0;
	}
	.grid-floor {
		position: absolute;
		left: -50%;
		right: -50%;
		bottom: -20%;
		height: 75%;
		background-image:
			linear-gradient(to right, rgba(47, 243, 255, 0.5) 1px, transparent 1px),
			linear-gradient(to bottom, rgba(255, 46, 136, 0.45) 1px, transparent 1px);
		background-size: 48px 48px;
		transform: perspective(320px) rotateX(62deg);
		transform-origin: center bottom;
		mask-image: linear-gradient(to top, #000 10%, transparent 85%);
		-webkit-mask-image: linear-gradient(to top, #000 10%, transparent 85%);
		animation: grid-scroll 6s linear infinite;
	}
	@keyframes grid-scroll {
		to {
			background-position: 0 48px;
		}
	}

	/* ---- Logo + sottotitolo ---- */
	.login-stage {
		position: relative;
		z-index: 1;
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: calc(100vh - 7rem);
	}
	.brand-logo {
		font-family: var(--font-display);
		font-size: clamp(1.6rem, 7vw, 2.6rem);
		letter-spacing: 0.04em;
		margin: 0 auto 0.5rem;
		line-height: 1.3;
	}
	.brand-logo .digi {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}
	.brand-logo .games {
		color: var(--accent);
		text-shadow: var(--glow-mag);
	}
	.subtitle {
		font-family: var(--font-ui);
		text-transform: uppercase;
		letter-spacing: 0.18em;
		color: var(--amber);
		font-size: 0.8rem;
		margin: 0 0 1rem;
	}
	.caret {
		animation: blink 1.05s steps(1) infinite;
	}
	@keyframes blink {
		50% {
			opacity: 0;
		}
	}

	/* ---- Card login ---- */
	.card {
		position: relative;
		max-width: 380px;
		margin: 0 auto 2rem;
		background: var(--panel);
		padding: 1.5rem;
		border-radius: 12px;
		border: 2px solid var(--accent);
		box-shadow: var(--glow-mag);
		text-align: left;
	}
	h1 {
		margin-top: 0;
		font-family: var(--font-display);
		font-size: 1rem;
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
		text-align: center;
	}
	form {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}
	label {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
		font-family: var(--font-ui);
		text-transform: uppercase;
		letter-spacing: 0.05em;
		font-size: 0.78rem;
		color: var(--muted);
	}
	input {
		padding: 0.6rem;
		border-radius: 4px;
		border: 2px solid var(--line);
		background: var(--inset);
		color: var(--text);
		font-family: var(--font-term);
		font-size: 1.1rem;
	}
	input:focus {
		outline: none;
		border-color: var(--cyan);
		box-shadow: var(--glow-cyan);
	}
	button[type='submit'] {
		margin-top: 0.5rem;
		padding: 0.8rem;
		border: 2px solid var(--amber);
		border-radius: 4px;
		background: linear-gradient(180deg, var(--accent), #c01e63);
		color: #fff;
		font-family: var(--font-ui);
		font-weight: 700;
		text-transform: uppercase;
		letter-spacing: 0.08em;
		font-size: 1rem;
		cursor: pointer;
		box-shadow: 0 0 18px rgba(255, 46, 136, 0.45);
		min-height: 44px;
		transition: box-shadow 0.12s;
	}
	button[type='submit']:hover {
		box-shadow: 0 0 26px rgba(255, 46, 136, 0.7);
	}
	button[type='submit']:disabled {
		opacity: 0.6;
	}
	.error {
		color: var(--danger);
		font-family: var(--font-term);
		font-size: 1.05rem;
		margin: 0;
	}
	.switch {
		font-size: 0.9rem;
		color: var(--muted);
		text-align: center;
	}
	.link {
		background: none;
		border: none;
		color: var(--cyan);
		cursor: pointer;
		font: inherit;
		padding: 0;
		text-decoration: underline;
	}
	@media (prefers-reduced-motion: reduce) {
		.grid-floor,
		.caret {
			animation: none;
		}
	}
</style>
