import struct
import zlib
import os
import math

def point_in_triangle(px, py, x1, y1, x2, y2, x3, y3):
    d1 = (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2)
    d2 = (px - x3) * (y2 - y3) - (x2 - x3) * (py - y3)
    d3 = (px - x1) * (y3 - y1) - (x3 - x1) * (py - y1)
    has_neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
    has_pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
    return not (has_neg and has_pos)

def point_in_quad(px, py, x1, y1, x2, y2, x3, y3, x4, y4):
    return (point_in_triangle(px, py, x1, y1, x2, y2, x3, y3) or
            point_in_triangle(px, py, x1, y1, x3, y3, x4, y4))

def create_anitrak_png(width, height, is_round=False):
    raw_data = bytearray()
    cx, cy = width / 2.0, height / 2.0
    outer_r = width / 2.0
    scale = width / 108.0

    for y in range(height):
        raw_data.append(0) # filter type 0
        orig_y = y / scale
        for x in range(width):
            orig_x = x / scale
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)

            # Circular mask for round icon
            if is_round and dist > outer_r - 0.5:
                raw_data.extend([0, 0, 0, 0])
                continue

            # Normalized coordinate
            nx = x / float(width)
            ny = y / float(height)

            # Base gradient: #090B10 (9, 11, 16) to #1A1E36 (26, 30, 54)
            diag = (nx * 0.4 + ny * 0.6)
            r = int(9 + (26 - 9) * diag)
            g = int(11 + (30 - 11) * diag)
            b = int(16 + (54 - 16) * diag)
            a = 255

            # Subtle cyber ring (radius 42/108 * width)
            ring_dist = abs(dist - 42.0 * scale)
            if ring_dist < 1.0 * scale:
                alpha = max(0.0, 1.0 - ring_dist / (1.0 * scale)) * 0.35
                r = int(r * (1 - alpha) + 56 * alpha)
                g = int(g * (1 - alpha) + 189 * alpha)
                b = int(b * (1 - alpha) + 248 * alpha)

            # Center glow halo behind emblem
            halo_dist = math.sqrt((orig_x - 54)**2 + (orig_y - 54)**2)
            if halo_dist < 26.0:
                h_alpha = (1.0 - halo_dist / 26.0) * 0.25
                r = int(r * (1 - h_alpha) + 56 * h_alpha)
                g = int(g * (1 - h_alpha) + 189 * h_alpha)
                b = int(b * (1 - h_alpha) + 248 * h_alpha)

            # Shonen Outer Chevrons
            # Left chevron: (27,50) to (34,44) to (32,56)
            if point_in_triangle(orig_x, orig_y, 27, 50, 34, 44, 32, 56):
                r, g, b = 30, 41, 59
            # Right chevron: (81,50) to (74,44) to (76,56)
            if point_in_triangle(orig_x, orig_y, 81, 50, 74, 44, 76, 56):
                r, g, b = 30, 41, 59

            # Left Wing of "A": M54,30 L50,30 L33,74 L41,74 L47,58 L54,39 Z
            if (point_in_quad(orig_x, orig_y, 50, 30, 54, 30, 54, 39, 47, 58) or
                point_in_quad(orig_x, orig_y, 50, 30, 47, 58, 41, 74, 33, 74)):
                r, g, b = 56, 189, 248  # Sky Blue #38BDF8

            # Right Wing of "A": M54,30 L58,30 L75,74 L67,74 L61,58 L54,39 Z
            if (point_in_quad(orig_x, orig_y, 54, 30, 58, 30, 61, 58, 54, 39) or
                point_in_quad(orig_x, orig_y, 58, 30, 75, 74, 67, 74, 61, 58)):
                r, g, b = 6, 182, 212   # Sky Cyan #06B6D4

            # Bottom Tracking Base Bar: (44,77) to (64,77) to (62,80) to (46,80)
            if point_in_quad(orig_x, orig_y, 44, 77, 64, 77, 62, 80, 46, 80):
                r, g, b = 245, 158, 11  # Sunset Amber #F59E0B

            # Streaming Play Button (Ruby Red): M46,45 L46,67 L65,56 Z
            if point_in_triangle(orig_x, orig_y, 46, 45, 46, 67, 65, 56):
                r, g, b = 244, 63, 94   # Ruby Red #F43F5E

            # Play Button Forward Highlight (Pure White): M48,49 L48,63 L60,56 Z
            if point_in_triangle(orig_x, orig_y, 48, 49, 48, 63, 60, 56):
                r, g, b = 255, 255, 255 # Pure White #FFFFFF

            # Play Core Accent (Dark Ruby): M50,53 L50,59 L55,56 Z
            if point_in_triangle(orig_x, orig_y, 50, 53, 50, 59, 55, 56):
                r, g, b = 190, 18, 60   # Dark Ruby #BE123C

            # Top Apex Anime Sparkle Star
            # Diamond from 54,23 to 59.5,28.5 to 54,34 to 48.5,28.5
            s_dx = abs(orig_x - 54.0)
            s_dy = abs(orig_y - 28.5)
            # 4-point star shape: (dx/5.5)^0.6 + (dy/5.5)^0.6 <= 1
            if (s_dx / 5.5)**0.6 + (s_dy / 5.5)**0.6 <= 1.0:
                r, g, b = 255, 255, 255

            # Star Shimmer Sparkle Dots at (35, 36) and (73, 36)
            if math.sqrt((orig_x - 35)**2 + (orig_y - 36)**2) <= 1.5 or \
               math.sqrt((orig_x - 73)**2 + (orig_y - 36)**2) <= 1.5:
                r, g, b = 253, 224, 71  # Golden Yellow #FDE047

            raw_data.extend([r, g, b, a])

    def chunk(tag, data):
        c = tag + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)

    png = bytearray(b'\x89PNG\r\n\x1a\n')
    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    png.extend(chunk(b'IHDR', ihdr))
    idat = zlib.compress(bytes(raw_data), 9)
    png.extend(chunk(b'IDAT', idat))
    png.extend(chunk(b'IEND', b''))
    return bytes(png)

densities = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

base_dir = '/app/applet/app/src/main/res'
for density, size in densities.items():
    dir_path = os.path.join(base_dir, f'mipmap-{density}')
    os.makedirs(dir_path, exist_ok=True)

    # Remove any old webp files
    for w in ['ic_launcher.webp', 'ic_launcher_round.webp']:
        wp = os.path.join(dir_path, w)
        if os.path.exists(wp):
            os.remove(wp)

    square_png = create_anitrak_png(size, size, is_round=False)
    with open(os.path.join(dir_path, 'ic_launcher.png'), 'wb') as f:
        f.write(square_png)

    round_png = create_anitrak_png(size, size, is_round=True)
    with open(os.path.join(dir_path, 'ic_launcher_round.png'), 'wb') as f:
        f.write(round_png)

print("All AniTrAk launcher icons generated successfully across mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi!")
