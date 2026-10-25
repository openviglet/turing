<?php
/**
 * One part of the Turing search block, as an inner block (D185): the search box, the
 * filters, the results or the pages. It draws the same pieces the block draws in its
 * default order, from the block's attributes (its context), inside the block's store.
 *
 * @package Viglet_Dumont
 *
 * @var array    $attributes The part's attributes.
 * @var WP_Block $block      The part, with the search block's attributes as its context.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$viglet_dumont_part = substr( $block->name, strlen( 'viglet-dumont/search-' ) );
if ( ! isset( Viglet_Dumont_Search_Block::PARTS[ $viglet_dumont_part ] ) ) {
	return;
}
?>
<div <?php echo get_block_wrapper_attributes( array( 'class' => 'viglet-dumont-search__part' ) ); // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- core escapes the wrapper attributes. ?>>
	<?php
	$viglet_dumont_html = Viglet_Dumont_Search_Block::pieces(
		Viglet_Dumont_Search_Block::PARTS[ $viglet_dumont_part ],
		Viglet_Dumont_Search_Block::attributes_from( $block->context )
	);
	echo $viglet_dumont_html; // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- the pieces escape their own output.
	?>
</div>
