import { C, type Palette } from './pixel';

export interface Sprite {
	frames: string[][];
	palette: Palette;
	grid?: number;
	fps?: number;
	motion?: string;
}

/* =============================================================================
   Sprite pixel-art (16×16). Disegnate a mano: ' '/'.' = trasparente, ogni
   altro carattere è una chiave nella palette. I colori usano i token del tema
   CRT, così le icone seguono il neon di tutto il sito.
============================================================================= */

// ── Forza 4 ──────────────────────────────────────────────────────────────
const connect4: Sprite = {
	motion: 'bob',
	palette: { P: '#2b6bff', R: C.danger, Y: C.amber },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'..PPPPPPPPPPPP..',
		'..PRRYYRRYYRRP..',
		'..PYYRRYYRRYYP..',
		'..PRRYYRRYYRRP..',
		'..PYYRRYYRRYYP..',
		'..PPPPPPPPPPPP..',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Impiccato ────────────────────────────────────────────────────────────
const hangman: Sprite = {
	motion: 'swing',
	palette: { W: C.amber, R: C.text, F: C.cyan },
	frames: [[
		'..WWWWWWW.......',
		'..W....R........',
		'..W....R........',
		'..W...FFF.......',
		'..W...FFF.......',
		'..W....F........',
		'..W..FFFFF......',
		'..W....F........',
		'..W...F.F.......',
		'..W..F...F......',
		'..W.............',
		'..W.............',
		'..WWWWW.........',
		'................',
		'................',
		'................'
	]]
};

// ── Quiz ───────────────────────────────────────────────────────────────────
const quiz: Sprite = {
	motion: 'bob',
	palette: { Q: C.amber },
	frames: [[
		'................',
		'....QQQQQQ......',
		'...QQ....QQ.....',
		'...QQ....QQ.....',
		'.........QQ.....',
		'........QQ......',
		'.......QQ.......',
		'......QQ........',
		'......QQ........',
		'......QQ........',
		'................',
		'......QQ........',
		'......QQ........',
		'................',
		'................',
		'................'
	]]
};

// ── Battaglia navale ───────────────────────────────────────────────────────
const battleship: Sprite = {
	motion: 'bob',
	palette: { F: C.danger, L: C.muted, C: C.text, H: C.muted, G: C.amber, B: C.cyan },
	frames: [[
		'................',
		'.......F........',
		'.......L........',
		'.....CCCCC......',
		'.....CCCCC......',
		'..GHHHHHHHHHG...',
		'..HHHHHHHHHHHH..',
		'...HHHHHHHHHH...',
		'....HHHHHHHH....',
		'................',
		'.BB..BB..BB..BB.',
		'..BB..BB..BB..B.',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Campo minato ─────────────────────────────────────────────────────────
// 2 fotogrammi: la miccia sfrigola (scintilla che cambia).
const minesweeper: Sprite = {
	motion: '',
	fps: 5,
	palette: { K: '#15152b', W: '#5a5a8a', F: C.amber, R: C.danger, A: C.amber },
	frames: [
		[
			'............R...',
			'...........F....',
			'..........F.....',
			'.........F......',
			'.....KKKKK......',
			'...KKKKKKKKK....',
			'..KKKKKKKKKKK...',
			'..KKWWKKKKKKK...',
			'..KKWKKKKKKKK...',
			'..KKKKKKKKKKK...',
			'...KKKKKKKKK....',
			'.....KKKKK......',
			'................',
			'................',
			'................',
			'................'
		],
		[
			'...........AAA..',
			'...........RR...',
			'..........F.....',
			'.........F......',
			'.....KKKKK......',
			'...KKKKKKKKK....',
			'..KKKKKKKKKKK...',
			'..KKWWKKKKKKK...',
			'..KKWKKKKKKKK...',
			'..KKKKKKKKKKK...',
			'...KKKKKKKKK....',
			'.....KKKKK......',
			'................',
			'................',
			'................',
			'................'
		]
	]
};

// ── Tris ───────────────────────────────────────────────────────────────────
const tris: Sprite = {
	motion: 'bob',
	palette: { G: C.muted, X: C.cyan, O: C.mag },
	frames: [[
		'................',
		'.X..XG....G.....',
		'..XX.G....G.....',
		'..XX.G....G.....',
		'.X..XG....G.....',
		'.GGGGGGGGGGGGGG.',
		'.....G.OO.G.....',
		'.....GO..OG.....',
		'.....GO..OG.....',
		'.....G.OO.G.....',
		'.GGGGGGGGGGGGGG.',
		'.....G....GX..X.',
		'.....G....G.XX..',
		'.....G....G.XX..',
		'.....G....GX..X.',
		'................'
	]]
};

// ── Dama ───────────────────────────────────────────────────────────────────
const dama: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan, M: C.mag, A: C.amber },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'.........A.A.A..',
		'.........AAAAAA.',
		'..CCCC....MMMM..',
		'.CCCCCC..MMMMMM.',
		'.CCCCCC..MMMMMM.',
		'..CCCC....MMMM..',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Scacchi (re con croce) ─────────────────────────────────────────────────
const chess: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan },
	frames: [[
		'.......CC.......',
		'.....CCCCCC.....',
		'.......CC.......',
		'......CCCC......',
		'.....CCCCCC.....',
		'.....CCCCCC.....',
		'......CCCC......',
		'.....CCCCCC.....',
		'.....CCCCCC.....',
		'......CCCC......',
		'.....CCCCCC.....',
		'....CCCCCCCC....',
		'...CCCCCCCCCC...',
		'................',
		'................',
		'................'
	]]
};

// ── Esiti partita ──────────────────────────────────────────────────────────
const win: Sprite = {
	motion: 'bob',
	palette: { A: C.amber, S: '#c9991f', W: '#fff6cf' },
	frames: [[
		'................',
		'..AAAAAAAAAA....',
		'.A.AWWWWWWA.A...',
		'.A.ASAAAASA.A...',
		'.A.ASAAAASA.A...',
		'..A.SAAAAAS.A...',
		'....SAAAAS......',
		'.....SAAS.......',
		'......AA........',
		'......AA........',
		'......AA........',
		'.....SAAS.......',
		'....AAAAAA......',
		'...AAAAAAAA.....',
		'................',
		'................'
	]]
};

const lose: Sprite = {
	motion: 'shake',
	fps: 2,
	palette: { B: C.text, K: '#0a0512', R: C.danger },
	frames: [
		[
			'................',
			'....BBBBBBBB....',
			'...BBBBBBBBBB...',
			'..BBBBBBBBBBBB..',
			'..BBBBBBBBBBBB..',
			'..BKKBBBBBBKKB..',
			'..BKKBBBBBBKKB..',
			'..BBBBBBBBBBBB..',
			'...BBBBKKBBBB...',
			'...BBBBBBBBBB...',
			'....BBBBBBBB....',
			'....BB.BB.BB....',
			'................',
			'................',
			'................',
			'................'
		],
		[
			'................',
			'....BBBBBBBB....',
			'...BBBBBBBBBB...',
			'..BBBBBBBBBBBB..',
			'..BBBBBBBBBBBB..',
			'..BRRBBBBBBRRB..',
			'..BRRBBBBBBRRB..',
			'..BBBBBBBBBBBB..',
			'...BBBBKKBBBB...',
			'...BBBBBBBBBB...',
			'....BBBBBBBB....',
			'....BB.BB.BB....',
			'................',
			'................',
			'................',
			'................'
		]
	]
};

const draw: Sprite = {
	motion: 'pulse',
	palette: { C: C.cyan, M: C.mag },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'..CCCC....MMMM..',
		'.CCCCCC..MMMMMM.',
		'.CCCCCCCMMMMMMM.',
		'.CCCCCCCMMMMMMM.',
		'.CCCCCC..MMMMMM.',
		'..CCCC....MMMM..',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Generico (area "Generale/Altro" nella segnalazione bug) ────────────────
const generale: Sprite = {
	motion: 'spin',
	palette: { G: C.muted, M: C.inset },
	frames: [[
		'................',
		'................',
		'.....G..G.......',
		'.....GGGG.......',
		'..G..GGGG..G....',
		'.GGGGGGGGGGGG...',
		'.GGGG.MM.GGGG...',
		'.GGGG.MM.GGGG...',
		'.GGGGGGGGGGGG...',
		'..G..GGGG..G....',
		'.....GGGG.......',
		'.....G..G.......',
		'................',
		'................',
		'................',
		'................'
	]]
};

export const SPRITES: Record<string, Sprite> = {
	// giochi (le chiavi coincidono con gli slug del catalogo)
	connect4,
	hangman,
	quiz,
	battleship,
	minesweeper,
	tris,
	dama,
	chess,
	// esiti
	win,
	lose,
	draw,
	// vari
	generale
};
