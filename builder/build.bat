@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

REM #################################################
REM          RAVAN APK BUILDER - Windows
REM                   v2.0
REM
REM  Developed by: Somesh
REM  GitHub: github.com/someshsrichandan
REM  LinkedIn: linkedin.com/in/someshsrichandan
REM #################################################

title Ravan APK Builder v2.0 - by Somesh

REM Get script directory
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "CONFIG_FILE=%SCRIPT_DIR%build_config.txt"
set "DEFAULT_LOGO=%PROJECT_DIR%\ravanrat.png"

goto :main_menu

:print_banner
cls
echo.
echo [91m╔══════════════════════════════════════════════════════════════╗[0m
echo [91m║                                                              ║[0m
echo [91m║     ██████╗  █████╗ ██╗   ██╗ █████╗ ███╗   ██╗              ║[0m
echo [91m║     ██╔══██╗██╔══██╗██║   ██║██╔══██╗████╗  ██║              ║[0m
echo [91m║     ██████╔╝███████║██║   ██║███████║██╔██╗ ██║              ║[0m
echo [91m║     ██╔══██╗██╔══██║╚██╗ ██╔╝██╔══██║██║╚██╗██║              ║[0m
echo [91m║     ██║  ██║██║  ██║ ╚████╔╝ ██║  ██║██║ ╚████║              ║[0m
echo [91m║     ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝╚═╝  ╚═══╝              ║[0m
echo [91m║                                                              ║[0m
echo [91m║                    APK BUILDER v2.0                          ║[0m
echo [91m║                                                              ║[0m
echo [91m╠══════════════════════════════════════════════════════════════╣[0m
echo [95m║  Developed by: Somes                                         ║[0m
echo [95m║  GitHub:   https://github.com/somes                          ║[0m
echo [95m║  LinkedIn: https://linkedin.com/in/somes                     ║[0m
echo [91m╚══════════════════════════════════════════════════════════════╝[0m
echo.
goto :eof

:check_requirements
echo [96m[*] Checking requirements...[0m
echo.

REM Check Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [91m[!] Java is not installed.[0m
    echo.
    echo [95m[^>] Options:[0m
    echo     1. Auto-install Java (download from web)
    echo     2. Show manual installation instructions
    echo     3. Skip (I'll install later)
    echo.
    set /p "JAVA_OPTION=    Choose option [2]: "
    if "!JAVA_OPTION!"=="" set "JAVA_OPTION=2"
    
    if "!JAVA_OPTION!"=="1" (
        call :install_java
    ) else if "!JAVA_OPTION!"=="2" (
        call :show_manual_java_install
        pause
        exit /b 1
    ) else (
        echo [93m[!] Skipping Java check. Build may fail.[0m
    )
) else (
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        echo [92m[✓] Java found: %%g[0m
    )
)

REM Check keytool
where keytool >nul 2>nul
if %errorlevel% equ 0 (
    echo [92m[✓] keytool found[0m
) else (
    echo [93m[!] keytool not found. Usually comes with JDK.[0m
)

echo.
goto :eof

:install_java
echo [96m[*] Opening Java download page...[0m
echo [93m    Please download and install JDK 11 or higher from:[0m
echo     https://adoptium.net/temurin/releases/
echo.
start "" "https://adoptium.net/temurin/releases/"
echo [93m[!] After installing Java, restart this script.[0m
pause
exit /b 1

:show_manual_java_install
echo.
echo [96m╔══════════════════════════════════════════════════════════════╗[0m
echo [96m║              MANUAL JAVA INSTALLATION GUIDE                  ║[0m
echo [96m╚══════════════════════════════════════════════════════════════╝[0m
echo.
echo [97mOption 1: Download from Adoptium (Recommended)[0m
echo     1. Go to: https://adoptium.net/temurin/releases/
echo     2. Download "JDK 11" or "JDK 17" for Windows x64
echo     3. Run the installer (choose "Add to PATH")
echo     4. Restart this script
echo.
echo [97mOption 2: Using winget (Windows 11)[0m
echo     winget install EclipseAdoptium.Temurin.11.JDK
echo.
echo [97mOption 3: Using Chocolatey[0m
echo     choco install temurin11
echo.
echo [97mOption 4: Using Scoop[0m
echo     scoop bucket add java
echo     scoop install temurin11-jdk
echo.
goto :eof

:generate_keystore
echo [96m[*] Keystore Configuration[0m
echo.

set "KEYSTORE_PATH=%PROJECT_DIR%\ravan-keystore.jks"

if exist "%KEYSTORE_PATH%" (
    echo [93m[!] Keystore already exists at: %KEYSTORE_PATH%[0m
    set /p "REGENERATE=    Generate new keystore? (y/N): "
    if /i not "!REGENERATE!"=="y" (
        echo [92m[✓] Using existing keystore[0m
        goto :eof
    )
    del "%KEYSTORE_PATH%" >nul 2>nul
)

echo [95m[^>] Enter keystore details:[0m
echo.

set /p "KEY_ALIAS=    Key alias [ravan-key]: "
if "!KEY_ALIAS!"=="" set "KEY_ALIAS=ravan-key"

set /p "KEYSTORE_PASS=    Keystore password [ravan123]: "
if "!KEYSTORE_PASS!"=="" set "KEYSTORE_PASS=ravan123"

set /p "CN_NAME=    Your name [Ravan Developer]: "
if "!CN_NAME!"=="" set "CN_NAME=Ravan Developer"

set /p "ORG_NAME=    Organization [Ravan Security]: "
if "!ORG_NAME!"=="" set "ORG_NAME=Ravan Security"

set /p "COUNTRY=    Country code [US]: "
if "!COUNTRY!"=="" set "COUNTRY=US"

set /p "VALIDITY_YEARS=    Validity in years [25]: "
if "!VALIDITY_YEARS!"=="" set "VALIDITY_YEARS=25"
set /a VALIDITY_DAYS=!VALIDITY_YEARS! * 365

echo.
echo [96m[*] Generating keystore...[0m

keytool -genkeypair -alias "!KEY_ALIAS!" -keyalg RSA -keysize 2048 -validity !VALIDITY_DAYS! -keystore "%KEYSTORE_PATH%" -storepass "!KEYSTORE_PASS!" -keypass "!KEYSTORE_PASS!" -dname "CN=!CN_NAME!, O=!ORG_NAME!, C=!COUNTRY!" 2>nul

if %errorlevel% equ 0 (
    echo [92m[✓] Keystore generated successfully![0m
    echo.
    
    REM Create keystore.properties for Gradle
    set "KEYSTORE_PROPS=%PROJECT_DIR%\keystore.properties"
    echo storeFile=ravan-keystore.jks> "!KEYSTORE_PROPS!"
    echo storePassword=!KEYSTORE_PASS!>> "!KEYSTORE_PROPS!"
    echo keyAlias=!KEY_ALIAS!>> "!KEYSTORE_PROPS!"
    echo keyPassword=!KEYSTORE_PASS!>> "!KEYSTORE_PROPS!"
    echo [92m[✓] Created keystore.properties for Gradle[0m
    
    REM Save to config
    echo KEYSTORE_PATH=%KEYSTORE_PATH%> "%CONFIG_FILE%"
    echo KEY_ALIAS=!KEY_ALIAS!>> "%CONFIG_FILE%"
    echo KEYSTORE_PASS=!KEYSTORE_PASS!>> "%CONFIG_FILE%"
    
    REM Show certificate info
    echo [96m[*] Certificate fingerprint:[0m
    keytool -list -v -keystore "%KEYSTORE_PATH%" -storepass "!KEYSTORE_PASS!" -alias "!KEY_ALIAS!" 2>nul | findstr "SHA256:"
    echo.
) else (
    echo [91m[!] Failed to generate keystore[0m
    pause
    exit /b 1
)
goto :eof

:configure_logo
echo [96m[*] Logo Configuration[0m
echo.

set "RES_DIR=%PROJECT_DIR%\app\src\main\res"

echo [95m[^>] Logo options:[0m
echo     1. Use default Ravan logo (ravanrat.png)
echo     2. Use custom logo (provide image path)
echo     3. Keep current logo (no change)
echo.
set /p "LOGO_OPTION=    Choose option [1]: "
if "!LOGO_OPTION!"=="" set "LOGO_OPTION=1"

set "LOGO_PATH="

if "!LOGO_OPTION!"=="1" (
    if exist "%DEFAULT_LOGO%" (
        set "LOGO_PATH=%DEFAULT_LOGO%"
        echo [92m[✓] Using default Ravan logo[0m
    ) else (
        echo [91m[!] Default logo not found at: %DEFAULT_LOGO%[0m
        goto :logo_done
    )
) else if "!LOGO_OPTION!"=="2" (
    set /p "CUSTOM_LOGO=    Enter path to logo image (PNG, 512x512): "
    if exist "!CUSTOM_LOGO!" (
        set "LOGO_PATH=!CUSTOM_LOGO!"
    ) else (
        echo [91m[!] Logo file not found: !CUSTOM_LOGO![0m
        goto :logo_done
    )
) else (
    echo [92m[✓] Keeping current logo[0m
    goto :logo_done
)

if not "!LOGO_PATH!"=="" (
    echo [96m[*] Copying logo to resources...[0m
    
    REM Copy to all mipmap folders
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-mdpi\ic_launcher.png" >nul 2>nul
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-hdpi\ic_launcher.png" >nul 2>nul
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xhdpi\ic_launcher.png" >nul 2>nul
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xxhdpi\ic_launcher.png" >nul 2>nul
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xxxhdpi\ic_launcher.png" >nul 2>nul
    
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-mdpi\ic_launcher_round.png" >nul 2>nul
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-hdpi\ic_launcher_round.png" >nul 2>nul
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xhdpi\ic_launcher_round.png" >nul 2>nul
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xxhdpi\ic_launcher_round.png" >nul 2>nul
    copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xxxhdpi\ic_launcher_round.png" >nul 2>nul
    
    echo [92m[✓] Logo copied to all densities[0m
    echo [93m    Note: For proper sizing, use Android Asset Studio online[0m
)

:logo_done
echo.
goto :eof

:configure_app
echo [96m[*] App Configuration[0m
echo.

set "STRINGS_FILE=%PROJECT_DIR%\app\src\main\res\values\strings.xml"

REM App name
set /p "APP_NAME=    Enter app name [Ravan Security]: "
if "!APP_NAME!"=="" set "APP_NAME=Ravan Security"

REM Update strings.xml using PowerShell
if exist "%STRINGS_FILE%" (
    powershell -Command "(Get-Content '%STRINGS_FILE%') -replace '<string name=\"app_name\">.*</string>', '<string name=\"app_name\">!APP_NAME!</string>' | Set-Content '%STRINGS_FILE%'"
    echo [92m[✓] App name set to: !APP_NAME![0m
)

REM Save to config
echo APP_NAME=!APP_NAME!>> "%CONFIG_FILE%"

REM Google Sheet URL
echo.
echo [95m[^>] Google Sheet Webhook Configuration[0m
echo [93m    This URL will receive device data when app starts.[0m
echo [93m    You need to set up Google Sheet manually (see README).[0m
echo [93m    Leave empty to skip.[0m
echo.
set /p "SHEET_URL=    Enter Google Sheet webhook URL: "

if not "!SHEET_URL!"=="" (
    echo SHEET_URL=!SHEET_URL!>> "%CONFIG_FILE%"
    echo [92m[✓] Google Sheet URL saved to config[0m
) else (
    echo [93m[!] Skipping Google Sheet configuration[0m
)

REM Version configuration
echo.
set /p "VERSION_NAME=    Enter version name [2.0]: "
if "!VERSION_NAME!"=="" set "VERSION_NAME=2.0"

set /p "VERSION_CODE=    Enter version code [20]: "
if "!VERSION_CODE!"=="" set "VERSION_CODE=20"

set "BUILD_GRADLE=%PROJECT_DIR%\app\build.gradle"
if exist "%BUILD_GRADLE%" (
    powershell -Command "(Get-Content '%BUILD_GRADLE%') -replace 'versionCode [0-9]+', 'versionCode !VERSION_CODE!' | Set-Content '%BUILD_GRADLE%'"
    powershell -Command "(Get-Content '%BUILD_GRADLE%') -replace 'versionName \".*\"', 'versionName \"!VERSION_NAME!\"' | Set-Content '%BUILD_GRADLE%'"
    echo [92m[✓] Version set to: !VERSION_NAME! (code: !VERSION_CODE!)[0m
    
    echo VERSION_NAME=!VERSION_NAME!>> "%CONFIG_FILE%"
    echo VERSION_CODE=!VERSION_CODE!>> "%CONFIG_FILE%"
)

echo.
goto :eof

:build_apk
echo [96m[*] Building APK...[0m
echo.

cd /d "%PROJECT_DIR%"

REM Load config if exists
if exist "%CONFIG_FILE%" (
    for /f "tokens=1,2 delims==" %%a in (%CONFIG_FILE%) do (
        set "%%a=%%b"
    )
)

echo [96m[*] Running Gradle build...[0m
echo.

call gradlew.bat assembleRelease

REM Check if build was successful
set "APK_PATH=%PROJECT_DIR%\app\build\outputs\apk\release\app-release.apk"
set "UNSIGNED_APK=%PROJECT_DIR%\app\build\outputs\apk\release\app-release-unsigned.apk"

if exist "%APK_PATH%" (
    set "FINAL_APK=%APK_PATH%"
    goto :copy_apk
)

if exist "%UNSIGNED_APK%" (
    echo [93m[*] Signing APK...[0m
    set "SIGNED_APK=%PROJECT_DIR%\app\build\outputs\apk\release\app-release-signed.apk"
    
    jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore "%KEYSTORE_PATH%" -storepass "!KEYSTORE_PASS!" -keypass "!KEYSTORE_PASS!" -signedjar "!SIGNED_APK!" "%UNSIGNED_APK%" "!KEY_ALIAS!" 2>nul
    
    if exist "!SIGNED_APK!" (
        set "FINAL_APK=!SIGNED_APK!"
        goto :copy_apk
    )
)

REM Build failed
echo.
echo [91m╔══════════════════════════════════════════════════════════════╗[0m
echo [91m║                      BUILD FAILED!                           ║[0m
echo [91m╚══════════════════════════════════════════════════════════════╝[0m
echo.
echo [91m[!] Check the error messages above[0m
echo [93m[!] Common fixes:[0m
echo     - Ensure Java 11+ is installed and in PATH
echo     - Check internet connection
echo     - Run: gradlew --stop ^&^& gradlew clean
pause
exit /b 1

:copy_apk
REM Create output folder
set "OUTPUT_DIR=%SCRIPT_DIR%output"
if not exist "!OUTPUT_DIR!" mkdir "!OUTPUT_DIR!"

REM Generate timestamp
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value 2^>nul') do set datetime=%%I
set "TIMESTAMP=!datetime:~0,8!_!datetime:~8,6!"

REM Create safe app name
set "APP_NAME_SAFE=!APP_NAME: =_!"
if "!APP_NAME_SAFE!"=="" set "APP_NAME_SAFE=Ravan"

set "OUTPUT_APK=!OUTPUT_DIR!\!APP_NAME_SAFE!-v!VERSION_NAME!-!TIMESTAMP!.apk"
copy /Y "!FINAL_APK!" "!OUTPUT_APK!" >nul

echo.
echo [92m╔══════════════════════════════════════════════════════════════╗[0m
echo [92m║                    BUILD SUCCESSFUL!                         ║[0m
echo [92m╚══════════════════════════════════════════════════════════════╝[0m
echo.
echo [92m[✓] APK saved to: !OUTPUT_APK![0m
echo.
echo [95m╔══════════════════════════════════════════════════════════════╗[0m
echo [95m║  Developed by: Somesh                             ║[0m
echo [95m║  GitHub:   https://github.com/someshsrichandan              ║[0m
echo [95m║  LinkedIn: https://linkedin.com/in/someshsrichandan         ║[0m
echo [95m╚══════════════════════════════════════════════════════════════╝[0m
echo.

set /p "OPEN_FOLDER=    Open output folder? (Y/n): "
if /i not "!OPEN_FOLDER!"=="n" (
    explorer "!OUTPUT_DIR!"
)
goto :eof

:main_menu
call :print_banner

echo [95m[^>] Build Options:[0m
echo.
echo     1. Full Build (Configure everything + Build)
echo     2. Quick Build (Use existing config)
echo     3. Generate Keystore Only
echo     4. Configure Logo Only
echo     5. Configure App Settings Only
echo     6. Check/Install Requirements
echo     7. Exit
echo.
set /p "MENU_OPTION=    Choose option [1]: "
if "!MENU_OPTION!"=="" set "MENU_OPTION=1"

echo.

if "!MENU_OPTION!"=="1" (
    call :check_requirements
    call :generate_keystore
    call :configure_logo
    call :configure_app
    call :build_apk
) else if "!MENU_OPTION!"=="2" (
    call :check_requirements
    call :build_apk
) else if "!MENU_OPTION!"=="3" (
    call :check_requirements
    call :generate_keystore
) else if "!MENU_OPTION!"=="4" (
    call :configure_logo
) else if "!MENU_OPTION!"=="5" (
    call :configure_app
) else if "!MENU_OPTION!"=="6" (
    call :check_requirements
    call :show_manual_java_install
) else if "!MENU_OPTION!"=="7" (
    echo [96m[*] Goodbye![0m
    echo [95m    Follow: https://github.com/someshsrichandan[0m
    exit /b 0
) else (
    echo [91m[!] Invalid option[0m
    pause
    exit /b 1
)

echo.
echo [92m[✓] Done![0m
echo.
pause
