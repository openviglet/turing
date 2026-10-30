/**
 * "How it works" (D195): with ?tour=1, a numbered marker before each Turing feature on the
 * page, opening a two-line note on what the feature does and a link to its docs. The note opens
 * in the flow, before the feature, so it never covers what it describes; Escape closes it.
 * Loaded only on a toured page (functions.php), in the page's language.
 *
 * A marker's number is the feature's chapter in the shopper's story (demo/story.md, D203), so
 * the tour, the site and the story walk the demo in one order across pages.
 */
( () => {
	const docs = 'https://openviglet.github.io/dumont/docs/connectors/wordpress/';
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
	// The single product's page is chapter 6 of the story: there the copilot answers about one item.
	const onProduct = document.body.classList.contains( 'single-product' );
	const features = [
		{
			selector: '.viglet-dumont-search__form',
			chapter: 2,
			anchor: '#the-turing-search-block',
			en: [
				'Search as you type',
				'Suggestions come from what the catalog holds, and a misspelling is offered its correction. Synonyms set in Turing make "raincoat" find rain jackets.',
			],
			pt: [
				'Busca enquanto digita',
				'As sugestões vêm do que o catálogo tem, e um erro de digitação recebe a correção. Sinônimos definidos no Turing fazem "capa de chuva" achar jaquetas impermeáveis.',
			],
		},
		{
			selector: '.viglet-dumont-search__facets',
			chapter: 2,
			anchor: '#fields-and-facets',
			en: [
				'Filters from the catalog',
				'Category, price, colour and use are WooCommerce fields the plugin sends to Turing as facets, each with its count for this search.',
			],
			pt: [
				'Filtros vindos do catálogo',
				'Categoria, preço, cor e uso são campos do WooCommerce que o plugin envia ao Turing como facetas, cada uma com sua contagem nesta busca.',
			],
		},
		{
			selector: '.viglet-dumont-search__answer',
			chapter: 3,
			anchor: '#search',
			en: [
				'An answer with its sources',
				"A question gets an answer written by Turing from the shop's own guides and products, with the sources it read. No source, no answer.",
			],
			pt: [
				'Uma resposta com as fontes',
				'Uma pergunta recebe uma resposta escrita pelo Turing a partir dos guias e produtos da loja, com as fontes que leu. Sem fonte, sem resposta.',
			],
		},
		{
			selector: '.wp-block-viglet-dumont-related',
			chapter: 6,
			anchor: '#the-turing-search-block',
			en: [
				'Related, by meaning',
				'Turing finds the products and guides most like this one by what they say, not by a tag someone set.',
			],
			pt: [
				'Relacionados pelo sentido',
				'O Turing encontra os produtos e guias mais parecidos com este pelo que dizem, não por uma etiqueta que alguém marcou.',
			],
		},
		{
			selector: '.wp-block-viglet-dumont-copilot',
			chapter: onProduct ? 6 : 5,
			anchor: '#search',
			en: [
				'Ask the catalog',
				'Describe a need in a sentence and Turing answers with in-stock products from this shop and why each fits; one click adds it to the cart.',
			],
			pt: [
				'Pergunte ao catálogo',
				'Descreva uma necessidade numa frase e o Turing responde com produtos em estoque desta loja e por que cada um serve; um clique adiciona ao carrinho.',
			],
		},
		{
			// A placed block; the corner launcher on every page is the site's chrome, not a stop.
			selector: '.wp-block-viglet-dumont-assistant:not(.is-floating)',
			chapter: 3,
			anchor: '#search',
			en: [
				'Ask this site',
				"A conversation with Turing over the shop's guides and products: each answer lists what it read, and the conversation follows the visitor from page to page.",
			],
			pt: [
				'Pergunte a este site',
				'Uma conversa com o Turing sobre os guias e produtos da loja: cada resposta lista o que leu, e a conversa acompanha o visitante de página em página.',
			],
		},
	];

	let open = null;
	const close = () => {
		if ( open ) {
			open.note.hidden = true;
			open.button.setAttribute( 'aria-expanded', 'false' );
			open.button.focus();
			open = null;
		}
	};

	// The features the page shows now: one it does not have, or keeps hidden, gets no marker.
	// In the order the page shows them.
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

	let placed = '';
	// Mark the features the page shows, in its order, each with its chapter. Run again when one
	// appears: the copilot and the Site assistant show only once Turing says they can answer (D179).
	const place = () => {
		const now = shown();
		const key = now.map( ( feature ) => feature.selector ).join( '|' );
		if ( key === placed || open ) {
			return;
		}
		placed = key;
		document
			.querySelectorAll( '.tarnfell-tour' )
			.forEach( ( wrap ) => wrap.remove() );
		now.forEach( ( feature, i ) => mark( feature, i + 1 ) );
	};

	// `position` keeps each note's id unique where two features share a chapter.
	const mark = ( feature, position ) => {
		const target = document.querySelector( feature.selector );
		const [ title, body ] = pt ? feature.pt : feature.en;
		const number = feature.chapter;
		const id = `tarnfell-tour-${ position }`;
		const wrap = document.createElement( 'div' );
		wrap.className = 'tarnfell-tour';
		const button = document.createElement( 'button' );
		button.type = 'button';
		button.className = 'tarnfell-tour__marker';
		button.textContent = String( number );
		button.setAttribute(
			'aria-label',
			text.marker.replace( '%d', number ).replace( '%s', title )
		);
		button.setAttribute( 'aria-expanded', 'false' );
		button.setAttribute( 'aria-controls', id );
		const note = document.createElement( 'div' );
		note.className = 'tarnfell-tour__note';
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

	place();
	new window.MutationObserver( ( changes ) => {
		if (
			changes.some(
				( change ) => ! change.target.closest?.( '.tarnfell-tour' )
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

	// The way out: the same page without ?tour.
	const leave = document.createElement( 'a' );
	leave.className = 'tarnfell-tour__leave';
	const url = new URL( window.location.href );
	url.searchParams.delete( 'tour' );
	leave.href = url.href;
	leave.textContent = text.close;
	document.body.append( leave );
} )();
