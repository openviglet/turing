# The demo video

A narrated walk through the demo store: a shopper's visit in eight chapters, recorded by
Playwright on the live demo stack, told by **Ana**, the store's owner, in the store's language.
`node bin/story-video.mjs --language <lang>` assembles one language's video into
`tests/e2e/artifacts/video/story.<lang>.mp4` (not committed: a release asset, D246).

Status, 2026-11-03: the **English** video, the primary one (D276), is recorded on Tarnfell
(6m53s) on a Turing image carrying T1190, T1191 and T1196, with the cards of D275 and the
narration as captions (D278). The **pt-BR** one (Recanto da Serra, 7m37s) was recorded on
2026-11-02. Open lines that show in them are listed at the end.

## What it is made of

| Piece | Where | Made by |
|---|---|---|
| Ana's lines: an opening, per chapter a line over the action and one over its result, a closing | `demo/narration/script.json` | written by hand; every claim is one the store really does |
| Her voice, one take per line and language | `demo/narration/<lang>/*.mp3` (not committed; `.prov.json` are) | `npm run story:narration [-- --language pt-BR]`: ElevenLabs `eleven_v3` through polyweave, buys only changed lines |
| Music bed | `demo/narration/music.mp3` (not committed) | polyweave `sound.buy` on ElevenLabs |
| Product and guide photos | `demo/photos/<kind>/<slug>.webp` (800 px, committed; PNGs are not) | `npm run demo:photos`: Ideogram 4.0 through polyweave; the seed imports a photo where one exists, draws one where none does |
| Footage of the hills | `demo/broll/*.mp4` (not committed; `.json` with source, author, licence are) | `npm run demo:broll` with `PIXABAY_API_KEY` |
| The recording | `tests/e2e/artifacts/story/story.webm` + `chapters.json` | `npm run e2e:story:record` (`tests/e2e/story/shopper.story.mjs`) |
| The video | `tests/e2e/artifacts/video/story.<lang>.mp4` | `node bin/story-video.mjs --language en` (or `pt-BR`) |
| An editable project | `tests/e2e/artifacts/video/story.<lang>.project/` (`story.<lang>.fcpxml` + `media/`) | `node bin/story-video.mjs --language en --project` (D290) |
| A clip per chapter | `tests/e2e/artifacts/video/story-chapter-<n>.<lang>.mp4` | `node bin/story-video.mjs --language en --chapter <n>` (D279) |
| The phone-width recording | `tests/e2e/artifacts/story-vertical/story.webm` + `chapters.json` | `npm run e2e:story:vertical` (D280) |
| A vertical clip per chapter, 9:16 | `tests/e2e/artifacts/video/story-chapter-<n>-vertical.<lang>.mp4` | `node bin/story-video.mjs --language en --vertical --chapter <n>` |

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

