<script lang="ts">
	import type { BoardProps } from './board';

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
		<div class="over-screen">
			<h2 class="title">Partita terminata!</h2>
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
			<button class="btn-primary" onclick={startQuiz}>Nuovo quiz</button>
		</div>

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
	:root {
		--bg: #0f172a;
		--panel: #1e293b;
		--accent: #6366f1;
		--text: #e2e8f0;
		--muted: #94a3b8;
	}

	.quiz {
		display: flex;
		flex-direction: column;
		align-items: center;
		min-height: 100%;
		padding: 1.5rem 1rem;
		color: var(--text);
		font-family: inherit;
	}

	/* ---- Start screen ---- */
	.start-screen,
	.over-screen {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1.2rem;
		max-width: 480px;
		width: 100%;
	}

	.title {
		font-size: 1.8rem;
		font-weight: 700;
		margin: 0;
		color: var(--text);
	}

	.subtitle {
		color: var(--muted);
		margin: 0;
		text-align: center;
	}

	/* ---- Buttons ---- */
	.btn-primary {
		padding: 0.7rem 1.6rem;
		border: none;
		border-radius: 10px;
		background: var(--accent);
		color: #fff;
		font-size: 1rem;
		font-weight: 600;
		cursor: pointer;
		transition: opacity 0.15s;
	}

	.btn-primary:hover {
		opacity: 0.88;
	}

	/* ---- Question / Reveal shared ---- */
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

	.category {
		font-size: 0.8rem;
		font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.06em;
		color: var(--accent);
		background: rgba(99, 102, 241, 0.15);
		padding: 0.2rem 0.6rem;
		border-radius: 20px;
	}

	.progress {
		font-size: 0.85rem;
		color: var(--muted);
	}

	.progress-bar-wrap {
		width: 100%;
		height: 4px;
		background: var(--panel);
		border-radius: 4px;
		overflow: hidden;
	}

	.progress-bar {
		height: 100%;
		background: var(--accent);
		border-radius: 4px;
		transition: width 0.4s ease;
	}

	.question-text {
		font-size: 1.15rem;
		font-weight: 600;
		line-height: 1.5;
		margin: 0;
		background: var(--panel);
		padding: 1rem 1.2rem;
		border-radius: 12px;
	}

	/* ---- Options ---- */
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
		padding: 0.8rem 1rem;
		border: 2px solid #334155;
		border-radius: 12px;
		background: var(--panel);
		color: var(--text);
		font-size: 1rem;
		text-align: left;
		cursor: pointer;
		transition: border-color 0.15s, background 0.15s;
	}

	.option:not(:disabled):hover {
		border-color: var(--accent);
		background: rgba(99, 102, 241, 0.1);
	}

	.option.selected {
		border-color: var(--accent);
		background: rgba(99, 102, 241, 0.2);
	}

	.option.disabled,
	.option:disabled {
		cursor: default;
		opacity: 0.75;
	}

	.option.correct {
		border-color: #16a34a;
		background: #14532d;
		opacity: 1;
	}

	.option.wrong {
		border-color: #dc2626;
		background: #7f1d1d;
		opacity: 1;
	}

	.option-letter {
		flex-shrink: 0;
		width: 1.8rem;
		height: 1.8rem;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		background: #334155;
		font-size: 0.85rem;
		font-weight: 700;
	}

	.option.correct .option-letter {
		background: #16a34a;
	}

	.option.wrong .option-letter {
		background: #dc2626;
	}

	.option-text {
		flex: 1;
	}

	.badge {
		flex-shrink: 0;
		font-size: 0.75rem;
		font-weight: 600;
		padding: 0.15rem 0.5rem;
		border-radius: 20px;
	}

	.correct-badge {
		background: #16a34a;
		color: #bbf7d0;
	}

	.wrong-badge {
		background: #dc2626;
		color: #fecaca;
	}

	/* ---- Waiting ---- */
	.waiting {
		text-align: center;
		color: var(--muted);
		font-size: 0.9rem;
		margin: 0;
		animation: pulse 1.5s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.5; }
	}

	/* ---- Scores mini (question phase) ---- */
	.scores-mini {
		display: flex;
		flex-direction: column;
		gap: 0.3rem;
		background: var(--panel);
		border-radius: 10px;
		padding: 0.75rem 1rem;
	}

	.score-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		font-size: 0.85rem;
		color: var(--muted);
	}

	.score-row.me {
		color: var(--text);
		font-weight: 600;
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

	/* ---- Scores table (reveal / game over) ---- */
	.scores-table {
		background: var(--panel);
		border-radius: 12px;
		padding: 1rem;
	}

	.scores-title {
		margin: 0 0 0.6rem;
		font-size: 0.9rem;
		color: var(--muted);
		text-transform: uppercase;
		letter-spacing: 0.05em;
	}

	.ranking-table {
		width: 100%;
		max-width: 420px;
	}

	table {
		width: 100%;
		border-collapse: collapse;
		font-size: 0.95rem;
	}

	thead th {
		text-align: left;
		color: var(--muted);
		font-size: 0.8rem;
		text-transform: uppercase;
		letter-spacing: 0.05em;
		padding: 0 0.5rem 0.5rem;
		border-bottom: 1px solid #334155;
	}

	tbody td {
		padding: 0.5rem;
		border-bottom: 1px solid #1e293b;
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
		color: var(--text);
	}

	.score {
		text-align: right;
		font-weight: 700;
		color: var(--accent);
		white-space: nowrap;
	}

	/* ---- Responsive ---- */
	@media (max-width: 480px) {
		.quiz {
			padding: 1rem 0.6rem;
		}

		.title {
			font-size: 1.4rem;
		}

		.question-text {
			font-size: 1rem;
		}

		.option {
			padding: 0.65rem 0.75rem;
			font-size: 0.95rem;
		}

		.option-letter {
			width: 1.5rem;
			height: 1.5rem;
			font-size: 0.8rem;
		}

		.email {
			max-width: 140px;
		}
	}
</style>
