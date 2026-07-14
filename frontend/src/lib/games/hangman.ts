export type HangmanGameState = {
	masked: string;
	wrong: string[];
	guessed: string[];
	wrongCount: number;
	maxWrong: number;
	accessories: string[];
	maxVowels: number;
	vowelsCalled: number;
	lettersPerPlayer: number;
	lettersUsed: Record<string, number>;
	eliminated: string[];
	winner: string | null;
	status: 'PLAYING' | 'WON' | 'LOST';
	currentTurn: string | null;
	players?: string[];
	lastBy?: string;
	lastLetter?: string;
};

export type HangmanOptions = {
	maxVowels: number; // 0 = illimitate
	lettersPerPlayer: number; // 0 = illimitati
	accessories: string[]; // "hat" | "pipe" | "shoes"
};

export const CLASSIC_OPTIONS: HangmanOptions = {
	maxVowels: 2,
	lettersPerPlayer: 0,
	accessories: []
};

export const ACCESSORIES: { key: string; label: string }[] = [
	{ key: 'hat', label: 'Cappello' },
	{ key: 'pipe', label: 'Pipa' },
	{ key: 'shoes', label: 'Scarpe' }
];

/**
 * Disegna il patibolo in ASCII in base agli errori. Le prime 6 tacche sono le parti del
 * corpo; ogni accessorio selezionato occupa una tacca successiva (hat → pipe → shoes) e
 * viene disegnato sulla figura.
 */
export function gallows(wrongCount: number, accessories: string[], lost: boolean): string {
	// Quante tacche mostrare (se perso, mostra tutto)
	const total = 6 + accessories.length;
	const shown = lost ? total : Math.min(wrongCount, total);

	const head = shown >= 1;
	const body = shown >= 2;
	const armL = shown >= 3;
	const armR = shown >= 4;
	const legL = shown >= 5;
	const legR = shown >= 6;
	// Accessori: tacche dopo la 6ª, nell'ordine in cui sono in `accessories`
	const accShown = new Set<string>();
	accessories.forEach((a, i) => {
		if (shown >= 7 + i) accShown.add(a);
	});

	const hat = accShown.has('hat');
	const pipe = accShown.has('pipe');
	const shoes = accShown.has('shoes');

	// Griglia 7 righe (col0..2 = figura, col5 = montante)
	const rows = [
		' +---+ '.split(''),
		' |   | '.split(''),
		'     | '.split(''),
		'     | '.split(''),
		'     | '.split(''),
		'     | '.split(''),
		'======='.split('')
	];

	// Cappello sopra la testa (riga 1) — solo la tesa: NON sovrascrive la corda (col1)
	if (hat) {
		rows[1][0] = '_';
		rows[1][2] = '_';
	}
	// Testa (riga 2)
	if (head) {
		rows[2][1] = 'O';
		// Pipa accanto al viso
		if (pipe) {
			rows[2][2] = '-';
			rows[2][3] = 'o';
		}
	}
	// Corpo + braccia (riga 3)
	if (armL) rows[3][0] = '/';
	if (body) rows[3][1] = '|';
	if (armR) rows[3][2] = '\\';
	// Gambe (riga 4)
	if (legL) rows[4][0] = '/';
	if (legR) rows[4][2] = '\\';
	// Scarpe sotto i piedi (riga 5)
	if (shoes) {
		rows[5][0] = 'L';
		rows[5][2] = 'J';
	}

	return rows.map((r) => r.join('').replace(/\s+$/, '')).join('\n');
}
