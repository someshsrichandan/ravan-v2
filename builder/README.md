# 🔥 Ravan APK Builder

<p align="center">
  <a href="https://github.com/someshsrichandan"><img src="https://img.shields.io/badge/Author-Somesh%20Srichandan-red?style=for-the-badge" alt="Author"></a>
</p>

---

## 👨‍💻 Developer

**Somesh **

- 🔗 GitHub: [github.com/someshsrichandan](https://github.com/someshsrichandan)
- 🔗 LinkedIn: [linkedin.com/in/someshsrichandan](https://linkedin.com/in/someshsrichandan)

---

## ✨ Features

| Feature                       | Description                                 |
| ----------------------------- | ------------------------------------------- |
| 🖥️ **Cross-Platform**         | Windows, Linux, macOS support               |
| ☕ **Auto Java Setup**        | Checks and helps install Java automatically |
| 🔐 **Certificate Generation** | Creates Android signing keystore            |
| 🎨 **Custom Logo**            | Replace app icon with your image            |
| 📝 **App Rename**             | Change app display name                     |
| 🔢 **Version Control**        | Set version name and code                   |
| 🌐 **Google Sheet URL**       | Configure webhook for data                  |
| 📦 **One-Click Build**        | Fully automated APK generation              |

---

## 🚀 Quick Start

### Windows

**PowerShell (Recommended)**

```powershell
cd builder
.\build.ps1
```

**Command Prompt**

```cmd
cd builder
build.bat
```

### Linux / macOS

```bash
cd builder
chmod +x build.sh
./build.sh
```

---

## 📋 Requirements

### Required

| Tool         | Version      | How to Get                           |
| ------------ | ------------ | ------------------------------------ |
| **Java JDK** | 11 or higher | Builder auto-installs or shows guide |

### Optional (for logo resizing)

| Tool            | Platform | Install                        |
| --------------- | -------- | ------------------------------ |
| **ImageMagick** | Linux    | `sudo apt install imagemagick` |
| **ImageMagick** | macOS    | `brew install imagemagick`     |

---

## 🛠️ Build Options

### 1️⃣ Full Build

Complete guided setup:

- Generates signing keystore
- Configures custom logo
- Sets app name
- Configures version
- Builds signed APK

### 2️⃣ Quick Build

Uses existing configuration from `build_config.json` to build immediately.

### 3️⃣ Generate Keystore Only

Creates Android signing certificate without building.

### 4️⃣ Configure Logo Only

Sets up custom app icon without full build.

### 5️⃣ Configure App Settings Only

Updates app name, version, Google Sheet URL.

### 6️⃣ Check/Install Requirements

Shows Java installation status and manual installation guide.

---

## ☕ Java Installation

### Automatic Installation

The builder attempts to install Java automatically:

| Platform          | Method                                |
| ----------------- | ------------------------------------- |
| **Windows**       | winget → Chocolatey → Manual download |
| **macOS**         | Homebrew                              |
| **Ubuntu/Debian** | apt                                   |
| **Fedora/RHEL**   | dnf                                   |
| **Arch Linux**    | pacman                                |

### Manual Installation

If auto-install fails:

**Windows**

```powershell
# Option 1: winget (Windows 11)
winget install EclipseAdoptium.Temurin.11.JDK

# Option 2: Chocolatey
choco install temurin11

# Option 3: Scoop
scoop bucket add java
scoop install temurin11-jdk

# Option 4: Manual download
# https://adoptium.net/temurin/releases/
```

**macOS**

```bash
# Homebrew
brew install openjdk@11
echo 'export PATH="/usr/local/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
```

**Ubuntu/Debian**

```bash
sudo apt update
sudo apt install openjdk-11-jdk
```

**Fedora/RHEL**

```bash
sudo dnf install java-11-openjdk-devel
```

**Arch Linux**

```bash
sudo pacman -S jdk11-openjdk
```

---

## 🎨 Logo Configuration

### Default Logo

The builder uses `ravanrat.png` from project root by default.

### Custom Logo

Provide path to any PNG image (512x512 recommended).

### Image Sizes

| Density | Size (pixels) |
| ------- | ------------- |
| mdpi    | 48 × 48       |
| hdpi    | 72 × 72       |
| xhdpi   | 96 × 96       |
| xxhdpi  | 144 × 144     |
| xxxhdpi | 192 × 192     |

### Transparent Background

On Linux/Mac with ImageMagick, the builder can remove white backgrounds.

### Online Tool

For best results, use [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html) to generate properly sized icons.

---

## 🔐 Keystore (Certificate)

### What is it?

Android requires all APKs to be signed with a certificate (keystore) for installation.

### Generated Details

| Property  | Default Value        |
| --------- | -------------------- |
| Algorithm | RSA 2048-bit         |
| Validity  | 25 years             |
| Format    | JKS                  |
| File      | `ravan-keystore.jks` |
| Alias     | `ravan-key`          |
| Password  | `ravan123`           |

### View Certificate

```bash
keytool -list -v -keystore ../ravan-keystore.jks
```

### ⚠️ Important

- **Backup your keystore!** If lost, you cannot update the app.
- Never share keystore password publicly.

---

## 📊 Google Sheet Integration

The builder saves the webhook URL to config. You need to set up the Google Sheet script manually (one time only).

### Setup Steps

1. **Create Google Sheet**
   - Go to [sheets.google.com](https://sheets.google.com)
   - Create new sheet

2. **Add Apps Script**
   - Extensions → Apps Script
   - Paste the webhook code (see main README)

3. **Deploy**
   - Deploy → New Deployment → Web App
   - Access: Anyone
   - Copy the URL

4. **Use in Builder**
   - Paste URL when prompted

---

## 📂 Output

Built APKs are saved to:

```
builder/output/
```

### Naming Format

```
{AppName}-v{Version}-{Timestamp}.apk
```

Example:

```
Ravan_Security-v2.0-20260118_203000.apk
```

---

## 📁 Config File

Settings are saved to `build_config.json`:

```json
{
  "KeystorePath": "..\\ravan-keystore.jks",
  "KeyAlias": "ravan-key",
  "KeystorePass": "ravan123",
  "AppName": "Ravan Security",
  "VersionName": "2.0",
  "VersionCode": 20,
  "SheetUrl": "https://script.google.com/..."
}
```

---

## 🛠️ Troubleshooting

### "Java not found"

- Run option 6 to see installation guide
- After installing, restart terminal

### "keytool not found"

- Ensure **JDK** (not JRE) is installed
- Add JDK bin to PATH

### "BUILD FAILED"

- Check internet connection (Gradle downloads dependencies)
- Run: `gradlew --stop` then `gradlew clean`
- Check for Android SDK license acceptance

### "Unsigned APK"

- Run Full Build (option 1) first to generate keystore
- Ensure keystore file exists

### Logo not changing

- Ensure destination folders exist in `res/`
- Try clearing Gradle cache: `gradlew clean`

---

## 📜 License

MIT License

---

<p align="center">
  <b>Created by Somesh Srichandan</b><br>
  <a href="https://github.com/someshsrichandan">GitHub</a> •
  <a href="https://linkedin.com/in/someshsrichandan">LinkedIn</a>
</p>
