/** Catalogo giochi condiviso (label) per home, lobby e inviti.
 *  Le icone sono pixel-art animate, risolte per slug da $lib/icons (vedi sprites.ts). */
export const GAME_CATALOG: { slug: string; label: string }[] = [
	{ slug: 'connect4', label: 'Forza 4' },
	{ slug: 'hangman', label: 'Impiccato' },
	{ slug: 'quiz', label: 'Quiz' },
	{ slug: 'battleship', label: 'Battaglia navale' },
	{ slug: 'minesweeper', label: 'Campo minato' },
	{ slug: 'tris', label: 'Tris' },
	{ slug: 'dama', label: 'Dama' },
	{ slug: 'chess', label: 'Scacchi' }
];

export const gameLabel = (slug: string): string =>
	GAME_CATALOG.find((g) => g.slug === slug)?.label ?? slug;
