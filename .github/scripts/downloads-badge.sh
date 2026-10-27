#!/usr/bin/env bash
# Vẽ huy hiệu "downloads" (kiểu shields.io) từ tổng lượt tải các file trong Releases.
# Dùng được cả khi repo private, vì shields.io không đọc được repo private.
#   downloads-badge.sh <số lượt tải> <file svg đầu ra>
set -euo pipefail

count="$1"
out="$2"

label="downloads"
label_w=70
value_w=$(( ${#count} * 7 + 12 ))
total_w=$(( label_w + value_w ))

cat > "$out" <<SVG
<svg xmlns="http://www.w3.org/2000/svg" width="${total_w}" height="20" role="img" aria-label="${label}: ${count}">
  <title>${label}: ${count}</title>
  <linearGradient id="s" x2="0" y2="100%"><stop offset="0" stop-color="#bbb" stop-opacity=".1"/><stop offset="1" stop-opacity=".1"/></linearGradient>
  <clipPath id="r"><rect width="${total_w}" height="20" rx="3" fill="#fff"/></clipPath>
  <g clip-path="url(#r)">
    <rect width="${label_w}" height="20" fill="#555"/>
    <rect x="${label_w}" width="${value_w}" height="20" fill="#7C3AED"/>
    <rect width="${total_w}" height="20" fill="url(#s)"/>
  </g>
  <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" font-size="11">
    <text x="$(( label_w / 2 ))" y="14">${label}</text>
    <text x="$(( label_w + value_w / 2 ))" y="14">${count}</text>
  </g>
</svg>
SVG
