import { useState, useSyncExternalStore } from "react";
import { ArrowLeft, ArrowRight, Play } from "lucide-react";
import { SiteNav } from "@/components/sections/site-nav";
import { SiteFooter } from "@/components/sections/site-footer";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Container } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import {
  WORDPRESS_FAQ,
  WORDPRESS_STORY,
  WORDPRESS_VIDEOS,
  type WordPressVideoLang,
} from "@/lib/site-content";
import { cn } from "@/lib/utils";
import { siteUrl, withBase } from "@/lib/site-url";
import { PUBLISHED_VIDEO_LANGS as PUBLISHED, videoLangFor } from "@/lib/wordpress-video";
import { WORDPRESS_TRANSCRIPT } from "@/lib/wordpress-transcript";

const noSubscription = () => () => {};
const browserVideoLang = () => videoLangFor(navigator.language);
/** The prerendered page shows the first published cut. */
const serverVideoLang = () => PUBLISHED[0];

/**
 * Structured data (@graph): BreadcrumbList (Home › WordPress), SoftwareApplication,
 * VideoObject for each published cut, and the FAQPage. Prerendered to static HTML.
 */
function WordPressJsonLd() {
  const graph = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "BreadcrumbList",
        itemListElement: [
          { "@type": "ListItem", position: 1, name: "Viglet Turing", item: siteUrl("/") },
          { "@type": "ListItem", position: 2, name: "WordPress", item: siteUrl("/wordpress/") },
        ],
      },
      {
        "@type": "SoftwareApplication",
        name: "Viglet Turing for WordPress and WooCommerce",
        applicationCategory: "BusinessApplication",
        operatingSystem: "WordPress",
        url: siteUrl("/wordpress/"),
        description:
          "A WordPress plugin that indexes posts, pages and WooCommerce products into Viglet Turing, then adds search as you type, filters, cited answers, a shopping copilot and search insights to the site. Open source (Apache 2.0).",
        offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
        license: "https://www.apache.org/licenses/LICENSE-2.0",
      },
      ...PUBLISHED.map((lang) => ({
        "@type": "VideoObject",
        name: WORDPRESS_VIDEOS[lang].title,
        inLanguage: lang,
        description:
          "One shopper's visit to the Viglet Turing demo store on WordPress and WooCommerce, from a vague need to a paid-for cart.",
        thumbnailUrl: siteUrl(`/wordpress/video-${lang}.jpg`),
        embedUrl: `https://www.youtube-nocookie.com/embed/${WORDPRESS_VIDEOS[lang].id}`,
        contentUrl: `https://www.youtube.com/watch?v=${WORDPRESS_VIDEOS[lang].id}`,
      })),
      {
        "@type": "FAQPage",
        mainEntity: WORDPRESS_FAQ.map((item) => ({
          "@type": "Question",
          name: item.q,
          acceptedAnswer: { "@type": "Answer", text: item.a },
        })),
      },
    ],
  };
  return (
    <script
      type="application/ld+json"
      // Prerendered to static HTML; this is our own content, not user input.
      dangerouslySetInnerHTML={{ __html: JSON.stringify(graph) }}
    />
  );
}

/**
 * The plugin video as a click-to-load facade: the poster is a local image and no
 * request reaches YouTube until the visitor presses play, so no third-party cookie
 * loads before consent. The embed is youtube-nocookie.
 */
