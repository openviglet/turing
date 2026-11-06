// Before the school's specs (D325): the demo school on its own WordPress (run-docker.cmd demo edu),
// checked rather than built, then seeded again and reindexed. Unlike the store's catalog, the
// school's seed is cheap and its dates are counted from today, so a run always meets an agenda
// whose "Today" is today. The default site and the stores are never touched.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { test } from '@playwright/test';
import { assertSameSite } from '../global-setup.mjs';
import { schoolFile, onSchool } from '../demo-edu/school.mjs';
import { demoSchool, schoolURL } from '../../../playwright.config.mjs';

test( 'the demo school is up, seeded from today and indexed', async () => {
	test.setTimeout( 5 * 60 * 1000 );
	try {
		onSchool( [ 'core', 'is-installed' ] );
	} catch {
		throw new Error(
			`The demo school is not installed: run \`run-docker.cmd demo ${ demoSchool.key }\` first.`
		);
	}
	assertSameSite( schoolURL, demoSchool.cli, 'WP_DEMO_EDU_URL' );
	const out = onSchool( [ 'eval-file', demoSchool.seed ] );
	const seeded = JSON.parse( out.slice( out.indexOf( '{' ) ) );
	const locale = onSchool( [ 'eval', 'echo get_locale();' ] )
		.trim()
		.split( /\s+/ )
		.pop();
	mkdirSync( dirname( schoolFile ), { recursive: true } );
	writeFileSync( schoolFile, JSON.stringify( { locale, ...seeded } ) );
	onSchool( [ 'dumont', 'reindex' ] );
	// Turing commits within a few seconds; the specs read what was just indexed.
	await new Promise( ( resolve ) => setTimeout( resolve, 5000 ) );
} );
