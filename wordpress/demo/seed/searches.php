<?php
/**
 * The demo store's search history (D269), run by run-docker after the catalog is indexed: the
 * searches a mountain-gear shop gets, in its language, a believable number of times each. A
 * shop that opened yesterday still has people searching it; without them the search page has
 * no popular searches and the insights hold only whatever the browser suite typed.
 *
 * Once per SN site: a second seed does not add to the counts.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a setup script run by WP-CLI, not plugin code.

$settings = viglet_dumont_get_settings();
$flag     = 'tarnfell_searches_' . sanitize_key( (string) ( $settings['site_name'] ?? '' ) );
if ( get_option( $flag ) ) {
	WP_CLI::log( 'The search history is already there.' );
	return;
}

// What people look for, most first. Every one finds something but the last: a word the shop
// does not use ("cantil" for its insulated bottle, "bothy bag" for its emergency shelter), which
// the story's chapter 8 finds among the searches that found nothing and turns into a synonym.
// The visit itself makes no search that finds nothing (D289).
$searches = 'pt_BR' === get_locale()
	? array(
		'barraca'              => 14,
		'jaqueta impermeável'  => 11,
		'bota de trilha'       => 9,
		'mochila 45 litros'    => 7,
		'saco de dormir'       => 6,
		'jaqueta de pluma'     => 5,
		'lanterna de cabeça'   => 4,
		'bastão de caminhada'  => 3,
		'garrafa térmica'      => 2,
		'cantil'               => 2,
	)
	: array(
		'rain jacket'          => 14,
		'two-person tent'      => 11,
		'hiking boots'         => 9,
		'45 litre backpack'    => 7,
		'sleeping bag'         => 6,
		'down jacket'          => 5,
		'head torch'           => 4,
		'trekking poles'       => 3,
		'insulated bottle'     => 2,
		'bothy bag'            => 2,
	);

$client = new Viglet_Dumont_API_Client( $settings );
$total  = 0;
foreach ( $searches as $query => $times ) {
	for ( $i = 0; $i < $times; $i++ ) {
		if ( ! is_wp_error( $client->search( $query, 1, 1 ) ) ) {
			++$total;
		}
	}
}
update_option( $flag, gmdate( 'c' ) );
WP_CLI::log( "{$total} searches recorded for the store's history." );
