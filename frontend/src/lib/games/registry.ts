import type { Component } from 'svelte';
import HangmanBoard from './HangmanBoard.svelte';
import Connect4Board from './Connect4Board.svelte';
import MinesweeperBoard from './MinesweeperBoard.svelte';
import QuizBoard from './QuizBoard.svelte';
import BattleshipBoard from './BattleshipBoard.svelte';
import TrisBoard from './TrisBoard.svelte';
import DamaBoard from './DamaBoard.svelte';
import ChessBoard from './ChessBoard.svelte';
import PongBoard from './PongBoard.svelte';
import BattleCityBoard from './BattleCityBoard.svelte';
import PokerBoard from './PokerBoard.svelte';

/** Mappa slug del gioco -> componente tabellone. Aggiungere un gioco = una riga qui. */
export const BOARDS: Record<string, Component<any>> = {
	hangman: HangmanBoard,
	connect4: Connect4Board,
	minesweeper: MinesweeperBoard,
	quiz: QuizBoard,
	battleship: BattleshipBoard,
	tris: TrisBoard,
	dama: DamaBoard,
	chess: ChessBoard,
	pong: PongBoard,
	battlecity: BattleCityBoard,
	poker: PokerBoard
};
