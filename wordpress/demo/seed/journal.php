<?php
/**
 * The demo storefront's trail journal (D194), loaded by catalog.php after the products: buying
 * guides and how-tos that name the shop's products, so one search finds both a guide and what
 * it recommends, which WordPress's own search cannot put together. Idempotent: a post is found
 * again by its slug.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a setup script run by WP-CLI, not plugin code.

$journal = array(
	// Buying guides.
	array( 'how-to-choose-a-waterproof-jacket', 'How to choose a waterproof jacket', 'Buying guides', array(
		'Hydrostatic head says how much water a fabric holds back; breathability says how much sweat it lets out. For a day of showers 10,000 mm is plenty. For a ridge in wind-driven rain, look for 20,000 mm or more and a hood that takes a helmet.',
		'The Catbells light rain jacket packs into its own pocket and weighs 240 g, the one to carry when rain is only forecast. The Helvellyn mountain shell is the one to wear when it has already started, with pit zips for the climb. For town and low paths, the Borrowdale everyday waterproof is longer and softer inside.',
		'Whatever you choose, wash it: a dirty shell wets out. A wash at 30 °C and a low tumble dry bring the water-repellent finish back.',
	) ),
	array( 'down-or-synthetic-insulation', 'Down or synthetic: choosing an insulated jacket', 'Buying guides', array(
		'Down is the warmest for its weight and packs smallest, but it loses its loft when wet. Synthetic fill is heavier for the same warmth and keeps working in drizzle.',
		'For cold, dry days and winter camps the Blencathra down jacket is the warmest we make for its weight. For the wet cold of a British winter the Glaramara synthetic jacket is the safer choice. If you move fast, the Kentmere hybrid jacket puts insulation where the wind hits and stretch fleece where you sweat.',
	) ),
	array( 'choosing-hiking-boots', 'Hiking boots or trail shoes?', 'Buying guides', array(
		'Boots protect ankles on rough ground and keep feet dry in bog; trail shoes are lighter and dry faster when they do get wet.',
		'For scree, snow patches and long days, the Scafell waterproof hiking boot takes a C1 crampon. For good paths, the Pillar lightweight hiking boot is comfortable from the first walk. If you would rather drain than stay dry, the Ennerdale trail shoe has 5 mm lugs for mud. In winter, only the Kirkstone winter boot takes a B2 crampon.',
		'Try boots on in the afternoon, when feet are largest, with the socks you will walk in.',
	) ),
	array( 'what-size-backpack', 'What size backpack do you need?', 'Buying guides', array(
		'A day on the fells needs room for a waterproof, a warm layer, lunch, water and a first-aid kit: about 25 to 30 litres. A night in a hut, 40 to 45. Carrying a tent and a week of food, 60 or more.',
		'The Helvellyn 28 litre daypack has a rain cover in its base. The Scafell 45 litre pack takes light kit for a night out. The Ennerdale 65 litre trekking pack carries a tent, with a hipbelt that takes most of the weight. For running, the Wasdale 12 litre running vest holds a waterproof and two flasks.',
	) ),
	array( 'choosing-a-backpacking-tent', 'Choosing a tent for backpacking', 'Buying guides', array(
		'Weight, space and how it stands up to wind are the three things to trade. A tent that pitches outer first keeps the inner dry when you pitch in rain.',
		'Alone, the Haystacks one-person tent weighs 1.1 kg and pitches outer first. For two, the Glaramara two-person tent gives each of you a door and a porch. For exposed, windy pitches and snow, the Blencathra mountain tent has four crossing poles. With children, the Langdale family tent has two bedrooms.',
	) ),
	array( 'sleeping-bag-temperature-ratings', 'Reading a sleeping bag temperature rating', 'Buying guides', array(
		'EN ISO 23537 gives two numbers. Comfort is the lowest temperature a cold sleeper stays comfortable at; limit is where a warm sleeper is curled up but safe. Choose by comfort.',
		'The Skiddaw three-season down bag has a comfort of -2 °C, enough from spring to autumn. For snow camps, the Bowfell winter down bag is comfortable to -10 °C. For summer and hostels, the Fairfield synthetic summer bag. A Grisedale sleeping mat matters as much as the bag: cold comes up from the ground.',
	) ),
	array( 'layering-for-the-hills', 'Layering for the hills', 'Buying guides', array(
		'Three layers do most of the work: a base layer that moves sweat away, a midlayer that holds warmth, and a shell that keeps out wind and rain.',
		'Start with the Coniston merino base layer, which stays fresh for days. Add the Fairfield grid fleece, the midlayer for most days. Carry a shell such as the Catbells light rain jacket, and keep a Haystacks down vest in the pack for the summit.',
	) ),
	array( 'trail-running-kit-list', 'A kit list for fell running', 'Buying guides', array(
		'Most fell races ask for full waterproof cover with taped seams, a hat, gloves, a map and a compass.',
		'The Wasdale running shell meets that rule at 150 g. Wear the Wansfell running top under it and the Ennerdale trail shoe on your feet; carry it all in the Wasdale 12 litre running vest. A Catbells merino beanie weighs 45 g.',
	) ),
	array( 'winter-hillwalking-kit', 'What to carry for winter hillwalking', 'Buying guides', array(
		'Winter on the British hills means short days, snow on the tops and wind that strips warmth. Carry more than you think you need.',
		'Wear the Kirkstone winter boot with crampons and the Borrowdale gaiters. Carry the Blencathra down jacket, the Helvellyn waterproof gloves and a spare pair, and the Wasdale head torch: the light goes by four.',
	) ),
	array( 'first-wild-camp-kit', 'Your first wild camp: what to take', 'Buying guides', array(
		'Shelter, sleep and something hot are the three things a first wild camp needs. Everything else is comfort.',
		'The Haystacks one-person tent, the Skiddaw three-season down bag and a Grisedale sleeping mat cover shelter and sleep. The Fairfield camp stove and the Langdale cook set boil water for tea in four minutes. Keep your sleeping bag in a Haystacks dry bag set inside the Scafell 45 litre pack.',
	) ),
	array( 'family-walking-kit', 'Walking with children: kit that helps', 'Buying guides', array(
		'Children get cold faster than adults and wet sooner. A pack of their own makes the walk theirs.',
		'The Catbells 20 litre kids pack has a whistle on the chest strap. Keep spare layers and snacks in it, and bring the Ennerdale insulated bottle with something hot. For a night out together, the Langdale family tent has room to stand.',
	) ),
	array( 'travel-light-kit', 'Travelling light with outdoor kit', 'Buying guides', array(
		'Outdoor kit makes good travel kit: it is light, dries fast and does not crease.',
		'The Borrowdale travel pack fits cabin luggage limits and opens like a suitcase. The Coniston merino base layer and the Grisedale wool jumper stay fresh for days. A Kirkstone silk liner keeps hostel bedding at a distance.',
	) ),
	// How-tos.
	array( 'how-to-reproof-a-waterproof-jacket', 'How to wash and reproof a waterproof jacket', 'How-tos', array(
		'When rain soaks into the outer fabric instead of beading, the shell is wetting out: the jacket still keeps water off, but you will feel damp from your own sweat.',
		'Wash it at 30 °C with a technical cleaner, never fabric softener. Tumble dry on low for twenty minutes: heat reactivates the finish. If water still soaks in, spray on a reproofer. This works for the Catbells light rain jacket, the Helvellyn mountain shell and the Skiddaw waterproof trousers alike.',
	) ),
	array( 'how-to-wash-a-down-jacket', 'How to wash a down jacket', 'How-tos', array(
		'Down clumps when wet, and a down jacket washed like a cotton one comes out flat.',
		'Use a down wash at 30 °C on a gentle cycle, rinse twice and tumble dry on low with two clean tennis balls until no clumps are left, which takes hours. The Blencathra down jacket and the Bowfell parka both come back to full loft this way.',
	) ),
	array( 'how-to-care-for-leather-boots', 'How to care for leather boots', 'How-tos', array(
		'Leather that dries hard cracks, and cracked leather lets water in.',
		'Brush the mud off while it is wet, dry the boots at room temperature with newspaper inside, never by a fire, and rub in a leather wax once they are dry. The Scafell waterproof hiking boot and the Kirkstone winter boot will last for years this way.',
	) ),
	array( 'how-to-pitch-a-tent-in-rain', 'How to pitch a tent in the rain', 'How-tos', array(
		'The trick is to keep the inner dry, and that starts at home: practise once in the garden.',
		'A tent that pitches outer first, like the Haystacks one-person tent or the Glaramara two-person tent, goes up with the inner clipped inside it. Peg the windward end first, then the poles, then the rest. Leave wet waterproofs in the porch.',
	) ),
	array( 'how-to-fit-a-backpack', 'How to fit a backpack', 'How-tos', array(
		'A pack carried on the shoulders hurts within the hour. The hipbelt should carry most of the weight.',
		'Loosen every strap, put the hipbelt on the top of your hip bones and tighten it, then the shoulder straps, then the load lifters to about 45 degrees. The Ennerdale 65 litre trekking pack and the Scafell 45 litre pack both have adjustable back lengths: set them before the first walk.',
	) ),
	array( 'how-to-store-a-sleeping-bag', 'How to store a sleeping bag', 'How-tos', array(
		'A sleeping bag kept in its stuff sack for months loses its loft for good.',
		'Store it loose in a large storage sack or hung in a cupboard. Dry it fully after every trip. The Skiddaw three-season down bag and the Bowfell winter down bag come with storage sacks for exactly this.',
	) ),
	array( 'how-to-use-trekking-poles', 'How to use trekking poles', 'How-tos', array(
		'Poles take weight off the knees on the way down and add a point of balance on rough ground.',
		'Set them so your elbow is at a right angle on the flat, shorter for the climb and longer for the descent. The Scafell trekking poles adjust from 105 to 135 cm with flick locks you can work in gloves.',
	) ),
	array( 'how-to-stay-warm-at-night-camping', 'How to stay warm at night when camping', 'How-tos', array(
		'Most cold nights are cold from below. A good mat matters more than a warmer bag.',
		'Sleep on a Grisedale sleeping mat, change into dry clothes before bed and eat something just before you sleep. A Kirkstone silk liner adds about two degrees, and a hot drink from the Ennerdale insulated bottle helps at three in the morning.',
	) ),
	array( 'how-to-dry-wet-boots', 'How to dry wet boots overnight', 'How-tos', array(
		'Boots dried by a fire or a radiator crack and come unglued.',
		'Take the insoles out, stuff the boots with newspaper and change it after an hour, then leave them somewhere warm and airy. The Ennerdale trail shoe needs none of this: it drains and dries as you walk.',
	) ),
	array( 'how-to-read-a-hydrostatic-head', 'What hydrostatic head means', 'How-tos', array(
		'Hydrostatic head is the height of a column of water, in millimetres, a fabric holds back before it leaks. More is more waterproof.',
		'A tent fly needs about 3,000 mm, a tent floor more because you kneel on it. A jacket needs 10,000 mm for showers and 20,000 mm or more for a storm: the Helvellyn mountain shell is rated 28,000 mm, the Borrowdale everyday waterproof 10,000 mm.',
	) ),
);

$ids = array();
foreach ( $journal as list( $slug, $title, $category, $paragraphs ) ) {
	$category_id = (int) ( term_exists( $category, 'category' ) ?: wp_insert_term( $category, 'category' ) )['term_id'];
	$content     = implode(
		"\n\n",
		array_map( static fn ( $p ) => "<!-- wp:paragraph -->\n<p>" . esc_html( $p ) . "</p>\n<!-- /wp:paragraph -->", $paragraphs )
	);
	$found       = get_page_by_path( $slug, OBJECT, 'post' );
	$args        = array(
		'post_type'     => 'post',
		'post_name'     => $slug,
		'post_title'    => $title,
		'post_content'  => $content,
		'post_excerpt'  => $paragraphs[0],
		'post_status'   => 'publish',
		'post_category' => array( $category_id ),
	);
	$ids[ $slug ] = $found ? (int) wp_update_post( array( 'ID' => $found->ID ) + $args ) : (int) wp_insert_post( $args );
}

return $ids;
