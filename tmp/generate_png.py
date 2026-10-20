import struct
import zlib
import os
import math

def create_png(width, height, is_round=False):
    # RGBA image buffer
    raw_data = bytearray()
    
    cx, cy = width / 2.0, height / 2.0
    outer_r = width / 2.0
    
    # Colors:
    # Midnight Frostmoon gradient: (9, 13, 22) to (30, 27, 75)
    # Teal moon glow: (94, 234, 212)
    # Golden charm: (252, 211, 77)
    # Violet hair: (129, 140, 248)
    
    for y in range(height):
        raw_data.append(0) # filter type 0
        ny = y / float(height)
        for x in range(width):
            nx = x / float(width)
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx*dx + dy*dy)
            
            # If round icon, transparent outside circle
            if is_round and dist > outer_r - 0.5:
                raw_data.extend([0, 0, 0, 0])
                continue
                
            # Base Frostmoon gradient
            r = int(9 + (30 - 9) * (nx * 0.4 + ny * 0.6))
            g = int(13 + (27 - 13) * (nx * 0.4 + ny * 0.6))
            b = int(22 + (75 - 22) * (nx * 0.4 + ny * 0.6))
            a = 255
            
            # Subtle teal ring around edge (dist approx 0.8 * outer_r)
            ring_dist = abs(dist - 0.78 * outer_r)
            if ring_dist < 1.2:
                r, g, b = 94, 234, 212
                
            # Inner moon / antler area
            # Crescent moon around center
            moon_dx = dx + 0.05 * width
            moon_dy = dy
            moon_d = math.sqrt(moon_dx*moon_dx + moon_dy*moon_dy)
            cut_dx = dx + 0.12 * width
            cut_dy = dy - 0.04 * width
            cut_d = math.sqrt(cut_dx*cut_dx + cut_dy*cut_dy)
            
            if 0.18 * width < moon_d < 0.32 * width and (cut_d > 0.22 * width or moon_dx > 0.08 * width):
                r = int(r * 0.3 + 94 * 0.7)
                g = int(g * 0.3 + 234 * 0.7)
                b = int(b * 0.3 + 212 * 0.7)
                
            # Glowing antler silhouette in upper center
            if dy < 0 and abs(dx) < 0.35 * width:
                # Left antler line
                adx = abs(dx) / float(width)
                ady = abs(dy) / float(height)
                # Curve approximation
                target_y = -0.15 - 0.5 * (adx - 0.05) ** 0.8 if adx > 0.04 else 0
                if abs(dy / float(height) - target_y) < 0.035 and adx < 0.28:
                    r, g, b = 94, 234, 212
                    
            # Central Dendro / priestess jewel
            if abs(dx) < 0.08 * width and abs(dy - 0.1 * height) < 0.12 * height:
                r = int(r * 0.2 + 52 * 0.8)
                g = int(g * 0.2 + 211 * 0.8)
                b = int(b * 0.2 + 153 * 0.8)

            raw_data.extend([r, g, b, a])
            
    # Assemble PNG chunks
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

base_dir = 'app/src/main/res'
for density, size in densities.items():
    dir_path = os.path.join(base_dir, f'mipmap-{density}')
    os.makedirs(dir_path, exist_ok=True)
    
    square_png = create_png(size, size, is_round=False)
    with open(os.path.join(dir_path, 'ic_launcher.png'), 'wb') as f:
        f.write(square_png)
        
    round_png = create_png(size, size, is_round=True)
    with open(os.path.join(dir_path, 'ic_launcher_round.png'), 'wb') as f:
        f.write(round_png)

print("All PNG icons generated successfully!")
