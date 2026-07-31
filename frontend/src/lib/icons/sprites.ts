import { C, type Palette } from './pixel';

export interface Sprite {
	frames: string[][];
	palette: Palette;
	grid?: number;
	fps?: number;
	motion?: string;
	/** Chiavi della palette ricolorabili dall'utente (vedi prop `tint` di <Icon>). */
	tintKeys?: string[];
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

// ── Chrome / navigazione ───────────────────────────────────────────────────
const gamepad: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan, R: C.danger, G: C.green, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'..CCCCCCCCCCCC..',
		'.CCKCCCCCCCGCCC.',
		'.CKKKCCCCCCCRCC.',
		'.CCKCCCCCCCGCCC.',
		'..CCCCCCCCCCCC..',
		'..CC......CC....',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const coin: Sprite = {
	motion: 'bob',
	palette: { A: C.amber, K: '#7a5a10' },
	frames: [[
		'................',
		'................',
		'....AAAAAAAA....',
		'..AAAAAAAAAAAA..',
		'.AAAAAAAAAAAAAA.',
		'.AAAKKKKKKKKAAA.',
		'.AAAAAAKKAAAAAA.',
		'.AAAAAAKKAAAAAA.',
		'.AAAAAAKKAAAAAA.',
		'.AAAAAAKKAAAAAA.',
		'.AAAAAAAAAAAAAA.',
		'..AAAAAAAAAAAA..',
		'....AAAAAAAA....',
		'................',
		'................',
		'................'
	]]
};

const tools: Sprite = {
	motion: 'swing',
	palette: { H: C.muted },
	frames: [[
		'................',
		'............HH..',
		'...........H.H..',
		'...........HH...',
		'..........HH....',
		'.........HH.....',
		'........HH......',
		'.......HH.......',
		'......HH........',
		'.....HH.........',
		'....HHH.........',
		'...HHH..........',
		'...HH...........',
		'................',
		'................',
		'................'
	]]
};

const bug: Sprite = {
	motion: 'float',
	palette: { R: C.danger, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'......KKKK......',
		'.....RRKKRR.....',
		'....RRRKKRRR....',
		'...RRKRKKRKRR...',
		'..RRRRRKKRRRRR..',
		'..RRKRRKKRRKRR..',
		'..RRRRRKKRRRRR..',
		'...RRRRKKRRRR...',
		'....RRRKKRRR....',
		'.....RRRRRR.....',
		'................',
		'................',
		'................',
		'................'
	]]
};

const mail: Sprite = {
	motion: 'bob',
	palette: { W: C.text, C: C.cyan },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'..WWWWWWWWWWWW..',
		'..WCWWWWWWWWCW..',
		'..WWCWWWWWWCWW..',
		'..WWWCWWWWCWWW..',
		'..WWWWCWWCWWWW..',
		'..WWWWWCCWWWWW..',
		'..WWWWWWWWWWWW..',
		'..WWWWWWWWWWWW..',
		'..WWWWWWWWWWWW..',
		'................',
		'................',
		'................'
	]]
};

const rocket: Sprite = {
	motion: 'float',
	palette: { W: C.text, C: C.cyan, R: C.danger, A: C.amber },
	frames: [[
		'................',
		'......WW........',
		'.....WWWW.......',
		'.....WCCW.......',
		'.....WCCW.......',
		'.....WWWW.......',
		'.....WWWW.......',
		'....WWWWWW......',
		'...W.WWWW.W.....',
		'.....WWWW.......',
		'......RR........',
		'.....RAAR.......',
		'......AA........',
		'................',
		'................',
		'................'
	]]
};

const house: Sprite = {
	motion: 'bob',
	palette: { R: C.danger, W: C.text, C: C.cyan, A: C.amber },
	frames: [[
		'................',
		'................',
		'.......RR.......',
		'......RRRR......',
		'.....RRRRRR.....',
		'....RRRRRRRR....',
		'...RRRRRRRRRR...',
		'..RRRRRRRRRRRR..',
		'...WWWWWWWWWW...',
		'...WWCCWWWWWW...',
		'...WWCCWWWWWW...',
		'...WWWWAAWWWW...',
		'...WWWWAAWWWW...',
		'...WWWWAAWWWW...',
		'................',
		'................'
	]]
};

const calendar: Sprite = {
	motion: 'bob',
	palette: { W: C.text, R: C.danger, C: C.cyan },
	frames: [[
		'................',
		'................',
		'....W....W......',
		'....W....W......',
		'..RRRRRRRRRRRR..',
		'..RRRRRRRRRRRR..',
		'..WWWWWWWWWWWW..',
		'..WWWWWWWWWWWW..',
		'..WWWCCCCCCWWW..',
		'..WWWCCCCCCWWW..',
		'..WWWWWWWWWWWW..',
		'..WWWWWWWWWWWW..',
		'................',
		'................',
		'................',
		'................'
	]]
};

const map: Sprite = {
	motion: 'bob',
	palette: { G: C.green, C: C.cyan, A: C.amber },
	frames: [[
		'................',
		'................',
		'................',
		'..GGGGGGGGGGGG..',
		'..GCGGGGGGGGGG..',
		'..GCGGGGGGGGGG..',
		'..GGCGGGGGGGGG..',
		'..GGCGGGGGGGGG..',
		'..GGGCCGGGGGGG..',
		'..GGGGGCCGGAGG..',
		'..GGGGGGGCCAGG..',
		'..GGGGGGGGGAGG..',
		'..GGGGGGGGGGGG..',
		'................',
		'................',
		'................'
	]]
};

const people: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan, M: C.mag },
	frames: [[
		'................',
		'................',
		'................',
		'....CC....MM....',
		'...CCCC..MMMM...',
		'...CCCC..MMMM...',
		'....CC....MM....',
		'..CCCCCCMMMMMM..',
		'.CCCCCCCMMMMMMM.',
		'.CCCCCCCMMMMMMM.',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const person: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan },
	frames: [[
		'................',
		'................',
		'......CCCC......',
		'.....CCCCCC.....',
		'.....CCCCCC.....',
		'......CCCC......',
		'....CCCCCCCC....',
		'..CCCCCCCCCCCC..',
		'.CCCCCCCCCCCCCC.',
		'.CCCCCCCCCCCCCC.',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const smiley: Sprite = {
	motion: 'bob',
	palette: { A: C.amber, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'....AAAAAAAA....',
		'..AAAAAAAAAAAA..',
		'.AAAAAAAAAAAAAA.',
		'.AAKKAAAAKKAAAA.',
		'.AAKKAAAAKKAAAA.',
		'.AAAAAAAAAAAAAA.',
		'.AAKAAAAAAAAKAA.',
		'.AAKKAAAAAAKKAA.',
		'.AAAKKKKKKKKAAA.',
		'..AAAAAAAAAAAA..',
		'....AAAAAAAA....',
		'................',
		'................',
		'................'
	]]
};

const lock: Sprite = {
	motion: 'bob',
	palette: { A: C.amber, S: C.muted, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'.....SSSS.......',
		'....S....S......',
		'....S....S......',
		'..AAAAAAAAAA....',
		'..AAAAAAAAAA....',
		'..AAAAKKAAAA....',
		'..AAAKKKKAAA....',
		'..AAAAKKAAAA....',
		'..AAAAAAAAAA....',
		'..AAAAAAAAAA....',
		'................',
		'................',
		'................',
		'................'
	]]
};

const chart: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan, A: C.amber, G: C.green, M: C.mag, L: C.muted },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'........GG......',
		'........GG......',
		'.....AA.GG......',
		'.....AA.GG.MM...',
		'..CC.AA.GG.MM...',
		'..CC.AA.GG.MM...',
		'..CC.AA.GG.MM...',
		'..CC.AA.GG.MM...',
		'.LLLLLLLLLLLLLL.',
		'................',
		'................'
	]]
};

