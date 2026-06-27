<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { api } from '$lib/api';
	import { gameLabel } from '$lib/games/catalog';
	import { showToast } from '$lib/notifications.svelte';
	import Icon from '$lib/icons/Icon.svelte';

	type Bug = {
		id: number;
		username: string;
		displayName: string;
		game: string;
		description: string;
		createdAt: string;
	};
	type AdminUser = { username: string; displayName: string; role: string };
	type PowerPrice = { id: string; game: string; label: string; emoji: string; cost: number; defaultCost: number };

	type Tab = 'bugs' | 'daily' | 'roadmap' | 'announcement' | 'users' | 'prices';
	const TABS: { id: Tab; label: string; icon: string }[] = [
		{ id: 'bugs', label: 'Bug', icon: 'bug' },
		{ id: 'daily', label: 'Parola del giorno', icon: 'calendar' },
		{ id: 'roadmap', label: 'Roadmap', icon: 'map' },
		{ id: 'announcement', label: 'Ultime Fix', icon: 'rocket' },
		{ id: 'users', label: 'Utenti', icon: 'people' },
		{ id: 'prices', label: 'Prezzi poteri', icon: 'coin' }
	];
	let tab = $state<Tab>('bugs');

	let bugs = $state<Bug[]>([]);
	let users = $state<AdminUser[]>([]);
	let prices = $state<PowerPrice[]>([]);
	let priceEdits = $state<Record<string, number>>({});
	let busy = $state<Record<string, boolean>>({});

	async function run<T>(key: string, fn: () => Promise<T>, onOk?: (r: T) => void) {
		if (busy[key]) return;
		busy = { ...busy, [key]: true };
		try {
			const r = await fn();
			onOk?.(r);
		} catch (e) {
			showToast((e as Error).message, 'error');
		} finally {
			busy = { ...busy, [key]: false };
		}
	}

	async function load() {
		try {
			bugs = await api<Bug[]>('/api/bugs');
			users = await api<AdminUser[]>('/api/admin/users');
		} catch (e) {
			showToast((e as Error).message, 'error');
		}
	}

	async function loadPrices() {
		try {
			prices = await api<PowerPrice[]>('/api/admin/power-prices');
			priceEdits = Object.fromEntries(prices.map((p) => [p.id, p.cost]));
		} catch (e) {
			showToast((e as Error).message, 'error');
		}
	}

	async function loadRoadmap() {
		try {
			const r = await api<{ content: string }>('/api/roadmap');
			roadmapContent = r.content;
		} catch { /* ignora */ }
	}

	let announcementContent = $state('');
	async function loadAnnouncement() {
		try {
			const a = await api<{ content: string }>('/api/announcement');
			announcementContent = a.content;
		} catch { /* ignora */ }
	}

	function saveAnnouncement() {
		run(
			'announcement',
			() =>
				api<{ message: string }>('/api/admin/announcement', {
					method: 'PUT',
					body: JSON.stringify({ content: announcementContent })
				}),
			(r) => showToast(r.message, 'success')
		);
	}

	onMount(() => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		if (auth.session.role !== 'admin') {
			goto('/');
			return;
		}
		load();
		loadRoadmap();
		loadAnnouncement();
		loadPrices();
	});

	function deleteBug(id: number) {
		run(`bug-${id}`, () => api(`/api/bugs/${id}`, { method: 'DELETE' }), () => {
			bugs = bugs.filter((b) => b.id !== id);
			showToast('Bug eliminato', 'success');
		});
	}

	function resetStats(u: AdminUser) {
		if (!confirm(`Azzerare le statistiche di ${u.displayName}?`)) return;
		run(
			`stats-${u.username}`,
			() => api<{ message: string }>(`/api/admin/users/${u.username}/reset-stats`, { method: 'POST' }),
			(r) => showToast(r.message, 'success')
		);
	}

	function dailyReset(u: AdminUser) {
		run(
			`daily-${u.username}`,
			() => api<{ message: string }>(`/api/admin/users/${u.username}/daily-reset`, { method: 'POST' }),
			(r) => showToast(r.message, 'success')
		);
	}

	function resetPassword(u: AdminUser) {
		const np = prompt(`Nuova password per ${u.username} (min 6):`);
		if (!np) return;
		run(
			`pwd-${u.username}`,
			() =>
				api<{ message: string }>(`/api/admin/users/${u.username}/reset-password`, {
					method: 'POST',
					body: JSON.stringify({ newPassword: np })
				}),
			(r) => showToast(r.message, 'success')
		);
	}

	let customWord = $state('');
	let roadmapContent = $state('');

	function saveRoadmap() {
		run(
			'roadmap',
			() =>
				api<{ message: string }>('/api/admin/roadmap', {
					method: 'PUT',
					body: JSON.stringify({ content: roadmapContent })
				}),
			(r) => showToast(r.message, 'success')
		);
	}

	function setDailyWord() {
		if (!customWord.trim()) return;
		run(
			'setword',
			() =>
				api<{ message: string }>('/api/admin/daily/set-word', {
					method: 'POST',
					body: JSON.stringify({ word: customWord.trim() })
				}),
			(r) => {
				showToast(r.message, 'success');
				customWord = '';
			}
		);
	}

	function resetDailyWord() {
		if (!confirm('Resettare la parola del giorno? Riparte con la parola automatica.')) return;
		run(
			'resetword',
			() => api<{ message: string }>('/api/admin/daily/reset', { method: 'POST' }),
			(r) => showToast(r.message, 'success')
		);
	}

	function savePrice(p: PowerPrice) {
		const cost = priceEdits[p.id];
		if (cost == null || cost < 0) return;
		run(
			`price-${p.id}`,
			() =>
				api<{ message: string }>(`/api/admin/power-prices/${p.id}`, {
					method: 'PUT',
					body: JSON.stringify({ cost })
				}),
			(r) => {
				showToast(r.message, 'success');
				prices = prices.map((x) => (x.id === p.id ? { ...x, cost } : x));
			}
		);
	}
