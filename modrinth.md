# 🛠️ FixLag: Performance without Compromise

**FixLag** is a lightweight but powerful performance suite for Paper, Purpur, and Spigot servers. While most lag-fix plugins simply delete items on a timer, FixLag uses **Smart Analysis** to keep your server running at a smooth 20 TPS without breaking your players' hard-earned farms.

### 🚀 Why choose FixLag?

* **❌ No More Accidental Deletions:** With the **Retrieve GUI**, players can recover items that were cleared during a cleanup. You can also whitelist custom-named items.
* **🧠 Reactive Scaling:** Features like **Dynamic Distance** and **Explosion Optimization** only kick in when the server actually struggles. If your TPS is fine, FixLag stays out of the way.
* **🛠️ One-Click Optimization:** The `/fixlag optimizeconfig` command scans your `spigot.yml`, `bukkit.yml`, and `paper.yml` to suggest better settings used by more experienced server admins.
* **📈 Staff Alerts:** Get instant notifications if a player is creating an entity-lag machine or if RAM usage exceeds 90%.

### ✨ Feature Highlights

* **Panic Mode:** Reactive AI freezer for mobs when TPS drops below 14.0.
* **Spawner Optimizer:** Automatically pauses mob spawners during low-TPS periods.
* **Redstone Profiling:** Find laggy contraptions with `/fixlag checkredstone`.
* **Mob Stacking:** Merges mobs into stacks to save CPU cycles.
* **XP Orb Merger:** Actively merges nearby XP orbs into single, high-value entities.
* **Chunk Analyzer:** Instantly find which base or farm is causing the most entity lag.

### 📋 Quick Start

1. Drop the JAR into your `plugins` folder.
2. Restart your server.
3. Configure your preferences in `plugins/FixLag/config.yml`.
4. Use `/serverinfo` to check your current performance!

---
**Permissions:** FixLag uses standard permissions (e.g., `fixlag.command`, `fixlag.serverinfo`). All commands default to OP.

**Support:** Found a bug? Please report it on our GitHub Issues page!