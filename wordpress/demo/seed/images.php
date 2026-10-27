<?php
/**
 * The demo storefront's pictures (D218), drawn by the seed itself: a flat illustration of each
 * product in its colour, and a fell landscape for each guide. Made for the fictional brand, so
 * the repository may redistribute them (see CREDITS.md), and nothing is downloaded. Each is
 * drawn at twice its size with GD and scaled down for smooth edges, saved as WebP, and imported
 * once: a rerun finds it again by the key it was imported under.
 *
 * Loaded by catalog.php, which calls tarnfell_product_image() and tarnfell_guide_image().
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a setup script run by WP-CLI, not plugin code.

require_once ABSPATH . 'wp-admin/includes/file.php';
require_once ABSPATH . 'wp-admin/includes/media.php';
require_once ABSPATH . 'wp-admin/includes/image.php';

/** Bump to draw every picture again on the next seed. */
const TARNFELL_IMAGES_VERSION = 1;

/** The shop's colours, as the attribute names them. */
const TARNFELL_COLOURS = array(
	'Slate'   => '#5B6670',
	'Moss'    => '#5E6B4F',
	'Ember'   => '#B44A17',
	'Sky'     => '#6FA3C7',
	'Glacier' => '#A9CBD6',
	'Navy'    => '#23395B',
	'Sand'    => '#C9B48A',
	'Black'   => '#2A2E30',
	'Brown'   => '#6B4A2E',
);

/** The backdrop each model sits on, one per model in turn: the theme's quieter colours. */
const TARNFELL_BACKDROPS = array( '#DDE3D3', '#E6DCC8', '#D5E2E8', '#E8E1D6', '#D9DED9' );

/**
 * What a model is drawn as, from its English name.
 *
 * @param string $model    The model's English name.
 * @param string $category The category slug.
 */
function tarnfell_kind( string $model, string $category ): string {
	foreach ( array(
		'vest'      => '/running vest/i',
		'gilet'     => '/down vest/i',
		'trousers'  => '/trousers/i',
		'parka'     => '/parka/i',
		'shoe'      => '/shoe/i',
		'bivvy'     => '/bivvy/i',
		'mat'       => '/mat$/i',
		'liner'     => '/liner/i',
		'gloves'    => '/gloves/i',
		'beanie'    => '/beanie/i',
		'poles'     => '/poles/i',
		'torch'     => '/torch/i',
		'gaiters'   => '/gaiters/i',
		'bottle'    => '/bottle/i',
		'mapcase'   => '/map case/i',
		'drybags'   => '/dry bag/i',
		'stove'     => '/stove/i',
		'cookset'   => '/cook set/i',
		'top'       => '/fleece|base layer|running top|jumper/i',
	) as $kind => $pattern ) {
		if ( preg_match( $pattern, $model ) ) {
			return $kind;
		}
	}

	return array(
		'waterproof-jackets' => 'jacket',
		'insulated-jackets'  => 'jacket',
		'fleece-midlayers'   => 'top',
		'hiking-boots'       => 'boot',
		'backpacks'          => 'pack',
		'tents'              => 'tent',
		'sleeping-bags'      => 'bag',
	)[ $category ] ?? 'drybags';
}

/**
 * A canvas that draws on a 100 by 100 grid, whatever its pixel size.
 */
final class Tarnfell_Canvas {

	/** @var GdImage */
	public $img;

	private float $sx;

	private float $sy;

	public function __construct( int $width, int $height ) {
		$this->img = imagecreatetruecolor( $width, $height );
		$this->sx  = $width / 100;
		$this->sy  = $height / 100;
	}

	/** A colour from #RRGGBB, darkened (below 1) or lightened (above 1). */
	public function colour( string $hex, float $shade = 1.0 ): int {
		$rgb = array_map( 'hexdec', str_split( ltrim( $hex, '#' ), 2 ) );
		$rgb = array_map(
			static fn ( $c ) => (int) max( 0, min( 255, $shade <= 1 ? $c * $shade : $c + ( 255 - $c ) * ( $shade - 1 ) ) ),
			$rgb
		);

		return imagecolorallocate( $this->img, $rgb[0], $rgb[1], $rgb[2] );
	}

	public function fill( int $colour ): void {
		imagefilledrectangle( $this->img, 0, 0, imagesx( $this->img ), imagesy( $this->img ), $colour );
	}

