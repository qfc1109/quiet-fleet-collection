#!/usr/bin/env bash
set -Eeuo pipefail

release_archive=''
asset_dir=$(cd "$(dirname "$0")" && pwd)
initial_extract_root=''
qfc_docker_root='/home/qfc-docker'
qfc_release_root='/home/qfc-releases'
resume_after_runtime_install=false
resume_after_image_pull=false
resume_after_container_start=false

usage() {
    cat <<'EOF'
Usage: bootstrap.sh --release /path/to/qfc-release.tar.gz [--resume-after-runtime-install|--resume-after-image-pull|--resume-after-container-start]

Initializes a fresh QFC deployment. It installs Docker, OpenJDK 21 and Nginx,
runs MySQL 8 and Redis in Docker, imports a new database, and starts QFC.
The script refuses to overwrite existing Docker, QFC, container, or database
state that could belong to a previous deployment.

--resume-after-runtime-install is only for recovering from this script's own
failed package-installation phase. It still refuses any QFC state and verifies
that Docker has no containers, volumes, images, plugins, or custom networks.

--resume-after-image-pull is only for recovering from this script's own image
download failure before any QFC container, volume, or database was created.

--resume-after-container-start is only for recovering from this script's own
failure after the empty MySQL and Redis containers were created but before the
four QFC databases were initialized.
EOF
}

die() {
    echo "ERROR: $*" >&2
    exit 1
}

cleanup() {
    if [[ -n "$initial_extract_root" && -d "$initial_extract_root" ]]; then
        rm -rf "$initial_extract_root"
    fi
}
trap cleanup EXIT

while [[ $# -gt 0 ]]; do
    case "$1" in
        --release)
            [[ $# -ge 2 ]] || die '--release requires an archive path.'
            release_archive="$2"
            shift 2
            ;;
        --resume-after-runtime-install)
            resume_after_runtime_install=true
            shift
            ;;
        --resume-after-image-pull)
            resume_after_image_pull=true
            shift
            ;;
        --resume-after-container-start)
            resume_after_container_start=true
            shift
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
[[ -f "$asset_dir/update.sh" ]] || die "Missing update.sh in $asset_dir."
recovery_option_count=0
[[ "$resume_after_runtime_install" == true ]] && recovery_option_count=$((recovery_option_count + 1))
[[ "$resume_after_image_pull" == true ]] && recovery_option_count=$((recovery_option_count + 1))
[[ "$resume_after_container_start" == true ]] && recovery_option_count=$((recovery_option_count + 1))
(( recovery_option_count <= 1 )) ||
    die 'Choose only one recovery option.'

assert_fresh_host() {
    local path

    if [[ "$resume_after_runtime_install" == true ]]; then
        assert_runtime_install_only_state
        [[ ! -e /etc/docker/daemon.json ]] || die 'A Docker daemon configuration already exists; refusing a runtime-install recovery on an unknown Docker configuration.'
        [[ ! -e /etc/nginx/sites-available/qfc-main && ! -e /etc/nginx/sites-available/qfc-admin ]] ||
            die 'QFC Nginx configuration already exists; this is not a package-install-only recovery.'
    else
        for path in /etc/qfc /opt/qfc /home/qfc /home/qfc-docker /home/qfc-releases; do
            [[ ! -e "$path" ]] || die "Existing QFC path found: $path. Refusing to overwrite a prior or partial deployment."
        done
        id qfc >/dev/null 2>&1 && die 'The qfc system user already exists; refusing to reuse an unknown account.'

        if command -v docker >/dev/null 2>&1 \
            || [[ -e /etc/docker/daemon.json ]] \
            || [[ -d /var/lib/docker ]] \
            || [[ -S /run/docker.sock ]] \
            || dpkg-query -W -f='${db:Status-Status}' docker.io 2>/dev/null | grep -qx installed; then
            die 'Docker is already installed or has existing state; this fresh-deployment script will not change another Docker installation.'
        fi

        if command -v nginx >/dev/null 2>&1 \
            || [[ -d /etc/nginx ]] \
            || dpkg-query -W -f='${db:Status-Status}' nginx 2>/dev/null | grep -qx installed; then
            die 'Nginx is already installed or configured; refusing to modify an existing web server.'
        fi
    fi
}

