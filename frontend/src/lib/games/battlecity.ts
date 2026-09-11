/**
 * Sprite pixel di Battle City, nello stesso formato delle icone del sito: righe di caratteri,
 * '.' = trasparente, ogni altra lettera è una chiave della palette. I tank sono 16×16 come
 * nell'originale NES; il board le disegna sul canvas scalate alla dimensione del campo.
 */
export type PixelSprite = { map: string[]; palette: Record<string, string> };

/**
 * Tank visto da sopra, rivolto in alto, come nell'originale NES: cingoli squadrati ai lati con
 * le tacche dei rulli (righe alterne B/C), scafo centrale, torretta rilevata e canna di 2 px che
 * esce dalla torretta. Quattro toni per il rilievo: K contorno, D ombra, B base, C luce.
 * Le altre tre direzioni sono rotazioni di questa mappa — canna compresa.
 */
const TANK_UP = [
	'......KKKK......',
	'......KCCK......',
	'......KCCK......',
	'KKKK..KCCK..KKKK',
	'KCBK..KCCK..KCBK',
	'KBBK.KKCCKK.KBBK',
	'KCBK.KCCCCK.KCBK',
	'KBBKKKCCCCKKKBBK',
	'KCBKCCBBBBCCKCBK',
	'KBBKCBBDDBBCKBBK',
	'KCBKCBDDDDBCKCBK',
	'KBBKCBDDDDBCKBBK',
	'KCBKCBBDDBBCKCBK',
	'KBBKCCCCCCCCKBBK',
	'KCBKKKKKKKKKKCBK',
	'KKKK........KKKK'
];

/** Ruota di 90° in senso orario una mappa quadrata di caratteri. */
function rotate(map: string[]): string[] {
	const n = map.length;
	const out: string[] = [];
	for (let r = 0; r < n; r++) {
		let row = '';
		for (let c = 0; c < n; c++) row += map[n - 1 - c][r] ?? '.';
		out.push(row);
	}
	return out;
}

const TANK_RIGHT = rotate(TANK_UP);
const TANK_DOWN = rotate(TANK_RIGHT);
const TANK_LEFT = rotate(TANK_DOWN);

export const TANK_SHAPES: Record<string, string[]> = {
	UP: TANK_UP,
	RIGHT: TANK_RIGHT,
	DOWN: TANK_DOWN,
	LEFT: TANK_LEFT
};

/**
 * Colori dei tank: giocatori come nell'originale (giallo e verde), nemici per tipo.
 * B = base, C = luce (bordi e torretta), D = ombra (pozzo della torretta e griglia motore).
 * Il contorno K è unico per tutti, come per le altre icone del sito.
 */
export const TANK_OUTLINE = '#141821';

export const TANK_COLORS: Record<string, { B: string; C: string; D: string }> = {
	P1: { B: '#ffcf3f', C: '#ffe89a', D: '#c2901c' },
	P2: { B: '#3dff9a', C: '#c9ffe4', D: '#17a862' },
	BASIC: { B: '#b9c2d0', C: '#eef3f9', D: '#7c8595' },
	FAST: { B: '#2ff3ff', C: '#d8feff', D: '#0e9fb0' },
	POWER: { B: '#ff2e88', C: '#ffc2dc', D: '#b0135c' },
	ARMOR: { B: '#b06bff', C: '#e6d2ff', D: '#6f37b5' }
};

/** L'aquila della base: 16×16, occupa un tile. */
export const EAGLE: PixelSprite = {
	palette: { A: '#e8f9ff', D: '#7b6ba8', K: '#141821', Y: '#ffcf3f' },
	map: [
		'................',
		'.......KK.......',
		'......KAAK......',
		'.....KAAAAK.....',
		'....KAADDAAK....',
		'...KAADDDDAAK...',
		'..KAADDDDDDAAK..',
		'..KADDDKKDDDAK..',
		'..KADDKAAKDDAK..',
		'..KAADDDDDDAAK..',
		'...KAADDDDAAK...',
		'....KYYKKYYK....',
		'.....KYYYYK.....',
		'......KYYK......',
		'.......KK.......',
		'................'
	]
};

