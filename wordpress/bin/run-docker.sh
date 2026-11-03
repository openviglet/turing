#!/bin/sh
# run-docker.cmd's start, for Linux and macOS (and the browser suite's CI job, D182): the
# admin screens built, WordPress and Turing up (docker-compose.yml next to this folder),
# WordPress installed with the plugin active, and the SN site provisioned with an API key.
#
#   sh bin/run-docker.sh              start both, install WordPress and provision Turing
#   sh bin/run-docker.sh demo         also the two demo storefronts side by side (D248, D283):
#                                     Tarnfell in English on WP_DEMO_PORT (38082) and Recanto
#                                     da Serra in Portuguese on WP_DEMO_PT_PORT (38083)
#   sh bin/run-docker.sh demo en|pt   one of them
#   sh bin/run-docker.sh demo reset en|pt  stop one store and delete its WordPress and database
#   sh bin/run-docker.sh fresh        also a WordPress the plugin was never installed on (D281),
#                                     on WP_FRESH_PORT (38084), for the developer video
#   sh bin/run-docker.sh fresh reset  delete it, to record that video again
#
# The same environment as run-docker.cmd: WP_PORT (8080), TURING_PORT (2700), SN_SITE
# (Sample), TURING_LOCALE (en_US), TURING_ADMIN_PASSWORD (dumont-dev), TURING_IMAGE, and for
# the demos WP_DEMO_PORT (38082), WP_DEMO_PT_PORT (38083), SN_DEMO_SITE (tarnfell) and
# SN_DEMO_PT_SITE (recanto-da-serra). OPENAI_API_KEY
# turns on Turing's generative AI. Provisioning runs bin/turing-setup.ps1, so it needs
# PowerShell (`pwsh`).
set -eu
cd "$(dirname "$0")/.."

: "${WP_DEMO_PORT:=38082}"
: "${WP_DEMO_PT_PORT:=38083}"
: "${SN_DEMO_SITE:=tarnfell}"
: "${SN_DEMO_PT_SITE:=recanto-da-serra}"
export WP_DEMO_PORT WP_DEMO_PT_PORT

# Which demo store (D283): its services' suffix, port, SN site and language.
demo_store() {
	case "$1" in
	en) sfx='' port=$WP_DEMO_PORT site=$SN_DEMO_SITE locale=en_US ;;
	pt) sfx='-pt' port=$WP_DEMO_PT_PORT site=$SN_DEMO_PT_SITE locale=pt_BR ;;
	*)
		echo "Unknown demo store: $1. Use en or pt." >&2
		exit 1
		;;
	esac
}

if [ "${1:-}" = demo ] && [ "${2:-}" = reset ]; then
	if [ -z "${3:-}" ]; then
		echo 'Name the store to reset: sh bin/run-docker.sh demo reset en or pt' >&2
		exit 1
	fi
	demo_store "$3"
	docker compose --profile demo rm -s -f "wordpress-demo$sfx" "cron-demo$sfx" "db-demo$sfx"
	docker volume rm "viglet-dumont-wp_wp-demo$sfx" "viglet-dumont-wp_db-demo$sfx"
	exit 0
fi

: "${WP_FRESH_PORT:=38084}"
export WP_FRESH_PORT
if [ "${1:-}" = fresh ] && [ "${2:-}" = reset ]; then
	docker compose --profile fresh rm -s -f wordpress-fresh cron-fresh db-fresh
	docker volume rm viglet-dumont-wp_wp-fresh viglet-dumont-wp_db-fresh
	exit 0
fi

: "${WP_PORT:=8080}"
: "${TURING_PORT:=2700}"
: "${SN_SITE:=Sample}"
: "${TURING_LOCALE:=en_US}"
: "${TURING_ADMIN_PASSWORD:=dumont-dev}"
export WP_PORT TURING_PORT TURING_ADMIN_PASSWORD
if [ -n "${TURING_IMAGE:-}" ]; then
	export TURING_IMAGE
fi

wp() {
	docker compose run --rm --no-deps cli wp "$@"
}

# The admin screens are built JS; src/ is mounted as is, so build it here.
for build in health mapping setup tuning insights turing-sdk; do
	if [ ! -f "src/admin/build/$build.asset.php" ]; then
		echo 'Building the admin screens...'
		npm ci --no-audit --no-fund
		npm run build
		break
	fi
done

docker compose up -d

echo 'Waiting for WordPress...'
tries=0
until wp db check >/dev/null 2>&1; do
	tries=$((tries + 1))
	if [ "$tries" -ge 60 ]; then
		echo 'WordPress did not come up. See: docker compose logs wordpress' >&2
		exit 1
	fi
	sleep 2
done

if ! wp core is-installed >/dev/null 2>&1; then
	echo 'Installing WordPress...'
	wp core install --url="http://localhost:$WP_PORT" --title='Viglet Dumont test' \
		--admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email
fi
wp plugin activate viglet-dumont >/dev/null

# The SN site is converged on every start; a key is minted only while the plugin has none.
key=$(wp option pluck viglet_dumont_settings api_key 2>/dev/null | tail -n 1 || true)
if [ -z "$key" ]; then
	key=$(pwsh -NoProfile -File bin/turing-setup.ps1 -Base "http://localhost:$TURING_PORT" \
		-Password "$TURING_ADMIN_PASSWORD" -Site "$SN_SITE" -Locale "$TURING_LOCALE")
	if [ -z "$key" ]; then
		echo 'Turing was not provisioned; the plugin has no API key.' >&2
		exit 1
	fi
	wp option patch update viglet_dumont_settings api_key "$key" >/dev/null
	echo 'The plugin has an API key for the local Turing.'
