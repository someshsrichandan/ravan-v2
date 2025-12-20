package com.security.ravan;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class RavanHttpServer extends NanoHTTPD {

    private final Context context;

    private static final String HTML_HEADER = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<title>Ravan Security</title>" +
            "<style>" +
            "* { margin: 0; padding: 0; box-sizing: border-box; }" +
            "body {" +
            "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;" +
            "background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);" +
            "min-height: 100vh;" +
            "color: #e8e8e8;" +
            "}" +
            ".container { max-width: 1200px; margin: 0 auto; padding: 20px; }" +
            ".header { text-align: center; padding: 30px 0; border-bottom: 1px solid rgba(255,255,255,0.1); margin-bottom: 30px; }"
            +
            ".header h1 { font-size: 2.5rem; background: linear-gradient(90deg, #e94560, #ff6b6b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 10px; }"
            +
            ".header p { color: #888; }" +
            ".nav { display: flex; gap: 15px; flex-wrap: wrap; justify-content: center; margin-bottom: 30px; }" +
            ".nav a { padding: 12px 24px; background: rgba(255,255,255,0.1); border-radius: 10px; color: #fff; text-decoration: none; transition: all 0.3s ease; border: 1px solid rgba(255,255,255,0.1); }"
            +
            ".nav a:hover { background: rgba(233, 69, 96, 0.3); border-color: #e94560; transform: translateY(-2px); }" +
            ".card { background: rgba(255,255,255,0.05); border-radius: 15px; padding: 25px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px); }"
            +
            ".file-list { list-style: none; }" +
            ".file-item { display: flex; align-items: center; padding: 15px; margin: 8px 0; background: rgba(255,255,255,0.03); border-radius: 10px; transition: all 0.3s ease; border: 1px solid transparent; }"
            +
            ".file-item:hover { background: rgba(255,255,255,0.08); border-color: rgba(233, 69, 96, 0.3); }" +
            ".file-icon { width: 45px; height: 45px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin-right: 15px; font-size: 1.3rem; }"
            +
            ".folder-icon { background: linear-gradient(135deg, #f39c12, #f1c40f); }" +
            ".file-icon-default { background: linear-gradient(135deg, #3498db, #2980b9); }" +
            ".file-icon-image { background: linear-gradient(135deg, #9b59b6, #8e44ad); }" +
            ".file-icon-video { background: linear-gradient(135deg, #e74c3c, #c0392b); }" +
            ".file-icon-audio { background: linear-gradient(135deg, #1abc9c, #16a085); }" +
            ".file-icon-doc { background: linear-gradient(135deg, #2ecc71, #27ae60); }" +
            ".file-info { flex: 1; }" +
            ".file-name { color: #fff; text-decoration: none; font-weight: 500; display: block; margin-bottom: 4px; }" +
            ".file-name:hover { color: #e94560; }" +
            ".file-meta { font-size: 0.85rem; color: #888; }" +
            ".breadcrumb { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; padding: 15px; background: rgba(0,0,0,0.2); border-radius: 10px; }"
            +
            ".breadcrumb a { color: #e94560; text-decoration: none; }" +
            ".breadcrumb span { color: #666; }" +
            "table { width: 100%; border-collapse: collapse; margin-top: 15px; }" +
            "th, td { padding: 15px; text-align: left; border-bottom: 1px solid rgba(255,255,255,0.1); }" +
            "th { background: rgba(233, 69, 96, 0.2); color: #e94560; font-weight: 600; }" +
            "tr:hover { background: rgba(255,255,255,0.03); }" +
            ".call-incoming { color: #2ecc71; }" +
            ".call-outgoing { color: #3498db; }" +
            ".call-missed { color: #e74c3c; }" +
            ".contact-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #e94560, #ff6b6b); display: flex; align-items: center; justify-content: center; font-weight: bold; margin-right: 12px; }"
            +
            ".empty-state { text-align: center; padding: 60px 20px; color: #888; }" +
            ".empty-state .icon { font-size: 4rem; margin-bottom: 20px; }" +
            "@media (max-width: 768px) { .header h1 { font-size: 1.8rem; } .nav a { padding: 10px 16px; font-size: 0.9rem; } th, td { padding: 10px 8px; font-size: 0.85rem; } }"
            +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "<div class=\"header\">" +
            "<h1>Ravan Security</h1>" +
            "<p>Device Security &amp; Recovery System</p>" +
            "</div>" +
            "<div class=\"nav\">" +
            "<a href=\"/\">Home</a>" +
            "<a href=\"/files\">Files</a>" +
            "<a href=\"/calls\">Call Logs</a>" +
            "<a href=\"/contacts\">Contacts</a>" +
            "</div>";

    private static final String HTML_FOOTER = "</div>" +
            "</body>" +
            "</html>";

    public RavanHttpServer(Context context, int port) {
        super(port);
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        try {
            if (uri.equals("/") || uri.isEmpty()) {
                return serveHome();
            } else if (uri.equals("/files") || uri.startsWith("/files/")) {
                return serveFiles(uri, params);
            } else if (uri.equals("/calls")) {
                return serveCallLogs();
            } else if (uri.equals("/contacts")) {
                return serveContacts();
            } else if (uri.startsWith("/download/")) {
                return serveDownload(uri);
            } else {
                return serve404();
            }
        } catch (Exception e) {
            return serveError(e.getMessage());
        }
    }

    private Response serveHome() {
        String ipv6 = MainActivity.getIPv6Address();
        String ipDisplay = (ipv6 != null ? ipv6 : "Not Available");

        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Device Status</h2>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;\">"
                +
                "<div style=\"padding: 20px; background: rgba(46, 204, 113, 0.1); border-radius: 10px; border-left: 4px solid #2ecc71;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">Server Status</div>" +
                "<div style=\"font-size: 1.3rem; font-weight: bold; color: #2ecc71;\">Online</div>" +
                "</div>" +
                "<div style=\"padding: 20px; background: rgba(52, 152, 219, 0.1); border-radius: 10px; border-left: 4px solid #3498db;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">Port</div>" +
                "<div style=\"font-size: 1.3rem; font-weight: bold; color: #3498db;\">8080</div>" +
                "</div>" +
                "<div style=\"padding: 20px; background: rgba(233, 69, 96, 0.1); border-radius: 10px; border-left: 4px solid #e94560;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">IPv6 Address</div>" +
                "<div style=\"font-size: 0.9rem; font-weight: bold; color: #e94560; word-break: break-all;\">"
                + ipDisplay + "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Quick Access</h2>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;\">"
                +
                "<a href=\"/files\" style=\"padding: 30px 20px; background: linear-gradient(135deg, rgba(52, 152, 219, 0.2), rgba(41, 128, 185, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(52, 152, 219, 0.3);\">"
                +
                "<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">&#128193;</div>" +
                "<div style=\"color: #3498db; font-weight: 600;\">File Manager</div>" +
                "</a>" +
                "<a href=\"/calls\" style=\"padding: 30px 20px; background: linear-gradient(135deg, rgba(46, 204, 113, 0.2), rgba(39, 174, 96, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(46, 204, 113, 0.3);\">"
                +
                "<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">&#128222;</div>" +
                "<div style=\"color: #2ecc71; font-weight: 600;\">Call Logs</div>" +
                "</a>" +
                "<a href=\"/contacts\" style=\"padding: 30px 20px; background: linear-gradient(135deg, rgba(155, 89, 182, 0.2), rgba(142, 68, 173, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(155, 89, 182, 0.3);\">"
                +
                "<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">&#128101;</div>" +
                "<div style=\"color: #9b59b6; font-weight: 600;\">Contacts</div>" +
                "</a>" +
                "</div>" +
                "</div>" +
                HTML_FOOTER;

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveFiles(String uri, Map<String, String> params) {
        String path = uri.equals("/files") ? "" : uri.substring(7);
        path = path.replace("%20", " ");

        File baseDir = Environment.getExternalStorageDirectory();
        File currentDir = new File(baseDir, path);

        if (!currentDir.exists()) {
            return serve404();
        }

        if (currentDir.isFile()) {
            return serveFileDownload(currentDir);
        }

        StringBuilder html = new StringBuilder(HTML_HEADER);

        // Breadcrumb
        html.append("<div class=\"breadcrumb\">");
        html.append("<a href=\"/files\">Storage</a>");

        if (!path.isEmpty()) {
            String[] parts = path.split("/");
            StringBuilder pathBuilder = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    pathBuilder.append("/").append(part);
                    html.append("<span>/</span>");
                    html.append("<a href=\"/files").append(pathBuilder).append("\">").append(part).append("</a>");
                }
            }
        }
        html.append("</div>");

        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">").append(path.isEmpty() ? "Storage" : currentDir.getName())
                .append("</h2>");

        File[] files = currentDir.listFiles();
        if (files != null && files.length > 0) {
            html.append("<ul class=\"file-list\">");

            // Sort: folders first, then files
            java.util.Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory())
                    return -1;
                if (!a.isDirectory() && b.isDirectory())
                    return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File file : files) {
                String fileName = file.getName();
                String filePath = path.isEmpty() ? fileName : path + "/" + fileName;
                String icon = getFileIcon(file);
                String iconClass = getFileIconClass(file);

                html.append("<li class=\"file-item\">");
                html.append("<div class=\"file-icon ").append(iconClass).append("\">").append(icon).append("</div>");
                html.append("<div class=\"file-info\">");
                html.append("<a class=\"file-name\" href=\"/files/").append(filePath).append("\">").append(fileName)
                        .append("</a>");
                html.append("<div class=\"file-meta\">");

                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    int count = subFiles != null ? subFiles.length : 0;
                    html.append("Folder - ").append(count).append(" items");
                } else {
                    html.append(formatFileSize(file.length())).append(" - ");
                    html.append(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(new Date(file.lastModified())));
                }

                html.append("</div></div>");

                if (file.isFile()) {
                    html.append("<a href=\"/download/").append(filePath)
                            .append("\" style=\"padding: 8px 16px; background: rgba(233, 69, 96, 0.2); border-radius: 8px; color: #e94560; text-decoration: none; font-size: 0.85rem;\">Download</a>");
                }

                html.append("</li>");
            }
            html.append("</ul>");
        } else {
            html.append(
                    "<div class=\"empty-state\"><div class=\"icon\">&#128237;</div><p>This folder is empty</p></div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveFileDownload(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            String mimeType = getMimeType(file.getName());
            Response response = newFixedLengthResponse(Response.Status.OK, mimeType, fis, file.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return response;
        } catch (Exception e) {
            return serveError("Cannot read file: " + e.getMessage());
        }
    }

    private Response serveDownload(String uri) {
        String path = uri.substring(10);
        path = path.replace("%20", " ");
        File baseDir = Environment.getExternalStorageDirectory();
        File file = new File(baseDir, path);

        if (!file.exists() || !file.isFile()) {
            return serve404();
        }

        return serveFileDownload(file);
    }

    private Response serveCallLogs() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Recent Call Logs</h2>");

        try {
            Cursor cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[] {
                            CallLog.Calls.NUMBER,
                            CallLog.Calls.CACHED_NAME,
                            CallLog.Calls.TYPE,
                            CallLog.Calls.DATE,
                            CallLog.Calls.DURATION
                    },
                    null, null,
                    CallLog.Calls.DATE + " DESC LIMIT 100");

            if (cursor != null && cursor.getCount() > 0) {
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>Type</th><th>Contact</th><th>Number</th><th>Date</th><th>Duration</th>");
                html.append("</tr></thead><tbody>");

                while (cursor.moveToNext()) {
                    String number = cursor.getString(0);
                    String name = cursor.getString(1);
                    int type = cursor.getInt(2);
                    long date = cursor.getLong(3);
                    int duration = cursor.getInt(4);

                    String typeIcon, typeClass;
                    switch (type) {
                        case CallLog.Calls.INCOMING_TYPE:
                            typeIcon = "Incoming";
                            typeClass = "call-incoming";
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            typeIcon = "Outgoing";
                            typeClass = "call-outgoing";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            typeIcon = "Missed";
                            typeClass = "call-missed";
                            break;
                        default:
                            typeIcon = "Other";
                            typeClass = "";
                    }

                    html.append("<tr>");
                    html.append("<td class=\"").append(typeClass).append("\">").append(typeIcon).append("</td>");
                    html.append("<td>").append(name != null ? name : "-").append("</td>");
                    html.append("<td>").append(number).append("</td>");
                    html.append("<td>").append(
                            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(date)))
                            .append("</td>");
                    html.append("<td>").append(formatDuration(duration)).append("</td>");
                    html.append("</tr>");
                }

                cursor.close();
                html.append("</tbody></table>");
            } else {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#128222;</div><p>No call logs found</p></div>");
            }
        } catch (SecurityException e) {
            html.append(
                    "<div class=\"empty-state\"><div class=\"icon\">&#128274;</div><p>Permission denied. Please grant call log permission in the app.</p></div>");
        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div><p>Error: ").append(e.getMessage())
                    .append("</p></div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveContacts() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Contacts</h2>");

        try {
            Cursor cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[] {
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            if (cursor != null && cursor.getCount() > 0) {
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>Name</th><th>Phone Number</th>");
                html.append("</tr></thead><tbody>");

                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    String initial = name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";

                    html.append("<tr>");
                    html.append("<td style=\"display: flex; align-items: center;\">");
                    html.append("<div class=\"contact-avatar\">").append(initial).append("</div>");
                    html.append("<span>").append(name != null ? name : "Unknown").append("</span>");
                    html.append("</td>");
                    html.append("<td>").append(number).append("</td>");
                    html.append("</tr>");
                }

                cursor.close();
                html.append("</tbody></table>");
            } else {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#128101;</div><p>No contacts found</p></div>");
            }
        } catch (SecurityException e) {
            html.append(
                    "<div class=\"empty-state\"><div class=\"icon\">&#128274;</div><p>Permission denied. Please grant contacts permission in the app.</p></div>");
        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div><p>Error: ").append(e.getMessage())
                    .append("</p></div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serve404() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#128269;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Page Not Found</h2>" +
                "<p>The requested page does not exist.</p>" +
                "<a href=\"/\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: linear-gradient(135deg, #e94560, #ff6b6b); border-radius: 10px; color: white; text-decoration: none;\">Go Home</a>"
                +
                "</div>" +
                "</div>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", html);
    }

    private Response serveError(String message) {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#9888;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Error</h2>" +
                "<p>" + message + "</p>" +
                "</div>" +
                "</div>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", html);
    }

    private String getFileIcon(File file) {
        if (file.isDirectory())
            return "&#128193;";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "&#128444;";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "&#127916;";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "&#127925;";
        if (name.matches(".*\\.(pdf)$"))
            return "&#128196;";
        if (name.matches(".*\\.(doc|docx|txt|rtf)$"))
            return "&#128221;";
        if (name.matches(".*\\.(xls|xlsx|csv)$"))
            return "&#128202;";
        if (name.matches(".*\\.(zip|rar|7z|tar|gz)$"))
            return "&#128230;";
        if (name.matches(".*\\.(apk)$"))
            return "&#128241;";
        return "&#128196;";
    }

    private String getFileIconClass(File file) {
        if (file.isDirectory())
            return "folder-icon";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "file-icon-image";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "file-icon-video";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "file-icon-audio";
        if (name.matches(".*\\.(pdf|doc|docx|txt|rtf|xls|xlsx|csv)$"))
            return "file-icon-doc";
        return "file-icon-default";
    }

    private String getMimeType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".gif"))
            return "image/gif";
        if (name.endsWith(".mp4"))
            return "video/mp4";
        if (name.endsWith(".mp3"))
            return "audio/mpeg";
        if (name.endsWith(".pdf"))
            return "application/pdf";
        if (name.endsWith(".zip"))
            return "application/zip";
        if (name.endsWith(".apk"))
            return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    private String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        if (size < 1024 * 1024)
            return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024)
            return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(int seconds) {
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
    }
}
