<?php
/**
 * The demo school (D325), run on the school's own WordPress by `run-docker.cmd demo edu` (or
 * `demo edu-pt`) and before every run of its specs (setup/demo-edu.setup.mjs): Lumen School in
 * English, Escola Lumen in Portuguese, picked by the site's language as the stores' seed is.
 *
 * What a school's site holds and a store's does not, so every non-commerce feature of the plugin
 * shows on one coherent site:
 *
 * - events on a DATE field with the agenda's periods (D307): one today, one this week, one later
 *   this month or the next, two debates a month apart (a sentence names the month, D324), and two
 *   in the past. Each card shows the event's day and its poster, not the day it was posted (D305);
 * - openings that close on a day and leave search after it (D308), one already closed;
 * - books whose sample chapter is a PDF, found by a sentence inside it (D309), and an opening
 *   whose full call is a PDF its page links (D319);
 * - the agenda, the openings and the library as search blocks each fixed to its type (D306).
 *
 * Every date is counted from today, so the agenda's periods hold whenever the seed runs: it is
 * idempotent (a post is found again by its key, kept in `_lumen_key`) and re-dates what it finds.
 * Everything is invented: the school, its people, its books.
 *
 * Prints JSON: { events, openings, books, pdfs, pages: { slug: id } }.
 *
 * @package Viglet_Turing
 */

// phpcs:disable -- a setup script run by WP-CLI, not plugin code.

require_once ABSPATH . 'wp-admin/includes/file.php';
require_once ABSPATH . 'wp-admin/includes/media.php';
require_once ABSPATH . 'wp-admin/includes/image.php';

$pt = 'pt_BR' === get_locale();
// Each text is a pair, English and Portuguese: the site's language picks one.
$say = static fn ( array $both ): string => $both[ $pt ? 1 : 0 ];

$today = new DateTimeImmutable( 'today', new DateTimeZone( 'UTC' ) );
// A day counted from today, as the meta keeps it (yyyy-mm-dd).
$day = static fn ( int $days ): string => $today->modify( "{$days} day" )->format( 'Y-m-d' );
// The 15th of a month counted from this one: far from either edge of it.
$mid = static fn ( int $months ): string => $today->modify( 'first day of this month' )->modify( "+{$months} month" )->format( 'Y-m-15' );

/**
 * The post with this key, created or updated with these fields; returns its id.
 */
$upsert = static function ( string $type, string $key, array $fields, array $meta = array() ): int {
	$found = get_posts(
		array(
			'post_type'   => $type,
			'post_status' => 'any',
			'meta_key'    => '_lumen_key',
			'meta_value'  => $key,
			'numberposts' => 1,
			'fields'      => 'ids',
		)
	);
	$args = array(
		'post_type'   => $type,
		'post_status' => 'publish',
	) + $fields;
	$id = $found ? (int) wp_update_post( array( 'ID' => $found[0] ) + $args ) : (int) wp_insert_post( $args );
	update_post_meta( $id, '_lumen_key', $key );
	foreach ( $meta as $name => $value ) {
		update_post_meta( $id, $name, $value );
	}

	return $id;
};

/**
 * An attachment with this key, uploaded once from bytes the seed makes; returns its id.
 */
$upload = static function ( string $key, string $file, string $title, int $parent, callable $bytes ): int {
	$found = get_posts(
		array(
			'post_type'   => 'attachment',
			'post_status' => 'inherit',
			'meta_key'    => '_lumen_key',
			'meta_value'  => $key,
			'numberposts' => 1,
			'fields'      => 'ids',
		)
	);
	if ( $found ) {
		wp_update_post( array( 'ID' => $found[0], 'post_title' => $title, 'post_parent' => $parent ) );
		return (int) $found[0];
	}
	$tmp = wp_tempnam( $file );
	file_put_contents( $tmp, $bytes() );
	$id = media_handle_sideload( array( 'name' => $file, 'tmp_name' => $tmp ), $parent, $title );
	if ( is_wp_error( $id ) ) {
		WP_CLI::error( "{$file}: " . $id->get_error_message() );
	}
	update_post_meta( $id, '_lumen_key', $key );

	return (int) $id;
};

