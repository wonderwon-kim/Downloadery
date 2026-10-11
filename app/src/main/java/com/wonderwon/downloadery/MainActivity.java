package com.wonderwon.downloadery;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.MimeTypeMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final String IDS = "ids";
    private static final String URL_PREFIX = "url_";
    private static final String NAME_PREFIX = "name_";
    private DownloadManager manager;
    private EditText urlInput;
    private EditText filenameInput;
    private LinearLayout history;
    private final Map<Long, TextView> rows = new LinkedHashMap<>();
    private final ArrayList<Long> ids = new ArrayList<>();
    private String pendingUrl;
    private String pendingName;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != -1) refreshRow(id);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    RECEIVER_EXPORTED);
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
            }
        } else {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 11);
        }
        buildUi();
        readHistory();
        handleIncomingIntent(getIntent());
    }

    @Override protected void onDestroy() {
        unregisterReceiver(downloadReceiver);
        super.onDestroy();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(10));
        root.setBackgroundColor(getColor(com.wonderwon.downloadery.R.color.background));

        TextView title = label("Downloadery", 28, R.color.text_primary);
        root.addView(title);
        TextView subtitle = label("A reliable, general-purpose Android downloader", 14, R.color.text_secondary);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(36)));

        urlInput = input(getString(R.string.url_hint), false);
        root.addView(urlInput);
        filenameInput = input(getString(R.string.filename_hint), false);
        root.addView(filenameInput, new LinearLayout.LayoutParams(-1, dp(54)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button paste = button(getString(R.string.paste));
        paste.setOnClickListener(v -> pasteUrl());
        Button download = button(getString(R.string.download));
        download.setOnClickListener(v -> enqueue());
        actions.addView(paste);
        actions.addView(download);
        root.addView(actions, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView heading = label(getString(R.string.history), 18, R.color.text_primary);
        heading.setPadding(0, dp(10), 0, dp(8));
        root.addView(heading);
        ScrollView scroll = new ScrollView(this);
        history = new LinearLayout(this);
        history.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(history);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void enqueue() {
        String value = urlInput.getText().toString().trim();
        if (!isHttpUrl(value)) {
            toast("Enter a valid http:// or https:// URL");
            return;
        }
        String name = filenameInput.getText().toString().trim();
        if (name.isEmpty()) name = fileNameFromUrl(value);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingUrl = value;
            pendingName = name;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 11);
            return;
        }
        startDownload(value, name);
    }

    private void startDownload(String value, String name) {
        long id = queueDownload(value, name);
        ids.add(id);
        getPreferences(MODE_PRIVATE).edit()
                .putString(IDS, joinIds())
                .putString(URL_PREFIX + id, value)
                .putString(NAME_PREFIX + id, name)
                .apply();
        addRow(id, name);
        urlInput.setText("");
        filenameInput.setText("");
        toast("Download queued");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 11 && pendingUrl != null) {
            String value = pendingUrl;
            String name = pendingName;
            pendingUrl = null;
            pendingName = null;
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload(value, name);
            } else {
                toast("Storage permission is required on Android 9 and older");
            }
        }
    }

    private void readHistory() {
        String saved = getPreferences(MODE_PRIVATE).getString(IDS, "");
        if (saved.isEmpty()) return;
        for (String part : saved.split(",")) {
            try { addRow(Long.parseLong(part), null); } catch (NumberFormatException ignored) { }
        }
    }

    private void addRow(long id, String suppliedName) {
        if (rows.containsKey(id)) return;
        String name = suppliedName;
        if (name == null) {
            name = getPreferences(MODE_PRIVATE).getString(NAME_PREFIX + id, "Download " + id);
        }
        TextView row = label(name + "\nChecking status…", 15, R.color.text_primary);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackgroundColor(getColor(R.color.surface));
        row.setOnClickListener(v -> openDownloadedFile(id));
        row.setOnLongClickListener(v -> handleLongPress(id));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, 0, 0, dp(6));
        history.addView(row, 0, rowParams);
        rows.put(id, row);
        refreshRow(id);
    }

    private long queueDownload(String value, String name) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(value));
        request.setTitle(name);
        request.setDescription(value);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(false);
        request.setVisibleInDownloadsUi(true);
        String mimeType = mimeTypeFor(name);
        if (!mimeType.isEmpty()) request.setMimeType(mimeType);
        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, "Downloadery/" + safeName(name));
        return manager.enqueue(request);
    }

    private boolean handleLongPress(long id) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        try (Cursor cursor = manager.query(query)) {
            if (!cursor.moveToFirst()) return true;
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_FAILED || status == DownloadManager.STATUS_PAUSED) {
                String url = getPreferences(MODE_PRIVATE).getString(URL_PREFIX + id, "");
                String name = getPreferences(MODE_PRIVATE).getString(NAME_PREFIX + id, "Download " + id);
                if (!url.isEmpty()) {
                    long retryId = queueDownload(url, name);
                    ids.add(retryId);
                    getPreferences(MODE_PRIVATE).edit()
                            .putString(IDS, joinIds())
                            .putString(URL_PREFIX + retryId, url)
                            .putString(NAME_PREFIX + retryId, name)
                            .apply();
                    addRow(retryId, name);
                    toast("Download retried");
                }
            } else if (status == DownloadManager.STATUS_PENDING
                    || status == DownloadManager.STATUS_RUNNING) {
                manager.remove(id);
                refreshRow(id);
                toast("Download cancelled");
            } else {
                openDownloadedFile(id);
            }
        }
        return true;
    }

    private void refreshRow(long id) {
        TextView row = rows.get(id);
        if (row == null) return;
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        try (Cursor cursor = manager.query(query)) {
            if (!cursor.moveToFirst()) {
                row.setText("Download " + id + "\nRemoved");
                return;
            }
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
            String state = statusText(status, reason);
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
            row.setText(title + "\n" + state);
        }
    }

    private void openDownloadedFile(long id) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        try (Cursor cursor = manager.query(query)) {
            if (!cursor.moveToFirst()) return;
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status != DownloadManager.STATUS_SUCCESSFUL) return;
            String uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(intent); } catch (Exception e) { toast("No app can open this file"); }
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) urlInput.setText(text.trim());
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            urlInput.setText(intent.getDataString());
        }
    }

    private void pasteUrl() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData data = clipboard.getPrimaryClip();
        if (data != null && data.getItemCount() > 0) {
            CharSequence text = data.getItemAt(0).coerceToText(this);
            urlInput.setText(text == null ? "" : text.toString().trim());
        }
    }

    static boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static String fileNameFromUrl(String value) {
        String path = Uri.parse(value).getPath();
        if (path != null && !path.endsWith("/")) {
            String candidate = path.substring(path.lastIndexOf('/') + 1);
            if (!candidate.isEmpty()) return safeName(Uri.decode(candidate));
        }
        return "download-" + System.currentTimeMillis();
    }

    static String safeName(String value) {
        String cleaned = value.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        return cleaned.isEmpty() ? "download" : cleaned;
    }

    private static String mimeTypeFor(String value) {
        int dot = value.lastIndexOf('.');
        if (dot < 0 || dot == value.length() - 1 || value.indexOf('/', dot) >= 0) return "";
        String extension = value.substring(dot + 1).toLowerCase();
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType == null ? "application/octet-stream" : mimeType;
    }

    private String joinIds() {
        StringBuilder result = new StringBuilder();
        for (Long id : ids) {
            if (result.length() > 0) result.append(',');
            result.append(id);
        }
        return result.toString();
    }

    private static String statusText(int status, int reason) {
        if (status == DownloadManager.STATUS_PENDING) return "Queued";
        if (status == DownloadManager.STATUS_RUNNING) return "Downloading";
        if (status == DownloadManager.STATUS_SUCCESSFUL) return "Completed";
        if (status == DownloadManager.STATUS_PAUSED) return "Paused (" + reason + ")";
        return "Failed (" + reason + ")";
    }

    private EditText input(String hint, boolean multiline) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_secondary));
        input.setSingleLine(!multiline);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setPadding(dp(12), 0, dp(12), 0);
        return input;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(getColor(R.color.text_primary));
        return button;
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        return view;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
