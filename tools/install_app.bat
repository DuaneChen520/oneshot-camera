@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0\.."

set "ADB=E:\AndoirdSDK\Sdk\platform-tools\adb.exe"
set "DEVICE_IP=192.168.6.104:45957"

echo ===================================================
echo [OneShot 实机调试] 编译并部署到真机
echo ===================================================

echo [1/3] 正在编译 Debug APK...
call gradlew.bat :app:assembleDebug
if %ERRORLEVEL% neq 0 (
    echo [错误] 编译失败，请检查编译错误。
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] 连接实机设备 (%DEVICE_IP%)...
"%ADB%" connect %DEVICE_IP%
"%ADB%" devices

echo [3/3] 安装 APK 到真机...
"%ADB%" install -r -t "app\build\outputs\apk\debug\app-debug.apk"
if %ERRORLEVEL% neq 0 (
    echo [错误] 安装失败，请确认手机已开机并开启 USB/无线调试授权。
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo 启动 OneShot 应用...
"%ADB%" shell am start -n dev.oneshot/.MainActivity

echo ===================================================
echo [成功] OneShot 已在实机启动！请在手机上直接操作测试。
echo ===================================================
pause