/**
 * A PDF whose pages hold these lines in Helvetica, its cross-reference table counted, as an
 * office's printer would make one. ASCII only: the standard font's encoding is all it needs.
 */
$pdf = static function ( array $lines ): string {
	$text = '';
	foreach ( $lines as $i => $line ) {
		$safe  = str_replace( array( '\\', '(', ')' ), array( '\\\\', '\\(', '\\)' ), $line );
		$text .= sprintf( 'BT /F1 %d Tf 72 %d Td (%s) Tj ET ', 0 === $i ? 20 : 12, 720 - 28 * $i, $safe );
	}
	$objects = array(
		'<< /Type /Catalog /Pages 2 0 R >>',
		'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
		'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>',
		'<< /Length ' . strlen( $text ) . " >>\nstream\n{$text}\nendstream",
		'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
	);
	$out     = "%PDF-1.4\n";
	$offsets = array();
	foreach ( $objects as $i => $object ) {
		$offsets[] = strlen( $out );
		$out      .= ( $i + 1 ) . " 0 obj\n{$object}\nendobj\n";
	}
	$xref = strlen( $out );
	$out .= "xref\n0 " . ( count( $objects ) + 1 ) . "\n0000000000 65535 f \n";
	foreach ( $offsets as $offset ) {
		$out .= sprintf( "%010d 00000 n \n", $offset );
	}

	return $out . 'trailer << /Size ' . ( count( $objects ) + 1 ) . " /Root 1 0 R >>\nstartxref\n{$xref}\n%%EOF";
};

/**
 * A flat picture, drawn rather than downloaded: a field of colour, a disc, and the title in
 * GD's own font. A poster is landscape, a book's cover portrait.
 */
$picture = static function ( int $width, int $height, string $hex, string $label ): callable {
	return static function () use ( $width, $height, $hex, $label ): string {
		$img   = imagecreatetruecolor( $width, $height );
		$rgb   = sscanf( $hex, '#%02x%02x%02x' );
		$back  = imagecolorallocate( $img, $rgb[0], $rgb[1], $rgb[2] );
		$light = imagecolorallocatealpha( $img, 255, 255, 255, 80 );
		$ink   = imagecolorallocate( $img, 255, 255, 255 );
		imagefilledrectangle( $img, 0, 0, $width, $height, $back );
		imagefilledellipse( $img, (int) ( $width * 0.75 ), (int) ( $height * 0.3 ), (int) ( $height * 0.6 ), (int) ( $height * 0.6 ), $light );
		$y = (int) ( $height * 0.62 );
		foreach ( explode( "\n", wordwrap( remove_accents( $label ), (int) ( $width / 11 ) ) ) as $line ) {
			imagestring( $img, 5, 24, $y, $line, $ink );
			$y += 20;
		}
		ob_start();
		imagepng( $img );

		return (string) ob_get_clean();
	};
};

$pdfs = 0;

