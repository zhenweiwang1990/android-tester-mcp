package ai.gbox.plugin.listeners

import ai.gbox.plugin.services.GboxApiService
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.wm.IdeFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GboxApplicationListener : ApplicationActivationListener {
    private val logger = thisLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isServerStarted = false

    override fun applicationActivated(ideFrame: IdeFrame) {
        logger.info("Gbox Android Studio Plugin activated")
        
        // Auto-start API server if not already started
        if (!isServerStarted) {
            scope.launch {
                try {
                    val apiService = service<GboxApiService>()
                    val result = apiService.startApiServer()
                    
                    if (result.success) {
                        logger.info("Auto-started Run-API-Server on port 8765")
                        isServerStarted = true
                    } else {
                        logger.warn("Failed to auto-start API server: ${result.message}")
                    }
                } catch (e: Exception) {
                    logger.error("Error auto-starting API server", e)
                }
            }
        }
        
        super.applicationActivated(ideFrame)
    }

    override fun applicationDeactivated(ideFrame: IdeFrame) {
        super.applicationDeactivated(ideFrame)
    }
}