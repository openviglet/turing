// Whether each visible select draws its chosen text clear of the arrow WordPress puts on its
// right (D192): the text, measured in the select's own font, must fit its padding box.

/**
 * The visible selects under a root whose chosen text runs under their arrow.
 *
 * @param {import('@playwright/test').Page} page The page.
 * @param {string}                          root A selector the selects are under.
 * @return {Promise<Array<{id: string, text: string, overflow: number}>>} Empty when all fit.
 */
export function selectsClearOfArrow( page, root ) {
	return page.evaluate( ( selector ) => {
		const context = document.createElement( 'canvas' ).getContext( '2d' );
		const cut = [];
		for ( const select of document.querySelectorAll(
			`${ selector } select`
		) ) {
			if ( ! select.offsetParent ) {
				continue;
			}
			const style = window.getComputedStyle( select );
			context.font = `${ style.fontWeight } ${ style.fontSize } ${ style.fontFamily }`;
			const text = select.selectedOptions[ 0 ]?.text || '';
			// WordPress draws the arrow 16px wide, 5px from the right edge: whatever the
			// padding, the text must end before it.
			const room =
				select.clientWidth -
				parseFloat( style.paddingLeft ) -
				Math.max( parseFloat( style.paddingRight ), 24 );
			const width = context.measureText( text ).width;
			if ( width > room ) {
				cut.push( {
					id: select.id || select.name,
					text,
					overflow: Math.ceil( width - room ),
				} );
			}
		}
		return cut;
	}, root );
}
