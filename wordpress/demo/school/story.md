# One family's week at the demo school

The order the school's surfaces follow: its tour numbers the features by these chapters, and
its specs (`tests/e2e/demo-edu/`, `npm run test:e2e:edu`) walk them on `run-docker.cmd demo edu`
(Lumen School, English, `localhost:38085`) or `demo edu-pt` (Escola Lumen, Portuguese,
`localhost:38086`, with `DEMO_LOCALE=pt_BR`). Nothing here needs a language model.

A parent whose daughter starts at the school next year wants to know what is on, whether
enrolment is still open, and which book the reading club talks about. The school is fictional:
every event, opening, book and author is invented.

The seed counts every date from the day it runs, so the agenda is never stale: the open day is
today, the science fair in three days, the choir concert in twelve, one debate on the 15th of
next month and another a month later, the parents' evening twenty days ago and sports day sixty.

| # | Chapter | Feature |
|---|---|---|
| 1 | Looks at what is on | A date's agenda periods (D307) |
| 2 | Says when, in words | A period read from a sentence (D324) |
| 3 | Reads the cards | The card's date and image per type (D305) |
| 4 | Looks for an opening | A block fixed to one type (D306), and leaving search on a day (D308) |
| 5 | Finds a book by a sentence | A PDF's inner words (D309), attached or linked (D319) |
| 6 | Backstage | The mapping that does all of the above |

## 1. Looks at what is on

She opens the **Agenda**: a search block fixed to events, so a book or a job never lands
there. Its **When** filter does not offer "last 7 days" as a blog's would; it offers an
agenda's periods, counted from today.

- en_US: **Today** lists the open day; **Next 7 days** adds the science fair; **Upcoming**
  adds the concert and both debates; **Past** holds the parents' evening and sports day.
- pt_BR: **Hoje**, **Próximos 7 dias**, **Este mês**, **Próximos**, **Passados**.

## 2. Says when, in words

She types what she would say at the front desk. The month is read as a period on the event's
day, offered as a filter, and the box keeps the words that are left.

- en_US: `debates in December` (the month after today's) → *When: Dec 1 – Dec 31*, one
  debate; the other one, a month later, is not listed.
- pt_BR: `debates em dezembro`.

## 3. Reads the cards

Each event's card shows the day it happens and its poster, not the day the office posted it.
An opening's card shows the day it closes.

## 4. Looks for an opening

**Openings** is fixed to openings. The arts scholarship and the library assistant post are
there; the summer camp call, which closed two days ago, is not, though its page is still
published. When the office moves a closing day into the past and saves, the opening leaves
search; moved forward, it is back.

## 5. Finds a book by a sentence

She remembers one line from the reading club's book, not its title. In the **Library**,
`lighthouse keeper counted forty one gulls` (`faroleiro contou quarenta e uma gaivotas`)
finds *The Tide Atlas* (*O Atlas das Marés*): the words are in its sample chapter, a PDF, and
nowhere in the book's page. The school's whole search lists the chapter itself as a PDF, and
the maths teacher's full call, a PDF the opening only links, is found by its words too.

## 6. Backstage

In wp-admin, **Viglet Turing Search → Fields** shows what the seed mapped: the event's day as a DATE
field with the agenda periods and as the card's date, its poster as the card's image; the
opening's closing day as both its card's date and the day it leaves search; the book's sample
chapter as text, and **Index PDF files** on in Settings.

The archive half of a school (a repository harvested over OAI-PMH by a Dumont source) is not
part of this demo yet.
