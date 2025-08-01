# Gbox Android Studio Plugin

A powerful Android Studio plugin that provides programmatic control over Android app execution through HTTP APIs and MCP (Model Context Protocol) integration. Perfect for automation, testing, and remote development workflows.

## Features

- **Start/Stop/Rerun/Debug** Android applications programmatically
- **HTTP REST API** for external control (port 8765)
- **MCP Integration** for Claude Code and other AI tools
- **Run Configuration Management** - list and select configurations
- **Menu Actions** - direct UI integration in Android Studio

## Installation

### From Source
1. Clone this repository
2. Open in IntelliJ IDEA or Android Studio
3. Run `./gradlew buildPlugin`
4. Install the generated plugin zip from `build/distributions/`

### Plugin Manager (Coming Soon)
The plugin will be available in the JetBrains Plugin Repository.

## Usage

### UI Actions
Access plugin features through the **Gbox** menu in Android Studio:
- **Start Android App** - Start the selected run configuration
- **Stop Android App** - Stop running processes  
- **Rerun Android App** - Stop and restart the app
- **Debug Android App** - Start debug session
- **Start/Stop API Server** - Control the HTTP API server

### HTTP API

Start the API server from the Gbox menu, then use these endpoints:

```bash
# Start the API server first (from Gbox menu or via action)

# Start Android app
curl -X POST http://localhost:8765/api/start \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "/path/to/project"}'

# Stop Android app  
curl -X POST http://localhost:8765/api/stop \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "/path/to/project"}'

# Rerun Android app
curl -X POST http://localhost:8765/api/rerun \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "/path/to/project"}'

# Debug Android app
curl -X POST http://localhost:8765/api/debug \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "/path/to/project"}'

# Get available configurations
curl http://localhost:8765/api/configurations

# Select a configuration
curl -X POST http://localhost:8765/api/select-configuration \
  -H "Content-Type: application/json" \
  -d '{"configurationName": "app", "projectPath": "/path/to/project"}'

# Check server status
curl http://localhost:8765/api/status
```

### MCP Integration (Claude Code)

The plugin provides MCP tools for integration with Claude Code and other AI assistants:

#### Available MCP Tools:
- `android_start_app` - Start Android application
- `android_stop_app` - Stop Android application  
- `android_rerun_app` - Rerun Android application
- `android_debug_app` - Start debug session
- `android_get_configurations` - List run configurations
- `android_select_configuration` - Select a run configuration

#### MCP Configuration

Create an MCP server configuration to use with Claude Code:

```json
{
  "mcpServers": {
    "gbox-android": {
      "command": "java",
      "args": [
        "-jar", 
        "/path/to/gbox-android-studio-plugin/build/libs/gbox-android-studio-plugin-1.0.0.jar",
        "--mcp"
      ]
    }
  }
}
```

Then you can use Claude Code to control your Android apps:

```
> Start the Android app in debug mode
> Stop the running Android application  
> Rerun the app with the "debug" configuration
> Show me all available run configurations
```

## API Reference

### Request/Response Format

All API endpoints return JSON responses in this format:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "configurationName": "app"
  }
}
```

### Error Responses

```json
{
  "success": false,
  "message": "Error description here"
}
```

### Parameters

- `projectPath` (optional): Path to the Android project. If not provided, uses the currently active project.
- `configurationName` (required for select-configuration): Name of the run configuration to select.

## Development

### Building the Plugin

```bash
./gradlew buildPlugin
```

### Running Tests

```bash
./gradlew test
```

### Running in Development

```bash
./gradlew runIde
```

## Requirements

- **Android Studio**: 2023.2+ (or IntelliJ IDEA with Android plugin)
- **Java**: 17+
- **Kotlin**: 1.9+
- **Android Projects**: Plugin works with any Android project with run configurations

## Architecture

The plugin consists of several key components:

- **AndroidControlService**: Core service for app lifecycle management
- **GboxApiService**: HTTP API server for external control
- **McpServer**: MCP protocol implementation for AI integration
- **Action Classes**: UI menu actions for direct user interaction
- **Plugin Configuration**: Manifest and service registrations

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and feature requests, please create an issue on GitHub.

## Changelog

### Version 1.0.0
- Initial release
- Support for start/stop/rerun/debug operations
- HTTP API endpoints for external control
- MCP integration for Claude Code
- UI menu actions in Android Studio