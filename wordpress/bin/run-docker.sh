#!/bin/sh
# run-docker.cmd's start, for Linux and macOS (and the browser suite's CI job, D182): the
# admin screens built, WordPress and Turing up (docker-compose.yml next to this folder),
# WordPress installed with the plugin active, and the SN site provisioned with an API key.
#
#   sh bin/run-docker.sh              start both, install WordPress and provision Turing
#   sh bin/run-docker.sh demo         also the demo storefront on WP_DEMO_PORT (38082), as
#                                     run-docker.cmd demo does (D248)
#   sh bin/run-docker.sh demo reset   stop the demo and delete its WordPress and database
#
# The same environment as run-docker.cmd: WP_PORT (8080), TURING_PORT (2700), SN_SITE
# (Sample), TURING_LOCALE (en_US), TURING_ADMIN_PASSWORD (dumont-dev), TURING_IMAGE, and for
# the demo WP_DEMO_PORT (38082), SN_DEMO_SITE (Demo) and DEMO_LOCALE (en_US). OPENAI_API_KEY
# turns on Turing's generative AI. Provisioning runs bin/turing-setup.ps1, so it needs
# PowerShell (`pwsh`).
set -eu
cd "$(dirname "$0")/.."

: "${WP_DEMO_PORT:=38082}"
: "${SN_DEMO_SITE:=Demo}"
: "${DEMO_LOCALE:=en_US}"
export WP_DEMO_PORT

if [ "${1:-}" = demo ] && [ "${2:-}" = reset ]; then
	docker compose --profile demo rm -s -f wordpress-demo cron-demo db-demo
	docker volume rm viglet-dumont-wp_wp-demo viglet-dumont-wp_db-demo
	exit 0
fi

: "${WP_PORT:=8080}"
: "${TURING_PORT:=2700}"
: "${SN_SITE:=Sample}"
: "${TURING_LOCALE:=en_US}"
: "${TURING_ADMIN_PASSWORD:=dumont-dev}"
# A Turing built from a local checkout wins over the published image, as in run-docker.cmd.
if [ -z "${TURING_IMAGE:-}" ] && docker image inspect turing-ce:local >/dev/null 2>&1; then
	TURING_IMAGE=turing-ce:local
fi
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

if [ "${1:-}" != demo ]; then
	exit 0
fi

# The demo storefront (D248), step for step as run-docker.cmd's :demo: a WordPress and an SN
# site of its own beside the browser tests' site, sharing its Turing.
wpd() {
	docker compose run --rm --no-deps cli-demo wp "$@"
}
demo_url="http://localhost:$WP_DEMO_PORT"
docker compose --profile demo up -d

echo 'Waiting for the demo WordPress...'
tries=0
until wpd db check >/dev/null 2>&1; do
	tries=$((tries + 1))
	if [ "$tries" -ge 60 ]; then
		echo 'The demo WordPress did not come up. See: docker compose logs wordpress-demo' >&2
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
		-Password "$TURING_ADMIN_PASSWORD" -Site "$SN_DEMO_SITE" -Locale "$DEMO_LOCALE")
else
	pwsh -NoProfile -File bin/turing-setup.ps1 -Base "http://localhost:$TURING_PORT" \
		-Password "$TURING_ADMIN_PASSWORD" -Site "$SN_DEMO_SITE" -Locale "$DEMO_LOCALE" -NoToken
fi
if [ -z "$demo_key" ]; then
	echo "Turing was not provisioned for $SN_DEMO_SITE; the demo has no API key." >&2
	exit 1
fi
wpd eval "\$s = get_option( 'viglet_dumont_settings', array() ); \$s['api_key'] = '$demo_key'; update_option( 'viglet_dumont_settings', \$s );"

wpd theme activate tarnfell
# The site speaks DEMO_LOCALE, and the seed writes the catalog in it (D217).
if [ "$DEMO_LOCALE" != en_US ]; then
	wpd language core install "$DEMO_LOCALE" >/dev/null 2>&1 || true
fi
wpd site switch-language "$DEMO_LOCALE"
wpd rewrite structure '/%postname%/' --hard
# A shop: WooCommerce on, with the demo's catalog (demo/seed), which runs demo/setup.php first.
wpd plugin is-installed woocommerce >/dev/null 2>&1 || wpd plugin install woocommerce
wpd plugin activate woocommerce
if [ "$DEMO_LOCALE" != en_US ]; then
	wpd language plugin install woocommerce "$DEMO_LOCALE" >/dev/null 2>&1 || true
fi
wpd eval-file /demo/seed/catalog.php
# The seed makes products indexable after saving them, so nothing queued them: send them now.
wpd dumont reindex

echo "Demo storefront: $demo_url/   (admin / admin), SN site $SN_DEMO_SITE"
