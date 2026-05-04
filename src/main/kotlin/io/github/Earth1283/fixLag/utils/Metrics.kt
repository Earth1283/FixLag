package io.github.Earth1283.fixLag.utils

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.*
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Level
import java.util.stream.Collectors
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection

class Metrics(private val plugin: Plugin, serviceId: Int) {
    private val metricsBase: MetricsBase

    init {
        val bStatsFolder = File(plugin.dataFolder.parentFile, "bStats")
        val configFile = File(bStatsFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true)
            config.addDefault("serverUuid", UUID.randomUUID().toString())
            config.addDefault("logFailedRequests", false)
            config.addDefault("logSentData", false)
            config.addDefault("logResponseStatusText", false)
            config.options().header(
                "bStats (https://bStats.org) collects some basic information for plugin authors, like how\n" +
                        "many people use their plugin and their total player count. It's recommended to keep bStats\n" +
                        "enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n" +
                        "performance penalty associated with having metrics enabled, and data sent to bStats is fully\n" +
                        "anonymous."
            ).copyDefaults(true)
            try {
                config.save(configFile)
            } catch (ignored: IOException) {
            }
        }
        val enabled = config.getBoolean("enabled", true)
        val serverUUID = config.getString("serverUuid") ?: UUID.randomUUID().toString()
        val logErrors = config.getBoolean("logFailedRequests", false)
        val logSentData = config.getBoolean("logSentData", false)
        val logResponseStatusText = config.getBoolean("logResponseStatusText", false)
        var isFolia = false
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            isFolia = true
        } catch (ignored: Exception) {
        }
        metricsBase = MetricsBase(
            "bukkit",
            serverUUID,
            serviceId,
            enabled,
            { builder: JsonObjectBuilder -> appendPlatformData(builder) },
            { builder: JsonObjectBuilder -> appendServiceData(builder) },
            if (isFolia) null else Consumer { submitDataTask: Runnable -> Bukkit.getScheduler().runTask(plugin, submitDataTask) },
            Supplier { plugin.isEnabled },
            BiConsumer { message: String, error: Throwable? -> plugin.logger.log(Level.WARNING, message, error) },
            Consumer { message: String -> plugin.logger.log(Level.INFO, message) },
            logErrors,
            logSentData,
            logResponseStatusText,
            false
        )
    }

    fun shutdown() {
        metricsBase.shutdown()
    }

    fun addCustomChart(chart: CustomChart) {
        metricsBase.addCustomChart(chart)
    }

    private fun appendPlatformData(builder: JsonObjectBuilder) {
        builder.appendField("playerAmount", playerAmount)
        builder.appendField("onlineMode", if (Bukkit.getOnlineMode()) 1 else 0)
        builder.appendField("bukkitVersion", Bukkit.getVersion())
        builder.appendField("bukkitName", Bukkit.getName())
        builder.appendField("javaVersion", System.getProperty("java.version"))
        builder.appendField("osName", System.getProperty("os.name"))
        builder.appendField("osArch", System.getProperty("os.arch"))
        builder.appendField("osVersion", System.getProperty("os.version"))
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors())
    }

    private fun appendServiceData(builder: JsonObjectBuilder) {
        builder.appendField("pluginVersion", plugin.description.version)
    }

    private val playerAmount: Int
        get() = try {
            val onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers")
            if (onlinePlayersMethod.returnType == Collection::class.java) {
                (onlinePlayersMethod.invoke(Bukkit.getServer()) as Collection<*>).size
            } else {
                (onlinePlayersMethod.invoke(Bukkit.getServer()) as Array<*>).size
            }
        } catch (e: Exception) {
            Bukkit.getOnlinePlayers().size
        }

    class MetricsBase(
        private val platform: String,
        private val serverUuid: String,
        private val serviceId: Int,
        private val enabled: Boolean,
        private val appendPlatformDataConsumer: Consumer<JsonObjectBuilder>,
        private val appendServiceDataConsumer: Consumer<JsonObjectBuilder>,
        private val submitTaskConsumer: Consumer<Runnable>?,
        private val checkServiceEnabledSupplier: Supplier<Boolean>,
        private val errorLogger: BiConsumer<String, Throwable>,
        private val infoLogger: Consumer<String>,
        private val logErrors: Boolean,
        private val logSentData: Boolean,
        private val logResponseStatusText: Boolean,
        skipRelocateCheck: Boolean
    ) {
        private val scheduler: ScheduledExecutorService
        private val customCharts: MutableSet<CustomChart> = HashSet()

        init {
            val scheduler = ScheduledThreadPoolExecutor(1) { task: Runnable ->
                val thread = Thread(task, "bStats-Metrics")
                thread.isDaemon = true
                thread
            }
            scheduler.executeExistingDelayedTasksAfterShutdownPolicy = false
            this.scheduler = scheduler
            if (!skipRelocateCheck) {
                checkRelocation()
            }
            if (enabled) {
                startSubmitting()
            }
        }

        fun addCustomChart(chart: CustomChart) {
            customCharts.add(chart)
        }

        fun shutdown() {
            scheduler.shutdown()
        }

        private fun startSubmitting() {
            val submitTask = Runnable {
                if (!enabled || !checkServiceEnabledSupplier.get()) {
                    scheduler.shutdown()
                    return@Runnable
                }
                if (submitTaskConsumer != null) {
                    submitTaskConsumer.accept(Runnable { submitData() })
                } else {
                    submitData()
                }
            }
            val initialDelay = (1000 * 60 * (3 + Math.random() * 3)).toLong()
            val secondDelay = (1000 * 60 * (Math.random() * 30)).toLong()
            scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS)
            scheduler.scheduleAtFixedRate(
                submitTask, initialDelay + secondDelay, (1000 * 60 * 30).toLong(), TimeUnit.MILLISECONDS
            )
        }

        private fun submitData() {
            val baseJsonBuilder = JsonObjectBuilder()
            appendPlatformDataConsumer.accept(baseJsonBuilder)
            val serviceJsonBuilder = JsonObjectBuilder()
            appendServiceDataConsumer.accept(serviceJsonBuilder)
            val chartData = customCharts.stream()
                .map { customChart: CustomChart -> customChart.getRequestJsonObject(errorLogger, logErrors) }
                .filter { obj: JsonObjectBuilder.JsonObject? -> Objects.nonNull(obj) }
                .toArray { size: Int -> arrayOfNulls<JsonObjectBuilder.JsonObject>(size) }
            serviceJsonBuilder.appendField("id", serviceId)
            serviceJsonBuilder.appendField("customCharts", chartData as Array<JsonObjectBuilder.JsonObject>)
            baseJsonBuilder.appendField("service", serviceJsonBuilder.build())
            baseJsonBuilder.appendField("serverUUID", serverUuid)
            baseJsonBuilder.appendField("metricsVersion", METRICS_VERSION)
            val data = baseJsonBuilder.build()
            scheduler.execute {
                try {
                    sendData(data)
                } catch (e: Exception) {
                    if (logErrors) {
                        errorLogger.accept("Could not submit bStats metrics data", e)
                    }
                }
            }
        }

        @Throws(Exception::class)
        private fun sendData(data: JsonObjectBuilder.JsonObject) {
            if (logSentData) {
                infoLogger.accept("Sent bStats metrics data: $data")
            }
            val url = String.format(REPORT_URL, platform)
            val connection = java.net.URI(url).toURL().openConnection() as HttpsURLConnection
            val compressedData = compress(data.toString())
            connection.requestMethod = "POST"
            connection.addRequestProperty("Accept", "application/json")
            connection.addRequestProperty("Connection", "close")
            connection.addRequestProperty("Content-Encoding", "gzip")
            connection.addRequestProperty("Content-Length", compressedData!!.size.toString())
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Metrics-Service/1")
            connection.doOutput = true
            DataOutputStream(connection.outputStream).use { outputStream -> outputStream.write(compressedData) }
            val builder = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { bufferedReader ->
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    builder.append(line)
                }
            }
            if (logResponseStatusText) {
                infoLogger.accept("Sent data to bStats and received response: $builder")
            }
        }

        private fun checkRelocation() {
            if (System.getProperty("bstats.relocatecheck") == null || System.getProperty("bstats.relocatecheck") != "false") {
                val defaultPackage = String(byteArrayOf('o'.code.toByte(), 'r'.code.toByte(), 'g'.code.toByte(), '.'.code.toByte(), 'b'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 's'.code.toByte()))
                val examplePackage = String(byteArrayOf('y'.code.toByte(), 'o'.code.toByte(), 'u'.code.toByte(), 'r'.code.toByte(), '.'.code.toByte(), 'p'.code.toByte(), 'a'.code.toByte(), 'c'.code.toByte(), 'k'.code.toByte(), 'a'.code.toByte(), 'g'.code.toByte(), 'e'.code.toByte()))
                if (MetricsBase::class.java.getPackage().name.startsWith(defaultPackage) || MetricsBase::class.java.getPackage().name.startsWith(examplePackage)) {
                    throw IllegalStateException("bStats Metrics class has not been relocated correctly!")
                }
            }
        }

        companion object {
            const val METRICS_VERSION = "3.1.0"
            private const val REPORT_URL = "https://bStats.org/api/v2/data/%s"

            @Throws(IOException::class)
            private fun compress(str: String?): ByteArray? {
                if (str == null) {
                    return null
                }
                val outputStream = ByteArrayOutputStream()
                GZIPOutputStream(outputStream).use { gzip -> gzip.write(str.toByteArray(StandardCharsets.UTF_8)) }
                return outputStream.toByteArray()
            }
        }
    }

    class AdvancedBarChart(chartId: String, private val callable: Callable<Map<String, IntArray>>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            var allSkipped = true
            for ((key, value) in map) {
                if (value.isEmpty()) {
                    continue
                }
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            return if (allSkipped) {
                null
            } else JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class SimplePie(chartId: String, private val callable: Callable<String>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val value = callable.call()
            return if (value == null || value.isEmpty()) {
                null
            } else JsonObjectBuilder().appendField("value", value).build()
        }
    }

    class DrilldownPie(chartId: String, private val callable: Callable<Map<String, Map<String, Int>>>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            var reallyAllSkipped = true
            for (entryValues in map.entries) {
                val valueBuilder = JsonObjectBuilder()
                var allSkipped = true
                for (valueEntry in map[entryValues.key]!!.entries) {
                    valueBuilder.appendField(valueEntry.key, valueEntry.value)
                    allSkipped = false
                }
                if (!allSkipped) {
                    reallyAllSkipped = false
                    valuesBuilder.appendField(entryValues.key, valueBuilder.build())
                }
            }
            return if (reallyAllSkipped) {
                null
            } else JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class SingleLineChart(chartId: String, private val callable: Callable<Int>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val value = callable.call()
            return if (value == 0) {
                null
            } else JsonObjectBuilder().appendField("value", value).build()
        }
    }

    class MultiLineChart(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            var allSkipped = true
            for ((key, value) in map) {
                if (value == 0) {
                    continue
                }
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            return if (allSkipped) {
                null
            } else JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class AdvancedPie(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            var allSkipped = true
            for ((key, value) in map) {
                if (value == 0) {
                    continue
                }
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            return if (allSkipped) {
                null
            } else JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    abstract class CustomChart(private val chartId: String) {
        fun getRequestJsonObject(errorLogger: BiConsumer<String, Throwable>, logErrors: Boolean): JsonObjectBuilder.JsonObject? {
            val builder = JsonObjectBuilder()
            builder.appendField("chartId", chartId)
            return try {
                val data = getChartData() ?: return null
                builder.appendField("data", data)
                builder.build()
            } catch (t: Throwable) {
                if (logErrors) {
                    errorLogger.accept("Failed to get data for custom chart with id $chartId", t)
                }
                null
            }
        }

        @Throws(Exception::class)
        protected abstract fun getChartData(): JsonObjectBuilder.JsonObject?
    }

    class SimpleBarChart(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            for ((key, value) in map) {
                valuesBuilder.appendField(key, intArrayOf(value))
            }
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class JsonObjectBuilder {
        private var builder: StringBuilder? = StringBuilder()
        private var hasAtLeastOneField = false

        init {
            builder!!.append("{")
        }

        fun appendNull(key: String): JsonObjectBuilder {
            appendFieldUnescaped(key, "null")
            return this
        }

        fun appendField(key: String, value: String?): JsonObjectBuilder {
            requireNotNull(value) { "JSON value must not be null" }
            appendFieldUnescaped(key, "\"" + escape(value) + "\"")
            return this
        }

        fun appendField(key: String, value: Int): JsonObjectBuilder {
            appendFieldUnescaped(key, value.toString())
            return this
        }

        fun appendField(key: String, `object`: JsonObject?): JsonObjectBuilder {
            requireNotNull(`object`) { "JSON object must not be null" }
            appendFieldUnescaped(key, `object`.toString())
            return this
        }

        fun appendField(key: String, values: Array<String>?): JsonObjectBuilder {
            requireNotNull(values) { "JSON values must not be null" }
            val escapedValues = Arrays.stream(values).map { value: String -> "\"" + escape(value) + "\"" }.collect(Collectors.joining(","))
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        fun appendField(key: String, values: IntArray?): JsonObjectBuilder {
            requireNotNull(values) { "JSON values must not be null" }
            val escapedValues = Arrays.stream(values).mapToObj { i: Int -> i.toString() }.collect(Collectors.joining(","))
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        fun appendField(key: String, values: Array<JsonObject>?): JsonObjectBuilder {
            requireNotNull(values) { "JSON values must not be null" }
            val escapedValues = Arrays.stream(values).map { obj: JsonObject -> obj.toString() }.collect(Collectors.joining(","))
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        private fun appendFieldUnescaped(key: String, escapedValue: String) {
            checkNotNull(builder) { "JSON has already been built" }
            requireNotNull(key) { "JSON key must not be null" }
            if (hasAtLeastOneField) {
                builder!!.append(",")
            }
            builder!!.append("\"").append(escape(key)).append("\":").append(escapedValue)
            hasAtLeastOneField = true
        }

        fun build(): JsonObject {
            checkNotNull(builder) { "JSON has already been built" }
            val `object` = JsonObject(builder!!.append("}").toString())
            builder = null
            return `object`
        }

        class JsonObject(private val value: String) {
            override fun toString(): String {
                return value
            }
        }

        companion object {
            private fun escape(value: String): String {
                val builder = StringBuilder()
                for (i in 0 until value.length) {
                    val c = value[i]
                    if (c == '"') {
                        builder.append("\\\"")
                    } else if (c == '\\') {
                        builder.append("\\\\")
                    } else if (c <= '\u000F') {
                        builder.append("\\u000").append(Integer.toHexString(c.code))
                    } else if (c <= '\u001F') {
                        builder.append("\\u00").append(Integer.toHexString(c.code))
                    } else {
                        builder.append(c)
                    }
                }
                return builder.toString()
            }
        }
    }
}
