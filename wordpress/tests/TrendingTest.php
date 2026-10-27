<?php
/**
 * Popular searches under the search box (D202): which of the week's searches become chips.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Trending;

final class TrendingTest extends PluginTestCase {

	public function test_typed_prefixes_long_searches_and_personal_data_are_never_shown(): void {
		$this->assertTrue( Viglet_Dumont_Trending::presentable( 'rain jacket' ) );
		$this->assertTrue( Viglet_Dumont_Trending::presentable( 'size 42 boots' ), 'a short number is a size, not a document' );
		$this->assertFalse( Viglet_Dumont_Trending::presentable( 'work*' ) );
		$this->assertFalse( Viglet_Dumont_Trending::presentable( 'ana@example.org' ) );
		$this->assertFalse( Viglet_Dumont_Trending::presentable( '123.456.789-09' ) );
		$this->assertFalse( Viglet_Dumont_Trending::presentable( '+55 11 98765 4321' ) );
		$this->assertFalse( Viglet_Dumont_Trending::presentable( str_repeat( 'long ', 10 ) ) );
		$this->assertFalse( Viglet_Dumont_Trending::presentable( '' ) );
	}

	public function test_the_chips_are_the_weeks_top_searches_that_find_something_now(): void {
		$cached = null;
		Functions\when( 'get_transient' )->justReturn( false );
		Functions\when( 'set_transient' )->alias(
			static function ( $key, $value ) use ( &$cached ) {
				$cached = $value;
				return true;
			}
		);
		$top = array(
			array(
				'term'     => 'workshop',
				'searches' => 40,
				'clicks'   => 4,
			),
			array(
				'term'     => 'work*',
				'searches' => 30,
				'clicks'   => 0,
			),
			array(
				'term'     => 'ana@example.org',
				'searches' => 9,
				'clicks'   => 0,
			),
			array(
				'term'     => 'xqzvbnmw',
				'searches' => 8,
				'clicks'   => 0,
			),
			array(
				'term'     => 'Workshop',
				'searches' => 6,
				'clicks'   => 0,
			),
			array(
				'term'     => 'seminar',
				'searches' => 5,
				'clicks'   => 1,
			),
			array(
				'term'     => 'rare',
				'searches' => 2,
				'clicks'   => 0,
			),
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) use ( $top ) {
				$this->requests[] = array( $url, $args );
				if ( str_contains( $url, '/metrics/clicks' ) ) {
					$body = array( 'terms' => $top );
				} elseif ( str_contains( $url, '/metrics/' ) ) {
					$body = array( 'terms' => array() );
				} elseif ( str_contains( $url, '/search?' ) ) {
					parse_str( (string) parse_url( $url, PHP_URL_QUERY ), $query );
					// The nonsense word finds nothing.
					$body = array( 'queryContext' => array( 'count' => 'xqzvbnmw' === $query['q'] ? 0 : 3 ) );
				} else {
					$body = array(
						array(
							'id'   => 'site-uuid',
							'name' => 'Sample Site',
						),
					);
				}
				return array(
					'response' => array( 'code' => str_ends_with( $url, '/click' ) ? 400 : 200 ),
					'body'     => json_encode( $body ),
				);
			}
		);

		$chips = ( new Viglet_Dumont_Trending( $this->settings() ) )->chips();

		$this->assertSame( array( 'workshop', 'seminar' ), $chips, 'a prefix, an e-mail, an empty answer, a case variant and a rare search left out' );
		$this->assertSame( $chips, $cached );
	}
}
