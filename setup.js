// Запускать после `npx cap add android`: node setup.js
const fs = require('fs'), path = require('path');
const pkg = 'com/example/musicplayer';
const dir = path.join('android/app/src/main/java', pkg);
if (!fs.existsSync(dir)) { console.error('Сначала выполните: npx cap add android'); process.exit(1); }
fs.copyFileSync('native/MediaLibraryPlugin.java', path.join(dir, 'MediaLibraryPlugin.java'));
fs.writeFileSync(path.join(dir, 'MainActivity.java'), `package com.example.musicplayer;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(MediaLibraryPlugin.class);
        super.onCreate(savedInstanceState);
        boolean dark = true;
        Window w = getWindow();
        int bg = dark ? Color.BLACK : Color.WHITE;
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(bg);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(w, w.getDecorView());
        c.setAppearanceLightStatusBars(!dark);
        c.setAppearanceLightNavigationBars(!dark);
    }
}
`);
const mf = 'android/app/src/main/AndroidManifest.xml';
let x = fs.readFileSync(mf, 'utf8');
if (!x.includes('READ_MEDIA_AUDIO')) {
  x = x.replace(/(<manifest[^>]*>)/, `$1
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />`);
  fs.writeFileSync(mf, x);
}
x = fs.readFileSync(mf, 'utf8');
if (!x.includes('POST_NOTIFICATIONS')) {
  x = x.replace(/(<manifest[^>]*>)/, `$1
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`);
  fs.writeFileSync(mf, x);
}

// Ускорение сборки Gradle
const gp = 'android/gradle.properties';
if (fs.existsSync(gp)) {
  let g = fs.readFileSync(gp, 'utf8');
  if (!g.includes('org.gradle.caching')) {
    g += '\norg.gradle.caching=true\norg.gradle.parallel=true\norg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8\n';
    fs.writeFileSync(gp, g);
  }
}
console.log('Готово: плагин MediaLibrary подключён.');
