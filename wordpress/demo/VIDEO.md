# The demo video

A narrated walk through the demo store: a shopper's visit in eight chapters, recorded by
Playwright on the live demo stack, told by **Ana**, the store's owner, in the store's language.
`npm run story:video` assembles it into `tests/e2e/artifacts/video/story.mp4` (not committed:
a release asset, D246).

Status, 2026-10-31: the **pt-BR** video is done (Recanto da Serra, 7m28s). The English one is
D261's remainder. Open lines that show in it are listed at the end.

## What it is made of

| Piece | Where | Made by |
|---|---|---|
| Ana's lines: an opening, per chapter a line over the action and one over its result, a closing | `demo/narration/script.json` | written by hand; every claim is one the store really does |
| Her voice, one take per line and language | `demo/narration/<lang>/*.mp3` (not committed; `.prov.json` are) | `npm run story:narration [-- --language pt-BR]`: ElevenLabs `eleven_v3` through polyweave, buys only changed lines |
| Music bed | `demo/narration/music.mp3` (not committed) | polyweave `sound.buy` on ElevenLabs |
| Product and guide photos | `demo/photos/<kind>/<slug>.webp` (800 px, committed; PNGs are not) | `npm run demo:photos`: Ideogram 4.0 through polyweave; the seed imports a photo where one exists, draws one where none does |
| Footage of the hills | `demo/broll/*.mp4` (not committed; `.json` with source, author, licence are) | `npm run demo:broll` with `PIXABAY_API_KEY` |
| The recording | `tests/e2e/artifacts/story/story.webm` + `chapters.json` | `npm run e2e:story:record` (`tests/e2e/story/shopper.story.mjs`) |
| The video | `tests/e2e/artifacts/video/story.mp4` | `node bin/story-video.mjs --language pt-BR` |

Credits for all of it: `demo/seed/CREDITS.md`.

## How the story and the voice stay in step

- A chapter's line is spoken **over its action**: the spec calls `speak('chapter-N')` where the
  action starts, reads the take's length, types at the pace that fills it, and presses Enter (or
  clicks) as her last word lands. `chapters.json` records where each line starts.
- The line about the **result** is spoken over that moment **held still** (the chapter's
  `highlight()`), so she always talks about what is on screen.
- The opening and closing cards hold a title for 3 s, then footage, photos and two of the store's
  pages, changing at a pause in her voice at least 4 s apart.
- Chapter cards are the title alone, 2.5 s.

## Making it again

1. **The stack, in the video's language.** pt-BR: `DEMO_LOCALE=pt_BR SN_DEMO_SITE=recanto-da-serra
   run-docker.cmd demo` (the store becomes *Recanto da Serra*, in reais, on its own SN site). English:
   `run-docker.cmd demo` (Tarnfell, SN site `Demo`). Any manual `wp` call on the demo container
   needs the same two variables, or it reindexes into the other language's core.
2. **The assets**: `npm run story:narration`, `npm run demo:photos`, `npm run demo:broll`.
3. **Check what the video will show before recording.** Run each search the story makes in the
   real search block and look at the results, the filter line and the answer; change a query that
   does not show a perfect result (`say` in the spec). The story itself fails on an empty or
   apologising AI answer.
4. **Record**: `DEMO_LOCALE=pt_BR npm run e2e:story:record`. A run that fails writes no new
   `chapters.json`; the video script refuses a recording older than the spec.
5. **Assemble**: `node bin/story-video.mjs --language pt-BR` (only that language's narration, the
   cards in it; the site's English stills are left alone).
6. **Look at it**: frames across the whole video, and the moment each line ends against its Enter.

## Rules learned making it

- Every search sent to the recording's SN site counts in its insights, which chapter 8 shows:
  probe a store with `populateMetrics: false`, or on another site.
- A take is placed only if its `.prov.json` says the current line, voice and model: change a line,
  buy it again.
- In user-facing words it is always **Viglet Turing**, never Turing alone.
- A clean video asks for a believable store: dated over months (D264), its own name, currency and
  pages in its language (D263, D268), a search history (D269), photos (D265).

## Open, and seen in the video

- D261: the English video.
- D265: 54 photos still to buy (Ideogram credit); the rest are drawings.
- D266: the pt-BR store's addresses are English (`product › …`).
- D267: a refined sentence is counted as a failed search in the insights.
- Turing T1190, T1191: the kit's first picks and its "Site: localhost" reasons. T1197 (the kit in
  the request's language) is fixed in Turing's source, not yet in the published image the stack runs.
- D246: attaching the video to a release and embedding it on the site.
