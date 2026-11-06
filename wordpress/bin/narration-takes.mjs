// The demo video's narration lines and their takes (D246), shared by story-narration.mjs,
// which buys them, and story-video.mjs, which places them. Ana's story is
// demo/narration/script.json; the developer walkthrough (D281) is developer.json, with its own
// voice and its takes under demo/narration/developer/.
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

export const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
export const narration = join( root, 'demo', 'narration' );

/**
 * One narration: its script, its lines, where each take lives and whether a take is current.
 *
 * @param {'story'|'developer'} story Which video's narration.
 */
export function narrationFor( story = 'story' ) {
	const script = JSON.parse(
		readFileSync(
			join(
				narration,
				'story' === story ? 'script.json' : `${ story }.json`
			),
			'utf8'
		)
	);
	const takes = 'story' === story ? narration : join( narration, story );
	return {
		script,
		/**
		 * Every line the video speaks, by the file its take goes to: the opening over the first
		 * card, each chapter's line over its card and the one about its result, and the closing.
		 *
		 * @return {Array<[string, Object<string, string>]>} Each take's name and its text by language.
		 */
		lines: () => [
			[ 'intro', script.intro ],
			...Object.entries( script.chapters ).flatMap(
				( [ n, chapter ] ) => [
					[ `chapter-${ n }`, chapter.card ],
					[ `chapter-${ n }-result`, chapter.result ],
				]
			),
			[ 'outro', script.outro ],
		],
		/**
		 * Where a take lives.
		 *
		 * @param {string} language The language, as the script names it.
		 * @param {string} name     The take's name.
		 */
		takeFile: ( language, name ) =>
			join( takes, language, `${ name }.mp3` ),
		/**
		 * Whether a take already says this line, in this voice and model: a take of an older
		 * script is neither placed in the video nor kept from being bought again.
		 *
		 * @param {string} file The take.
		 * @param {string} text The line it must say.
		 */
		current: ( file, text ) => {
			const record = `${ file }.prov.json`;
			if ( ! existsSync( file ) || ! existsSync( record ) ) {
				return false;
			}
			const prov = JSON.parse( readFileSync( record, 'utf8' ) );
			return (
				prov.prompt === text &&
				prov.details?.voice === script.voice &&
				prov.engine?.model === script.model
			);
		},
	};
}

// Ana's story, as every caller used it before D281.
export const { script, lines, takeFile, current } = narrationFor();
