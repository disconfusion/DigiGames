<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api';
	import { auth } from '$lib/auth.svelte';
	import { ACCESSORIES, CLASSIC_OPTIONS, type HangmanOptions } from '$lib/games/hangman';

	type RoomView = {
		code: string;
		gameSlug: string;
		hostEmail: string;
		status: string;
		players: number;
		maxPlayers: number;
		isPrivate: boolean;
	};

	const GAMES = [
		{ slug: 'connect4', label: 'Forza 4' },
		{ slug: 'hangman', label: 'Impiccato' },
		{ slug: 'quiz', label: 'Quiz' },
		{ slug: 'battleship', label: 'Battaglia navale' },
		{ slug: 'minesweeper', label: 'Campo minato' },
		{ slug: 'tris', label: 'Tris' },
		{ slug: 'dama', label: 'Dama' },
		{ slug: 'chess', label: 'Scacchi' }
	];
	const labelOf = (slug: string) => GAMES.find((g) => g.slug === slug)?.label ?? slug;

	let rooms = $state<RoomView[]>([]);
	let error = $state('');
	let newGame = $state('connect4');
	let isPrivate = $state(false);
	let joinCode = $state('');

	// Opzioni quiz
	let quizSource = $state<'local' | 'opentdb' | 'mixed'>('local');

	// Opzioni scacchi
	let chessMinutes = $state(0);

	// Opzioni impiccato
	let hmPreset = $state<'classic' | 'custom'>('classic');
	let hmOptions = $state<HangmanOptions>({ ...CLASSIC_OPTIONS, accessories: [] });

	function applyClassic() {
		hmPreset = 'classic';
		hmOptions = { ...CLASSIC_OPTIONS, accessories: [] };
	}

	function toggleAccessory(key: string) {
		hmPreset = 'custom';
		hmOptions.accessories = hmOptions.accessories.includes(key)
			? hmOptions.accessories.filter((a) => a !== key)
			: [...hmOptions.accessories, key];
	}

	async function refresh() {
		try {
			rooms = await api<RoomView[]>('/api/rooms');
		} catch (e) {
			error = e instanceof Error ? e.message : 'Errore';
		}
	}

	async function create() {
		try {
			let options: unknown = undefined;
			if (newGame === 'hangman') options = hmOptions;
			else if (newGame === 'quiz') options = { source: quizSource };
			else if (newGame === 'chess') options = { minutesPerPlayer: chessMinutes };

			const r = await api<RoomView>('/api/rooms', {
				method: 'POST',
				body: JSON.stringify({ gameSlug: newGame, isPrivate, options })
			});
			goto(`/room/${r.code}`);
		} catch (e) {
			error = e instanceof Error ? e.message : 'Errore';
		}
	}

	function join(code: string) {
		const c = code.trim().toUpperCase();
		if (c) goto(`/room/${c}`);
	}

	onMount(() => {
		if (!auth.session) {
			goto('/login');
			return;
		}
		refresh();
	});
</script>

