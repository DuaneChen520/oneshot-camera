#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

ICONS = {
    "aspect_ratio_open_gate": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- 4:3 Open Gate Optical Reticle Corners -->
  <path d="M 3 7 V 5.5 A 2 2 0 0 1 5.5 3 H 7" />
  <path d="M 17 3 H 18.5 A 2 2 0 0 1 21 5.5 V 7" />
  <path d="M 21 17 V 18.5 A 2 2 0 0 1 18.5 21 H 17" />
  <path d="M 7 21 H 5.5 A 2 2 0 0 1 3 18.5 V 17" />
  <!-- 16:9 Scope Aspect Inner Frame (Cinema Guide) -->
  <rect x="3" y="7.5" width="18" height="9" rx="1" stroke-dasharray="2 1.5" opacity="0.65" />
  <!-- Center Optical Axis Crosshair -->
  <line x1="12" y1="10.5" x2="12" y2="13.5" />
  <line x1="10.5" y1="12" x2="13.5" y2="12" />
</svg>""",

    "lens_mount_flip": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Outer Cine Bayonet Dual Rotation Arrows -->
  <path d="M 4.5 12 A 8 8 0 0 1 18.2 5.8 M 15 3.5 L 18.5 5.8 L 15.5 8.5" />
  <path d="M 19.5 12 A 8 8 0 0 1 5.8 18.2 M 9 20.5 L 5.5 18.2 L 8.5 15.5" />
  <!-- PL Lens Mount Flange Ring -->
  <circle cx="12" cy="12" r="4.5" />
  <!-- Locking Lugs (120-deg Cine Standard) -->
  <line x1="11" y1="7.5" x2="13" y2="7.5" />
  <line x1="8.2" y1="14" x2="9.2" y2="12.4" />
  <line x1="15.8" y1="14" x2="14.8" y2="12.4" />
  <!-- Central Optical Core -->
  <circle cx="12" cy="12" r="1.5" fill="currentColor" />
</svg>""",

    "film_cartridge_35mm": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- 35mm Metal Canister Body -->
  <rect x="4" y="6" width="10" height="15" rx="2" />
  <!-- Top Spool Nub -->
  <path d="M 7 6 V 3.5 A 0.5 0.5 0 0 1 7.5 3 H 10.5 A 0.5 0.5 0 0 1 11 3.5 V 6" />
  <!-- Bottom Spool End -->
  <line x1="7.5" y1="21" x2="10.5" y2="21" />
  <!-- Film Exit Velvet Lip & Strip Tongue -->
  <path d="M 14 8.5 H 19.5 C 20.3 8.5 21 9.2 21 10 V 14.5 C 21 15.3 20.3 16 19.5 16 H 14" />
  <!-- 35mm Sprocket Perforations on Leader -->
  <rect x="16" y="10.5" width="2.2" height="3.5" rx="0.5" fill="currentColor" />
  <!-- Metal Rib Grooves -->
  <line x1="4" y1="10.5" x2="14" y2="10.5" opacity="0.4" />
  <line x1="4" y1="16.5" x2="14" y2="16.5" opacity="0.4" />
</svg>""",

    "film_stock_lut": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Film Strip Chassis Frame -->
  <rect x="3" y="3" width="18" height="18" rx="2.5" />
  <!-- Left Sprocket Holes -->
  <rect x="4.8" y="5.5" width="2.2" height="2.2" rx="0.4" fill="currentColor" />
  <rect x="4.8" y="10.9" width="2.2" height="2.2" rx="0.4" fill="currentColor" />
  <rect x="4.8" y="16.3" width="2.2" height="2.2" rx="0.4" fill="currentColor" />
  <!-- Right Sprocket Holes -->
  <rect x="17" y="5.5" width="2.2" height="2.2" rx="0.4" fill="currentColor" />
  <rect x="17" y="10.9" width="2.2" height="2.2" rx="0.4" fill="currentColor" />
  <rect x="17" y="16.3" width="2.2" height="2.2" rx="0.4" fill="currentColor" />
  <!-- Film Exposure Gate Window -->
  <rect x="9" y="5" width="6" height="14" rx="1" opacity="0.6" />
  <!-- 3D LUT Color Transform Contour Wave -->
  <path d="M 9.5 14 C 11 14 11 10 12 10 C 13 10 13 12 14.5 12" />
  <circle cx="12" cy="10" r="1" fill="currentColor" />
</svg>""",

    "fps_shutter_angle": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Rotary Shutter Housing Rim (360 degrees) -->
  <circle cx="12" cy="12" r="9" />
  <!-- 180-degree Rotary Blade Sector (Cine Standard 1/48s at 24fps) -->
  <path d="M 12 12 L 12 3 A 9 9 0 0 1 21 12 Z" fill="currentColor" opacity="0.3" />
  <!-- Rotary Sector Border Lines -->
  <line x1="12" y1="12" x2="12" y2="3" />
  <line x1="12" y1="12" x2="21" y2="12" />
  <!-- Central Precision Hub & Bearing -->
  <circle cx="12" cy="12" r="2.8" />
  <circle cx="12" cy="12" r="1.2" fill="currentColor" />
  <!-- Shutter Rotation Speed Arc Indicator -->
  <path d="M 6.5 8 A 7.5 7.5 0 0 0 4.5 12" stroke-dasharray="1.5 1.5" />
</svg>""",

    "dynamic_range_hlg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Optical Transfer Curve (Log / Hybrid Log Gamma) -->
  <path d="M 3.5 19.5 C 7.5 19.5 11 16 13.5 11.5 C 15.5 8 18 5.5 21 5" />
  <!-- Baseline Coordinate Scales -->
  <path d="M 3.5 4.5 V 19.5 H 20.5" opacity="0.4" />
  <!-- 10-bit Quantization Steps (Highlight Headroom) -->
  <line x1="8.5" y1="19.5" x2="8.5" y2="18" opacity="0.6" />
  <line x1="13.5" y1="19.5" x2="13.5" y2="16.5" opacity="0.6" />
  <line x1="18.5" y1="19.5" x2="18.5" y2="14" opacity="0.6" />
  <!-- Cine HDR Optical Flare Star -->
  <path d="M 19 6.5 L 19.5 8 L 21 8.5 L 19.5 9 L 19 10.5 L 18.5 9 L 17 8.5 L 18.5 8 Z" fill="currentColor" />
</svg>""",

    "dcg_dual_gain": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Sensor Photodiode Cell (Charge Source) -->
  <rect x="8" y="3" width="8" height="5" rx="1.5" />
  <line x1="10" y1="5.5" x2="14" y2="5.5" />
  <!-- Dual Branch Transmission Gate -->
  <line x1="12" y1="8" x2="12" y2="10.5" />
  <path d="M 12 10.5 L 6.5 13.5 M 12 10.5 L 17.5 13.5" />
  <circle cx="12" cy="10.5" r="1.2" fill="currentColor" />
  <!-- Left Branch: Low Conversion Gain (LCG - High Full-Well Capacity) -->
  <line x1="6.5" y1="13.5" x2="6.5" y2="16" />
  <line x1="4.5" y1="16" x2="8.5" y2="16" />
  <line x1="4.5" y1="18" x2="8.5" y2="18" />
  <line x1="6.5" y1="18" x2="6.5" y2="21" />
  <!-- Right Branch: High Conversion Gain (HCG - Ultra Low Noise Floor) -->
  <line x1="17.5" y1="13.5" x2="17.5" y2="16" />
  <line x1="15.5" y1="16" x2="19.5" y2="16" />
  <line x1="15.5" y1="18" x2="19.5" y2="18" />
  <line x1="17.5" y1="18" x2="17.5" y2="21" />
  <!-- Common Summing Node -->
  <line x1="6.5" y1="21" x2="17.5" y2="21" />
</svg>""",

    "focus_peaking": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Central Focus Target Reticle -->
  <circle cx="12" cy="12" r="3.5" />
  <circle cx="12" cy="12" r="1.2" fill="currentColor" />
  <line x1="12" y1="4" x2="12" y2="7.5" />
  <line x1="12" y1="16.5" x2="12" y2="20" />
  <line x1="4" y1="12" x2="7.5" y2="12" />
  <line x1="16.5" y1="12" x2="20" y2="12" />
  <!-- High-Frequency Edge Detection Peaks (Contrast Scan) -->
  <path d="M 3.5 17.5 L 6 13.5 L 8 16.5 L 10 14" opacity="0.8" />
  <path d="M 14 14 L 16 16.5 L 18 13.5 L 20.5 17.5" opacity="0.8" />
</svg>""",

    "zebra_stripes": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Monitor Exposure Viewfinder Window -->
  <rect x="3" y="4" width="18" height="16" rx="2" />
  <!-- 45-degree High-Exposure Warning Zebra Slits -->
  <line x1="5.5" y1="18.5" x2="9" y2="15" />
  <line x1="6" y1="13.5" x2="13.5" y2="6" />
  <line x1="9.5" y1="18.5" x2="17.5" y2="10.5" />
  <line x1="13.5" y1="18.5" x2="18.5" y2="13.5" />
  <line x1="17" y1="18.5" x2="18.5" y2="17" />
  <!-- 100% IRE Clipping Warning Pip -->
  <path d="M 16 6.5 L 18 9.5 H 14 Z" fill="currentColor" opacity="0.5" />
</svg>""",

    "motion_photo_live": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Core Decisive Frame (Instantaneous Capture) -->
  <rect x="8" y="8" width="8" height="8" rx="1.5" />
  <circle cx="12" cy="12" r="1.5" fill="currentColor" />
  <!-- Inner Ambient Audio-Temporal Concentric Ring -->
  <circle cx="12" cy="12" r="6.2" stroke-dasharray="2 1.8" opacity="0.6" />
  <!-- Outer Live Emulsion Motion Waves -->
  <path d="M 12 2 A 10 10 0 0 1 22 12" />
  <path d="M 12 22 A 10 10 0 0 1 2 12" />
  <!-- Live Motion Playback Micro Indicator -->
  <polygon points="18.5,16.5 21,18 18.5,19.5" fill="currentColor" />
</svg>""",

    "lut_burn_in": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Lower Film Substrate Bed -->
  <path d="M 3.5 18.5 H 20.5 A 1 1 0 0 0 21.5 17.5 V 14.5 A 1 1 0 0 0 20.5 13.5 H 3.5 A 1 1 0 0 0 2.5 14.5 V 17.5 A 1 1 0 0 0 3.5 18.5 Z" />
  <rect x="5.5" y="15" width="2" height="2" rx="0.4" fill="currentColor" />
  <rect x="16.5" y="15" width="2" height="2" rx="0.4" fill="currentColor" />
  <!-- Upper Laser / Chemical Burn-In Emitter Nozzle -->
  <path d="M 9.5 3 H 14.5 L 13 7 H 11 Z" />
  <!-- Focused Solidifying Energy Beam -->
  <line x1="11" y1="7" x2="8.5" y2="13.5" stroke-dasharray="1.5 1" opacity="0.5" />
  <line x1="13" y1="7" x2="15.5" y2="13.5" stroke-dasharray="1.5 1" opacity="0.5" />
  <line x1="12" y1="7" x2="12" y2="13.5" />
  <!-- Pure Film Chemistry Fusion Spark -->
  <path d="M 12 11.5 L 12.8 12.8 L 14.2 13.5 L 12.8 14.2 L 12 15.5 L 11.2 14.2 L 9.8 13.5 L 11.2 12.8 Z" fill="currentColor" />
</svg>""",

    "photo_format_raw": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Sensor Bayer Filter Frame -->
  <rect x="3" y="3" width="18" height="18" rx="2.5" />
  <!-- Optical Isolation Channel Dividers -->
  <line x1="12" y1="3" x2="12" y2="21" opacity="0.4" />
  <line x1="3" y1="12" x2="21" y2="12" opacity="0.4" />
  <!-- Top-Left Red Pixel (R) -->
  <circle cx="7.5" cy="7.5" r="2.2" />
  <!-- Top-Right Green Pixel (Gr) -->
  <rect x="14.3" y="5.3" width="4.4" height="4.4" rx="1" fill="currentColor" opacity="0.35" />
  <!-- Bottom-Left Green Pixel (Gb) -->
  <rect x="5.3" y="14.3" width="4.4" height="4.4" rx="1" fill="currentColor" opacity="0.35" />
  <!-- Bottom-Right Blue Pixel (B) -->
  <circle cx="16.5" cy="16.5" r="2.2" fill="currentColor" />
</svg>""",

    "video_codec_cinema": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Cinema Digital Magazine / Tape Cassette Housing -->
  <rect x="3" y="5" width="18" height="14" rx="2" />
  <!-- Left Spool Hub -->
  <circle cx="8" cy="12" r="3" />
  <circle cx="8" cy="12" r="1.2" fill="currentColor" />
  <!-- Right Spool Hub -->
  <circle cx="16" cy="12" r="3" />
  <circle cx="16" cy="12" r="1.2" fill="currentColor" />
  <!-- Inter-Spool Film Tape Bridge -->
  <line x1="8" y1="9" x2="16" y2="9" />
  <!-- Precision Read/Write Gate Window -->
  <path d="M 9.5 16 H 14.5 L 13.5 14 H 10.5 Z" opacity="0.7" />
  <!-- Data Tape Perforation Marks -->
  <line x1="4.5" y1="7" x2="5.5" y2="7" />
  <line x1="18.5" y1="7" x2="19.5" y2="7" />
</svg>""",

    "histogram_parade": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Director Monitor Scope Window -->
  <rect x="3" y="4" width="18" height="16" rx="2" />
  <!-- 18% Neutral Gray Mid-Tone Reference Line -->
  <line x1="4" y1="12" x2="20" y2="12" stroke-dasharray="2 1.5" opacity="0.4" />
  <!-- Discrete RGB Histogram & Luminance Waveform -->
  <path d="M 4.5 17.5 L 6.5 14 L 8.5 15.5 L 11 9 L 13 12 L 15.5 7 L 17.5 14.5 L 19.5 17.5" />
  <!-- Core Exposure Crest Pillars -->
  <line x1="11" y1="9" x2="11" y2="17.5" opacity="0.4" />
  <line x1="15.5" y1="7" x2="15.5" y2="17.5" opacity="0.4" />
</svg>""",

    "video_quality_4k": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Precision DCI Cinema Monitor Bezel -->
  <rect x="3" y="5" width="18" height="14" rx="2" />
  <!-- Geometric "4" Glyphs -->
  <path d="M 6.8 9.2 V 12.4 H 10.5 M 9.6 9.2 V 14.8" />
  <!-- Geometric "K" Glyphs -->
  <path d="M 13.5 9.2 V 14.8 M 17.2 9.2 L 14.6 12 L 17.5 14.8" />
  <!-- Calibration Tick Marks on Edges -->
  <line x1="12" y1="5" x2="12" y2="6.5" opacity="0.6" />
  <line x1="12" y1="17.5" x2="12" y2="19" opacity="0.6" />
</svg>""",

    "color_space_gamut": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- CIE 1931 Spectral Locus Horseshoe Curve -->
  <path d="M 4 20 L 4 10.5 C 4 6.5 7.5 3.5 12 3.5 C 16.5 3.5 20 6.5 20 12 C 20 16 18.5 18 16 20 Z" />
  <!-- Display P3 / Rec.2020 Cinema Triangle -->
  <path d="M 12 6.8 L 18 17.8 H 6 Z" opacity="0.75" />
  <!-- D65 Illuminant Reference White Point -->
  <circle cx="12" cy="14" r="1.2" fill="currentColor" />
</svg>""",

    "shutter_mechanical": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Mechanical Lens Barrel Knurled Rim -->
  <circle cx="12" cy="12" r="9.5" />
  <!-- Interlocking Mechanical Shutter Leaves -->
  <path d="M 12 2.5 C 13.5 6 14.5 9 12 12" />
  <path d="M 20.2 7.3 C 17.5 9.5 14.5 11 12 12" />
  <path d="M 20.2 16.7 C 16.5 16 13.5 14.5 12 12" />
  <path d="M 12 21.5 C 10.5 18 9.5 15 12 12" />
  <path d="M 3.8 16.7 C 6.5 14.5 9.5 13 12 12" />
  <path d="M 3.8 7.3 C 7.5 8 10.5 9.5 12 12" />
  <!-- Aperture Center Pin-hole -->
  <circle cx="12" cy="12" r="2.2" opacity="0.5" />
</svg>""",

    "gallery_film_frame": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- 35mm Slide Mount Outer Frame -->
  <rect x="3" y="4" width="18" height="16" rx="2" />
  <!-- Top Perforations -->
  <rect x="5.5" y="5.5" width="2" height="1.8" rx="0.4" fill="currentColor" />
  <rect x="9.5" y="5.5" width="2" height="1.8" rx="0.4" fill="currentColor" />
  <rect x="13.5" y="5.5" width="2" height="1.8" rx="0.4" fill="currentColor" />
  <rect x="17.5" y="5.5" width="2" height="1.8" rx="0.4" fill="currentColor" />
  <!-- Bottom Perforations -->
  <rect x="5.5" y="16.7" width="2" height="1.8" rx="0.4" fill="currentColor" />
  <rect x="9.5" y="16.7" width="2" height="1.8" rx="0.4" fill="currentColor" />
  <rect x="13.5" y="16.7" width="2" height="1.8" rx="0.4" fill="currentColor" />
  <rect x="17.5" y="16.7" width="2" height="1.8" rx="0.4" fill="currentColor" />
  <!-- Exposed Film Gate Image -->
  <rect x="5.5" y="8.8" width="13" height="6.4" rx="0.8" opacity="0.4" />
  <path d="M 6.5 14 L 9.5 11 L 12 13 L 14.5 10.5 L 17.5 14 Z" fill="currentColor" opacity="0.25" />
  <circle cx="16" cy="10.5" r="0.8" fill="currentColor" />
</svg>""",

    "cinema_reel_magazine": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Coaxial Twin-Drum Magazine (Supply & Take-up Drums) -->
  <circle cx="8" cy="9.5" r="5.5" />
  <circle cx="16" cy="9.5" r="5.5" />
  <circle cx="8" cy="9.5" r="1.5" fill="currentColor" />
  <circle cx="16" cy="9.5" r="1.5" fill="currentColor" />
  <!-- Film Gate Throat Passage to Camera Core -->
  <path d="M 6.2 14.5 L 7.5 20.5 H 16.5 L 17.8 14.5" />
  <!-- Film Drive Sprocket Roller in Throat -->
  <circle cx="12" cy="17.5" r="1.8" />
  <circle cx="12" cy="17.5" r="0.6" fill="currentColor" />
</svg>""",

    "q_menu_matrix": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Milled Aircraft-Grade Alloy Q-Panel Chassis -->
  <rect x="3" y="3" width="18" height="18" rx="3.5" />
  <!-- 2x3 Precision Tactical Matrix Push-Buttons -->
  <rect x="5.5" y="5.5" width="5" height="4" rx="1" />
  <rect x="13.5" y="5.5" width="5" height="4" rx="1" />
  <rect x="5.5" y="11" width="5" height="4" rx="1" />
  <!-- Active Illuminated Key Slot (Amber Grain Glow) -->
  <rect x="13.5" y="11" width="5" height="4" rx="1" fill="currentColor" opacity="0.75" />
  <rect x="5.5" y="16.5" width="5" height="2.2" rx="0.7" opacity="0.5" />
  <rect x="13.5" y="16.5" width="5" height="2.2" rx="0.7" opacity="0.5" />
</svg>""",

    "ultra_hdr_spark": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <path d="M 12 3 C 12 7.5 15.5 11 20 11 C 15.5 11 12 14.5 12 19 C 12 14.5 8.5 11 4 11 C 8.5 11 12 7.5 12 3 Z" fill="currentColor" opacity="0.25" />
  <path d="M 12 3 C 12 7.5 15.5 11 20 11 C 15.5 11 12 14.5 12 19 C 12 14.5 8.5 11 4 11 C 8.5 11 12 7.5 12 3 Z" />
  <circle cx="12" cy="11" r="1.5" fill="currentColor" />
  <circle cx="19" cy="4" r="1" fill="currentColor" />
</svg>""",

    "thermal_monitor": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Thermometer Sensor Body -->
  <path d="M 14 14.76 V 5 A 2 2 0 0 0 10 5 V 14.76 A 4 4 0 1 0 14 14.76 Z" />
  <circle cx="12" cy="17" r="1.8" fill="currentColor" />
  <line x1="12" y1="9" x2="12" y2="14" stroke-width="2" />
  <!-- Thermal Radiation Flow Rays -->
  <line x1="17" y1="6" x2="20" y2="6" opacity="0.6" />
  <line x1="17" y1="10" x2="19" y2="10" opacity="0.6" />
</svg>""",

    "framing_reticle_cross": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <!-- Optical Ground Glass Framing Marks -->
  <path d="M 4 8 V 4 H 8" />
  <path d="M 16 4 H 20 V 8" />
  <path d="M 20 16 V 20 H 16" />
  <path d="M 8 20 H 4 V 16" />
  <!-- Center Scope Reticle Cross -->
  <circle cx="12" cy="12" r="2.5" />
  <line x1="12" y1="2" x2="12" y2="6" opacity="0.5" />
  <line x1="12" y1="18" x2="12" y2="22" opacity="0.5" />
  <line x1="2" y1="12" x2="6" y2="12" opacity="0.5" />
  <line x1="18" y1="12" x2="22" y2="12" opacity="0.5" />
</svg>"""
}

def main():
    dirs = ["preview/icons", "app/src/main/res/drawable"]
    for d in dirs:
        os.makedirs(d, exist_ok=True)

    for name, svg in ICONS.items():
        # Write to preview/icons
        p_path = os.path.join("preview/icons", f"{name}.svg")
        with open(p_path, "w", encoding="utf-8") as f:
            f.write(svg.strip() + "\n")

        # Write to app/src/main/res/drawable
        a_path = os.path.join("app/src/main/res/drawable", f"ic_{name}.svg")
        with open(a_path, "w", encoding="utf-8") as f:
            f.write(svg.strip() + "\n")

    print(f"Generated {len(ICONS)} SVG icons successfully into preview/icons and app/src/main/res/drawable!")

if __name__ == "__main__":
    main()
