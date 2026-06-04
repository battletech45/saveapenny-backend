#!/usr/bin/env bash
set -euo pipefail

detect_tesseract_lib() {
    local os
    os="$(uname -s)"

    case "$os" in
        Darwin)
            if command -v brew >/dev/null 2>&1; then
                local prefix
                prefix="$(brew --prefix tesseract 2>/dev/null || true)"
                if [[ -n "$prefix" && -d "$prefix/lib" ]]; then
                    echo "$prefix/lib"
                    return 0
                fi
            fi
            for p in /opt/homebrew/lib /usr/local/lib; do
                if ls "$p"/libtesseract* >/dev/null 2>&1; then
                    echo "$p"
                    return 0
                fi
            done
            ;;

        Linux)
            for p in /usr/lib/x86_64-linux-gnu /usr/lib64 /usr/lib /usr/lib/aarch64-linux-gnu; do
                if ls "$p"/libtesseract* >/dev/null 2>&1; then
                    echo "$p"
                    return 0
                fi
            done
            local found
            found="$(ldconfig -p 2>/dev/null | awk '/libtesseract\.so/ {print $4; exit}')"
            if [[ -n "$found" ]]; then
                dirname "$found"
                return 0
            fi
            ;;
    esac
    return 1
}

TESS_LIB="${TESS_LIB:-$(detect_tesseract_lib || true)}"

if [[ -z "$TESS_LIB" || ! -d "$TESS_LIB" ]]; then
    echo "Tesseract lib directory not found." >&2
    echo "Install tesseract (e.g. 'brew install tesseract' or 'apt install tesseract-ocr libtesseract-dev')" >&2
    echo "Or set TESS_LIB=/path/to/tess/lib and re-run." >&2
    exit 1
fi

echo "Using tesseract lib: $TESS_LIB"

exec mvn spring-boot:run \
    -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.jvmArguments="-Djna.library.path=${TESS_LIB} --enable-native-access=ALL-UNNAMED"
