#!/usr/bin/env bash
set -Eeuo pipefail

release_archive=''

usage() {
    cat <<'EOF'
Usage: update.sh --release /path/to/qfc-release.tar.gz

Installs a new QFC application release without running database initialization.
The script switches /opt/qfc/current atomically and restores the previous
release if the backend health endpoint does not become ready.
EOF
}

die() {
    echo "ERROR: $*" >&2
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --release)
            [[ $# -ge 2 ]] || die '--release requires an archive path.'
            release_archive="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            die "Unknown argument: $1"
            ;;
    esac
done

[[ "$(id -u)" -eq 0 ]] || die 'Run this script as root.'
[[ -n "$release_archive" ]] || die '--release is required.'
[[ -f "$release_archive" ]] || die "Release archive does not exist: $release_archive"
[[ -f /etc/qfc/qfc.env ]] || die 'Missing /etc/qfc/qfc.env. Run bootstrap.sh first.'
[[ -f /etc/systemd/system/qfc.service ]] || die 'Missing qfc.service. Run bootstrap.sh first.'

release_base=/home/qfc-releases
install -d -m 0755 -o root -g root "$release_base"
incoming_root=$(mktemp -d "$release_base/.incoming.XXXXXX")
cleanup() {
    if [[ -d "$incoming_root" ]]; then
        rm -rf "$incoming_root"
    fi
}
trap cleanup EXIT

tar -xzf "$release_archive" --no-same-owner -C "$incoming_root"
release_root=$(find "$incoming_root" -mindepth 1 -maxdepth 1 -type d -name 'qfc-*' -print -quit)
release_count=$(find "$incoming_root" -mindepth 1 -maxdepth 1 -type d -name 'qfc-*' -print | wc -l)
[[ "$release_count" -eq 1 ]] || die 'Release archive must contain exactly one qfc-* top-level directory.'

[[ -f "$release_root/MANIFEST.sha256" ]] || die 'Release archive does not contain MANIFEST.sha256.'
(
    cd "$release_root"
    sha256sum -c MANIFEST.sha256
)

for required_path in server/qfc-server.jar web/index.html admin-web/index.html db/schema.sql db/data.sql RELEASE_VERSION; do
    [[ -f "$release_root/$required_path" ]] || die "Release archive is missing $required_path."
done

version=$(tr -d '\r\n' < "$release_root/RELEASE_VERSION")
[[ "$version" =~ ^[0-9]{8}T[0-9]{6}Z-[0-9a-f]+(-dirty)?$ ]] || die "Unexpected release version: $version"
target_release="$release_base/qfc-$version"
[[ ! -e "$target_release" ]] || die "Release already exists: $target_release"

mv "$release_root" "$target_release"
rmdir "$incoming_root"
incoming_root=''
chown -R root:root "$target_release"
find "$target_release" -type d -exec chmod 0755 {} +
find "$target_release" -type f -exec chmod 0644 {} +

previous_release=''
if [[ -L /opt/qfc/current ]]; then
    previous_release=$(readlink -f /opt/qfc/current)
fi

rollback_release() {
    echo "New release $version did not pass the health check; restoring the previous release." >&2
    if [[ -n "$previous_release" && -d "$previous_release" ]]; then
        rollback_link="/opt/qfc/.rollback-$version"
        ln -s "$previous_release" "$rollback_link"
        mv -Tf "$rollback_link" /opt/qfc/current
        systemctl restart qfc || true
    else
        systemctl stop qfc || true
    fi
}

fail_after_switch() {
    local message="$1"
    echo "$message" >&2
    set +e
    rollback_release
    systemctl --no-pager --full status qfc
    exit 1
}

new_link="/opt/qfc/.current-$version"
ln -s "$target_release" "$new_link"
mv -Tf "$new_link" /opt/qfc/current

if ! systemctl restart qfc; then
    fail_after_switch "New release $version could not be started."
fi

healthy=false
for attempt in $(seq 1 30); do
    if systemctl is-active --quiet qfc && curl --connect-timeout 2 --max-time 5 --fail --silent --show-error http://127.0.0.1:8081/api/public/projects >/dev/null; then
        healthy=true
        break
    fi
    sleep 2
done

if [[ "$healthy" != true ]]; then
    fail_after_switch "New release $version did not pass the health check."
fi

echo "QFC release $version is active."
