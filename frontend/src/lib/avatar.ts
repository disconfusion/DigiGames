/** Avatar ASCII componibile: cappello, occhi, naso, bocca. Tutto monospace. */

export const EYES = [
	'o o', 'O O', '^ ^', '- -', '* *', '@ @', '> <', 'x x',
	'. .', 'q p', 'T T', '$ $', '/ \\', '# #', 'u u', 'O o'
];

export const NOSES = ['   ', ' . ', ' L ', ' > ', ' ^ ', ' v ', ' 7 ', ' i ', ' c ', ' o '];

export const MOUTHS = [
	'___', ' o ', 'vvv', '   ', ' u ', '===', ' ~ ', ' D ',
	'> <', ' 3 ', ' p ', 'www', ' v ', ' x ', ' O ', '---'
];

export const HATS = [
	'       ', // niente
	'  ___  ',
	' /===\\ ',
	' _---_ ',
	' \\\\|// ',
	' (###) ',
	' .-^-. ',
	' <===> ',
	' vvv  '
];

export type AvatarSpec = { eyes: number; nose: number; mouth: number; hat: number };

export const DEFAULT_AVATAR: AvatarSpec = { eyes: 0, nose: 0, mouth: 0, hat: 0 };

const clamp = (v: unknown, max: number): number => {
	const n = typeof v === 'number' ? v : 0;
	return Number.isFinite(n) && n >= 0 && n < max ? Math.floor(n) : 0;
};

/** Parsa la spec memorizzata (stringa JSON) tornando sempre una spec valida. */
export function parseAvatar(raw: string | null | undefined): AvatarSpec {
	if (!raw) return { ...DEFAULT_AVATAR };
	try {
		const o = JSON.parse(raw);
		return {
			eyes: clamp(o.eyes, EYES.length),
			nose: clamp(o.nose, NOSES.length),
			mouth: clamp(o.mouth, MOUTHS.length),
			hat: clamp(o.hat, HATS.length)
		};
	} catch {
		return { ...DEFAULT_AVATAR };
	}
}

export function serializeAvatar(spec: AvatarSpec): string {
	return JSON.stringify(spec);
}

/** Rende l'avatar come faccia ASCII multi-riga (monospace). */
export function renderAvatar(spec: AvatarSpec): string {
	const eyes = EYES[clamp(spec.eyes, EYES.length)];
	const nose = NOSES[clamp(spec.nose, NOSES.length)];
	const mouth = MOUTHS[clamp(spec.mouth, MOUTHS.length)];
	const hat = HATS[clamp(spec.hat, HATS.length)];

	const lines: string[] = [];
	if (spec.hat > 0) lines.push(hat);
	lines.push('.-----.');
	lines.push(`| ${eyes} |`);
	lines.push(`| ${nose} |`);
	lines.push(`| ${mouth} |`);
	lines.push("'-----'");
	return lines.join('\n');
}
