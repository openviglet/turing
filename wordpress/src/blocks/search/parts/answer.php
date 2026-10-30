<?php
/**
 * A piece of the Turing search block (D185). Turing's grounded answer, when the block asks for one (D166).
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
<?php if ( ! empty( $attributes['showAnswer'] ) ) : ?>
	<section class="viglet-dumont-search__answer" aria-labelledby="<?php echo esc_attr( $viglet_dumont_id ); ?>-answer" hidden data-wp-bind--hidden="!state.showAnswer" data-wp-bind--aria-busy="state.answerStreaming">
		<h2 class="viglet-dumont-search__answer-label" id="<?php echo esc_attr( $viglet_dumont_id ); ?>-answer"><?php esc_html_e( 'Answer generated from this site', 'viglet-dumont' ); ?></h2>
		<div class="viglet-dumont-search__answer-text" data-wp-watch="callbacks.renderAnswer"></div>
		<ol class="viglet-dumont-search__sources" aria-label="<?php esc_attr_e( 'Sources', 'viglet-dumont' ); ?>">
			<template data-wp-each--source="state.answerSources" data-wp-each-key="context.source.key">
				<li data-wp-bind--value="context.source.rank">
					<a data-wp-bind--href="context.source.url" data-wp-text="context.source.title" data-wp-on--click="actions.citationClicked"></a>
				</li>
			</template>
		</ol>
		<p class="viglet-dumont-search__answer-feedback" data-wp-bind--hidden="state.answerRated">
			<?php esc_html_e( 'Was this helpful?', 'viglet-dumont' ); ?>
			<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.answerHelpful"><?php esc_html_e( 'Yes', 'viglet-dumont' ); ?></button>
			<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.answerNotHelpful"><?php esc_html_e( 'No', 'viglet-dumont' ); ?></button>
		</p>
		<p class="viglet-dumont-search__answer-feedback" role="status" hidden data-wp-bind--hidden="!state.answerRated"><?php esc_html_e( 'Thank you.', 'viglet-dumont' ); ?></p>
	</section>
<?php endif; ?>
