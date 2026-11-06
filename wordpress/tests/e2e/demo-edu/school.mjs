// What the school's specs share (D325): WP-CLI on the school's own WordPress, what the last setup
// recorded (its language, and each seeded post's id and title by key), the words the plugin
// shows in the school's language, and the titles a search page lists.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect } from '@playwright/test';
import { wp } from '../global-setup.mjs';
import { demoSchool } from '../../../playwright.config.mjs';

const root = join( import.meta.dirname, '..', '..', '..' );

/** Where setup/demo-edu.setup.mjs writes what it seeded. */
export const schoolFile = join(
	root,
	'tests',
	'e2e',
	'.auth',
	'demo-edu.json'
);

/**
 * Run WP-CLI on the school's WordPress.
 *
 * @param {string[]} args The wp arguments.
 */
export const onSchool = ( args ) => wp( args, demoSchool.cli );

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
export const ask = ( args ) =>
	onSchool( args ).trim().split( /\r?\n/ ).pop().trim();

/**
 * @return {{locale: string, pages: Object<string, number>, keys: Object<string, {id: number, title: string}>}}
 * What the last setup seeded: the school's language, its pages, and every post and file by key.
 */
export const seeded = () => JSON.parse( readFileSync( schoolFile, 'utf8' ) );

/**
 * A seeded post's or file's id, by the key the seed keeps in `_lumen_key`.
 *
 * @param {string} key Its key, e.g. `science-fair`, or `sample-tide-atlas` for a book's PDF.
 * @return {string} Its id, as WP-CLI takes it.
 */
export const idOf = ( key ) => String( seeded().keys[ key ].id );

/**
 * A seeded post's or file's title in the school's language, by its key.
 *
 * @param {string} key Its key.
 * @return {string} Its title.
 */
export const titleOf = ( key ) => seeded().keys[ key ].title;

const english = {
	periods: {
		today: 'Today',
		week: 'Next 7 days',
		month: 'This month',
		upcoming: 'Upcoming',
		past: 'Past',
	},
	when: 'When',
	months: [
		'January',
		'February',
		'March',
		'April',
		'May',
		'June',
		'July',
		'August',
		'September',
		'October',
		'November',
		'December',
	],
	in: 'in',
	pdf: 'PDF',
	marker: ( chapter, name ) => `Story chapter ${ chapter }: ${ name }`,
	features: [
		'An agenda, not an archive',
		'Say when, in words',
		'The card shows the event',
	],
};
const portuguese = {
	periods: {
		today: 'Hoje',
		week: 'Próximos 7 dias',
		month: 'Este mês',
		upcoming: 'Próximos',
		past: 'Passados',
	},
	when: 'Quando',
	months: [
		'janeiro',
		'fevereiro',
		'março',
		'abril',
		'maio',
		'junho',
		'julho',
		'agosto',
		'setembro',
		'outubro',
		'novembro',
		'dezembro',
	],
	in: 'em',
	pdf: 'PDF',
	marker: ( chapter, name ) => `Capítulo ${ chapter } da história: ${ name }`,
	features: [
		'Uma agenda, não um arquivo',
		'Diga quando, em palavras',
		'O card mostra o evento',
	],
};

/** @return {Object} The plugin's and the theme's words in the school's language. */
export const say = () => ( 'pt_BR' === seeded().locale ? portuguese : english );

/**
 * Open a page of the school with a query and filters, wait for its results, and return the
 * titles it lists.
 *
 * @param {import('@playwright/test').Page} page  The page.
 * @param {string}                          path  The page's address, e.g. `/agenda/`.
 * @param {string}                          q     The query.
 * @param {string[]}                        [fqs] Filter queries.
 * @return {Promise<string[]>} The titles listed.
 */
export async function titlesOn( page, path, q, fqs = [] ) {
	const params = new URLSearchParams( { q } );
	fqs.forEach( ( fq ) => params.append( 'fq[]', fq ) );
	await page.goto( `${ path }?${ params }` );
	await expect( page.locator( '.viglet-turing-search__summary' ) ).toHaveText(
		/result/
	);
	return page
		.locator(
			'.viglet-turing-search__results .viglet-turing-search__title'
		)
		.allTextContents();
}

/**
 * The titles a search lists once Turing has had time to commit what was just sent.
 *
 * @param {import('@playwright/test').Page} page The page.
 * @param {string}                          path The page's address.
 * @param {string}                          q    The query.
 */
export async function titlesAfterCommit( page, path, q ) {
	await page.waitForTimeout( 4000 );
	return titlesOn( page, path, q );
}

/**
 * Send the queue now, as the worker would. The cron service may be sending it at that very
 * moment, and then the run refuses ("Another worker"): wait for that one and send again.
 *
 * @param {import('@playwright/test').Page} page The page, for its clock.
 */
export async function drainQueue( page ) {
	try {
		onSchool( [ 'turing', 'queue', 'run' ] );
	} catch {
		await page.waitForTimeout( 5000 );
		onSchool( [ 'turing', 'queue', 'run' ] );
	}
}
