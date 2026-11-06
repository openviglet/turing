<?php
/**
 * Viglet Turing Upgrade: the one-time move from the plugin's old prefix (T1253).
 *
 * The plugin was born in Dumont as viglet_dumont and was renamed to viglet_turing before its
 * first WordPress.org release. A site that ran the old name keeps its settings, its post
 * flags, its scheduled jobs and the blocks in its pages: this copies each to the new name
 * once, and is the one place in the plugin that still spells the old one.
 *
 * @package Viglet_Turing
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Moves the old prefix's stored data to the new one, once per site.
 */
final class Viglet_Turing_Upgrade {

	/** Set once the move ran on this site; holds the plugin version that ran it. */
	public const DONE_OPTION = 'viglet_turing_prefix_migrated';

	/** The prefix the plugin stored its data under before T1253. */
	private const OLD = 'viglet_dumont';

	private const NEW = 'viglet_turing';

	/** The block namespace, and the class WordPress derives from it, before and after. */
	private const OLD_BLOCK = 'viglet-dumont';

	private const NEW_BLOCK = 'viglet-turing';

	/**
	 * Runs the move unless this site already had it.
	 */
	public static function maybe_run(): void {
		if ( false !== get_option( self::DONE_OPTION, false ) ) {
			return;
		}
		self::run();
		update_option( self::DONE_OPTION, defined( 'VIGLET_TURING_VERSION' ) ? VIGLET_TURING_VERSION : '1', true );
	}

	/**
	 * Moves everything, and says how much of each it moved.
	 *
	 * @return array{options: int, meta: int, cron: int, content: int}
	 */
	public static function run(): array {
		$moved = array(
			'options' => self::options(),
			'meta'    => self::post_meta(),
			'cron'    => self::cron(),
			'content' => self::content(),
		);
		if ( array_sum( $moved ) > 0 && function_exists( 'wp_cache_flush' ) ) {
			wp_cache_flush();
		}

		return $moved;
	}

	/**
	 * A stored key's name under the new prefix, or null when the key is not one of ours.
	 *
	 * @param string $name A key under the old prefix, with or without a leading underscore.
	 */
	public static function renamed( string $name ): ?string {
		$lead = str_starts_with( $name, '_' ) ? '_' : '';
		$rest = substr( $name, strlen( $lead ) );
		if ( self::OLD !== $rest && ! str_starts_with( $rest, self::OLD . '_' ) ) {
			return null;
		}

		return $lead . self::NEW . substr( $rest, strlen( self::OLD ) );
	}

	/**
	 * Copies each option to its new key, unless the new key already holds a value, and drops
	 * the old one. Transients are caches and locks: they are dropped, not moved.
	 */
	private static function options(): int {
		global $wpdb;

		$like = $wpdb->esc_like( self::OLD . '_' ) . '%';
		// phpcs:ignore WordPress.DB.DirectDatabaseQuery -- a one-time move of this plugin's own rows.
		$rows = (array) $wpdb->get_results(
			$wpdb->prepare(
				"SELECT option_name, option_value, autoload FROM {$wpdb->options} WHERE option_name = %s OR option_name LIKE %s",
				self::OLD,
				$like
			),
			ARRAY_A
		);

		$moved = 0;
		foreach ( $rows as $row ) {
			$old = (string) $row['option_name'];
			$new = self::renamed( $old );
			if ( null === $new ) {
				continue;
			}
			if ( false === get_option( $new, false ) ) {
				add_option( $new, maybe_unserialize( $row['option_value'] ), '', in_array( $row['autoload'], array( 'yes', 'on', 'auto-on', 'auto' ), true ) );
				++$moved;
			}
			delete_option( $old );
		}

		foreach ( array( '_transient_', '_transient_timeout_', '_site_transient_', '_site_transient_timeout_' ) as $kind ) {
			// phpcs:ignore WordPress.DB.DirectDatabaseQuery -- dropping this plugin's own caches, once.
			$wpdb->query(
				$wpdb->prepare(
					"DELETE FROM {$wpdb->options} WHERE option_name LIKE %s",
					$wpdb->esc_like( $kind . self::OLD . '_' ) . '%'
				)
			);
		}

		return $moved;
	}

