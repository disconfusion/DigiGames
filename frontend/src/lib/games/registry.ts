import type { Component } from 'svelte';
import HangmanBoard from './HangmanBoard.svelte';
import Connect4Board from './Connect4Board.svelte';
import MinesweeperBoard from './MinesweeperBoard.svelte';
import QuizBoard from './QuizBoard.svelte';
import BattleshipBoard from './BattleshipBoard.svelte';

/** Mappa slug del gioco -> componente tabellone. Aggiungere un gioco = una riga qui. */
export const BOARDS: Record<string, Component<any>> = {
	hangman: HangmanBoard,
	connect4: Connect4Board,
	minesweeper: MinesweeperBoard,
	quiz: QuizBoard,
	battleship: BattleshipBoard
};