function VideoFacade() {
  // The browser's language picks the cut after hydration, until the visitor chooses one.
  const browserLang = useSyncExternalStore(noSubscription, browserVideoLang, serverVideoLang);
  const [chosen, setLang] = useState<WordPressVideoLang | null>(null);
  const [playing, setPlaying] = useState(false);
  const lang = chosen ?? browserLang;
  const video = WORDPRESS_VIDEOS[lang];

  return (
    <div>
      <div className="relative aspect-video overflow-hidden rounded-2xl border border-white/10 bg-black shadow-2xl">
        {playing && video.id ? (
          <iframe
            className="absolute inset-0 size-full"
            src={`https://www.youtube-nocookie.com/embed/${video.id}?autoplay=1&rel=0`}
            title={video.title}
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          />
        ) : (
          <button
            type="button"
            onClick={() => setPlaying(true)}
            className="group absolute inset-0 size-full"
            aria-label={`Play the video: ${video.title}`}
          >
            <img
              src={withBase(`/wordpress/video-${lang}.jpg`)}
              alt=""
              width={1280}
              height={720}
              className="size-full object-cover transition-opacity group-hover:opacity-90"
            />
            <span className="absolute top-1/2 left-1/2 flex size-16 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg transition-transform group-hover:scale-110">
              <Play className="ml-1 size-7" />
            </span>
          </button>
        )}
      </div>
      {PUBLISHED.length > 1 && (
        <fieldset className="mt-3 flex gap-2">
          <legend className="sr-only">Video language</legend>
          {PUBLISHED.map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={option === lang}
              onClick={() => {
                setLang(option);
                setPlaying(false);
              }}
              className={cn(
                "rounded-full px-3 py-1 text-xs font-semibold transition-colors",
                option === lang ? "bg-white text-[#080b18]" : "bg-white/10 text-white hover:bg-white/20",
              )}
            >
              {WORDPRESS_VIDEOS[option].label}
            </button>
          ))}
        </fieldset>
      )}
      <p className="mt-3 text-xs text-[#7f8bbd]">
        Plays from YouTube (youtube-nocookie.com) after you press play.
      </p>
      {/* T1257: what Ana says in this cut, as text for readers, search and assistants. */}
      <details className="mt-3 text-sm text-[#aeb7d4]">
        <summary className="cursor-pointer font-semibold text-white">
          {lang === "pt-BR" ? "Transcrição" : "Transcript"}
        </summary>
        <div className="mt-3 max-h-72 space-y-3 overflow-y-auto pr-2 leading-relaxed" lang={lang}>
          {(WORDPRESS_TRANSCRIPT[lang] ?? []).map((paragraph) => (
            <p key={paragraph}>{paragraph}</p>
          ))}
        </div>
      </details>
    </div>
  );
}

/**
 * WordPress and WooCommerce (/wordpress): the plugin that indexes a WordPress site into
 * Turing and searches it from WordPress, told as the demo store's story. The plugin talks
 * to Turing directly, so its page lives here; Dumont is linked for the other sources.
 */