assert_runtime_install_only_state() {
    if [[ ! -e /etc/qfc && ! -e /opt/qfc && ! -e /home/qfc && ! -e /home/qfc-docker && ! -e /home/qfc-releases ]] \
        && ! id qfc >/dev/null 2>&1; then
        return
    fi

    [[ -d /etc/qfc && -d /opt/qfc && -d /opt/qfc/bin && -d /home/qfc && -d /home/qfc/storage && -d /home/qfc/logs && -d /home/qfc-releases ]] ||
        die 'The existing QFC paths are not the known package-install-only recovery state.'
    id qfc >/dev/null 2>&1 || die 'The expected qfc system user is missing from the package-install-only recovery state.'
    [[ ! -e /home/qfc-docker && ! -e /etc/qfc/qfc.env && ! -e /etc/qfc/mysql-root.env && ! -e /etc/systemd/system/qfc.service && ! -e /opt/qfc/current ]] ||
        die 'QFC configuration or a Docker data root already exists; this is not a package-install-only recovery.'
    [[ -z "$(find /etc/qfc -mindepth 1 -print -quit)" ]] || die 'The QFC secret directory is not empty; refusing recovery.'
    [[ -z "$(find /opt/qfc/bin -mindepth 1 -print -quit)" ]] || die 'The QFC binary directory is not empty; refusing recovery.'
    [[ -z "$(find /home/qfc-releases -mindepth 1 -print -quit)" ]] || die 'The QFC release directory is not empty; refusing recovery.'
    [[ -z "$(find /home/qfc/storage /home/qfc/logs -mindepth 1 -print -quit)" ]] ||
        die 'QFC storage or logs are not empty; refusing recovery.'
}

assert_image_pull_only_state() {
    [[ -d /etc/qfc && -d /opt/qfc && -d /opt/qfc/bin && -d /home/qfc && -d /home/qfc/storage && -d /home/qfc/logs && -d /home/qfc-releases && -d "$qfc_docker_root" ]] ||
        die 'The existing paths are not the known image-pull-only recovery state.'
    id qfc >/dev/null 2>&1 || die 'The expected qfc system user is missing from the image-pull-only recovery state.'
    [[ -f /etc/qfc/qfc.env && -f /etc/qfc/mysql-root.env && -f /etc/qfc/application.yml && -f /etc/qfc/logback-spring.xml && -f /etc/systemd/system/qfc.service && -f /opt/qfc/bin/update.sh ]] ||
        die 'The required QFC runtime files are missing from the image-pull-only recovery state.'
    [[ ! -e /opt/qfc/current && ! -L /opt/qfc/current && -e /etc/nginx/sites-enabled/qfc-main && -e /etc/nginx/sites-enabled/qfc-admin ]] ||
        die 'The existing release or Nginx configuration is not the known image-pull-only recovery state.'
    docker info --format '{{.DockerRootDir}}' | grep -Fx "$qfc_docker_root" >/dev/null ||
        die 'Docker is not using the expected QFC data root; refusing image-pull recovery.'
    assert_no_docker_runtime_state
    assert_only_qfc_image_candidates
    [[ -z "$(find /home/qfc-releases -mindepth 1 -print -quit)" ]] || die 'The QFC release directory is not empty; refusing recovery.'
    [[ -z "$(find /home/qfc/storage /home/qfc/logs -mindepth 1 -print -quit)" ]] ||
        die 'QFC storage or logs are not empty; refusing recovery.'
}

