#!/usr/bin/env bash
# Install JHasher system-wide
# Usage: ./install.sh              (installs to /usr/local/bin)
#        ./install.sh ~/.local/bin  (installs to custom path)
set -euo pipefail

INSTALL_DIR="${1:-/usr/local/bin}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_DIR="$SCRIPT_DIR/build/libs"

JAR=$(find "$JAR_DIR" -name 'jhasher-*-all.jar' -print -quit 2>/dev/null || true)

if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
    echo "[*] Building JHasher..."
    "$SCRIPT_DIR/gradlew" shadowJar --no-daemon -q
    JAR=$(find "$JAR_DIR" -name 'jhasher-*-all.jar' -print -quit)
fi

DEST="$INSTALL_DIR/jhasher"

echo "[*] Installing JHasher"
echo "    JAR:  $JAR"
echo "    Dest: $DEST"

if [ "$INSTALL_DIR" = "/usr/local/bin" ] && [ "$(id -u)" -ne 0 ]; then
    echo "[*] Need sudo for $INSTALL_DIR"
    sudo mkdir -p "$INSTALL_DIR"
    sudo tee "$DEST" > /dev/null << LAUNCHER
#!/usr/bin/env bash
exec java -jar "$JAR" "\$@"
LAUNCHER
    sudo chmod +x "$DEST"
else
    mkdir -p "$INSTALL_DIR"
    cat > "$DEST" << LAUNCHER
#!/usr/bin/env bash
exec java -jar "$JAR" "\$@"
LAUNCHER
    chmod +x "$DEST"
fi

echo "[+] Installed! Run: jhasher --help"
