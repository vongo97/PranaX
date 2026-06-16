import os
from PIL import Image
from collections import Counter

def generate_android_icons():
    # Cargar el cuadrante 4
    img_path = 'logo_quadrant_4.png'
    img = Image.open(img_path).convert('RGB')
    
    # Suponiendo que el logo está en el centro superior y el texto abajo.
    # Vamos a recortar el área donde probablemente esté el logo (evitando los bordes y el fondo).
    # Un cuadrante mide 512x512. El texto suele estar debajo de y=400.
    # Recortaremos un cuadrado central superior: x de 60 a 452, y de 40 a 432
    logo_crop = img.crop((60, 40, 452, 432))
    
    # Ahora tenemos un logo de 392x392.
    # Obtener el color predominante del borde para usarlo como fondo
    left_edge = [img.getpixel((0, y)) for y in range(512)]
    top_edge = [img.getpixel((x, 0)) for x in range(512)]
    bg_color = Counter(left_edge + top_edge).most_common(1)[0][0]
    
    # Crear un lienzo cuadrado nuevo de 512x512 con ese fondo
    final_icon = Image.new('RGB', (512, 512), bg_color)
    
    # Pegar el logo en el centro
    offset_x = (512 - logo_crop.width) // 2
    offset_y = (512 - logo_crop.height) // 2
    final_icon.paste(logo_crop, (offset_x, offset_y))
    
    # Redimensionar para las densidades de Android
    sizes = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }
    
    res_dir = r'C:\Users\vongo\OneDrive\Escritorio\Proyectos\App_android\BreathingApp\app\src\main\res'
    
    for density, size in sizes.items():
        folder = os.path.join(res_dir, f'mipmap-{density}')
        os.makedirs(folder, exist_ok=True)
        
        # Guardar icono cuadrado
        icon = final_icon.resize((size, size), Image.Resampling.LANCZOS)
        icon.save(os.path.join(folder, 'ic_launcher.png'))
        
        # Guardar ícono circular (ic_launcher_round)
        # Crear máscara circular
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        
        round_icon = icon.copy()
        round_icon.putalpha(mask)
        round_icon.save(os.path.join(folder, 'ic_launcher_round.png'))
        
    print("Íconos de PranaX generados e insertados con éxito.")

if __name__ == '__main__':
    generate_android_icons()