<h1>Lobby</h1>
{#if error}<p class="error">{error}</p>{/if}

<section class="panel">
	<h2>Crea una partita</h2>
	<div class="row">
		<select bind:value={newGame}>
			{#each GAMES as g (g.slug)}
				<option value={g.slug}>{g.label}</option>
			{/each}
		</select>
		<label class="check">
			<input type="checkbox" bind:checked={isPrivate} /> Privata (solo su invito)
		</label>
		<button onclick={create}>Crea</button>
	</div>

	{#if newGame === 'chess'}
		<div class="hm-options">
			<div class="opt">
				<label for="chessMinutes">Minuti a testa</label>
				<input id="chessMinutes" type="number" min="0" max="180" bind:value={chessMinutes} />
				<span class="hint">0 = senza limite di tempo</span>
			</div>
		</div>
	{/if}

	{#if newGame === 'quiz'}
		<div class="hm-options">
			<div class="opt">
				<label for="quizSource">Sorgente domande</label>
				<select id="quizSource" bind:value={quizSource}>
					<option value="local">Banca IT locale</option>
					<option value="opentdb">Open Trivia DB (varietà, in inglese)</option>
					<option value="mixed">Mista (locale + OpenTDB)</option>
				</select>
			</div>
			<span class="hint">OpenTDB è gratuito ma le domande sono in inglese; in caso di errore di rete si usa la banca locale.</span>
		</div>
	{/if}

	{#if newGame === 'hangman'}
		<div class="hm-options">
			<div class="preset-row">
				<button class="preset" class:active={hmPreset === 'classic'} onclick={applyClassic}>
					Classica
				</button>
				<button class="preset" class:active={hmPreset === 'custom'} onclick={() => (hmPreset = 'custom')}>
					Personalizzata
				</button>
			</div>

			<div class="opt">
				<label for="maxVowels">Vocali max chiamabili (totali)</label>
				<input
					id="maxVowels"
					type="number"
					min="0"
					max="5"
					bind:value={hmOptions.maxVowels}
					oninput={() => (hmPreset = 'custom')}
				/>
				<span class="hint">0 = illimitate</span>
			</div>

			<div class="opt">
				<label for="lettersPerPlayer">Lettere a testa</label>
				<input
					id="lettersPerPlayer"
					type="number"
					min="0"
					max="26"
					bind:value={hmOptions.lettersPerPlayer}
					oninput={() => (hmPreset = 'custom')}
				/>
				<span class="hint">0 = illimitate</span>
			</div>

			<div class="opt">
				<span class="opt-label">Accessori (ognuno = +1 vita)</span>
				<div class="acc-row">
					{#each ACCESSORIES as a (a.key)}
						<label class="acc" class:on={hmOptions.accessories.includes(a.key)}>
							<input
								type="checkbox"
								checked={hmOptions.accessories.includes(a.key)}
								onchange={() => toggleAccessory(a.key)}
							/>
							{a.emoji} {a.label}
						</label>
					{/each}
				</div>
				<span class="hint">
					Errori consentiti: {6 + hmOptions.accessories.length}
				</span>
			</div>
		</div>
	{/if}
</section>

<section class="panel">
	<h2>Entra con codice</h2>
	<div class="row">
		<input placeholder="ABC123" bind:value={joinCode} maxlength="6" />
		<button onclick={() => join(joinCode)}>Entra</button>
	</div>
</section>

<section class="panel">
	<div class="row between">
		<h2>Stanze pubbliche</h2>
		<button class="link" onclick={refresh}>↻ Aggiorna</button>
	</div>
	{#if rooms.length === 0}
		<p class="muted">Nessuna stanza aperta. Creane una!</p>
	{:else}
		<ul class="rooms">
			{#each rooms as r (r.code)}
				<li>
					<div>
						<strong>{labelOf(r.gameSlug)}</strong>
						<span class="muted">· {r.code} · {r.players}/{r.maxPlayers} · host {r.hostEmail}</span>
					</div>
					<button onclick={() => join(r.code)} disabled={r.players >= r.maxPlayers}>
						{r.players >= r.maxPlayers ? 'Piena' : 'Entra'}
					</button>
				</li>
			{/each}
		</ul>
	{/if}
</section>

<style>
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
	.row {
		display: flex;
		gap: 0.75rem;
		align-items: center;
		flex-wrap: wrap;
	}
	.row.between {
		justify-content: space-between;
	}
	select,
	input {
		padding: 0.55rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font-size: 1rem;
	}
	button {
		padding: 0.55rem 1rem;
		border: none;
		border-radius: 8px;
		background: var(--accent);
		color: white;
		cursor: pointer;
		font-size: 0.95rem;
	}
	button:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.link {
		background: none;
		color: var(--muted);
	}
	.check {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		color: var(--muted);
		font-size: 0.9rem;
	}
	.rooms {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}
	.rooms li {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 0.75rem;
		padding: 0.6rem 0.75rem;
		background: #0f172a;
		border-radius: 8px;
		flex-wrap: wrap;
	}
	.muted {
		color: var(--muted);
		font-size: 0.9rem;
	}
	.error {
		color: #f87171;
	}
	.hm-options {
		margin-top: 1rem;
		padding-top: 1rem;
		border-top: 1px solid #334155;
		display: flex;
		flex-direction: column;
		gap: 0.85rem;
	}
	.preset-row {
		display: flex;
		gap: 0.5rem;
	}
	.preset {
		background: #1e293b;
		border: 1px solid #334155;
		color: var(--muted);
	}
	.preset.active {
		background: var(--accent);
		color: white;
		border-color: var(--accent);
	}
	.opt {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		flex-wrap: wrap;
	}
	.opt label,
	.opt-label {
		font-size: 0.9rem;
		min-width: 12rem;
	}
	.opt input[type='number'] {
		width: 4.5rem;
	}
	.hint {
		color: var(--muted);
		font-size: 0.8rem;
	}
	.acc-row {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
	}
	.acc {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		padding: 0.35rem 0.7rem;
		border-radius: 20px;
		background: #0f172a;
		border: 1px solid #334155;
		color: var(--muted);
		font-size: 0.85rem;
		cursor: pointer;
	}
	.acc.on {
		background: #1e3a5f;
		border-color: #3b82f6;
		color: #93c5fd;
	}
</style>
