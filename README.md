# 🛠️ FixLag - Your Minecraft Server Performance Booster! 🚀

Tired of lag ruining your players' experience? FixLag is here to help! This plugin automatically removes unnecessary entities from your Minecraft server at regular intervals, helping to reduce lag and improve overall performance.

## ✨ Features



* **Automatic Entity Removal:** Configurable list of entities to automatically delete.

* **Mob Stacking:** Automatically stacks nearby mobs of the same type to reduce entity count.

* **Smart Clear:** Automatically triggers cleanup when server TPS drops below a configurable threshold.

* **Configurable Interval:** Set the frequency at which entity deletion occurs.

* **Warning System:** Option to warn players before a cleanup occurs.

* **Overload Detection (if configured):** Detects and potentially acts upon server overload conditions.

* **Manual Cleanup:** Use a command to trigger entity deletion on demand. Does not interrupt with the regular cleanup cycle

* **Server Information Command:** Display real-time server performance stats.

* **Garbage Collection Information:** View the JVM garbage collection details.

* **Automatic Update Checks:** Stay up-to-date with the latest improvements and fixes.



## ⚙️ Commands



| Command       | Permission             | Description                                                      | Usage             |

|---------------|------------------------|------------------------------------------------------------------|-------------------|

| `/fixlag`     | `fixlag.command`       | Manually triggers the entity deletion process.                   | `/fixlag`         |

| `/gcinfo`     | `fixlag.gcinfo`        | Displays JVM memory and Garbage Collection information.          | `/gcinfo`         |

| `/serverinfo` | `fixlag.serverinfo`    | Shows server performance information (TPS, MSPT, RAM, CPU).     | `/serverinfo`     |



## 🔒 Permissions



| Permission Node        | Default | Description                                                                |

|------------------------|---------|----------------------------------------------------------------------------|

| `fixlag.overload.exempt` | `false` | Players with this permission will not trigger overload warnings.           |

| `fixlag.overload.notify` | `op`    | Players with this permission will receive overload notifications.         |

| `fixlag.command`       | `op`    | Allows players to use the `/fixlag` command.                               |

| `fixlag.gcinfo`        | `op`    | Allows players to use the `/gcinfo` command to view GC information.      |

| `fixlag.serverinfo`    | `op`    | Allows players to use the `/serverinfo` command to view server information. |

| `fixlag.notify.update` | `op`    | Allows players to receive update notifications.                            |



## 🛠️ Configuration

This plugin's main aim is to maximize configurability for server owners via `config.yml`

The main configuration file for FixLag is `config.yml`, located in the `plugins/FixLag/` directory. Here's a breakdown of the key options:



```yaml

# List of entity types (in uppercase) to be automatically deleted.

# Example:

# entities-to-delete:

#   - DROPPED_ITEM

#   - ARROW

entities-to-delete:

  - DROPPED_ITEM

  - ARROW



# Interval in seconds between automatic entity deletions.

deletion-interval-seconds: 300 # Default: 300 seconds (5 minutes)



# Enable warning message before entity deletion.

enable-warning: true



# Message to be displayed to players before cleanup. Use %time% for the countdown.

warning-message: "&eEntities will be cleared in &6%time% &eseconds."



# Time in seconds before deletion to send the warning message.

warning-time-seconds: 5 # Default: 5 seconds



# Settings for overload detection (if implemented).

overload-detection:

  check-interval-seconds: 30 # Interval in seconds to check for overloads (if this feature exists)



# Mob Stacking Configuration

mob-stacking:

  enabled: true

  radius: 10

  max-stack-size: 50

  allowed-entities:

    - ZOMBIE

    - SKELETON

    # ...

  name-format: "&e%type% &6x%count%"



# Smart Clear Configuration

smart-clear:

  enabled: true

  tps-threshold: 16.0

  check-interval-seconds: 10

  cooldown-seconds: 300



# Message broadcasted after a successful cleanup. Use %count% for the number of deleted entities.

cleanup-broadcast-message: "&aCleaned up &2%count% &aunnecessary entities."



# Log memory statistics to the console after each cleanup.

log-memory-stats: false



# Interval in seconds to check for plugin updates. Set to 0 to disable.

update-check-interval-seconds: 86400 # Default: 86400 seconds (24 hours)
