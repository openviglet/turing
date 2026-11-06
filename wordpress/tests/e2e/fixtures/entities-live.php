<?php
/**
 * Turing's entity extraction on a real post (D310, Turing T1228), for ai/entities.spec.mjs:
 * `wp eval-file /e2e/entities-live.php` opts in with one name variant, publishes a post
 * naming a person (written as the variant), an institution and two places, indexes it and
 * prints what Turing extracted and what each facet value finds; `... cleanup` puts the
 * site back. Every name here is invented.
 *
 * @package Viglet_Turing
 */

// phpcs:disable WordPress.WP.GlobalVariablesOverride.Prohibited, WordPress.NamingConventions.PrefixAllGlobals -- a WP-CLI fixture, not plugin code.

$step = $args[0] ?? 'run';

if ( 'cleanup' === $step ) {
	$post = get_page_by_path( 'entities-live-check', OBJECT, 'post' );
	if ( $post ) {
		wp_delete_post( $post->ID, true );
		( new Viglet_Turing_Operations() )->index( array( $post->ID ) );
	}
	$stored = (array) get_option( 'viglet_turing_settings', array() );
	unset( $stored['entity_extraction'], $stored['name_variants'], $stored['generative_ai'] );
	update_option( 'viglet_turing_settings', $stored );
	( new Viglet_Turing_Entities( viglet_turing_get_settings() ) )->sync_variants( new Viglet_Turing_API_Client(), array() );
	( new Viglet_Turing_Mapping( viglet_turing_get_settings() ) )->provision( new Viglet_Turing_API_Client(), true );
	echo wp_json_encode( array( 'cleaned' => true ) ), "\n";
	return;
}

$stored                      = (array) get_option( 'viglet_turing_settings', array() );
$stored['generative_ai']     = 1;
$stored['entity_extraction'] = 1;
$stored['name_variants']     = 'Ana Paula Lima | Lima, Ana Paula';
update_option( 'viglet_turing_settings', $stored );
$settings = viglet_turing_get_settings();
$client   = new Viglet_Turing_API_Client( $settings );
( new Viglet_Turing_Mapping( $settings ) )->provision( $client, true );
( new Viglet_Turing_Entities( $settings ) )->sync_variants( $client, Viglet_Turing_Entities::variants( $settings['name_variants'] ) );

$existing = get_page_by_path( 'entities-live-check', OBJECT, 'post' );
$id       = wp_insert_post(
	array(
		'ID'           => $existing ? $existing->ID : 0,
		'post_title'   => 'Coastal research week in Recife',
		'post_name'    => 'entities-live-check',
		'post_status'  => 'publish',
		'post_content' => 'The opening lecture is given by Lima, Ana Paula, who leads the marine biology group at the Federal University of Pernambuco. The week runs in Recife, with field trips to Porto de Galinhas.',
	)
);
( new Viglet_Turing_Operations( $settings ) )->index( array( $id ) );

$indexer  = new Viglet_Turing_Indexer( $settings );
$doc_id   = $indexer->build_doc_id( $id );
$entities = array();
// Turing reads the post with its LLM while it imports: wait until the names are there.
for ( $try = 0; $try < 24 && ! $entities; $try++ ) {
	sleep( 5 );
	$found = ( new Viglet_Turing_Entities( $settings ) )->of_document( $client, $doc_id );
	foreach ( is_array( $found ) ? $found : array() as $kind ) {
		if ( $kind['values'] ) {
			$entities[ $kind['field'] ] = $kind['values'];
		}
	}
}
$finds = array();
foreach ( array( 'entity_person:Ana Paula Lima', 'entity_place:Recife' ) as $filter ) {
	$hits             = $client->search( '*', 1, 5, array( 'fq[]' => array( $indexer->own_filter(), $filter ) ) );
	$finds[ $filter ] = array_map( static fn ( $d ) => $d['fields']['id'] ?? '', $hits['results']['document'] ?? array() );
}
echo wp_json_encode(
	array(
		'doc'      => $doc_id,
		'entities' => $entities,
		'finds'    => $finds,
	)
), "\n";
