<?php
/**
 * A contact sheet of every picture images.php draws (D218), to look at before seeding:
 * `docker compose run --rm --no-deps cli-demo wp eval-file /demo/seed/preview-images.php`,
 * then open /wp-content/uploads/tarnfell-preview.webp on the demo site.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a setup script run by WP-CLI, not plugin code.

require_once __DIR__ . '/images.php';

$kinds   = array( 'jacket', 'parka', 'gilet', 'top', 'trousers', 'boot', 'shoe', 'pack', 'vest', 'tent', 'bivvy', 'bag', 'mat', 'liner', 'gloves', 'beanie', 'poles', 'torch', 'gaiters', 'bottle', 'mapcase', 'drybags', 'stove', 'cookset' );
$colours = array_keys( TARNFELL_COLOURS );
$cell    = 200;
$cols    = 6;
$rows    = (int) ceil( count( $kinds ) / $cols ) + 2;
$sheet   = imagecreatetruecolor( $cols * $cell, $rows * $cell );
imagefill( $sheet, 0, 0, imagecolorallocate( $sheet, 255, 255, 255 ) );

foreach ( $kinds as $i => $kind ) {
	$c = new Tarnfell_Canvas( 800, 800 );
	tarnfell_draw_product( $c, $kind, TARNFELL_COLOURS[ $colours[ $i % count( $colours ) ] ], TARNFELL_BACKDROPS[ $i % count( TARNFELL_BACKDROPS ) ] );
	imagecopyresampled( $sheet, $c->img, ( $i % $cols ) * $cell, intdiv( $i, $cols ) * $cell, 0, 0, $cell, $cell, 800, 800 );
}
for ( $g = 0; $g < 4; $g++ ) {
	$c = new Tarnfell_Canvas( 800, 450 );
	tarnfell_draw_guide( $c, $g );
	imagecopyresampled( $sheet, $c->img, ( $g % 2 ) * 3 * $cell, ( $rows - 2 + intdiv( $g, 2 ) ) * $cell, 0, 0, 3 * $cell, $cell, 800, 450 );
}

$out = wp_upload_dir()['basedir'] . '/tarnfell-preview.webp';
imagewebp( $sheet, $out, 85 );
echo $out, "\n";