	/** @param float[] $points x, y pairs on the grid. */
	public function poly( array $points, int $colour ): void {
		$pixels = array();
		foreach ( array_chunk( $points, 2 ) as list( $x, $y ) ) {
			$pixels[] = (int) round( $x * $this->sx );
			$pixels[] = (int) round( $y * $this->sy );
		}
		imagefilledpolygon( $this->img, $pixels, $colour );
	}

	public function ellipse( float $cx, float $cy, float $w, float $h, int $colour ): void {
		imagefilledellipse( $this->img, (int) ( $cx * $this->sx ), (int) ( $cy * $this->sy ), (int) ( $w * $this->sx ), (int) ( $h * $this->sy ), $colour );
	}

	public function rect( float $x1, float $y1, float $x2, float $y2, int $colour, float $radius = 0 ): void {
		if ( $radius <= 0 ) {
			imagefilledrectangle( $this->img, (int) ( $x1 * $this->sx ), (int) ( $y1 * $this->sy ), (int) ( $x2 * $this->sx ), (int) ( $y2 * $this->sy ), $colour );
			return;
		}
		$r = $radius;
		$this->rect( $x1 + $r, $y1, $x2 - $r, $y2, $colour );
		$this->rect( $x1, $y1 + $r, $x2, $y2 - $r, $colour );
		foreach ( array( array( $x1 + $r, $y1 + $r ), array( $x2 - $r, $y1 + $r ), array( $x1 + $r, $y2 - $r ), array( $x2 - $r, $y2 - $r ) ) as list( $cx, $cy ) ) {
			$this->ellipse( $cx, $cy, 2 * $r, 2 * $r, $colour );
		}
	}

	public function line( float $x1, float $y1, float $x2, float $y2, int $colour, float $width ): void {
		imagesetthickness( $this->img, max( 1, (int) round( $width * $this->sx ) ) );
		imageline( $this->img, (int) ( $x1 * $this->sx ), (int) ( $y1 * $this->sy ), (int) ( $x2 * $this->sx ), (int) ( $y2 * $this->sy ), $colour );
		imagesetthickness( $this->img, 1 );
	}

	/** Scaled to the given size and written as WebP. */
	public function save( string $path, int $width, int $height ): void {
		$out = imagecreatetruecolor( $width, $height );
		imagecopyresampled( $out, $this->img, 0, 0, 0, 0, $width, $height, imagesx( $this->img ), imagesy( $this->img ) );
		imagewebp( $out, $path, 82 );
	}
}

/**
 * Draw one product kind in a colour on a backdrop.
 *
 * @param Tarnfell_Canvas $c        The canvas.
 * @param string          $kind     What to draw (tarnfell_kind()).
 * @param string          $hex      The product's colour.
 * @param string          $backdrop The backdrop's colour.
 */