// The agenda. [key, title, day, venue, text, poster colour]
$events = array(
	array( 'open-day', array( 'Open day', 'Dia de portas abertas' ), $day( 0 ), array( 'Main hall', 'Salão principal' ), array( 'Families walk the school with the pupils who study here: the labs, the art rooms and the garden. No booking needed.', 'Famílias percorrem a escola com os alunos que estudam aqui: os laboratórios, as salas de arte e a horta. Não precisa reservar.' ), '#1E2A44' ),
	array( 'science-fair', array( 'Science fair', 'Feira de ciências' ), $day( 3 ), array( 'Gymnasium', 'Ginásio' ), array( 'Year 8 shows the experiments it built this term, from a water filter to a wind turbine made of bottles.', 'O 8º ano mostra os experimentos que construiu no bimestre, de um filtro de água a uma turbina eólica feita de garrafas.' ), '#2F5D50' ),
	array( 'choir-concert', array( 'Winter choir concert', 'Concerto de inverno do coral' ), $day( 12 ), array( 'Auditorium', 'Auditório' ), array( 'The school choir sings the songs it rehearsed since August, with the string group.', 'O coral da escola canta as músicas que ensaiou desde agosto, com o grupo de cordas.' ), '#9A3B12' ),
	array( 'debate-car-free', array( 'Debate: should the school go car-free?', 'Debate: a escola deve fechar a rua para carros?' ), $mid( 1 ), array( 'Library', 'Biblioteca' ), array( 'One of the monthly debates the upper school runs: two teams, one motion, and the audience votes at the end.', 'Um dos debates mensais do ensino médio: dois times, uma moção, e a plateia vota no final.' ), '#40454F' ),
	array( 'debate-phones', array( 'Debate: phones in the classroom', 'Debate: celular na sala de aula' ), $mid( 2 ), array( 'Library', 'Biblioteca' ), array( 'One of the monthly debates the upper school runs: this time pupils argue both sides of a rule they live with.', 'Um dos debates mensais do ensino médio: desta vez os alunos defendem os dois lados de uma regra que vivem.' ), '#40454F' ),
	array( 'parents-evening', array( "Parents' evening", 'Reunião de pais' ), $day( -20 ), array( 'Classrooms', 'Salas de aula' ), array( 'Each tutor met the families of their class to talk about the first term.', 'Cada tutor recebeu as famílias da sua turma para falar do primeiro bimestre.' ), '#1E2A44' ),
	array( 'sports-day', array( 'Sports day', 'Gincana esportiva' ), $day( -60 ), array( 'Playing field', 'Quadra' ), array( 'Relays, the long jump and the tug of war, house against house.', 'Revezamento, salto em distância e cabo de guerra, equipe contra equipe.' ), '#2F5D50' ),
);
foreach ( $events as list( $key, $title, $when, $venue, $text, $colour ) ) {
	$id     = $upsert( 'event', $key, array( 'post_title' => $say( $title ), 'post_name' => sanitize_title( $say( $title ) ), 'post_content' => '<!-- wp:paragraph --><p>' . esc_html( $say( $text ) ) . '</p><!-- /wp:paragraph -->', 'post_excerpt' => $say( $text ) ) );
	$poster = $upload( "poster-{$key}", "lumen-poster-{$key}.png", $say( $title ), $id, $picture( 800, 450, $colour, $say( $title ) ) );
	update_post_meta( $id, 'event_day', $when );
	update_post_meta( $id, 'venue', $say( $venue ) );
	// The card's image is the poster (D305), not a featured image: the event has none.
	update_post_meta( $id, 'poster', (string) wp_get_attachment_url( $poster ) );
}

// The openings. [key, title, closes, kind, text, pdf lines or null]
$staff     = array( 'Staff', 'Equipe' );
$openings  = array(
	array( 'maths-teacher', array( 'Maths teacher, upper school', 'Professor de matemática, ensino médio' ), $day( 30 ), $staff, array( 'A full-time post from February, teaching four classes. The full call, with the selection stages, is a PDF.', 'Vaga em tempo integral a partir de fevereiro, com quatro turmas. O edital completo, com as etapas da seleção, está em PDF.' ), array( array( 'Maths teacher: the full call', 'Professor de matematica: o edital completo' ), array( 'Candidates teach algebra and geometry to classes of twenty four pupils.', 'Os candidatos ensinam algebra e geometria para turmas de vinte e quatro alunos.' ) ) ),
	array( 'enrolment-year-6', array( 'Enrolment: year 6 places for next year', 'Matrícula: vagas no 6º ano para o próximo ano' ), $day( 45 ), array( 'Enrolment', 'Matrícula' ), array( 'Families apply online with the last school report. Siblings of current pupils are seen first.', 'As famílias se inscrevem pela internet com o último boletim. Irmãos de alunos têm prioridade.' ), null ),
	array( 'arts-scholarship', array( 'Arts scholarship', 'Bolsa de artes' ), $day( 14 ), array( 'Scholarship', 'Bolsa' ), array( 'Two full scholarships for pupils who draw, paint or make music, chosen from a portfolio.', 'Duas bolsas integrais para alunos que desenham, pintam ou fazem música, escolhidos por portfólio.' ), null ),
	array( 'library-assistant', array( 'Library assistant, part-time', 'Auxiliar de biblioteca, meio período' ), $day( 20 ), $staff, array( 'Mornings at the library desk: lending, shelving and the reading club for the younger years.', 'Manhãs no balcão da biblioteca: empréstimos, organização das estantes e o clube de leitura dos mais novos.' ), null ),
	// Closed two days ago: never in search (D308), though its page is still published.
	array( 'summer-monitors', array( 'Summer camp monitors', 'Monitores da colônia de férias' ), $day( -2 ), $staff, array( 'Monitors for the two weeks of summer camp, with first-aid training.', 'Monitores para as duas semanas da colônia de férias, com curso de primeiros socorros.' ), null ),
);
foreach ( $openings as list( $key, $title, $closes, $kind, $text, $call ) ) {
	$id   = $upsert( 'opening', $key, array( 'post_title' => $say( $title ), 'post_name' => sanitize_title( $say( $title ) ), 'post_excerpt' => $say( $text ) ) );
	$body = '<!-- wp:paragraph --><p>' . esc_html( $say( $text ) ) . '</p><!-- /wp:paragraph -->';
	if ( $call ) {
		// The full call is a PDF the page links (D319), not an attachment of it.
		$file  = $upload( "call-{$key}", "lumen-call-{$key}.pdf", $say( $call[0] ), 0, static fn () => $pdf( array( $say( $call[0] ), $say( $call[1] ) ) ) );
		$body .= "\n\n" . '<!-- wp:paragraph --><p><a href="' . esc_url( (string) wp_get_attachment_url( $file ) ) . '">' . esc_html( $say( array( 'Read the full call (PDF)', 'Leia o edital completo (PDF)' ) ) ) . '</a></p><!-- /wp:paragraph -->';
		++$pdfs;
	}
	wp_update_post( array( 'ID' => $id, 'post_content' => $body ) );
	update_post_meta( $id, 'closes_on', $closes );
	update_post_meta( $id, 'opening_kind', $say( $kind ) );
}

