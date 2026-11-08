#!/usr/bin/env bash
set -euo pipefail

DATABASES=(mysql mongodb postgres sqlserver mariadb)

latest_common_version=""

for db in "${DATABASES[@]}"; do
    echo "Checking Debezium Quarkus extension: $db"

    metadata_url="https://repo.maven.apache.org/maven2/io/debezium/quarkus/debezium-quarkus-${db}/maven-metadata.xml"

    versions=$(curl --fail --silent --show-error --location "$metadata_url" |
        sed -nE 's#.*<version>([0-9]+\.[0-9]+\.[0-9]+\.Final)</version>.*#\1#p' |
        sort -V)

    latest=$(printf '%s\n' "$versions" | tail -n 1)

    if [[ -z "$latest" ]]; then
        echo "ERROR: No stable release found for $db" >&2
        exit 1
    fi

    echo "Latest stable version: $latest"

    if [[ -z "$latest_common_version" ]]; then
        latest_common_version="$latest"
    elif [[ "$latest" != "$latest_common_version" ]]; then
        echo "ERROR: Extensions do not share the same latest stable version." >&2
        echo "Expected: $latest_common_version, Found for $db: $latest" >&2
        exit 1
    fi
done
APPLY="${APPLY:-0}"

update_dependency() {
    local db="$1"
    local file="templates/capturing/${db}/capturing.java.qute"
    local pattern="^//DEPS io.debezium.quarkus:debezium-quarkus-${db}:"

    local count
    count=$(grep -Ec "$pattern" "$file" || true)

    if [[ "$count" -ne 1 ]]; then
        echo "ERROR: Expected exactly one dependency in $file; found $count" >&2
        return 1
    fi

    if grep -qE "${pattern}${latest_common_version}$" "$file"; then
        echo "Already up to date: $file"
        return 0
    fi

    if [[ "$APPLY" == "1" ]]; then
        sed -i -E \
            "s#(${pattern})[^[:space:]]+#\1${latest_common_version}#" "$file"
        echo "Updated: $file"
    else
        echo "Would update: $file"
    fi
}

update_docker_image() {
    local db="$1"
    local available="$2"
    local file="templates/capturing/${db}/docker-compose.yaml"
    local image="example-${db}"

    if [[ "$available" != "true" ]]; then
        echo "Skipping unavailable Docker image: $image"
        return 0
    fi

    local count
    count=$(grep -Ec "^[[:space:]]*image: quay.io/debezium/${image}:" "$file" || true)

    if [[ "$count" -ne 1 ]]; then
        echo "ERROR: Expected exactly one Docker image in $file; found $count" >&2
        return 1
    fi

    if grep -qE "^[[:space:]]*image: quay.io/debezium/${image}:${docker_tag}$" "$file"; then
        echo "Already up to date: $file"
        return 0
    fi

    if [[ "$APPLY" == "1" ]]; then
        sed -i -E \
            "s#(^[[:space:]]*image: quay.io/debezium/${image}:)[^[:space:]]+#\\1${docker_tag}#" "$file"
        echo "Updated: $file"
    else
        echo "Would update: $file"
    fi
}
IFS='.' read -r major minor _ <<< "$latest_common_version"
docker_tag="${major}.${minor}"
check_quay_tag() {
    local image="$1"
    local tag="$2"
    local api_url="https://quay.io/api/v1/repository/debezium/${image}/tag/?specificTag=${tag}&onlyActiveTags=true&limit=1"
    local response

    if ! response=$(curl --fail --silent --show-error --location "$api_url" 2>/dev/null); then
        echo "WARNING: Could not check Quay for ${image}:${tag}; skipping image update." >&2
        return 2
    fi

    if ! jq -e --arg tag "$tag" '.tags[]? | select(.name == $tag)' \
        <<< "$response" >/dev/null; then
        echo "WARNING: Quay tag ${image}:${tag} was not found; skipping image update." >&2
        return 1
    fi

    echo "Quay tag available: ${image}:${tag}"
    return 0
}
mysql_image_available=false
mariadb_image_available=false

if check_quay_tag "example-mysql" "$docker_tag"; then
    mysql_image_available=true
fi

if check_quay_tag "example-mariadb" "$docker_tag"; then
    mariadb_image_available=true
fi
echo
echo
echo "=== Preflight validation ==="

# Validate all five Maven dependency lines.
for db in "${DATABASES[@]}"; do
    file="templates/capturing/${db}/capturing.java.qute"

    if [[ ! -f "$file" ]]; then
        echo "ERROR: Missing file: $file" >&2
        exit 1
    fi

    count=$(grep -Ec "^//DEPS io.debezium.quarkus:debezium-quarkus-${db}:" "$file" || true)

    if [[ "$count" -ne 1 ]]; then
        echo "ERROR: Expected exactly one dependency in $file; found $count" >&2
        exit 1
    fi
done

# Validate both Docker image lines.
for db in mysql mariadb; do
    file="templates/capturing/${db}/docker-compose.yaml"

    if [[ ! -f "$file" ]]; then
        echo "ERROR: Missing file: $file" >&2
        exit 1
    fi

    count=$(grep -Ec "^[[:space:]]*image: quay.io/debezium/example-${db}:" "$file" || true)

    if [[ "$count" -ne 1 ]]; then
        echo "ERROR: Expected exactly one Docker image in $file; found $count" >&2
        exit 1
    fi
done

echo "Preflight passed: all seven target files are valid."
echo "=== Capturing template updates ==="

for db in "${DATABASES[@]}"; do
    update_dependency "$db"
done

update_docker_image "mysql" "$mysql_image_available"
update_docker_image "mariadb" "$mariadb_image_available"
echo
echo "=== Proposed updates (DRY RUN) ==="
echo "Maven dependency version: $latest_common_version"


if [[ "$mysql_image_available" == true ]]; then
    echo "MySQL Docker image tag:   $docker_tag"
else
    echo "MySQL Docker image:       skipped"
fi

if [[ "$mariadb_image_available" == true ]]; then
    echo "MariaDB Docker image tag: $docker_tag"
else
    echo "MariaDB Docker image:     skipped"
fi
if [[ "$APPLY" == "1" ]]; then
    echo "Template updates applied."
else
    echo "Dry run complete. No repository files modified."
fi
