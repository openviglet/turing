<?php
/**
 * One part of the Turing search block, as an inner block (D185): the search box, the
 * filters, the results or the pages. It draws the same pieces the block draws in its
 * default order, from the block's attributes (its context), inside the block's store.
 *
 * @package Viglet_Turing
 *
 * @var array    $attributes The part's attributes.
 * @var WP_Block $block      The part, with the search block's attributes as its context.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$viglet_turing_part = substr( $block->name, strlen( 'viglet-turing/search-' ) );
if ( ! isset( Viglet_Turing_Search_Block::PARTS[ $viglet_turing_part ] ) ) {
	return;
}
?>
<div <?php echo wp_kses_data( get_block_wrapper_attributes( array( 'class' => 'viglet-turing-search__part' ) ) ); ?>>
	<?php
	Viglet_Turing_Search_Block::render(
		Viglet_Turing_Search_Block::PARTS[ $viglet_turing_part ],
		Viglet_Turing_Search_Block::attributes_from( $block->context )
	);
	?>
</div>
