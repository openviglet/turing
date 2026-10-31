import { useEffect, useState } from "react";
import { fetchSitePersonas, type TuringClient } from "@viglet/turing-sdk";
import type { TuringPersonaOption } from "@viglet/turing-react-ui";

/**
 * T1193: the personas the playground offers ("same question, different eyes" +
 * "validate content as persona X"). Live, they are exactly what the demo site's
 * `GET /api/sn/{site}/personas` returns (T1143), the ids its content-fit accepts,
 * so the picker never offers an id the backend refuses. Offline, the scripted mode
 * shows these samples; they never reach a backend.
 */
export const OFFLINE_SAMPLE_PERSONAS: TuringPersonaOption[] = [
  {
    id: "sample-skeptical-developer",
    name: "Skeptical Developer",
    description: "Senior engineer. Wants precise, verifiable answers and distrusts hype.",
  },
  {
    id: "sample-enterprise-buyer",
    name: "Enterprise Buyer",
    description: "Decision-maker scanning for business value, security and compliance.",
  },
  {
    id: "sample-first-time-user",
    name: "First-time User",
    description: "New to the product. Needs plain, jargon-free explanations.",
  },
];

/** The site's personas, or none when the call fails: a failure offers nothing rather than a guess. */
export async function loadSitePersonas(client: TuringClient, site: string): Promise<TuringPersonaOption[]> {
  try {
    return (await fetchSitePersonas(client, site)).map((p) => ({
      id: p.id,
      name: p.name,
      description: p.description ?? undefined,
    }));
  } catch {
    return [];
  }
}

/**
 * The playground's personas: the site's own while live (`null` until they load),
 * the offline samples otherwise. An empty live list means the persona features hide.
 */
export function usePlaygroundPersonas(
  client: TuringClient | null,
  site: string | null,
): TuringPersonaOption[] | null {
  const live = Boolean(client && site);
  const [loaded, setLoaded] = useState<{ site: string; personas: TuringPersonaOption[] } | null>(null);

  useEffect(() => {
    if (!client || !site) return;
    let alive = true;
    void loadSitePersonas(client, site).then((personas) => {
      if (alive) setLoaded({ site, personas });
    });
    return () => {
      alive = false;
    };
  }, [client, site]);

  if (!live) return OFFLINE_SAMPLE_PERSONAS;
  return loaded?.site === site ? loaded.personas : null;
}
