<?php
/**
 * A piece of the Turing search block (D202). Popular searches: chips under the box before a
 * visitor types, each a search this site answers.
 *
 * Drawn by Viglet_Turing_Search_Block::pieces(), for the block or one of its inner blocks.
 *
 * @package Viglet_Turing
 *
 * @var array    $attributes         The search block's attributes.
 * @var string   $viglet_turing_id   The id the block's pieces share.
 * @var callable $viglet_turing_show Whether a part of a result shows.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

// A scoped block (D306) shows none: the site's popular searches may find nothing in its scope,
// and checking each chip against the scope would cost a search per chip.
if ( empty( $attributes['showTrending'] ) || array() !== Viglet_Turing_Search_Block::scope( $attributes ) ) {
	return;
}
$viglet_turing_chips = ( new Viglet_Turing_Trending() )->chips();
if ( ! $viglet_turing_chips ) {
	return;
}
?>
<nav class="viglet-turing-search__trending" aria-label="<?php esc_attr_e( 'Popular searches', 'viglet-turing-search' ); ?>" data-wp-bind--hidden="state.hasQuery"<?php echo '' !== Viglet_Turing_Search_Block::query() ? ' hidden' : ''; ?>>
	<span class="viglet-turing-search__trending-label"><?php esc_html_e( 'Popular:', 'viglet-turing-search' ); ?></span>
	<ul>
		<?php foreach ( $viglet_turing_chips as $viglet_turing_chip ) : ?>
			<li><a class="viglet-turing-search__chip" href="<?php echo esc_url( add_query_arg( 'q', rawurlencode( $viglet_turing_chip ) ) ); ?>"><?php echo esc_html( $viglet_turing_chip ); ?></a></li>
		<?php endforeach; ?>
	</ul>
</nav>
