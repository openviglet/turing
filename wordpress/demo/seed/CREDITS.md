# Demo storefront credits

Tarnfell, its products and its journal are fictional, made for this repository.

## Images

Every product and guide picture is drawn at seed time by [`images.php`](images.php) (GD,
saved as WebP): a flat illustration of each product in its colour, and a fell landscape for
each guide. Nothing is downloaded, and no photograph, real person or real brand's product
appears in any of them. They are part of this repository and dedicated to the public domain
([CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/)), so they may be
redistributed with it.

[`preview-images.php`](preview-images.php) draws every kind on one contact sheet, to look at
before a seed.

## Narration

The demo video's narration ([`../narration/`](../narration/), D246) is synthesised speech:
ElevenLabs' premade voice *Matilda* on the `eleven_multilingual_v2` model, bought on an
ElevenLabs plan that grants commercial use, so the takes may be published with the video.
The lines are [`script.json`](../narration/script.json), written for this repository; each
take's `.prov.json` records the voice, the model, the plan and the line it says.
`npm run story:narration` buys a take again only when its line changes.
