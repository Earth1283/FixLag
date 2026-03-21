# 🛠️ FixLag - Minecraft Server Performance Suite 🚀

FixLag is a comprehensive performance optimization plugin designed for modern Minecraft servers (Paper/Spigot 1.17+). Unlike simple "clearlag" scripts, FixLag provides a suite of reactive and proactive tools to maintain high TPS without disrupting gameplay or breaking technical farms.

## ✨ Core Features

### 🧹 Entity Management & Safety
* **Smart Clear:** Automatically triggers entity cleanups only when the server's 1-minute TPS drops below a configurable threshold (default: 16.0).
* **Item Recovery System:** Accidentally cleared a valuable item? Use `/fixlag retrieve` to open a temporary GUI and recover entities deleted during the last cycle.
* **Mob Stacking:** Reduces entity processing overhead by stacking similar nearby mobs into a single entity with a numeric multiplier.
* **Intelligent Filtering:** Configuration allows for excluding custom-named items, ensuring player-named pets or items remain safe.

### ⚙️ Reactive Optimizations
* **Explosion Optimizer:** During low TPS, the plugin can limit the number of blocks destroyed by TNT and disable item drops from explosions to prevent cascading lag.
* **Panic Mode:** Temporarily freezes mob AI when TPS drops critically low (e.g., 14.0) to save CPU. AI is automatically restored once TPS recovers.
* **Spawner Optimizer:** Automatically pauses mob spawners from spawning new entities when server performance is low.
* **Dynamic Distance:** Automatically adjusts view distance and simulation distance (1.18+) in real-time based on server load.
* **Overload Detection:** Scans for high entity densities around players and alerts staff before the server crashes.

### 📊 Monitoring & Administration
* **Server Analytics:** Detailed `/serverinfo` command showing TPS, MSPT, RAM, CPU usage, and per-world entity/chunk counts.
* **Redstone Analyzer:** Profile and locate laggy redstone contraptions with `/fixlag checkredstone`.
* **Chunk Analyzer:** Identify "laggy" chunks with the `/fixlag checkchunks` command, which ranks loaded chunks by entity count.
* **Config Optimizer:** Use `/fixlag optimizeconfig` to analyze your `bukkit.yml`, `spigot.yml`, and `paper.yml` files. The plugin will suggest optimized values to improve performance.

## ⚙️ Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/fixlag` | `fixlag.command` | Manually triggers entity cleanup. |
| `/fixlag retrieve` | `fixlag.retrieve` | Opens the GUI to recover deleted items. |
| `/fixlag checkchunks` | `fixlag.checkchunks` | Analyzes chunks for high entity counts. |
| `/fixlag checkredstone` | `fixlag.checkredstone` | Profile redstone activity across chunks. |
| `/fixlag optimizeconfig`| `fixlag.optimizeconfig`| Suggests optimizations for server config files. |
| `/serverinfo` | `fixlag.serverinfo` | Displays real-time performance stats. |
| `/gcinfo` | `fixlag.gcinfo` | Displays JVM Garbage Collection details. |

## 🛠️ Development & Building

The project is built using Java 21 and the Gradle wrapper.

**Build Requirements:**
* JDK 21
* Gradle 8.10 (provided via wrapper)

**To build the project:**
```bash
./gradlew build
```
The file will be put at `path/to/fixlag/build/libs/fixlag-x.y.z.jar`.
