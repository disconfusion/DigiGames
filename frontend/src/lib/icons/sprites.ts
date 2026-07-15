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
	// casate (le chiavi coincidono con gli id di HouseCatalog backend)
	grifondoro,
	serpeverde,
	corvonero,
	tassorosso
};
