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

## The demo videos

The demo videos, their narration, music, footage and thumbnail, are made in the marketing
repository (`marketing/turing/wordpress`), whose `CREDITS.md` credits them. They show this
store, so the credits on this page apply to what they show of it.

## Photos

The product and guide photos in [`../photos/`](../photos/) are photorealistic images generated
with Ideogram 4.0 (`npm run demo:photos`, D265), bought through polyweave inside the budget in
`polyweave.toml`. They show the invented products, laid flat on a table, pitched on level grass
or out on the hill, and invented scenes: no real person, place name, brand or logo was asked
for. Every product and guide has one. The repository keeps an 800 px WebP of each, the size a
shop shows, with its `.prov.json` recording the prompt and the service; the full-size PNG
Ideogram returns is not committed. The seed shows each photo whole, over a blurred copy of
itself where its shape differs from the frame, since Ideogram does not always return the aspect
ratio asked. A product or guide without a photo would be drawn by the seed, as above.
