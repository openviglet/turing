import { useEffect, useState, useSyncExternalStore } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  getConsent,
  initAnalyticsFromConsent,
  OPEN_CONSENT_EVENT,
  setConsent,
  subscribeConsent,
  type Consent,
} from "@/lib/analytics";
import { CHROME } from "@/lib/i18n";
import { useLocale } from "@/lib/locale-context";

const undecidedNow = () => getConsent() === null;
/** The prerendered HTML never carries the banner, so hydration starts hidden. */
const undecidedOnServer = () => false;

/**
 * Privacy-first consent banner (T1097). No analytics run until the visitor
 * accepts: on mount GA loads only if consent was already granted, otherwise
 * the banner shows and nothing loads. Accept and Decline weigh the same (LGPD:
 * refusing must be as easy as accepting). The footer's "Cookie settings" link
 * reopens it.
 */
export function ConsentBanner() {
  const locale = useLocale();
  const t = CHROME[locale].consent;
  const undecided = useSyncExternalStore(subscribeConsent, undecidedNow, undecidedOnServer);
  const [reopened, setReopened] = useState(false);

  useEffect(() => {
    initAnalyticsFromConsent();
    const reopen = () => setReopened(true);
    window.addEventListener(OPEN_CONSENT_EVENT, reopen);
    return () => window.removeEventListener(OPEN_CONSENT_EVENT, reopen);
  }, []);

  if (!undecided && !reopened) return null;

  const choose = (consent: Consent) => {
    setConsent(consent);
    setReopened(false);
  };

  return (
    <section
      aria-label={t.region}
      aria-live="polite"
      className="fixed inset-x-0 bottom-0 z-50 p-4 sm:p-6"
    >
      <div className="mx-auto max-w-3xl rounded-2xl border border-border bg-card p-5 shadow-xl sm:p-6">
        <div className="flex items-start gap-4">
          <div className="min-w-0 flex-1">
            <p className="mb-1 font-bold text-foreground">{t.title}</p>
            <p className="text-sm leading-relaxed text-muted-foreground">{t.body}</p>
          </div>
          <button
            type="button"
            onClick={() => choose("denied")}
            aria-label={t.declineAndClose}
            className="shrink-0 text-muted-foreground transition-colors hover:text-foreground"
          >
            <X className="size-4" />
          </button>
        </div>
        <div className="mt-4 flex flex-wrap justify-end gap-3">
          <Button variant="outline" onClick={() => choose("denied")}>
            {t.decline}
          </Button>
          <Button onClick={() => choose("granted")}>{t.accept}</Button>
        </div>
      </div>
    </section>
  );
}
