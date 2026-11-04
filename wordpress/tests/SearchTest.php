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
						'name'   => 'type',
						'label'  => array(
							'lang' => 'en_US',
							'text' => 'Type',
						),
						'facets' => array(
							array(
								'count'       => 3,
								'filterQuery' => 'type:post',
								'label'       => 'post',
								'link'        => '/api/sn/Sample/search?q=*&fq[]=type:post',
							),
						),
					),
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

		$this->assertSame( 'Type', $facet['label'] );
		$this->assertSame(
			array(
				array(
					'label' => 'post',
					'count' => 3,
					'link'  => 'type:post',
				),
			),
			$facet['facetValues']
		);
	}

	/** The site's scope is not a facet the visitor picks (D303). */
	public function test_the_site_key_facet_is_not_listed(): void {
		$names = array_column( $this->parse( $this->response() )['facets'], 'name' );

		$this->assertSame( array( 'type' ), $names );
	}

	/**
	 * Template mode is scoped like every read path (D303): on an SN site other WordPress sites
	 * share, an unscoped search listed their documents on this site's results page.
	 */
	public function test_the_results_page_searches_this_sites_scope_before_the_visitors_filters(): void {
		$params = ( new Viglet_Dumont_Search( $this->settings( array( 'site_key' => 'site-a' ) ) ) )->request_params( array( 'type:post' ), 'newest' );

		$this->assertSame( array( 'site_key:site-a', 'type:post' ), $params['fq[]'] );
		$this->assertSame( 'newest', $params['sort'] );
		$this->assertArrayNotHasKey( 'sort', ( new Viglet_Dumont_Search( $this->settings() ) )->request_params( array(), '' ) );
	}

	public function test_the_spelling_suggestion_is_the_corrected_text(): void {
		$this->assertSame( 'zebra', $this->parse( $this->response() )['did_you_mean'] );

		$response = $this->response();
		$response['widget']['spellCheck']['correctedText'] = false;
		$this->assertSame( '', $this->parse( $response )['did_you_mean'] );
	}
}
