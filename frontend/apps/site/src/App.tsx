import { useEffect } from "react";
import { LandingPage } from "@/pages/landing";
import { FeaturePage } from "@/pages/feature-page";
import { MigratePage } from "@/pages/migrate";
import { ComparePage } from "@/pages/compare";
import { AemPage } from "@/pages/aem";
import { PersonaPage } from "@/pages/persona";
import { SecurityPage } from "@/pages/security";
import { WordPressPage } from "@/pages/wordpress";
import { EducacaoPage } from "@/pages/educacao";
import { ConsentBanner } from "@/components/consent-banner";
import { usePages } from "@/content/use-pages";
import { LocaleProvider } from "@/lib/locale";
import { resolveRoute } from "./route-match";

/** Standalone (non-feature) routes with their own page component. */
export const MIGRATE_PATH = "/migrate/";
export const COMPARE_PATH = "/compare/";
export const AEM_PATH = "/aem-search/";
export const PERSONA_PATH = "/persona/";
export const SECURITY_PATH = "/security/";
export const WORDPRESS_PATH = "/wordpress/";
export const EDUCACAO_PATH = "/educacao/";

export default function App({ path = "/" }: Readonly<{ path?: string }>) {
  // Cross-page links to a home anchor (e.g. "/#playground") full-load "/" and
  // then rely on the browser's native hash scroll, but the target section
  // mounts AFTER React does (and doesn't exist at all before hydration in dev),
  // so the native jump finds nothing and stays at the top. Re-run the scroll
  // once the element is present (retrying across a few frames), plus on
  // hashchange for same-page anchor clicks.
  useEffect(() => {
    const scrollToHash = (smooth: boolean) => {
      const { hash } = window.location;
      if (hash.length < 2) return;
      const id = decodeURIComponent(hash.slice(1));
      let tries = 0;
      const tick = () => {
        const el = document.getElementById(id);
        if (el) {
          el.scrollIntoView({ behavior: smooth ? "smooth" : "auto", block: "start" });
        } else if (tries++ < 30) {
          requestAnimationFrame(tick);
        }
      };
      tick();
    };
    scrollToHash(false);
    const onHashChange = () => scrollToHash(true);
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  // T1376: the language is read from the path (/br/… is pt-BR) and given to every
  // component once; the page is chosen by the route below the prefix.
  const { locale, route, slug } = resolveRoute(path);
  return (
    <LocaleProvider locale={locale}>
      <Page path={route} slug={slug} />
      {/* T1097 — GA loads only after the visitor opts in. */}
      <ConsentBanner />
    </LocaleProvider>
  );
}

function Page({ path, slug }: Readonly<{ path: string; slug: string | null }>) {
  const { FEATURE_DETAILS } = usePages();
  if (/^\/migrate\/?$/.test(path)) return <MigratePage />;
  if (/^\/compare\/?$/.test(path)) return <ComparePage />;
  if (/^\/aem-search\/?$/.test(path)) return <AemPage />;
  if (/^\/persona\/?$/.test(path)) return <PersonaPage />;
  if (/^\/security\/?$/.test(path)) return <SecurityPage />;
  if (/^\/wordpress\/?$/.test(path)) return <WordPressPage />;
  if (/^\/educacao\/?$/.test(path)) return <EducacaoPage />;
  if (slug) return <FeaturePage detail={FEATURE_DETAILS[slug]} />;
  return <LandingPage />;
}
