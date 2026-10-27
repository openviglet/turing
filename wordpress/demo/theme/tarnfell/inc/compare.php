<?php
/**
 * The "Compare searches" block (D195): one search box, WordPress's own search on the left and
 * the plugin's Turing search on the right, on the same query and the same catalog. The left
 * column is core's search as a theme's search page runs it (a WP_Query with `s`), never a
 * weakened copy, so the comparison is honest. Lives in the demo theme: no production site
 * ships a compare page.
 *
 * @package Tarnfell
 */

/**
 * The curated searches (curated.json), each with the feature it shows.
 *
 * @return array<int, array{query: string, shows: string, native?: ?string}>
 */
function tarnfell_curated(): array {
	$file = get_theme_file_path( 'curated.json' );
	$list = is_readable( $file ) ? json_decode( (string) file_get_contents( $file ), true ) : array();

	return is_array( $list ) ? $list : array();
}

/**
 * The block's markup.
 */
function tarnfell_render_compare(): string {
	// A read-only page parameter: nothing is changed, so there is no nonce to check.
	// phpcs:ignore WordPress.Security.NonceVerification.Recommended
	$query = sanitize_text_field( wp_unslash( $_GET['q'] ?? '' ) );

	ob_start();
	?>
	<div class="tarnfell-compare">
		<form class="tarnfell-compare__form" role="search" method="get" action="<?php echo esc_url( get_permalink() ); ?>">
			<?php if ( ! get_option( 'permalink_structure' ) ) : ?>
				<input type="hidden" name="page_id" value="<?php echo esc_attr( (string) get_the_ID() ); ?>" />
			<?php endif; ?>
			<label class="screen-reader-text" for="tarnfell-compare-q">Search both</label>
			<input id="tarnfell-compare-q" type="search" name="q" value="<?php echo esc_attr( $query ); ?>" placeholder="Search both at once…" />
			<button type="submit" class="wp-element-button">Search both</button>
		</form>
		<ul class="tarnfell-compare__examples" aria-label="Try a search">
			<?php foreach ( tarnfell_curated() as $example ) : ?>
				<?php
				// Only the searches both columns can show: a filter or a correction needs the parts
				// the Turing column leaves out.
				if ( ! array_key_exists( 'native', $example ) ) {
					continue;
				}
				?>
				<li>
					<a href="<?php echo esc_url( add_query_arg( 'q', rawurlencode( $example['query'] ) ) ); ?>"<?php echo $example['query'] === $query ? ' aria-current="true"' : ''; ?>><?php echo esc_html( $example['query'] ); ?></a>
					<span><?php echo esc_html( $example['shows'] ); ?></span>
				</li>
			<?php endforeach; ?>
		</ul>
		<?php if ( '' !== $query ) : ?>
			<div class="tarnfell-compare__columns">
				<section class="tarnfell-compare__native" aria-labelledby="tarnfell-native">
					<h2 id="tarnfell-native">WordPress search</h2>
					<?php echo tarnfell_native_results( $query ); // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- escaped where built. ?>
				</section>
				<section class="tarnfell-compare__turing" aria-labelledby="tarnfell-turing">
					<h2 id="tarnfell-turing">Turing search</h2>
					<?php
					// The plugin's search block with its results alone; it reads `q` like this page.
					echo do_blocks( '<!-- wp:viglet-dumont/search {"perPage":5,"showFacets":false} --><!-- wp:viglet-dumont/search-results /--><!-- /wp:viglet-dumont/search -->' ); // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- the block escapes its own output.
					?>
				</section>
			</div>
		<?php endif; ?>
	</div>
	<?php
	return (string) ob_get_clean();
}

/**
 * WordPress's own search for a query, as its search page asks it: products, posts and pages,
 * five of them, with the count it found.
 *
 * @param string $query The visitor's words.
 */
function tarnfell_native_results( string $query ): string {
	$found = new WP_Query(
		array(
			's'              => $query,
			'post_type'      => array( 'product', 'post', 'page' ),
			'post_status'    => 'publish',
			'posts_per_page' => 5,
			'no_found_rows'  => false,
		)
	);
	if ( ! $found->have_posts() ) {
		return '<p class="tarnfell-compare__summary">No results found.</p>';
	}
	$html = sprintf(
		'<p class="tarnfell-compare__summary">%s</p><ol class="tarnfell-compare__results">',
		esc_html( sprintf( 1 === $found->found_posts ? '%d result' : '%d results', $found->found_posts ) )
	);
	foreach ( $found->posts as $post ) {
		$html .= sprintf(
			'<li><a href="%s">%s</a></li>',
			esc_url( get_permalink( $post ) ),
			esc_html( get_the_title( $post ) )
		);
	}

	return $html . '</ol>';
}

add_action(
	'init',
	static function (): void {
		register_block_type(
			'tarnfell/compare',
			array(
				'title'           => 'Compare searches',
				'render_callback' => 'tarnfell_render_compare',
			)
		);
	}
);