1. **The stores.** `run-docker.cmd demo` sets up both, side by side (D283): Tarnfell in English on
   `localhost:38082` (SN site `tarnfell`) and Recanto da Serra in reais on `localhost:38083` (SN
   site `recanto-da-serra`). `run-docker.cmd demo reset pt` rebuilds one without touching the other.
   WP-CLI on the Portuguese store is `cli-demo-pt`; each store keeps its SN site and language in
   its own settings (D272, D274), so a manual `wp` call needs no variables. (Before D283 any
   manual `wp` call on the demo container needed both variables, or it reindexed into the other
   language's core.)
2. **The assets**: `npm run story:narration`, `npm run demo:photos`, `npm run demo:broll`.
3. **Check what the video will show before recording.** Run each search the story makes in the
   real search block and look at the results, the filter line and the answer; change a query that
   does not show a perfect result (`say` in the spec). The story itself fails on an empty or
   apologising AI answer.
4. **Record**: `npm run e2e:story:record` for Tarnfell, `DEMO_LOCALE=pt_BR npm run
   e2e:story:record` for Recanto da Serra. A run that fails writes no new `chapters.json`; the
   video script refuses a recording older than the spec.
5. **Assemble**: `node bin/story-video.mjs --language en` (or `pt-BR`): only that language's
   narration and cards, into `story.<lang>.mp4`, so one never overwrites the other. Only the
   English run copies its stills into the site's WordPress page.
6. **Look at it**: frames across the whole video, and the moment each line ends against its Enter.
   To change a cut, a level or a timing by hand, add `--project`: the same timeline is written
   as an FCPXML for DaVinci Resolve (File > Import > Timeline), with every file it uses copied
   into its `media/` folder (D290). V1 is the recording, transcoded once to H.264 and cut by
   chapter, with each held result as a still; V2 the cards; V3 the opening and closing shots,
   as rendered; A1 each take; A2 the music bed at the video's level. Chapters are markers.
   The project folder also gets `finish.py` (from `demo/resolve/finish.py`, D293), for what
   FCPXML cannot carry. Resolve's free version runs scripts only from inside the app: in
   Workspace > Console > Py3, `exec(open(r"<project folder>\finish.py").read())`. It creates
   the project `story.<lang>`, imports the timeline, prints how many clips came in offline,
   adds the captions as a subtitle track, applies the Fairlight preset "Viglet story voice"
   when one is saved, and queues a YouTube render without starting it.
7. **The clips** (D279): `--chapter 2`, `3` and `5` are the ones to publish (a sentence read as
   a price filter, the Site assistant with its sources, the kit). Each opens on the chapter's
   title under the product's name, plays the chapter with both of Ana's lines, holds the result
   to the end of her second, and closes on the full-video and Playground card. Where that runs
   past 58 s, the stretch between her first line and the result, where she says nothing, plays
   faster until it fits (chapter 5 at about 2x). The recording carries no language: build a
   language's clips right after recording that language's store.
8. **The vertical clips** (D280), for Shorts and the like: `npm run e2e:story:vertical` plays
   the same story with the same takes laid out 432 CSS pixels wide in a phone-emulated
   1080x1920 viewport (the meta viewport set to `width=432`), so each frame is a whole page at
   a phone's width and the frame's full resolution, into its own folder; it replaces neither
   recording. A device scale factor does not do this: Playwright films at the viewport's CSS
   size, so a 432x768 viewport at 2.5x lands small in a corner of the frame. Then
   `--vertical --chapter <n>` cuts it as above, with the cards laid out for 9:16 and the
   captions burned into the picture above the bottom fifth, where vertical players draw their
   own title and buttons over a caption track. On a phone the search filters are a drawer
   behind Filters, which the story opens and closes. Anything else the story cannot show at
   that width is a finding for the plugin's layout, filed on its own.

## The developer walkthrough (D281)

A second, shorter piece for whoever puts the plugin on a client's site: install, connect,
index, then the storefront and the insights. Its recording exists; its narration and assembly
are still open on D281.

1. **The store.** `run-docker.cmd fresh` (or `sh bin/run-docker.sh fresh`) adds a WordPress on
   `localhost:38084` that the plugin was never installed on: Tarnfell's theme, WooCommerce and
   catalog, seeded once through WP-CLI with `src/` mounted, then that copy deactivated and every
   option it wrote deleted (`demo/seed/forget-plugin.php`). Its web container has no plugin at
   all, and its connection is not set from the environment.
2. **Record**: `npm run e2e:developer:record` (`tests/e2e/developer/install.developer.mjs`)
   builds the release zip as `bin/build-zip.sh` lays it, deletes the SN site `tarnfell-shop` in
   Turing so the Setup page creates it, mints an API key, and plays six chapters: upload and
   activate the zip, connect on the Setup page, turn on Products under Settings, Indexing, run
   the first index, search the storefront, open Insights. It leaves `walkthrough.webm` and
   `chapters.json` in `tests/e2e/artifacts/developer/`.
3. **Again**: the run installs the plugin, so it skips a store that has it. Either
   `run-docker.cmd fresh reset` then `run-docker.cmd fresh`, or, faster, `wp plugin deactivate`
   and `wp plugin delete viglet-turing-search` on `cli-fresh`, then `wp eval-file
   /demo/seed/forget-plugin.php`.

Its first run found D299: the Setup page could not create an SN site at all.

## The frame follows the action (D292)

The story marks the element each step acts on (`focus()` in the spec: the search box, the
answer, the assistant, the comparison, the kit's plan, the product question) with its box on
screen, into `chapters.json`. The video eases in on each box over 0.8 s, holds while the step
runs and eases back out before the next, at most 1.6x so text stays sharp from 1080p, with a
quarter of the box's size as room; a box too big to zoom, or a step too short for both moves,
stays at the full frame. The held result frame is taken from the followed clip, so it is
pushed in too. Vertical cuts are already a phone's page and are left alone. The Resolve
project does not carry the moves yet.

## The mix (D291)

- The music bed ducks under every line: 0.12 under a take, 0.3 between takes, with a 0.4 s
  ramp either side (about 8 dB). The project carries the same envelope as volume keyframes
  on A2, so it can be moved in Fairlight.
- The mix is set to -14 LUFS integrated, YouTube's playback level, under -1 dBTP: a measured
  gain, a limiter 1.5 dB under the ceiling (AAC overshoots a sample-peak limit), and a few
  corrections until it is within 0.3 LU. loudnorm alone stopped at -15.1. The script prints
  what each mix measured beside the video's length.
- Dissolves, half a second, wherever a card meets the recording or a shot and between the
  opening and closing shots: the still crossfades over the clip beside it and goes on through
  the blend, so no piece moves and every take stays in sync. Inside a chapter the cuts stay
  hard. The Resolve project keeps hard cuts there for now (D300).
- No J-cuts: every chapter's line starts 0.2 to 1.1 s into the chapter's action, after its
  card, because the action is paced to the line. A line recorded to start under its card
  would be the case for one.

## Rules learned making it

- Every search sent to the recording's SN site counts in its insights, which chapter 8 shows:
  probe a store with `populateMetrics: false`, or on another site.
- A take is placed only if its `.prov.json` says the current line, voice and model: change a line,
  buy it again.
- The Turing image must carry T1190, T1191, T1196 and T1200 (`ghcr.io/openviglet/turing`
  2026.3.8 or later). A `latest` pulled before 2026-11-02 lacks T1200, so it counts every
  sentence the block searches uncounted, and chapter 8 lists it as a failed search:
  `docker pull ghcr.io/openviglet/turing:latest`, then `docker compose up -d turing`.
- Chapter 8 shows today's insights, so the store's own tests and probes show there too. Before
  recording, clear the SN site's metrics (`DELETE /api/sn/<site id>/metrics`, T1195), delete
  the `tarnfell_searches_<site>` option and run `demo/seed/searches.php` again.
- The Playground blueprint indexes into the same SN site, `tarnfell`, under the site key
  `tarnfell`, while the local store's key is `localhost`. A local Playground run leaves a second
  copy of the catalog there, and Build my kit (which has no site filter) shows every product
  twice with a "Site: localhost" reason. Before recording, `fq[]=site_key:tarnfell` must find
  nothing; delete those documents if it does.
- In user-facing words it is always **Viglet Turing**, never Turing alone.
- A clean video asks for a believable store: dated over months (D264), its own name, currency and
  pages in its language (D263, D268), a search history (D269), photos (D265).

## Open, and seen in the video

- D265: 54 photos still to buy (Ideogram credit); the rest are drawings.
- D282: the video on the site, the docs, the README and the release (YouTube embeds).
- D283: the English and pt-BR stores side by side, so recording one does not delete the other.

Gone from it since the first cut: the English addresses on the pt-BR store (D266), a refined
sentence counted as a failed search (D267), and the kit's "Site: localhost" reasons (T1191).
Chapter 8's failed search is now "cantil": "bastão de caminhada" finds its products since T1196.
