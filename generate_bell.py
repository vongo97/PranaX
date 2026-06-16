import math
import wave
import struct
import os

def generate_tibetan_bowl(filename, freqs, amplitudes, decays, duration_sec=3.0, sample_rate=44100):
    num_samples = int(duration_sec * sample_rate)
    
    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        for i in range(num_samples):
            t = float(i) / sample_rate
            sample = 0.0
            attack = min(1.0, t / 0.02) # Ataque mucho más rápido
            
            for f, a, d in zip(freqs, amplitudes, decays):
                envelope = attack * math.exp(-t * d)
                sample += a * envelope * math.sin(2.0 * math.pi * f * t)
            
            # Ligera saturación cálida (soft clipping) para que suene más orgánico
            sample = math.tanh(sample * 1.5)
            sample = max(-1.0, min(1.0, sample))
            
            int_sample = int(sample * 32767.0)
            data = struct.pack('<h', int_sample)
            wav_file.writeframesraw(data)

# Frecuencias inarmónicas (no exactas) y "beating" (vibración acústica) para que no suene a robot.
# 800 y 804 Hz crean una oscilación natural. 
# Duración corta de 2.5 segundos.
print("Generando campana acústica corta...")
generate_tibetan_bowl("campana_acustica.wav", 
                      [800.0, 804.0, 1620.0, 2450.0], # Frecuencias base + batimiento + inarmónicos
                      [0.4, 0.4, 0.2, 0.1],           # Amplitudes
                      [1.5, 1.5, 3.5, 6.0],           # Decaimiento muy rápido para agudos
                      duration_sec=2.5)
print("¡Listo!")
