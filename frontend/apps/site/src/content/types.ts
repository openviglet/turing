/**
 * T1377: the shape of the site's copy, one dictionary per locale (content/<locale>/).
 *
 * English is the type: each locale's dictionary is declared `SiteContent`, so tsc
 * refuses a Portuguese dictionary that leaves a key out, where a lookup library would
 * show the English sentence on a /br/ page. No i18n library: the site is static and
 * prerendered, and a typed module needs no lookup at runtime.
 */
import type { DestinationKey } from "@/lib/destinations";

/** The words of the chrome every page shares: navigation, footer, the hero's way back, the cookie banner. */
export type ChromeStrings = Readonly<{
  nav: Readonly<{
    explore: string;
    docs: string;
    aem: string;
    product: string;
    capabilities: string;
    personas: string;
    why: string;
    developers: string;
    marketplace: string;
    solutions: string;
    wordpress: string;
    education: string;
    security: string;
    compare: string;
    migrate: string;
    getStarted: string;
    portfolio: string;
    home: string;
    openMenu: string;
    closeMenu: string;
  }>;
  footer: Readonly<{
    tagline: string;
    product: string;
    developers: string;
    open: string;
    howItWorks: string;
    aemSearch: string;
    selfHost: string;
    docs: string;
    cookies: string;
    license: string;
  }>;
  hero: Readonly<{ back: string }>;
  consent: Readonly<{
    region: string;
    title: string;
    body: string;
    decline: string;
    declineAndClose: string;
    accept: string;
  }>;
}>;

export type SiteContent = Readonly<{
  /** The chrome every page shares (T1371). */
  chrome: ChromeStrings;
  /** The label of each shared destination (lib/destinations.ts), by key. */
  destinations: Readonly<Record<DestinationKey, string>>;
}>;
