#!/bin/sh
# run-docker.cmd's start, for Linux and macOS (and the browser suite's CI job, D182): the
# admin screens built, WordPress and Turing up (docker-compose.yml next to this folder),
# WordPress installed with the plugin active, and the SN site provisioned with an API key.
#
#   sh bin/run-docker.sh
#
# The same environment as run-docker.cmd: WP_PORT (8080), TURING_PORT (2700), SN_SITE
# (Sample), TURING_LOCALE (en_US), TURING_ADMIN_PASSWORD (dumont-dev), TURING_IMAGE.
# Provisioning runs bin/turing-setup.ps1, so it needs PowerShell (`pwsh`).
set -eu
cd "$(dirname "$0")/.."

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
