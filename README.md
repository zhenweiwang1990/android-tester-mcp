# Android Tester MCP

A Model Context Protocol (MCP) plugin for controlling Android Studio and Android applications through the Gbox Android Studio Plugin. This plugin provides tools for starting, stopping, debugging, and managing Android applications directly from Claude Code.

## Features

- **Start Android Apps**: Launch Android applications in the current project
- **Stop Android Apps**: Stop currently running Android applications
- **Rerun Android Apps**: Stop and restart Android applications
- **Debug Android Apps**: Start debugging sessions for Android applications
- **Configuration Management**: List and select Android run configurations
- **API Server Status**: Check the status of the Android Studio plugin API server

## Prerequisites

1. **Android Studio Plugin**: Install the Gbox Android Studio Plugin
2. **API Server**: Start the API server from the Gbox menu in Android Studio (runs on port 8765)

## Setup

1. **Install Dependencies**
   ```bash
   npm install
   ```

2. **Build the Project**
   ```bash
   npm run build
   ```

## Usage

This is an MCP server that provides tools for Android Studio control. Once running, you can use the following tools:

### Config in Cursor

```
{
  "mcpServers": {
    "gbox-android": {
      "command": "node",
      "args": ["/YOU_PATH_TO_THIS_REPO/android-tester-mcp/dist/index.js"]
    }
  }
}
```

### Available Tools

- **android_start_app**: Start the Android application in the current project
- **android_stop_app**: Stop the currently running Android application
- **android_rerun_app**: Rerun the Android application (stop and start)
- **android_debug_app**: Start debugging the Android application
- **android_get_configurations**: Get list of available Android run configurations
- **android_select_configuration**: Select a specific Android run configuration
- **android_api_status**: Check the status of the Android Studio plugin API server

## Requirements

- Node.js and npm
- Android Studio with Gbox Android Studio Plugin installed
- Android Studio Plugin API server running on port 8765

## Sample Prompts in Cursor

> Start the Android app in debug mode

> Stop the running Android application

> Rerun the app with the "debug" configuration

> Show me all available run configurations

> Select the "app" configuration and start debugging

## How it Works

This MCP server communicates with the Gbox Android Studio Plugin via HTTP API calls to port 8765. The plugin must be installed in Android Studio and the API server must be running for the tools to work.

The server provides a bridge between Claude Code and Android Studio, allowing you to control Android application execution programmatically through natural language commands.