/** Simboli dei power-up (16×16, un tile): stella, granata, casco, pala, tank, orologio. */
export const POWERUP_SPRITES: Record<string, PixelSprite> = {
	STAR: {
		palette: { Y: '#ffcf3f', K: '#141821' },
		map: [
			'................',
			'.......KK.......',
			'......KYYK......',
			'......KYYK......',
			'.KKKKKKYYKKKKKK.',
			'.KYYYYYYYYYYYYK.',
			'..KYYYYYYYYYYK..',
			'...KYYYYYYYYK...',
			'....KYYYYYYK....',
			'...KYYYYYYYYK...',
			'..KYYYK..KYYYK..',
			'.KYYYK....KYYYK.',
			'.KYYK......KYYK.',
			'..KK........KK..',
			'................',
			'................'
		]
	},
	GRENADE: {
		palette: { G: '#3dff9a', K: '#141821', W: '#e8f9ff' },
		map: [
			'................',
			'.........WW.....',
			'........WKKW....',
			'.......WKGGK....',
			'......KKGGGK....',
			'.....KGGGGGK....',
			'....KGGGGGGK....',
			'....KGGGGGGK....',
			'....KGGGGGGK....',
			'....KGGGGGGK....',
			'.....KGGGGK.....',
			'......KKKK......',
			'................',
			'................',
			'................',
			'................'
		]
	},
	HELMET: {
		palette: { C: '#2ff3ff', K: '#141821', W: '#e8f9ff' },
		map: [
			'................',
			'................',
			'.....KKKKKK.....',
			'...KKCCCCCCKK...',
			'..KCCCCCCCCCCK..',
			'.KCCCWWWWCCCCK..',
			'.KCCCCCCCCCCCK..',
			'.KCCCCCCCCCCCK..',
			'.KCCCCCCCCCCCK..',
			'..KKKKKKKKKKK...',
			'...KCCK..KCCK...',
			'....KK....KK....',
			'................',
			'................',
			'................',
			'................'
		]
	},
	SHOVEL: {
		palette: { S: '#b9c2d0', B: '#a9702f', K: '#141821' },
		map: [
			'................',
			'.......KK.......',
			'.......BB.......',
			'.......BB.......',
			'.......BB.......',
			'.......BB.......',
			'.....KKBBKK.....',
			'....KSSSSSSK....',
			'....KSSSSSSK....',
			'....KSSSSSSK....',
			'.....KSSSSK.....',
			'......KSSK......',
			'.......KK.......',
			'................',
			'................',
			'................'
		]
	},
	TANK: {
		palette: { Y: '#ffcf3f', K: '#141821' },
		map: [
			'................',
			'................',
			'..K..........K..',
			'.KYK..KKKK..KYK.',
			'.KYK.KYYYYK.KYK.',
			'.KYKKYYYYYYKKYK.',
			'.KYYYYYYYYYYYYK.',
			'.KYYYYYYYYYYYYK.',
			'.KYYYYYYYYYYYYK.',
			'.KYKKYYYYYYKKYK.',
			'.KYK.KYYYYK.KYK.',
			'.KYK..KKKK..KYK.',
			'..K..........K..',
			'................',
			'................',
			'................'
		]
	},
	CLOCK: {
		palette: { A: '#e8f9ff', K: '#141821', R: '#ff5277' },
		map: [
			'................',
			'.....KKKKKK.....',
			'...KKAAAAAAKK...',
			'..KAAAAAAAAAAK..',
			'.KAAAAARAAAAAAK.',
			'.KAAAAARAAAAAAK.',
			'.KAAAAARAAAAAAK.',
			'.KAAAAARRRAAAAK.',
			'.KAAAAAAAAAAAAK.',
			'.KAAAAAAAAAAAAK.',
			'..KAAAAAAAAAAK..',
			'...KKAAAAAAKK...',
			'.....KKKKKK.....',
			'................',
			'................',
			'................'
		]
	}
};

/** Etichette dei power-up per l'HUD e i messaggi. */
export const POWERUP_LABELS: Record<string, string> = {
	STAR: 'Stella: cannone potenziato',
	GRENADE: 'Granata: nemici in campo distrutti',
	HELMET: 'Casco: scudo temporaneo',
	SHOVEL: 'Pala: base in acciaio',
	TANK: 'Tank: vita extra',
	CLOCK: 'Orologio: nemici congelati'
};
