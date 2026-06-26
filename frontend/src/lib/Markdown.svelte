<script lang="ts">
	import { marked } from 'marked';
	import DOMPurify from 'dompurify';

	let { source = '' }: { source?: string } = $props();

	// marked sincrono; sanitizzazione client-side (SPA ssr=false)
	const html = $derived(source ? DOMPurify.sanitize(marked.parse(source, { async: false }) as string) : '');
</script>

{#if html}
	<div class="md">{@html html}</div>
{/if}

<style>
	.md {
		color: var(--text);
		font-size: 1rem;
		line-height: 1.7;
		word-wrap: break-word;
		overflow-wrap: break-word;
	}
	.md :global(h1),
	.md :global(h2),
	.md :global(h3),
	.md :global(h4) {
		color: var(--accent);
		line-height: 1.3;
		margin: 1.4em 0 0.6em;
		text-shadow: 0 0 8px rgba(255, 46, 136, 0.4);
	}
	.md :global(h1) { font-size: 1.5rem; }
	.md :global(h2) { font-size: 1.25rem; }
	.md :global(h3) { font-size: 1.1rem; }
	.md :global(h1:first-child),
	.md :global(h2:first-child),
	.md :global(h3:first-child) { margin-top: 0; }
	.md :global(p) { margin: 0.6em 0; }
	.md :global(a) {
		color: var(--cyan);
		text-decoration: underline;
	}
	.md :global(a:hover) { color: var(--accent); }
	.md :global(ul),
	.md :global(ol) {
		margin: 0.6em 0;
		padding-left: 1.5em;
	}
	.md :global(li) { margin: 0.3em 0; }
	.md :global(li::marker) { color: var(--cyan); }
	.md :global(strong) { color: var(--amber); }
	.md :global(em) { color: var(--green); }
	.md :global(code) {
		font-family: 'VT323', monospace;
		font-size: 1.05em;
		background: var(--inset);
		padding: 0.1em 0.4em;
		border-radius: 4px;
		border: 1px solid var(--line);
		color: var(--cyan);
	}
	.md :global(pre) {
		background: var(--inset);
		border: 1px solid var(--line);
		border-radius: 8px;
		padding: 1em;
		overflow-x: auto;
		margin: 0.8em 0;
	}
	.md :global(pre code) {
		background: none;
		border: none;
		padding: 0;
	}
	.md :global(blockquote) {
		border-left: 3px solid var(--accent);
		margin: 0.8em 0;
		padding: 0.2em 0 0.2em 1em;
		color: var(--muted);
	}
	.md :global(hr) {
		border: none;
		border-top: 1px solid var(--line);
		margin: 1.5em 0;
	}
	.md :global(table) {
		border-collapse: collapse;
		margin: 0.8em 0;
		width: 100%;
		display: block;
		overflow-x: auto;
	}
	.md :global(th),
	.md :global(td) {
		border: 1px solid var(--line);
		padding: 0.4em 0.7em;
		text-align: left;
	}
	.md :global(th) {
		background: var(--inset);
		color: var(--cyan);
	}
	.md :global(input[type='checkbox']) {
		accent-color: var(--accent);
		margin-right: 0.4em;
	}
</style>
