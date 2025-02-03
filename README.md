# FixLag Plugin Documentation

## Overview
FixLag is a Minecraft plugin designed to reduce server lag by removing specific entities that may cause performance issues. This plugin provides the `/fixlag` command, which removes predefined entities from the world and notifies all online players of the number and type of removed entities.

## Features
- Clears specific lag-causing entities:
  - Minecarts
  - Arrows
  - Snowballs
  - Ender Pearls
  - Primed TNT
  - Dropped Items
- Notifies all online players about the number and type of removed entities.
- Requires the `fixlag.clear` permission to execute the command.

## Installation
1. Download the FixLag plugin `.jar` file.
2. Place the `.jar` file into the `plugins` folder of your Minecraft server.
3. Restart or reload the server.

## Commands
### `/fixlag`
- **Description**: Removes specified entities from the world and notifies all players.
- **Permission**: `fixlag.clear`
- **Usage**: `/fixlag`

## Permissions
| Permission      | Description                                      | Default  |
|-----------------|--------------------------------------------------|----------|
| `fixlag.clear`  | Allows the player to execute `/fixlag`           | OP       |
| `fixlag.notify` | (Not required) All players receive notifications | Everyone |

## How It Works
1. The player with the `fixlag.clear` permission executes `/fixlag`.
2. The plugin scans the world and removes the specified entities.
3. The number of removed entities per type is counted.
4. A message is broadcasted to all online players showing how many of each type were removed.

## Example Notification
```
FixLag removed the following entities:
- 10 MINECART
- 5 ARROW
- 3 SNOWBALL
```

## Configuration
No configuration is required; the plugin works out of the box.
In fact, because of how bad I am at developing plugins, I can't even make a config file that has content, so I just dropped it.
