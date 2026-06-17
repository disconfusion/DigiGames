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

<div class="card">
	<h1>{mode === 'login' ? 'Accedi' : 'Registrati'}</h1>

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
			{loading ? '…' : mode === 'login' ? 'Accedi' : 'Crea account'}
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

<style>
	.card {
		max-width: 380px;
		margin: 2rem auto;
		background: var(--panel);
		padding: 1.5rem;
		border-radius: 12px;
	}
	h1 {
		margin-top: 0;
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
		font-size: 0.9rem;
		color: var(--muted);
	}
	input {
		padding: 0.6rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font-size: 1rem;
	}
	button[type='submit'] {
		margin-top: 0.5rem;
		padding: 0.7rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: white;
		font-size: 1rem;
		cursor: pointer;
	}
	button[type='submit']:disabled {
		opacity: 0.6;
	}
	.error {
		color: #f87171;
		font-size: 0.9rem;
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
		color: var(--accent);
		cursor: pointer;
		font: inherit;
		padding: 0;
	}
</style>
