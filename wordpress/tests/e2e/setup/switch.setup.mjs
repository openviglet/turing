// Turns the stack's WordPress into the variant the next project tests: the theme and the
// language named in that project's `metadata` (playwright.config.mjs). global-setup's seed
// puts back Twenty Twenty-Five in English at the start of every run.
import { test } from '@playwright/test';
import { wp } from '../global-setup.mjs';

test( 'switch the site', ( {}, testInfo ) => {
	const { theme, version, locale } = testInfo.project.metadata;
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
} );
