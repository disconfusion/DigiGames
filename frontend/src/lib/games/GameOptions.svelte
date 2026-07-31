<script lang="ts">
	import { ACCESSORIES, CLASSIC_OPTIONS, type HangmanOptions } from '$lib/games/hangman';
	import Icon from '$lib/icons/Icon.svelte';

	let { game, options = $bindable() }: { game: string; options: unknown } = $props();

	let quizSource = $state<'local' | 'opentdb' | 'mixed'>('local');
	let chessMinutes = $state(0);
	let trisVanish = $state(false);
	let pongPoints = $state(7);
	// Battle City: co-op contro l'IA o duello 1v1, e livello di partenza
	let bcMode = $state<'coop' | 'duello'>('coop');
	let bcLevel = $state(1);
	let hmPreset = $state<'classic' | 'custom'>('classic');
	// Sorgente delle parole: dizionario online (con difficoltà e lingua) o parole locali
	let hmSource = $state<'locale' | 'dizionario'>('locale');
	let hmLang = $state('it');
	let hmDifficulty = $state<'random' | 'facile' | 'media' | 'difficile'>('random');
	const LANGS = [
		{ code: 'it', label: 'Italiano' },
		{ code: 'en', label: 'Inglese' },
		{ code: 'es', label: 'Spagnolo' },
		{ code: 'fr', label: 'Francese' },
		{ code: 'de', label: 'Tedesco' },
		{ code: 'pt-br', label: 'Portoghese (BR)' },
		{ code: 'ro', label: 'Romeno' }
	];
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

	// Mantiene `options` allineato al gioco selezionato e ai valori scelti.
	$effect(() => {
		if (game === 'hangman') {
			options = {
				maxVowels: hmOptions.maxVowels,
				lettersPerPlayer: hmOptions.lettersPerPlayer,
				accessories: [...hmOptions.accessories],
				wordSource: hmSource,
				lang: hmLang,
				difficulty: hmDifficulty
			};
		} else if (game === 'quiz') {
			options = { source: quizSource };
		} else if (game === 'chess') {
			options = { minutesPerPlayer: chessMinutes };
		} else if (game === 'tris') {
			options = { vanish: trisVanish };
		} else if (game === 'pong') {
			options = { pointsToWin: pongPoints };
		} else if (game === 'battlecity') {
			options = { mode: bcMode, level: bcLevel };
		} else {
			options = undefined;
		}
	});

	const hasOptions = $derived(
		game === 'hangman' ||
			game === 'quiz' ||
			game === 'chess' ||
			game === 'tris' ||
			game === 'pong' ||
			game === 'battlecity'
	);
</script>

