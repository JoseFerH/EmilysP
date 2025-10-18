@echo off
setlocal enabledelayedexpansion

REM === CONFIGURACIÓN ===
set PROJECT_NAME=workbreak_timer
set MAIN_PATH=lib\main.dart

echo 🚀 Creando proyecto Flutter: %PROJECT_NAME%
flutter create %PROJECT_NAME%
cd %PROJECT_NAME%

echo 🧹 Limpiando main.dart original...
del %MAIN_PATH%

echo 📁 Copiando tu main.dart personalizado...
REM Asegúrate de tener tu main.dart en la misma carpeta del script
copy ..\main.dart %MAIN_PATH%

echo 🧩 Agregando dependencias requeridas...
(for /f "delims=" %%i in ('findstr /v /c:"flutter_local_notifications" pubspec.yaml ^| findstr /v /c:"shared_preferences" ^| findstr /v /c:"timezone" ^| findstr /v /c:"flutter_native_timezone"') do @echo %%i)>tmp.yaml
del pubspec.yaml
ren tmp.yaml pubspec.yaml

(
echo.
echo dependencies:
echo ^  flutter:
echo ^    sdk: flutter
echo ^  flutter_local_notifications: ^13.0.0
echo ^  shared_preferences: ^2.1.0
echo ^  timezone: ^0.9.0
echo ^  flutter_native_timezone: ^2.0.0
)>>pubspec.yaml

echo 📦 Ejecutando flutter pub get...
flutter pub get

echo ✅ Proyecto creado correctamente.
echo Puedes abrirlo con VSCode o Android Studio:
echo     code .
echo o correrlo directamente con:
echo     flutter run
echo.
echo 🎯 Ubicación final: %cd%

pause
