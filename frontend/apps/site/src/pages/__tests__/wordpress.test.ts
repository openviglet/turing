import { describe, expect, it } from "vitest";

import { videoLangFor } from "@/lib/wordpress-video";
import { WORDPRESS_STORY, WORDPRESS_VIDEOS } from "@/lib/site-content";
import { routes } from "@/entry-server";

/** T1251 — the WordPress page of Turing's own. */
describe("WordPress page", () => {
  it("is prerendered at /wordpress/", () => {
    expect(routes.map((r) => r.path)).toContain("/wordpress/");
  });

  it("tells the story in eight chapters, one still each", () => {
    expect(WORDPRESS_STORY).toHaveLength(8);
  });

  it("plays pt-BR to every visitor while the English cut is unpublished", () => {
    expect(WORDPRESS_VIDEOS["pt-BR"].id).toBe("UVoQXGcd-aI");
    expect(WORDPRESS_VIDEOS.en.id).toBeNull();
    expect(videoLangFor("pt-BR")).toBe("pt-BR");
    expect(videoLangFor("en-US")).toBe("pt-BR");
    expect(videoLangFor(undefined)).toBe("pt-BR");
  });
});
