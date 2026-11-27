import { Container, Logo } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import type { DestinationKey } from "@/lib/destinations";
import { reopenConsent } from "@/lib/analytics";
import { localeHref } from "@/lib/site-url";
import { useLocale } from "@/lib/locale-context";
import type { Locale } from "@/lib/i18n";
import { useContent } from "@/content/use-content";
import type { SiteContent } from "@/content";

function columns(content: SiteContent, locale: Locale): { heading: string; links: { label: string; href: string }[] }[] {
  const t = content.chrome.footer;
  const nav = content.chrome.nav;
  return [
    {
      heading: t.product,
      links: [
        // Root-anchored so they also work from the /features/* pages (match SiteNav).
        { label: nav.capabilities, href: localeHref("/#features", locale) },
        { label: t.howItWorks, href: localeHref("/#how", locale) },
        { label: nav.compare, href: localeHref("/compare/", locale) },
        { label: nav.migrate, href: localeHref("/migrate/", locale) },
        { label: t.aemSearch, href: localeHref("/aem-search/", locale) },
        { label: "WordPress", href: localeHref("/wordpress/", locale) },
        { label: nav.education, href: localeHref("/educacao/", locale) },
        { label: nav.security, href: localeHref("/security/", locale) },
        { label: t.selfHost, href: localeHref("/#self-host", locale) },
        { label: nav.marketplace, href: localeHref("/#marketplace", locale) },
        { label: "React SDK", href: LINKS.storybook },
      ],
    },
    // T1270: one destination per job, labelled by that job (lib/destinations.ts).
    {
      heading: t.developers,
      links: destinations(content, ["getStarted", "docs", "download", "source", "npm", "cli"]),
    },
    {
      heading: t.open,
      links: destinations(content, ["releaseNotes", "devRoadmap", "portfolio"]),
    },
  ];
}

function destinations(content: SiteContent, keys: DestinationKey[]) {
  return keys.map((key) => ({
    label: content.destinations[key],
    href: LINKS[key],
  }));
}

export function SiteFooter() {
  const locale = useLocale();
  const content = useContent();
  const t = content.chrome.footer;
  const COLUMNS = columns(content, locale);
  return (
    <footer className="bg-[#07080f] text-[#8b94b3]">
      <Container className="py-14">
        <div className="grid gap-8 sm:grid-cols-2 md:grid-cols-[1.4fr_1fr_1fr_1fr]">
          <div>
            <Logo className="text-[#f2f4fb]" />
            <p className="mt-3 max-w-xs text-sm leading-relaxed">{t.tagline}</p>
          </div>
          {COLUMNS.map((col) => (
            <div key={col.heading}>
              <h3 className="text-xs font-bold tracking-[0.06em] text-[#f2f4fb] uppercase">
                {col.heading}
              </h3>
              <ul className="mt-4 space-y-2.5">
                {col.links.map((link) => (
                  <li key={link.label}>
                    <a
                      href={link.href}
                      target={link.href.startsWith("http") ? "_blank" : undefined}
                      rel={link.href.startsWith("http") ? "noreferrer" : undefined}
                      className="text-sm transition-colors hover:text-[#f2f4fb]"
                    >
                      {link.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="mt-12 flex flex-col justify-between gap-2 border-t border-[#161a2b] pt-6 text-xs text-[#5e6580] sm:flex-row">
          <span>
            © {new Date().getFullYear()} Viglet Turing · {t.license}
          </span>
          <span className="space-x-3">
            <a href={LINKS.portfolio} target="_blank" rel="noreferrer" className="hover:text-[#f2f4fb]">
              viglet.org
            </a>
            <a href={LINKS.docs} className="hover:text-[#f2f4fb]">
              {t.docs}
            </a>
            <button type="button" onClick={reopenConsent} className="hover:text-[#f2f4fb]">
              {t.cookies}
            </button>
          </span>
        </div>
      </Container>
    </footer>
  );
}
