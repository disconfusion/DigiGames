/** Utility per le icone pixel-art.
 *  Una "mappa" è un array di righe di caratteri: ' ' o '.' = pixel trasparente,
 *  ogni altro carattere è una chiave nella palette → colore di quel pixel. */
export type Palette = Record<string, string>;

export interface Cell {
	x: number;
	y: number;
	fill: string;
}

/** Espande una mappa pixel in celle 1×1 pronte per i <rect> SVG. */
export function toRects(map: string[], palette: Palette): Cell[] {
	const cells: Cell[] = [];
	for (let y = 0; y < map.length; y++) {
		const row = map[y];
		for (let x = 0; x < row.length; x++) {
			const ch = row[x];
			if (ch === ' ' || ch === '.') continue;
			const fill = palette[ch];
			if (fill) cells.push({ x, y, fill });
		}
	}
	return cells;
}

/** Scorciatoie verso i token del tema CRT (vedi retro-crt-theme.css). */
export const C = {
	cyan: 'var(--cyan)',
	mag: 'var(--accent)',
	amber: 'var(--amber)',
	green: 'var(--green)',
	danger: 'var(--danger)',
	text: 'var(--text)',
	muted: 'var(--muted)',
	line: 'var(--line)',
	inset: 'var(--inset)'
} as const;
