package com.syndicati.services.mail;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Native Java email UI composer (no external template files).
 *
 * Produces HTML with the same visual direction as the Horizon mail UI.
 */
public final class SyndicatiEmailComposer {

    private static final String BASE_CSS =
        "body{margin:0;padding:0;font-family:Inter,-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background-color:#0c0c0c;color:#ffffff;-webkit-font-smoothing:antialiased;}" +
        ".wrapper{width:100%;table-layout:fixed;background-color:#0c0c0c;padding:60px 0;}" +
        ".main{background-color:#0c0c0c;margin:0 auto;width:95%;max-width:850px;border-spacing:0;border-radius:48px;overflow:hidden;border:1px solid rgba(255,75,92,0.12);box-shadow:0 40px 100px rgba(0,0,0,0.8);}" +
        ".header{padding:80px 0 60px 0;text-align:center;background:radial-gradient(circle at top, rgba(255,75,92,0.1) 0%, transparent 60%);}" +
        ".logo-text{font-size:38px;font-weight:900;letter-spacing:-2px;color:#ffffff;margin:0;text-transform:uppercase;}" +
        ".accent-dot{color:#ff4b5c;display:inline-block;}" +
        ".content{padding:0 60px 80px 60px;font-size:18px;line-height:1.8;text-align:left;}" +
        "h1,h2,h3{color:#ffffff;margin-top:0;font-weight:800;letter-spacing:-0.5px;}" +
        "p{margin-bottom:24px;color:rgba(255,255,255,0.7);}" +
        ".button-container{text-align:center;margin:40px 0;}" +
        ".button{display:inline-block;background:rgba(255,75,92,0.1);border:1px solid rgba(255,75,92,0.4);color:#ff4b5c !important;padding:16px 36px;border-radius:16px;text-decoration:none;font-weight:800;font-size:16px;text-transform:uppercase;letter-spacing:1px;box-shadow:0 0 20px rgba(255,75,92,0.1);}" +
        ".footer{padding:40px;text-align:center;font-size:13px;color:rgba(255,255,255,0.4);border-top:1px solid rgba(255,255,255,0.05);}" +
        ".footer p{margin:5px 0;color:rgba(255,255,255,0.4);}" +
        "@media screen and (max-width:600px){.content{padding:40px 25px;}.button{width:100%;box-sizing:border-box;}.main{border-radius:0;margin-top:0;}}";

    private SyndicatiEmailComposer() {
    }

    public static String twoFactorCode(String firstName, String code) {
        String body =
            "<h2 style='color:#ff4b5c;'>Security Protocol</h2>" +
            "<p>Hello " + esc(nameOrFallback(firstName)) + ",</p>" +
            "<p>We received a request to access or reset your Syndicati account. Please use the following authorization code to verify your identity.</p>" +
            "<div style='background:rgba(255,75,92,0.05);padding:40px;border-radius:24px;border:1px solid rgba(255,75,92,0.2);margin:35px 0;text-align:center;'>" +
            "<p style='margin:0 0 15px 0;font-size:13px;color:rgba(255,255,255,0.4);text-transform:uppercase;letter-spacing:2px;'>Your Authorization Code</p>" +
            "<div style='font-size:42px;font-weight:800;color:#ff4b5c;letter-spacing:8px;font-family:Courier New,monospace;'>" + esc(code) + "</div>" +
            "</div>" +
            "<div style='margin-top:25px;padding:20px;background:rgba(0,0,0,0.4);border-radius:16px;border-left:4px solid #ff4b5c;'>" +
            "<p style='margin:0;font-size:14px;color:rgba(255,255,255,0.7);line-height:1.6;'><strong>Warning:</strong> For your security, this code will expire in 15 minutes.</p>" +
            "</div>" +
            "<p style='font-size:14px;color:rgba(255,255,255,0.4);text-align:center;margin-top:40px;'>Syndicati Neural Network - Advanced Identity Protection</p>";

        String footerExtra = "<p style='margin-top:15px;font-style:italic;color:#ff4b5c;font-size:11px;'>Protocol: AUTH-2FA-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now()) + " - Encrypted Transmission</p>";
        return wrap(body, footerExtra);
    }

