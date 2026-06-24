import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import { execSync } from 'child_process';

let gitCommit = 'dev';
let whatsnew: string[] = [];
try {
	gitCommit = execSync('git rev-parse --short HEAD').toString().trim();
	const msgs = execSync('git log --format=%s -40').toString().trim().split('\n');
	whatsnew = msgs
		.filter((m) => /^feat/i.test(m))
		.map((m) => m.replace(/^feat.*?:\s*/i, '').trim())
		.filter(Boolean)
		.slice(0, 8);
} catch { /* git non disponibile (CI senza repo) */ }

export default defineConfig({
	define: {
		__GIT_COMMIT__: JSON.stringify(gitCommit),
		__WHATSNEW__: JSON.stringify(whatsnew)
	},
	plugins: [
		sveltekit({
			compilerOptions: {
				runes: ({ filename }) =>
					filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},
			adapter: adapter({
				pages: 'build',
				assets: 'build',
				fallback: 'index.html',
				precompress: false
			})
		})
	]
});
