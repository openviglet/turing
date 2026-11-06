// Copies the Turing JS SDK's ES module into src/admin/build/, where the plugin registers it
// as the `@viglet/turing-sdk` script module. The SDK is one zero-dependency file, so it is
// shipped as built rather than rebundled. Run by `npm run build`.
//
// The SDK is read from this repository's workspace (frontend/packages/sdk), not from npm,
// so the plugin ships the SDK of its own commit. A checkout with no SDK build gets one here.
import { execSync } from 'node:child_process';
import {
	copyFileSync,
	existsSync,
	mkdirSync,
	readFileSync,
	writeFileSync,
} from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
const frontend = join( root, '..', 'frontend' );
const sdkDir = join( frontend, 'packages', 'sdk' );
const sdkFile = join( sdkDir, 'dist', 'turing-sdk.js' );
if ( ! existsSync( sdkFile ) ) {
	process.stdout.write(
		'No SDK build yet: building @viglet/turing-sdk in frontend/\n'
	);
	try {
		execSync( 'pnpm --filter @viglet/turing-sdk build', {
			cwd: frontend,
			stdio: 'inherit',
		} );
	} catch {
		process.stderr.write(
			'Could not build the SDK: run `pnpm install` and `pnpm --filter @viglet/turing-sdk build` in frontend/.\n'
		);
		process.exit( 1 );
	}
}
const { version } = JSON.parse(
	readFileSync( join( sdkDir, 'package.json' ), 'utf8' )
);

const out = join( root, 'src', 'admin', 'build' );
mkdirSync( out, { recursive: true } );
copyFileSync( sdkFile, join( out, 'turing-sdk.js' ) );
// The version the PHP side registers the module with, so a new SDK busts browser caches.
writeFileSync(
	join( out, 'turing-sdk.asset.php' ),
	`<?php return array( 'version' => '${ version }' );\n`
);
process.stdout.write(
	`@viglet/turing-sdk ${ version } -> src/admin/build/turing-sdk.js\n`
);