else
	pwsh -NoProfile -File bin/turing-setup.ps1 -Base "http://localhost:$TURING_PORT" \
		-Password "$TURING_ADMIN_PASSWORD" -Site "$SN_SITE" -Locale "$TURING_LOCALE" -NoToken
fi

echo "WordPress:  http://localhost:$WP_PORT/wp-admin   (admin / admin)"
echo "Turing:     http://localhost:$TURING_PORT   (admin / $TURING_ADMIN_PASSWORD), SN site $SN_SITE"

if [ "${1:-}" = fresh ]; then
	# A WordPress the plugin was never installed on (D281), as run-docker.cmd's :fresh: seeded
	# once by WP-CLI with src/ mounted, then that copy deactivated and all it stored deleted.
	wpf() {
		docker compose run --rm --no-deps cli-fresh wp "$@"
	}
	docker compose --profile fresh up -d wordpress-fresh cron-fresh
	echo 'Waiting for the fresh WordPress...'
	tries=0
	until wpf db check >/dev/null 2>&1; do
		tries=$((tries + 1))
		if [ "$tries" -ge 60 ]; then
			echo 'The fresh WordPress did not come up. See: docker compose logs wordpress-fresh' >&2
			exit 1
		fi
		sleep 2
	done
	if ! wpf core is-installed >/dev/null 2>&1; then
		echo 'Installing the fresh WordPress and its catalog...'
		wpf core install --url="http://localhost:$WP_FRESH_PORT" --title='Tarnfell' \
			--admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email
		wpf theme activate tarnfell
		wpf rewrite structure '/%postname%/' --hard
		wpf plugin install woocommerce --activate
		wpf plugin activate viglet-dumont
		wpf eval-file /demo/seed/catalog.php
		wpf plugin deactivate viglet-dumont
		wpf eval-file /demo/seed/forget-plugin.php
		wpf rewrite flush --hard
	fi
	echo "Fresh store: http://localhost:$WP_FRESH_PORT/wp-admin   (admin / admin), no plugin installed"
	exit 0
fi

if [ "${1:-}" != demo ]; then
	exit 0
fi

# The demo storefronts (D248, D283), step for step as run-docker.cmd's :demo: each a WordPress
# and an SN site of its own beside the browser tests' site, sharing its Turing. `demo` sets up
# both, `demo en` or `demo pt` one.
wpd() {
	docker compose run --rm --no-deps "cli-demo$sfx" wp "$@"
}

setup_demo() {
	demo_store "$1"
	demo_url="http://localhost:$port"
	docker compose --profile demo up -d "wordpress-demo$sfx" "cron-demo$sfx"

	echo "Waiting for the demo WordPress ($locale)..."
	tries=0
	until wpd db check >/dev/null 2>&1; do
		tries=$((tries + 1))
		if [ "$tries" -ge 60 ]; then
			echo "The demo WordPress did not come up. See: docker compose logs wordpress-demo$sfx" >&2
			exit 1
		fi
		sleep 2
	done

	if ! wpd core is-installed >/dev/null 2>&1; then
		echo 'Installing the demo WordPress...'
		wpd core install --url="$demo_url" --title='Tarnfell' \
			--admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email
	fi
	wpd plugin activate viglet-dumont >/dev/null 2>&1 || true

	demo_key=$(wpd option pluck viglet_dumont_settings api_key 2>/dev/null | tail -n 1 || true)
	if [ -z "$demo_key" ]; then
		demo_key=$(pwsh -NoProfile -File bin/turing-setup.ps1 -Base "http://localhost:$TURING_PORT" \
			-Password "$TURING_ADMIN_PASSWORD" -Site "$site" -Locale "$locale")
	else
		pwsh -NoProfile -File bin/turing-setup.ps1 -Base "http://localhost:$TURING_PORT" \
			-Password "$TURING_ADMIN_PASSWORD" -Site "$site" -Locale "$locale" -NoToken
	fi
	if [ -z "$demo_key" ]; then
		echo "Turing was not provisioned for $site; the demo has no API key." >&2
		exit 1
	fi
	# The key, the SN site and the language it indexes and searches in (D272, D274), as the
	# site's own settings.
	wpd eval "\$s = get_option( 'viglet_dumont_settings', array() ); \$s['api_key'] = '$demo_key'; \$s['site_name'] = '$site'; \$s['locale'] = '$locale'; update_option( 'viglet_dumont_settings', \$s );"

	wpd theme activate tarnfell
	# The store speaks its language, and the seed writes the catalog in it (D217).
	if [ "$locale" != en_US ]; then
		wpd language core install "$locale" >/dev/null 2>&1 || true
	fi
	wpd site switch-language "$locale"
	wpd rewrite structure '/%postname%/' --hard
	# A shop: WooCommerce on, with the demo's catalog (demo/seed), which runs demo/setup.php first.
	wpd plugin is-installed woocommerce >/dev/null 2>&1 || wpd plugin install woocommerce
	wpd plugin activate woocommerce
	if [ "$locale" != en_US ]; then
		wpd language plugin install woocommerce "$locale" >/dev/null 2>&1 || true
	fi
	wpd eval-file /demo/seed/catalog.php
	# The seed sets the shop's addresses in its language (D266), after WooCommerce registered them.
	wpd rewrite flush --hard
	# The seed makes products indexable after saving them, so nothing queued them: send them now.
	wpd dumont reindex
	# The store's search history, once the catalog it finds is indexed (D269).
	wpd eval-file /demo/seed/searches.php

	echo "Demo storefront ($locale): $demo_url/   (admin / admin), SN site $site"
}

for store in ${2:-en pt}; do
	setup_demo "$store"
done
