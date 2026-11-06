// A book is found by a sentence inside its sample chapter, a PDF (D325, D309): the library, fixed
// to books, lists the book whose chapter holds the words, and the school's whole search lists the
// chapter itself as a PDF. An opening's full call, a PDF its page only links (D319), is found by
// a sentence inside it too. None of these words is in any post's own text.
import { expect, test } from '../fixtures.mjs';
import { say, seeded, titleOf, titlesOn } from './school.mjs';

const sentences = {
	en_US: {
		tide: 'lighthouse keeper counted forty one gulls',
		call: 'algebra and geometry to classes of twenty four pupils',
	},
	pt_BR: {
		tide: 'faroleiro contou quarenta e uma gaivotas',
		call: 'algebra e geometria para turmas de vinte e quatro alunos',
	},
};
const sentence = () => sentences[ seeded().locale ] || sentences.en_US;

test( "the library finds a book by a sentence in its sample chapter's PDF", async ( {
	page,
	look,
	axe,
} ) => {
	const book = titleOf( 'tide-atlas' );
	const titles = await titlesOn( page, '/library/', sentence().tide );
	expect( titles[ 0 ] ).toBe( book );
	// The library is fixed to books: its chapter, a PDF, is not listed here.
	await expect(
		page.locator(
			'.viglet-turing-search__results .viglet-turing-search__type'
		)
	).not.toContainText( [ say().pdf ] );
	await look( 'demo-edu-library' );
	await axe( 'demo-edu-library' );
} );

test( 'the whole search lists the sample chapter itself as a PDF', async ( {
	page,
} ) => {
	const chapter = titleOf( 'sample-tide-atlas' );
	expect( await titlesOn( page, '/search/', sentence().tide ) ).toContain(
		chapter
	);
	await expect(
		page
			.locator( '.viglet-turing-search__result' )
			.filter( { hasText: chapter } )
			.locator( '.viglet-turing-search__type' )
	).toHaveText( say().pdf );
} );

test( "an opening's call, a PDF its page links, is found by its words", async ( {
	page,
} ) => {
	expect( await titlesOn( page, '/search/', sentence().call ) ).toContain(
		titleOf( 'call-maths-teacher' )
	);
} );