// The library. [key, title, author, shelf, text, sample chapter's lines, cover colour]
$books = array(
	array( 'tide-atlas', array( 'The Tide Atlas', 'O Atlas das Marés' ), 'Odila Marsk', array( 'Science', 'Ciências' ), array( 'Why the sea comes and goes twice a day, told through the coasts that live by it.', 'Por que o mar vai e volta duas vezes por dia, contado pelas costas que vivem dele.' ), array( array( 'The Tide Atlas: sample chapter', 'O Atlas das Mares: capitulo de amostra' ), array( 'The lighthouse keeper counted forty one gulls on the north jetty.', 'O faroleiro contou quarenta e uma gaivotas no molhe norte.' ) ), '#2F5D50' ),
	array( 'clockwork-orchard', array( 'The Clockwork Orchard', 'O Pomar de Corda' ), 'Teodoro Vennik', array( 'Fiction', 'Ficção' ), array( 'A girl inherits an orchard whose trees keep time, and has a summer to learn why.', 'Uma menina herda um pomar cujas árvores marcam as horas, e tem um verão para descobrir por quê.' ), array( array( 'The Clockwork Orchard: sample chapter', 'O Pomar de Corda: capitulo de amostra' ), array( 'Every pear tree in the valley ticked at noon.', 'Toda pereira do vale fazia tique ao meio dia.' ) ), '#9A3B12' ),
	array( 'salt-roads', array( 'The Salt Roads', 'Os Caminhos do Sal' ), 'Ilse Carrow', array( 'History', 'História' ), array( 'How a mineral everyone needed drew the first long roads across a continent.', 'Como um mineral de que todos precisavam abriu as primeiras estradas longas de um continente.' ), array( array( 'The Salt Roads: sample chapter', 'Os Caminhos do Sal: capitulo de amostra' ), array( 'Caravans traded salt for indigo at the river ford.', 'As caravanas trocavam sal por anil no vau do rio.' ) ), '#1E2A44' ),
);
foreach ( $books as list( $key, $title, $author, $shelf, $text, $sample, $colour ) ) {
	$id     = $upsert( 'book', $key, array( 'post_title' => $say( $title ), 'post_name' => sanitize_title( $say( $title ) ), 'post_content' => '<!-- wp:paragraph --><p>' . esc_html( $say( $text ) ) . '</p><!-- /wp:paragraph -->', 'post_excerpt' => $say( $text ) ) );
	$cover  = $upload( "cover-{$key}", "lumen-cover-{$key}.png", $say( $title ), $id, $picture( 400, 600, $colour, $say( $title ) . "\n" . $author ) );
	// The sample chapter is attached to the book (D309) and named by its meta, so its words are
	// both a PDF of their own in search and part of the book's text.
	$chapter = $upload( "sample-{$key}", "lumen-sample-{$key}.pdf", $say( $sample[0] ), $id, static fn () => $pdf( array( $say( $sample[0] ), $say( $sample[1] ) ) ) );
	++$pdfs;
	set_post_thumbnail( $id, $cover );
	update_post_meta( $id, 'author_name', $author );
	update_post_meta( $id, 'shelf', $say( $shelf ) );
	update_post_meta( $id, 'sample', (string) $chapter );
}

