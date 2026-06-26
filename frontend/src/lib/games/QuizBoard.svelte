<script lang="ts">
	import type { BoardProps } from './board';
	import GameResultOverlay from './GameResultOverlay.svelte';

	let { send, event, me }: BoardProps = $props();

	// -------------------------------------------------------------------------
	// Tipi locali
	// -------------------------------------------------------------------------

	type QuizQuestion = {
		text: string;
		category: string;
		options: string[];
	};

	type QuestionState = {
		phase: 'QUESTION';
		qIndex: number;
		total: number;
		question: QuizQuestion;
		answeredEmails: string[];
		playerCount: number;
		scores: Record<string, number>;
	};

	type RevealState = {
		phase: 'REVEAL';
		qIndex: number;
		total: number;
		question: QuizQuestion;
		correctIndex: number;
		answers: Record<string, number>;
		scores: Record<string, number>;
	};

	type GameOverState = {
		ranking: { username: string; score: number }[];
	};

	// -------------------------------------------------------------------------
	// Stato reattivo
	// -------------------------------------------------------------------------

	let gameState = $state<QuestionState | RevealState | null>(null);
	let gameOver = $state<GameOverState | null>(null);
	/** Indice scelto dal giocatore corrente per la domanda attiva. */
	let myAnswer = $state<number | null>(null);

	$effect(() => {
		const e = event;
		if (!e) return;

		if (e.type === 'game:state' && (e as Record<string, unknown>).game === 'quiz') {
			const s = e as unknown as QuestionState | RevealState;
			// Reset myAnswer quando cambia domanda
			if (!gameState || (gameState as QuestionState | RevealState).qIndex !== s.qIndex) {
				myAnswer = null;
			}
			gameState = s;
			gameOver = null;
		} else if (e.type === 'game:over') {
			const o = e as unknown as { ranking: { username: string; score: number }[] };
			gameOver = { ranking: o.ranking };
			gameState = null;
		}
	});

	// -------------------------------------------------------------------------
	// Azioni
	// -------------------------------------------------------------------------

	function startQuiz() {
		send({ type: 'game:start' });
	}

	function submitAnswer(index: number) {
		if (myAnswer !== null) return; // gia' risposto
		myAnswer = index;
		send({ type: 'answer', index });
	}

	function nextQuestion() {
		send({ type: 'next' });
	}

	// -------------------------------------------------------------------------
	// Derived helpers
	// -------------------------------------------------------------------------

	const isQuestion = $derived(gameState?.phase === 'QUESTION');
	const isReveal = $derived(gameState?.phase === 'REVEAL');

	/** In fase REVEAL: indice risposta data dal giocatore corrente. */
	const myRevealAnswer = $derived(
		isReveal && gameState
			? ((gameState as RevealState).answers[me.username] ?? null)
			: null
	);

	/** Punteggi ordinati decrescenti da mostrare nel REVEAL. */
	const sortedScores = $derived(
		gameState
			? Object.entries(gameState.scores)
					.sort(([, a], [, b]) => b - a)
					.map(([username, score]) => ({ username, score }))
			: []
	);
</script>

