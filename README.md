# 🛡️ Ravan Security - Anti-Theft Android Application

A simple yet powerful Android application that creates an HTTP server on port 8080, allowing remote access to your device via IPv6. Designed for anti-theft security and device recovery purposes.

![Android](https://img.shields.io/badge/Android-11%2B-green?logo=android)
![Java](https://img.shields.io/badge/Java-8-orange?logo=java)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 📋 Table of Contents

- [Features](#-features)
- [Screenshots](#-screenshots)
- [Requirements](#-requirements)
- [Installation](#-installation)
  - [Option 1: Install Pre-built APK](#option-1-install-pre-built-apk)
  - [Option 2: Build from Source](#option-2-build-from-source)
- [Usage](#-usage)
- [Permissions](#-permissions)
- [Project Structure](#-project-structure)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🌐 **HTTP Server** | Runs on port 8080 with a beautiful, responsive web interface |
| 📡 **IPv6 Support** | Access your device remotely via public IPv6 (no port forwarding needed!) |
| 📁 **File Manager** | Browse, view, and download files from device storage |
| 📞 **Call Logs** | View incoming, outgoing, and missed calls with details |
| 👥 **Contacts** | Access contact list with phone numbers |
| 🔄 **Background Service** | Runs as a foreground service with persistent notification |
| 🚀 **Auto-Start** | Automatically starts when device boots |
| 📱 **Android 11-15+** | Full compatibility with modern Android versions |

---

## 📱 Screenshots

The web interface features a modern dark theme with:
- Dashboard with device status
- File browser with folder navigation
- Call log viewer with call types
- Contacts list with avatars

---

## 📋 Requirements

### For Installing APK
- Android device running **Android 11 (API 30)** or higher
- Enable "Install from unknown sources"

### For Building from Source
- **Operating System**: Windows 10/11, macOS, or Linux
- **Java Development Kit (JDK)**: Version 8 or higher
- **Android SDK**: API Level 34 (Android 14)
- **Gradle**: 8.2 (included via wrapper)
- **Android Studio**: Arctic Fox (2020.3.1) or later (optional but recommended)

---

## 🔧 Installation

### Option 1: Install Pre-built APK

1. **Download** the `RavanSecurity-v1.0-signed.apk` file
2. **Transfer** to your Android device
3. **Enable** installation from unknown sources:
   - Go to **Settings** → **Security** → **Install unknown apps**
   - Select your file manager and enable **Allow from this source**
4. **Install** the APK by tapping on it
5. **Open** the app and grant all permissions

### Option 2: Build from Source

#### Step 1: Install Prerequisites

##### Windows

1. **Install JDK 8 or higher**:
   ```powershell
   # Using Chocolatey
   choco install openjdk11
   
   # Or download from: https://adoptium.net/
   ```

2. **Install Android SDK** (via Android Studio or Command Line Tools):
   - Download Android Studio: https://developer.android.com/studio
   - Or download Command Line Tools: https://developer.android.com/studio#command-tools

3. **Set Environment Variables**:
   ```powershell
   # Add to System Environment Variables
   JAVA_HOME = C:\Program Files\Java\jdk-11
   ANDROID_HOME = C:\Users\<username>\AppData\Local\Android\Sdk
   
   # Add to PATH
   %JAVA_HOME%\bin
   %ANDROID_HOME%\platform-tools
   %ANDROID_HOME%\tools
   ```

##### macOS

1. **Install JDK**:
   ```bash
   brew install openjdk@11
   ```

2. **Install Android SDK**:
   ```bash
   brew install --cask android-studio
   # Or
   brew install --cask android-commandlinetools
   ```

3. **Set Environment Variables** (add to `~/.zshrc` or `~/.bashrc`):
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home)
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
   ```

##### Linux (Ubuntu/Debian)

1. **Install JDK**:
   ```bash
   sudo apt update
   sudo apt install openjdk-11-jdk
   ```

2. **Install Android SDK**:
   ```bash
   # Download command line tools from Android website
   mkdir -p ~/android-sdk/cmdline-tools
   cd ~/android-sdk/cmdline-tools
   # Extract downloaded tools here
   
   # Install required SDK components
   ./bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```

3. **Set Environment Variables** (add to `~/.bashrc`):
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
   export ANDROID_HOME=$HOME/android-sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
   ```

#### Step 2: Clone the Repository

```bash
git clone https://github.com/yourusername/ravan-security.git
cd ravan-security
```

#### Step 3: Create local.properties

Create a `local.properties` file in the project root:

```properties
# Windows
sdk.dir=C:\\Users\\<username>\\AppData\\Local\\Android\\Sdk

# macOS
sdk.dir=/Users/<username>/Library/Android/sdk

# Linux
sdk.dir=/home/<username>/android-sdk
```

#### Step 4: Generate Signing Keystore

```bash
# Generate a new keystore for signing
keytool -genkeypair -v -keystore ravan-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ravan -storepass ravan123 -keypass ravan123 -dname "CN=Ravan Security, OU=Security, O=Ravan, L=Unknown, ST=Unknown, C=IN"
```

#### Step 5: Build the APK

##### Using Command Line

```bash
# Windows
.\gradlew.bat clean assembleRelease

# macOS / Linux
./gradlew clean assembleRelease
```

##### Using Android Studio

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
4. APK will be in `app/build/outputs/apk/release/`

#### Step 6: Install on Device

```bash
# Connect device via USB with debugging enabled
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 🚀 Usage

### Starting the Server

1. **Open** the Ravan Security app
2. **Grant** all requested permissions:
   - Storage access
   - Contacts
   - Call logs
   - Notifications (Android 13+)
3. For **Android 11+**, grant "All files access" when prompted
4. Tap **"Disable Battery Optimization"** for reliable background operation
5. Tap **"Start Server"**
6. **Copy** the displayed IPv6 URL

### Accessing Your Device

1. Open any web browser on another device
2. Enter the URL: `http://[your-ipv6-address]:8080`
3. Browse files, view call logs, or access contacts

### Example URL Format
```
http://[2001:db8:85a3::8a2e:370:7334]:8080
```

> **Note**: Both devices must have IPv6 connectivity. The accessing device must be able to reach your phone's IPv6 address.

---

## 🔐 Permissions

| Permission | Purpose | Android Version |
|------------|---------|-----------------|
| `INTERNET` | HTTP server operation | All |
| `ACCESS_NETWORK_STATE` | Network status check | All |
| `READ_EXTERNAL_STORAGE` | File access | ≤ Android 12 |
| `MANAGE_EXTERNAL_STORAGE` | Full file access | Android 11+ |
| `READ_MEDIA_*` | Media file access | Android 13+ |
| `READ_CALL_LOG` | Call history access | All |
| `READ_CONTACTS` | Contacts access | All |
| `FOREGROUND_SERVICE` | Background operation | Android 9+ |
| `POST_NOTIFICATIONS` | Service notification | Android 13+ |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on boot | All |
| `WAKE_LOCK` | Keep CPU awake | All |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent app kill | Android 6+ |

---

## 📁 Project Structure

```
ravan-v2/
├── app/
│   ├── build.gradle                 # App-level Gradle configuration
│   ├── proguard-rules.pro           # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml      # App manifest with permissions
│       ├── java/com/security/ravan/
│       │   ├── MainActivity.java        # Main UI & permission handling
│       │   ├── HttpServerService.java   # Foreground service
│       │   ├── RavanHttpServer.java     # HTTP server with web UI
│       │   └── BootReceiver.java        # Boot broadcast receiver
│       └── res/
│           ├── layout/
│           │   └── activity_main.xml    # Main activity layout
│           ├── drawable/                # Icons and backgrounds
│           └── values/                  # Colors, strings, themes
├── build.gradle                     # Root Gradle configuration
├── settings.gradle                  # Project settings
├── gradle.properties                # Gradle properties
├── gradlew                          # Gradle wrapper (Unix)
├── gradlew.bat                      # Gradle wrapper (Windows)
├── gradle/wrapper/
│   ├── gradle-wrapper.jar           # Gradle wrapper JAR
│   └── gradle-wrapper.properties    # Wrapper configuration
├── ravan-keystore.jks               # Signing keystore (git-ignored)
├── .gitignore                       # Git ignore rules
└── README.md                        # This file
```

---

## 🔧 Troubleshooting

### Build Issues

**Problem**: `SDK location not found`
```bash
# Create local.properties with your SDK path
echo "sdk.dir=/path/to/android/sdk" > local.properties
```

**Problem**: `Could not find com.android.tools.build:gradle`
```bash
# Ensure you have internet connection and run
./gradlew --refresh-dependencies
```

**Problem**: `Java version mismatch`
```bash
# Check Java version
java -version

# Set JAVA_HOME to correct version
export JAVA_HOME=/path/to/jdk
```

### Runtime Issues

**Problem**: Server not accessible
- Ensure both devices have IPv6 connectivity
- Check if firewall is blocking port 8080
- Verify the IPv6 address is correct (not link-local fe80::)

**Problem**: App killed in background
- Disable battery optimization for the app
- Lock the app in recent apps (device-specific)
- Some manufacturers have aggressive battery saving - check device settings

**Problem**: Permissions denied
- Go to Settings → Apps → Ravan Security → Permissions
- Grant all required permissions manually

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## ⚠️ Disclaimer

This application is intended for **legitimate anti-theft and device recovery purposes only**. 

- Use this application only on devices you own or have explicit permission to monitor
- Respect privacy laws in your jurisdiction
- The developers are not responsible for any misuse of this application

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Troubleshooting](#-troubleshooting) section
2. Open an issue on GitHub
3. Provide device model, Android version, and error logs

---

Made with ❤️ for device security