function tarnfell_draw_product( Tarnfell_Canvas $c, string $kind, string $hex, string $backdrop ): void {
	$c->fill( $c->colour( '#F4F1EA' ) );
	$c->ellipse( 50, 52, 84, 84, $c->colour( $backdrop ) );
	$main  = $c->colour( $hex );
	$dark  = $c->colour( $hex, 0.72 );
	$light = $c->colour( $hex, 1.35 );
	$metal = $c->colour( '#8A9096' );
	$ink   = $c->colour( '#1B1F1C' );
	$paper = $c->colour( '#FFFFFF' );
	$bg    = $c->colour( $backdrop );

	switch ( $kind ) {
		case 'jacket':
		case 'parka':
		case 'top':
		case 'gilet':
			$hem = 'parka' === $kind ? 90 : 82;
			if ( 'jacket' === $kind || 'parka' === $kind ) {
				$c->ellipse( 50, 25, 24, 18, $dark );
			}
			if ( 'gilet' !== $kind ) {
				$c->poly( array( 36, 28, 22, 36, 14, 74, 22, 76, 30, 46 ), $main );
				$c->poly( array( 64, 28, 78, 36, 86, 74, 78, 76, 70, 46 ), $main );
				$c->rect( 13, 72, 23, 77, $dark );
				$c->rect( 77, 72, 87, 77, $dark );
			}
			$c->poly( array( 36, 27, 64, 27, 68, $hem, 32, $hem ), $main );
			if ( 'top' === $kind ) {
				$c->ellipse( 50, 28, 14, 7, $dark );
			}
			$c->line( 50, 30, 50, $hem, $dark, 1.2 );
			$c->rect( 38, 56, 46, 58, $dark );
			$c->rect( 54, 56, 62, 58, $dark );
			if ( 'gilet' === $kind ) {
				foreach ( array( 40, 52, 64, 76 ) as $y ) {
					$c->line( 34, $y, 66, $y, $dark, 0.6 );
				}
			}
			break;
		case 'trousers':
			$c->rect( 34, 16, 66, 23, $dark );
			$c->poly( array( 34, 23, 50, 23, 48, 88, 35, 88 ), $main );
			$c->poly( array( 50, 23, 66, 23, 65, 88, 52, 88 ), $main );
			$c->line( 42, 30, 41, 86, $light, 0.8 );
			$c->line( 58, 30, 59, 86, $light, 0.8 );
			break;
		case 'boot':
			$c->poly( array( 38, 20, 58, 20, 60, 60, 38, 62 ), $main );
			$c->poly( array( 34, 58, 60, 56, 78, 66, 82, 78, 32, 78 ), $main );
			$c->rect( 30, 76, 84, 84, $ink, 2 );
			$c->rect( 36, 18, 60, 24, $dark, 2 );
			foreach ( array( 30, 38, 46, 54 ) as $y ) {
				$c->line( 44, $y, 56, $y + 2, $light, 1 );
			}
			break;
		case 'shoe':
			$c->poly( array( 28, 54, 54, 48, 78, 60, 84, 72, 26, 72 ), $main );
			$c->rect( 24, 70, 86, 78, $ink, 2 );
			foreach ( array( 52, 58, 64 ) as $x ) {
				$c->line( $x - 4, 54, $x + 2, 60, $light, 1 );
			}
			break;
		case 'pack':
			$c->line( 36, 28, 32, 80, $dark, 2.5 );
			$c->line( 64, 28, 68, 80, $dark, 2.5 );
			$c->rect( 30, 22, 70, 86, $main, 10 );
			$c->rect( 30, 16, 70, 34, $dark, 8 );
			$c->rect( 37, 54, 63, 78, $c->colour( $hex, 0.86 ), 5 );
			$c->line( 37, 60, 63, 60, $dark, 0.8 );
			break;
		case 'vest':
			$c->poly( array( 30, 22, 42, 22, 44, 82, 28, 82 ), $main );
			$c->poly( array( 58, 22, 70, 22, 72, 82, 56, 82 ), $main );
			$c->rect( 29, 44, 43, 62, $dark, 3 );
			$c->rect( 57, 44, 71, 62, $dark, 3 );
			$c->line( 42, 40, 58, 40, $dark, 1.5 );
			$c->line( 42, 66, 58, 66, $dark, 1.5 );
			break;
		case 'tent':
			$c->poly( array( 12, 80, 50, 24, 88, 80 ), $main );
			$c->poly( array( 50, 24, 88, 80, 70, 80 ), $dark );
			$c->poly( array( 40, 80, 50, 50, 60, 80 ), $c->colour( $hex, 0.55 ) );
			$c->line( 8, 81, 92, 81, $ink, 1.2 );
			break;
		case 'bivvy':
			$c->ellipse( 54, 66, 76, 20, $main );
			$c->ellipse( 24, 60, 22, 26, $dark );
			$c->ellipse( 24, 62, 12, 12, $c->colour( $hex, 0.5 ) );
			$c->line( 8, 77, 92, 77, $ink, 1.2 );
			break;
		case 'bag':
		case 'liner':
			$fill = 'liner' === $kind ? $light : $main;
			$c->ellipse( 50, 24, 32, 22, $fill );
			$c->poly( array( 34, 24, 66, 24, 60, 86, 40, 86 ), $fill );
			$c->ellipse( 50, 86, 20, 8, $fill );
			$c->ellipse( 50, 24, 16, 10, $dark );
			if ( 'bag' === $kind ) {
				foreach ( array( 38, 50, 62, 74 ) as $y ) {
					$c->line( 37, $y, 63, $y, $dark, 0.6 );
				}
			}
			$c->line( 60, 30, 57, 84, $dark, 0.8 );
			break;
		case 'mat':
			$c->rect( 26, 14, 74, 88, $main, 8 );
			foreach ( array( 24, 32, 40, 48, 56, 64, 72, 80 ) as $y ) {
				$c->line( 29, $y, 71, $y, $dark, 0.7 );
			}
			$c->rect( 64, 16, 70, 20, $ink, 1 );
			break;
		case 'gloves':
			foreach ( array( 37, 43.5, 50, 56.5 ) as $x ) {
				$c->rect( $x, 18, $x + 6, 46, $main, 3 );
			}
			$c->poly( array( 37, 52, 26, 40, 22, 45, 33, 64 ), $main );
			$c->rect( 36, 36, 63, 76, $main, 6 );
			$c->rect( 34, 70, 65, 86, $dark, 2 );
			break;
		case 'beanie':
			// The crown, the top half of an ellipse, then the folded band.
			$dome = array();
			for ( $a = 0; $a <= 180; $a += 10 ) {
				$dome[] = 50 + 29 * cos( deg2rad( $a ) );
				$dome[] = 64 - 36 * sin( deg2rad( $a ) );
			}
			$c->poly( $dome, $main );
			$c->rect( 20, 60, 80, 76, $dark, 3 );
			$c->ellipse( 50, 27, 14, 14, $dark );
			break;
		case 'poles':
			$c->line( 42, 16, 32, 88, $metal, 2 );
			$c->line( 58, 16, 68, 88, $metal, 2 );
			$c->rect( 38, 12, 46, 30, $main, 3 );
			$c->rect( 54, 12, 62, 30, $main, 3 );
			$c->ellipse( 33.5, 80, 8, 3, $ink );
			$c->ellipse( 66.5, 80, 8, 3, $ink );
			break;
		case 'torch':
			$c->ellipse( 50, 52, 70, 42, $dark );
			$c->ellipse( 50, 52, 60, 32, $bg );
			$c->rect( 38, 56, 62, 78, $main, 4 );
			$c->ellipse( 50, 67, 12, 12, $c->colour( '#FFF6CF' ) );
			break;
		case 'gaiters':
			$c->rect( 28, 20, 46, 82, $main, 5 );
			$c->rect( 54, 20, 72, 82, $main, 5 );
			$c->line( 37, 22, 37, 80, $dark, 1 );
			$c->line( 63, 22, 63, 80, $dark, 1 );
			$c->rect( 27, 22, 47, 26, $dark, 1 );
			$c->rect( 53, 22, 73, 26, $dark, 1 );
			break;
		case 'bottle':
			$c->rect( 38, 24, 62, 88, $main, 9 );
			$c->rect( 41, 12, 59, 26, $dark, 2 );
			$c->rect( 38, 46, 62, 52, $light );
			break;
		case 'mapcase':
			$c->line( 30, 32, 50, 12, $ink, 0.8 );
			$c->line( 70, 32, 50, 12, $ink, 0.8 );
			$c->rect( 24, 30, 76, 86, $main, 3 );
			$c->rect( 28, 38, 72, 82, $paper, 2 );
			foreach ( array( 46, 54, 62, 70 ) as $y ) {
				$c->line( 31, $y, 69, $y + 3, $c->colour( '#A9CBD6' ), 0.6 );
			}
			$c->line( 48, 38, 52, 82, $c->colour( '#5E6B4F' ), 0.8 );
			break;
		case 'stove':
			$c->rect( 36, 58, 64, 88, $main, 5 );
			$c->rect( 42, 46, 58, 58, $metal, 2 );
			$c->line( 42, 46, 30, 38, $metal, 1.5 );
			$c->line( 58, 46, 70, 38, $metal, 1.5 );
			$c->ellipse( 50, 40, 10, 8, $c->colour( '#6FA3C7' ) );
			break;
		case 'cookset':
			$c->rect( 26, 42, 74, 84, $main, 4 );
			$c->ellipse( 50, 42, 50, 9, $dark );
			$c->line( 74, 54, 90, 50, $dark, 2 );
			$c->rect( 26, 56, 74, 60, $light );
			break;
		default: // Dry bags: three rolled tops in three sizes.
			foreach ( array( array( 18, 50, 38, 86 ), array( 40, 34, 62, 86 ), array( 64, 44, 84, 86 ) ) as $i => list( $x1, $y1, $x2, $y2 ) ) {
				$c->rect( $x1, $y1, $x2, $y2, 1 === $i ? $main : $c->colour( $hex, 0.86 + 0.14 * $i ), 4 );
				$c->rect( $x1 - 1, $y1 - 6, $x2 + 1, $y1 + 2, $dark, 2 );
			}
	}
}

