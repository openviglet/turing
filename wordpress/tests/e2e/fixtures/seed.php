<?php
/**
 * The data the browser tests expect, made idempotent: run through WP-CLI by global-setup
 * (`wp eval-file /e2e/seed.php`) on the run-docker.cmd stack, never on a real site.
 *
 * - twelve posts dated 1 to 12 January 2026, with a `seats` meta (20 or 40), four of them
 *   titled "Workshop on ..."
 *   (one post, "Only on the second site", with the argument `second`);
 * - a page "Busca" holding the Turing search block, and "Header search" holding core's;
 * - the `seats` field mapped as an INT facet and `topic` (the title's last word, twelve
 *   values) as a STRING facet, the seats again as `capacity` ranges (up to 30, 30 or
 *   more), and search answered by Turing.
 *
 * Prints JSON: { searchPageId, corePageId, postId } for the specs.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a test fixture run by WP-CLI, not plugin code.

// `wp eval-file seed.php second` seeds the multisite network's second site instead: one post
// no other site has, so a search that crosses sites shows (multisite.spec.mjs).
$titles = 'second' === ( $args[0] ?? '' ) ? array( 'Only on the second site' ) : array(
	'Workshop on search', 'Workshop on facets', 'Seminar on indexing', 'Course on WordPress',
	'Lecture on Turing', 'Workshop on blocks', 'Seminar on design', 'Course on PHP',
	'Lecture on React', 'Workshop on testing', 'Seminar on CORS', 'Course on Docker',
);

$post_id = 0;
foreach ( $titles as $i => $title ) {
	$found = get_posts( array( 'title' => $title, 'post_type' => 'post', 'post_status' => 'any', 'numberposts' => 1, 'fields' => 'ids' ) );
	$id    = $found ? (int) $found[0] : (int) wp_insert_post(
		array(
			'post_title'   => $title,
			'post_content' => "About $title.",
			'post_status'  => 'publish',
		)
	);
	// One day apart from 1 January 2026, so "newest" has an order to show; fixed rather
	// than relative, so the dates in the stored screenshots do not move with the calendar.
	$date = sprintf( '2026-01-%02d 10:00:00', $i + 1 );
	if ( get_post_field( 'post_date', $id ) !== $date ) {
		wp_update_post(
			array(
				'ID'            => $id,
				'post_date'     => $date,
				'post_date_gmt' => get_gmt_from_date( $date ),
			)
		);
	}
	update_post_meta( $id, 'seats', 0 === $i % 2 ? '20' : '40' );
	// A facet with more values than a group shows before "Show more": the title's last word.
	update_post_meta( $id, 'topic', substr( strrchr( ' ' . $title, ' ' ), 1 ) );
	delete_post_meta( $id, '_viglet_dumont_exclude' );
	if ( 0 === $i ) {
		$post_id = $id;
	}
	// A workshop whose title shares no word with the seeded pages (related.spec.mjs).
	if ( 'Workshop on facets' === $title ) {
		$related_id = $id;
	}
}

$page = get_page_by_path( 'busca' );
$page_id = $page ? $page->ID : (int) wp_insert_post(
	array(
		'post_type'    => 'page',
		'post_name'    => 'busca',
		'post_title'   => 'Busca',
		'post_status'  => 'publish',
		'post_content' => '<!-- wp:viglet-dumont/search /-->',
	)
);
// The block with its defaults again: an editor spec changes its settings and saves.
if ( '<!-- wp:viglet-dumont/search /-->' !== get_post_field( 'post_content', $page_id ) ) {
	wp_update_post(
		array(
			'ID'           => $page_id,
			'post_content' => '<!-- wp:viglet-dumont/search /-->',
		)
	);
}

// The search block with Turing's grounded answer turned on (answer.spec.mjs).
$ask    = get_page_by_path( 'ask' );
$ask_id = $ask ? $ask->ID : (int) wp_insert_post(
	array(
		'post_type'    => 'page',
		'post_name'    => 'ask',
		'post_title'   => 'Ask',
		'post_status'  => 'publish',
		'post_content' => '<!-- wp:viglet-dumont/search {"showAnswer":true} /-->',
	)
);

// The search block with its parts as inner blocks, the pages moved above the results
// (parts.spec.mjs).
$parts_content = implode(
	"\n",
	array(
		'<!-- wp:viglet-dumont/search {"perPage":5} -->',
		'<!-- wp:viglet-dumont/search-box /-->',
		'<!-- wp:viglet-dumont/search-pages /-->',
		'<!-- wp:viglet-dumont/search-filters /-->',
		'<!-- wp:viglet-dumont/search-results /-->',
		'<!-- /wp:viglet-dumont/search -->',
	)
);
$parts    = get_page_by_path( 'search-parts' );
$parts_id = $parts ? $parts->ID : (int) wp_insert_post(
	array(
		'post_type'    => 'page',
		'post_name'    => 'search-parts',
		'post_title'   => 'Search parts',
		'post_status'  => 'publish',
		'post_content' => $parts_content,
	)
);
if ( $parts_content !== get_post_field( 'post_content', $parts_id ) ) {
	wp_update_post( array( 'ID' => $parts_id, 'post_content' => $parts_content ) );
}

// The Related content block on a page no other document resembles (related.spec.mjs).
$lonely    = get_page_by_path( 'related-none' );
$lonely_id = $lonely ? $lonely->ID : (int) wp_insert_post(
	array(
		'post_type'    => 'page',
		'post_name'    => 'related-none',
		'post_title'   => 'Quokkas',
		'post_status'  => 'publish',
		'post_content' => "<!-- wp:paragraph --><p>Marsupials of Rottnest.</p><!-- /wp:paragraph -->\n\n<!-- wp:viglet-dumont/related /-->",
	)
);
// An event log this version wrote: older ones stored sentences, in whatever language (D180).
delete_option( 'viglet_dumont_events' );

// Each post's neighbours as Turing answers them now, not as a previous run cached them.
global $wpdb;
$wpdb->query( "DELETE FROM {$wpdb->options} WHERE option_name LIKE '\\_transient%viglet\\_dumont\\_related\\_%'" );

// A page with the theme's own Search block, which the instant-search overlay takes over.
$core    = get_page_by_path( 'header-search' );
$core_id = $core ? $core->ID : (int) wp_insert_post(
	array(
		'post_type'    => 'page',
		'post_name'    => 'header-search',
		'post_title'   => 'Header search',
		'post_status'  => 'publish',
		'post_content' => '<!-- wp:search {"label":"Search","buttonText":"Search"} /-->',
	)
);

update_option(
	'viglet_dumont_mapping',
	array(
		'post' => array(
			'exclude' => false,
			'fields'  => array(
				array(
					'source'      => 'meta:seats',
					'field'       => 'seats',
					'type'        => 'INT',
					'multi'       => false,
					'facet'       => true,
					'facet_label' => 'Seats',
					'weight'      => null,
				),
				array(
					'source'      => 'meta:topic',
					'field'       => 'topic',
					'type'        => 'STRING',
					'multi'       => false,
					'facet'       => true,
					'facet_label' => 'Topic',
					'weight'      => null,
				),
				// The seats again, as ranges only: a range facet (Turing custom facet).
				array(
					'source'      => 'meta:seats',
					'field'       => 'capacity',
					'type'        => 'INT',
					'multi'       => false,
					'facet'       => false,
					'facet_label' => 'Capacity',
					'weight'      => null,
					'range'       => true,
					'ranges'      => array(
						array(
							'label' => '',
							'from'  => null,
							'to'    => 30,
						),
						array(
							'label' => '',
							'from'  => 30,
							'to'    => null,
						),
					),
				),
			),
		),
	),
	false
);

$settings                   = get_option( 'viglet_dumont_settings', array() );
$settings['search_enabled'] = 1;
$settings['search_mode']    = 'theme';
// Search analytics (site/analytics.spec.mjs): clicks to Turing, events to a stubbed gtag.
$settings['track_clicks']      = 1;
$settings['analytics_ga']      = 1;
$settings['ga_measurement_id'] = 'G-E2ETEST';
// Related content after every post (related.spec.mjs).
$settings['related_after'] = array( 'post' );
update_option( 'viglet_dumont_settings', $settings );

echo wp_json_encode(
	array(
		'searchPageId' => $page_id,
		'corePageId'   => $core_id,
		'askPageId'    => $ask_id,
		'lonelyPageId' => $lonely_id,
		'partsPageId'  => $parts_id,
		'relatedPostId' => $related_id ?? 0,
		'postId'       => $post_id,
	)
);