    public static String commentNotification(String authorFirstName, String commenterName, String publicationTitle, String commentSnippet) {
        String body =
            "<h2 style='color:#ff4b5c;'>New Discussion Activity</h2>" +
            "<p>Hello " + esc(nameOrFallback(authorFirstName)) + ",</p>" +
            "<p><strong>" + esc(commenterName) + "</strong> has just interacted with your publication.</p>" +
            "<div style='background:rgba(255,75,92,0.03);padding:35px;border-radius:24px;border:1px solid rgba(255,75,92,0.2);margin:35px 0;'>" +
            "<p style='font-size:12px;color:rgba(255,255,255,0.4);margin-bottom:10px;text-transform:uppercase;letter-spacing:1px;'>On Publication</p>" +
            "<h3 style='margin-bottom:20px;color:#ffffff;font-size:18px;letter-spacing:-0.5px;'>" + esc(publicationTitle) + "</h3>" +
            "<div style='background:rgba(0,0,0,0.4);padding:25px;border-radius:16px;border:1px solid rgba(255,255,255,0.05);line-height:1.6;color:rgba(255,255,255,0.8);font-style:italic;'>" +
            "&ldquo;" + esc(commentSnippet) + "&rdquo;" +
            "</div>" +
            "</div>" +
            "<div class='button-container'><a href='#' class='button'>Join Discussion</a></div>" +
            "<p style='font-size:14px;color:rgba(255,255,255,0.4);text-align:center;margin-top:40px;'>Syndicati Engagement Engine - Neural Notification System</p>";

        String footerExtra = "<p style='margin-top:15px;font-style:italic;color:#ff4b5c;font-size:11px;'>Ref: COM-NOT-" + DateTimeFormatter.ofPattern("HHmm").format(LocalDateTime.now()) + " - Real-time Protocol</p>";
        return wrap(body, footerExtra);
    }

    public static String publicationAnnouncement(String firstName, String title, String description, String authorName) {
        String body =
            "<h2 style='color:#ff4b5c;'>New Community Announcement</h2>" +
            "<p>Hello " + esc(nameOrFallback(firstName)) + ",</p>" +
            "<p>A new important announcement has been published in the Syndicati Community forum that requires your attention.</p>" +
            "<div style='background:rgba(255,75,92,0.03);padding:35px;border-radius:24px;border:1px solid rgba(255,75,92,0.2);margin:35px 0;'>" +
            "<h3 style='margin-bottom:15px;color:#ffffff;font-size:22px;letter-spacing:-0.5px;'>" + esc(title) + "</h3>" +
            "<p style='font-size:12px;color:rgba(255,255,255,0.4);margin-bottom:20px;text-transform:uppercase;letter-spacing:1px;'>Published by " + esc(authorName) + "</p>" +
            "<div style='background:rgba(0,0,0,0.4);padding:25px;border-radius:16px;border:1px solid rgba(255,255,255,0.05);line-height:1.6;color:rgba(255,255,255,0.8);'>" +
            esc(description).replace("\n", "<br>") +
            "</div>" +
            "</div>" +
            "<div class='button-container'><a href='#' class='button'>View Announcement</a></div>" +
            "<p style='font-size:14px;color:rgba(255,255,255,0.4);text-align:center;margin-top:40px;'>Syndicati Forum Services - Public Information Bureau</p>";

        String footerExtra = "<p style='margin-top:15px;font-style:italic;color:#ff4b5c;font-size:11px;'>Ref: ANN-PUB-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now()) + " - Global Broadcast</p>";
        return wrap(body, footerExtra);
    }

