// The axe pass shared by the specs (the `axe` fixture) and `npm run e2e:look -- --a11y`.
import AxeBuilder from '@axe-core/playwright';

// What fails a spec: axe's two highest impacts. Moderate and minor are printed by look only.
export const failingImpacts = [ 'serious', 'critical' ];

/**
 * Run axe against the WCAG 2.1 A and AA rules on what the page shows now.
 *
 * @param {import('@playwright/test').Page} page
 * @param {Object}                          options
 * @param {string}                          [options.include] A selector to scope the pass to,
 *                                                            e.g. the plugin's own region.
 * @param {string[]}                        [options.exclude] Selectors to leave out.
 * @return {Promise<Object[]>} axe's violations.
 */
export async function axeViolations( page, { include, exclude = [] } = {} ) {
	// A colour mid-transition reads as a contrast failure; measure what settles.
	await page.evaluate( () =>
		Promise.all(
			document
				.getAnimations()
				// A spinner never finishes.
				.filter(
					( animation ) =>
						Infinity !==
						animation.effect?.getComputedTiming().iterations
				)
				.map( ( animation ) => animation.finished )
		)
	);
	let builder = new AxeBuilder( { page } ).withTags( [
		'wcag2a',
		'wcag2aa',
		'wcag21a',
		'wcag21aa',
	] );
	if ( include ) {
		builder = builder.include( include );
	}
	for ( const selector of exclude ) {
		builder = builder.exclude( selector );
	}
	return ( await builder.analyze() ).violations;
}

/**
 * One line per violation: impact, rule, what it means and where (the first few nodes).
 *
 * @param {Object} violation An axe violation.
 */
export function describe( violation ) {
	const targets = violation.nodes
		.slice( 0, 3 )
		.map( ( node ) => node.target.join( ' ' ) );
	const more =
		violation.nodes.length > 3 ? ` +${ violation.nodes.length - 3 }` : '';
	return `${ violation.impact } ${ violation.id }: ${ violation.help } [${ targets.join( ', ' ) }${ more }]`;
}
