package com.example.musicplayer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.util.Base64;
import android.util.Size;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PluginMethod;
import java.io.ByteArrayOutputStream;

@CapacitorPlugin(name = "MediaLibrary", permissions = {
    @Permission(strings = {Manifest.permission.READ_MEDIA_AUDIO}, alias = "audio33"),
    @Permission(strings = {Manifest.permission.READ_EXTERNAL_STORAGE}, alias = "audioLegacy"),
    @Permission(strings = {Manifest.permission.POST_NOTIFICATIONS}, alias = "notif")
})
public class MediaLibraryPlugin extends Plugin {
    private String al() { return Build.VERSION.SDK_INT >= 33 ? "audio33" : "audioLegacy"; }

    @PluginMethod
    public void list(PluginCall call) {
        if (getPermissionState(al()) != PermissionState.GRANTED) {
            if (!call.getBoolean("ask", true)) {
                JSObject r = new JSObject();
                r.put("items", new JSArray());
                r.put("denied", true);
                call.resolve(r);
                return;
            }
            requestPermissionForAlias(al(), call, "afterPerm");
            return;
        }
        doList(call);
    }

    @PermissionCallback
    private void afterPerm(PluginCall call) {
        if (getPermissionState(al()) == PermissionState.GRANTED) doList(call);
        else {
            JSObject r = new JSObject();
            r.put("items", new JSArray());
            r.put("denied", true);
            call.resolve(r);
        }
    }

    private void doList(PluginCall call) {
        try {
            JSArray arr = new JSArray();
            Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] cols = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DISPLAY_NAME};
            try (Cursor c = getContext().getContentResolver().query(base, cols,
                    MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE")) {
                while (c != null && c.moveToNext()) {
                    long id = c.getLong(0);
                    JSObject o = new JSObject();
                    o.put("id", id);
                    o.put("uri", ContentUris.withAppendedId(base, id).toString());
                    o.put("title", c.getString(1));
                    o.put("artist", c.getString(2));
                    o.put("album", c.getString(3));
                    o.put("dur", c.getLong(4) / 1000.0);
                    o.put("size", c.getLong(5));
                    o.put("name", c.getString(6));
                    arr.put(o);
                }
            }
            JSObject r = new JSObject();
            r.put("items", arr);
            call.resolve(r);
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void art(PluginCall call) {
        final String uri = call.getString("uri");
        new Thread(() -> {
            JSObject r = new JSObject();
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    Bitmap b = getContext().getContentResolver().loadThumbnail(Uri.parse(uri), new Size(400, 400), null);
                    ByteArrayOutputStream o = new ByteArrayOutputStream();
                    b.compress(Bitmap.CompressFormat.JPEG, 85, o);
                    r.put("data", Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP));
                }
            } catch (Exception e) { }
            call.resolve(r);
        }).start();
    }

    private static final String ACT = "com.example.musicplayer.MEDIA";
    private static final String CH = "player_island";

    @Override
    public void load() {
        ContextCompat.registerReceiver(getContext(), new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                JSObject o = new JSObject();
                o.put("a", i.getStringExtra("a"));
                notifyListeners("mediaAction", o, true);
            }
        }, new IntentFilter(ACT), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @PluginMethod
    public void island(PluginCall call) {
        boolean cancel = Boolean.TRUE.equals(call.getBoolean("cancel", false));
        if (!cancel && Build.VERSION.SDK_INT >= 33 && getPermissionState("notif") != PermissionState.GRANTED) {
            requestPermissionForAlias("notif", call, "islandPerm");
            return;
        }
        doIsland(call);
    }

    @PermissionCallback
    private void islandPerm(PluginCall call) {
        if (getPermissionState("notif") == PermissionState.GRANTED) doIsland(call);
        else {
            JSObject r = new JSObject();
            r.put("ok", false);
            call.resolve(r);
        }
    }

    private PendingIntent act(String a, int code) {
        Intent i = new Intent(ACT).setPackage(getContext().getPackageName());
        i.putExtra("a", a);
        return PendingIntent.getBroadcast(getContext(), code, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private JSONObject pic(String key) throws Exception {
        return new JSONObject().put("type", 1).put("pic", key);
    }

    private void doIsland(PluginCall call) {
        JSObject r = new JSObject();
        try {
            Context ctx = getContext();
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Boolean.TRUE.equals(call.getBoolean("cancel", false))) {
                nm.cancel(7);
                r.put("ok", true);
                call.resolve(r);
                return;
            }
            if (Build.VERSION.SDK_INT < 26) { call.resolve(r); return; }
            NotificationChannel ch = new NotificationChannel(CH, "Плеер", NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);

            String title = call.getString("title", "");
            String artist = call.getString("artist", "");
            boolean pl = !Boolean.FALSE.equals(call.getBoolean("playing", true));
            String art = call.getString("art", "");
            boolean hasArt = art != null && !art.isEmpty();
            Icon pic;
            if (hasArt) {
                byte[] b = Base64.decode(art, Base64.DEFAULT);
                pic = Icon.createWithBitmap(BitmapFactory.decodeByteArray(b, 0, b.length));
            } else {
                pic = Icon.createWithResource(ctx, ctx.getApplicationInfo().icon);
            }

            JSONObject big = new JSONObject()
                .put("imageTextInfoLeft", new JSONObject().put("type", 1).put("picInfo", pic("miui.focus.pic_art"))
                    .put("textInfo", new JSONObject().put("title", title).put("content", artist)))
                .put("picInfo", pic("miui.focus.pic_art"));
            JSONObject small = new JSONObject().put("picInfo", pic("miui.focus.pic_art"));
            JSONObject island = new JSONObject().put("islandProperty", 1).put("bigIslandArea", big).put("smallIslandArea", small);
            JSONObject p = new JSONObject()
                .put("protocol", 1).put("business", "music")
                .put("islandFirstFloat", false).put("enableFloat", false).put("updatable", true)
                .put("ticker", title).put("tickerPic", "miui.focus.pic_art")
                .put("aodTitle", title).put("aodPic", "miui.focus.pic_art")
                .put("param_island", island)
                .put("baseInfo", new JSONObject().put("title", title).put("content", artist).put("type", 2));
            String json = new JSONObject().put("param_v2", p).toString();

            Intent open = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
            Notification.Builder b = new Notification.Builder(ctx, CH)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(artist)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
            if (hasArt) b.setLargeIcon(pic);
            b.addAction(new Notification.Action.Builder(Icon.createWithResource(ctx, android.R.drawable.ic_media_previous), "Назад", act("prev", 1)).build());
            b.addAction(new Notification.Action.Builder(Icon.createWithResource(ctx, pl ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play), pl ? "Пауза" : "Играть", act("toggle", 2)).build());
            b.addAction(new Notification.Action.Builder(Icon.createWithResource(ctx, android.R.drawable.ic_media_next), "Вперёд", act("next", 3)).build());
            b.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2));

            Bundle ex = new Bundle();
            Bundle pics = new Bundle();
            pics.putParcelable("miui.focus.pic_art", pic);
            ex.putBundle("miui.focus.pics", pics);
            b.addExtras(ex);
            Notification n = b.build();
            n.extras.putString("miui.focus.param", json);
            nm.notify(7, n);
            r.put("ok", true);
            call.resolve(r);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }
}
