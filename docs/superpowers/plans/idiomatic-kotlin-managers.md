# Idiomatic Kotlin Refactoring for Managers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `TaskManager`, `ChunkAnalyzer`, `MessageManager`, and `PerformanceMonitor` to use more idiomatic Kotlin features like trailing lambdas, property syntax, and collection transformations.

**Architecture:** Surgical updates to existing manager classes to improve code quality and readability without changing behavior.

**Tech Stack:** Kotlin, Bukkit API, Paper/Adventure API.

---

### Task 1: Refactor MessageManager.kt (DRY out replacements)

**Files:**
- Modify: `src/main/kotlin/io/github/Earth1283/fixLag/managers/MessageManager.kt`

- [ ] **Step 1: Extract replacement logic into a private extension function**

```kotlin
    private fun String.applyReplacements(vararg replacements: String): String {
        var result = this
        for (i in replacements.indices step 2) {
            if (i + 1 < replacements.size) {
                result = result.replace(replacements[i], replacements[i + 1])
            }
        }
        return result
    }
```

- [ ] **Step 2: Update getMessage methods to use applyReplacements**

```kotlin
    fun getMessage(key: String, includePrefix: Boolean, vararg replacements: String): String {
        var raw = (messagesConfig.getString(key) ?: "Error: Message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)
        
        if (includePrefix) {
            val prefixRaw = messagesConfig.getString("prefix") ?: "<gray>[<green>FixLag<gray>] <reset>"
            raw = prefixRaw + raw
        }
        val comp = miniMessage.deserialize(raw)
        return legacySerializer.serialize(comp)
    }
```

- [ ] **Step 3: Update getComponentMessage methods to use applyReplacements**

```kotlin
    fun getComponentMessage(key: String, includePrefix: Boolean, vararg replacements: String): Component {
        var raw = (messagesConfig.getString(key) ?: "Error: Message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)
            
        if (includePrefix) {
            val prefixRaw = messagesConfig.getString("prefix") ?: "<gray>[<green>FixLag<gray>] <reset>"
            raw = prefixRaw + raw
        }
        return miniMessage.deserialize(raw)
    }
```

- [ ] **Step 4: Update getLogMessage to use applyReplacements**

```kotlin
    fun getLogMessage(key: String, vararg replacements: String): String {
        val raw = (messagesConfig.getString(key) ?: "Error: Log message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)
        val comp = miniMessage.deserialize(raw)
        return PlainTextComponentSerializer.plainText().serialize(comp)
    }
```

- [ ] **Step 5: Update logInfo and logWarn to use applyReplacements**

```kotlin
    fun logInfo(key: String, vararg replacements: String) {
        val raw = (messagesConfig.getString(key) ?: "Error: Log message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)
        plugin.componentLogger.info(miniMessage.deserialize(raw))
    }

    fun logWarn(key: String, vararg replacements: String) {
        val raw = (messagesConfig.getString(key) ?: "Error: Log message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)
        plugin.componentLogger.warn(miniMessage.deserialize(raw))
    }
```

- [ ] **Step 6: Commit changes**

```bash
git add src/main/kotlin/io/github/Earth1283/fixLag/managers/MessageManager.kt
git commit -m "refactor: DRY out string replacements in MessageManager"
```

---

### Task 2: Refactor TaskManager.kt (Trailing lambdas and property syntax)

**Files:**
- Modify: `src/main/kotlin/io/github/Earth1283/fixLag/managers/TaskManager.kt`

- [ ] **Step 1: Update scheduler calls to use trailing lambdas**

```kotlin
    fun startSmartClearTask() {
        smartClearTask?.cancel()
        if (configManager.isSmartClearEnabled) {
            smartClearTask = Bukkit.getScheduler().runTaskTimer(plugin, { checkSmartClear() }, 20L * 30, configManager.smartClearCheckIntervalTicks)
        }
    }

    fun startLagNotifierTask() {
        lagNotifierTask?.cancel()
        if (configManager.isLagNotificationsEnabled) {
            lagNotifierTask = Bukkit.getScheduler().runTaskTimer(plugin, { lagNotifier.checkLag() }, 20L * 30, configManager.lagNotificationsCheckIntervalTicks)
        }
    }
```

