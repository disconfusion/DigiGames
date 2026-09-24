import type { Side } from '$lib/theme.svelte';

/** Catalogo giochi condiviso (label) per home, lobby e inviti.
 *  Le icone sono pixel-art animate, risolte per slug da $lib/icons (vedi sprites.ts).
 *  `side`: lato del sito in cui il gioco è proposto (default sala giochi, vedi theme.svelte.ts). */
export const GAME_CATALOG: { slug: string; label: string; side?: Side }[] = [
	{ slug: 'connect4', label: 'Forza 4' },
	{ slug: 'hangman', label: 'Impiccato' },
	{ slug: 'quiz', label: 'Quiz' },
	{ slug: 'battleship', label: 'Battaglia navale' },
	{ slug: 'minesweeper', label: 'Campo minato' },
	{ slug: 'tris', label: 'Tris' },
	{ slug: 'dama', label: 'Dama' },
	{ slug: 'chess', label: 'Scacchi' },
	{ slug: 'pong', label: 'Pong' },
	{ slug: 'battlecity', label: 'Battle City' },
	{ slug: 'poker', label: 'Poker', side: 'casino' }
];

export const gameLabel = (slug: string): string =>
	GAME_CATALOG.find((g) => g.slug === slug)?.label ?? slug;

export const sideOf = (slug: string): Side =>
	GAME_CATALOG.find((g) => g.slug === slug)?.side ?? 'arcade';

/** Giochi proposti nel lato indicato (creazione stanze, inviti, segnalazioni). */
export const gamesFor = (side: Side) => GAME_CATALOG.filter((g) => sideOf(g.slug) === side);

/** Nome del gioco visto da chi sta nel lato indicato: i tavoli del casinò restano un
 *  segreto per chi è in sala giochi (es. invito a Poker ricevuto su DigiGames). */
export const gameLabelFor = (slug: string, side: Side): string =>
	sideOf(slug) === 'casino' && side !== 'casino' ? 'un tavolo segreto' : gameLabel(slug);
