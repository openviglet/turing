import { useEffect, useState } from "react";
import { ArrowRight, ArrowUpRight, ChevronDown, Menu, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ModeToggle } from "@/components/mode-toggle";
import { Container, Logo } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { cn } from "@/lib/utils";
import { localeHref } from "@/lib/site-url";
import { useLocale } from "@/lib/locale-context";
import type { Locale } from "@/lib/i18n";
import { useContent } from "@/content/use-content";
import type { ChromeStrings } from "@/content";

// Hrefs are root-anchored ("/#…") so they also work from the feature pages:
// the browser navigates home and scrolls to the section.
//
// The bar stays uncluttered: direct links flank two grouped dropdowns. AEM is
// pulled OUT of the Solutions dropdown as a top-level link because it is the
// headline solution, but it is an ordinary link like the rest (T1269: the hub's
// header has no highlighted pills).
function navLinks(t: ChromeStrings["nav"], locale: Locale) {
  return [
    { label: t.explore, href: LINKS.explore },
    { label: t.docs, href: LINKS.docs },
    { label: t.aem, href: localeHref("/aem-search/", locale) },
  ];
}

function navGroups(t: ChromeStrings["nav"], locale: Locale) {
  return [
    {
      label: t.product,
      items: [
        { label: t.capabilities, href: localeHref("/#features", locale) },
        { label: t.personas, href: localeHref("/persona/", locale) },
        { label: t.why, href: localeHref("/#why", locale) },
        { label: t.developers, href: localeHref("/#developers", locale) },
        { label: t.marketplace, href: localeHref("/#marketplace", locale) },
      ],
    },
    {
      label: t.solutions,
      items: [
        { label: t.wordpress, href: localeHref("/wordpress/", locale) },
        { label: t.education, href: localeHref("/educacao/", locale) },
        { label: t.security, href: localeHref("/security/", locale) },
        { label: t.compare, href: localeHref("/compare/", locale) },
        { label: t.migrate, href: localeHref("/migrate/", locale) },
      ],
    },
  ];
}

export function SiteNav() {
  const locale = useLocale();
  const t = useContent().chrome.nav;
  const NAV_LINKS = navLinks(t, locale);
  const NAV_GROUPS = navGroups(t, locale);
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const closeMenu = () => setMenuOpen(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Close the mobile menu on Escape (the panel itself closes on any nav click).
  useEffect(() => {
    if (!menuOpen) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setMenuOpen(false);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [menuOpen]);

  return (
    <header
      className={cn(
        "sticky top-0 z-50 backdrop-blur-md transition-colors",
        scrolled
          ? "border-b border-border bg-background/80"
          : "border-b border-transparent bg-background/60"
      )}
    >
      <Container className="flex h-16 items-center gap-4">
        <div className="flex shrink-0 items-center gap-2.5">
          <a href={localeHref("/", locale)} className="text-base" aria-label={t.home}>
            {/* T1268: the full product name shows at every width (never an
                icon-only or shortened mark). Below sm the layout makes the room
                instead: a smaller mark, and "Get started" moves into the menu. */}
            <Logo className="hidden sm:inline-flex" />
            <Logo size={28} className="text-sm sm:hidden" />
          </a>
          {/* Fluid path back to the umbrella brand: keeps the product from
              feeling like a dead end and reinforces the Viglet portfolio. */}
          <a
            href={LINKS.portfolio}
            className="hidden items-center gap-1 text-xs font-semibold whitespace-nowrap text-muted-foreground transition-colors hover:text-foreground sm:inline-flex"
          >
            {t.portfolio}
            <ArrowUpRight className="size-3" />
          </a>
        </div>
        <nav className="ml-4 hidden items-center gap-5 lg:flex xl:ml-6 xl:gap-6">
          {NAV_LINKS.map((item) => (
            <a
              key={item.label}
              href={item.href}
              className="text-sm font-semibold whitespace-nowrap text-muted-foreground transition-colors hover:text-foreground"
            >
              {item.label}
            </a>
          ))}
          {NAV_GROUPS.map((group) => (
            <DropdownMenu key={group.label}>
              <DropdownMenuTrigger className="group flex items-center gap-1 text-sm font-semibold whitespace-nowrap text-muted-foreground outline-hidden transition-colors hover:text-foreground data-[state=open]:text-foreground">
                {group.label}
                <ChevronDown className="size-3.5 transition-transform group-data-[state=open]:rotate-180" />
              </DropdownMenuTrigger>
              {/* Prevent Radix from returning focus to the trigger on close:
                  its trigger.focus() scrolls the (top-of-page) trigger back
                  into view, which cancels the native hash scroll to the target
                  section when clicking a "/#…" item from the landing page
                  itself. Cross-page items full-load so they're unaffected. */}
              <DropdownMenuContent
                align="start"
                className="min-w-44"
                onCloseAutoFocus={(e) => e.preventDefault()}
              >
                {group.items.map((item) => (
                  <DropdownMenuItem key={item.label} asChild>
                    <a href={item.href} className="w-full cursor-pointer font-medium">
                      {item.label}
                    </a>
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-2">
          <Button asChild size="sm" className="hidden sm:inline-flex">
            <a href={LINKS.getStarted}>
              {t.getStarted}
              <ArrowRight className="size-4" />
            </a>
          </Button>
          <ModeToggle />
          {/* Mobile/tablet menu trigger: the desktop <nav> is hidden below lg,
              so without this there'd be no way to reach Docs/AEM/Product/Solutions. */}
          <button
            type="button"
            onClick={() => setMenuOpen((v) => !v)}
            aria-label={menuOpen ? t.closeMenu : t.openMenu}
            aria-expanded={menuOpen}
            className="flex size-9 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground lg:hidden"
          >
            {menuOpen ? <X className="size-5" /> : <Menu className="size-5" />}
          </button>
        </div>
      </Container>

      {/* Mobile/tablet nav panel */}
      {menuOpen && (
        <nav className="border-t border-border bg-background/95 backdrop-blur-md lg:hidden">
          <Container className="flex flex-col py-3">
            <Button asChild size="sm" className="mb-2 w-full sm:hidden">
              <a href={LINKS.getStarted} onClick={closeMenu}>
                {t.getStarted}
                <ArrowRight className="size-4" />
              </a>
            </Button>
            {NAV_LINKS.map((item) => (
              <a
                key={item.label}
                href={item.href}
                onClick={closeMenu}
                className="rounded-lg px-2 py-2.5 text-sm font-semibold text-foreground transition-colors hover:bg-muted"
              >
                {item.label}
              </a>
            ))}
            {NAV_GROUPS.map((group) => (
              <div key={group.label} className="mt-2 border-t border-border/60 pt-2">
                <p className="px-2 py-1 text-[0.7rem] font-bold tracking-wide text-muted-foreground/70 uppercase">
                  {group.label}
                </p>
                {group.items.map((item) => (
                  <a
                    key={item.label}
                    href={item.href}
                    onClick={closeMenu}
                    className="block rounded-lg px-2 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                  >
                    {item.label}
                  </a>
                ))}
              </div>
            ))}
            <a
              href={LINKS.portfolio}
              onClick={closeMenu}
              className="mt-3 inline-flex w-fit items-center gap-1 px-2 text-xs font-semibold text-muted-foreground transition-colors hover:text-foreground"
            >
              {t.portfolio}
              <ArrowUpRight className="size-3" />
            </a>
          </Container>
        </nav>
      )}
    </header>
  );
}
