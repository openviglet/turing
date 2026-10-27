// What the live-LLM suite spent (D174), from Turing's own ledger: every SN call records its
// site's tokens and dollars (T1127), so the run's cost is the summary after it less the
// summary before it. The teardown reports it beside the answer times and fails a run over
// budget:
//
//   AI_TOKEN_BUDGET  tokens one run may spend (default 60000)
//   AI_COST_BUDGET   dollars one run may spend (default 0.50)
//
// A call Turing could not price is booked as unpriced, not as free (T1144), so a run with one
// fails too: its dollar total is then no measure of what it cost.
import { wp } from '../global-setup.mjs';

const turingURL = process.env.TURING_URL || 'http://localhost:2700';

export const budget = {
	tokens: Number( process.env.AI_TOKEN_BUDGET || 60000 ),
	usd: Number( process.env.AI_COST_BUDGET || 0.5 ),
};

/**
 * A plugin setting, read through WP-CLI.
 *
 * @param {string} key The setting.
 */
function setting( key ) {
	return wp( [ 'option', 'pluck', 'viglet_dumont_settings', key ] )
		.trim()
		.split( '\n' )
		.pop()
		.trim();
}

/**
 * The SN site's LLM spending so far, as Turing's cost summary totals it.
 *
 * @return {Promise<{tokens: number, usd: number, requests: number, unpriced: number}>} The totals.
 */
export async function spent() {
	const site = setting( 'site_name' );
	const response = await fetch(
		`${ turingURL }/api/v2/llm/cost/summary?site=${ encodeURIComponent(
			site
		) }`,
		{ headers: { Key: setting( 'api_key' ) } }
	);
	if ( ! response.ok ) {
		throw new Error(
			`Turing's cost summary answered HTTP ${ response.status }`
		);
	}
	const summary = await response.json();
	return {
		tokens: Number( summary.totalTokens || 0 ),
		usd: Number( summary.totalCostUsd || 0 ),
		requests: Number( summary.totalRequests || 0 ),
		unpriced: Number( summary.unpricedRequests || 0 ),
	};
}

/**
 * What a run spent, from the totals before it and after it.
 *
 * @param {{tokens: number, usd: number, requests: number, unpriced: number}} before The totals before the run.
 * @param {{tokens: number, usd: number, requests: number, unpriced: number}} after  The totals after it.
 */
export function difference( before, after ) {
	return {
		tokens: after.tokens - before.tokens,
		usd: Math.round( ( after.usd - before.usd ) * 1e6 ) / 1e6,
		requests: after.requests - before.requests,
		unpriced: after.unpriced - before.unpriced,
	};
}

/**
 * Why a run's spending is over budget, or nothing when it is within it.
 *
 * @param {{tokens: number, usd: number, unpriced: number}} run What the run spent.
 * @return {string[]} One sentence per budget exceeded.
 */
export function overBudget( run ) {
	const over = [];
	if ( run.tokens > budget.tokens ) {
		over.push(
			`${ run.tokens } tokens, over the budget of ${ budget.tokens } (AI_TOKEN_BUDGET)`
		);
	}
	if ( run.unpriced > 0 ) {
		over.push(
			`${ run.unpriced } calls Turing could not price, so the dollar total is no measure`
		);
	}
	if ( run.usd > budget.usd ) {
		over.push(
			`$${ run.usd }, over the budget of $${ budget.usd } (AI_COST_BUDGET)`
		);
	}
	return over;
}
