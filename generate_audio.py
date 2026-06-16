import wave
import struct
import math
import random

SAMPLE_RATE = 44100
DURATION = 5  # seconds
NUM_SAMPLES = SAMPLE_RATE * DURATION

def write_wav(filename, samples):
    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(SAMPLE_RATE)
        for s in samples:
            # Clamp between -32767 and 32767
            val = int(max(min(s * 32767, 32767), -32767))
            wav_file.writeframes(struct.pack('<h', val))

print("Generating Ocean...")
# Ocean: Low pass filtered brown/pink noise.
# We'll generate brownian noise (integration of white noise)
ocean_samples = []
last_val = 0.0
for _ in range(NUM_SAMPLES):
    white = random.uniform(-1.0, 1.0)
    last_val = (last_val + white * 0.05) / 1.05
    ocean_samples.append(last_val * 2.0)
write_wav("ocean.wav", ocean_samples)

print("Generating Wind...")
# Wind: Pink noise (we'll approximate with multiple octaves of white noise)
wind_samples = []
b0 = b1 = b2 = b3 = b4 = b5 = b6 = 0.0
for _ in range(NUM_SAMPLES):
    white = random.uniform(-1.0, 1.0)
    b0 = 0.99886 * b0 + white * 0.0555179
    b1 = 0.99332 * b1 + white * 0.0750759
    b2 = 0.96900 * b2 + white * 0.1538520
    b3 = 0.86650 * b3 + white * 0.3104856
    b4 = 0.55000 * b4 + white * 0.5329522
    b5 = -0.7616 * b5 - white * 0.0168980
    pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
    b6 = white * 0.115926
    wind_samples.append(pink * 0.1)
write_wav("wind.wav", wind_samples)

print("Generating Rain...")
# Rain: White noise with a high pass filter + some random drops (clicks)
rain_samples = []
last_rain = 0.0
for _ in range(NUM_SAMPLES):
    white = random.uniform(-1.0, 1.0)
    # High pass filter
    val = white - last_rain
    last_rain = white
    # Drops
    if random.random() < 0.005:
        val += random.uniform(-1.0, 1.0) * 2.0
    rain_samples.append(val * 0.3)
write_wav("rain.wav", rain_samples)

print("Generating Bowl Drone...")
# Bowl Drone: Mix of 432Hz sine wave and its harmonics
bowl_samples = []
for i in range(NUM_SAMPLES):
    t = i / SAMPLE_RATE
    freq = 432.0
    sine1 = math.sin(2 * math.pi * freq * t)
    sine2 = math.sin(2 * math.pi * (freq * 2.01) * t) * 0.3
    sine3 = math.sin(2 * math.pi * (freq * 3.02) * t) * 0.1
    val = (sine1 + sine2 + sine3) * 0.5
    # Fade in/out slightly to make it seamless loop (very basic)
    env = 1.0
    if i < SAMPLE_RATE * 0.5:
        env = i / (SAMPLE_RATE * 0.5)
    elif i > NUM_SAMPLES - (SAMPLE_RATE * 0.5):
        env = (NUM_SAMPLES - i) / (SAMPLE_RATE * 0.5)
    bowl_samples.append(val * env)
write_wav("bowl.wav", bowl_samples)

print("Done!")
