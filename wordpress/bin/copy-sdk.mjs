// Copies the Turing JS SDK's ES module into src/admin/build/, where the plugin registers it
// as the `@viglet/turing-sdk` script module. The SDK is one zero-dependency file, so it is
// shipped as published rather than rebundled. Run by `npm run build`.
import { copyFileSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
// The SDK's `exports` map hides package.json from require.resolve, so read it in place.
const sdkDir = join( root, 'node_modules', '@viglet', 'turing-sdk' );
const { version } = JSON.parse(
	readFileSync( join( sdkDir, 'package.json' ), 'utf8' )
);

const out = join( root, 'src', 'admin', 'build' );
mkdirSync( out, { recursive: true } );
copyFileSync(
	join( sdkDir, 'dist', 'turing-sdk.js' ),
	join( out, 'turing-sdk.js' )
);
// The version the PHP side registers the module with, so a new SDK busts browser caches.
writeFileSync(
	join( out, 'turing-sdk.asset.php' ),
	`<?php return array( 'version' => '${ version }' );\n`
);
process.stdout.write(
	`@viglet/turing-sdk ${ version } -> src/admin/build/turing-sdk.js\n`
);
