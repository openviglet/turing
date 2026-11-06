// The demo storefront's curated searches (demo/theme/tarnfell/curated.json) and the language the
// demo speaks now (D217), which setup/demo.setup.mjs records: an entry with no `locale` is
// English, and only the entries in the demo's language are checked against it.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = join( import.meta.dirname, '..', '..', '..' );

/** Where demo.setup.mjs writes the demo's locale. */
export const demoFile = join( root, 'tests', 'e2e', '.auth', 'demo.json' );

/** Every curated search, in every language. */
export const curated = JSON.parse(
	readFileSync(
		join( root, 'demo', 'theme', 'tarnfell', 'curated.json' ),
		'utf8'
	)
);

/** @return {string} The demo's locale, as the last demo setup read it. */
export function demoLocale() {
	return JSON.parse( readFileSync( demoFile, 'utf8' ) ).locale;
}

/**
 * @param {{locale?: string}} search A curated search.
 * @return {string} Its language.
 */
export const localeOf = ( search ) => search.locale || 'en_US';