- [ ] **Step 2: Use Kotlin property syntax for getCustomName()**

```kotlin
    private fun deleteEntities(): List<ItemStack> {
        // ... inside loop
                    if (configManager.isIgnoreCustomNamedItems && entity.customName != null) {
                        continue
                    }
        // ...
    }
```

- [ ] **Step 3: Commit changes**

```bash
git add src/main/kotlin/io/github/Earth1283/fixLag/managers/TaskManager.kt
git commit -m "refactor: use trailing lambdas and property syntax in TaskManager"
```

---

### Task 3: Refactor ChunkAnalyzer.kt (Trailing lambdas)

**Files:**
- Modify: `src/main/kotlin/io/github/Earth1283/fixLag/managers/ChunkAnalyzer.kt`

- [ ] **Step 1: Update nested scheduler calls to use trailing lambdas**

```kotlin
    fun analyzeChunks(sender: CommandSender) {
        sender.sendMessage(messageManager.getMessage("chunk_analysis_started"))

        Bukkit.getScheduler().runTaskAsynchronously(plugin) {
            val chunkEntityCounts = mutableMapOf<ChunkSnapshotWrapper, Int>()

            Bukkit.getScheduler().runTask(plugin) {
                for (world in Bukkit.getWorlds()) {
                    for (chunk in world.loadedChunks) {
                        val entityCount = chunk.entities.size
                        if (entityCount > 0) {
                            chunkEntityCounts[ChunkSnapshotWrapper(world.name, chunk.x, chunk.z)] = entityCount
                        }
                    }
                }

                // Now sort and display async
                Bukkit.getScheduler().runTaskAsynchronously(plugin) {
                    val sortedChunks = chunkEntityCounts.entries
                        .sortedByDescending { it.value }
                        .take(10)

                    sender.sendMessage(messageManager.getMessage("chunk_analysis_header"))
                    if (sortedChunks.isEmpty()) {
                        sender.sendMessage(messageManager.getMessage("chunk_analysis_no_data"))
                    } else {
                        for (entry in sortedChunks) {
                            val chunk = entry.key
                            val count = entry.value
                            sender.sendMessage(messageManager.getMessage("chunk_analysis_entry",
                                "<world>", chunk.worldName,
                                "<x>", chunk.x.toString(),
                                "<z>", chunk.z.toString(),
                                "<count>", count.toString()))
                        }
                    }
                }
            }
        }
    }
```

- [ ] **Step 2: Commit changes**

```bash
git add src/main/kotlin/io/github/Earth1283/fixLag/managers/ChunkAnalyzer.kt
git commit -m "refactor: use trailing lambdas in ChunkAnalyzer"
```

---

### Task 4: Refactor PerformanceMonitor.kt (Idiomatic string building)

**Files:**
- Modify: `src/main/kotlin/io/github/Earth1283/fixLag/managers/PerformanceMonitor.kt`

- [ ] **Step 1: Replace StringBuilder with joinToString in getMemoryAndGCInfo**

```kotlin
        val gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()
        val gcStats = gcMXBeans.joinToString("\n") { gcBean ->
            "${gcBean.name}: Collections=${gcBean.collectionCount}, Time=${gcBean.collectionTime}ms"
        }
```

- [ ] **Step 2: Replace StringBuilder with buildString and joinToString in getServerInfo**

```kotlin
        // Calculate World Stats
        val worldStats = buildString {
            append(messageManager.getRawMessage("server_info_world_header")).append("\n")
            val worldEntries = Bukkit.getWorlds().joinToString("\n") { world ->
                messageManager.getRawMessage("server_info_world_entry")
                    .replace("%fixlag_world_name%", world.name)
                    .replace("%fixlag_world_chunks%", world.loadedChunks.size.toString())
                    .replace("%fixlag_world_entities%", world.entityCount.toString())
            }
            append(worldEntries)
        }
```

- [ ] **Step 3: Commit changes**

```bash
git add src/main/kotlin/io/github/Earth1283/fixLag/managers/PerformanceMonitor.kt
git commit -m "refactor: use idiomatic string building in PerformanceMonitor"
```

---

### Task 5: Verification

- [ ] **Step 1: Compile the project to ensure no syntax errors**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Final commit if any fixes were needed during verification**
