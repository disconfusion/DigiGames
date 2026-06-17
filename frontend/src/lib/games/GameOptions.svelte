<script lang="ts">
	import { ACCESSORIES, CLASSIC_OPTIONS, type HangmanOptions } from '$lib/games/hangman';

	let { game, options = $bindable() }: { game: string; options: unknown } = $props();

	let quizSource = $state<'local' | 'opentdb' | 'mixed'>('local');
	let chessMinutes = $state(0);
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

	// Mantiene `options` allineato al gioco selezionato e ai valori scelti.
	$effect(() => {
		if (game === 'hangman') {
			options = {
				maxVowels: hmOptions.maxVowels,
				lettersPerPlayer: hmOptions.lettersPerPlayer,
				accessories: [...hmOptions.accessories]
			};
		} else if (game === 'quiz') {
			options = { source: quizSource };
		} else if (game === 'chess') {
			options = { minutesPerPlayer: chessMinutes };
		} else {
			options = undefined;
		}
	});

	const hasOptions = $derived(game === 'hangman' || game === 'quiz' || game === 'chess');
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
		{:else if game === 'hangman'}
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
							{a.emoji} {a.label}
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
