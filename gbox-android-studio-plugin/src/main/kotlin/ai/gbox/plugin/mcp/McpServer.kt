package ai.gbox.plugin.mcp

import ai.gbox.plugin.services.AndroidControlService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP (Model Context Protocol) Server implementation for Android Studio Plugin control.
 * This allows external tools like Claude Code to control Android app execution.
 */
class McpServer {
    private val logger = thisLogger()
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val androidService = service<AndroidControlService>()
    private var isRunning = false
    private val requestHandlers = ConcurrentHashMap<String, suspend (Map<String, Any?>) -> Any>()
    
    init {
        initializeHandlers()
    }
    
    data class McpRequest(
        val jsonrpc: String = "2.0",
        val id: Any?,
        val method: String,
        val params: Map<String, Any?> = emptyMap()
    )
    
    data class McpResponse(
        val jsonrpc: String = "2.0",
        val id: Any?,
        val result: Any? = null,
        val error: McpError? = null
    )
    
    data class McpError(
        val code: Int,
        val message: String,
        val data: Any? = null
    )
    
    private fun initializeHandlers() {
        // Initialize MCP protocol handlers
        requestHandlers["initialize"] = { params ->
            mapOf<String, Any>(
                "protocolVersion" to "2024-11-05",
                "capabilities" to mapOf<String, Any>(
                    "logging" to mapOf<String, Any>(),
                    "tools" to mapOf<String, Any>(
                        "listChanged" to true
                    )
                ),
                "serverInfo" to mapOf<String, Any>(
                    "name" to "gbox-android-studio-plugin",
                    "version" to "1.0.0"
                )
            )
        }
        
        requestHandlers["tools/list"] = { _ ->
            mapOf(
                "tools" to listOf(
                    mapOf(
                        "name" to "android_start_app",
                        "description" to "Start the Android application in the current project",
                        "inputSchema" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "projectPath" to mapOf(
                                    "type" to "string",
                                    "description" to "Optional path to the Android project"
                                )
                            )
                        )
                    ),
                    mapOf(
                        "name" to "android_stop_app",
                        "description" to "Stop the currently running Android application",
                        "inputSchema" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "projectPath" to mapOf(
                                    "type" to "string",
                                    "description" to "Optional path to the Android project"
                                )
                            )
                        )
                    ),
                    mapOf(
                        "name" to "android_rerun_app",
                        "description" to "Rerun the Android application (stop and start)",
                        "inputSchema" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "projectPath" to mapOf(
                                    "type" to "string",
                                    "description" to "Optional path to the Android project"
                                )
                            )
                        )
                    ),
                    mapOf(
                        "name" to "android_debug_app",
                        "description" to "Start debugging the Android application",
                        "inputSchema" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "projectPath" to mapOf(
                                    "type" to "string",
                                    "description" to "Optional path to the Android project"
                                )
                            )
                        )
                    ),
                    mapOf(
                        "name" to "android_get_configurations",
                        "description" to "Get list of available Android run configurations",
                        "inputSchema" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "projectPath" to mapOf(
                                    "type" to "string",
                                    "description" to "Optional path to the Android project"
                                )
                            )
                        )
                    ),
                    mapOf(
                        "name" to "android_select_configuration",
                        "description" to "Select a specific Android run configuration",
                        "inputSchema" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "configurationName" to mapOf(
                                    "type" to "string",
                                    "description" to "Name of the configuration to select"
                                ),
                                "projectPath" to mapOf(
                                    "type" to "string",
                                    "description" to "Optional path to the Android project"
                                )
                            ),
                            "required" to listOf("configurationName")
                        )
                    )
                )
            )
        }
        
        requestHandlers["tools/call"] = { params ->
            val name = params["name"] as? String ?: throw IllegalArgumentException("Tool name is required")
            val arguments = (params["arguments"] as? Map<String, Any?>) ?: emptyMap()
            
            when (name) {
                "android_start_app" -> {
                    val projectPath = arguments["projectPath"] as? String
                    val result = androidService.startApp(projectPath).get()
                    mapOf(
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to if (result.success) {
                                    "✅ Android app started successfully: ${result.message}" +
                                            (result.configurationName?.let { "\nConfiguration: $it" } ?: "")
                                } else {
                                    "❌ Failed to start Android app: ${result.message}"
                                }
                            )
                        )
                    )
                }
                
                "android_stop_app" -> {
                    val projectPath = arguments["projectPath"] as? String
                    val result = androidService.stopApp(projectPath).get()
                    mapOf(
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to if (result.success) {
                                    "✅ Android app stopped successfully: ${result.message}"
                                } else {
                                    "❌ Failed to stop Android app: ${result.message}"
                                }
                            )
                        )
                    )
                }
                
                "android_rerun_app" -> {
                    val projectPath = arguments["projectPath"] as? String
                    val result = androidService.rerunApp(projectPath).get()
                    mapOf(
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to if (result.success) {
                                    "✅ Android app rerun successful: ${result.message}" +
                                            (result.configurationName?.let { "\nConfiguration: $it" } ?: "")
                                } else {
                                    "❌ Failed to rerun Android app: ${result.message}"
                                }
                            )
                        )
                    )
                }
                
                "android_debug_app" -> {
                    val projectPath = arguments["projectPath"] as? String
                    val result = androidService.debugApp(projectPath).get()
                    mapOf(
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to if (result.success) {
                                    "🐛 Android app debug session started: ${result.message}" +
                                            (result.configurationName?.let { "\nConfiguration: $it" } ?: "")
                                } else {
                                    "❌ Failed to start debug session: ${result.message}"
                                }
                            )
                        )
                    )
                }
                
                "android_get_configurations" -> {
                    val projectPath = arguments["projectPath"] as? String
                    val configurations = androidService.getRunConfigurations(projectPath)
                    mapOf(
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to "📱 Available Android configurations (${configurations.size}):\n" +
                                        configurations.joinToString("\n") { "• $it" }
                            )
                        )
                    )
                }
                
                "android_select_configuration" -> {
                    val configurationName = arguments["configurationName"] as? String
                        ?: throw IllegalArgumentException("Configuration name is required")
                    val projectPath = arguments["projectPath"] as? String
                    val result = androidService.selectConfiguration(configurationName, projectPath)
                    mapOf(
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to if (result.success) {
                                    "✅ Configuration selected: ${result.message}"
                                } else {
                                    "❌ Failed to select configuration: ${result.message}"
                                }
                            )
                        )
                    )
                }
                
                else -> throw IllegalArgumentException("Unknown tool: $name")
            }
        }
    }
    
    suspend fun handleStdioMcp() {
        withContext(Dispatchers.IO) {
            isRunning = true
            logger.info("Starting MCP Server in stdio mode")
            
            val input = BufferedReader(InputStreamReader(System.`in`))
            val output = OutputStreamWriter(System.out)
            
            try {
                while (isRunning) {
                    val line = input.readLine() ?: break
                    if (line.isBlank()) continue
                    
                    try {
                        val request = objectMapper.readValue(line, McpRequest::class.java)
                        val response = handleRequest(request)
                        val responseJson = objectMapper.writeValueAsString(response)
                        
                        output.write(responseJson)
                        output.write("\n")
                        output.flush()
                        
                    } catch (e: Exception) {
                        logger.error("Error processing MCP request", e)
                        val errorResponse = McpResponse(
                            id = null,
                            error = McpError(
                                code = -32603,
                                message = "Internal error: ${e.message}"
                            )
                        )
                        val errorJson = objectMapper.writeValueAsString(errorResponse)
                        output.write(errorJson)
                        output.write("\n")
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                logger.error("MCP Server error", e)
            } finally {
                isRunning = false
                logger.info("MCP Server stopped")
            }
        }
    }
    
    private suspend fun handleRequest(request: McpRequest): McpResponse {
        return try {
            val handler = requestHandlers[request.method]
                ?: throw IllegalArgumentException("Unknown method: ${request.method}")
            
            val result = handler(request.params)
            
            McpResponse(
                id = request.id,
                result = result
            )
        } catch (e: Exception) {
            logger.error("Error handling MCP request", e)
            McpResponse(
                id = request.id,
                error = McpError(
                    code = -32602,
                    message = e.message ?: "Unknown error"
                )
            )
        }
    }
    
    fun stop() {
        isRunning = false
    }
}