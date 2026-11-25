# Музыка: офлайн-плеер (Android)

## Сборка APK на GitHub (без установки чего-либо)
1. Создайте репозиторий и загрузите в него ВСЁ содержимое этой папки (включая скрытую `.github`).
2. Вкладка Actions -> Build APK (запускается сама после загрузки, 5-10 мин).
3. Внизу запуска, раздел Artifacts -> music-apk -> скачать, распаковать, установить app-debug.apk.

## Сборка на компьютере (Node.js 20+, Android Studio)
    npm install
    npm run android
    npx cap open android
Build -> Build Bundle(s) / APK(s) -> Build APK(s).

## Структура
- www/index.html: плеер (один файл)
- native/MediaLibraryPlugin.java: автозагрузка музыки, уведомление, Hyper Island
- setup.js: подключает плагин, разрешения, цвет строки состояния, ускорение Gradle
- .github/workflows/build.yml: облачная сборка

Обновить плеер: замените www/index.html и загрузите в репозиторий.
Hyper Island работает только при наличии разрешения Xiaomi на焦点通知.
