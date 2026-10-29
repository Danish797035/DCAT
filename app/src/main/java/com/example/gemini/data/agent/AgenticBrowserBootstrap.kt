package com.example.gemini.data.agent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * D~C@T: "Agentic browser" bootstrap. Makes sure the in-app Browser Automation MCP server (browser_mcp) and the
 * bundled Google Flow extension MCP server (google_flow_mcp) are registered for both agents, using the same specs
 * as the Settings quick presets. Each server is added at most once per agent: once added, a later removal or
 * "disabled" flag set by the user is respected and never undone.
 */
class AgenticBrowserBootstrap(
    private val context: Context,
    private val scope: CoroutineScope,
    /** Adds the given servers to the agy mcp_config.json; returns the names that are now present, or null if agy is not reachable. */
    private val ensureAgy: suspend (List<com.example.gemini.domain.model.McpServerSpec>, Set<String>) -> Set<String>?,
    /** Returns Claude's configured server names, or null if Claude is unavailable. */
    private val listClaude: suspend () -> Set<String>?,
    /** Adds one Claude MCP server; returns true when it was added. */
    private val addClaude: suspend (com.example.gemini.data.agent.claude.ClaudeMcpAddRequest) -> Boolean
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var retryJob: Job? = null

    init { current = this }

    private fun deviceId(): String = runCatching {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
    }.getOrNull().orEmpty()

    private data class Server(val name: String, val url: String)

    private fun servers(): List<Server> {
        val ext = com.example.gemini.ui.browser.BrowserExtensionManager.instance
        return listOf(
            Server("browser_mcp", "http://127.0.0.1:${com.example.gemini.data.remote.AndroidLocalBridgeServer.DEFAULT_PORT}/browser"),
            Server(ext.mcpServerName(FLOW_EXTENSION_ID), ext.mcpUrl(FLOW_EXTENSION_ID))
        )
    }

    private fun headers(): Map<String, String> = deviceId().let { if (it.isNotBlank()) mapOf("X-Device-Id" to it) else emptyMap() }

    private fun seededKey(agent: String, name: String) = "seeded_${agent}_$name"

    /** Starts (or restarts) a background run that retries for a while until both agents are set up. */
    fun requestRun() {
        if (!isEnabled(context)) return
        if (retryJob?.isActive == true) return
        retryJob = scope.launch {
            repeat(MAX_ATTEMPTS) {
                val done = runCatching { runOnce() }.onFailure { Log.w(TAG, "bootstrap failed: ${it.message}") }.getOrDefault(false)
                if (done || !isEnabled(context)) return@launch
                delay(RETRY_DELAY_MS)
            }
        }
    }

    /** One pass over both agents. Returns true when nothing is left to do. */
    private suspend fun runOnce(): Boolean = mutex.withLock {
        val list = servers()
        val hdrs = headers()

        // ---- Antigravity (agy mcp_config.json)
        val agyPending = list.filter { !prefs.getBoolean(seededKey("agy", it.name), false) }
        var agyDone = agyPending.isEmpty()
        if (!agyDone) {
            val specs = agyPending.map { com.example.gemini.domain.model.McpServerSpec(serverName = it.name, serverUrl = it.url, headers = hdrs) }
            val present = ensureAgy(specs, agyPending.map { it.name }.toSet())
            if (present != null) {
                agyPending.filter { it.name in present }.forEach { prefs.edit().putBoolean(seededKey("agy", it.name), true).apply() }
                agyDone = agyPending.all { it.name in present }
            }
        }

        // ---- Claude Code (claude mcp add, user scope)
        val claudePending = list.filter { !prefs.getBoolean(seededKey("claude", it.name), false) }
        var claudeDone = claudePending.isEmpty()
        if (!claudeDone) {
            val existing = listClaude()
            if (existing != null) {
                claudeDone = true
                for (s in claudePending) {
                    val ok = s.name in existing || addClaude(
                        com.example.gemini.data.agent.claude.ClaudeMcpAddRequest(name = s.name, transport = "http", url = s.url, headers = hdrs)
                    )
                    if (ok) prefs.edit().putBoolean(seededKey("claude", s.name), true).apply() else claudeDone = false
                }
            }
        }
        Log.i(TAG, "bootstrap pass: agyDone=$agyDone claudeDone=$claudeDone")
        agyDone && claudeDone
    }

    companion object {
        private const val TAG = "AgenticBrowser"
        private const val PREFS = "dcat_agentic_browser"
        private const val KEY_ENABLED = "agentic_browser_enabled"
        const val FLOW_EXTENSION_ID = "google-flow"
        private const val MAX_ATTEMPTS = 30
        private const val RETRY_DELAY_MS = 20_000L

        @Volatile
        private var current: AgenticBrowserBootstrap? = null

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

        /**
         * Settings toggle. Turning it on again forgets which servers were added before, so anything the user had
         * removed is registered again; turning it off stops auto-registration but leaves configured servers alone.
         */
        fun setEnabled(context: Context, enabled: Boolean) {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val e = p.edit().putBoolean(KEY_ENABLED, enabled)
            if (enabled) p.all.keys.filter { it.startsWith("seeded_") }.forEach { e.remove(it) }
            e.apply()
            if (enabled) current?.requestRun()
        }
    }
}
