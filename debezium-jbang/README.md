# Debezium CLI (JBang)

The Debezium CLI manages the lifecycle of CDC pipelines. It has two subsystems:

* **Build** (`init`, `validate`, `build`, `push`): scaffolds a `dbz.yaml` descriptor and assembles a standalone Debezium Server ZIP or a daemonless OCI image.
* **Platform** (`catalog`, `connection`, `source`, `destination`, `transform`, `pipeline`, `switch`, `config`): talks to the REST API of a Debezium Management Platform.

The full command reference is in the [official documentation](https://debezium.io/documentation/reference/3.8/operations/debezium-cli.html).

## Prerequisites

* Java 21+
* [JBang](https://www.jbang.dev/documentation/jbang/latest/installation.html) 0.138.0 or later
* Optional: a container registry when using `debezium push`
* Optional: a running Debezium Management Platform when using the platform commands

## Install the released version

```shell
jbang app install debezium@debezium
debezium --version
```

Update to the latest release:

```shell
jbang app install --force debezium@debezium
```

## Run the CLI from this repository

Use this to try out changes made in this repository. The released `debezium` command is not affected.

### 1. Build the project

The JBang script resolves `debezium-jbang-core` from your local Maven repository, so the project must be installed first. Run this from the repository root, and repeat it after every code change:

```shell
./mvnw clean install -DskipITs -DskipTests
```

The build also regenerates `debezium-jbang/debezium-jbang-main/dist/DebeziumJBang.java`, the script used below.

### 2. Install a local command

Install the script as an app, so you can run it without typing `jbang`:

```shell
jbang app install --force --name debezium-dev ./debezium-jbang/debezium-jbang-main/dist/DebeziumJBang.java
```

`debezium-dev` is created in `~/.jbang/bin`, which `jbang app setup` adds to your `PATH` (open a new shell afterwards). The launcher references the `dist` script by path, so you only install it once. After a rebuild, `debezium-dev` runs the new code.

```shell
debezium-dev version
debezium-dev init --source postgres --sink kafka
```

* The script defaults to `debezium.jbang.version=CURRENT-SNAPSHOT`, which matches the version built from this repository. You only need `-Ddebezium.jbang.version=<version>` to run a different version.
* The name `debezium-dev` keeps it separate from the released `debezium` app. To replace the released app with the local build, use `--name debezium` instead.
* Remove it with `jbang app uninstall debezium-dev`.

### Alternative: a catalog alias

You can also register a JBang alias and call it as `jbang debezium-dev <args>`:

```shell
jbang alias add --name debezium-dev ./debezium-jbang/debezium-jbang-main/dist/DebeziumJBang.java
jbang debezium-dev version
```

### Alternative: run the script directly

```shell
./debezium-jbang/debezium-jbang-main/dist/DebeziumJBang.java version
```

## Quick start

```shell
# Build subsystem
debezium init --source postgres --sink kafka   # creates dbz.yaml
export DBZ_DB_USER=postgres DBZ_DB_PASS=secret
debezium validate
debezium build                                 # target/debezium-server-<version>.zip
debezium build --image                         # target/debezium-server.tar (no Docker daemon needed)
debezium push --registry ghcr.io/my-org

# Platform subsystem
debezium config set --env=dev platform-url https://dev.example.com
debezium switch dev
debezium catalog list
debezium pipeline list
```

The Platform URL is resolved from `--platform-address`, then the `DEBEZIUM_PLATFORM_URL` environment variable, then `~/.dbz/config.yaml`.
