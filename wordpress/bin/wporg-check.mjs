// The WordPress.org review's findings held as a gate. The pre-review of 3 Nov 2026 flagged
// unescaped output, nonces, bundled translations and load_plugin_textdomain() in a package that
// PHPCS and Plugin Check had both passed: they honour `phpcs:ignore`, the reviewers' tools do not,
// and the zip uploaded was the GitHub one. So this checks what the review reads.
//
//   node bin/wporg-check.mjs                      the source rules
//   node bin/wporg-check.mjs --package <folder>   and the unzipped WordPress.org package
//                                                 (bin/build-zip.sh --wporg)
//
// Exits non-zero, naming each finding, on any of them.
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join( fileURLToPath( import.meta.url ), '..', '..' );
const src = join( root, 'src' );
const findings = [];
const find = ( file, line, text ) =>
	findings.push(
		`${ relative( root, file ).replace( /\\/g, '/' ) }${
			line ? `:${ line }` : ''
		}: ${ text }`
	);

/**
 * Every PHP file under a folder, the admin build left out (generated).
 *
 * @param {string} dir The folder.
 * @return {string[]} The files.
 */
const phpFiles = ( dir ) =>
	readdirSync( dir ).flatMap( ( name ) => {
		const path = join( dir, name );
		if ( statSync( path ).isDirectory() ) {
			return 'build' === name ? [] : phpFiles( path );
		}
		return name.endsWith( '.php' ) ? [ path ] : [];
	} );

// Output printed unescaped on purpose, each with the reason a reviewer gets. A new one fails here
// until it is escaped, or added with its reason and said in the reply to the review.
const unescapedAllowed = {
	// A block that wraps others prints $content, rendered and escaped by its inner blocks.
	'src/blocks/search/render.php': 1,
	// The assistant block's own render.php escapes it; kses would strip its dialog and directives.
	'src/includes/class-viglet-dumont-assistant.php': 1,
};
// Where load_plugin_textdomain() may stand: the loader the WordPress.org package leaves out.
const textdomainAllowed = new Set( [
	'src/includes/bundled-translations.php',
] );
// Where an admin notice may be hooked (Guideline 11): the plugin's own screens only.
const noticesAllowed = new Set( [
	'src/includes/class-viglet-dumont-admin.php',
] );

const unescaped = {};
for ( const file of phpFiles( src ) ) {
	const name = relative( root, file ).replace( /\\/g, '/' );
	readFileSync( file, 'utf8' )
		.split( '\n' )
		.forEach( ( text, n ) => {
			const line = n + 1;
			const silencer = text.match(
				/phpcs:(?:ignore|disable)\b[^\n]*WordPress\.Security\.[\w.]+/
			);
			if ( silencer && ! /\s--\s*\S/.test( text ) ) {
				find(
					file,
					line,
					'a WordPress.Security silencer with no reason: add " -- why" on the same line'
				);
			}
			if (
				/phpcs:(?:ignore|disable)\b[^\n]*WordPress\.Security\.EscapeOutput/.test(
					text
				)
			) {
				unescaped[ name ] = ( unescaped[ name ] ?? 0 ) + 1;
			}
			if (
				/\bload_plugin_textdomain\s*\(/.test( text ) &&
				! textdomainAllowed.has( name )
			) {
				find(
					file,
					line,
					'load_plugin_textdomain() outside bundled-translations.php: WordPress.org loads the translations itself'
				);
			}
			if (
				/['"](?:network_)?admin_notices['"]/.test( text ) &&
				! noticesAllowed.has( name )
			) {
				find(
					file,
					line,
					"an admin notice outside the admin class (Guideline 11): keep notices to the plugin's own screens"
				);
			}
		} );
}
for ( const [ name, count ] of Object.entries( unescaped ) ) {
	if ( count > ( unescapedAllowed[ name ] ?? 0 ) ) {
		find(
			join( root, name ),
			0,
			`${ count } unescaped output silencer(s), ${
				unescapedAllowed[ name ] ?? 0
			} allowed: escape the output (esc_html, esc_attr, esc_url, wp_kses_post)`
		);
	}
}

// The package as uploaded: unzipped bin/build-zip.sh --wporg output.
const packageAt = process.argv.includes( '--package' )
	? process.argv[ process.argv.indexOf( '--package' ) + 1 ]
	: null;
if ( packageAt ) {
	if ( ! existsSync( join( packageAt, 'viglet-dumont.php' ) ) ) {
		find(
			packageAt,
			0,
			'not the plugin folder: pass the viglet-turing-search/ folder inside the zip'
		);
	} else {
		const files = ( dir ) =>
			readdirSync( dir ).flatMap( ( name ) => {
				const path = join( dir, name );
				return statSync( path ).isDirectory()
					? files( path )
					: [ path ];
			} );
		for ( const file of files( packageAt ) ) {
			const name = relative( packageAt, file ).replace( /\\/g, '/' );
			if ( /\.(po|mo|l10n\.php)$|^languages\/.+\.json$/.test( name ) ) {
				find(
					file,
					0,
					'a translation file in the package: translate.wordpress.org serves them (build with --wporg)'
				);
			}
			if ( 'includes/bundled-translations.php' === name ) {
				find(
					file,
					0,
					'the bundled-translations loader is in the package: build it with bin/build-zip.sh --wporg'
				);
			}
			if (
				name.endsWith( '.php' ) &&
				/\bload_plugin_textdomain\s*\(/.test(
					readFileSync( file, 'utf8' )
				)
			) {
				find(
					file,
					0,
					'load_plugin_textdomain() in the package: not needed on WordPress.org'
				);
			}
		}
		const readme = join( packageAt, 'readme.txt' );
		const version = readFileSync(
			join( packageAt, 'viglet-dumont.php' ),
			'utf8'
		).match( /^\s*\*\s*Version:\s*(\S+)/m )?.[ 1 ];
		const stable = existsSync( readme )
			? readFileSync( readme, 'utf8' ).match(
					/^Stable tag:\s*(\S+)/m
				)?.[ 1 ]
			: null;
		if ( ! stable ) {
			find( readme, 0, 'no readme.txt with a Stable tag in the package' );
		} else if ( stable !== version ) {
			find(
				readme,
				0,
				`Stable tag ${ stable } is not the plugin's Version ${ version }`
			);
		}
	}
}

if ( findings.length ) {
	process.stderr.write(
		`wporg-check: ${ findings.length } finding(s)\n${ findings
			.map( ( f ) => `  ${ f }` )
			.join( '\n' ) }\n`
	);
	process.exit( 1 );
}
process.stdout.write(
	`wporg-check: no finding in the source${
		packageAt ? ' or the package' : ''
	}\n`
);