<div class="quiz">

	<!-- ===== Schermata iniziale ===== -->
	{#if !gameState && !gameOver}
		<div class="start-screen">
			<h2 class="title">Quiz Multiplayer</h2>
			<p class="subtitle">5 domande, cultura generale e programmazione web.</p>
			<button class="btn-primary" onclick={startQuiz}>Inizia quiz</button>
		</div>

	<!-- ===== Schermata fine partita ===== -->
	{:else if gameOver}
		{@const myRank = gameOver.ranking.findIndex((p) => p.username === me.username)}
		<GameResultOverlay
			result={myRank === 0 ? 'win' : 'lose'}
			title={myRank === 0 ? 'PRIMO POSTO' : 'QUIZ FINITO'}
			message={myRank >= 0 ? `Sei arrivato ${myRank + 1}º su ${gameOver.ranking.length}` : ''}
			playAgainLabel="Nuovo quiz"
			onPlayAgain={startQuiz}
		>
			<div class="ranking-table">
				<table>
					<thead>
						<tr><th>#</th><th>Giocatore</th><th>Punti</th></tr>
					</thead>
					<tbody>
						{#each gameOver.ranking as player, i (player.username)}
							<tr class:me={player.username === me.username}>
								<td class="rank">
									{#if i === 0}🥇{:else if i === 1}🥈{:else if i === 2}🥉{:else}{i + 1}{/if}
								</td>
								<td class="email">{player.username}</td>
								<td class="score">{player.score}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
		</GameResultOverlay>

	<!-- ===== Domanda in corso ===== -->
	{:else if isQuestion && gameState}
		{@const qs = gameState as QuestionState}
		<div class="question-screen">
			<div class="question-header">
				<span class="category">{qs.question.category}</span>
				<span class="progress">Domanda {qs.qIndex + 1}/{qs.total}</span>
			</div>

			<div class="progress-bar-wrap">
				<div class="progress-bar" style="width: {((qs.qIndex + 1) / qs.total) * 100}%"></div>
			</div>

			<p class="question-text">{qs.question.text}</p>

			<div class="options">
				{#each qs.question.options as option, i (i)}
					<button
						class="option"
						class:selected={myAnswer === i}
						class:disabled={myAnswer !== null}
						disabled={myAnswer !== null}
						onclick={() => submitAnswer(i)}
					>
						<span class="option-letter">{String.fromCharCode(65 + i)}</span>
						<span class="option-text">{option}</span>
					</button>
				{/each}
			</div>

			{#if myAnswer !== null}
				<p class="waiting">
					In attesa degli altri ({qs.answeredEmails.length}/{qs.playerCount})
				</p>
			{/if}

			<!-- Tabella punteggi laterale -->
			{#if sortedScores.length > 1}
				<div class="scores-mini">
					{#each sortedScores as p (p.username)}
						<div class="score-row" class:me={p.username === me.username}>
							<span class="score-email">{p.username}</span>
							<span class="score-pts">{p.score} pt</span>
						</div>
					{/each}
				</div>
			{/if}
		</div>

	<!-- ===== Fase REVEAL ===== -->
	{:else if isReveal && gameState}
		{@const rs = gameState as RevealState}
		<div class="reveal-screen">
			<div class="question-header">
				<span class="category">{rs.question.category}</span>
				<span class="progress">Domanda {rs.qIndex + 1}/{rs.total}</span>
			</div>

			<p class="question-text">{rs.question.text}</p>

			<div class="options">
				{#each rs.question.options as option, i (i)}
					<button
						class="option"
						class:correct={i === rs.correctIndex}
						class:wrong={i !== rs.correctIndex && myRevealAnswer === i}
						disabled
					>
						<span class="option-letter">{String.fromCharCode(65 + i)}</span>
						<span class="option-text">{option}</span>
						{#if i === rs.correctIndex}
							<span class="badge correct-badge">Corretta</span>
						{:else if myRevealAnswer === i}
							<span class="badge wrong-badge">La tua risposta</span>
						{/if}
					</button>
				{/each}
			</div>

			<div class="scores-table">
				<h3 class="scores-title">Punteggi</h3>
				<table>
					<thead>
						<tr><th>Giocatore</th><th>Punti</th></tr>
					</thead>
					<tbody>
						{#each sortedScores as p (p.username)}
							<tr class:me={p.username === me.username}>
								<td class="email">{p.username}</td>
								<td class="score">{p.score}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>

			<button class="btn-primary" onclick={nextQuestion}>
				{rs.qIndex + 1 < rs.total ? 'Prossima domanda' : 'Vedi classifica'}
			</button>
		</div>
	{/if}
</div>

<style>
	/* ================================================================
	 * QuizBoard — tema Retro/CRT/Synthwave
	 * Usa esclusivamente i token da retro-crt-theme.css via var(...)
	 * ================================================================ */

	.quiz {
		display: flex;
		flex-direction: column;
		align-items: center;
		min-height: 100%;
		padding: 1.5rem 1rem;
		color: var(--text);
		font-family: var(--font-ui, inherit);
		background: var(--bg);
	}

	/* ---- Start screen / Game over ---- */
	.start-screen,
	.over-screen {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1.2rem;
		max-width: 480px;
		width: 100%;
	}

	/* Titolo con font display retro e glow magenta */
	.title {
		font-family: var(--font-display);
		font-size: 1.4rem;
		font-weight: 400;
		margin: 0;
		color: var(--accent);
		text-shadow: var(--glow-mag);
		text-align: center;
		line-height: 1.6;
	}

	.subtitle {
		font-family: var(--font-ui);
		color: var(--muted);
		margin: 0;
		text-align: center;
		font-size: 0.9rem;
	}

	/* ---- Pulsante principale ---- */
	.btn-primary {
		min-height: 44px; /* hit target mobile */
		padding: 0.7rem 1.8rem;
		border: 2px solid var(--accent);
		border-radius: 6px;
		background: transparent;
		color: var(--accent);
		font-family: var(--font-display);
		font-size: 0.75rem;
		cursor: pointer;
		letter-spacing: 0.08em;
		text-transform: uppercase;
		text-shadow: var(--glow-mag);
		box-shadow: 0 0 10px color-mix(in srgb, var(--accent) 30%, transparent);
		transition: background 0.15s, box-shadow 0.15s, color 0.15s;
	}

	.btn-primary:hover,
	.btn-primary:focus-visible {
		background: color-mix(in srgb, var(--accent) 15%, transparent);
		box-shadow: 0 0 20px color-mix(in srgb, var(--accent) 55%, transparent);
		outline: none;
	}

	/* ---- Schermate domanda / reveal ---- */
	.question-screen,
	.reveal-screen {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		width: 100%;
		max-width: 640px;
	}

	.question-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		flex-wrap: wrap;
		gap: 0.4rem;
	}

	/* Chip categoria — fondo cyan con testo scuro */
	.category {
		font-family: var(--font-display);
		font-size: 0.6rem;
		font-weight: 400;
		text-transform: uppercase;
		letter-spacing: 0.08em;
		color: var(--bg);
		background: var(--cyan);
		padding: 0.3rem 0.8rem;
		border-radius: 20px;
		box-shadow: 0 0 8px color-mix(in srgb, var(--cyan) 50%, transparent);
		line-height: 1.8;
	}

	.progress {
		font-family: var(--font-ui);
		font-size: 0.82rem;
		color: var(--muted);
	}

	/* ---- Barra progresso con gradiente neon e glow ---- */
	.progress-bar-wrap {
		width: 100%;
		height: 6px;
		background: var(--inset);
		border-radius: 4px;
		overflow: hidden;
		border: 1px solid var(--line);
	}

	.progress-bar {
		height: 100%;
		background: linear-gradient(90deg, var(--accent), var(--cyan));
		border-radius: 4px;
		box-shadow: 0 0 8px color-mix(in srgb, var(--cyan) 70%, transparent);
		transition: width 0.4s ease;
	}

	/* Disabilita animazione se l'utente preferisce ridotta */
	@media (prefers-reduced-motion: reduce) {
		.progress-bar {
			transition: none;
		}
	}

	/* ---- Card testo domanda su fondo inset ---- */
	.question-text {
		font-family: var(--font-ui);
		font-size: 1.1rem;
		font-weight: 600;
		line-height: 1.6;
		margin: 0;
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 10px;
		padding: 1rem 1.2rem;
		color: var(--text);
	}

	/* ---- Opzioni risposta ---- */
	.options {
		display: flex;
		flex-direction: column;
		gap: 0.65rem;
	}

	.option {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		width: 100%;
		min-height: 44px; /* hit target mobile */
		padding: 0.8rem 1rem;
		border: 2px solid var(--line);
		border-radius: 10px;
		background: var(--panel);
		color: var(--text);
		font-family: var(--font-ui);
		font-size: 0.95rem;
		text-align: left;
		cursor: pointer;
		transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
	}

	.option:not(:disabled):hover {
		border-color: var(--cyan);
		background: color-mix(in srgb, var(--cyan) 8%, var(--panel));
		box-shadow: 0 0 10px color-mix(in srgb, var(--cyan) 25%, transparent);
	}

	/* Opzione selezionata in fase QUESTION */
	.option.selected {
		border-color: var(--accent);
		background: color-mix(in srgb, var(--accent) 15%, var(--panel));
		box-shadow: 0 0 12px color-mix(in srgb, var(--accent) 35%, transparent);
	}

	.option.disabled,
	.option:disabled {
		cursor: default;
		opacity: 0.75;
	}

	/* Risposta corretta — verde neon */
	.option.correct {
		border-color: var(--green);
		background: color-mix(in srgb, var(--green) 12%, var(--inset));
		box-shadow: 0 0 14px color-mix(in srgb, var(--green) 40%, transparent);
		opacity: 1;
	}

	/* Risposta sbagliata — rosso/magenta */
	.option.wrong {
		border-color: var(--danger);
		background: color-mix(in srgb, var(--danger) 12%, var(--inset));
		box-shadow: 0 0 14px color-mix(in srgb, var(--danger) 40%, transparent);
		opacity: 1;
	}

	/* ---- Pasticca/badge lettera opzione ---- */
	.option-letter {
		flex-shrink: 0;
		width: 2rem;
		height: 2rem;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		background: var(--line);
		font-family: var(--font-display); /* 'Press Start 2P' */
		font-size: 0.6rem;
		font-weight: 400;
		color: var(--text);
		letter-spacing: 0;
		line-height: 1;
	}

	.option.correct .option-letter {
		background: var(--green);
		color: var(--bg);
		box-shadow: 0 0 8px color-mix(in srgb, var(--green) 60%, transparent);
	}

	.option.wrong .option-letter {
		background: var(--danger);
		color: var(--bg);
		box-shadow: 0 0 8px color-mix(in srgb, var(--danger) 60%, transparent);
	}

	.option.selected .option-letter {
		background: var(--accent);
		color: var(--bg);
	}

	.option-text {
		flex: 1;
	}

	/* ---- Badge inline (Corretta / La tua risposta) ---- */
	.badge {
		flex-shrink: 0;
		font-family: var(--font-ui);
		font-size: 0.7rem;
		font-weight: 700;
		padding: 0.2rem 0.6rem;
		border-radius: 20px;
		text-transform: uppercase;
		letter-spacing: 0.05em;
	}

	.correct-badge {
		background: color-mix(in srgb, var(--green) 20%, transparent);
		color: var(--green);
		border: 1px solid var(--green);
	}

	.wrong-badge {
		background: color-mix(in srgb, var(--danger) 20%, transparent);
		color: var(--danger);
		border: 1px solid var(--danger);
	}

	/* ---- Attesa altri giocatori ---- */
	.waiting {
		text-align: center;
		color: var(--muted);
		font-family: var(--font-ui);
		font-size: 0.88rem;
		margin: 0;
		animation: pulse 1.5s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50%       { opacity: 0.4; }
	}

	@media (prefers-reduced-motion: reduce) {
		.waiting { animation: none; }
	}

	/* ---- Punteggi mini (fase domanda) ---- */
	.scores-mini {
		display: flex;
		flex-direction: column;
		gap: 0.3rem;
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 10px;
		padding: 0.75rem 1rem;
	}

	.score-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		font-family: var(--font-ui);
		font-size: 0.82rem;
		color: var(--muted);
	}

	.score-row.me {
		color: var(--cyan);
		font-weight: 700;
		text-shadow: var(--glow-cyan);
	}

	.score-email {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		max-width: 70%;
	}

	.score-pts {
		flex-shrink: 0;
	}

	/* ---- Tabella punteggi (reveal / fine partita) ---- */
	.scores-table {
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 10px;
		padding: 1rem;
	}

	.scores-title {
		margin: 0 0 0.6rem;
		font-family: var(--font-display);
		font-size: 0.65rem;
		color: var(--muted);
		text-transform: uppercase;
		letter-spacing: 0.08em;
	}

	.ranking-table {
		width: 100%;
		max-width: 420px;
	}

	table {
		width: 100%;
		border-collapse: collapse;
		font-family: var(--font-ui);
		font-size: 0.92rem;
	}

	thead th {
		text-align: left;
		color: var(--muted);
		font-size: 0.72rem;
		text-transform: uppercase;
		letter-spacing: 0.07em;
		padding: 0 0.5rem 0.5rem;
		border-bottom: 1px solid var(--line);
	}

	tbody td {
		padding: 0.5rem;
		border-bottom: 1px solid var(--line);
	}

	tbody tr:last-child td {
		border-bottom: none;
	}

	tbody tr.me td {
		color: var(--text);
		font-weight: 700;
	}

	.rank {
		width: 2.5rem;
		font-size: 1.1rem;
		text-align: center;
	}

	.email {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		max-width: 260px;
		color: var(--muted);
	}

	tbody tr.me .email {
		color: var(--cyan);
		text-shadow: var(--glow-cyan);
	}

	.score {
		text-align: right;
		font-weight: 700;
		color: var(--amber);
		white-space: nowrap;
	}

	/* ---- Responsive ---- */
	@media (max-width: 480px) {
		.quiz {
			padding: 1rem 0.6rem;
		}

		.title {
			font-size: 1rem;
		}

		.question-text {
			font-size: 0.95rem;
		}

		.option {
			padding: 0.65rem 0.75rem;
			font-size: 0.9rem;
		}

		.option-letter {
			width: 1.7rem;
			height: 1.7rem;
			font-size: 0.55rem;
		}

		.email {
			max-width: 140px;
		}
	}
</style>
