export type HangmanGameState = {
	masked: string;
	wrong: string[];
	guessed: string[];
	wrongCount: number;
	maxWrong: number;
	status: 'PLAYING' | 'WON' | 'LOST';
	lastBy?: string;
	lastLetter?: string;
};
