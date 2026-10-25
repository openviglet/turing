<?php
/**
 * The search page reads Turing's response as Turing shapes it.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Viglet_Dumont_Search;

final class SearchTest extends PluginTestCase {

	/** A search response trimmed from a real Turing (turing-ce, 2026-10-24). */
	private function response(): array {
		return array(
			'queryContext' => array( 'count' => 23 ),
			'results'      => array(
				'document' => array( array( 'fields' => array( 'id' => 'a:post:1' ) ) ),
			),
			'widget'       => array(
				'facet'      => array(
					array(
						'name'   => 'site_key',
						'label'  => array(
							'lang' => 'en_US',
							'text' => 'Site',
						),
						'facets' => array(
							array(
								'count'       => 3,
								'filterQuery' => 'site_key:localhost',
								'label'       => 'localhost',
								'link'        => '/api/sn/Sample/search?q=*&fq[]=site_key:localhost',
							),
						),
					),
				),
				'spellCheck' => array(
					'correctedText' => true,
					'corrected'     => array( 'text' => 'zebra' ),
				),
			),
		);
	}

	private function parse( array $response ): array {
		return ( new Viglet_Dumont_Search( $this->settings() ) )->parse_results(
			$response,
			array(
				'per_page'     => 10,
				'results'      => array(),
				'total'        => 0,
				'facets'       => array(),
				'did_you_mean' => '',
			)
		);
	}

	public function test_the_total_and_pages_come_from_the_top_level_query_context(): void {
		$data = $this->parse( $this->response() );

		$this->assertSame( 23, $data['total'] );
		$this->assertSame( 3, $data['total_pages'] );
		$this->assertCount( 1, $data['results'] );
	}

	public function test_a_facet_value_links_to_its_filter_query(): void {
		$facet = $this->parse( $this->response() )['facets'][0];

		$this->assertSame( 'Site', $facet['label'] );
		$this->assertSame(
			array(
				array(
					'label' => 'localhost',
					'count' => 3,
					'link'  => 'site_key:localhost',
				),
			),
			$facet['facetValues']
		);
	}

	public function test_the_spelling_suggestion_is_the_corrected_text(): void {
		$this->assertSame( 'zebra', $this->parse( $this->response() )['did_you_mean'] );

		$response = $this->response();
		$response['widget']['spellCheck']['correctedText'] = false;
		$this->assertSame( '', $this->parse( $response )['did_you_mean'] );
	}
}
