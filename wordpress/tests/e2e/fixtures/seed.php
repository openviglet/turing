<?php
/**
 * The data the browser tests expect, made idempotent: run through WP-CLI by global-setup
 * (`wp eval-file /e2e/seed.php`) on the run-docker.cmd stack, never on a real site.
 *
 * - twelve posts with a `seats` meta (20 or 40), four of them titled "Workshop on ...";
 * - a page "Busca" holding the Turing search block;
 * - the `seats` field mapped as an INT facet, and search answered by Turing.
 *
 * Prints JSON: { searchPageId, postId } for the specs.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a test fixture run by WP-CLI, not plugin code.

$titles = array(
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
	update_post_meta( $id, 'seats', 0 === $i % 2 ? '20' : '40' );
	delete_post_meta( $id, '_viglet_dumont_exclude' );
	if ( 0 === $i ) {
		$post_id = $id;
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
			),
		),
	),
	false
);

$settings                   = get_option( 'viglet_dumont_settings', array() );
$settings['search_enabled'] = 1;
$settings['search_mode']    = 'theme';
update_option( 'viglet_dumont_settings', $settings );

echo wp_json_encode(
	array(
		'searchPageId' => $page_id,
		'postId'       => $post_id,
	)
);