export function WordPressPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <WordPressJsonLd />
      <SiteNav />
      <main className="flex-1">
        {/* hero */}
        <section className="relative isolate overflow-hidden bg-[#080b18] py-16 text-[#f2f4fb] lg:py-20">
          <div className="grid-bg pointer-events-none absolute inset-0 -z-10" />
          <div className="animate-orb-a pointer-events-none absolute -top-40 -left-20 -z-10 h-[460px] w-[460px] rounded-full bg-[radial-gradient(circle,#4169E1,transparent_70%)] opacity-40 blur-[70px]" />
          <Container className="grid gap-10 lg:grid-cols-2 lg:items-center">
            <div className="min-w-0">
              <a
                href={withBase("/")}
                className="inline-flex items-center gap-1.5 text-sm font-semibold text-[#aeb7d4] transition-colors hover:text-white"
              >
                <ArrowLeft className="size-4" /> Back to overview
              </a>
              <p className="mt-6 text-xs font-extrabold tracking-[0.12em] text-[#7f8bbd] uppercase">
                Viglet Turing for WordPress and WooCommerce
              </p>
              <h1 className="mt-3 text-3xl font-extrabold leading-[1.1] tracking-tight sm:text-4xl lg:text-5xl">
                Search that understands what a shopper means, answered from your own catalog
              </h1>
              <p className="mt-5 text-lg leading-relaxed text-[#aeb7d4]">
                A WordPress plugin that indexes posts, pages and WooCommerce products into
                Turing, then puts search as you type, filters, cited answers, a shopping
                copilot and search insights on the site. Open source, Apache 2.0.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <Button asChild size="lg">
                  <a href={LINKS.wordpressDocs}>
                    Install the plugin <ArrowRight className="size-4" />
                  </a>
                </Button>
                <Button
                  asChild
                  size="lg"
                  variant="outline"
                  className="border-white/20 bg-white/10 text-white hover:bg-white/20 hover:text-white"
                >
                  <a href={LINKS.wordpressDemo} target="_blank" rel="noreferrer">
                    Try the demo store in your browser
                  </a>
                </Button>
              </div>
            </div>
            <div className="min-w-0">
              <VideoFacade />
            </div>
          </Container>
        </section>

        {/* the shopper's story */}
        <section id="story" className="scroll-mt-24 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-12 max-w-2xl text-center">
              <p className="text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
                One shopper, one visit
              </p>
              <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
                Her first two days on the fells, in eight steps
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                Tarnfell is a demo outdoor shop: every product and guide is invented, and
                everything else is the plugin and Turing running. Each picture is taken from
                the recorded story.
              </p>
            </div>
            <ol className="space-y-16">
              {WORDPRESS_STORY.map((c, i) => (
                <li key={c.title} className="grid items-center gap-8 lg:grid-cols-5 [&>*]:min-w-0">
                  <div className={i % 2 ? "lg:order-2 lg:col-span-2" : "lg:col-span-2"}>
                    <p className="font-mono text-sm font-bold text-primary">
                      Chapter {i + 1} · {c.title}
                    </p>
                    <h3 className="mt-2 text-2xl font-extrabold tracking-tight">{c.feature}</h3>
                    <p className="mt-3 leading-relaxed text-muted-foreground">{c.body}</p>
                  </div>
                  <img
                    src={withBase(`/wordpress/chapter-${i + 1}.webp`)}
                    alt={c.alt}
                    width={1280}
                    height={720}
                    loading={i ? "lazy" : "eager"}
                    className="w-full rounded-2xl border border-border shadow-sm lg:col-span-3"
                  />
                </li>
              ))}
            </ol>
          </Container>
        </section>

        {/* install band */}
        <section className="border-y border-border bg-muted/40 py-16 lg:py-20">
          <Container className="max-w-3xl text-center">
            <p className="text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
              On your own store
            </p>
            <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
              Install it, point it at Turing, reindex
            </h2>
            <p className="mt-3 text-base leading-relaxed text-muted-foreground">
              The plugin indexes posts, pages, custom post types and WooCommerce products,
              provisions their fields as facets, and keeps the index in step as content
              changes. The answers, the copilot and the assistant use the language model
              configured in Turing, and stay hidden on a Turing that has none.
            </p>
            <div className="mt-8 flex flex-wrap justify-center gap-3">
              <Button asChild size="lg">
                <a href={LINKS.wordpressDocs}>
                  Read the plugin's docs <ArrowRight className="size-4" />
                </a>
              </Button>
              <Button asChild size="lg" variant="outline">
                <a href={LINKS.docs}>Run Turing</a>
              </Button>
            </div>
          </Container>
        </section>

        {/* FAQ */}
        <section id="faq" className="scroll-mt-24 py-16 lg:py-20">
          <Container className="max-w-3xl">
            <h2 className="mb-8 text-center text-2xl font-extrabold tracking-tight sm:text-3xl">
              WordPress teams ask us
            </h2>
            <dl className="divide-y divide-border">
              {WORDPRESS_FAQ.map((item) => (
                <div key={item.q} className="py-6">
                  <dt className="text-base font-bold">{item.q}</dt>
                  <dd className="mt-2 text-sm leading-relaxed text-muted-foreground">{item.a}</dd>
                </div>
              ))}
            </dl>
          </Container>
        </section>

        {/* beyond WordPress */}
        <section id="beyond" className="scroll-mt-24 pb-16">
          <Container>
            <Card className="mx-auto max-w-3xl gap-0 border-primary/20 bg-primary/5 p-8 text-center sm:p-10">
              <h2 className="text-2xl font-extrabold tracking-tight">Beyond WordPress</h2>
              <p className="mx-auto mt-3 max-w-xl text-base leading-relaxed text-muted-foreground">
                Search more than one site? Viglet Dumont indexes Adobe Experience Manager,
                databases, file shares and any REST source into the same Turing, so one
                answer can cite a product, a policy PDF and a page together.
              </p>
              <div className="mt-8 flex flex-wrap justify-center gap-3">
                <Button asChild size="lg">
                  <a href={LINKS.dumont} target="_blank" rel="noreferrer">
                    Viglet Dumont <ArrowRight className="size-4" />
                  </a>
                </Button>
                <Button asChild size="lg" variant="outline">
                  <a href={withBase("/aem-search/")}>AEM AI Search</a>
                </Button>
              </div>
            </Card>
          </Container>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
