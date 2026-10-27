/**
 * "How it works" (D195): with ?tour=1, a numbered marker before each Turing feature on the
 * page, opening a two-line note on what the feature does and a link to its docs. The note opens
 * in the flow, before the feature, so it never covers what it describes; Escape closes it.
 * Loaded only on a toured page (functions.php), in the page's language.
 */
( () => {
	const docs = 'https://docs.viglet.org/dumont/connectors/wordpress';
	const pt = ( document.documentElement.lang || '' ).startsWith( 'pt' );
	const text = pt
		? {
				marker: 'Recurso do Turing %d: %s',
				docs: 'Como funciona',
				close: 'Sair do tour',
			}
		: {
				marker: 'Turing feature %d: %s',
				docs: 'How it works',
				close: 'Leave the tour',
			};
	const features = [
		{
			selector: '.viglet-dumont-search__form',
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

	let number = 0;
	for ( const feature of features ) {
		const target = document.querySelector( feature.selector );
		// A feature this page does not have, or keeps hidden for now, gets no marker.
		if ( ! target || target.closest( '[hidden]' ) ) {
			continue;
		}
		number++;
		const [ title, body ] = pt ? feature.pt : feature.en;
		const id = `tarnfell-tour-${ number }`;
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
	}

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