	/**
	 * Renames the post flags (`_viglet_dumont_exclude`, the index state, ...) in place.
	 */
	private static function post_meta(): int {
		global $wpdb;

		$old = '_' . self::OLD . '_';
		// phpcs:ignore WordPress.DB.DirectDatabaseQuery -- a one-time rename of this plugin's own keys.
		return (int) $wpdb->query(
			$wpdb->prepare(
				"UPDATE {$wpdb->postmeta} SET meta_key = CONCAT(%s, SUBSTRING(meta_key, %d)) WHERE meta_key LIKE %s",
				'_' . self::NEW . '_',
				strlen( $old ) + 1,
				$wpdb->esc_like( $old ) . '%'
			)
		);
	}

	/**
	 * Re-schedules each pending job under its new hook, at the same time and with the same
	 * arguments, so a post's "leaves search" date and a running Index All carry on.
	 */
	private static function cron(): int {
		$crons = function_exists( '_get_cron_array' ) ? _get_cron_array() : array();
		$moved = 0;
		foreach ( (array) $crons as $timestamp => $hooks ) {
			foreach ( (array) $hooks as $hook => $events ) {
				$new = self::renamed( (string) $hook );
				if ( null === $new ) {
					continue;
				}
				foreach ( (array) $events as $event ) {
					$args = $event['args'];
					wp_unschedule_event( (int) $timestamp, (string) $hook, $args );
					if ( empty( $event['schedule'] ) ) {
						wp_schedule_single_event( (int) $timestamp, $new, $args );
					} else {
						wp_schedule_event( (int) $timestamp, (string) $event['schedule'], $new, $args );
					}
					++$moved;
				}
			}
		}

		return $moved;
	}

	/**
	 * Renames the plugin's blocks in saved posts, templates and block widgets, so a page that
	 * holds a Turing search block keeps rendering it.
	 */
	private static function content(): int {
		global $wpdb;

		$moved = 0;
		// phpcs:ignore WordPress.DB.DirectDatabaseQuery -- a one-time rename of this plugin's own blocks.
		$moved += (int) $wpdb->query(
			$wpdb->prepare(
				"UPDATE {$wpdb->posts} SET post_content = REPLACE(REPLACE(REPLACE(post_content, %s, %s), %s, %s), %s, %s) WHERE post_content LIKE %s",
				'<!-- wp:' . self::OLD_BLOCK . '/',
				'<!-- wp:' . self::NEW_BLOCK . '/',
				'<!-- /wp:' . self::OLD_BLOCK . '/',
				'<!-- /wp:' . self::NEW_BLOCK . '/',
				'wp-block-' . self::OLD_BLOCK . '-',
				'wp-block-' . self::NEW_BLOCK . '-',
				'%' . $wpdb->esc_like( 'wp:' . self::OLD_BLOCK . '/' ) . '%'
			)
		);

		$widgets = get_option( 'widget_block', false );
		if ( is_array( $widgets ) ) {
			$changed = false;
			foreach ( $widgets as $key => $widget ) {
				if ( is_array( $widget ) && isset( $widget['content'] ) && is_string( $widget['content'] ) ) {
					$content = self::blocks( $widget['content'] );
					if ( $content !== $widget['content'] ) {
						$widgets[ $key ]['content'] = $content;
						$changed                    = true;
						++$moved;
					}
				}
			}
			if ( $changed ) {
				update_option( 'widget_block', $widgets );
			}
		}

		return $moved;
	}

	/**
	 * The markup with the plugin's blocks under their new names.
	 *
	 * @param string $markup Block markup.
	 */
	public static function blocks( string $markup ): string {
		return str_replace(
			array( '<!-- wp:' . self::OLD_BLOCK . '/', '<!-- /wp:' . self::OLD_BLOCK . '/', 'wp-block-' . self::OLD_BLOCK . '-' ),
			array( '<!-- wp:' . self::NEW_BLOCK . '/', '<!-- /wp:' . self::NEW_BLOCK . '/', 'wp-block-' . self::NEW_BLOCK . '-' ),
			$markup
		);
	}
}
