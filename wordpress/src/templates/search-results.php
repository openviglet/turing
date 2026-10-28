<?php
/**
 * Viglet Dumont Search Results Template
 *
 * Variables available:
 * - $query        (string)  Search query
 * - $page         (int)     Current page
 * - $per_page     (int)     Results per page
 * - $results      (array)   Search result documents
 * - $total        (int)     Total results
 * - $total_pages  (int)     Total pages
 * - $facets       (array)   Facet groups
 * - $did_you_mean (string)  Spelling suggestion
 * - $error        (string)  Error message
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Extracted from the template data by Viglet_Dumont_Search::render_template().
 *
 * @var string      $query
 * @var int         $page
 * @var array       $results
 * @var int         $total
 * @var int         $total_pages
 * @var array       $facets
 * @var string      $did_you_mean
 * @var string|null $error
 */

wp_enqueue_style(
	'viglet-dumont-search',
	VIGLET_DUMONT_URL . 'templates/css/search.css',
	array(),
	VIGLET_DUMONT_VERSION
);
?>

<main id="main" class="site-main">
	<div class="vd-search">
		<header class="vd-search__header">
			<h1 class="vd-search__title">
				<?php
				printf(
					/* translators: %s: the search query. */
					esc_html__( 'Search results for: %s', 'viglet-dumont' ),
					'<span>' . esc_html( $query ) . '</span>'
				);
				?>
			</h1>

			<?php if ( $total > 0 ) : ?>
				<p class="vd-search__count">
					<?php
					printf(
						/* translators: %d: how many results the search found. */
						esc_html( _n( '%d result found', '%d results found', $total, 'viglet-dumont' ) ),
						(int) $total
					);
					?>
				</p>
			<?php endif; ?>
		</header>

		<?php if ( $error ) : ?>
			<div class="vd-search__error">
				<p><?php echo esc_html( $error ); ?></p>
			</div>
		<?php endif; ?>

		<?php if ( ! empty( $did_you_mean ) ) : ?>
			<div class="vd-search__dym">
				<?php esc_html_e( 'Did you mean:', 'viglet-dumont' ); ?>
				<a href="<?php echo esc_url( home_url( '/?s=' . rawurlencode( $did_you_mean ) ) ); ?>">
					<?php echo esc_html( $did_you_mean ); ?>
				</a>
			</div>
		<?php endif; ?>

		<div class="vd-search__layout">
			<?php if ( ! empty( $facets ) ) : ?>
				<aside class="vd-search__sidebar">
					<?php foreach ( $facets as $viglet_dumont_facet ) : ?>
						<?php if ( ! empty( $viglet_dumont_facet['facetValues'] ) ) : ?>
							<div class="vd-facet">
								<h3 class="vd-facet__title">
									<?php echo esc_html( $viglet_dumont_facet['label'] ?? $viglet_dumont_facet['name'] ?? '' ); ?>
								</h3>
								<ul class="vd-facet__list">
									<?php foreach ( $viglet_dumont_facet['facetValues'] as $viglet_dumont_value ) : ?>
										<li>
											<a href="<?php echo esc_url( add_query_arg( 'fq[]', $viglet_dumont_value['link'] ?? '', false ) ); ?>">
												<?php echo esc_html( $viglet_dumont_value['label'] ?? '' ); ?>
												<span class="vd-facet__count"><?php echo esc_html( $viglet_dumont_value['count'] ?? 0 ); ?></span>
											</a>
										</li>
									<?php endforeach; ?>
								</ul>
							</div>
						<?php endif; ?>
					<?php endforeach; ?>
				</aside>
			<?php endif; ?>

			<div class="vd-search__results">
				<?php if ( empty( $results ) && ! $error ) : ?>
					<div class="vd-search__empty">
						<p><?php esc_html_e( 'No results found. Try different keywords.', 'viglet-dumont' ); ?></p>
					</div>
				<?php else : ?>
					<?php foreach ( $results as $viglet_dumont_item ) : ?>
						<?php
						$viglet_dumont_fields = $viglet_dumont_item['fields'] ?? array();
						$viglet_dumont_title  = $viglet_dumont_fields['title'] ?? '';
						$viglet_dumont_url    = $viglet_dumont_fields['url'] ?? '#';
						$viglet_dumont_text   = '';
						if ( ! empty( $viglet_dumont_item['highlighting'] ) ) {
							$viglet_dumont_text = implode( ' ... ', $viglet_dumont_item['highlighting'] );
						} elseif ( ! empty( $viglet_dumont_fields['abstract'] ) ) {
							$viglet_dumont_text = $viglet_dumont_fields['abstract'];
						} elseif ( ! empty( $viglet_dumont_fields['text'] ) ) {
							$viglet_dumont_text = wp_trim_words( $viglet_dumont_fields['text'], 30 );
						}
						$viglet_dumont_author = $viglet_dumont_fields['author'] ?? '';
						$viglet_dumont_date   = $viglet_dumont_fields['publication_date'] ?? '';
						$viglet_dumont_image  = $viglet_dumont_fields['image'] ?? '';
						$viglet_dumont_type   = $viglet_dumont_fields['type'] ?? '';
						?>
						<article class="vd-result">
							<?php if ( $viglet_dumont_image ) : ?>
								<div class="vd-result__image">
									<img src="<?php echo esc_url( $viglet_dumont_image ); ?>"
										alt="<?php echo esc_attr( $viglet_dumont_title ); ?>"
										loading="lazy" />
								</div>
							<?php endif; ?>
							<div class="vd-result__content">
								<?php if ( $viglet_dumont_type ) : ?>
									<span class="vd-result__type"><?php echo esc_html( ucfirst( $viglet_dumont_type ) ); ?></span>
								<?php endif; ?>
								<h2 class="vd-result__title">
									<a href="<?php echo esc_url( $viglet_dumont_url ); ?>"><?php echo esc_html( $viglet_dumont_title ); ?></a>
								</h2>
								<?php if ( $viglet_dumont_text ) : ?>
									<p class="vd-result__excerpt"><?php echo wp_kses_post( $viglet_dumont_text ); ?></p>
								<?php endif; ?>
								<div class="vd-result__meta">
									<?php if ( $viglet_dumont_author ) : ?>
										<span class="vd-result__author"><?php echo esc_html( $viglet_dumont_author ); ?></span>
									<?php endif; ?>
									<?php if ( $viglet_dumont_date ) : ?>
										<time class="vd-result__date" datetime="<?php echo esc_attr( $viglet_dumont_date ); ?>">
											<?php echo esc_html( date_i18n( get_option( 'date_format' ), strtotime( $viglet_dumont_date ) ) ); ?>
										</time>
									<?php endif; ?>
								</div>
							</div>
						</article>
					<?php endforeach; ?>
				<?php endif; ?>

				<?php if ( $total_pages > 1 ) : ?>
					<nav class="vd-pagination">
						<?php
						echo wp_kses_post(
							paginate_links(
								array(
									'base'      => add_query_arg( 'paged', '%#%' ),
									'format'    => '',
									'current'   => $page,
									'total'     => $total_pages,
									'prev_text' => '&laquo; ' . __( 'Previous', 'viglet-dumont' ),
									'next_text' => __( 'Next', 'viglet-dumont' ) . ' &raquo;',
									'type'      => 'list',
								)
							)
						);
						?>
					</nav>
				<?php endif; ?>
			</div>
		</div>
	</div>
</main>