/**
 * Draw a fell landscape, its ridges set by the seed so each guide gets its own.
 *
 * @param Tarnfell_Canvas $c    The canvas.
 * @param int             $seed The guide's number.
 */
function tarnfell_draw_guide( Tarnfell_Canvas $c, int $seed ): void {
	mt_srand( 4000 + $seed );
	$skies = array( '#D5E2E8', '#E6DCC8', '#DDE3D3', '#F0E2D0' );
	$c->fill( $c->colour( $skies[ $seed % count( $skies ) ] ) );
	$c->ellipse( 20 + mt_rand( 0, 60 ), 18 + mt_rand( 0, 10 ), 9, 16, $c->colour( '#F4F1EA' ) );
	foreach ( array( array( '#7C8C86', 34 ), array( '#5E6B4F', 48 ), array( '#2E5E73', 62 ), array( '#1F3B2D', 76 ) ) as list( $hex, $base ) ) {
		$points = array( 0, 100 );
		for ( $x = 0; $x <= 100; $x += 10 ) {
			$points[] = $x;
			$points[] = $base - mt_rand( 0, 22 );
		}
		array_push( $points, 100, 100 );
		$c->poly( $points, $c->colour( $hex ) );
	}
	// A tarn in the valley.
	$c->ellipse( 30 + mt_rand( 0, 40 ), 90, 40, 8, $c->colour( '#6FA3C7' ) );
}

