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
 */

if ( ! defined( 'ABSPATH' ) ) {
    exit;
}

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
                    esc_html__( 'Search results for: %s', 'viglet-dumont' ),
                    '<span>' . esc_html( $query ) . '</span>'
                );
                ?>
            </h1>

            <?php if ( $total > 0 ) : ?>
                <p class="vd-search__count">
                    <?php
                    printf(
                        esc_html( _n( '%d result found', '%d results found', $total, 'viglet-dumont' ) ),
                        $total
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
                <a href="<?php echo esc_url( home_url( '/?s=' . urlencode( $did_you_mean ) ) ); ?>">
                    <?php echo esc_html( $did_you_mean ); ?>
                </a>
            </div>
        <?php endif; ?>

        <div class="vd-search__layout">
            <?php if ( ! empty( $facets ) ) : ?>
                <aside class="vd-search__sidebar">
                    <?php foreach ( $facets as $facet ) : ?>
                        <?php if ( ! empty( $facet['facetValues'] ) ) : ?>
                            <div class="vd-facet">
                                <h3 class="vd-facet__title">
                                    <?php echo esc_html( $facet['label'] ?? $facet['name'] ?? '' ); ?>
                                </h3>
                                <ul class="vd-facet__list">
                                    <?php foreach ( $facet['facetValues'] as $value ) : ?>
                                        <li>
                                            <a href="<?php echo esc_url( add_query_arg( 'fq[]', $value['link'] ?? '', false ) ); ?>">
                                                <?php echo esc_html( $value['label'] ?? '' ); ?>
                                                <span class="vd-facet__count"><?php echo esc_html( $value['count'] ?? 0 ); ?></span>
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
                    <?php foreach ( $results as $item ) : ?>
                        <?php
                        $fields = $item['fields'] ?? array();
                        $title  = $fields['title'] ?? '';
                        $url    = $fields['url'] ?? '#';
                        $text   = '';
                        if ( ! empty( $item['highlighting'] ) ) {
                            $text = implode( ' ... ', $item['highlighting'] );
                        } elseif ( ! empty( $fields['abstract'] ) ) {
                            $text = $fields['abstract'];
                        } elseif ( ! empty( $fields['text'] ) ) {
                            $text = wp_trim_words( $fields['text'], 30 );
                        }
                        $author = $fields['author'] ?? '';
                        $date   = $fields['publication_date'] ?? '';
                        $image  = $fields['image'] ?? '';
                        $type   = $fields['type'] ?? '';
                        ?>
                        <article class="vd-result">
                            <?php if ( $image ) : ?>
                                <div class="vd-result__image">
                                    <img src="<?php echo esc_url( $image ); ?>"
                                         alt="<?php echo esc_attr( $title ); ?>"
                                         loading="lazy" />
                                </div>
                            <?php endif; ?>
                            <div class="vd-result__content">
                                <?php if ( $type ) : ?>
                                    <span class="vd-result__type"><?php echo esc_html( ucfirst( $type ) ); ?></span>
                                <?php endif; ?>
                                <h2 class="vd-result__title">
                                    <a href="<?php echo esc_url( $url ); ?>"><?php echo esc_html( $title ); ?></a>
                                </h2>
                                <?php if ( $text ) : ?>
                                    <p class="vd-result__excerpt"><?php echo wp_kses_post( $text ); ?></p>
                                <?php endif; ?>
                                <div class="vd-result__meta">
                                    <?php if ( $author ) : ?>
                                        <span class="vd-result__author"><?php echo esc_html( $author ); ?></span>
                                    <?php endif; ?>
                                    <?php if ( $date ) : ?>
                                        <time class="vd-result__date" datetime="<?php echo esc_attr( $date ); ?>">
                                            <?php echo esc_html( date_i18n( get_option( 'date_format' ), strtotime( $date ) ) ); ?>
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
                        echo paginate_links( array(
                            'base'      => add_query_arg( 'paged', '%#%' ),
                            'format'    => '',
                            'current'   => $page,
                            'total'     => $total_pages,
                            'prev_text' => '&laquo; ' . __( 'Previous', 'viglet-dumont' ),
                            'next_text' => __( 'Next', 'viglet-dumont' ) . ' &raquo;',
                            'type'      => 'list',
                        ) );
                        ?>
                    </nav>
                <?php endif; ?>
            </div>
        </div>
    </div>
</main>
