# Wallubot spigot addon

<https://wallubot.com> is a support chatbot for automating customer support mainly on Discord.

This plugin allows you to connect your Minecraft server to Wallubot and provide support on any Spigot server (starting from 1.8.8 and up to the latest version).

## Installing the plugin

1. Download the latest version from [releases](https://github.com/toppev/wallubot-spigot/releases) (`Assets -> wallubot-spigot-x.y.z-SNAPSHOT-all.jar`)
2. Click "Watch" to be notified of new updates (optional).
3. Drop the jar file in your `plugins/` directory.
4. Start the server and configure the `plugins/WallubotSpigot/config.yml`.
    1. Get the API key from <https://panel.wallubot.com/addons>
    2. Set the API key in the config file and configure the rest of the settings.
    3. Save the config.yml
5. Restart the server or reload the configuration with `/wallu reload`.

## Development

### Getting started (developers)

Just clone this repository :)

- `./gradlew build` - Build the plugin
- `bash ./start-test-server.sh` - Build the plugin and start a test server on port 25565 (requires Docker)

### Building

Run `./gradlew build` and see `build/libs/` directory.  
The file includes all the dependencies (Kotlin Standard Library + Client generated from the Wallu's OpenAPI spec).

### Debugging

You can toggle debugging with `/wallu debug`. The plugin will log more information when the debugging mode is enabled.

## FAQ

### How does it work?
This addon works by sending messages to Wallu's servers where they are processed by Wallu's AI. The AI may decide to answer the message or not in which case the configured commands will be executed.

### Are my messages safe?
Wallu may stores messages that it has answered to (no other messages) and you can control how long the messages are stored in the Wallu's admin panel.
Wallu can use the messages to improve answers on your server and Wallu will never output previous messages from your Minecraft server - they may only be shown on the admin panel of your server.   
Read more: <https://wallubot.com/privacy>

### Is this free?
Wallu has a free plan. Check our other plans from <https://wallubot.com/plans>.

### Is Wallu open source? Can I host Wallu myself?
Wallu's AI is not open source, so it cannot be self-hosted. However, this addon is open source so feel free to customize it.
Also, Wallu offers the [developer API](https://wallubot.com/developers) for integrating Wallu to your own services or addons.