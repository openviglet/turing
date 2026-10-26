// Turns the stack's WordPress into the variant the next project tests: the theme, the language
// and the plugins named in that project's `metadata` (playwright.config.mjs), then that
// variant's own seed, reindexed. global-setup puts back Twenty Twenty-Five in English with no
// other plugin at the start of every run.
import { test } from '@playwright/test';
import { wp } from '../global-setup.mjs';

test( 'switch the site', async ( {}, testInfo ) => {
	const { theme, version, locale, plugins, seed } = testInfo.project.metadata;
	for ( const plugin of plugins || [] ) {
		try {
			wp( [ 'plugin', 'is-installed', plugin ] );
		} catch {
			wp( [ 'plugin', 'install', plugin ] );
		}
		wp( [ 'plugin', 'activate', plugin ] );
	}
	if ( theme ) {
		try {
			wp( [ 'theme', 'is-installed', theme ] );
		} catch {
			wp( [
				'theme',
				'install',
				theme,
				...( version ? [ `--version=${ version }` ] : [] ),
			] );
		}
		wp( [ 'theme', 'activate', theme ] );
	}
	if ( locale ) {
		try {
			wp( [ 'language', 'core', 'is-installed', locale ] );
		} catch {
			wp( [ 'language', 'core', 'install', locale ] );
		}
		wp( [ 'site', 'switch-language', locale ] );
		// The admin follows the site's language only while it has none of its own.
		try {
			wp( [ 'user', 'meta', 'delete', 'admin', 'locale' ] );
		} catch {}
	}
	if ( seed ) {
		wp( [ 'eval-file', seed ] );
		wp( [ 'dumont', 'reindex' ] );
		// Turing commits within a few seconds; the variant's specs read what was just indexed.
		await new Promise( ( resolve ) => setTimeout( resolve, 5000 ) );
	}
} );