// Two news posts, so the home has some and the posts keep their default card.
$news = array(
	array( 'library-saturdays', array( 'The library opens on Saturdays', 'A biblioteca abre aos sábados' ), array( 'From this month the library opens on Saturday mornings for pupils and their families.', 'A partir deste mês a biblioteca abre aos sábados de manhã para alunos e famílias.' ) ),
	array( 'garden-harvest', array( 'The school garden gave its first harvest', 'A horta da escola deu a primeira colheita' ), array( 'Year 4 picked the lettuces and carrots it planted in the spring, and the canteen served them.', 'O 4º ano colheu as alfaces e cenouras que plantou na primavera, e a cantina serviu.' ) ),
);
foreach ( $news as list( $key, $title, $text ) ) {
	$upsert( 'post', $key, array( 'post_title' => $say( $title ), 'post_name' => sanitize_title( $say( $title ) ), 'post_content' => '<!-- wp:paragraph --><p>' . esc_html( $say( $text ) ) . '</p><!-- /wp:paragraph -->' ) );
}

// The pages: a home, the agenda, the openings and the library (each a search block fixed to its
// type, D306), the whole school's search page and the news. [slug => title, content]
$block = static fn ( array $attributes ): string => '<!-- wp:viglet-turing/search ' . wp_json_encode( $attributes + array( 'suggestFilters' => true, 'align' => 'wide' ) ) . ' /-->';
$pages = array(
	'home'     => array( array( 'Home', 'Início' ), '' ),
	'agenda'   => array( array( 'Agenda', 'Agenda' ), $block( array( 'filters' => array( 'type:event' ), 'perPage' => 20, 'placeholder' => $say( array( 'Debates in November, concerts this month…', 'Debates em novembro, concertos este mês…' ) ) ) ) ),
	'openings' => array( array( 'Openings', 'Oportunidades' ), $block( array( 'filters' => array( 'type:opening' ), 'placeholder' => $say( array( 'Jobs, enrolment, scholarships…', 'Vagas, matrícula, bolsas…' ) ) ) ) ),
	'library'  => array( array( 'Library', 'Biblioteca' ), $block( array( 'filters' => array( 'type:book' ), 'resultsLayout' => 'grid', 'placeholder' => $say( array( 'A sentence you remember from a book…', 'Uma frase de que você se lembra de um livro…' ) ) ) ) ),
	'search'   => array( array( 'Search the school', 'Buscar na escola' ), $block( array( 'placeholder' => $say( array( 'Search events, openings, books…', 'Busque eventos, oportunidades, livros…' ) ) ) ) ),
	'news'     => array( array( 'News', 'Notícias' ), '' ),
);
$ids   = array();
foreach ( $pages as $slug => list( $title, $content ) ) {
	$found        = get_page_by_path( $slug, OBJECT, 'page' );
	$args         = array(
		'post_type'    => 'page',
		'post_name'    => $slug,
		'post_title'   => $say( $title ),
		'post_content' => $content,
		'post_status'  => 'publish',
	);
	$ids[ $slug ] = $found ? (int) wp_update_post( array( 'ID' => $found->ID ) + $args ) : (int) wp_insert_post( $args );
	// The pages are the ways in, not content: never a result.
	update_post_meta( $ids[ $slug ], '_viglet_turing_exclude', '1' );
}

foreach ( array( array( 'hello-world', 'post' ), array( 'sample-page', 'page' ) ) as list( $default, $kind ) ) {
	$post = get_page_by_path( $default, OBJECT, $kind );
	if ( $post ) {
		wp_delete_post( $post->ID, true );
	}
}

