#!/usr/bin/env node
// D317: every translatable string the plugin ships is translated to pt_BR, and the committed
// .pot is the one the source produces today. The pt-br browser spec only notices a string the
// .po already translates, so a new string, or one the .po never received, reached pt_BR sites
// in English unseen.
//
//   npm run check:i18n              regenerate the .pot (WP-CLI in Docker) and check both
//   npm run check:i18n -- --no-pot  check the committed .po against the committed .pot only
//
// Fails naming each string, so a task sees it before committing. The README's "Translations"
// section says how to regenerate and translate.
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
const languages = join( root, 'src', 'languages' );
const potFile = join( languages, 'viglet-turing-search.pot' );
const poFile = join( languages, 'viglet-turing-search-pt_BR.po' );

/**
 * The entries of a .po or .pot: msgid (with its context) to its translations, continuation
 * lines joined. The header (an empty msgid) is left out.
 *
 * @param {string} source The file's text.
 * @return {Map<string, string[]>} Each key to its msgstr values (one, or one per plural form).
 */
export function entries( source ) {
	const found = new Map();
	let current = null;
	let field = null;
	const unquote = ( s ) => JSON.parse( s );
	const close = () => {
		if ( current && '' !== current.msgid ) {
			const key = `${ current.msgctxt ?? '' }\u0004${ current.msgid }`;
			found.set( key, current.msgstr );
		}
	};
	for ( const line of source.split( /\r?\n/ ) ) {
		const start =
			/^(msgctxt|msgid|msgid_plural|msgstr(?:\[(\d+)\])?) (".*")$/.exec(
				line
			);
		if ( start ) {
			if (
				'msgctxt' === start[ 1 ] ||
				( 'msgid' === start[ 1 ] && current?.msgstr.length )
			) {
				close();
				current = null;
			}
			current ??= { msgid: '', msgstr: [] };
			if ( start[ 1 ].startsWith( 'msgstr' ) ) {
				field = { msgstr: Number( start[ 2 ] ?? 0 ) };
				current.msgstr[ field.msgstr ] = unquote( start[ 3 ] );
			} else {
				field = start[ 1 ];
				current[ field ] = unquote( start[ 3 ] );
			}
		} else if ( current && field && /^".*"$/.test( line ) ) {
			if ( 'object' === typeof field ) {
				current.msgstr[ field.msgstr ] += unquote( line );
			} else {
				current[ field ] += unquote( line );
			}
		} else {
			field = null;
		}
	}
	close();
	return found;
}

/**
 * A key as a reader names it: the msgid, and its context when it has one.
 *
 * @param {string} key An entries() key.
 */
const shown = ( key ) => {
	const [ context, msgid ] = key.split( '\u0004' );
	return context ? `${ msgid } (context: ${ context })` : msgid;
};

/**
 * The .pot the source produces today, through the README's WP-CLI in Docker.
 */
function freshPot() {
	const out = mkdtempSync( join( tmpdir(), 'viglet-i18n-' ) );
	try {
		execFileSync(
			'docker',
			[
				'run',
				'--rm',
				'-v',
				`${ root }:/plugin`,
				'-v',
				`${ out }:/out`,
				'-w',
				'/plugin',
				'wordpress:cli-2.12-php8.5',
				'wp',
				'i18n',
				'make-pot',
				'.',
				'/out/fresh.pot',
				'--slug=viglet-turing-search',
				'--domain=viglet-turing-search',
				'--exclude=node_modules,vendor,src/admin/build,tests,build,demo',
			],
			{ stdio: [ 'ignore', 'ignore', 'inherit' ] }
		);
		return readFileSync( join( out, 'fresh.pot' ), 'utf8' );
	} finally {
		rmSync( out, { recursive: true, force: true } );
	}
}

function main() {
	const problems = [];
	const committed = entries( readFileSync( potFile, 'utf8' ) );
	const translations = entries( readFileSync( poFile, 'utf8' ) );

	if ( ! process.argv.includes( '--no-pot' ) ) {
		const fresh = entries( freshPot() );
		for ( const key of fresh.keys() ) {
			if ( ! committed.has( key ) ) {
				problems.push( `not in the committed .pot: ${ shown( key ) }` );
			}
		}
		for ( const key of committed.keys() ) {
			if ( ! fresh.has( key ) ) {
				problems.push(
					`in the .pot but no longer in the source: ${ shown( key ) }`
				);
			}
		}
	}

	for ( const key of committed.keys() ) {
		const msgstr = translations.get( key );
		if ( ! msgstr ) {
			problems.push( `missing from the pt_BR .po: ${ shown( key ) }` );
		} else if ( ! msgstr.length || msgstr.some( ( s ) => ! s ) ) {
			problems.push( `untranslated in pt_BR: ${ shown( key ) }` );
		}
	}

	if ( problems.length ) {
		process.stderr.write(
			`${ problems.length } translation problem(s) in ${ relative( root, languages ) }:\n`
		);
		problems.forEach( ( p ) => process.stderr.write( `  ${ p }\n` ) );
		process.stderr.write(
			'Regenerate and translate as the README\'s "Translations" section says.\n'
		);
		process.exit( 1 );
	}
	process.stdout.write(
		`${ committed.size } strings, all translated to pt_BR.\n`
	);
}

if ( process.argv[ 1 ] === fileURLToPath( import.meta.url ) ) {
	main();
}