assert_container_start_only_state() {
    assert_image_pull_only_base_state
    [[ -e /opt/qfc/current || -L /opt/qfc/current ]] && die 'A QFC release is already active; refusing container-start recovery.'
    [[ -z "$(find /home/qfc-releases -mindepth 1 -print -quit)" ]] || die 'The QFC release directory is not empty; refusing recovery.'
    [[ -z "$(find /home/qfc/storage /home/qfc/logs -mindepth 1 -print -quit)" ]] ||
        die 'QFC storage or logs are not empty; refusing recovery.'

    docker container inspect qfc-mysql >/dev/null 2>&1 || die 'Missing qfc-mysql container for container-start recovery.'
    docker container inspect qfc-redis >/dev/null 2>&1 || die 'Missing qfc-redis container for container-start recovery.'
    docker volume inspect qfc-mysql-data >/dev/null 2>&1 || die 'Missing qfc-mysql-data volume for container-start recovery.'
    docker volume inspect qfc-redis-data >/dev/null 2>&1 || die 'Missing qfc-redis-data volume for container-start recovery.'

    unexpected_containers=$(docker container ls -a --format '{{.Names}}' | grep -Ev '^(qfc-mysql|qfc-redis)$' || true)
    unexpected_volumes=$(docker volume ls -q | grep -Ev '^(qfc-mysql-data|qfc-redis-data)$' || true)
    [[ -z "$unexpected_containers" && -z "$unexpected_volumes" ]] ||
        die 'Unexpected Docker containers or volumes found; refusing container-start recovery.'

    wait_for_mysql
    existing_databases=$(run_mysql -Nse "SELECT COALESCE(GROUP_CONCAT(SCHEMA_NAME ORDER BY SCHEMA_NAME SEPARATOR ','), '') FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME IN ('qfc_site', 'qfc_site_log', 'qfc_admin', 'qfc_admin_log');")
    [[ -z "$existing_databases" ]] || die "Existing QFC database(s) found: $existing_databases. Refusing container-start recovery."
}

assert_image_pull_only_base_state() {
    [[ -d /etc/qfc && -d /opt/qfc && -d /opt/qfc/bin && -d /home/qfc && -d /home/qfc/storage && -d /home/qfc/logs && -d /home/qfc-releases && -d "$qfc_docker_root" ]] ||
        die 'The existing paths are not the known QFC recovery state.'
    id qfc >/dev/null 2>&1 || die 'The expected qfc system user is missing from the QFC recovery state.'
    [[ -f /etc/qfc/qfc.env && -f /etc/qfc/mysql-root.env && -f /etc/qfc/application.yml && -f /etc/qfc/logback-spring.xml && -f /etc/systemd/system/qfc.service && -f /opt/qfc/bin/update.sh ]] ||
        die 'The required QFC runtime files are missing from the QFC recovery state.'
    [[ -e /etc/nginx/sites-enabled/qfc-main && -e /etc/nginx/sites-enabled/qfc-admin ]] ||
        die 'The expected QFC Nginx configuration is missing from the QFC recovery state.'
    docker info --format '{{.DockerRootDir}}' | grep -Fx "$qfc_docker_root" >/dev/null ||
        die 'Docker is not using the expected QFC data root; refusing recovery.'
}

assert_supported_os() {
    [[ -r /etc/os-release ]] || die 'Unable to identify the operating system.'
    # shellcheck disable=SC1091
    . /etc/os-release
    [[ "${ID:-}" == debian && "${VERSION_ID:-}" == 13 ]] ||
        die "This deployment script requires Debian 13; found ${PRETTY_NAME:-an unknown operating system}."
}

require_free_kib() {
    local path="$1"
    local minimum_kib="$2"
    local available_kib
    available_kib=$(df -Pk "$path" | awk 'NR == 2 { print $4 }')
    [[ -n "$available_kib" ]] || die "Unable to determine free space for $path."
    if (( available_kib < minimum_kib )); then
        die "Insufficient free space on $path: need at least $minimum_kib KiB, found $available_kib KiB."
    fi
}

generate_secret() {
    openssl rand -hex "$1"
}

read_qfc_env() {
    local key="$1"
    grep -m 1 "^$key=" /etc/qfc/qfc.env | cut -d '=' -f 2-
}

read_mysql_root_env() {
    local key="$1"
    grep -m 1 "^$key=" /etc/qfc/mysql-root.env | cut -d '=' -f 2-
}