    public static String passwordReset(String firstName, String newPassword) {
        String body =
            "<h2 style='color:#ff4b5c;'>Account Restored</h2>" +
            "<p>Hello " + esc(nameOrFallback(firstName)) + ",</p>" +
            "<p>Your password reset has been successfully verified. Your account access has been restored with a newly generated secure credential.</p>" +
            "<div style='background:rgba(255,75,92,0.03);padding:35px;border-radius:24px;border:1px solid rgba(255,75,92,0.2);margin:35px 0;'>" +
            "<h3 style='margin-bottom:20px;color:#ff4b5c;font-size:18px;letter-spacing:-0.5px;'>New Temporary Credential</h3>" +
            "<div style='background:rgba(0,0,0,0.6);padding:20px;border-radius:12px;text-align:center;margin-bottom:20px;border:1px solid rgba(255,255,255,0.05);'>" +
            "<p style='margin:0 0 8px 0;font-size:12px;color:rgba(255,255,255,0.3);text-transform:uppercase;'>Password</p>" +
            "<code style='font-size:24px;color:#ffffff;font-weight:700;letter-spacing:1px;'>" + esc(newPassword) + "</code>" +
            "</div>" +
            "<div style='padding:15px;background:rgba(255,75,92,0.1);border-radius:12px;border:1px solid rgba(255,75,92,0.2);'>" +
            "<p style='margin:0;font-size:13px;color:#ff4b5c;line-height:1.5;text-align:center;'><strong>Action Required:</strong> Please sign in and update your password immediately from your profile settings.</p>" +
            "</div>" +
            "</div>" +
            "<div class='button-container'><a href='#' class='button'>Log In Now</a></div>" +
            "<p style='font-size:14px;color:rgba(255,255,255,0.4);text-align:center;margin-top:40px;'>Syndicati Security Services - Threat Mitigation Unit</p>";

        String footerExtra = "<p style='margin-top:15px;font-style:italic;color:#ff4b5c;font-size:11px;'>Ref: PWD-RST-" + DateTimeFormatter.ofPattern("HHmmss").format(LocalDateTime.now()) + " - Zero Trust Protocol</p>";
        return wrap(body, footerExtra);
    }

    public static String reclamationNotification(String firstName, String reclamationTitle, String userName, String reclamationDescription) {
        String body =
            "<h2 style='color:#ff4b5c;'>New Support Request</h2>" +
            "<p>Hello " + esc(nameOrFallback(firstName)) + ",</p>" +
            "<p>A new reclamation has been filed in the Syndicati system that requires review from an administrator or syndic.</p>" +
            "<div style='background:rgba(255,75,92,0.03);padding:35px;border-radius:24px;border:1px solid rgba(255,75,92,0.2);margin:35px 0;'>" +
            "<h3 style='margin-bottom:10px;color:#ffffff;font-size:20px;letter-spacing:-0.5px;'>" + esc(reclamationTitle) + "</h3>" +
            "<p style='font-size:12px;color:rgba(255,255,255,0.4);margin-bottom:20px;text-transform:uppercase;letter-spacing:1px;'>Filed by " + esc(userName) + "</p>" +
            "<div style='background:rgba(0,0,0,0.4);padding:25px;border-radius:16px;border:1px solid rgba(255,255,255,0.05);line-height:1.6;color:rgba(255,255,255,0.8);'>" +
            esc(reclamationDescription).replace("\n", "<br>") +
            "</div>" +
            "</div>" +
            "<div class='button-container'><a href='#' class='button'>Open Dashboard</a></div>" +
            "<p style='font-size:14px;color:rgba(255,255,255,0.4);text-align:center;margin-top:40px;'>Syndicati Operations - Priority Management System</p>";

        String footerExtra = "<p style='margin-top:15px;font-style:italic;color:#ff4b5c;font-size:11px;'>Ref: REC-NEW-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now()) + " - Neural Transmission</p>";
        return wrap(body, footerExtra);
    }

