/** Tipi e utilità condivise del poker (Texas Hold'em). */

/** Una carta è l'intero 0..51 che manda il server: rango = c / 4, seme = c % 4. */
export type Card = number;

const RANK_LABELS = ['2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A'];
/** Ordine dei semi come nel backend: picche, cuori, quadri, fiori. */
const SUIT_SPRITES = ['suit-spade', 'suit-heart', 'suit-diamond', 'suit-club'];

export const cardRank = (c: Card): number => Math.floor(c / 4);
export const cardSuit = (c: Card): number => c % 4;
export const rankLabel = (c: Card): string => RANK_LABELS[cardRank(c)] ?? '?';
export const suitSprite = (c: Card): string => SUIT_SPRITES[cardSuit(c)] ?? 'suit-spade';
/** Cuori e quadri: cambiano il colore del rango sulla carta. */
export const isRedSuit = (c: Card): boolean => cardSuit(c) === 1 || cardSuit(c) === 2;

export type PokerMove = 'fold' | 'check' | 'call' | 'raise';

export type PokerSeat = {
	id: string;
	name: string;
	bot: boolean;
	chips: number;
	bet: number;
	committed: number;
	folded: boolean;
	allIn: boolean;
	out: boolean;
	dealer: boolean;
	you: boolean;
	/** Carte visibili: le proprie, o quelle mostrate allo showdown. Altrimenti null. */
	cards: Card[] | null;
	/** Ha carte in mano (per disegnare il dorso quando non sono visibili). */
	hasCards: boolean;
};

export type PokerAward = {
	id: string;
	name: string;
	amount: number;
	hand: string | null;
	five: Card[];
};

export type PokerHandResult = {
	handNo: number;
	pot: number;
	showdown: boolean;
	awards: PokerAward[];
	busted: string[];
};

export type PokerGameState = {
	status: 'PLAYING' | 'OVER';
	phase: 'BETTING' | 'HAND_OVER';
	street: 'PREFLOP' | 'FLOP' | 'TURN' | 'RIVER';
	handNo: number;
	pot: number;
	currentBet: number;
	smallBlind: number;
	bigBlind: number;
	board: Card[];
	dealer: string | null;
	actor: string | null;
	/** Millisecondi residui del turno in corso (0 = nessun timer attivo). */
	turnMs: number;
	buyin: number;
	pool: number;
	startingChips: number;
	botLevel: string;
	seats: PokerSeat[];
	log: string[];
	lastHand?: PokerHandResult;
	you?: {
		seat: string;
		chips: number;
		cards: Card[];
		yourTurn: boolean;
		legal: PokerMove[];
		call: number;
		minRaiseTo: number;
		maxRaiseTo: number;
	};
};

export type PokerOver = {
	status: 'WON' | 'LOST';
	winner: string | null;
	winnerName: string | null;
	ranking: { place: number; id: string; name: string; bot: boolean; chips: number }[];
	buyin: number;
	pool: number;
	tokensWon: number;
	tokensLost: number;
};

export type PokerOptions = {
	/** Posti riservati ai giocatori umani: è anche il tetto di ingressi della stanza (1..6). */
	humanSeats: number;
	/** Avversari IA al tavolo (0..5). Umani + IA non superano i 6 posti. */
	botSeats: number;
	/** Buyin in Token: 0 = partita amichevole, si giocano solo fiches. */
	buyin: number;
	/** Fiches consegnate a ogni giocatore all'inizio. */
	startingChips: number;
	botLevel: 'facile' | 'normale' | 'tosta';
};

export const DEFAULT_POKER_OPTIONS: PokerOptions = {
	humanSeats: 2,
	botSeats: 2,
	buyin: 0,
	startingChips: 1000,
	botLevel: 'normale'
};

export const STREET_LABELS: Record<string, string> = {
	PREFLOP: 'Pre-flop',
	FLOP: 'Flop',
	TURN: 'Turn',
	RIVER: 'River'
};

export const BOT_LEVEL_LABELS: Record<string, string> = {
	facile: 'Facile',
	normale: 'Normale',
	tosta: 'Tosta'
};
