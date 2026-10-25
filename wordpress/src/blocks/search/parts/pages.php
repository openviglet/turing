<?php
/**
 * A piece of the Turing search block (D185). The result pages.
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
<nav class="viglet-dumont-search__pages" hidden data-wp-bind--hidden="!state.hasPages" aria-label="<?php esc_attr_e( 'Result pages', 'viglet-dumont' ); ?>">
	<template data-wp-each--page="state.pages">
		<button type="button" data-wp-on--click="actions.goToPage" data-wp-bind--aria-current="context.page.current" data-wp-text="context.page.text"></button>
	</template>
</nav>
