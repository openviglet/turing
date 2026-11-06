/**
 * "How it works" (D325): with ?tour=1, a numbered marker before each Turing feature on the
 * page, opening a two-line note on what it does and a link to its docs. The note opens in the
 * flow, before the feature, so it never covers what it describes; Escape closes it. Loaded only
 * on a toured page (functions.php), in the page's language.
 *
 * A marker's number is the feature's chapter in the school's story (demo/school/story.md), as
 * Tarnfell's tour numbers the shopper's.
 */
( () => {
	const docs = 'https://dumont.viglet.org/docs/connectors/wordpress/';
	const pt = ( document.documentElement.lang || '' ).startsWith( 'pt' );
	const text = pt
		? {
				marker: 'Capítulo %d da história: %s',
				docs: 'Como funciona',
				close: 'Sair do tour',
			}
		: {
				marker: 'Story chapter %d: %s',
				docs: 'How it works',
				close: 'Leave the tour',
			};
	const features = [
		{
			selector: '.viglet-dumont-search__form',
			chapter: 2,
			anchor: '#the-turing-search-block',
			en: [
				'Say when, in words',
				'"Debates in November" or "this week" becomes a filter on the event\'s day, with or without a language model behind Turing.',
			],
			pt: [
				'Diga quando, em palavras',
				'"Debates em novembro" ou "nesta semana" vira um filtro no dia do evento, com ou sem um modelo de linguagem no Turing.',
			],
		},
		{
			selector: '.viglet-dumont-search__facets',
			chapter: 1,
			anchor: '#fields-and-facets',
			en: [
				'An agenda, not an archive',
				'The event\'s day is a date field with the agenda\'s periods: Today, Next 7 days, This month, Upcoming and Past, counted from today.',
			],
			pt: [
				'Uma agenda, não um arquivo',
				'O dia do evento é um campo de data com os períodos da agenda: Hoje, Próximos 7 dias, Este mês, Próximos e Passados, contados a partir de hoje.',
			],
		},
		{
			selector: '.viglet-dumont-search__results',
			chapter: 3,
			anchor: '#fields-and-facets',
			en: [
				'The card shows the event',
				'Each card shows the day the event happens and its poster, mapped per type, instead of the day it was posted.',
			],
			pt: [
				'O card mostra o evento',
				'Cada card mostra o dia em que o evento acontece e o seu cartaz, mapeados por tipo, em vez do dia em que foi publicado.',
			],
		},
	];

	let open = null;
	const close = () => {
		if ( open ) {
			open.note.hidden = true;
			open.button.setAttribute( 'aria-expanded', 'false' );
			open = null;
		}
	};

	// The features the page shows now, in its order: one it keeps hidden gets no marker.
	const shown = () =>
		features
			.map( ( feature ) => ( {
				feature,
				target: document.querySelector( feature.selector ),
			} ) )
			.filter(
				( { target } ) => target && ! target.closest( '[hidden]' )
			)
			.sort( ( a, b ) =>
				// eslint-disable-next-line no-bitwise -- compareDocumentPosition answers in bits.
				a.target.compareDocumentPosition( b.target ) &
				window.Node.DOCUMENT_POSITION_FOLLOWING
					? -1
					: 1
			)
			.map( ( { feature } ) => feature );

	const mark = ( feature, position ) => {
		const target = document.querySelector( feature.selector );
		const [ title, body ] = pt ? feature.pt : feature.en;
		const id = `lumen-tour-${ position }`;
		const wrap = document.createElement( 'div' );
		wrap.className = 'lumen-tour';
		const button = document.createElement( 'button' );
		button.type = 'button';
		button.className = 'lumen-tour__marker';
		button.textContent = String( feature.chapter );
		button.setAttribute(
			'aria-label',
			text.marker.replace( '%d', feature.chapter ).replace( '%s', title )
		);
		button.setAttribute( 'aria-expanded', 'false' );
		button.setAttribute( 'aria-controls', id );
		const note = document.createElement( 'div' );
		note.className = 'lumen-tour__note';
		note.id = id;
		note.hidden = true;
		note.innerHTML = '<strong></strong><p></p><a></a>';
		note.querySelector( 'strong' ).textContent = title;
		note.querySelector( 'p' ).textContent = body;
		const link = note.querySelector( 'a' );
		link.href = docs + feature.anchor;
		link.textContent = text.docs;
		wrap.append( button, note );
		target.before( wrap );

		button.addEventListener( 'click', () => {
			const was = open && open.note === note;
			close();
			if ( ! was ) {
				note.hidden = false;
				button.setAttribute( 'aria-expanded', 'true' );
				open = { button, note };
			}
		} );
	};

	let placed = '';
	// Mark what the page shows; again when a feature appears, as the facets do once Turing answers.
	const place = () => {
		const now = shown();
		const key = now.map( ( feature ) => feature.selector ).join( '|' );
		if ( key === placed || open ) {
			return;
		}
		placed = key;
		document
			.querySelectorAll( '.lumen-tour' )
			.forEach( ( wrap ) => wrap.remove() );
		now.forEach( ( feature, i ) => mark( feature, i + 1 ) );
	};

	place();
	new window.MutationObserver( ( changes ) => {
		if (
			changes.some(
				( change ) => ! change.target.closest?.( '.lumen-tour' )
			)
		) {
			place();
		}
	} ).observe( document.body, {
		subtree: true,
		attributes: true,
		attributeFilter: [ 'hidden' ],
	} );

	document.addEventListener( 'keydown', ( event ) => {
		if ( 'Escape' === event.key ) {
			close();
		}
	} );
} )();
