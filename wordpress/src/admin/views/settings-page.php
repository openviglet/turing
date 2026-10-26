<?php
/**
 * The settings page, included by Viglet_Dumont_Admin::render_settings_page().
 *
 * Variables available: $settings (array), $post_types (WP_Post_Type[]).
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Set by the method that includes this view.
 *
 * @var array          $settings
 * @var WP_Post_Type[] $post_types
 */
?>
<div class="wrap viglet-dumont-wrap">
	<div class="viglet-dumont-header">
		<div class="viglet-dumont-header__inner">
			<h1>
				<span class="dashicons dashicons-search"></span>
				<?php esc_html_e( 'Viglet Dumont', 'viglet-dumont' ); ?>
			</h1>
			<span class="viglet-dumont-version">v<?php echo esc_html( VIGLET_DUMONT_VERSION ); ?></span>
		</div>
		<p class="viglet-dumont-header__desc">
			<?php esc_html_e( 'Integrate WordPress with Viglet Turing search engine for powerful content indexing and search.', 'viglet-dumont' ); ?>
		</p>
	</div>

	<div class="viglet-dumont-content">
		<div class="viglet-dumont-tabs">
			<nav class="viglet-dumont-tabs__nav">
				<a href="#connection" class="viglet-dumont-tab active" data-tab="connection">
					<span class="dashicons dashicons-admin-site-alt3"></span>
					<?php esc_html_e( 'Connection', 'viglet-dumont' ); ?>
				</a>
				<a href="#indexing" class="viglet-dumont-tab" data-tab="indexing">
					<span class="dashicons dashicons-database-add"></span>
					<?php esc_html_e( 'Indexing', 'viglet-dumont' ); ?>
				</a>
				<a href="#search" class="viglet-dumont-tab" data-tab="search">
					<span class="dashicons dashicons-search"></span>
					<?php esc_html_e( 'Search', 'viglet-dumont' ); ?>
				</a>
				<a href="#actions" class="viglet-dumont-tab" data-tab="actions">
					<span class="dashicons dashicons-controls-play"></span>
					<?php esc_html_e( 'Actions', 'viglet-dumont' ); ?>
				</a>
			</nav>
		</div>

		<form method="post" action="options.php" id="viglet-dumont-settings-form">
			<?php
			settings_fields( 'viglet_dumont_settings_group' );

			// A value set by wp-config.php or the environment is shown read-only, naming what sets it.
			$sources  = Viglet_Dumont_Settings::sources();
			$locked   = static fn ( string $key ) => Viglet_Dumont_Settings::is_overridden( $key ) ? 'readonly' : '';
			$env_note = static function ( string $key ) use ( $sources ) {
				if ( Viglet_Dumont_Settings::is_overridden( $key ) ) {
					printf(
						'<p class="description"><strong>%s</strong></p>',
						esc_html(
							sprintf(
								/* translators: %s: the constant or environment variable's name. */
								__( 'Set by %s outside WordPress; change it there.', 'viglet-dumont' ),
								$sources[ $key ]['name']
							)
						)
					);
				}
			};
			?>

			<!-- Connection Tab -->
			<div class="viglet-dumont-panel" id="panel-connection">
				<div class="viglet-dumont-card">
					<div class="viglet-dumont-card__header">
						<h2><?php esc_html_e( 'Server Connection', 'viglet-dumont' ); ?></h2>
						<p><?php esc_html_e( 'Configure the connection to your Viglet Turing server.', 'viglet-dumont' ); ?></p>
					</div>
					<div class="viglet-dumont-card__body">
						<div class="viglet-dumont-field">
							<label for="server_url"><?php esc_html_e( 'Server URL', 'viglet-dumont' ); ?></label>
							<input type="url" id="server_url"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[server_url]"
									value="<?php echo esc_attr( $settings['server_url'] ); ?>"
									class="regular-text" <?php echo esc_attr( $locked( 'server_url' ) ); ?>
									placeholder="http://localhost:2700" />
							<p class="description"><?php esc_html_e( 'The full URL of your Turing server (e.g., http://localhost:2700).', 'viglet-dumont' ); ?></p>
							<?php $env_note( 'server_url' ); ?>
						</div>

						<div class="viglet-dumont-field">
							<label for="site_name"><?php esc_html_e( 'Site Name', 'viglet-dumont' ); ?></label>
							<input type="text" id="site_name"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[site_name]"
									value="<?php echo esc_attr( $settings['site_name'] ); ?>"
									class="regular-text" <?php echo esc_attr( $locked( 'site_name' ) ); ?>
									placeholder="default" />
							<p class="description"><?php esc_html_e( 'The Turing Semantic Navigation site name to use for indexing and search.', 'viglet-dumont' ); ?></p>
							<?php $env_note( 'site_name' ); ?>
						</div>

						<div class="viglet-dumont-field">
							<label for="site_key"><?php esc_html_e( 'Site Key', 'viglet-dumont' ); ?></label>
							<input type="text" id="site_key"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[site_key]"
									value="<?php echo esc_attr( $settings['site_key'] ); ?>"
									class="regular-text" <?php echo esc_attr( $locked( 'site_key' ) ); ?>
									placeholder="<?php echo esc_attr( ( new Viglet_Dumont_Indexer( array_merge( $settings, array( 'site_key' => '' ) ) ) )->site_key() ); ?>" />
							<p class="description"><?php esc_html_e( 'Starts every document id and fills the site_key field, so several WordPress sites can share one SN site. Empty uses this site\'s address. Changing it needs an Index All.', 'viglet-dumont' ); ?></p>
							<?php $env_note( 'site_key' ); ?>
						</div>

						<div class="viglet-dumont-field">
							<label for="api_key"><?php esc_html_e( 'API Key', 'viglet-dumont' ); ?></label>
							<?php
							// The key is never printed back: the field stays empty and keeps the stored key unless one is typed.
							?>
							<input type="password" id="api_key"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[api_key]"
									value=""
									class="regular-text" <?php echo esc_attr( $locked( 'api_key' ) ); ?>
									placeholder="<?php echo '' !== (string) $settings['api_key'] ? esc_attr__( 'A key is stored', 'viglet-dumont' ) : ''; ?>"
									autocomplete="new-password" />
							<p class="description"><?php esc_html_e( 'Optional. API key for authentication with the Turing server.', 'viglet-dumont' ); ?></p>
							<?php $env_note( 'api_key' ); ?>
						</div>

						<div class="viglet-dumont-field">
							<label for="public_url"><?php esc_html_e( 'Public Turing URL', 'viglet-dumont' ); ?></label>
							<input type="url" id="public_url"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[public_url]"
									value="<?php echo esc_attr( $settings['public_url'] ); ?>"
									class="regular-text" <?php echo esc_attr( $locked( 'public_url' ) ); ?> />
							<p class="description"><?php esc_html_e( 'Where visitors\' browsers reach Turing, when it differs from the Server URL this server uses. Empty uses the Server URL.', 'viglet-dumont' ); ?></p>
							<?php $env_note( 'public_url' ); ?>
						</div>

						<div class="viglet-dumont-field-row">
							<div class="viglet-dumont-field">
								<label for="locale"><?php esc_html_e( 'Locale', 'viglet-dumont' ); ?></label>
								<?php $env_note( 'locale' ); ?>
								<select id="locale" name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[locale]" <?php disabled( Viglet_Dumont_Settings::is_overridden( 'locale' ) ); ?>>
									<?php
									$locales = array(
										'en_US' => 'English (US)',
										'en_GB' => 'English (UK)',
										'pt_BR' => 'Portugu&ecirc;s (Brasil)',
										'pt_PT' => 'Portugu&ecirc;s (Portugal)',
										'es_ES' => 'Espa&ntilde;ol',
										'fr_FR' => 'Fran&ccedil;ais',
										'de_DE' => 'Deutsch',
										'it_IT' => 'Italiano',
										'ja_JP' => '&#26085;&#26412;&#35486;',
										'zh_CN' => '&#20013;&#25991;',
									);
									foreach ( $locales as $code => $label ) :
										?>
										<option value="<?php echo esc_attr( $code ); ?>" <?php selected( $settings['locale'], $code ); ?>>
											<?php echo esc_html( $label ); ?>
										</option>
									<?php endforeach; ?>
								</select>
							</div>

							<div class="viglet-dumont-field">
								<label for="source_app"><?php esc_html_e( 'Source App Identifier', 'viglet-dumont' ); ?></label>
								<input type="text" id="source_app"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[source_app]"
										value="<?php echo esc_attr( $settings['source_app'] ); ?>"
										class="regular-text"
										placeholder="wordpress" />
							</div>
						</div>
					</div>

					<div class="viglet-dumont-card__footer">
						<button type="button" id="viglet-dumont-ping" class="button button-secondary">
							<span class="dashicons dashicons-migrate"></span>
							<?php esc_html_e( 'Test Connection', 'viglet-dumont' ); ?>
						</button>
						<span id="viglet-dumont-ping-result" class="viglet-dumont-status"></span>
					</div>
				</div>
			</div>

			<!-- Indexing Tab -->
			<div class="viglet-dumont-panel" id="panel-indexing" style="display:none;">
				<div class="viglet-dumont-card">
					<div class="viglet-dumont-card__header">
						<h2><?php esc_html_e( 'Content Indexing', 'viglet-dumont' ); ?></h2>
						<p><?php esc_html_e( 'Choose which content types and metadata to index in Turing.', 'viglet-dumont' ); ?></p>
					</div>
					<div class="viglet-dumont-card__body">
						<h3><?php esc_html_e( 'Post Types', 'viglet-dumont' ); ?></h3>
						<div class="viglet-dumont-toggles">
							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[index_posts]"
										value="1" <?php checked( $settings['index_posts'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label"><?php esc_html_e( 'Posts', 'viglet-dumont' ); ?></span>
							</label>

							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[index_pages]"
										value="1" <?php checked( $settings['index_pages'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label"><?php esc_html_e( 'Pages', 'viglet-dumont' ); ?></span>
							</label>

							<?php foreach ( $post_types as $type ) : ?>
								<label class="viglet-dumont-toggle">
									<input type="checkbox"
											name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[index_custom_types][]"
											value="<?php echo esc_attr( $type->name ); ?>"
											<?php checked( in_array( $type->name, $settings['index_custom_types'] ?? array(), true ) ); ?> />
									<span class="viglet-dumont-toggle__slider"></span>
									<span class="viglet-dumont-toggle__label"><?php echo esc_html( $type->labels->name ); ?></span>
								</label>
							<?php endforeach; ?>
						</div>

						<hr class="viglet-dumont-separator" />

						<h3><?php esc_html_e( 'Metadata', 'viglet-dumont' ); ?></h3>
						<div class="viglet-dumont-toggles">
							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[index_categories]"
										value="1" <?php checked( $settings['index_categories'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label"><?php esc_html_e( 'Categories', 'viglet-dumont' ); ?></span>
							</label>

							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[index_tags]"
										value="1" <?php checked( $settings['index_tags'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label"><?php esc_html_e( 'Tags', 'viglet-dumont' ); ?></span>
							</label>

							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[index_comments]"
										value="1" <?php checked( $settings['index_comments'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label"><?php esc_html_e( 'Comments', 'viglet-dumont' ); ?></span>
							</label>
						</div>

						<hr class="viglet-dumont-separator" />

						<h3><?php esc_html_e( 'Advanced', 'viglet-dumont' ); ?></h3>

						<div class="viglet-dumont-field">
							<label for="index_custom_fields"><?php esc_html_e( 'Custom Fields', 'viglet-dumont' ); ?></label>
							<input type="text" id="index_custom_fields"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[index_custom_fields]"
									value="<?php echo esc_attr( $settings['index_custom_fields'] ); ?>"
									class="regular-text"
									placeholder="field1, field2, field3" />
							<p class="description"><?php esc_html_e( 'Comma-separated list of custom field names to include in the index.', 'viglet-dumont' ); ?></p>
						</div>

						<div class="viglet-dumont-field">
							<label for="exclude_ids"><?php esc_html_e( 'Exclude Posts/Pages', 'viglet-dumont' ); ?></label>
							<input type="text" id="exclude_ids"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[exclude_ids]"
									value="<?php echo esc_attr( $settings['exclude_ids'] ); ?>"
									class="regular-text"
									placeholder="1, 5, 23" />
							<p class="description"><?php esc_html_e( 'Comma-separated list of post/page IDs to exclude from indexing.', 'viglet-dumont' ); ?></p>
						</div>

						<div class="viglet-dumont-field-row">
							<div class="viglet-dumont-field">
								<label for="batch_size"><?php esc_html_e( 'Batch Size', 'viglet-dumont' ); ?></label>
								<input type="number" id="batch_size"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[batch_size]"
										value="<?php echo esc_attr( $settings['batch_size'] ); ?>"
										min="10" max="200" step="10" />
								<p class="description"><?php esc_html_e( 'Number of documents to send per batch (10-200).', 'viglet-dumont' ); ?></p>
							</div>
						</div>

						<hr class="viglet-dumont-separator" />

						<h3><?php esc_html_e( 'Automation', 'viglet-dumont' ); ?></h3>
						<div class="viglet-dumont-toggles">
							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[auto_index]"
										value="1" <?php checked( $settings['auto_index'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label">
									<?php esc_html_e( 'Auto-index on publish', 'viglet-dumont' ); ?>
									<small><?php esc_html_e( 'Automatically index content when published or updated.', 'viglet-dumont' ); ?></small>
								</span>
							</label>

							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[auto_deindex]"
										value="1" <?php checked( $settings['auto_deindex'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label">
									<?php esc_html_e( 'Auto-remove on delete/unpublish', 'viglet-dumont' ); ?>
									<small><?php esc_html_e( 'Automatically remove content from the index when trashed or unpublished.', 'viglet-dumont' ); ?></small>
								</span>
							</label>
						</div>
					</div>
				</div>
			</div>

			<!-- Search Tab -->
			<div class="viglet-dumont-panel" id="panel-search" style="display:none;">
				<div class="viglet-dumont-card">
					<div class="viglet-dumont-card__header">
						<h2><?php esc_html_e( 'Search Settings', 'viglet-dumont' ); ?></h2>
						<p><?php esc_html_e( 'Configure how Turing search replaces the default WordPress search.', 'viglet-dumont' ); ?></p>
					</div>
					<div class="viglet-dumont-card__body">
						<div class="viglet-dumont-toggles">
							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[search_enabled]"
										value="1" <?php checked( $settings['search_enabled'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label">
									<?php esc_html_e( 'Replace WordPress search with Turing', 'viglet-dumont' ); ?>
									<small><?php esc_html_e( 'When enabled, all search queries will be handled by the Turing search engine.', 'viglet-dumont' ); ?></small>
								</span>
							</label>
						</div>

						<div class="viglet-dumont-field">
							<label for="search_mode"><?php esc_html_e( 'Results page', 'viglet-dumont' ); ?></label>
							<select id="search_mode" name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[search_mode]">
								<option value="theme" <?php selected( $settings['search_mode'], 'theme' ); ?>><?php esc_html_e( 'The theme\'s own search page (block and classic themes)', 'viglet-dumont' ); ?></option>
								<option value="template" <?php selected( $settings['search_mode'], 'template' ); ?>><?php esc_html_e( 'The plugin\'s results page, with facets (classic themes)', 'viglet-dumont' ); ?></option>
							</select>
							<p class="description"><?php esc_html_e( 'With the theme\'s page, Turing ranks the results and the theme shows them with its own header, footer and styles. If Turing does not answer, WordPress search answers instead.', 'viglet-dumont' ); ?></p>
						</div>

						<hr class="viglet-dumont-separator" />

						<div class="viglet-dumont-field">
							<label for="results_per_page"><?php esc_html_e( 'Results Per Page', 'viglet-dumont' ); ?></label>
							<input type="number" id="results_per_page"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[results_per_page]"
									value="<?php echo esc_attr( $settings['results_per_page'] ); ?>"
									min="1" max="100" />
						</div>

						<hr class="viglet-dumont-separator" />

						<div class="viglet-dumont-toggles">
							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[instant_overlay]"
										value="1" <?php checked( $settings['instant_overlay'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label">
									<?php esc_html_e( 'Instant search from anywhere', 'viglet-dumont' ); ?>
									<small><?php esc_html_e( 'Ctrl+K (⌘K on a Mac) and the theme\'s own search box open a search that answers as the visitor types.', 'viglet-dumont' ); ?></small>
								</span>
							</label>
						</div>

						<div class="viglet-dumont-field">
							<label for="search_page"><?php esc_html_e( 'Full search page', 'viglet-dumont' ); ?></label>
							<?php
							wp_dropdown_pages(
								array(
									'id'                => 'search_page',
									'name'              => esc_attr( VIGLET_DUMONT_OPTION ) . '[search_page]',
									'selected'          => (int) $settings['search_page'],
									'show_option_none'  => esc_html__( 'Automatic: the page with the Turing search block', 'viglet-dumont' ),
									'option_none_value' => '0',
								)
							);
							?>
							<p class="description"><?php esc_html_e( 'Where Enter in the instant search goes, with the query. Without a page holding the Turing search block, the theme\'s search page answers.', 'viglet-dumont' ); ?></p>
						</div>
					</div>
				</div>

				<div class="viglet-dumont-card">
					<div class="viglet-dumont-card__header">
						<h2><?php esc_html_e( 'Search Analytics', 'viglet-dumont' ); ?></h2>
						<p><?php esc_html_e( 'What the search block and the instant search report about the searches visitors make. Events carry the query, the result and its rank, never who the visitor is. With a consent plugin that supports the WP Consent API, nothing is sent until the visitor allows statistics.', 'viglet-dumont' ); ?></p>
					</div>
					<div class="viglet-dumont-card__body">
						<div class="viglet-dumont-toggles">
							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[track_clicks]"
										value="1" <?php checked( $settings['track_clicks'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label">
									<?php esc_html_e( 'Report result clicks to Turing', 'viglet-dumont' ); ?>
									<small><?php esc_html_e( 'Turing counts which result was opened for each search, for its click-through rates. Turing records them only with search metrics turned on.', 'viglet-dumont' ); ?></small>
								</span>
							</label>
							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[analytics_ga]"
										value="1" <?php checked( $settings['analytics_ga'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label">
									<?php esc_html_e( 'Send search events to Google Analytics', 'viglet-dumont' ); ?>
									<small><?php esc_html_e( 'Searches, searches with no results, refined searches and result clicks, through the Google tag or Tag Manager the site already loads.', 'viglet-dumont' ); ?></small>
								</span>
							</label>
						</div>

						<div class="viglet-dumont-field">
							<label for="ga_measurement_id"><?php esc_html_e( 'Measurement ID', 'viglet-dumont' ); ?></label>
							<input type="text" id="ga_measurement_id"
									name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[ga_measurement_id]"
									value="<?php echo esc_attr( $settings['ga_measurement_id'] ); ?>"
									placeholder="G-XXXXXXXXXX" spellcheck="false" autocomplete="off" />
							<p class="description"><?php esc_html_e( 'Optional. Leave it empty to send to whatever the Google tag is set up for.', 'viglet-dumont' ); ?></p>
						</div>
					</div>
				</div>

				<div class="viglet-dumont-card">
					<div class="viglet-dumont-card__header">
						<h2><?php esc_html_e( 'Related Content', 'viglet-dumont' ); ?></h2>
						<p><?php esc_html_e( 'The posts Turing finds most like the one being read. Add the Related content block anywhere, or have it follow the content of these post types:', 'viglet-dumont' ); ?></p>
					</div>
					<div class="viglet-dumont-card__body">
						<div class="viglet-dumont-toggles">
							<?php foreach ( get_post_types( array( 'public' => true ), 'objects' ) as $viglet_dumont_type ) : ?>
								<?php
								if ( 'attachment' === $viglet_dumont_type->name ) {
									continue;
								}
								?>
								<label class="viglet-dumont-toggle">
									<input type="checkbox"
											name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[related_after][]"
											value="<?php echo esc_attr( $viglet_dumont_type->name ); ?>"
											<?php checked( in_array( $viglet_dumont_type->name, (array) $settings['related_after'], true ) ); ?> />
									<span class="viglet-dumont-toggle__slider"></span>
									<span class="viglet-dumont-toggle__label"><?php echo esc_html( $viglet_dumont_type->labels->name ); ?></span>
								</label>
							<?php endforeach; ?>
						</div>
					</div>
				</div>

				<div class="viglet-dumont-card">
					<div class="viglet-dumont-card__header">
						<h2><?php esc_html_e( 'Generative AI', 'viglet-dumont' ); ?></h2>
						<p><?php esc_html_e( 'Answers with sources and chat over this site\'s content, through Turing\'s Default AI Agent. Each answer is an LLM call billed to whoever runs Turing\'s AI provider.', 'viglet-dumont' ); ?></p>
					</div>
					<div class="viglet-dumont-card__body">
						<div class="viglet-dumont-toggles">
							<label class="viglet-dumont-toggle">
								<input type="checkbox"
										name="<?php echo esc_attr( VIGLET_DUMONT_OPTION ); ?>[generative_ai]"
										value="1" <?php checked( $settings['generative_ai'], 1 ); ?> />
								<span class="viglet-dumont-toggle__slider"></span>
								<span class="viglet-dumont-toggle__label">
									<?php esc_html_e( 'Turn on generative AI for this site in Turing', 'viglet-dumont' ); ?>
									<small><?php esc_html_e( 'Sent with the fields: on the next indexing run, or when the Fields screen is saved. It replaces an agent chosen for this site in Turing\'s console, and turning it off here later leaves Turing\'s setting as it is.', 'viglet-dumont' ); ?></small>
								</span>
							</label>
						</div>
					</div>
				</div>
			</div>

			<!-- Save Button (visible on settings tabs) -->
			<div class="viglet-dumont-save" id="viglet-dumont-save">
				<?php submit_button( __( 'Save Settings', 'viglet-dumont' ), 'primary large', 'submit', false ); ?>
			</div>
		</form>

		<!-- Actions Tab (outside form) -->
		<div class="viglet-dumont-panel" id="panel-actions" style="display:none;">
			<div class="viglet-dumont-card">
				<div class="viglet-dumont-card__header">
					<h2><?php esc_html_e( 'Indexing Actions', 'viglet-dumont' ); ?></h2>
					<p><?php esc_html_e( 'Manage your Turing search index.', 'viglet-dumont' ); ?></p>
				</div>
				<div class="viglet-dumont-card__body">

					<!-- Connection Status -->
					<div class="viglet-dumont-action-section">
						<div class="viglet-dumont-action-row">
							<div>
								<h3><?php esc_html_e( 'Connection Status', 'viglet-dumont' ); ?></h3>
								<p><?php esc_html_e( 'Test the connection to your Turing server.', 'viglet-dumont' ); ?></p>
							</div>
							<button type="button" id="viglet-dumont-action-ping" class="button button-secondary">
								<span class="dashicons dashicons-migrate"></span>
								<?php esc_html_e( 'Test Connection', 'viglet-dumont' ); ?>
							</button>
						</div>
						<div id="viglet-dumont-action-ping-result" class="viglet-dumont-action-result"></div>
					</div>

					<hr class="viglet-dumont-separator" />

					<!-- Index All Content -->
					<div class="viglet-dumont-action-section">
						<div class="viglet-dumont-action-row">
							<div>
								<h3><?php esc_html_e( 'Index All Content', 'viglet-dumont' ); ?></h3>
								<p><?php esc_html_e( 'Send all published content to the Turing index. This may take a while for large sites.', 'viglet-dumont' ); ?></p>
							</div>
							<div class="viglet-dumont-action-buttons">
								<label class="screen-reader-text" for="viglet-dumont-index-type"><?php esc_html_e( 'Content to index', 'viglet-dumont' ); ?></label>
								<select id="viglet-dumont-index-type">
									<option value="all"><?php esc_html_e( 'All Content', 'viglet-dumont' ); ?></option>
									<option value="post"><?php esc_html_e( 'Posts Only', 'viglet-dumont' ); ?></option>
									<option value="page"><?php esc_html_e( 'Pages Only', 'viglet-dumont' ); ?></option>
								</select>
								<button type="button" id="viglet-dumont-index-all" class="button button-primary">
									<span class="dashicons dashicons-database-add"></span>
									<?php esc_html_e( 'Start Indexing', 'viglet-dumont' ); ?>
								</button>
							</div>
						</div>
						<div id="viglet-dumont-index-progress" class="viglet-dumont-progress" style="display:none;">
							<div class="viglet-dumont-progress__bar">
								<div class="viglet-dumont-progress__fill" style="width: 0%"></div>
							</div>
							<span class="viglet-dumont-progress__text">0%</span>
							<span class="viglet-dumont-progress__message"></span>
						</div>
					</div>

					<hr class="viglet-dumont-separator" />

					<!-- Delete All -->
					<div class="viglet-dumont-action-section">
						<div class="viglet-dumont-action-row">
							<div>
								<h3><?php esc_html_e( 'Delete All Indexed Content', 'viglet-dumont' ); ?></h3>
								<p><?php esc_html_e( 'Remove all WordPress content from the Turing index. This does not affect your WordPress content.', 'viglet-dumont' ); ?></p>
							</div>
							<button type="button" id="viglet-dumont-delete-all" class="button button-link-delete">
								<span class="dashicons dashicons-trash"></span>
								<?php esc_html_e( 'Delete All', 'viglet-dumont' ); ?>
							</button>
						</div>
						<div id="viglet-dumont-delete-result" class="viglet-dumont-action-result"></div>
					</div>
				</div>
			</div>

			<!-- Content Stats -->
			<div class="viglet-dumont-card viglet-dumont-card--stats" id="viglet-dumont-stats">
				<div class="viglet-dumont-card__header">
					<h2><?php esc_html_e( 'Content Overview', 'viglet-dumont' ); ?></h2>
				</div>
				<div class="viglet-dumont-card__body">
					<div class="viglet-dumont-stats-grid" id="viglet-dumont-stats-grid">
						<p class="viglet-dumont-loading"><?php esc_html_e( 'Loading stats...', 'viglet-dumont' ); ?></p>
					</div>
				</div>
			</div>
		</div>

	</div>
</div>
