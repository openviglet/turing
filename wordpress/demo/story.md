# One shopper's story through the demo store

The order every surface follows: the tour, the site and the docs walk the demo in these
chapters, and `npm run e2e:story` plays them end to end on `run-docker.cmd demo` with
`OPENAI_API_KEY` set (chapters 3 to 6 ask Turing's language model).

The shopper is preparing her first summer crossing of the fells: two days, one night out.
She knows roughly what she wants and not what it is called.

| # | Chapter | Feature |
|---|---|---|
| 1 | Arrives | Popular searches (D202) |
| 2 | Says what she needs | Text to filters and spelling (D199) |
| 3 | Asks | The Site assistant's cited answer (D198) |
| 4 | Compares | Products side by side, with a summary (D200) |
| 5 | Builds the kit | Build my kit (D201) |
| 6 | Checks one item | The product page, Related and its copilot |
| 7 | Buys | The cart and checkout, in test mode |
| 8 | Backstage | Compare searches (D195) and Search insights (D178) |

## 1. Arrives

She opens **Search the shop**. Before she types, *Popular:* chips under the box show what
other visitors found, so she sees the catalog at a glance. Only searches that found
something become chips.

- en_US: chips such as `rain jacket`, `daypack`.
- pt_BR: chips such as `jaqueta de chuva`, `mochila`.

## 2. Says what she needs

She types a sentence, not a filter. The price is read as money, applied as a filter and
said back as *Understood: …*, and the box keeps the words that are left.

- en_US: `light rain jacket under $400` → *Understood: Price: ≤ 400*, the box keeps
  `light rain jacket`, rain jackets first.
- pt_BR: `jaqueta de chuva leve até R$ 400` → *Entendido: Price: ≤ 400*.

Then she misspells a name and the search offers the correction, and she tries a word the
shop does not use, which finds nothing (chapter 8 comes back to it).

- `helvelyn` → the correction `helvellyn`.
- `packraft` → no results.

## 3. Asks

She opens the **Site assistant** in the corner and asks about the night out. The answer is
written from the shop's guides and products, with the sources it read: the backpack guide.

- en_US: `What size backpack do I need for a night in a hut?`
- pt_BR: `Que mochila eu levo para uma noite num refúgio?`

## 4. Compares

She searches `waterproof jacket` (`jaqueta impermeável`), ticks **Compare** on three
jackets and opens them side by side. The rows that differ are marked, and a summary
written by AI from the table sits above it.

## 5. Builds the kit

On the showcase's **Ask the catalog**, she switches to **Plan a kit** and describes the
trip. The kit comes back staged by need, each product priced and stocked by the shop, with
the journal guides that cover the plan. She adds the kit to the cart; the total is
WooCommerce's.

- en_US: `Two days crossing the mountains in July, first time`
- pt_BR: `Dois dias atravessando a montanha em julho, primeira vez`

## 6. Checks one item

She opens one product from the kit. **Related** shows the products and guides most like
it, and the product's own copilot (*Not sure it fits your trip? Ask us*) answers a
question about it with the products it cites.

- en_US: `Will this keep me dry on a wet day in the hills?`
- pt_BR: `Isso me mantém seca num dia de chuva na montanha?`

## 7. Buys

From the cart she checks out with **Check payment**, the demo's offline method: the order
is placed and nothing is charged.

## 8. Backstage

The shop owner's view of the same visit.

- **Compare searches**: `raincoat` (`capa de chuva`) on WordPress's own search finds
  nothing; Turing finds the guide and the rain jackets through a synonym.
- **Search insights** (wp-admin, today): her searches are among the top ones, and a search
  that found nothing, `packraft`, is listed with **Add a synonym**. Saved as
  `packraft, dry bag`, the same search now finds the dry bags.
