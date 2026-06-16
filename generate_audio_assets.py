import math
import wave
import struct
import random
import os

RAW_DIR = "app/src/main/res/raw"

def generate_tibetan_bowl(filename, freqs, amplitudes, decays, duration_sec=6.0, sample_rate=44100):
    num_samples = int(duration_sec * sample_rate)
    filepath = os.path.join(RAW_DIR, filename)
    
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        for i in range(num_samples):
            t = float(i) / sample_rate
            sample = 0.0
            attack = min(1.0, t / 0.05)
            for f, a, d in zip(freqs, amplitudes, decays):
                envelope = attack * math.exp(-t * d)
                sample += a * envelope * math.sin(2.0 * math.pi * f * t)
            
            sample = max(-1.0, min(1.0, sample))
            int_sample = int(sample * 32767.0)
            data = struct.pack('<h', int_sample)
            wav_file.writeframesraw(data)

def generate_brown_noise(filename, duration_sec=30.0, sample_rate=44100):
    num_samples = int(duration_sec * sample_rate)
    filepath = os.path.join(RAW_DIR, filename)
    
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        last_val = 0.0
        for i in range(num_samples):
            white = random.uniform(-1.0, 1.0)
            last_val = (last_val + 0.02 * white) / 1.02
            
            t = float(i) / sample_rate
            envelope = 1.0
            if t < 2.0: envelope = t / 2.0
            elif t > duration_sec - 2.0: envelope = (duration_sec - t) / 2.0
                
            sample = last_val * 4.0 * envelope
            sample = max(-1.0, min(1.0, sample))
            int_sample = int(sample * 32767.0)
            data = struct.pack('<h', int_sample)
            wav_file.writeframesraw(data)

def generate_rain(filename, duration_sec=30.0, sample_rate=44100):
    num_samples = int(duration_sec * sample_rate)
    filepath = os.path.join(RAW_DIR, filename)
    
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        last_val = 0.0
        for i in range(num_samples):
            white = random.uniform(-1.0, 1.0)
            # Filtro pasa-altos simple para simular sonido de lluvia
            last_val = white - last_val * 0.9
            
            # Gotitas de agua cayendo de forma aleatoria (pequeños impulsos rápidos)
            drip = 0.0
            if random.random() < 0.0003:
                drip = random.uniform(0.15, 0.45)
                
            t = float(i) / sample_rate
            envelope = 1.0
            if t < 2.0: envelope = t / 2.0
            elif t > duration_sec - 2.0: envelope = (duration_sec - t) / 2.0
                
            sample = (last_val * 0.06 + drip * 0.35) * envelope
            sample = max(-1.0, min(1.0, sample))
            int_sample = int(sample * 32767.0)
            data = struct.pack('<h', int_sample)
            wav_file.writeframesraw(data)

def generate_ocean(filename, duration_sec=30.0, sample_rate=44100):
    num_samples = int(duration_sec * sample_rate)
    filepath = os.path.join(RAW_DIR, filename)
    
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        last_val = 0.0
        for i in range(num_samples):
            white = random.uniform(-1.0, 1.0)
            last_val = (last_val + 0.02 * white) / 1.02 # Ruido marrón
            
            # Modulación periódica lenta (LFO de ~6.5 segundos para simular las olas)
            t = float(i) / sample_rate
            wave_mod = 0.45 + 0.55 * math.sin(2.0 * math.pi * t / 6.5)
            
            envelope = 1.0
            if t < 2.0: envelope = t / 2.0
            elif t > duration_sec - 2.0: envelope = (duration_sec - t) / 2.0
                
            sample = last_val * 4.0 * wave_mod * envelope
            sample = max(-1.0, min(1.0, sample))
            int_sample = int(sample * 32767.0)
            data = struct.pack('<h', int_sample)
            wav_file.writeframesraw(data)

print("Borrando viejos MP3 incompatibles...")
for f in ["chime.mp3", "bowl_high.mp3", "bowl_low.mp3", "forest_bg.mp3"]:
    p = os.path.join(RAW_DIR, f)
    if os.path.exists(p):
        os.remove(p)

print("Generando Cuenco Grave...")
generate_tibetan_bowl("bowl_low.wav", [185.0, 555.0, 925.0], [0.6, 0.25, 0.1], [1.5, 2.5, 4.0])

print("Generando Cuenco Agudo...")
generate_tibetan_bowl("bowl_high.wav", [310.0, 930.0, 1550.0], [0.6, 0.25, 0.1], [1.5, 2.5, 4.0])

print("Generando Campana de Cristal...")
generate_tibetan_bowl("chime.wav", 
                      [800.0, 804.0, 1620.0, 2450.0], 
                      [0.4, 0.4, 0.2, 0.1], 
                      [1.5, 1.5, 3.5, 6.0], 
                      duration_sec=2.5)

print("Generando Sonido de Viento/Bosque...")
generate_brown_noise("forest_bg.wav", duration_sec=30.0)

print("Generando Sonido de Lluvia...")
generate_rain("rain_bg.wav", duration_sec=30.0)

print("Generando Sonido de Océano...")
generate_ocean("ocean_bg.wav", duration_sec=30.0)

print("¡Todos los sonidos WAV nativos han sido generados con éxito!")
