/**
 * T1027: the outbound links, on their own. T1270: derived from the typed
 * destination map in lib/destinations.ts, which says what each one is for;
 * this module only resolves site paths against the base.
 */
import { DESTINATIONS, destinationHref, type DestinationKey } from "@/lib/destinations";
import { withBase } from "@/lib/site-url";

export const LINKS = Object.fromEntries(
  (Object.keys(DESTINATIONS) as DestinationKey[]).map((key) => [key, destinationHref(key, withBase)])
) as Record<DestinationKey, string>;
