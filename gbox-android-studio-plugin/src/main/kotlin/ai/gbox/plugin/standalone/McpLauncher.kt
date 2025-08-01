package ai.gbox.plugin.standalone

import ai.gbox.plugin.mcp.McpServer
import kotlinx.coroutines.runBlocking

/**
 * Standalone MCP server launcher for external use.
 * This can be used to run the MCP server outside of Android Studio
 * for testing or integration with external tools.
 */
object McpLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val mcpServer = McpServer()
        
        println("Starting Gbox MCP Server...")
        println("Use this with Claude Code or other MCP clients")
        
        runBlocking {
            try {
                mcpServer.handleStdioMcp()
            } catch (e: Exception) {
                System.err.println("MCP Server error: ${e.message}")
                e.printStackTrace()
            } finally {
                mcpServer.stop()
            }
        }
    }
}