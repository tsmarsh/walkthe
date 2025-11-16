#!/bin/bash

# Script to convert PPM images to animated GIF using ffmpeg
# Usage: ./create_animation.sh [input_pattern] [output.gif] [fps]
#
# Examples:
#   ./create_animation.sh "output/frame_%04d.ppm" simulation.gif 10
#   ./create_animation.sh "output/energy_*.ppm" energy.gif 15

set -e

# Default values
INPUT_PATTERN="${1:-output/frame_%04d.ppm}"
OUTPUT_GIF="${2:-animation.gif}"
FPS="${3:-10}"

# Check if ffmpeg is installed
if ! command -v ffmpeg &> /dev/null; then
    echo "Error: ffmpeg is not installed"
    echo "Install with: sudo pacman -S ffmpeg  (Arch Linux)"
    echo "           or: sudo apt install ffmpeg  (Debian/Ubuntu)"
    exit 1
fi

echo "=== PPM to GIF Animation Generator ==="
echo ""
echo "Input pattern: $INPUT_PATTERN"
echo "Output file:   $OUTPUT_GIF"
echo "Frame rate:    $FPS fps"
echo ""

# Convert PPM sequence to GIF
# Two-pass for better quality:
# 1. Generate color palette from input
# 2. Use palette to create GIF

PALETTE="palette.png"

echo "Step 1: Generating color palette..."
ffmpeg -framerate "$FPS" -i "$INPUT_PATTERN" -vf "palettegen=stats_mode=diff" -y "$PALETTE"

echo ""
echo "Step 2: Creating GIF animation..."
ffmpeg -framerate "$FPS" -i "$INPUT_PATTERN" -i "$PALETTE" \
    -lavfi "paletteuse=dither=bayer:bayer_scale=5" \
    -y "$OUTPUT_GIF"

# Clean up palette
rm -f "$PALETTE"

echo ""
echo "✓ Animation created: $OUTPUT_GIF"

# Show file info
FILE_SIZE=$(du -h "$OUTPUT_GIF" | cut -f1)
FRAME_COUNT=$(ffprobe -v error -select_streams v:0 -count_frames -show_entries stream=nb_read_frames -of default=nokey=1:noprint_wrappers=1 "$OUTPUT_GIF" 2>/dev/null || echo "unknown")

echo ""
echo "File size:     $FILE_SIZE"
echo "Frame count:   $FRAME_COUNT"
echo "Duration:      ~$(echo "scale=1; $FRAME_COUNT / $FPS" | bc 2>/dev/null || echo "?")s"
