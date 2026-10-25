<?php
/**
 * A piece of the Turing search block (D185). "Did you mean", when Turing has a correction.
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
?>
<p class="viglet-dumont-search__correction" hidden data-wp-bind--hidden="!state.hasCorrection">
	<?php esc_html_e( 'Did you mean:', 'viglet-dumont' ); ?>
	<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.applyCorrection" data-wp-text="state.corrected"></button>
</p>