const cart: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan, A: C.amber },
	frames: [[
		'................',
		'................',
		'.CC.............',
		'...C............',
		'...C............',
		'..CCCCCCCCCCC...',
		'..C.AAAAAAAA.C..',
		'..C.AAAAAAAA.C..',
		'..C.AAAAAAAA.C..',
		'..CCCCCCCCCCC...',
		'...C.......C....',
		'..CCC.....CCC...',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Parola del Giorno / esiti vari ──────────────────────────────────────────
const book: Sprite = {
	motion: 'bob',
	palette: { W: C.text, C: C.cyan },
	frames: [[
		'................',
		'................',
		'................',
		'..WWWWW..WWWWW..',
		'.WWWWWW..WWWWWW.',
		'.WCCCW....WCCCW.',
		'.WCCCW....WCCCW.',
		'.WCCCW....WCCCW.',
		'.WCCCW....WCCCW.',
		'.WWWWWW..WWWWWW.',
		'..WWWWW..WWWWW..',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const globe: Sprite = {
	motion: 'float',
	palette: { C: C.cyan, G: C.green },
	frames: [[
		'................',
		'................',
		'................',
		'....CCCCCC......',
		'..CCCCCCCCCC....',
		'.CCCGGCCCCCC....',
		'.CCGGGCCCGGCC...',
		'.CCCCCCCGGGCC...',
		'.CCGGCCCCCCCC...',
		'.CCGGGCCCGGCC...',
		'..CCCCCCCCCC....',
		'....CCCCCC......',
		'................',
		'................',
		'................',
		'................'
	]]
};

const letters: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan, A: C.amber },
	frames: [[
		'................',
		'................',
		'..CCCCCCCCCC....',
		'..C........C....',
		'..C..AAAA..C....',
		'..C.AA..AA.C....',
		'..C.AA..AA.C....',
		'..C.AAAAAA.C....',
		'..C.AA..AA.C....',
		'..C.AA..AA.C....',
		'..C........C....',
		'..CCCCCCCCCC....',
		'................',
		'................',
		'................',
		'................'
	]]
};

const party: Sprite = {
	motion: 'pulse',
	palette: { A: C.amber, M: C.mag, C: C.cyan, G: C.green },
	frames: [[
		'................',
		'............M...',
		'.........C......',
		'...........G....',
		'.......M........',
		'....AAAA...C....',
		'...AAAAA...G....',
		'..AAAAA.........',
		'.AAAA...........',
		'.AAA............',
		'.AA.............',
		'.A..............',
		'................',
		'................',
		'................',
		'................'
	]]
};

const sync: Sprite = {
	motion: 'spin',
	palette: { C: C.cyan },
	frames: [[
		'................',
		'................',
		'.....CCCCC......',
		'...CCC...CCC....',
		'..CC.......CC...',
		'..C.........C...',
		'..C.........C.C.',
		'..C.........CCC.',
		'..C..........CC.',
		'..CC.......CC...',
		'...CCC...CCC....',
		'.....CCCCC......',
		'................',
		'................',
		'................',
		'................'
	]]
};

const clock: Sprite = {
	motion: 'bob',
	palette: { W: C.text, C: C.cyan, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'.....CCCC.......',
		'...CCWWWWCC.....',
		'..CWWWWWWWWC....',
		'.CWWWWKWWWWWC...',
		'.CWWWWKWWWWWC...',
		'CWWWWWKWWWWWWC..',
		'CWWWWWKKKWWWWC..',
		'CWWWWWWWWWWWWC..',
		'.CWWWWWWWWWWC...',
		'..CWWWWWWWWC....',
		'...CCWWWWCC.....',
		'.....CCCC.......',
		'................',
		'................'
	]]
};

const speech: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'................',
		'.CCCCCCCCCCCC...',
		'.CCCCCCCCCCCC...',
		'.CKKCCKKCCKKC...',
		'.CKKCCKKCCKKC...',
		'.CCCCCCCCCCCC...',
		'.CCCCCCCCCCCC...',
		'..CCC...........',
		'..CC............',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const check: Sprite = {
	motion: '',
	palette: { G: C.green },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'.............GG.',
		'............GG..',
		'...........GG...',
		'..GG......GG....',
		'...GG....GG.....',
		'....GG..GG......',
		'.....GGGG.......',
		'......GG........',
		'................',
		'................',
		'................',
		'................'
	]]
};