</script>

<h1><Icon name="tools" size={22} title="Admin" /> Pannello Admin</h1>

<nav class="tabs">
	{#each TABS as t (t.id)}
		<button class="tab" class:active={tab === t.id} onclick={() => (tab = t.id)}>
			<Icon name={t.icon} size={16} /> {t.label}{#if t.id === 'bugs' && bugs.length}<span class="count">{bugs.length}</span>{/if}
		</button>
	{/each}
</nav>

{#if tab === 'bugs'}
	<section class="panel">
		<h2><Icon name="bug" size={18} /> Segnalazioni bug ({bugs.length})</h2>
		{#if bugs.length === 0}
			<p class="muted">Nessuna segnalazione.</p>
		{:else}
			<ul class="bugs">
				{#each bugs as b (b.id)}
					<li>
						<div class="bug-meta">
							<strong>{b.displayName}</strong>
							<span class="muted">@{b.username} · {gameLabel(b.game)} · {b.createdAt.slice(0, 16).replace('T', ' ')}</span>
						</div>
						<p class="bug-desc">{b.description}</p>
						<button class="danger" disabled={busy[`bug-${b.id}`]} onclick={() => deleteBug(b.id)}>Elimina</button>
					</li>
				{/each}
			</ul>
		{/if}
	</section>
{:else if tab === 'daily'}
	<section class="panel">
		<h2><Icon name="calendar" size={18} /> Parola del Giorno</h2>
		<div class="daily-controls">
			<form class="word-form" onsubmit={(e) => { e.preventDefault(); setDailyWord(); }}>
				<input placeholder="Parola personalizzata (solo a-z)…" bind:value={customWord} autocomplete="off" />
				<button type="submit" disabled={!customWord.trim() || busy['setword']}>Imposta parola</button>
			</form>
			<button class="danger" disabled={busy['resetword']} onclick={resetDailyWord}>Reset → parola automatica</button>
		</div>
		<p class="muted hint">Impostare una parola resetta anche tutte le mosse di oggi. Il reset ripristina la parola automatica calcolata dalla data.</p>
	</section>
{:else if tab === 'roadmap'}
	<section class="panel">
		<h2><Icon name="map" size={18} /> Roadmap</h2>
		<form class="roadmap-form" onsubmit={(e) => { e.preventDefault(); saveRoadmap(); }}>
			<textarea placeholder="Scrivi la roadmap in testo libero o Markdown…" bind:value={roadmapContent} rows="14"></textarea>
			<button type="submit" disabled={busy['roadmap']}>Salva roadmap</button>
		</form>
		<p class="muted hint">Visibile a tutti gli utenti su <a href="/roadmap" target="_blank">/roadmap</a>. NB: al riavvio del server viene riallineata al file roadmap.md del repo.</p>
	</section>
{:else if tab === 'announcement'}
	<section class="panel">
		<h2><Icon name="rocket" size={18} /> Ultime Fix</h2>
		<form class="roadmap-form" onsubmit={(e) => { e.preventDefault(); saveAnnouncement(); }}>
			<textarea placeholder="Scrivi le novità in Markdown (titoli, elenchi…). Vuoto = niente modale." bind:value={announcementContent} rows="12"></textarea>
			<button type="submit" disabled={busy['announcement']}>Salva e mostra a tutti</button>
		</form>
		<p class="muted hint">È la modale "Novità" che appare in home dopo il login. Ogni salvataggio la rende di nuovo visibile a tutti (una volta a testa).</p>
	</section>
{:else if tab === 'users'}
	<section class="panel">
		<h2><Icon name="people" size={18} /> Utenti ({users.length})</h2>
		<ul class="users">
			{#each users as u (u.username)}
				<li>
					<div>
						<strong>{u.displayName}</strong>
						<span class="muted">@{u.username}{u.role === 'admin' ? ' · admin' : ''}</span>
					</div>
					<div class="actions">
						<button disabled={busy[`daily-${u.username}`]} onclick={() => dailyReset(u)}>Reset parola del giorno</button>
						<button disabled={busy[`stats-${u.username}`]} onclick={() => resetStats(u)}>Azzera stats</button>
						<button disabled={busy[`pwd-${u.username}`]} onclick={() => resetPassword(u)}>Reset password</button>
					</div>
				</li>
			{/each}
		</ul>
	</section>
{:else if tab === 'prices'}
	<section class="panel">
		<h2><Icon name="coin" size={18} /> Prezzi poteri</h2>
		{#if prices.length === 0}
			<p class="muted">Nessun potere disponibile.</p>
		{:else}
			<ul class="prices">
				{#each prices as p (p.id)}
					<li>
						<div class="price-meta">
							<span class="p-emoji"><Icon name={p.id} size={28} title={p.label} /></span>
							<div>
								<strong>{p.label}</strong>
								<span class="muted">{gameLabel(p.game)} · default {p.defaultCost}</span>
							</div>
						</div>
						<div class="price-edit">
							<input type="number" min="0" bind:value={priceEdits[p.id]} />
							<span class="unit">Token</span>
							<button disabled={busy[`price-${p.id}`] || priceEdits[p.id] === p.cost} onclick={() => savePrice(p)}>Salva</button>
						</div>
					</li>
				{/each}
			</ul>
			<p class="muted hint">Il prezzo modificato sovrascrive il default ed è subito attivo nello shop.</p>
		{/if}
	</section>
{/if}

<style>
	h1 {
		margin: 0 0 1rem;
	}
	.tabs {
		display: flex;
		gap: 0.4rem;
		flex-wrap: wrap;
		margin-bottom: 1.25rem;
		border-bottom: 1px solid var(--line);
		padding-bottom: 0.5rem;
	}
	.tab {
		display: inline-flex;
		align-items: center;
		gap: 0.4rem;
		padding: 0.45rem 0.9rem;
		border: 1px solid var(--line);
		border-radius: 999px;
		background: var(--panel);
		color: var(--muted);
		cursor: pointer;
		font-size: 0.85rem;
		font-family: var(--font-ui);
	}
	.tab.active {
		border-color: var(--cyan);
		color: var(--cyan);
		box-shadow: var(--glow-cyan);
	}
	.count {
		background: var(--accent);
		color: #fff;
		border-radius: 999px;
		min-width: 1.2rem;
		height: 1.2rem;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		font-size: 0.72rem;
		font-weight: 700;
		padding: 0 0.3rem;
	}
	.panel {
		background: var(--panel);
		padding: 1rem 1.25rem;
		border-radius: 12px;
		margin-bottom: 1.25rem;
	}
	h2 {
		font-size: 1.05rem;
		margin: 0 0 0.75rem;
	}
	.muted {
		color: var(--muted);
		font-size: 0.85rem;
	}
	button:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}
	.bugs,
	.users,
	.prices {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
	}
	.bugs li {
		background: var(--inset);
		border-radius: 8px;
		padding: 0.7rem 0.9rem;
	}
	.bug-meta {
		display: flex;
		flex-direction: column;
	}
	.bug-desc {
		margin: 0.5rem 0;
		white-space: pre-wrap;
	}
	.users li,
	.prices li {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 0.75rem;
		background: var(--inset);
		border-radius: 8px;
		padding: 0.6rem 0.9rem;
		flex-wrap: wrap;
	}
	.actions {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
	}
	button {
		padding: 0.45rem 0.9rem;
		border: 1px solid var(--line);
		border-radius: 8px;
		background: var(--panel);
		color: var(--text);
		cursor: pointer;
		font-size: 0.85rem;
	}
	button.danger {
		background: #7f1d1d;
		border-color: #991b1b;
		color: #fecaca;
		margin-top: 0.4rem;
	}
	.daily-controls {
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
	}
	.word-form {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
	}
	.word-form input {
		flex: 1;
		min-width: 180px;
		padding: 0.45rem 0.7rem;
		border-radius: 8px;
		border: 1px solid var(--line);
		background: var(--inset);
		color: var(--text);
		font-size: 0.9rem;
	}
	.hint {
		margin: 0.5rem 0 0;
		font-size: 0.8rem;
	}
	.roadmap-form {
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
	}
	.roadmap-form textarea {
		width: 100%;
		padding: 0.6rem 0.8rem;
		border-radius: 8px;
		border: 1px solid var(--line);
		background: var(--inset);
		color: var(--text);
		font-size: 0.9rem;
		font-family: ui-monospace, monospace;
		resize: vertical;
		box-sizing: border-box;
	}
	.hint a {
		color: var(--accent);
	}
	.price-meta {
		display: flex;
		align-items: center;
		gap: 0.7rem;
	}
	.price-meta > div {
		display: flex;
		flex-direction: column;
	}
	.p-emoji {
		font-size: 1.4rem;
	}
	.price-edit {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}
	.price-edit input {
		width: 5rem;
		padding: 0.4rem 0.5rem;
		border-radius: 8px;
		border: 1px solid var(--line);
		background: var(--inset);
		color: var(--text);
		font-size: 0.9rem;
	}
	.unit {
		color: var(--muted);
		font-size: 0.8rem;
	}
</style>
