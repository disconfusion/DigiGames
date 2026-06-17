/** Catalogo giochi condiviso (label + emoji) per home, lobby e inviti. */
export const GAME_CATALOG: { slug: string; label: string; emoji: string }[] = [
	{ slug: 'connect4', label: 'Forza 4', emoji: '🔴' },
	{ slug: 'hangman', label: 'Impiccato', emoji: '🔤' },
	{ slug: 'quiz', label: 'Quiz', emoji: '❓' },
	{ slug: 'battleship', label: 'Battaglia navale', emoji: '🚢' },
	{ slug: 'minesweeper', label: 'Campo minato', emoji: '💣' },
	{ slug: 'tris', label: 'Tris', emoji: '❌' },
	{ slug: 'dama', label: 'Dama', emoji: '⛀' }
];

export const gameLabel = (slug: string): string =>
	GAME_CATALOG.find((g) => g.slug === slug)?.label ?? slug;
