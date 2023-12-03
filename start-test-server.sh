#!/usr/bin/env sh

set -e

./gradlew build

mkdir -p test-server-data/plugins
cp ./build/libs/wallubot-spigot*.jar test-server-data/plugins/wallubot-spigot.jar

docker compose -p wallubot-spigot -f test.docker-compose.yml run --publish 127.0.0.1:25565:25565 mc