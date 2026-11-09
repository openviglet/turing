import { expect, test } from "@playwright/test";

import { DESTINATIONS } from "../src/lib/destinations";

/**
 * T1273: the destinations that leave the site (T1270's map), followed for
 * real. Network trouble and bot walls are not product regressions, so they are
 * annotated `inconclusive` and the case passes; a 404/410 or a 5xx that
 * survives the retries is a real broken destination. Opt-in: EXTERNAL_LINKS=1.
 */
const external = Object.entries(DESTINATIONS).flatMap(([key, d]) => ("url" in d ? [[key, d.url] as const] : []));
const INCONCLUSIVE = new Set([401, 403, 429]);

test.skip(!process.env.EXTERNAL_LINKS, "external destinations run only with EXTERNAL_LINKS=1");

for (const [key, url] of external) {
  test(`${key} answers: ${url}`, async ({ request }, info) => {
    let status: number;
    try {
      status = (await request.get(url, { maxRedirects: 10, timeout: 20_000 })).status();
    } catch (e) {
      info.annotations.push({ type: "inconclusive", description: `network: ${(e as Error).message}` });
      return;
    }
    if (INCONCLUSIVE.has(status)) {
      info.annotations.push({ type: "inconclusive", description: `HTTP ${status} (bot wall or rate limit)` });
      return;
    }
    expect(status, url).toBeLessThan(400);
  });
}