{#if hasOptions}
	<div class="game-options">
		{#if game === 'chess'}
			<div class="opt">
				<label for="chessMinutes">Minuti a testa</label>
				<input id="chessMinutes" type="number" min="0" max="180" bind:value={chessMinutes} />
				<span class="hint">0 = senza limite di tempo</span>
			</div>
		{:else if game === 'quiz'}
			<div class="opt">
				<label for="quizSource">Sorgente domande</label>
				<select id="quizSource" bind:value={quizSource}>
					<option value="local">Banca IT locale</option>
					<option value="opentdb">Open Trivia DB (varietà, in inglese)</option>
					<option value="mixed">Mista (locale + OpenTDB)</option>
				</select>
				<span class="hint">OpenTDB è gratuito ma in inglese; su errore di rete si usa la banca locale.</span>
			</div>
		{:else if game === 'pong'}
			<div class="opt">
				<label for="pongPoints">Punti per vincere</label>
				<input id="pongPoints" type="number" min="1" max="21" bind:value={pongPoints} />
				<span class="hint">
					Il primo che li raggiunge vince (1–21, default 7). Il colore del monitor si sceglie
					in partita, ognuno il suo.
				</span>
			</div>
		{:else if game === 'battlecity'}
			<div class="opt">
				<label for="bcMode">Modalità</label>
				<select id="bcMode" bind:value={bcMode}>
					<option value="coop">Co-op: difendete l'aquila dai tank nemici</option>
					<option value="duello">Duello 1v1: primo a 3 colpi</option>
				</select>
				<span class="hint">
					In co-op si abbattono 20 tank IA per livello e si raccolgono i bonus; nel duello
					niente nemici, solo voi due sulla mappa a muri distruttibili.
				</span>
			</div>
			{#if bcMode === 'coop'}
				<div class="opt">
					<label for="bcLevel">Livello di partenza</label>
					<input id="bcLevel" type="number" min="1" max="99" bind:value={bcLevel} />
					<span class="hint">
						I primi 6 sono disegnati a mano, dal 7° in poi sono generati e sempre più difficili.
						Completando un livello si passa al successivo.
					</span>
				</div>
			{/if}
		{:else if game === 'tris'}
			<div class="opt">
				<label class="switch">
					<input type="checkbox" bind:checked={trisVanish} />
					<span>Modalità sparizione</span>
				</label>
				<span class="hint">
					Ogni giocatore tiene al massimo 3 segni: piazzando il 4°, il proprio più vecchio
					sparisce. Niente pareggi.
				</span>
			</div>
		{:else if game === 'hangman'}
			<div class="opt">
				<label for="hmSource">Parole</label>
				<select id="hmSource" bind:value={hmSource}>
					<option value="locale">Elenco locale (sempre disponibile)</option>
					<option value="dizionario">Dizionario online (parole nuove ogni volta)</option>
				</select>
				<span class="hint">
					Col dizionario la prima parola richiede qualche secondo; le successive vengono
					preparate in background mentre giocate. Se non risponde si usa l'elenco locale.
				</span>
			</div>
			{#if hmSource === 'dizionario'}
				<div class="opt">
					<label for="hmLang">Lingua</label>
					<select id="hmLang" bind:value={hmLang}>
						{#each LANGS as l (l.code)}<option value={l.code}>{l.label}</option>{/each}
					</select>
				</div>
			{/if}
			<div class="opt">
				<label for="hmDifficulty">Difficoltà</label>
				<select id="hmDifficulty" bind:value={hmDifficulty}>
					<option value="random">Casuale</option>
					<option value="facile">Facile (4-6 lettere)</option>
					<option value="media">Media (7-9 lettere)</option>
					<option value="difficile">Difficile (10+ lettere)</option>
				</select>
				<span class="hint">La difficoltà è la lunghezza della parola: più lettere, più tentativi.</span>
			</div>
			<div class="preset-row">
				<button class="preset" class:active={hmPreset === 'classic'} onclick={applyClassic}>Classica</button>
				<button class="preset" class:active={hmPreset === 'custom'} onclick={() => (hmPreset = 'custom')}>
					Personalizzata
				</button>
			</div>
			<div class="opt">
				<label for="maxVowels">Vocali max (totali)</label>
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
							<Icon name={a.key} size={16} title={a.label} /> {a.label}
						</label>
					{/each}
				</div>
				<span class="hint">Errori consentiti: {6 + hmOptions.accessories.length}</span>
			</div>
		{/if}
	</div>
{/if}

<style>
	.game-options {
		width: 100%;
		margin-top: 0.5rem;
		padding-top: 0.75rem;
		border-top: 1px solid #334155;
		display: flex;
		flex-direction: column;
		gap: 0.7rem;
		text-align: left;
	}
	.opt {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
	}
	.opt label,
	.opt-label {
		font-size: 0.85rem;
		color: var(--muted);
	}
	.opt input[type='number'],
	.opt select {
		padding: 0.45rem;
		border-radius: 8px;
		border: 1px solid #334155;
		background: #0f172a;
		color: var(--text);
		font-size: 0.95rem;
		width: 100%;
	}
	.hint {
		color: var(--muted);
		font-size: 0.78rem;
	}
	.switch {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		font-size: 0.9rem;
		color: var(--text);
		cursor: pointer;
	}
	.switch input {
		width: 1.1rem;
		height: 1.1rem;
		accent-color: var(--accent);
		cursor: pointer;
	}
	.preset-row {
		display: flex;
		gap: 0.5rem;
	}
	.preset {
		flex: 1;
		padding: 0.4rem;
		border-radius: 8px;
		background: #1e293b;
		border: 1px solid #334155;
		color: var(--muted);
		cursor: pointer;
		font-size: 0.85rem;
	}
	.preset.active {
		background: var(--accent);
		color: white;
		border-color: var(--accent);
	}
	.acc-row {
		display: flex;
		gap: 0.4rem;
		flex-wrap: wrap;
	}
	.acc {
		display: flex;
		align-items: center;
		gap: 0.3rem;
		padding: 0.3rem 0.6rem;
		border-radius: 20px;
		background: #0f172a;
		border: 1px solid #334155;
		color: var(--muted);
		font-size: 0.8rem;
		cursor: pointer;
	}
	.acc.on {
		background: #1e3a5f;
		border-color: #3b82f6;
		color: #93c5fd;
	}
</style>
