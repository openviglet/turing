/**
 * The compare block's summary (D200): asked for once the table is on the page, and shown only
 * when Turing wrote one the plugin found grounded in the table. Anything else leaves the table
 * alone, which is the feature without AI.
 */
for ( const box of document.querySelectorAll(
	'.viglet-dumont-compare__summary[data-summary-url]'
) ) {
	window
		.fetch( box.dataset.summaryUrl, { credentials: 'same-origin' } )
		.then( ( response ) => ( response.ok ? response.json() : {} ) )
		.then( ( answer ) => {
			if ( answer?.summary ) {
				box.querySelector(
					'.viglet-dumont-compare__summary-text'
				).textContent = answer.summary;
				box.hidden = false;
			}
		} )
		.catch( () => {} );
}
