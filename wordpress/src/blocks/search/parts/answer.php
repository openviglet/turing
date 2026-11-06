<?php
/**
 * A piece of the Turing search block (D185). Turing's grounded answer, when the block asks for one (D166).
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
?>
<?php if ( ! empty( $attributes['showAnswer'] ) ) : ?>
	<p class="viglet-turing-search__answer-paused" role="status" hidden data-wp-bind--hidden="!state.showAnswerPaused" data-wp-text="state.pausedLabel"></p>
	<section class="viglet-turing-search__answer" aria-labelledby="<?php echo esc_attr( $viglet_turing_id ); ?>-answer" hidden data-wp-bind--hidden="!state.showAnswer" data-wp-bind--aria-busy="state.answerStreaming">
		<h2 class="viglet-turing-search__answer-label" id="<?php echo esc_attr( $viglet_turing_id ); ?>-answer"><?php esc_html_e( 'Answer generated from this site', 'viglet-turing-search' ); ?></h2>
		<div class="viglet-turing-search__answer-text" data-wp-watch="callbacks.renderAnswer"></div>
		<ol class="viglet-turing-search__sources" aria-label="<?php esc_attr_e( 'Sources', 'viglet-turing-search' ); ?>">
			<template data-wp-each--source="state.answerSources" data-wp-each-key="context.source.key">
				<li data-wp-bind--value="context.source.rank">
					<a data-wp-bind--href="context.source.url" data-wp-text="context.source.title" data-wp-on--click="actions.citationClicked"></a>
				</li>
			</template>
		</ol>
		<p class="viglet-turing-search__answer-feedback" data-wp-bind--hidden="state.answerRated">
			<?php esc_html_e( 'Was this helpful?', 'viglet-turing-search' ); ?>
			<button type="button" class="viglet-turing-search__link" data-wp-on--click="actions.answerHelpful"><?php esc_html_e( 'Yes', 'viglet-turing-search' ); ?></button>
			<button type="button" class="viglet-turing-search__link" data-wp-on--click="actions.answerNotHelpful"><?php esc_html_e( 'No', 'viglet-turing-search' ); ?></button>
		</p>
		<p class="viglet-turing-search__answer-feedback" role="status" hidden data-wp-bind--hidden="!state.answerRated"><?php esc_html_e( 'Thank you.', 'viglet-turing-search' ); ?></p>
	</section>
<?php endif; ?>
