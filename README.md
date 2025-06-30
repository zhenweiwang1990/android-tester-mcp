# Android Tester MCP

A Model Context Protocol (MCP) plugin for automating Android devices using the Gbox SDK. This plugin provides tools for creating Android boxes, taking screenshots, performing AI-powered UI actions, and managing applications.

## Features

- **Create Android Boxes**: Spin up virtual or physical Android devices
- **Screenshot Capture**: Take screenshots of the current display
- **AI-Powered Actions**: Perform UI actions using natural language instructions
- **App Management**: Install, uninstall, and launch Android applications
- **Box Management**: List and get information about your Android boxes

## Setup

1. **Install Dependencies**
   ```bash
   npm install
   ```

2. **Set Environment Variables**
   
   Create a `.env` file in the project root:
   ```
   GBOX_API_KEY=your_gbox_api_key_here
   ```

3. **Build the Project**
   ```bash
   npm run build
   ```

## Usage

This is an MCP server that provides tools for Android automation. Once running, you can use the following tools:

### Config in Cursor

```
{
  "mcpServers": {
    "gbox-android": {
      "command": "node",
      "args": ["/YOU_PATH_TO_THIS_REPO/android-tester-mcp/dist/index.js"],
      "env": {
        "GBOX_API_KEY": "YOUR API KEY"
      }
    }
  }
}

```

### Available Tools

- **create_android_box**: Create a new Android box with specified configuration
- **list_boxes**: List all current boxes belonging to your API key
- **get_box**: Get detailed information about a specific box
- **get_screenshot**: Take a screenshot of the Android device
- **ai_action**: Perform UI actions using natural language (e.g., "click the login button")
- **install_apk**: Install an APK file from local path or URL
- **uninstall_apk**: Uninstall an app by package name
- **open_app**: Launch an installed application

## Requirements

- Node.js and npm
- Valid Gbox API key
- TypeScript (for development)

## Sample Prompts in Cursor

> Test the apk of this project, create an android environment on gbox like a user, just install it and test it by instructing the MCP tool ai_action. Keep developing and testing until all requirements are met.