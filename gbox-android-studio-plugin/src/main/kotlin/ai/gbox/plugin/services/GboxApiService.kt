package ai.gbox.plugin.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.Executors

@Service(Service.Level.APP)
class GboxApiService {
    private val logger = thisLogger()
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val port = 8765 // Default port
    
    data class ApiRequest(
        val action: String,
        val projectPath: String? = null,
        val configurationName: String? = null
    )
    
    data class ApiResponse(
        val success: Boolean,
        val message: String,
        val data: Any? = null
    )

    fun startApiServer(): ApiResponse {
        if (isRunning) {
            return ApiResponse(false, "API server is already running on port $port")
        }
        
        return try {
            serverSocket = ServerSocket()
            serverSocket?.bind(InetSocketAddress("localhost", port))
            
            serverJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                isRunning = true
                logger.info("Run-API-Server started on port $port")
                
                while (isRunning && !currentCoroutineContext()[Job]?.isCancelled!!) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let { socket ->
                            launch {
                                handleHttpRequest(socket)
                            }
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            logger.error("Error accepting client connection", e)
                        }
                    }
                }
            }
            
            ApiResponse(true, "API server started successfully on port $port")
        } catch (e: Exception) {
            logger.error("Failed to start API server", e)
            ApiResponse(false, "Failed to start API server: ${e.message}")
        }
    }

    fun stopApiServer(): ApiResponse {
        return try {
            isRunning = false
            serverJob?.cancel()
            serverSocket?.close()
            
            ApiResponse(true, "API server stopped successfully")
        } catch (e: Exception) {
            logger.error("Error stopping API server", e)
            ApiResponse(false, "Error stopping API server: ${e.message}")
        }
    }

    private suspend fun handleHttpRequest(socket: java.net.Socket) {
        withContext(Dispatchers.IO) {
            try {
                val input = socket.getInputStream().bufferedReader()
                val output = socket.getOutputStream()
                
                // Read HTTP request
                val requestLine = input.readLine() ?: return@withContext
                val parts = requestLine.split(" ")
                
                if (parts.size < 3) return@withContext
                
                val method = parts[0]
                val path = parts[1]
                
                // Read headers
                val headers = mutableMapOf<String, String>()
                var line = input.readLine()
                while (!line.isNullOrEmpty()) {
                    val headerParts = line.split(": ", limit = 2)
                    if (headerParts.size == 2) {
                        headers[headerParts[0]] = headerParts[1]
                    }
                    line = input.readLine()
                }
                
                // Read body for POST requests
                val body = if (method == "POST" && headers["Content-Length"] != null) {
                    val contentLength = headers["Content-Length"]!!.toInt()
                    val bodyChars = CharArray(contentLength)
                    input.read(bodyChars, 0, contentLength)
                    String(bodyChars)
                } else null
                
                val response = when {
                    path == "/api/start" && method == "POST" -> handleStartApp(body)
                    path == "/api/stop" && method == "POST" -> handleStopApp(body)
                    path == "/api/rerun" && method == "POST" -> handleRerunApp(body)
                    path == "/api/debug" && method == "POST" -> handleDebugApp(body)
                    path == "/api/configurations" && method == "GET" -> handleGetConfigurations(body)
                    path == "/api/select-configuration" && method == "POST" -> handleSelectConfiguration(body)
                    path == "/api/status" && method == "GET" -> handleGetStatus()
                    else -> ApiResponse(false, "Endpoint not found")
                }
                
                val responseJson = objectMapper.writeValueAsString(response)
                val httpResponse = buildHttpResponse(responseJson)
                
                output.write(httpResponse.toByteArray())
                output.flush()
                
            } catch (e: Exception) {
                logger.error("Error handling HTTP request", e)
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                    logger.error("Error closing socket", e)
                }
            }
        }
    }
    
    private fun buildHttpResponse(content: String): String {
        val headers = listOf(
            "HTTP/1.1 200 OK",
            "Content-Type: application/json",
            "Content-Length: ${content.toByteArray().size}",
            "Access-Control-Allow-Origin: *",
            "Access-Control-Allow-Methods: GET, POST, OPTIONS",
            "Access-Control-Allow-Headers: Content-Type",
            "" // blank line to separate headers from body
        )
        return (headers + content).joinToString("\r\n")
    }

    private suspend fun handleStartApp(body: String?): ApiResponse {
        return try {
            val request = body?.let { objectMapper.readValue(it, ApiRequest::class.java) }
            val androidService = com.intellij.openapi.components.service<AndroidControlService>()
            val result = androidService.startApp(request?.projectPath).get()
            
            ApiResponse(result.success, result.message, mapOf(
                "configurationName" to result.configurationName
            ))
        } catch (e: Exception) {
            logger.error("Error starting app", e)
            ApiResponse(false, "Error starting app: ${e.message}")
        }
    }

    private suspend fun handleStopApp(body: String?): ApiResponse {
        return try {
            val request = body?.let { objectMapper.readValue(it, ApiRequest::class.java) }
            val androidService = com.intellij.openapi.components.service<AndroidControlService>()
            val result = androidService.stopApp(request?.projectPath).get()
            
            ApiResponse(result.success, result.message)
        } catch (e: Exception) {
            logger.error("Error stopping app", e)
            ApiResponse(false, "Error stopping app: ${e.message}")
        }
    }

    private suspend fun handleRerunApp(body: String?): ApiResponse {
        return try {
            val request = body?.let { objectMapper.readValue(it, ApiRequest::class.java) }
            val androidService = com.intellij.openapi.components.service<AndroidControlService>()
            val result = androidService.rerunApp(request?.projectPath).get()
            
            ApiResponse(result.success, result.message, mapOf(
                "configurationName" to result.configurationName
            ))
        } catch (e: Exception) {
            logger.error("Error rerunning app", e)
            ApiResponse(false, "Error rerunning app: ${e.message}")
        }
    }

    private suspend fun handleDebugApp(body: String?): ApiResponse {
        return try {
            val request = body?.let { objectMapper.readValue(it, ApiRequest::class.java) }
            val androidService = com.intellij.openapi.components.service<AndroidControlService>()
            val result = androidService.debugApp(request?.projectPath).get()
            
            ApiResponse(result.success, result.message, mapOf(
                "configurationName" to result.configurationName
            ))
        } catch (e: Exception) {
            logger.error("Error debugging app", e)
            ApiResponse(false, "Error debugging app: ${e.message}")
        }
    }

    private fun handleGetConfigurations(body: String?): ApiResponse {
        return try {
            val request = body?.let { objectMapper.readValue(it, ApiRequest::class.java) }
            val androidService = com.intellij.openapi.components.service<AndroidControlService>()
            val configurations = androidService.getRunConfigurations(request?.projectPath)
            
            ApiResponse(true, "Retrieved ${configurations.size} Android configurations", mapOf(
                "configurations" to configurations
            ))
        } catch (e: Exception) {
            logger.error("Error getting configurations", e)
            ApiResponse(false, "Error getting configurations: ${e.message}")
        }
    }

    private fun handleSelectConfiguration(body: String?): ApiResponse {
        return try {
            val request = body?.let { objectMapper.readValue(it, ApiRequest::class.java) }
                ?: return ApiResponse(false, "Configuration name is required")
            
            if (request.configurationName.isNullOrEmpty()) {
                return ApiResponse(false, "Configuration name cannot be empty")
            }
            
            val androidService = com.intellij.openapi.components.service<AndroidControlService>()
            val result = androidService.selectConfiguration(request.configurationName, request.projectPath)
            
            ApiResponse(result.success, result.message)
        } catch (e: Exception) {
            logger.error("Error selecting configuration", e)
            ApiResponse(false, "Error selecting configuration: ${e.message}")
        }
    }

    private fun handleGetStatus(): ApiResponse {
        return ApiResponse(true, "Run-API-Server is running", mapOf(
            "port" to port,
            "version" to "1.0.0",
            "endpoints" to listOf(
                "POST /api/start - Start Android app",
                "POST /api/stop - Stop Android app", 
                "POST /api/rerun - Rerun Android app",
                "POST /api/debug - Debug Android app",
                "GET /api/configurations - Get run configurations",
                "POST /api/select-configuration - Select run configuration",
                "GET /api/status - Get server status"
            )
        ))
    }

    fun getServerStatus(): ApiResponse {
        return if (isRunning) {
            ApiResponse(true, "Server is running on port $port")
        } else {
            ApiResponse(false, "Server is not running")
        }
    }
}