mysql_root_password() {
    read_mysql_root_env QFC_MYSQL_ROOT_PASSWORD
}

run_mysql() {
    local password
    password=$(mysql_root_password)
    docker exec -i -e MYSQL_PWD="$password" qfc-mysql mysql -uroot --default-character-set=utf8mb4 "$@"
}

wait_for_mysql() {
    for attempt in $(seq 1 60); do
        if run_mysql -Nse 'SELECT 1' >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
    done
    die 'MySQL 8 did not become ready within 120 seconds.'
}

verify_initial_release() {
    initial_extract_root=$(mktemp -d /tmp/qfc-initial-release.XXXXXX)
    tar -xzf "$release_archive" --no-same-owner -C "$initial_extract_root"

    initial_release_root=$(find "$initial_extract_root" -mindepth 1 -maxdepth 1 -type d -name 'qfc-*' -print -quit)
    release_count=$(find "$initial_extract_root" -mindepth 1 -maxdepth 1 -type d -name 'qfc-*' -print | wc -l)
    [[ "$release_count" -eq 1 ]] || die 'Release archive must contain exactly one qfc-* top-level directory.'
    [[ -f "$initial_release_root/MANIFEST.sha256" ]] || die 'Release archive does not contain MANIFEST.sha256.'
    (
        cd "$initial_release_root"
        sha256sum -c MANIFEST.sha256
    )
    for required_path in db/schema.sql db/data.sql server/qfc-server.jar web/index.html admin-web/index.html; do
        [[ -f "$initial_release_root/$required_path" ]] || die "Release archive is missing $required_path."
    done
}

install_runtime_packages() {
    local packages=(docker.io docker-cli openjdk-21-jre-headless nginx curl ca-certificates openssl apache2-utils)
    local fallback_sources

    require_free_kib / 1048576
    require_free_kib /var 524288
    require_free_kib /home 5242880

    export DEBIAN_FRONTEND=noninteractive
    if ! apt-get update || ! apt-get install -y --no-install-recommends "${packages[@]}"; then
        echo 'The configured APT source failed; retrying only this installation with official Debian HTTPS sources.' >&2
        fallback_sources=$(mktemp /tmp/qfc-debian-sources.XXXXXX)
        cat > "$fallback_sources" <<'EOF'
deb https://deb.debian.org/debian trixie main
deb https://deb.debian.org/debian trixie-updates main
deb https://security.debian.org/debian-security trixie-security main
EOF
        if ! apt-get -o Dir::Etc::sourcelist="$fallback_sources" -o Dir::Etc::sourceparts='-' update \
            || ! apt-get -o Dir::Etc::sourcelist="$fallback_sources" -o Dir::Etc::sourceparts='-' install -y --no-install-recommends "${packages[@]}"; then
            rm -f "$fallback_sources"
            die 'Unable to install required runtime packages from the configured or official Debian sources.'
        fi
        rm -f "$fallback_sources"
    fi
    apt-get clean
}

assert_empty_docker_state() {
    systemctl enable --now docker
    for attempt in $(seq 1 30); do
        if docker info >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    docker info >/dev/null 2>&1 || die 'Docker did not become available within 30 seconds after installation.'

    assert_no_docker_runtime_state
    if docker image ls -aq | grep -q .; then
        die 'Docker has existing images; refusing to alter its data root.'
    fi
}

assert_no_docker_runtime_state() {
    if docker container ls -aq | grep -q . \
        || docker volume ls -q | grep -q . \
        || docker plugin ls -q | grep -q . \
        || docker network ls --format '{{.Name}}' | grep -Ev '^(bridge|host|none)$' | grep -q .; then
        die 'Docker has existing containers, volumes, plugins, or custom networks; refusing to alter or recover state.'
    fi
}

assert_only_qfc_image_candidates() {
    local image
    while IFS= read -r image; do
        case "$image" in
            mysql:8.0|redis:7-alpine|docker.m.daocloud.io/library/mysql:8.0|docker.m.daocloud.io/library/redis:7-alpine|dockerproxy.net/library/mysql:8.0|dockerproxy.net/library/redis:7-alpine)
                ;;
            *)
                die "Unexpected Docker image found: $image. Refusing image-pull recovery."
                ;;
        esac
    done < <(docker image ls --format '{{.Repository}}:{{.Tag}}' | sort -u)
}

