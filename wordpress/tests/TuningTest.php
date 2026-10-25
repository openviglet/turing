<?php
/**
 * Search tuning writes only what is its own to Turing, and applies synonyms once written.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Tuning;
use WP_Error;

final class TuningTest extends PluginTestCase {

	protected function setUp(): void {
		parent::setUp();

		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				$path             = (string) parse_url( $url, PHP_URL_PATH );
				$bodies           = array(
					'/api/sn'                       => array(
						array(
							'id'   => 'other',
							'name' => 'Another',
						),
						array(
							'id'   => 's1',
							'name' => 'sample site',
						),
					),
					'/api/sn/s1/synonym/operator'   => array(
						'id'   => 'operator',
						'name' => 'set in the console',
					),
					'/api/sn/s1/synonym/ours'       => array(
						'id'   => 'ours',
						'name' => 'WordPress: lecture, talk',
					),
					'/api/sn/s1/ranking-expression' => array(
						array(
							'id'   => 'old',
							'name' => 'WordPress: newer first',
						),
						array(
							'id'   => 'operator',
							'name' => 'set in the console',
						),
					),
					'/api/sn/s1/synonym/mine'       => array(
						array( 'terms' => array( 'workshop', 'workshop*' ) ),
						array( 'terms' => array( 'work', 'workshop' ) ),
						array( 'terms' => array( 'lecture', 'talk' ) ),
						array( 'terms' => array( 'talk' ) ),
					),
				);
				return array(
					'response' => array( 'code' => 200 ),
					'body'     => json_encode( $bodies[ $path ] ?? array( 'ok' => true ) ),
				);
			}
		);
	}

	private function calls(): array {
		return array_map( static fn ( $r ) => $r[1]['method'] . ' ' . parse_url( $r[0], PHP_URL_PATH ), $this->requests );
	}

	public function test_a_synonym_is_named_as_the_plugins_and_applied_to_the_engine(): void {
		$result = ( new Viglet_Dumont_Tuning( $this->settings() ) )->add_synonym( array( ' lecture ', 'talk', 'talk', '' ) );

		$this->assertTrue( $result );
		$this->assertSame(
			array( 'GET /api/sn', 'POST /api/sn/s1/synonym', 'POST /api/sn/s1/synonym/apply' ),
			$this->calls(),
			'the site is found by name, case aside'
		);
		$this->assertSame(
			array(
				'name'     => 'WordPress: lecture, talk',
				'type'     => 'REGULAR',
				'language' => 'pt_BR',
				'terms'    => array( 'lecture', 'talk' ),
				'enabled'  => true,
			),
			$this->body( 1 )
		);
	}

	public function test_one_word_is_not_a_synonym_and_nothing_is_sent(): void {
		$result = ( new Viglet_Dumont_Tuning( $this->settings() ) )->add_synonym( array( 'talk' ) );

		$this->assertInstanceOf( WP_Error::class, $result );
		$this->assertSame( array(), $this->requests );
	}

	public function test_what_an_operator_set_in_the_console_is_never_removed_here(): void {
		$result = ( new Viglet_Dumont_Tuning( $this->settings() ) )->remove_synonym( 'operator' );

		$this->assertInstanceOf( WP_Error::class, $result );
		$this->assertSame( 'viglet_dumont_not_ours', $result->get_error_code() );
		$this->assertNotContains( 'DELETE /api/sn/s1/synonym/operator', $this->calls() );
	}

	public function test_boosts_replace_the_plugins_ranking_expressions_and_leave_an_operators(): void {
		$result = ( new Viglet_Dumont_Tuning( $this->settings() ) )->set_boosts( true, false, array( 'page' => 'down' ) );

		$this->assertTrue( $result );
		$this->assertContains( 'DELETE /api/sn/s1/ranking-expression/old', $this->calls() );
		$this->assertNotContains( 'DELETE /api/sn/s1/ranking-expression/operator', $this->calls() );
		$posted = array_values(
			array_filter(
				array_keys( $this->requests ),
				fn ( $i ) => 'POST' === $this->requests[ $i ][1]['method']
			)
		);
		$this->assertCount( 2, $posted );
		$this->assertSame(
			array(
				'attribute' => 'publication_date',
				'condition' => 1,
				'value'     => 'asc',
			),
			$this->body( $posted[0] )['turSNRankingConditions'][0],
			'newer first is Turing\'s recency form'
		);
		$this->assertSame( 2, $this->body( $posted[1] )['turSNRankingConditions'][0]['condition'], 'down is "is not": the others go up' );
	}

	public function test_suggestions_leave_out_wildcards_prefixes_and_single_words(): void {
		$this->assertSame(
			array( array( 'terms' => array( 'lecture', 'talk' ) ) ),
			( new Viglet_Dumont_Tuning( $this->settings() ) )->suggestions()
		);
	}

	public function test_the_plugins_own_synonym_is_removed_and_the_rest_reapplied(): void {
		$result = ( new Viglet_Dumont_Tuning( $this->settings() ) )->remove_synonym( 'ours' );

		$this->assertTrue( $result );
		$this->assertSame(
			array( 'GET /api/sn', 'GET /api/sn/s1/synonym/ours', 'DELETE /api/sn/s1/synonym/ours', 'POST /api/sn/s1/synonym/apply' ),
			$this->calls()
		);
	}
}
