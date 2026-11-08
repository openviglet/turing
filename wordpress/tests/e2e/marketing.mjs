// The demo videos are made in the marketing repository (marketing/turing/wordpress), from the
// stories recorded here: their narration paces a recording, and a recording is written there
// for the video to be assembled from. Found beside the Turing repository (D:\Git\viglet\marketing
// next to D:\Git\viglet\turing\<version>) or named by VIGLET_MARKETING_WORDPRESS. Only a
// recording needs it; the stories run as tests without it.
import { existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const plugin = resolve(
	dirname( fileURLToPath( import.meta.url ) ),
	'..',
	'..'
);

export const marketing = process.env.VIGLET_MARKETING_WORDPRESS
	? resolve( process.env.VIGLET_MARKETING_WORDPRESS )
	: resolve( plugin, '..', '..', '..', 'marketing', 'turing', 'wordpress' );

/**
 * A video's narration (its lines, where each take lives, whether a take is current), from the
 * marketing repository's bin/narration-takes.mjs.
 *
 * @param {'story'|'developer'} story Which video's narration.
 */
export async function narrationFor( story ) {
	const file = join( marketing, 'bin', 'narration-takes.mjs' );
	if ( ! existsSync( file ) ) {
		throw new Error(
			`A recording is paced to the video's narration, in the marketing repository, which is not at ${ marketing }: clone it there or set VIGLET_MARKETING_WORDPRESS.`
		);
	}
	const takes = await import( pathToFileURL( file ).href );
	return takes.narrationFor( story );
}

/**
 * Where a recording is written, for the marketing repository to assemble.
 *
 * @param {string} folder The recording's folder: story, story-vertical or developer.
 */
export const recording = ( folder ) => join( marketing, 'recordings', folder );
