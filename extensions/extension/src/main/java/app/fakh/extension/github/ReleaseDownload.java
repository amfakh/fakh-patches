/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.extension.github;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.util.Locale;

import app.fakh.extension.shared.AppRuntime;
import app.fakh.extension.shared.Features;
import app.fakh.extension.shared.I18n;

/**
 * Downloads release assets through Android's {@link DownloadManager} instead of handing the URL off to the browser,
 * falling back to the original chooser intent if enqueueing fails.
 */
@SuppressWarnings("unused")
public final class ReleaseDownload {
    static final String FOLDER_KEY = "fakh_download_folder";
    static final String DEFAULT_FOLDER = "GitHub";
    private static final String TAG = "FakhReleaseDownload";
    private static String pendingAsset;

    private ReleaseDownload() {
    }

    /** Captures the {@code ReleaseAsset} metadata right before the download branch builds its view intent. */
    public static void prepare(Object asset) {
        pendingAsset = asset == null ? null : asset.toString();
    }

    /** Subfolder under Download, or empty to save directly in Download. */
    static String folder() {
        return sanitize(Features.text(FOLDER_KEY, DEFAULT_FOLDER));
    }

    /** Relative path shown to the user, such as {@code Download/GitHub}. */
    static String location() {
        String sub = folder();
        return sub.isEmpty() ? Environment.DIRECTORY_DOWNLOADS : Environment.DIRECTORY_DOWNLOADS + "/" + sub;
    }

    /** Strips leading/trailing slashes and dot segments so DownloadManager accepts the relative path. */
    static String sanitize(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder();
        for (String part : raw.split("[/\\\\]+")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty() || ".".equals(trimmed) || "..".equals(trimmed)) continue;
            if (out.length() > 0) out.append('/');
            out.append(trimmed);
        }
        return out.toString();
    }

    /** Enqueues the release asset URL from {@code intent} into the system download manager. */
    public static void download(Intent intent) {
        String assetInfo = pendingAsset;
        pendingAsset = null;
        Context context = AppRuntime.application();
        if (context == null || intent == null) return;
        Intent target = Intent.ACTION_CHOOSER.equals(intent.getAction())
                ? intent.getParcelableExtra(Intent.EXTRA_INTENT)
                : intent;
        Uri uri = target == null ? null : target.getData();
        if (uri == null) {
            startChooser(context, intent);
            return;
        }
        String fileName = field(assetInfo, "name=", ", size=");
        if (fileName == null || fileName.isEmpty()) fileName = fallbackName(uri);
        String contentType = field(assetInfo, "contentType=", ")");
        String sub = folder();
        String subPath = sub.isEmpty() ? fileName : sub + "/" + fileName;
        try {
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) {
                startChooser(context, intent);
                return;
            }
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setTitle(fileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, subPath);
            String mime = mimeType(fileName, contentType);
            if (mime != null) request.setMimeType(mime);
            manager.enqueue(request);
            Toast.makeText(context, I18n.f("Downloading to %1$s", location()), Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            Log.w(TAG, "Download", error);
            startChooser(context, intent);
        }
    }

    private static String field(String info, String startKey, String endKey) {
        if (info == null) return null;
        int start = info.indexOf(startKey);
        if (start < 0) return null;
        start += startKey.length();
        int end = info.indexOf(endKey, start);
        if (end <= start) return null;
        return info.substring(start, end).trim();
    }

    private static String fallbackName(Uri uri) {
        String last = uri.getLastPathSegment();
        if (last == null || last.trim().isEmpty()) return "download";
        int slash = last.lastIndexOf('/');
        if (slash >= 0) last = last.substring(slash + 1);
        return last.isEmpty() ? "download" : last;
    }

    private static String mimeType(String fileName, String contentType) {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
            String mapped = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (mapped != null) return mapped;
        }
        if (contentType != null && !contentType.isEmpty() && !"application/octet-stream".equals(contentType)) {
            return contentType;
        }
        return null;
    }

    private static void startChooser(Context context, Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable error) {
            Log.w(TAG, "Fallback chooser", error);
        }
    }
}