configure_docker_data_root() {
    local docker_config=/etc/docker/daemon.json
    local expected_root="$qfc_docker_root"

    assert_empty_docker_state
    install -d -m 0700 "$expected_root"
    if [[ -e "$docker_config" ]]; then
        grep -F "\"data-root\": \"$expected_root\"" "$docker_config" >/dev/null ||
            die "Existing $docker_config does not use $expected_root; refusing to overwrite Docker configuration."
    else
        install -d -m 0755 /etc/docker
        cat > "$docker_config" <<EOF
{
  "data-root": "$expected_root"
}
EOF
    fi

    systemctl restart docker
    docker info --format '{{.DockerRootDir}}' | grep -Fx "$expected_root" >/dev/null ||
        die 'Docker did not start with the expected data-root.'
}

create_qfc_runtime_paths() {
    if ! id qfc >/dev/null 2>&1; then
        useradd --system --home /home/qfc --create-home --shell /usr/sbin/nologin qfc
    fi
    install -d -m 0755 /opt/qfc /opt/qfc/bin
    install -d -m 0755 -o root -g root "$qfc_release_root"
    install -d -m 0750 -o qfc -g qfc /home/qfc /home/qfc/storage /home/qfc/logs
    # qfc must read the non-secret application and Logback configuration; the
    # two secret env files themselves remain root-owned with mode 0600.
    install -d -m 0751 -o root -g qfc /etc/qfc
}

create_qfc_environment() {
    [[ ! -e /etc/qfc/qfc.env ]] || die 'Existing /etc/qfc/qfc.env found; refusing to overwrite deployment secrets.'

    local mysql_root
    local mysql_app
    local redis_password
    local jwt_secret
    mysql_root=$(generate_secret 32)
    mysql_app=$(generate_secret 32)
    redis_password=$(generate_secret 32)
    jwt_secret=$(generate_secret 48)

    umask 077
    cat > /etc/qfc/qfc.env <<EOF
QFC_MYSQL_HOST=127.0.0.1
QFC_MYSQL_PORT=3306
QFC_MYSQL_USERNAME=qfc_app
QFC_MYSQL_PASSWORD=$mysql_app
QFC_MYSQL_IMAGE=mysql:8.0
QFC_REDIS_HOST=127.0.0.1
QFC_REDIS_PORT=6379
QFC_REDIS_PASSWORD=$redis_password
QFC_REDIS_IMAGE=redis:7-alpine
QFC_JWT_SECRET=$jwt_secret
EOF
    chmod 0600 /etc/qfc/qfc.env

    cat > /etc/qfc/mysql-root.env <<EOF
QFC_MYSQL_ROOT_PASSWORD=$mysql_root
EOF
    chmod 0600 /etc/qfc/mysql-root.env
}

install_server_assets() {
    install -m 0755 "$asset_dir/update.sh" /opt/qfc/bin/update.sh
    install -m 0644 "$asset_dir/qfc.service" /etc/systemd/system/qfc.service
    install -m 0644 "$asset_dir/application.yml" /etc/qfc/application.yml
    install -m 0644 "$asset_dir/logback-spring.xml" /etc/qfc/logback-spring.xml
    install -m 0644 "$asset_dir/nginx-qfc-main.conf" /etc/nginx/sites-available/qfc-main
    install -m 0644 "$asset_dir/nginx-qfc-admin.conf" /etc/nginx/sites-available/qfc-admin
    ln -sfn /etc/nginx/sites-available/qfc-main /etc/nginx/sites-enabled/qfc-main
    ln -sfn /etc/nginx/sites-available/qfc-admin /etc/nginx/sites-enabled/qfc-admin

    if [[ -L /etc/nginx/sites-enabled/default ]]; then
        default_target=$(readlink -f /etc/nginx/sites-enabled/default)
        if [[ "$default_target" == /etc/nginx/sites-available/default ]]; then
            rm -f /etc/nginx/sites-enabled/default
        fi
    fi

    systemctl daemon-reload
    systemctl enable qfc
}

