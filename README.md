# 🛡️ Ravan Security

A simple Android app that turns your phone into a secure server you can access from anywhere using IPv6.

## 🚀 How to Use

1. **Install** the app on your Android phone.
2. **Open** it and grant the permissions (Storage, Contacts, etc.).
3. Tap **Start Server**.
4. You will see a URL like `http://[2405:201:...]:8080`.
5. Enter that URL in a browser on any other device to access your files and logs.

---

## 📊 Automatic IP Reporting (Google Sheets)

Since IPv6 addresses change often, you can set up a Google Sheet to automatically receive your phone's latest IP address.

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
3. Add your cloned URL inside it like this:
   ```properties
   WEBHOOK_URL=https://script.google.com/macros/s/YOUR-LONG-URL-HERE/exec
   ```
4. Build and install the app. Now, whenever your internet changes, the new IP will appear in your Google Sheet!

---

## ⚠️ Disclaimer

This tool is for **Mainly for Educational Purpose**. Use it only on your own devices.