    public static String responseNotification(String firstName, String reclamationTitle, String adminName, String responseTitle, String responseMessage, String responseImage) {
        String body =
            "<h2 style='color:#ff4b5c;'>Resolution Update</h2>" +
            "<p>Hello " + esc(nameOrFallback(firstName)) + ",</p>" +
            "<p>A response has been posted regarding your reclamation: <strong>" + esc(reclamationTitle) + "</strong>.</p>" +
            "<div style='background:rgba(255,75,92,0.03);padding:35px;border-radius:24px;border:1px solid rgba(255,75,92,0.2);margin:35px 0;text-align:center;'>" +
            "<p style='font-size:12px;color:rgba(255,255,255,0.4);margin-bottom:10px;text-transform:uppercase;letter-spacing:1px;'>Response from " + esc(adminName) + "</p>" +
            "<h3 style='margin-bottom:25px;color:#ffffff;font-size:24px;letter-spacing:-0.5px;'>" + esc(responseTitle) + "</h3>" +
            "<div style='padding:20px;background:rgba(255,75,92,0.1);border-radius:16px;border:1px solid rgba(255,75,92,0.2);'>" +
            "<p style='margin:0;font-size:15px;color:#ff4b5c;line-height:1.6;'>For more information and to view the full response details, please visit our application.</p>" +
            "</div>" +
            "</div>" +
            "<div class='button-container'><a href='#' class='button'>Open Application</a></div>" +
            "<p style='font-size:14px;color:rgba(255,255,255,0.4);text-align:center;margin-top:40px;'>Syndicati Operations - Priority Communication Unit</p>";

        String footerExtra = "<p style='margin-top:15px;font-style:italic;color:#ff4b5c;font-size:11px;'>Ref: REP-LNK-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now()) + " - Direct Access Protocol</p>";
        return wrap(body, footerExtra);
    }

    public static String statusChangeNotification(String firstName, String reclamationTitle, String newStatus) {
        String displayStatus = newStatus.replace("_", " ").toUpperCase();
        String body =
            "<h2 style='color:#ff4b5c;'>Status Progress Update</h2>" +
            "<p>Hello " + esc(nameOrFallback(firstName)) + ",</p>" +
            "<p>The status of your reclamation <strong>\"" + esc(reclamationTitle) + "\"</strong> has been updated.</p>" +
            "<div style='background:rgba(255,75,92,0.03);padding:35px;border-radius:24px;border:1px solid rgba(255,75,92,0.2);margin:35px 0;text-align:center;'>" +
            "<p style='font-size:12px;color:rgba(255,255,255,0.4);margin-bottom:10px;text-transform:uppercase;letter-spacing:1px;'>Current Status</p>" +
            "<div style='display:inline-block;padding:12px 30px;background:rgba(255,75,92,0.1);border-radius:12px;border:1px solid rgba(255,75,92,0.3);'>" +
            "<h3 style='margin:0;color:#ff4b5c;font-size:22px;letter-spacing:1px;'>" + esc(displayStatus) + "</h3>" +
            "</div>" +
            "<p style='margin-top:25px;font-size:15px;color:rgba(255,255,255,0.7);line-height:1.6;'>Our team is actively processing your request. You can track the real-time progress of your reclamation in our application.</p>" +
            "</div>" +
            "<div class='button-container'><a href='#' class='button'>Track Progress</a></div>" +
            "<p style='font-size:14px;color:rgba(255,255,255,0.4);text-align:center;margin-top:40px;'>Syndicati Workflow - Status Monitoring Division</p>";

        String footerExtra = "<p style='margin-top:15px;font-style:italic;color:#ff4b5c;font-size:11px;'>Ref: STA-UPD-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now()) + " - Status Sync</p>";
        return wrap(body, footerExtra);
    }

    private static String wrap(String body, String footerExtra) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><style>"
            + BASE_CSS
            + "</style></head><body>"
            + "<div class='wrapper'><table class='main' align='center'><tr><td class='header'><h1 class='logo-text'>Syndicati<span class='accent-dot'>.</span></h1></td></tr>"
            + "<tr><td class='content'>" + body + "</td></tr>"
            + "<tr><td class='footer'>"
            + "<p>&copy; " + DateTimeFormatter.ofPattern("yyyy").format(LocalDateTime.now()) + " Syndicati Community. All rights reserved.</p>"
            + "<p style='letter-spacing:2px;text-transform:uppercase;font-size:10px;margin-top:15px;color:rgba(255,75,92,0.5);'>Obsidian Engine - Red Protocol</p>"
            + footerExtra
            + "</td></tr></table></div></body></html>";
    }

    private static String nameOrFallback(String value) {
        return value == null || value.isBlank() ? "User" : value;
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}