start_data_containers() {
    local mysql_image
    local redis_image
    local mysql_password
    local redis_password
    mysql_image=$(read_qfc_env QFC_MYSQL_IMAGE)
    redis_image=$(read_qfc_env QFC_REDIS_IMAGE)
    mysql_password=$(mysql_root_password)
    redis_password=$(read_qfc_env QFC_REDIS_PASSWORD)

    docker container inspect qfc-mysql >/dev/null 2>&1 &&
        die 'Container qfc-mysql already exists; refusing to replace database state.'
    docker container inspect qfc-redis >/dev/null 2>&1 &&
        die 'Container qfc-redis already exists; refusing to replace Redis state.'
    docker volume inspect qfc-mysql-data >/dev/null 2>&1 &&
        die 'Volume qfc-mysql-data already exists; refusing to reuse database data.'
    docker volume inspect qfc-redis-data >/dev/null 2>&1 &&
        die 'Volume qfc-redis-data already exists; refusing to reuse Redis data.'

    mysql_image=$(pull_qfc_image QFC_MYSQL_IMAGE "$mysql_image" \
        docker.m.daocloud.io/library/mysql:8.0 \
        dockerproxy.net/library/mysql:8.0)
    redis_image=$(pull_qfc_image QFC_REDIS_IMAGE "$redis_image" \
        docker.m.daocloud.io/library/redis:7-alpine \
        dockerproxy.net/library/redis:7-alpine)
    docker volume create qfc-mysql-data >/dev/null
    docker volume create qfc-redis-data >/dev/null

    docker run -d --name qfc-mysql --restart unless-stopped \
        -p 127.0.0.1:3306:3306 \
        -e MYSQL_ROOT_PASSWORD="$mysql_password" \
        -v qfc-mysql-data:/var/lib/mysql \
        "$mysql_image" \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_ci

    docker run -d --name qfc-redis --restart unless-stopped \
        -p 127.0.0.1:6379:6379 \
        -v qfc-redis-data:/data \
        "$redis_image" \
        redis-server --appendonly yes --dir /data --requirepass "$redis_password" --maxmemory 128mb --maxmemory-policy noeviction
}

pull_qfc_image() {
    local environment_key="$1"
    local primary_image="$2"
    shift 2
    local fallback_image

    if docker pull "$primary_image" >&2; then
        printf '%s' "$primary_image"
        return
    fi

    for fallback_image in "$@"; do
        echo "Unable to pull $primary_image; trying the scoped mirror image $fallback_image." >&2
        if docker pull "$fallback_image" >&2; then
            sed -i "s|^${environment_key}=.*|${environment_key}=${fallback_image}|" /etc/qfc/qfc.env
            printf '%s' "$fallback_image"
            return
        fi
    done

    die "Unable to pull $primary_image or its configured fallback mirrors."
}

initialize_database() {
    local existing_databases
    local app_password
    existing_databases=$(run_mysql -Nse "SELECT COALESCE(GROUP_CONCAT(SCHEMA_NAME ORDER BY SCHEMA_NAME SEPARATOR ','), '') FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME IN ('qfc_site', 'qfc_site_log', 'qfc_admin', 'qfc_admin_log');")
    [[ -z "$existing_databases" ]] || die "Existing QFC database(s) found: $existing_databases. Refusing to initialize mixed or existing data."

    docker exec -i -e MYSQL_PWD="$(mysql_root_password)" qfc-mysql mysql -uroot --default-character-set=utf8mb4 < "$initial_release_root/db/schema.sql"
    docker exec -i -e MYSQL_PWD="$(mysql_root_password)" qfc-mysql mysql -uroot --default-character-set=utf8mb4 < "$initial_release_root/db/data.sql"

    app_password=$(read_qfc_env QFC_MYSQL_PASSWORD)
    run_mysql <<EOF
CREATE USER IF NOT EXISTS 'qfc_app'@'%' IDENTIFIED BY '$app_password';
GRANT ALL PRIVILEGES ON qfc_site.* TO 'qfc_app'@'%';
GRANT ALL PRIVILEGES ON qfc_site_log.* TO 'qfc_app'@'%';
GRANT ALL PRIVILEGES ON qfc_admin.* TO 'qfc_app'@'%';
GRANT ALL PRIVILEGES ON qfc_admin_log.* TO 'qfc_app'@'%';
FLUSH PRIVILEGES;
EOF

    database_count=$(run_mysql -Nse "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME IN ('qfc_site', 'qfc_site_log', 'qfc_admin', 'qfc_admin_log');")
    [[ "$database_count" == 4 ]] || die "Expected four QFC databases after initialization, found $database_count."
}

