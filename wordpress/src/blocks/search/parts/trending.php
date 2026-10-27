<?php
/**
 * A piece of the Turing search block (D202). Popular searches: chips under the box before a
 * visitor types, each a search this site answers.
 *
 * Drawn by Viglet_Dumont_Search_Block::pieces(), for the block or one of its inner blocks.
 *
 * @package Viglet_Dumont
 *
 * @var array    $attributes         The search block's attributes.
 * @var string   $viglet_dumont_id   The id the block's pieces share.
 * @var callable $viglet_dumont_show Whether a part of a result shows.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

if ( empty( $attributes['showTrending'] ) ) {
	return;
}
$viglet_dumont_chips = ( new Viglet_Dumont_Trending() )->chips();
if ( ! $viglet_dumont_chips ) {
	return;
}
?>
<nav class="viglet-dumont-search__trending" aria-label="<?php esc_attr_e( 'Popular searches', 'viglet-dumont' ); ?>" data-wp-bind--hidden="state.hasQuery"<?php echo '' !== Viglet_Dumont_Search_Block::query() ? ' hidden' : ''; ?>>
	<span class="viglet-dumont-search__trending-label"><?php esc_html_e( 'Popular:', 'viglet-dumont' ); ?></span>
	<ul>
		<?php foreach ( $viglet_dumont_chips as $viglet_dumont_chip ) : ?>
			<li><a class="viglet-dumont-search__chip" href="<?php echo esc_url( add_query_arg( 'q', rawurlencode( $viglet_dumont_chip ) ) ); ?>"><?php echo esc_html( $viglet_dumont_chip ); ?></a></li>
		<?php endforeach; ?>
	</ul>
</nav>
