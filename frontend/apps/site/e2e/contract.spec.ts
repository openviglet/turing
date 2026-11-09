import { expect, test, type Page } from "@playwright/test";

/**
 * T1273: the rendered viglet.org hub contract, route by route, in both themes
 * and at the three widths the hub is reviewed at. Each case attaches its
 * screenshot to the report: these checks catch breakage, the screenshots are
 * what a person reviews for clipping, readability and stray decoration.
 */
const ROUTES = ["/", "/security/", "/features/search/", "/wordpress/", "/docs/", "/docs/rag/"];
const WIDTHS = [390, 768, 1440];
const THEMES = ["light", "dark"] as const;

/** Theme for the site and Starlight; consent decided so the banner covers nothing. */
async function prime(page: Page, theme: "light" | "dark", consent: string | null = "denied") {
  // Once per tab: init scripts also run on reload, which must keep what the
  // visitor chose since.
  await page.addInitScript(
    ([t, c]) => {
      if (sessionStorage.getItem("e2e-primed")) return;
      sessionStorage.setItem("e2e-primed", "1");
      localStorage.setItem("turing-site-theme", t as string);
      localStorage.setItem("starlight-theme", t as string);
      if (c) localStorage.setItem("turing-analytics-consent", c);
      else localStorage.removeItem("turing-analytics-consent");
    },
    [theme, consent]
  );
}

for (const route of ROUTES) {
  for (const width of WIDTHS) {
    for (const theme of THEMES) {
      test(`${route} at ${width}px, ${theme}`, async ({ page }, info) => {
        const errors: string[] = [];
        page.on("pageerror", (e) => errors.push(e.message));
        await page.setViewportSize({ width, height: 900 });
        await prime(page, theme);

        const response = await page.goto(route, { waitUntil: "networkidle" });
        expect(response?.status()).toBe(200);

        // The theme actually applied.
        const dark = await page.evaluate(
          () =>
            document.documentElement.classList.contains("dark") ||
            document.documentElement.dataset.theme === "dark"
        );
        expect(dark).toBe(theme === "dark");

        // The full product name is visible in the header at every width (T1268).
        // (The site renders a desktop and a mobile wordmark; one is display:none.)
        await expect(
          page.locator("header").getByText("Viglet Turing", { exact: true }).filter({ visible: true }).first()
        ).toBeVisible();
        await expect(page.locator("h1").first()).toBeVisible();

        // No page-wide horizontal scroll.
        const overflow = await page.evaluate(
          () => document.documentElement.scrollWidth - document.documentElement.clientWidth
        );
        expect(overflow).toBeLessThanOrEqual(1);

        expect(errors).toEqual([]);
        await info.attach(`${route} ${width} ${theme}`, {
          body: await page.screenshot(),
          contentType: "image/png",
        });
      });
    }
  }
}

test("the mobile menu opens, closes on Escape and returns focus to its button", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await prime(page, "light");
  await page.goto("/");
  const toggle = page.getByRole("button", { name: "Open menu" });
  await toggle.click();
  const panel = page.locator("header nav").filter({ hasText: "Get started" });
  await expect(panel).toBeVisible();
  // The way back to the portfolio stays discoverable on small screens (T1270).
  await expect(panel.getByRole("link", { name: /part of viglet\.org/ })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(panel).toBeHidden();
  const button = page.getByRole("button", { name: "Open menu" });
  await expect(button).toHaveAttribute("aria-expanded", "false");
  await expect(button).toBeFocused();
});

test("Get started is reachable by keyboard and opens the Quick start", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await prime(page, "light");
  await page.goto("/");
  for (let i = 0; i < 40; i++) {
    await page.keyboard.press("Tab");
    const label = await page.evaluate(() => document.activeElement?.textContent?.trim() ?? "");
    if (label.startsWith("Get started")) break;
  }
  await expect(page.locator(":focus")).toContainText("Get started");
  await Promise.all([page.waitForURL(/\/docs\/getting-started\/quick-start\/$/), page.keyboard.press("Enter")]);
  await expect(page.locator("h1").first()).toHaveText("Quick start");
});

test("reduced motion leaves no endless animation running on the home", async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await prime(page, "light");
  await page.goto("/", { waitUntil: "networkidle" });
  const endless = await page.evaluate(
    () =>
      document
        .getAnimations()
        .filter((a) => a.playState === "running" && a.effect?.getComputedTiming().iterations === Infinity).length
  );
  expect(endless).toBe(0);
});

test("consent: fresh asks, decline holds across reloads and loads nothing, settings reopen it", async ({ page }) => {
  const analytics: string[] = [];
  page.on("request", (r) => {
    if (/googletagmanager|google-analytics/.test(r.url())) analytics.push(r.url());
  });
  await prime(page, "light", null);
  await page.goto("/");
  const banner = page.getByRole("region", { name: "Cookie consent" }).or(page.getByLabel("Cookie consent"));
  await expect(banner.first()).toBeVisible();

  await page.getByRole("button", { name: "Decline", exact: true }).click();
  await expect(banner.first()).toBeHidden();
  await page.reload();
  await expect(banner.first()).toBeHidden();

  await page.getByRole("button", { name: "Cookie settings" }).click();
  await expect(banner.first()).toBeVisible();
  await page.getByRole("button", { name: "Decline", exact: true }).click();
  expect(analytics).toEqual([]);
});

test("the hero search demo still answers offline, from its sample corpus", async ({ page }) => {
  await prime(page, "light");
  await page.goto("/");
  const box = page.getByPlaceholder(/Search your content/).first();
  await box.fill("SSO");
  await expect(page.locator("#top").getByText(/SSO/).nth(1)).toBeVisible();
});