set_initial_admin_password() {
    local initial_password
    local password_hash
    local site_admin_count
    local admin_admin_count

    site_admin_count=$(run_mysql -Nse "SELECT COUNT(*) FROM qfc_site.site_user WHERE username = 'admin';")
    admin_admin_count=$(run_mysql -Nse "SELECT COUNT(*) FROM qfc_admin.admin_user WHERE username = 'admin';")
    [[ "$site_admin_count" == 1 && "$admin_admin_count" == 1 ]] || die 'The imported seed data does not contain exactly one site and one admin account named admin.'

    initial_password=$(generate_secret 18)
    password_hash=$(printf '%s\n' "$initial_password" | htpasswd -n -i -B -C 10 admin | cut -d ':' -f 2-)
    [[ "$password_hash" == '$2a$10$'* || "$password_hash" == '$2b$10$'* || "$password_hash" == '$2y$10$'* ]] ||
        die 'Unable to generate a valid BCrypt password hash for the initial administrator.'

    run_mysql <<EOF
UPDATE qfc_site.site_user
SET password_hash = '$password_hash', updated_at = NOW()
WHERE username = 'admin';
UPDATE qfc_admin.admin_user
SET password_hash = '$password_hash', updated_at = NOW()
WHERE username = 'admin';
EOF

    umask 077
    cat > /root/qfc-initial-admin-credentials.txt <<EOF
QFC 初始管理员凭据（首次部署时生成）

主站与后台账号：admin
密码：$initial_password

请首次登录后立即修改密码；确认保存后可删除此文件。
EOF
    chmod 0600 /root/qfc-initial-admin-credentials.txt
    echo 'A random initial admin password was written only to /root/qfc-initial-admin-credentials.txt (mode 0600).'
}

verify_services() {
    nginx -t
    systemctl enable --now nginx
    systemctl reload nginx
    systemctl is-active --quiet docker
    systemctl is-active --quiet nginx
    systemctl is-active --quiet qfc
    docker ps --format '{{.Names}}' | grep -Fx qfc-mysql >/dev/null
    docker ps --format '{{.Names}}' | grep -Fx qfc-redis >/dev/null
    curl --connect-timeout 2 --max-time 10 --fail --silent --show-error http://127.0.0.1:8081/api/public/projects >/dev/null
    curl --connect-timeout 2 --max-time 10 --fail --silent --show-error http://127.0.0.1/ >/dev/null
    curl --connect-timeout 2 --max-time 10 --fail --silent --show-error http://127.0.0.1:8082/login >/dev/null
    echo 'QFC initial deployment completed successfully.'
}

assert_supported_os
if [[ "$resume_after_image_pull" == true ]]; then
    assert_image_pull_only_state
    verify_initial_release
elif [[ "$resume_after_container_start" == true ]]; then
    assert_container_start_only_state
    verify_initial_release
else
    assert_fresh_host
    verify_initial_release
    install_runtime_packages
    configure_docker_data_root
    create_qfc_runtime_paths
    create_qfc_environment
    install_server_assets
fi
if [[ "$resume_after_container_start" != true ]]; then
    start_data_containers
fi
wait_for_mysql
initialize_database
set_initial_admin_password
/opt/qfc/bin/update.sh --release "$release_archive"
verify_services