const cross: Sprite = {
	motion: '',
	palette: { R: C.danger },
	frames: [[
		'................',
		'................',
		'................',
		'..RR......RR....',
		'...RR....RR.....',
		'....RR..RR......',
		'.....RRRR.......',
		'......RR........',
		'.....RRRR.......',
		'....RR..RR......',
		'...RR....RR.....',
		'..RR......RR....',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Frecce selettore (chevron) ───────────────────────────────────────────────
const arrow_right: Sprite = {
	motion: '',
	palette: { A: C.cyan },
	frames: [[
		'................',
		'................',
		'....AA..........',
		'.....AA.........',
		'......AA........',
		'.......AA.......',
		'........AA......',
		'.........AA.....',
		'.........AA.....',
		'........AA......',
		'.......AA.......',
		'......AA........',
		'.....AA.........',
		'....AA..........',
		'................',
		'................'
	]]
};

const arrow_left: Sprite = {
	motion: '',
	palette: { A: C.cyan },
	frames: [[
		'................',
		'................',
		'..........AA....',
		'.........AA.....',
		'........AA......',
		'.......AA.......',
		'......AA........',
		'.....AA.........',
		'.....AA.........',
		'......AA........',
		'.......AA.......',
		'........AA......',
		'.........AA.....',
		'..........AA....',
		'................',
		'................'
	]]
};

// ── Avviso (triangolo ambra con punto esclamativo) ───────────────────────────
const warning: Sprite = {
	motion: '',
	palette: { A: C.amber, B: C.danger },
	frames: [[
		'................',
		'.......A........',
		'.......A........',
		'......A.A.......',
		'......A.A.......',
		'.....A...A......',
		'.....A.B.A......',
		'....A..B..A.....',
		'....A..B..A.....',
		'...A...B...A....',
		'...A.......A....',
		'..A....B....A...',
		'..A.........A...',
		'..AAAAAAAAAAA...',
		'................',
		'................'
	]]
};

// ── Pallini di stato ────────────────────────────────────────────────────────
const DOT_MAP = [
	'................',
	'................',
	'................',
	'................',
	'.....DDDD.......',
	'....DDDDDD......',
	'...DDDDDDDD.....',
	'...DDDDDDDD.....',
	'...DDDDDDDD.....',
	'...DDDDDDDD.....',
	'....DDDDDD......',
	'.....DDDD.......',
	'................',
	'................',
	'................',
	'................'
];
const online: Sprite = { motion: 'pulse', palette: { D: C.green }, frames: [DOT_MAP] };
const wait: Sprite = { motion: 'pulse', palette: { D: C.amber }, frames: [DOT_MAP] };

// ── Medaglie podio ──────────────────────────────────────────────────────────
const MEDAL_MAP = [
	'................',
	'..R........R....',
	'..RR......RR....',
	'...RR....RR.....',
	'....RR..RR......',
	'.....DDDD.......',
	'...DDDDDDDD.....',
	'..DDDDDDDDDD....',
	'..DDDDWWDDDD....',
	'..DDDWWWWDDD....',
	'..DDDDWWDDDD....',
	'...DDDDDDDD.....',
	'.....DDDD.......',
	'................',
	'................',
	'................'
];
const gold: Sprite = { motion: 'pulse', palette: { R: C.mag, D: C.amber, W: '#fff6cf' }, frames: [MEDAL_MAP] };
const silver: Sprite = { motion: 'bob', palette: { R: C.cyan, D: '#cdd6e3', W: C.text }, frames: [MEDAL_MAP] };
const bronze: Sprite = { motion: 'bob', palette: { R: C.muted, D: '#d98a4a', W: '#ffe0b0' }, frames: [MEDAL_MAP] };

// ── Battaglia navale (statistiche + poteri) ─────────────────────────────────
const target: Sprite = {
	motion: 'pulse',
	palette: { R: C.danger, W: C.text, A: C.amber },
	frames: [[
		'................',
		'................',
		'....RRRRRR......',
		'..RRRRRRRRRR....',
		'.RRWWWWWWWWRR...',
		'.RWWRRRRRRWWR...',
		'.RWRRWWWWRRWR...',
		'.RWRWWAAWWRWR...',
		'.RWRWWAAWWRWR...',
		'.RWRRWWWWRRWR...',
		'.RWWRRRRRRWWR...',
		'.RRWWWWWWWWRR...',
		'..RRRRRRRRRR....',
		'....RRRRRR......',
		'................',
		'................'
	]]
};

const hourglass: Sprite = {
	motion: 'bob',
	palette: { A: C.amber, W: C.text },
	frames: [[
		'................',
		'................',
		'..WWWWWWWWWW....',
		'..WAAAAAAAAW....',
		'...WAAAAAAW.....',
		'....WAAAAW......',
		'.....WAAW.......',
		'......WW........',
		'......WW........',
		'.....WAAW.......',
		'....WAAAAW......',
		'...WAAAAAAW.....',
		'..WAAAAAAAAW....',
		'..WWWWWWWWWW....',
		'................',
		'................'
	]]
};

const fire: Sprite = {
	motion: 'pulse',
	palette: { R: C.danger, A: C.amber, Y: '#ffe08a' },
	frames: [[
		'................',
		'................',
		'................',
		'.......R........',
		'......RAR.......',
		'.....RAAR.......',
		'.....RAAAR......',
		'....RAAYAAR.....',
		'....RAYYYAR.....',
		'...RAAYYYAAR....',
		'...RAAYYYYAR....',
		'...RAAAYAAAR....',
		'....RAAAAAR.....',
		'.....RRRRR......',
		'................',
		'................'
	]]
};

const water: Sprite = {
	motion: 'bob',
	palette: { C: C.cyan, W: '#cdf6ff' },
	frames: [[
		'................',
		'................',
		'.......C........',
		'.......CC.......',
		'......CCC.......',
		'......CCCC......',
		'.....CCCCC......',
		'.....CWCCC......',
		'....CCWCCCC.....',
		'....CCWCCCC.....',
		'....CCCCCCC.....',
		'.....CCCCC......',
		'......CCC.......',
		'................',
		'................',
		'................'
	]]
};

const dice: Sprite = {
	motion: 'swing',
	palette: { W: C.text, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'..WWWWWWWWWW....',
		'..WWWWWWWWWW....',
		'..WWKKWWKKWW....',
		'..WWKKWWKKWW....',
		'..WWWWKKWWWW....',
		'..WWWWKKWWWW....',
		'..WWKKWWKKWW....',
		'..WWKKWWKKWW....',
		'..WWWWWWWWWW....',
		'..WWWWWWWWWW....',
		'................',
		'................',
		'................',
		'................'
	]]
};

const pencil: Sprite = {
	motion: 'swing',
	palette: { A: C.amber, W: C.text, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'...........WW...',
		'..........WWW...',
		'.........AAW....',
		'........AAA.....',
		'.......AAA......',
		'......AAA.......',
		'.....AAA........',
		'....AAA.........',
		'...AAA..........',
		'..KAA...........',
		'..K.............',
		'................',
		'................',
		'................'
	]]
};

const bomb: Sprite = {
	motion: '',
	palette: { K: '#15152b', W: '#5a5a8a', F: C.amber, A: C.amber },
	frames: [[
		'............A...',
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
	]]
};

const flag: Sprite = {
	motion: 'swing',
	palette: { R: C.danger, L: C.muted },
	frames: [[
		'................',
		'................',
		'....LRRRRR......',
		'....LRRRRRRR....',
		'....LRRRRRRRR...',
		'....LRRRRRR.....',
		'....LRRR........',
		'....L...........',
		'....L...........',
		'....L...........',
		'....L...........',
		'...LLLLL........',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Accessori impiccato ─────────────────────────────────────────────────────
const hat: Sprite = {
	motion: 'bob',
	palette: { K: '#241a3a', B: C.mag },
	frames: [[
		'................',
		'................',
		'................',
		'.....KKKKKK.....',
		'.....KKKKKK.....',
		'.....KKKKKK.....',
		'.....KKKKKK.....',
		'.....BBBBBB.....',
		'...KKKKKKKKKK...',
		'..KKKKKKKKKKKK..',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const pipe: Sprite = {
	motion: 'bob',
	palette: { W: '#c2925a' },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'............WWW.',
		'...........W..W.',
		'..WWWWWWWWWWW.W.',
		'..WWWWWWWWWWWWW.',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const shoes: Sprite = {
	motion: 'bob',
	palette: { W: C.text, C: C.cyan, L: C.muted },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'.....WWW........',
		'.....WWWW.......',
		'....WWWWWWW.....',
		'...WWCWWWWWWW...',
		'..WWWCWWWWWWWW..',
		'..WWWWWWWWWWWW..',
		'..LLLLLLLLLLLL..',
		'................',
		'................',
		'................'
	]]
};

const hand: Sprite = {
	motion: 'swing',
	palette: { C: C.cyan },
	frames: [[
		'................',
		'................',
		'...C.C.C.C......',
		'...C.C.C.C......',
		'...CCCCCCC......',
		'.CCCCCCCCC......',
		'CCCCCCCCCC......',
		'.CCCCCCCCC......',
		'..CCCCCCC.......',
		'...CCCCC........',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const bolt: Sprite = {
	motion: 'pulse',
	palette: { A: C.amber },
	frames: [[
		'................',
		'................',
		'........AAAA....',
		'.......AAA......',
		'......AAA.......',
		'.....AAAAAAA....',
		'........AAA.....',
		'.......AAA......',
		'......AAA.......',
		'.....AAA........',
		'....AA..........',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Accessori avatar (comprabili nello shop, indossati sul volto) ────────────
const acc_corona: Sprite = {
	motion: 'bob',
	palette: { G: C.amber, R: C.danger },
	frames: [[
		'................',
		'................',
		'...G...GG...G...',
		'...G...GG...G...',
		'...GG..GG..GG...',
		'...GG.GGGG.GG...',
		'...GGGGGGGGGG...',
		'...GGGGRRGGGG...',
		'...GGGGGGGGGG...',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const acc_cilindro: Sprite = {
	motion: 'bob',
	palette: { K: '#241a3a', A: C.amber },
	frames: [[
		'................',
		'....KKKKKKKK....',
		'....KKKKKKKK....',
		'....KKKKKKKK....',
		'....KKKKKKKK....',
		'....KKKKKKKK....',
		'....AAAAAAAA....',
		'....KKKKKKKK....',
		'..KKKKKKKKKKKK..',
		'.KKKKKKKKKKKKKK.',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const acc_cuffie: Sprite = {
	motion: 'bob',
	palette: { K: '#241a3a', C: C.cyan },
	frames: [[
		'................',
		'................',
		'....KKKKKKKK....',
		'..KKKK....KKKK..',
		'..KK........KK..',
		'..KK........KK..',
		'..KK........KK..',
		'.KKK........KKK.',
		'.KCK........KCK.',
		'.KCK........KCK.',
		'.KKK........KKK.',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const acc_occhiali: Sprite = {
	palette: { F: C.cyan, L: C.inset },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'..FFFFF..FFFFF..',
		'FFFLLLFFFFLLLFFF',
		'..FLLLF..FLLLF..',
		'..FFFFF..FFFFF..',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const acc_shades: Sprite = {
	palette: { M: C.mag, D: '#0a0a14', G: C.cyan },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'.MMMMMMMMMMMMMM.',
		'.MGDDDDMMGDDDDM.',
		'.MDDDDDMMDDDDDM.',
		'..MDDDM..MDDDM..',
		'...MMM....MMM...',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const acc_baffi: Sprite = {
	palette: { B: '#7a4a24', D: '#5a3418' },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................',
		'.B....BBBB....B.',
		'.BB..BBBBBB..BB.',
		'..BBBBB..BBBBB..',
		'...DDDD..DDDD...',
		'................',
		'................',
		'................',
		'................'
	]]
};

// ── Poteri shop (radar/sposta/espandi/esca) ─────────────────────────────────
const radar: Sprite = {
	motion: 'pulse',
	palette: { C: C.cyan, L: C.muted },
	frames: [[
		'................',
		'................',
		'........CCCC....',
		'.......C....C...',
		'......C....C....',
		'.....C....C.....',
		'.....C...C......',
		'....C.CCC.......',
		'....CC..........',
		'...LCL..........',
		'..LLLLL.........',
		'.LLLLLLL........',
		'................',
		'................',
		'................',
		'................'
	]]
};

const move: Sprite = {
	motion: '',
	palette: { C: C.cyan },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'...C........C...',
		'..CC........CC..',
		'.CCCCCCCCCCCCCC.',
		'..CC........CC..',
		'...C........C...',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const plus: Sprite = {
	motion: 'pulse',
	palette: { G: C.green },
	frames: [[
		'................',
		'................',
		'................',
		'......GG........',
		'......GG........',
		'......GG........',
		'..GGGGGGGGGG....',
		'..GGGGGGGGGG....',
		'......GG........',
		'......GG........',
		'......GG........',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const trap: Sprite = {
	motion: '',
	palette: { R: C.danger, L: C.muted },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'................',
		'.R..........R...',
		'.RR........RR...',
		'..RR......RR....',
		'...RR....RR.....',
		'....RRRRRR......',
		'..LLLLLLLLLL....',
		'..LLLLLLLLLL....',
		'................',
		'................',
		'................',
		'................'
	]]
};

/* =============================================================================
   Companion (cosmetici acquistabili dallo shop). Le chiavi coincidono con gli
   id del CompanionCatalog backend → <Icon name={companionId} /> ovunque.
============================================================================= */

const gondola: Sprite = {
	motion: 'float',
	palette: { K: '#5a5472', F: C.danger, A: C.amber, Y: '#ffe08a' },
	frames: [[
		'................',
		'..............K.',
		'.............K..',
		'............K...',
		'...F.......K....',
		'..FAF.....K.....',
		'..FYF....K......',
		'...A....K.......',
		'K......KK.......',
		'KK....KK........',
		'.KKKKKKK........',
		'.KKKKKKKKKKKK...',
		'..KKKKKKKKKK....',
		'................',
		'................',
		'................'
	]]
};

const leone: Sprite = {
	motion: 'bob',
	palette: { A: C.amber, S: '#c9991f', K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'...A.A.A.A.A....',
		'..AAAAAAAAAAA...',
		'.AAAAAAAAAAAAA..',
		'.AAASAAAAASAAA..',
		'.AAKAAAAAAKAAA..',
		'.AAAAAKKAAAAAA..',
		'.AAAASKKSAAAAA..',
		'.AAAAAAAAAAAA...',
		'..AAAAAAAAAAA...',
		'...A.A.A.A.A....',
		'................',
		'................',
		'................',
		'................'
	]]
};

const mose: Sprite = {
	motion: '',
	fps: 1.2,
	palette: { B: '#1a6bd6', Y: C.amber, C: '#7fd4ff' },
	frames: [
		[
			'................',
			'................',
			'..Y.Y.Y.Y.Y.Y...',
			'..Y.Y.Y.Y.Y.Y...',
			'..Y.Y.Y.Y.Y.Y...',
			'..YYYYYYYYYYYY..',
			'.BBBBBBBBBBBBBB.',
			'.BCBBBCBBBBCBBB.',
			'.BBBBBBBBBBBBBB.',
			'.BBBBBBBBBBBBBB.',
			'.BBBBBBBBBBBBBB.',
			'................',
			'................',
			'................',
			'................',
			'................'
		],
		[
			'................',
			'................',
			'................',
			'................',
			'..YYYYYYYYYYYY..',
			'..YYYYYYYYYYYY..',
			'.BBBBBBBBBBBBBB.',
			'.BCBBBCBBBBCBBB.',
			'.BBBBBBBBBBBBBB.',
			'.BBBBBBBBBBBBBB.',
			'.BBBBBBBBBBBBBB.',
			'................',
			'................',
			'................',
			'................',
			'................'
		]
	]
};

const dart180: Sprite = {
	motion: 'pulse',
	palette: { R: C.danger, G: C.green, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'....RRRRRR......',
		'..RRRRRRRRRR....',
		'.RRGGGGGGGGRR...',
		'.RGGGRRRRGGGR...',
		'.RGGRRKKRRGGR...',
		'.RGGRRKKRRGGR...',
		'.RGGGRRRRGGGR...',
		'.RRGGGGGGGGRR...',
		'..RRRRRRRRRR....',
		'....RRRRRR......',
		'................',
		'................',
		'................',
		'................'
	]]
};

const stambecco: Sprite = {
	motion: 'bob',
	palette: { D: '#6b4a28', H: '#9a6b3a', K: '#0a0512' },
	frames: [[
		'................',
		'.D..........D...',
		'.DD........DD...',
		'..DD......DD....',
		'..DD......DD....',
		'...DD....DD.....',
		'....HHHHHHHH....',
		'...HHKHHHHKHH...',
		'...HHHHHHHHHH...',
		'....HHHHHHHH....',
		'.....HHHHHH.....',
		'......HHHH......',
		'.......HH.......',
		'................',
		'................',
		'................'
	]]
};

const lupo: Sprite = {
	motion: 'bob',
	palette: { G: '#8a93a8', S: '#5a6378', K: '#0a0512' },
	frames: [[
		'................',
		'..G..........G..',
		'.GG..........GG.',
		'.GGG........GGG.',
		'.GGGGGGGGGGGGGG.',
		'.GGGGGGGGGGGGGG.',
		'.GGKGGGGGGGGKGG.',
		'.GGGGGGGGGGGGGG.',
		'..GGGGGGGGGGGG..',
		'...GGGGSSGGGG...',
		'....GGSKSGG.....',
		'.....GGGGGG.....',
		'................',
		'................',
		'................',
		'................'
	]]
};

const batman: Sprite = {
	motion: 'bob',
	palette: { K: '#33334d', W: C.text },
	frames: [[
		'................',
		'.K..........K...',
		'.KK........KK...',
		'.KKK......KKK...',
		'.KKKKKKKKKKKKKK.',
		'.KKKKKKKKKKKKKK.',
		'.KKWWKKKKKKWWKK.',
		'.KKWWKKKKKKWWKK.',
		'.KKKKKKKKKKKKKK.',
		'..KKKK....KKKK..',
		'...KK......KK...',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const persona5: Sprite = {
	motion: 'bob',
	palette: { W: C.text, K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'................',
		'................',
		'.WW........WW...',
		'.WWWW....WWWW...',
		'.WWKWW..WWKWW...',
		'.WWKWW..WWKWW...',
		'.WWWWW..WWWWW...',
		'..WWW....WWW....',
		'...W......W.....',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

const sly: Sprite = {
	motion: 'bob',
	palette: { B: '#2b6bff', K: '#0a0512' },
	frames: [[
		'................',
		'................',
		'................',
		'..B........B....',
		'.BBB......BBB...',
		'.BBBBBBBBBBBBBB.',
		'.BBKKBBBBBBKKBB.',
		'.BBKKBBBBBBKKBB.',
		'.BBBBBBBBBBBBBB.',
		'..BBB......BBB..',
		'................',
		'................',
		'................',
		'................',
		'................',
		'................'
	]]
};

/* =============================================================================
   Stemmi casata (clan). Emblemi pixel ORIGINALI ispirati alle 4 case: scudo
   coi colori della casa + animale stilizzato (leone/serpente/aquila/tasso).
============================================================================= */

const grifondoro: Sprite = {
	motion: 'bob',
	palette: { O: '#4a0a0a', P: '#9b1c1c', S: '#f0c64a', K: '#3a0808' },
	frames: [[
		'................',
		'.OOOOOOOOOOOO...',
		'.OPPPPPPPPPPO...',
		'.OPSSSSSSSSPO...',
		'.OPSSSSSSSSPO...',
		'.OPSSKSSKSSPO...',
		'.OPSSSSSSSSPO...',
		'.OPSSSKKSSSPO...',
		'.OPSSSSSSSSPO...',
		'..OPPPPPPPPO....',
		'...OPPPPPPO.....',
		'....OPPPPO......',
		'.....OPPO.......',
		'......OO........',
		'................',
		'................'
	]]
};

const serpeverde: Sprite = {
	motion: 'bob',
	palette: { O: '#0a2a18', P: '#1a6b3a', S: '#cdd6e3' },
	frames: [[
		'................',
		'.OOOOOOOOOOOO...',
		'.OPPPPPPPPPPO...',
		'.OPSSSSSSPPPO...',
		'.OPPPPPPSSPPO...',
		'.OPPPPSSSPPPO...',
		'.OPPSSSPPPPPO...',
		'.OPSSPPPPPPPO...',
		'.OPSSSSSSSPPO...',
		'..OPPPPPPPPO....',
		'...OPPPPPPO.....',
		'....OPPPPO......',
		'.....OPPO.......',
		'......OO........',
		'................',
		'................'
	]]
};

const corvonero: Sprite = {
	motion: 'bob',
	palette: { O: '#0a1a40', P: '#1a3a7a', S: '#b97e3a' },
	frames: [[
		'................',
		'.OOOOOOOOOOOO...',
		'.OPPPPPPPPPPO...',
		'.OPPPPSSPPPPO...',
		'.OPSSPSSPSSPO...',
		'.OSSSSSSSSSSO...',
		'.OPSSSSSSSSPO...',
		'.OPPPSSSSPPPO...',
		'.OPPPPSSPPPPO...',
		'..OPPPPPPPPO....',
		'...OPPPPPPO.....',
		'....OPPPPO......',
		'.....OPPO.......',
		'......OO........',
		'................',
		'................'
	]]
};

const tassorosso: Sprite = {
	motion: 'bob',
	palette: { O: '#6b4e00', P: '#e8c020', S: '#161616', W: '#f4f4f4' },
	frames: [[
		'................',
		'.OOOOOOOOOOOO...',
		'.OPPPPPPPPPPO...',
		'.OPPSSSSSSPPO...',
		'.OPSSSWWSSSPO...',
		'.OPSSWWWWSSPO...',
		'.OPSSWWWWSSPO...',
		'.OPSSSWWSSSPO...',
		'.OPPSSSSSSPPO...',
		'..OPPPPPPPPO....',
		'...OPPPPPPO.....',
		'....OPPPPO......',
		'.....OPPO.......',
		'......OO........',
		'................',
		'................'
	]]
};

// ── Pong ───────────────────────────────────────────────────────────────────
const pong: Sprite = {
	motion: 'pulse',
	fps: 5,
	palette: { P: C.cyan, N: C.muted, B: C.text },
	frames: [
		[
			'................',
			'................',
			'................',
			'..PP...N....PP..',
			'..PP...N....PP..',
			'..PP........PP..',
			'..PP...N....PP..',
			'..PP...N.BB.PP..',
			'..PP...N.BB.PP..',
			'..PP........PP..',
			'..PP...N....PP..',
			'..PP...N....PP..',
			'................',
			'................',
			'................',
			'................'
		],
		[
			'................',
			'................',
			'................',
			'..PP...N....PP..',
			'..PP...N....PP..',
			'..PP........PP..',
			'..PP...N....PP..',
			'..PP.BBN....PP..',
			'..PP.BBN....PP..',
			'..PP........PP..',
			'..PP...N....PP..',
			'..PP...N....PP..',
			'................',
			'................',
			'................',
			'................'
		]
	]
};

// ── Companion: scarabeo rinoceronte (colore scelto dall'utente) ────────────
// Vista dall'alto: corno, testa con occhi, elitre divise dalla linea centrale, sei zampe che
// si muovono fra i due fotogrammi. B = corazza ricolorabile, K = dettagli scuri (visibili su
// qualunque tinta), Z = zampe (stessa tinta della corazza, tenute separate per leggibilità).
const scarabeo: Sprite = {
	motion: 'bob',
	fps: 3,
	palette: { B: '#3dff9a', K: '#10121a', Z: '#3dff9a' },
	tintKeys: ['B', 'Z'],
	frames: [
		[
			'................',
			'.......BB.......',
			'.......BB.......',
			'.....BBBBBB.....',
			'.....BKBBKB.....',
			'....BBBBBBBB....',
			'Z...BBBBBBBB...Z',
			'.Z.BBBBBBBBBB.Z.',
			'...BBBBKKBBBB...',
			'Z..BBBBKKBBBB..Z',
			'.Z.BBBBKKBBBB.Z.',
			'...BBBBKKBBBB...',
			'Z...BBBKKBBB...Z',
			'.Z...BBBBBB...Z.',
			'......BBBB......',
			'................'
		],
		[
			'................',
			'.......BB.......',
			'.......BB.......',
			'.....BBBBBB.....',
			'.....BKBBKB.....',
			'....BBBBBBBB....',
			'.Z..BBBBBBBB..Z.',
			'Z..BBBBBBBBBB..Z',
			'...BBBBKKBBBB...',
			'.Z.BBBBKKBBBB.Z.',
			'Z..BBBBKKBBBB..Z',
			'...BBBBKKBBBB...',
			'.Z..BBBKKBBB..Z.',
			'Z....BBBBBB....Z',
			'......BBBB......',
			'................'
		]
	]
};

// ── Companion: "Lo Special", il panino del paninaro Fabio ─────────────────
// R = sugo che coce da 200 anni: esce dai lati, schizza sopra e gocciola sotto (2 fotogrammi).
const panino: Sprite = {
	motion: 'float',
	fps: 3,
	palette: { C: '#a9702f', B: '#e0aa63', R: '#d6382f', S: '#8b3a2a', G: C.green },
	frames: [
		[
			'................',
			'.....R....R.....',
			'.....CCCCCC.....',
			'...CCBBBBBBCC...',
			'..CBBBBBBBBBBC..',
			'..CBRRRRRRRRBC..',
			'.RSSSSSSSSSSSSR.',
			'RSSSSGSSSSGSSSSR',
			'.RRSSSSSSSSSSRR.',
			'..CBRRRRRRRRBC..',
			'..CBBBBBBBBBBC..',
			'...CCBBBBBBCC...',
			'.....CCCCCC.....',
			'.....R..R.R.....',
			'......R....R....',
			'................'
		],
		[
			'................',
			'.....R....R.....',
			'.....CCCCCC.....',
			'...CCBBBBBBCC...',
			'..CBBBBBBBBBBC..',
			'..CBRRRRRRRRBC..',
			'.RSSSSSSSSSSSSR.',
			'RSSSSGSSSSGSSSSR',
			'.RRSSSSSSSSSSRR.',
			'..CBRRRRRRRRBC..',
			'..CBBBBBBBBBBC..',
			'...CCBBBBBBCC...',
			'.....CCCCCC.....',
			'.....R....R.....',
			'......R..R......',
			'.......R........'
		]
	]
};

// ── Companion: il castoro di DBeaver ──────────────────────────────────────
const castoro: Sprite = {
	motion: 'bob',
	palette: { B: '#8a5a2b', T: '#5b3a1c', K: '#0a0512', W: C.text, N: '#3a2410' },
	// Coda piatta a destra (T) per non confonderlo con un orso, incisivi bianchi al centro.
	frames: [[
		'................',
		'..BB......BB....',
		'.BBBBBBBBBBBB...',
		'.BBKBBBBBBKBB...',
		'.BBBBBBBBBBBB...',
		'..BBBBNNBBBB....',
		'...BBWWWWBB.....',
		'....BWWWWB......',
		'...BBBBBBBB.....',
		'..BBBBBBBBBTTTT.',
		'..BBBBBBBBBTTTT.',
		'..BBBBBBBBBTTTT.',
		'...BBBBBBBB.TTT.',
		'....BB..BB......',
		'................',
		'................'
	]]
};

// ── Companion: Lancer (Gears of War) ─────────────────────────────────────
// La motosega è il tratto riconoscibile: catena chiara con denti a zig-zag ambrati sopra la
// canna, in due fotogrammi (la catena scorre). Prima era una barra rossa piatta, illeggibile.
const lancer: Sprite = {
	motion: 'shake',
	fps: 8,
	palette: { G: '#6b7280', M: '#c3ccd6', D: '#ffcf3f', R: C.danger, K: '#141821' },
	frames: [
		[
			'................',
			'..D.D.D.D.D.D...',
			'..MMMMMMMMMMM...',
			'..MKMKMKMKMKM...',
			'..MMMMMMMMMMM...',
			'..RRRRRRRRRRR...',
			'GGGGGGGGGGGGGGG.',
			'GGKGGGGGGGKGGGG.',
			'GGGGGGGGGGG.....',
			'...GGGG..GG.....',
			'...GGGG.........',
			'...GGGG.........',
			'....GG..........',
			'................',
			'................',
			'................'
		],
		[
			'................',
			'...D.D.D.D.D.D..',
			'..MMMMMMMMMMM...',
			'..KMKMKMKMKMK...',
			'..MMMMMMMMMMM...',
			'..RRRRRRRRRRR...',
			'GGGGGGGGGGGGGGG.',
			'GGKGGGGGGGKGGGG.',
			'GGGGGGGGGGG.....',
			'...GGGG..GG.....',
			'...GGGG.........',
			'...GGGG.........',
			'....GG..........',
			'................',
			'................',
			'................'
		]
	]
};

/* ─────────────────────────────────────────────────────────────────────────────
   Companion: insetti (colore scelto dall'utente), animali e oggetti d'ufficio.
   Nei tintabili la chiave del corpo sta in `tintKeys`; i dettagli scuri restano
   fissi così ogni tinta resta leggibile.
───────────────────────────────────────────────────────────────────────────── */

// ── Ape ───────────────────────────────────────────────────────────────────
const ape: Sprite = {
	motion: 'float',
	fps: 6,
	palette: { Y: '#ffcf3f', K: '#1a1206', W: '#dff6ff' },
	tintKeys: ['Y'],
	frames: [
		[
			'................',
			'...K.......K....',
			'....K.....K.....',
			'.....KKKKKK.....',
			'.....KYYYYK.....',
			'....WWYYYYWW....',
			'...WWWYYYYWWW...',
			'....WWYYYYWW....',
			'.....YYYYYY.....',
			'.....KKKKKK.....',
			'.....YYYYYY.....',
			'.....KKKKKK.....',
			'......YYYY......',
			'......KKKK......',
			'.......KK.......',
			'................'
		],
		[
			'................',
			'...K.......K....',
			'....K.....K.....',
			'.....KKKKKK.....',
			'.....KYYYYK.....',
			'...WWWYYYYWWW...',
			'....WWYYYYWW....',
			'...WWWYYYYWWW...',
			'.....YYYYYY.....',
			'.....KKKKKK.....',
			'.....YYYYYY.....',
			'.....KKKKKK.....',
			'......YYYY......',
			'......KKKK......',
			'.......KK.......',
			'................'
		]
	]
};

// ── Vespa (più affusolata dell'ape, vita sottile) ─────────────────────────
const vespa: Sprite = {
	motion: 'float',
	fps: 6,
	palette: { Y: '#ffe14a', K: '#151007', W: '#dff6ff' },
	tintKeys: ['Y'],
	frames: [
		[
			'................',
			'....K.....K.....',
			'.....K...K......',
			'.....KKKKK......',
			'.....KYYYK......',
			'....WWYYYWW.....',
			'...WWWYYYWWW....',
			'.....KYYYK......',
			'......KYK.......',
			'.....YYYYY......',
			'.....KKKKK......',
			'.....YYYYY......',
			'......KKK.......',
			'.......YY.......',
			'.......K........',
			'................'
		],
		[
			'................',
			'....K.....K.....',
			'.....K...K......',
			'.....KKKKK......',
			'.....KYYYK......',
			'...WWWYYYWWW....',
			'....WWYYYWW.....',
			'.....KYYYK......',
			'......KYK.......',
			'.....YYYYY......',
			'.....KKKKK......',
			'.....YYYYY......',
			'......KKK.......',
			'.......YY.......',
			'.......K........',
			'................'
		]
	]
};

// ── Calabrone (grosso, torace largo) ─────────────────────────────────────
const calabrone: Sprite = {
	motion: 'float',
	fps: 6,
	palette: { Y: '#e8862b', K: '#120d05', W: '#e8f9ff' },
	tintKeys: ['Y'],
	frames: [
		[
			'................',
			'..K..........K..',
			'...K........K...',
			'.WW..KKKKKK..WW.',
			'..WW.KYYYYK.WW..',
			'...WWKYYYYKWW...',
			'....KYYYYYYK....',
			'....KYYKKYYK....',
			'....KYYYYYYK....',
			'.....KKKKKK.....',
			'.....YYYYYY.....',
			'.....KKKKKK.....',
			'......YYYY......',
			'......KKKK......',
			'.......KK.......',
			'................'
		],
		[
			'................',
			'..K..........K..',
			'...K........K...',
			'..WW.KKKKKK.WW..',
			'...WWKYYYYKWW...',
			'.WW..KYYYYK..WW.',
			'....KYYYYYYK....',
			'....KYYKKYYK....',
			'....KYYYYYYK....',
			'.....KKKKKK.....',
			'.....YYYYYY.....',
			'.....KKKKKK.....',
			'......YYYY......',
			'......KKKK......',
			'.......KK.......',
			'................'
		]
	]
};

// ── Farfalla (ali tintabili, sbattono) ───────────────────────────────────
const farfalla: Sprite = {
	motion: 'float',
	fps: 5,
	palette: { W: '#ff2e88', K: '#1a0d1f', S: '#ffe14a' },
	tintKeys: ['W'],
	frames: [
		[
			'................',
			'.....K....K.....',
			'......K..K......',
			'.......KK.......',
			'WWW....KK....WWW',
			'WWWWW..KK..WWWWW',
			'WWWWWWWKKWWWWWWW',
			'WWWSWWWKKWWWSWWW',
			'WWWWWWWKKWWWWWWW',
			'.WWWWWWKKWWWWWW.',
			'..WWWWWKKWWWWW..',
			'...WWWWKKWWWW...',
			'....WWWKKWWW....',
			'.....WWKKWW.....',
			'......WKKW......',
			'................'
		],
		[
			'................',
			'.....K....K.....',
			'......K..K......',
			'.......KK.......',
			'.......KK.......',
			'..WWW..KK..WWW..',
			'.WWWWWWKKWWWWWW.',
			'.WWSWWWKKWWWSWW.',
			'.WWWWWWKKWWWWWW.',
			'..WWWWWKKWWWWW..',
			'...WWWWKKWWWW...',
			'....WWWKKWWW....',
			'.....WWKKWW.....',
			'......WKKW......',
			'.......KK.......',
			'................'
		]
	]
};

// ── Macchinetta Frog dell'ufficio ────────────────────────────────────────
// Caffè infinito e, nella tanica dell'acqua mai lavata, qualcosa che si muove.
const frog: Sprite = {
	motion: 'bob',
	fps: 3,
	palette: { M: '#7c8896', D: '#2ff3ff', K: '#141821', C: '#a9702f', T: '#1f3b3a', A: '#3dff9a', S: '#e8f9ff' },
	frames: [
		[
			'................',
			'..MMMMMMMM.TTT..',
			'..MDDDDDDM.TAT..',
			'..MMMMMMMM.TAT..',
			'..MMKKKKMM.TAT..',
			'..MM.SS.MM.AAA..',
			'..MM.SS.MM.WAW..',
			'..MMCCCCMM.AAA..',
			'..MMMMMMMM.TTT..',
			'..MMMMMMMM......',
			'..MMKKKKMM......',
			'..MMMMMMMM......',
			'..MMMMMMMM......',
			'..MM....MM......',
			'................',
			'................'
		],
		[
			'................',
			'..MMMMMMMM.TTT..',
			'..MDDDDDDM.TTT..',
			'..MMMMMMMM.TAT..',
			'..MMKKKKMM.TAT..',
			'..MM.SS.MM.AAA..',
			'..MM.SS.MM.AWA..',
			'..MMCCCCMM.WAW..',
			'..MMMMMMMM.AAA..',
			'..MMMMMMMM......',
			'..MMKKKKMM......',
			'..MMMMMMMM......',
			'..MMMMMMMM......',
			'..MM....MM......',
			'................',
			'................'
		]
	]
};

// ── Procione (24x24: banda nera continua sugli occhi fra due fasce chiare, muso, coda ad anelli) ─
const procione: Sprite = {
	motion: 'bob',
	grid: 24,
	fps: 3,
	palette: { G: '#9aa3ad', S: '#6b7280', K: '#141821', W: '#e8f9ff', T: '#7f8794' },
	frames: [
		[
			'........................',
			'.....GGG........GGG.....',
			'....GSSSG......GSSSG....',
			'....GGGGGGGGGGGGGGGG....',
			'...GGGGGGGGGGGGGGGGGG...',
			'...GWWWWWWWWWWWWWWWWG...',
			'...KKKKKKKKKKKKKKKKKK...',
			'...KKWKKKKKKKKKKKKWKK...',
			'...KKKKKKKKKKKKKKKKKK...',
			'...GWWWWWWWWWWWWWWWWG...',
			'....GGGGWWWWWWWWGGGG....',
			'.....GGGGWWWWWWGGGG.....',
			'......GGGGGKKGGGGG......',
			'.......GGGGKKGGGG.......',
			'........GGGGGGGG........',
			'......GGGGGGGGGGGG......',
			'.....GGGGGGGGGGGGGG.....',
			'....GGGGGGGGGGGGGGGG....',
			'....GGGSGGGGGGGGGGGGTTTT',
			'....GGGGGGGGGGGGGGGGKKKK',
			'....GGGGGGGGGGGGGGGGTTTT',
			'.....GGGGGGGGGGGGGGKKKK.',
			'.....KKKGG...GGKKK......',
			'........................'
		],
		[
			'........................',
			'.....GGG........GGG.....',
			'....GSSSG......GSSSG....',
			'....GGGGGGGGGGGGGGGG....',
			'...GGGGGGGGGGGGGGGGGG...',
			'...GWWWWWWWWWWWWWWWWG...',
			'...KKKKKKKKKKKKKKKKKK...',
			'...KKWKKKKKKKKKKKKWKK...',
			'...KKKKKKKKKKKKKKKKKK...',
			'...GWWWWWWWWWWWWWWWWG...',
			'....GGGGWWWWWWWWGGGG....',
			'.....GGGGWWWWWWGGGG.....',
			'......GGGGGKKGGGGG......',
			'.......GGGGKKGGGG.......',
			'........GGGGGGGG........',
			'......GGGGGGGGGGGG..TTTT',
			'.....GGGGGGGGGGGGGGKKKK.',
			'....GGGGGGGGGGGGGGGGTTTT',
			'....GGGSGGGGGGGGGGGKKKK.',
			'....GGGGGGGGGGGGGGGG....',
			'....GGGGGGGGGGGGGGGG....',
			'.....GGGGGGGGGGGGGG.....',
			'.....KKKGG...GGKKK......',
			'........................'
		]
	]
};

// ── Gatto (24x24: orecchie con interno rosa, occhi verdi con pupilla, baffi, coda arricciata) ─
const gatto: Sprite = {
	motion: 'bob',
	grid: 24,
	fps: 3,
	palette: {
		C: '#8a93a8', S: '#5f6878', L: '#b6bfd0', K: '#141821',
		E: C.green, P: '#141821', N: '#ff9ec4', M: '#3b2430', W: '#e8f9ff', T: '#6b7280'
	},
	frames: [
		[
			'........................',
			'......CC........CC......',
			'.....CCCC......CCCC.....',
			'.....CNNC......CNNC.....',
			'.....CCCCC....CCCCC.....',
			'.....CCCCCCCCCCCCCC.....',
			'.....CSCCCCCCCCCCSC.....',
			'.....CCEEPCCCCPEECC.....',
			'.....CCEEECCCCEEECC.....',
			'.....CCCCCCCCCCCCCC.....',
			'...WWCCCCCCNNCCCCCCWW...',
			'....WCCCCCCMMCCCCCCW....',
			'.....CCCCCCCCCCCCCC.....',
			'......CCCCCCCCCCCC......',
			'......CCCCCCCCCCCC......',
			'.....CCCCCCCCCCCCCC.....',
			'.....CCCSCCCCCCSCCC..TT.',
			'.....CCCCCCCCCCCCCC.TT..',
			'.....CCCCCCCCCCCCCCTT...',
			'.....CCCCCCCCCCCCCC.....',
			'.....CCLCCCCCCCCLCC.....',
			'.....CCCCC....CCCCC.....',
			'......LLL......LLL......',
			'........................'
		],
		[
			'........................',
			'......CC........CC......',
			'.....CCCC......CCCC.....',
			'.....CNNC......CNNC.....',
			'.....CCCCC....CCCCC.....',
			'.....CCCCCCCCCCCCCC.....',
			'.....CSCCCCCCCCCCSC.....',
			'.....CCEEPCCCCPEECC.....',
			'.....CCEEECCCCEEECC.....',
			'.....CCCCCCCCCCCCCC.....',
			'...WWCCCCCCNNCCCCCCWW...',
			'....WCCCCCCMMCCCCCCW....',
			'.....CCCCCCCCCCCCCC.....',
			'......CCCCCCCCCCCC......',
			'......CCCCCCCCCCCC......',
			'.....CCCCCCCCCCCCCC.TT..',
			'.....CCCSCCCCCCSCCCTT...',
			'.....CCCCCCCCCCCCCCT....',
			'.....CCCCCCCCCCCCCC.....',
			'.....CCCCCCCCCCCCCC.....',
			'.....CCLCCCCCCCCLCC.....',
			'.....CCCCC....CCCCC.....',
			'......LLL......LLL......',
			'........................'
		]
	]
};

// ── Cani: chihuahua, akita (la razza di Hachiko), pastore tedesco, dalmata ─
const chihuahua: Sprite = {
	motion: 'bob',
	palette: { B: '#d8a86a', K: '#141821', N: '#3a2a1a' },
	frames: [[
		'................',
		'.BB..........BB.',
		'.BBB........BBB.',
		'.BBBB......BBBB.',
		'.BBBBBBBBBBBBBB.',
		'..BBKBBBBBBKBB..',
		'..BBBBBNNBBBBB..',
		'...BBBBNNBBBB...',
		'.....BBBBBB.....',
		'....BBBBBBBB....',
		'....BBBBBBBB....',
		'.....BBBBBB..B..',
		'.....BB..BB.BB..',
		'................',
		'................',
		'................'
	]]
};

const akita: Sprite = {
	motion: 'bob',
	palette: { B: '#e0a55c', W: '#f7f3e8', K: '#141821', N: '#2a1c10' },
	frames: [[
		'................',
		'..BB.......BB...',
		'..BBB.....BBB...',
		'..BBBBBBBBBBB...',
		'..BBKBBBBBKBB...',
		'..BBWWWNNWWBB...',
		'...BWWWNNWWB....',
		'....BWWWWWB.....',
		'....BBBBBB..BB..',
		'...BBBBBBBB.BBB.',
		'...WBBBBBBBBBB..',
		'...WWBBBBBB.BB..',
		'....WW..WW......',
		'................',
		'................',
		'................'
	]]
};

const pastore: Sprite = {
	motion: 'bob',
	palette: { K: '#2b2b2f', B: '#b5793a', W: '#e8f9ff', N: '#141821' },
	frames: [[
		'................',
		'..KK.......KK...',
		'..KKK.....KKK...',
		'..KKKKKKKKKKK...',
		'..KKWKKKKKWKK...',
		'..KKKKKNNKKKK...',
		'...KKBBNNBBK....',
		'....KBBBBBK.....',
		'....BBBBBB..KK..',
		'...BBBBBBBB.KKK.',
		'...KBBBBBBBBKK..',
		'...KKBBBBBB.KK..',
		'....BB..BB......',
		'................',
		'................',
		'................'
	]]
};

const dalmata: Sprite = {
	motion: 'bob',
	palette: { W: '#f2f6ff', K: '#141821', N: '#3a3a44' },
	frames: [[
		'................',
		'..KK.......KK...',
		'..WKW.....WKW...',
		'..WWWWWWWWWWW...',
		'..WKWWKWWWKWW...',
		'..WWWWWNNWWWW...',
		'...WWKWNNWWKW...',
		'....WWWWWWW.....',
		'....WWKWWW..WW..',
		'...WWWWWWKW.WKW.',
		'...WKWWWWWWWWW..',
		'...WWWKWWW..WW..',
		'....WW..WW......',
		'................',
		'................',
		'................'
	]]
};

// ── Capybara (di profilo, il roditore più tranquillo del mondo) ───────────
const capybara: Sprite = {
	motion: 'bob',
	palette: { B: '#a5763f', K: '#141821', N: '#4a2f16' },
	frames: [[
		'................',
		'................',
		'.........BB.....',
		'.......BBBBBB...',
		'......BBBKBBBB..',
		'.....BBBBBBBNN..',
		'..BBBBBBBBBBBB..',
		'.BBBBBBBBBBBBB..',
		'BBBBBBBBBBBBBB..',
		'BBBBBBBBBBBBB...',
		'BBBBBBBBBBBB....',
		'.BB.BB..BB.B....',
		'.BB.BB..BB.B....',
		'................',
		'................',
		'................'
	]]
};

// ── Ratto (24x24: orecchie tonde con interno rosa, occhi col riflesso, baffi, coda lunga) ─
const ratto: Sprite = {
	motion: 'bob',
	grid: 24,
	fps: 3,
	palette: { G: '#a8a2b8', S: '#7b7590', K: '#141821', N: '#ff9ec4', W: '#ffffff', T: '#e0a6bd' },
	frames: [
		[
			'........................',
			'........................',
			'........GGG.....GGG.....',
			'.......GNNNG...GNNNG....',
			'.......GNNNG...GNNNG....',
			'.......GGGGGGGGGGGGG....',
			'......GGGGGGGGGGGGGGG...',
			'......GGKWGGGGGWKGGGG...',
			'......GGGGGGGGGGGGGGGG..',
			'.....WGGGGGGGNNGGGGGGW..',
			'......GGGGGGKKGGGGGGG...',
			'.......GGGGGGGGGGGGG....',
			'......GGGGGGGGGGGGG.....',
			'.....GGGGGGGGGGGGGG.....',
			'....GGGGSGGGGGGGGGG.....',
			'...GGGGGGGGGGGGGGGG.....',
			'..GGGGGGGGGGGGGGGG......',
			'.TGGGGGGGGGGGGGGG.......',
			'TT.GGGGGGGGGGGGG........',
			'T...NGGGG..GGGGN........',
			'TT......................',
			'.TT.....................',
			'..TT....................',
			'........................'
		],
		[
			'........................',
			'........................',
			'........GGG.....GGG.....',
			'.......GNNNG...GNNNG....',
			'.......GNNNG...GNNNG....',
			'.......GGGGGGGGGGGGG....',
			'......GGGGGGGGGGGGGGG...',
			'......GGKWGGGGGWKGGGG...',
			'......GGGGGGGGGGGGGGGG..',
			'.....WGGGGGGGNNGGGGGGW..',
			'......GGGGGGKKGGGGGGG...',
			'.......GGGGGGGGGGGGG....',
			'......GGGGGGGGGGGGG.....',
			'.....GGGGGGGGGGGGGG.....',
			'....GGGGSGGGGGGGGGG.....',
			'...GGGGGGGGGGGGGGGG.....',
			'..GGGGGGGGGGGGGGGG......',
			'.TGGGGGGGGGGGGGGG.......',
			'TT.GGGGGGGGGGGGG........',
			'T...NGGGG..GGGGN........',
			'.T......................',
			'.TT.....................',
			'...TT...................',
			'.....T..................'
		]
	]
};

// ── Pipistrello e la sua forma vampiro (sbloccabile dal profilo) ─────────
const pipistrello: Sprite = {
	motion: 'float',
	fps: 5,
	palette: { B: '#6b5aa8', K: '#141821', W: '#e8f9ff' },
	frames: [
		[
			'................',
			'..BB........BB..',
			'.BBBB......BBBB.',
			'BBBBBB....BBBBBB',
			'BBBBBBB..BBBBBBB',
			'BBBBBBBBBBBBBBBB',
			'.BBBBBBBBBBBBBB.',
			'...BB.BBBB.BB...',
			'......BKKB......',
			'......BBBB......',
			'.......WW.......',
			'................',
			'................',
			'................',
			'................',
			'................'
		],
		[
			'................',
			'................',
			'..BB........BB..',
			'.BBBB......BBBB.',
			'BBBBBB....BBBBBB',
			'.BBBBBBBBBBBBBB.',
			'..BBBBBBBBBBBB..',
			'....BBBBBBBB....',
			'......BKKB......',
			'......BBBB......',
			'.......WW.......',
			'................',
			'................',
			'................',
			'................',
			'................'
		]
	]
};

const pipistrello_vampiro: Sprite = {
	motion: 'float',
	fps: 5,
	palette: { B: '#2a1230', K: '#141821', R: C.danger, W: '#e8f9ff', C: '#7a0f2a' },
	frames: [
		[
			'................',
			'..BB........BB..',
			'.BBBB......BBBB.',
			'BBBBBB....BBBBBB',
			'BBBBBBB..BBBBBBB',
			'BBBBBBBBBBBBBBBB',
			'.BBBBBBBBBBBBBB.',
			'...BB.BBBB.BB...',
			'.....CBRRBC.....',
			'.....CBBBBC.....',
			'......WWWW......',
			'.......WW.......',
			'................',
			'................',
			'................',
			'................'
		],
		[
			'................',
			'................',
			'..BB........BB..',
			'.BBBB......BBBB.',
			'BBBBBB....BBBBBB',
			'.BBBBBBBBBBBBBB.',
			'..BBBBBBBBBBBB..',
			'....BBBBBBBB....',
			'.....CBRRBC.....',
			'.....CBBBBC.....',
			'......WWWW......',
			'.......WW.......',
			'................',
			'................',
			'................',
			'................'
		]
	]
};

// ── Companion: elmo di Master Chief ──────────────────────────────────────
const masterchief: Sprite = {
	motion: 'bob',
	palette: { G: '#3f6b3a', V: C.amber, K: '#0a0512' },
	frames: [[
		'................',
		'.....GGGGGG.....',
		'...GGGGGGGGGG...',
		'..GGGGGGGGGGGG..',
		'..GGVVVVVVVVGG..',
		'..GVVVVVVVVVVG..',
		'..GVVVVVVVVVVG..',
		'..GGVVVVVVVVGG..',
		'..GGGGGGGGGGGG..',
		'..GGGGGGGGGGGG..',
		'...GGGGGGGGGG...',
		'....GGGGGGGG....',
		'.....GGGGGG.....',
		'................',
		'................',
		'................'
	]]
};

// ── Info (tooltip di spiegazione) ─────────────────────────────────────────
const info: Sprite = {
	motion: 'pulse',
	palette: { C: C.cyan, K: '#0a0512' },
	frames: [[
		'................',
		'.....CCCCCC.....',
		'...CCCCCCCCCC...',
		'..CCCCCCCCCCCC..',
		'..CCCCCKKCCCCC..',
		'.CCCCCCKKCCCCCC.',
		'.CCCCCCCCCCCCCC.',
		'.CCCCCCKKCCCCCC.',
		'.CCCCCCKKCCCCCC.',
		'.CCCCCCKKCCCCCC.',
		'..CCCCCKKCCCCC..',
		'..CCCCCKKCCCCC..',
		'...CCCCCCCCCC...',
		'.....CCCCCC.....',
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
	pong,
	// esiti
	win,
	lose,
	draw,
	// chrome / navigazione
	generale,
	gamepad,
	coin,
	tools,
	bug,
	mail,
	rocket,
	house,
	calendar,
	map,
	people,
	person,
	smiley,
	lock,
	chart,
	cart,
	// parola del giorno / vari
	book,
	globe,
	letters,
	party,
	sync,
	clock,
	speech,
	info,
	check,
	cross,
	warning,
	arrow_left,
	arrow_right,
	online,
	wait,
	// podio
	gold,
	silver,
	bronze,
	// battaglia navale: statistiche
	target,
	hourglass,
	fire,
	water,
	dice,
	pencil,
	bomb,
	flag,
	// accessori impiccato
	hat,
	pipe,
	shoes,
	hand,
	bolt,
	// accessori avatar (shop)
	acc_corona,
	acc_cilindro,
	acc_cuffie,
	acc_occhiali,
	acc_shades,
	acc_baffi,
	// poteri shop (alias per id potere → sprite)
	radar,
	move,
	plus,
	trap,
	bs_torpedo: target,
	bs_radar: radar,
	bs_move_ship: move,
	bs_extend_ship: plus,
	bs_expand_board: map,
	bs_extra_ship: battleship,
	bs_decoy: trap,
	// companion (le chiavi coincidono con gli id del CompanionCatalog backend)
	gondola,
	leone,
	mose,
	dart180,
	stambecco,
	lupo,
	batman,
	persona5,
	sly,
	scarabeo,
	panino,
	castoro,
	lancer,
	masterchief,
	ape,
	vespa,
	calabrone,
	farfalla,
	frog,
	procione,
	gatto,
	chihuahua,
	akita,
	pastore,
	dalmata,
	capybara,
	ratto,
	pipistrello,
	// forma alternativa: id sprite = "<companion>_<forma>" (vedi CompanionForm lato backend)
	pipistrello_vampiro,
	// casate (le chiavi coincidono con gli id di HouseCatalog backend)
	grifondoro,
	serpeverde,
	corvonero,
	tassorosso
};
