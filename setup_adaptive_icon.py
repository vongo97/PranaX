import os
from PIL import Image

def setup_adaptive_icons():
    # Cargar el ícono puro
    img_path = r'C:\Users\vongo\.gemini\antigravity-ide\brain\fdda2fc6-2419-496d-ac94-58e7459fe4a9\pranax_pure_icon_1780700984900.png'
    img = Image.open(img_path).convert("RGBA")
    
    # Remover fondo blanco
    datas = img.getdata()
    newData = []
    for item in datas:
        if item[0] > 240 and item[1] > 240 and item[2] > 240:
            newData.append((255, 255, 255, 0))
        else:
            newData.append(item)
    img.putdata(newData)
    
    # Recortar espacios vacíos extra si los hay
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
        
    # Crear un lienzo grande y centrar el logo para que tenga un margen interno
    # Los íconos adaptativos requieren una zona de seguridad (72x72 en un lienzo de 108x108)
    canvas_size = 1024
    safe_zone = 650
    final_icon = Image.new('RGBA', (canvas_size, canvas_size), (255, 255, 255, 0))
    
    # Redimensionar el logo para que quepa en la zona segura
    ratio = min(safe_zone / img.width, safe_zone / img.height)
    new_w = int(img.width * ratio)
    new_h = int(img.height * ratio)
    img_resized = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    offset_x = (canvas_size - new_w) // 2
    offset_y = (canvas_size - new_h) // 2
    final_icon.paste(img_resized, (offset_x, offset_y), img_resized)
    
    sizes = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }
    
    res_dir = r'C:\Users\vongo\OneDrive\Escritorio\Proyectos\App_android\BreathingApp\app\src\main\res'
    
    # Escribir el foreground
    for density, size in sizes.items():
        # Para adaptive icon, el foreground es 108x108 dp
        adaptive_size = int(size * (108/48))
        folder = os.path.join(res_dir, f'mipmap-{density}')
        os.makedirs(folder, exist_ok=True)
        
        icon = final_icon.resize((adaptive_size, adaptive_size), Image.Resampling.LANCZOS)
        icon.save(os.path.join(folder, 'ic_launcher_foreground.png'))
        
        # Guardar icono tradicional (legacy) para teléfonos antiguos
        legacy_icon = Image.new('RGBA', (size, size), (255, 255, 255, 255))
        legacy_icon.paste(icon.resize((size, size)), (0,0), icon.resize((size, size)))
        legacy_icon.save(os.path.join(folder, 'ic_launcher.png'))
        
        # Round legacy
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        round_icon = legacy_icon.copy()
        round_icon.putalpha(mask)
        round_icon.save(os.path.join(folder, 'ic_launcher_round.png'))

    # Escribir el XML del Adaptive Icon
    anydpi_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')
    os.makedirs(anydpi_dir, exist_ok=True)
    
    xml_content = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/white"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
"""
    with open(os.path.join(anydpi_dir, 'ic_launcher.xml'), 'w') as f:
        f.write(xml_content)
    with open(os.path.join(anydpi_dir, 'ic_launcher_round.xml'), 'w') as f:
        f.write(xml_content)
        
    print("Íconos Adaptativos inyectados!")

if __name__ == '__main__':
    setup_adaptive_icons()