update_option( 'show_on_front', 'page' );
update_option( 'page_on_front', $ids['home'] );
update_option( 'page_for_posts', $ids['news'] );
update_option( 'blogname', $say( array( 'Lumen School', 'Escola Lumen' ) ) );
update_option( 'blogdescription', $say( array( 'Learning in the open', 'Aprender às claras' ) ) );

// The school's types in search, its PDFs too (D309), and the search on its own page.
$settings                       = viglet_turing_get_settings();
$settings['index_custom_types'] = array( 'event', 'opening', 'book' );
$settings['index_pdfs']         = 1;
$settings['search_enabled']     = 1;
$settings['instant_overlay']    = 1;
$settings['search_page']        = $ids['search'];
$settings['related_after']      = array();
viglet_turing_update_settings( $settings );

// The fields: an event's day with the agenda's periods, its venue and poster; an opening's
// closing day, after which it leaves search, and its kind; a book's author, shelf and the text
// of its sample chapter.
$label   = static fn ( array $both ): string => $say( $both );
$mapping = get_option( 'viglet_turing_mapping', array() );
$mapping['event'] = array(
	'fields'     => array(
		array( 'source' => 'meta:event_day', 'field' => 'event_day', 'type' => 'DATE', 'facet_label' => $label( array( 'When', 'Quando' ) ), 'range' => true, 'periods' => 'agenda' ),
		array( 'source' => 'meta:venue', 'field' => 'venue', 'type' => 'STRING', 'facet' => true, 'facet_label' => $label( array( 'Venue', 'Local' ) ), 'card' => true ),
		array( 'source' => 'meta:poster', 'field' => 'poster', 'type' => 'STRING' ),
	),
	'card_date'  => 'event_day',
	'card_image' => 'poster',
);
$mapping['opening'] = array(
	'fields'    => array(
		array( 'source' => 'meta:closes_on', 'field' => 'closes_on', 'type' => 'DATE' ),
		array( 'source' => 'meta:opening_kind', 'field' => 'opening_kind', 'type' => 'STRING', 'facet' => true, 'facet_label' => $label( array( 'Kind', 'Tipo' ) ), 'card' => true ),
	),
	'card_date' => 'closes_on',
	'expires'   => 'closes_on',
);
$mapping['book'] = array(
	'fields' => array(
		array( 'source' => 'meta:author_name', 'field' => 'author_name', 'type' => 'STRING', 'facet' => true, 'facet_label' => $label( array( 'Author', 'Autor' ) ), 'card' => true ),
		array( 'source' => 'meta:shelf', 'field' => 'shelf', 'type' => 'STRING', 'facet' => true, 'facet_label' => $label( array( 'Shelf', 'Estante' ) ) ),
		array( 'source' => 'file:sample', 'field' => 'sample_text', 'type' => 'TEXT' ),
	),
);
update_option( 'viglet_turing_mapping', $mapping, false );

// Words a family uses for the same thing.
$tuning = new Viglet_Turing_Tuning();
$have   = $tuning->all();
if ( ! is_wp_error( $have ) ) {
	$kept    = array_map( static fn ( $row ) => implode( '|', $row['terms'] ), (array) $have['synonyms'] );
	$synonym = $pt ? array( 'vaga', 'emprego', 'oportunidade' ) : array( 'vacancy', 'job', 'opening' );
	if ( ! in_array( implode( '|', $synonym ), $kept, true ) ) {
		$tuning->add_synonym( $synonym );
	}
}

// Every seeded post and file by its key, with its id and title in the site's language: what the
// browser tests look for, read once here rather than once per check.
$keyed = array();
foreach ( get_posts(
	array(
		'post_type'   => array( 'event', 'opening', 'book', 'post', 'attachment' ),
		'post_status' => 'any',
		'meta_key'    => '_lumen_key',
		'numberposts' => -1,
	)
) as $post ) {
	$keyed[ get_post_meta( $post->ID, '_lumen_key', true ) ] = array(
		'id'    => $post->ID,
		'title' => $post->post_title,
	);
}

echo wp_json_encode(
	array(
		'events'   => count( $events ),
		'openings' => count( $openings ),
		'books'    => count( $books ),
		'pdfs'     => $pdfs,
		'pages'    => $ids,
		'keys'     => $keyed,
	)
), "\n";
