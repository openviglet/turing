<?php
/**
 * A piece of the Turing search block (D185). "Did you mean", when Turing has a correction for a
 * search that found something; and for one that found nothing, the corrected results with a way
 * back to the search as typed (D259), or, for a sentence, the results that match some of its
 * words (D258).
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
<p class="viglet-dumont-search__showing-for" role="status" hidden data-wp-bind--hidden="!state.hasShowingFor">
	<?php esc_html_e( 'Showing results for', 'viglet-dumont' ); ?>
	<strong data-wp-text="state.showingFor"></strong>.
	<?php esc_html_e( 'Search instead for', 'viglet-dumont' ); ?>
	<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.searchInstead" data-wp-text="state.insteadOf"></button>
</p>
<p class="viglet-dumont-search__close-matches" role="status" hidden data-wp-bind--hidden="!state.hasCloseMatches">
	<?php esc_html_e( 'Showing close matches: nothing matched every word you searched for.', 'viglet-dumont' ); ?>
</p>
<p class="viglet-dumont-search__correction" hidden data-wp-bind--hidden="!state.hasCorrection">
	<?php esc_html_e( 'Did you mean:', 'viglet-dumont' ); ?>
	<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.applyCorrection" data-wp-text="state.corrected"></button>
</p>
