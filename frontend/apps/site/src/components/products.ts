/**
 * T1268: the Viglet product identities, as the viglet.org hub names them.
 *
 * Visitor-facing labels render `fullName` and nothing shorter: the hub's
 * naming decision (always Viglet Turing / Viglet Dumont / Viglet Shio, even on
 * repeated mentions) has no short display alias to fall back to. `id` is the
 * technical key (slugs, logo identifiers, colour lookups) and is never shown.
 */
export type ProductId = "turing" | "dumont" | "shio";

export interface ProductIdentity {
  id: ProductId;
  fullName: string;
}

export const PRODUCTS: Record<ProductId, ProductIdentity> = {
  turing: { id: "turing", fullName: "Viglet Turing" },
  dumont: { id: "dumont", fullName: "Viglet Dumont" },
  shio: { id: "shio", fullName: "Viglet Shio" },
};