/**
 * The attachment drawn and imported under this key, or 0 when it is not there yet.
 *
 * @param string $key What the picture is, with the drawing version.
 */
function tarnfell_find_image( string $key ): int {
	$found = get_posts(
		array(
			'post_type'   => 'attachment',
			'post_status' => 'inherit',
			'meta_key'    => '_tarnfell_image',
			'meta_value'  => $key,
			'fields'      => 'ids',
			'numberposts' => 1,
		)
	);

	return $found ? (int) $found[0] : 0;
}

/**
 * Import a drawn WebP as an attachment, keyed so the next seed finds it again.
 *
 * @param string   $key    The key.
 * @param string   $name   The file name, without extension.
 * @param string   $alt    The picture's alternative text.
 * @param callable $draw   Draws it: fn ( string $path ) => void.
 */
function tarnfell_import_image( string $key, string $name, string $alt, callable $draw ): int {
	$id = tarnfell_find_image( $key );
	if ( ! $id ) {
		$tmp = wp_tempnam( $name . '.webp' );
		$draw( $tmp );
		$id = media_handle_sideload(
			array(
				'name'     => $name . '.webp',
				'tmp_name' => $tmp,
			),
			0,
			$alt
		);
		if ( is_wp_error( $id ) ) {
			@unlink( $tmp );
			fwrite( STDERR, "Image {$name}: " . $id->get_error_message() . "\n" );
			return 0;
		}
		update_post_meta( $id, '_tarnfell_image', $key );
	}
	update_post_meta( $id, '_wp_attachment_image_alt', $alt );

	return (int) $id;
}

/**
 * A product's picture: its model's kind, in its colour, on its model's backdrop. 800 px square.
 *
 * @param int    $model_index The model's place in the catalog, from 0.
 * @param string $model       The model's English name.
 * @param string $category    The category slug.
 * @param string $colour      The English colour name.
 * @param string $alt         The product's name, as the site shows it.
 */
function tarnfell_product_image( int $model_index, string $model, string $category, string $colour, string $alt ): int {
	$kind     = tarnfell_kind( $model, $category );
	$hex      = TARNFELL_COLOURS[ $colour ] ?? '#5B6670';
	$backdrop = TARNFELL_BACKDROPS[ $model_index % count( TARNFELL_BACKDROPS ) ];
	$name     = sanitize_title( "{$model}-{$colour}" );

	return tarnfell_import_image(
		'v' . TARNFELL_IMAGES_VERSION . ":product:{$name}",
		$name,
		$alt,
		static function ( string $path ) use ( $kind, $hex, $backdrop ) {
			$c = new Tarnfell_Canvas( 1600, 1600 );
			tarnfell_draw_product( $c, $kind, $hex, $backdrop );
			$c->save( $path, 800, 800 );
		}
	);
}

/**
 * A guide's picture: a fell landscape of its own. 800 by 450.
 *
 * @param int    $index The guide's place in the journal, from 0.
 * @param string $slug  The guide's slug.
 * @param string $alt   What the picture shows, in the site's language.
 */
function tarnfell_guide_image( int $index, string $slug, string $alt ): int {
	return tarnfell_import_image(
		'v' . TARNFELL_IMAGES_VERSION . ":guide:{$slug}",
		$slug,
		$alt,
		static function ( string $path ) use ( $index ) {
			$c = new Tarnfell_Canvas( 1600, 900 );
			tarnfell_draw_guide( $c, $index );
			$c->save( $path, 800, 450 );
		}
	);
}
