package ai.gbox.plugin.services

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.android.tools.idea.run.AndroidRunConfiguration
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ExecutionManager
import com.intellij.openapi.components.ServiceManager

@Service(Service.Level.APP)
class AndroidControlService {
    // Helper to await a CompletableFuture inside coroutines without blocking
    private suspend fun <T> CompletableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
        this.whenComplete { result, exc ->
            if (exc == null) cont.resume(result) else cont.resumeWithException(exc)
        }
        cont.invokeOnCancellation { this.cancel(true) }
    }

    // Use Default dispatcher for background work to avoid blocking the UI thread.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    data class ExecutionResult(
        val success: Boolean,
        val message: String,
        val configurationName: String? = null
    )

    fun startApp(projectPath: String? = null): CompletableFuture<ExecutionResult> {
        val future = CompletableFuture<ExecutionResult>()
        
        serviceScope.launch {
            try {
                val project = getProject(projectPath)
                if (project == null) {
                    future.complete(ExecutionResult(false, "Project not found or not open"))
                    return@launch
                }
                
                val runManager = RunManager.getInstance(project)
                val selectedConfiguration = runManager.selectedConfiguration
                
                if (selectedConfiguration == null) {
                    future.complete(ExecutionResult(false, "No run configuration selected"))
                    return@launch
                }
                
                val configuration = selectedConfiguration.configuration
                if (configuration !is AndroidRunConfiguration) {
                    future.complete(ExecutionResult(false, "Selected configuration is not an Android configuration"))
                    return@launch
                }
                
                ApplicationManager.getApplication().invokeLater {
                    try {
                        val executor = DefaultRunExecutor.getRunExecutorInstance()
                        val runner = ProgramRunner.getRunner(executor.id, configuration)
                        
                        if (runner != null) {
                            ExecutionUtil.runConfiguration(selectedConfiguration, executor)
                            future.complete(ExecutionResult(
                                true, 
                                "Android app started successfully", 
                                configuration.name
                            ))
                        } else {
                            future.complete(ExecutionResult(false, "No suitable runner found for configuration"))
                        }
                    } catch (e: Exception) {
                        future.complete(ExecutionResult(false, "Failed to start app: ${e.message}"))
                    }
                }
            } catch (e: Exception) {
                future.complete(ExecutionResult(false, "Error: ${e.message}"))
            }
        }
        
        return future
    }

    fun stopApp(projectPath: String? = null): CompletableFuture<ExecutionResult> {
        val future = CompletableFuture<ExecutionResult>()
        
        serviceScope.launch {
            try {
                val project = getProject(projectPath)
                if (project == null) {
                    future.complete(ExecutionResult(false, "Project not found or not open"))
                    return@launch
                }
                
                ApplicationManager.getApplication().invokeLater {
                    try {
                        // Stop all running processes for the project
                        val executionManager = ExecutionManager.getInstance(project)
                        val runningProcesses = executionManager.getRunningProcesses()
                        
                        if (runningProcesses.isEmpty()) {
                            future.complete(ExecutionResult(false, "No running processes found"))
                            return@invokeLater
                        }
                        
                        runningProcesses.forEach { process ->
                            process.destroyProcess()
                        }
                        
                        future.complete(ExecutionResult(
                            true, 
                            "Stopped ${runningProcesses.size} running process(es)"
                        ))
                    } catch (e: Exception) {
                        future.complete(ExecutionResult(false, "Failed to stop app: ${e.message}"))
                    }
                }
            } catch (e: Exception) {
                future.complete(ExecutionResult(false, "Error: ${e.message}"))
            }
        }
        
        return future
    }

    fun rerunApp(projectPath: String? = null): CompletableFuture<ExecutionResult> {
        val future = CompletableFuture<ExecutionResult>()
        
        serviceScope.launch {
            try {
                // First stop the app (await without blocking UI thread)
                val stopResult = stopApp(projectPath).await()

                // Wait a moment for cleanup
                kotlinx.coroutines.delay(1000)

                // Then start it again
                val startResult = startApp(projectPath).await()
                
                if (startResult.success) {
                    future.complete(ExecutionResult(
                        true, 
                        "App rerun successful: ${startResult.message}", 
                        startResult.configurationName
                    ))
                } else {
                    future.complete(ExecutionResult(
                        false, 
                        "Rerun failed - Stop: ${stopResult.message}, Start: ${startResult.message}"
                    ))
                }
            } catch (e: Exception) {
                future.complete(ExecutionResult(false, "Rerun error: ${e.message}"))
            }
        }
        
        return future
    }

    fun debugApp(projectPath: String? = null): CompletableFuture<ExecutionResult> {
        // Uses background dispatcher; UI interactions are wrapped with invokeLater

        val future = CompletableFuture<ExecutionResult>()
        
        serviceScope.launch {
            try {
                val project = getProject(projectPath)
                if (project == null) {
                    future.complete(ExecutionResult(false, "Project not found or not open"))
                    return@launch
                }
                
                val runManager = RunManager.getInstance(project)
                val selectedConfiguration = runManager.selectedConfiguration
                
                if (selectedConfiguration == null) {
                    future.complete(ExecutionResult(false, "No run configuration selected"))
                    return@launch
                }
                
                val configuration = selectedConfiguration.configuration
                if (configuration !is AndroidRunConfiguration) {
                    future.complete(ExecutionResult(false, "Selected configuration is not an Android configuration"))
                    return@launch
                }
                
                ApplicationManager.getApplication().invokeLater {
                    try {
                        val executor = DefaultDebugExecutor.getDebugExecutorInstance()
                        val runner = ProgramRunner.getRunner(executor.id, configuration)
                        
                        if (runner != null) {
                            ExecutionUtil.runConfiguration(selectedConfiguration, executor)
                            future.complete(ExecutionResult(
                                true, 
                                "Android app debug session started successfully", 
                                configuration.name
                            ))
                        } else {
                            future.complete(ExecutionResult(false, "No suitable debug runner found for configuration"))
                        }
                    } catch (e: Exception) {
                        future.complete(ExecutionResult(false, "Failed to start debug session: ${e.message}"))
                    }
                }
            } catch (e: Exception) {
                future.complete(ExecutionResult(false, "Debug error: ${e.message}"))
            }
        }
        
        return future
    }

    fun getRunConfigurations(projectPath: String? = null): List<String> {
        val project = getProject(projectPath) ?: return emptyList()
        val runManager = RunManager.getInstance(project)
        
        return runManager.allSettings
            .filter { it.configuration is AndroidRunConfiguration }
            .map { it.name }
    }

    fun selectConfiguration(configurationName: String, projectPath: String? = null): ExecutionResult {
        // Runs quickly; no heavy work here

        val project = getProject(projectPath) 
            ?: return ExecutionResult(false, "Project not found or not open")
        
        val runManager = RunManager.getInstance(project)
        val targetConfiguration = runManager.allSettings
            .find { it.name == configurationName && it.configuration is AndroidRunConfiguration }
        
        return if (targetConfiguration != null) {
            runManager.selectedConfiguration = targetConfiguration
            ExecutionResult(true, "Configuration '$configurationName' selected")
        } else {
            ExecutionResult(false, "Android configuration '$configurationName' not found")
        }
    }

    private fun getProject(projectPath: String?): Project? {
        return if (projectPath != null) {
            ProjectManager.getInstance().openProjects.find { 
                it.basePath == projectPath 
            }
        } else {
            // If no explicit path is provided, use the first opened project (defaultProject has no run configs)
            ProjectManager.getInstance().openProjects.firstOrNull()
        }
    }

    fun dispose() {
        // Cleanup resources if needed
    }
}