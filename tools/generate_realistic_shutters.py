#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generates Hyper-Realistic Skeuomorphic Cinema Shutter & Button SVG Assets
"""
import os

REALISTIC_SVGS = {
    # 1. 35mm Slide Negative Mount with Sprocket Holes & Film Preview
    "shutter_gallery_film": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
  <defs>
    <!-- Film Base Gradient (Amber/Bronze Acetate Film Stock) -->
    <linearGradient id="filmBaseGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#24201c"/>
      <stop offset="50%" stop-color="#181512"/>
      <stop offset="100%" stop-color="#0e0d0b"/>
    </linearGradient>

    <!-- Slide Plastic/Metal Outer Bezel -->
    <linearGradient id="slideBezelGrad" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#3c3c44"/>
      <stop offset="15%" stop-color="#28282e"/>
      <stop offset="85%" stop-color="#18181c"/>
      <stop offset="100%" stop-color="#0e0e11"/>
    </linearGradient>

    <!-- Glossy Glass Surface Glare Reflection -->
    <linearGradient id="glassGlareGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#ffffff" stop-opacity="0.35"/>
      <stop offset="35%" stop-color="#ffffff" stop-opacity="0.08"/>
      <stop offset="50%" stop-color="#ffffff" stop-opacity="0.0"/>
      <stop offset="80%" stop-color="#ffffff" stop-opacity="0.0"/>
      <stop offset="100%" stop-color="#ffffff" stop-opacity="0.15"/>
    </linearGradient>

    <!-- Clip Path for the Inner Photographic Emulsion Window -->
    <clipPath id="filmGateClip">
      <rect x="10" y="16" width="44" height="32" rx="2.5"/>
    </clipPath>
  </defs>

  <!-- Drop Shadow under Slide -->
  <rect x="3" y="5" width="58" height="56" rx="6" fill="#000000" opacity="0.6" filter="blur(2px)"/>

  <!-- 35mm Slide Mount Chassis -->
  <rect x="4" y="4" width="56" height="56" rx="5" fill="url(#slideBezelGrad)" stroke="#4e4e58" stroke-width="0.8"/>
  <rect x="6" y="6" width="52" height="52" rx="4" fill="url(#filmBaseGrad)" stroke="#1a1a1e" stroke-width="0.5"/>

  <!-- TOP SPROCKET HOLE ROW -->
  <g fill="#070709" stroke="#322d26" stroke-width="0.5">
    <rect x="9" y="8.5" width="5.5" height="4.5" rx="1"/>
    <rect x="19" y="8.5" width="5.5" height="4.5" rx="1"/>
    <rect x="29" y="8.5" width="5.5" height="4.5" rx="1"/>
    <rect x="39" y="8.5" width="5.5" height="4.5" rx="1"/>
    <rect x="49" y="8.5" width="5.5" height="4.5" rx="1"/>
  </g>

  <!-- TOP FILM EDGE CODE / MARGIN PRINT (Authentic Kodak Font Feel) -->
  <text x="11" y="7.5" fill="#f5a623" font-family="'SF Mono', monospace" font-size="3" font-weight="bold" letter-spacing="0.4" opacity="0.85">KODAK 400</text>
  <text x="41" y="7.5" fill="#f5a623" font-family="'SF Mono', monospace" font-size="3" font-weight="bold" opacity="0.85">12A ▸</text>

  <!-- INNER EXPOSURE GATE (THE FRAME) -->
  <!-- Inner Border/Rebate around Photo -->
  <rect x="9.5" y="15.5" width="45" height="33" rx="3" fill="#050506" stroke="#221e1a" stroke-width="0.75"/>

  <!-- Default Photographic Landscape Scene inside Window (Replaced by User Photo if Available) -->
  <g clip-path="url(#filmGateClip)" id="galleryDefaultScene">
    <!-- Sunset Dusk Gradient -->
    <rect x="10" y="16" width="44" height="32" fill="url(#duskSkyGrad)"/>
    <!-- Distant Mountains -->
    <path d="M 10 38 L 18 29 L 26 35 L 36 24 L 46 36 L 54 30 L 54 48 L 10 48 Z" fill="#2d1c24"/>
    <path d="M 10 42 L 21 34 L 32 40 L 42 31 L 54 41 L 54 48 L 10 48 Z" fill="#181119"/>
    <!-- Golden Sunset Sun Disk -->
    <circle cx="37" cy="23" r="5" fill="#f5a623" opacity="0.9"/>
    <!-- Film Grain Texture Overlay -->
    <rect x="10" y="16" width="44" height="32" fill="#f5a623" opacity="0.08" style="mix-blend-mode: overlay;"/>
  </g>

  <!-- Image Anchor Element for Dynamic Thumbnail Injection -->
  <image id="svgInjectedThumbnail" x="10" y="16" width="44" height="32" preserveAspectRatio="xMidYMid slice" clip-path="url(#filmGateClip)" href="" style="display:none;"/>

  <!-- BOTTOM SPROCKET HOLE ROW -->
  <g fill="#070709" stroke="#322d26" stroke-width="0.5">
    <rect x="9" y="51" width="5.5" height="4.5" rx="1"/>
    <rect x="19" y="51" width="5.5" height="4.5" rx="1"/>
    <rect x="29" y="51" width="5.5" height="4.5" rx="1"/>
    <rect x="39" y="51" width="5.5" height="4.5" rx="1"/>
    <rect x="49" y="51" width="5.5" height="4.5" rx="1"/>
  </g>

  <!-- BOTTOM FILM EDGE CODE -->
  <text x="11" y="58" fill="#f5a623" font-family="'SF Mono', monospace" font-size="3" font-weight="bold" opacity="0.85">SAFETY FILM</text>
  <text x="43" y="58" fill="#f5a623" font-family="'SF Mono', monospace" font-size="3" font-weight="bold" opacity="0.85">▸ 13</text>

  <!-- 45-DEGREE HIGH-GLOSS EMULSION GLASS GLARE -->
  <path d="M 10 16 L 36 16 L 10 42 Z" fill="url(#glassGlareGrad)" clip-path="url(#filmGateClip)" pointer-events="none"/>
  <path d="M 28 16 L 54 16 L 54 26 L 18 48 L 10 48 L 10 45 Z" fill="url(#glassGlareGrad)" opacity="0.6" clip-path="url(#filmGateClip)" pointer-events="none"/>
</svg>""",

    # 2. Motion Photo (Live Emulsion Concentric Alloy Shutter)
    "shutter_motion_live": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
  <defs>
    <!-- Outer Bronze Alloy Rim Gradient -->
    <radialGradient id="bronzeRimGrad" cx="35%" cy="35%" r="65%">
      <stop offset="0%" stop-color="#ffe2a3"/>
      <stop offset="30%" stop-color="#f5a623"/>
      <stop offset="70%" stop-color="#a06008"/>
      <stop offset="100%" stop-color="#4a2e03"/>
    </radialGradient>

    <!-- Inner Stepped Chamber Gradient -->
    <radialGradient id="motionChamberGrad" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#1f180d"/>
      <stop offset="75%" stop-color="#120e07"/>
      <stop offset="100%" stop-color="#080603"/>
    </radialGradient>

    <!-- Glowing Live Emulsion Core -->
    <radialGradient id="liveCoreGlow" cx="40%" cy="40%" r="60%">
      <stop offset="0%" stop-color="#fff8eb"/>
      <stop offset="25%" stop-color="#ffcc66"/>
      <stop offset="65%" stop-color="#f5a623"/>
      <stop offset="100%" stop-color="#c97705"/>
    </radialGradient>
  </defs>

  <!-- Deep Drop Shadow -->
  <circle cx="32" cy="34" r="28" fill="#000000" opacity="0.7" filter="blur(2px)"/>

  <!-- Outer Bronze Alloy Bezel Ring -->
  <circle cx="32" cy="32" r="28" fill="url(#bronzeRimGrad)"/>
  <circle cx="32" cy="32" r="26.5" fill="none" stroke="#2a1a03" stroke-width="0.75" opacity="0.6"/>

  <!-- Radial Knurling Teeth / Notches (36 teeth around perimeter) -->
  <g stroke="#3a2202" stroke-width="0.9" opacity="0.7">
    <line x1="32" y1="4.5" x2="32" y2="6.8"/>
    <line x1="32" y1="57.2" x2="32" y2="59.5"/>
    <line x1="4.5" y1="32" x2="6.8" y2="32"/>
    <line x1="57.2" y1="32" x2="59.5" y2="32"/>
    <line x1="12.5" y1="12.5" x2="14.2" y2="14.2"/>
    <line x1="49.8" y1="49.8" x2="51.5" y2="51.5"/>
    <line x1="12.5" y1="51.5" x2="14.2" y2="49.8"/>
    <line x1="49.8" y1="14.2" x2="51.5" y2="12.5"/>
  </g>

  <!-- Middle Recessed Dark Anodized Chamber -->
  <circle cx="32" cy="32" r="23" fill="url(#motionChamberGrad)" stroke="#f5a623" stroke-width="1.2" stroke-opacity="0.7"/>

  <!-- Concentric Live Sound/Temporal Motion Wave Rings -->
  <circle cx="32" cy="32" r="18" fill="none" stroke="#f5a623" stroke-width="0.8" stroke-dasharray="2 2" opacity="0.45"/>
  <circle cx="32" cy="32" r="13.5" fill="none" stroke="#f5a623" stroke-width="1.0" opacity="0.7"/>

  <!-- Central Live Emulsion Core Button Piston -->
  <circle cx="32" cy="32" r="8.5" fill="url(#liveCoreGlow)" stroke="#ffe4b0" stroke-width="0.8"/>
  <circle cx="30" cy="30" r="2.5" fill="#ffffff" opacity="0.75"/>

  <!-- "LIVE" Micro Laser Engraving underneath core -->
  <text x="32" y="47.5" fill="#f5a623" font-family="'SF Mono', monospace" font-size="5" font-weight="900" letter-spacing="1.2" text-anchor="middle" opacity="0.95">LIVE</text>
</svg>""",

    # 3. Decisive Frame Still Shutter (Precision Stainless Mechanical Shutter Assembly)
    "shutter_decisive_mechanical": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80" width="80" height="80">
  <defs>
    <!-- Outer Cold Steel Chassis Knurl Gradient -->
    <radialGradient id="outerSteelKnurl" cx="30%" cy="30%" r="70%">
      <stop offset="0%" stop-color="#4a4a52"/>
      <stop offset="35%" stop-color="#2c2c34"/>
      <stop offset="70%" stop-color="#18181e"/>
      <stop offset="100%" stop-color="#0a0a0d"/>
    </radialGradient>

    <!-- Chrome Bezel Mirror Reflection Ring -->
    <linearGradient id="chromeRing" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#ffffff"/>
      <stop offset="25%" stop-color="#a8a8b2"/>
      <stop offset="45%" stop-color="#555560"/>
      <stop offset="55%" stop-color="#e8e8ed"/>
      <stop offset="75%" stop-color="#7a7a85"/>
      <stop offset="100%" stop-color="#ffffff"/>
    </linearGradient>

    <!-- Center Raised Shutter Button Cap (Convex Brushed Silver Disc) -->
    <radialGradient id="buttonCapGrad" cx="38%" cy="35%" r="65%">
      <stop offset="0%" stop-color="#ffffff"/>
      <stop offset="40%" stop-color="#e2e2e8"/>
      <stop offset="75%" stop-color="#a0a0aa"/>
      <stop offset="100%" stop-color="#60606a"/>
    </radialGradient>

    <!-- Cable Release Threaded Socket (Deep Dark Pit with Brass Thread) -->
    <radialGradient id="cableSocketPit" cx="45%" cy="45%" r="55%">
      <stop offset="0%" stop-color="#141416"/>
      <stop offset="70%" stop-color="#08080a"/>
      <stop offset="100%" stop-color="#000000"/>
    </radialGradient>
  </defs>

  <!-- Heavy Drop Shadow under Shutter Assembly -->
  <circle cx="40" cy="43" r="37" fill="#000000" opacity="0.8" filter="blur(3px)"/>

  <!-- Layer 1: Outer Aircraft Alloy Base Frame -->
  <circle cx="40" cy="40" r="38" fill="url(#outerSteelKnurl)"/>
  <circle cx="40" cy="40" r="37" fill="none" stroke="#5a5a66" stroke-width="0.8"/>

  <!-- Knurling Grooves / Grip Serrations on Outer Bezel -->
  <g stroke="#1a1a22" stroke-width="1.0" opacity="0.8">
    <line x1="40" y1="2" x2="40" y2="6"/>
    <line x1="40" y1="74" x2="40" y2="78"/>
    <line x1="2" y1="40" x2="6" y2="40"/>
    <line x1="74" y1="40" x2="78" y2="40"/>
    <line x1="13.1" y1="13.1" x2="16.0" y2="16.0"/>
    <line x1="64.0" y1="64.0" x2="66.9" y2="66.9"/>
    <line x1="13.1" y1="66.9" x2="16.0" y2="64.0"/>
    <line x1="64.0" y1="16.0" x2="66.9" y2="13.1"/>
    <!-- Additional Micro Ticks -->
    <line x1="25.5" y1="5.1" x2="26.7" y2="8.8"/>
    <line x1="54.5" y1="5.1" x2="53.3" y2="8.8"/>
    <line x1="25.5" y1="74.9" x2="26.7" y2="71.2"/>
    <line x1="54.5" y1="74.9" x2="53.3" y2="71.2"/>
    <line x1="5.1" y1="25.5" x2="8.8" y2="26.7"/>
    <line x1="5.1" y1="54.5" x2="8.8" y2="53.3"/>
    <line x1="74.9" y1="25.5" x2="71.2" y2="26.7"/>
    <line x1="74.9" y1="54.5" x2="71.2" y2="53.3"/>
  </g>

  <!-- Layer 2: High-Mirror Precision Chrome Step Collar -->
  <circle cx="40" cy="40" r="32" fill="url(#chromeRing)"/>
  <circle cx="40" cy="40" r="30.5" fill="#1e1e24"/>

  <!-- Layer 3: Piston Travel Ring (Recessed Chamber) -->
  <circle cx="40" cy="40" r="28.5" fill="#0c0c0e" stroke="#000000" stroke-width="1.5"/>

  <!-- Layer 4: The Movable Button Piston Cap (Convex Dome) -->
  <circle cx="40" cy="40" r="25" fill="url(#buttonCapGrad)" stroke="#ffffff" stroke-width="0.8"/>
  <circle cx="40" cy="40" r="21.5" fill="none" stroke="#888894" stroke-width="0.5" opacity="0.6"/>

  <!-- Center Shutter Release Cable Threaded Socket Hole -->
  <circle cx="40" cy="40" r="8" fill="url(#cableSocketPit)" stroke="#383840" stroke-width="1"/>
  
  <!-- Internal Brass Screw Threads in Socket Hole -->
  <circle cx="40" cy="40" r="6" fill="none" stroke="#f5a623" stroke-width="0.6" stroke-dasharray="2 1.5" opacity="0.75"/>
  <circle cx="40" cy="40" r="4" fill="none" stroke="#d48810" stroke-width="0.5" stroke-dasharray="1.5 1.5" opacity="0.6"/>
  <circle cx="40" cy="40" r="2" fill="#000000"/>

  <!-- Mechanical Cable Socket Slit Cross (Screwdriver / Pin Guide) -->
  <line x1="33" y1="40" x2="47" y2="40" stroke="#222226" stroke-width="1.0"/>
  <line x1="40" y1="33" x2="40" y2="47" stroke="#222226" stroke-width="1.0"/>
</svg>""",

    # 4. Cinema Video Shutter (Darkroom Red Fresnel Lens Tally Button)
    "shutter_cinema_tally": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
  <defs>
    <!-- Heavy Anodized Steel Protective Guard Ring -->
    <radialGradient id="tallyGuardGrad" cx="30%" cy="30%" r="70%">
      <stop offset="0%" stop-color="#3c3838"/>
      <stop offset="50%" stop-color="#241e1e"/>
      <stop offset="100%" stop-color="#120c0c"/>
    </radialGradient>

    <!-- Fresnel Ruby Red Convex Lens Gradient (Standby State) -->
    <radialGradient id="rubyLensGrad" cx="40%" cy="35%" r="65%">
      <stop offset="0%" stop-color="#ff6b6b"/>
      <stop offset="30%" stop-color="#e03e3e"/>
      <stop offset="65%" stop-color="#941818"/>
      <stop offset="90%" stop-color="#4d0808"/>
      <stop offset="100%" stop-color="#260202"/>
    </radialGradient>

    <!-- Fresnel Active Firing Tally Glow Gradient -->
    <radialGradient id="tallyActiveGlow" cx="45%" cy="40%" r="60%">
      <stop offset="0%" stop-color="#ffffff"/>
      <stop offset="25%" stop-color="#ff8585"/>
      <stop offset="60%" stop-color="#e03e3e"/>
      <stop offset="100%" stop-color="#800a0a"/>
    </radialGradient>
  </defs>

  <!-- Drop Shadow -->
  <circle cx="32" cy="34" r="28" fill="#000000" opacity="0.7" filter="blur(2px)"/>

  <!-- Outer Heavy Industrial Guard Bezel -->
  <circle cx="32" cy="32" r="28" fill="url(#tallyGuardGrad)"/>
  <circle cx="32" cy="32" r="26.5" fill="none" stroke="#e03e3e" stroke-width="0.8" opacity="0.5"/>

  <!-- Four Recessed Hex Machine Screws on Bezel (12, 3, 6, 9 o'clock) -->
  <circle cx="32" cy="7" r="1.5" fill="#443c3c" stroke="#110a0a" stroke-width="0.5"/>
  <circle cx="32" cy="57" r="1.5" fill="#443c3c" stroke="#110a0a" stroke-width="0.5"/>
  <circle cx="7" cy="32" r="1.5" fill="#443c3c" stroke="#110a0a" stroke-width="0.5"/>
  <circle cx="57" cy="32" r="1.5" fill="#443c3c" stroke="#110a0a" stroke-width="0.5"/>

  <!-- Intermediate Deep Trench (prevents accidental trigger) -->
  <circle cx="32" cy="32" r="22.5" fill="#0e0404" stroke="#000000" stroke-width="1.2"/>

  <!-- The Darkroom Red Fresnel Lamp Core -->
  <circle id="tallyCoreElement" cx="32" cy="32" r="18.5" fill="url(#rubyLensGrad)" stroke="#ff8585" stroke-width="0.8"/>

  <!-- Fresnel Lens Circular Prismatic Grooves -->
  <circle cx="32" cy="32" r="14.5" fill="none" stroke="#ff9999" stroke-width="0.6" opacity="0.45"/>
  <circle cx="32" cy="32" r="10.5" fill="none" stroke="#ffb3b3" stroke-width="0.6" opacity="0.55"/>
  <circle cx="32" cy="32" r="6.5" fill="none" stroke="#ffffff" stroke-width="0.7" opacity="0.7"/>

  <!-- Center Specular Highlight Pinhole -->
  <circle cx="28" cy="27" r="2.8" fill="#ffffff" opacity="0.85" filter="blur(0.5px)"/>

  <!-- Cinema REC Symbol (Center Dot that morphs or indicates recording) -->
  <circle id="tallyRecIndicator" cx="32" cy="32" r="3.2" fill="#ffffff" opacity="0.9"/>
</svg>""",

    # 5. Bayonet PL Cinema Lens Mount Flip Button
    "shutter_lens_mount_flip": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
  <defs>
    <!-- Outer Stainless Steel PL Flange Ring -->
    <radialGradient id="plFlangeGrad" cx="30%" cy="30%" r="70%">
      <stop offset="0%" stop-color="#484850"/>
      <stop offset="40%" stop-color="#2a2a30"/>
      <stop offset="80%" stop-color="#16161a"/>
      <stop offset="100%" stop-color="#0a0a0c"/>
    </radialGradient>

    <!-- Chrome Locking Lug Gradient -->
    <linearGradient id="chromeLugGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#ffffff"/>
      <stop offset="50%" stop-color="#9a9aa4"/>
      <stop offset="100%" stop-color="#4a4a54"/>
    </linearGradient>

    <!-- Multi-Coated Optical Glass Lens Element (Magenta/Cyan Anti-Reflective Coating) -->
    <radialGradient id="coatedOpticsGrad" cx="40%" cy="35%" r="65%">
      <stop offset="0%" stop-color="#1c1228"/>
      <stop offset="45%" stop-color="#0e1824"/>
      <stop offset="75%" stop-color="#060e18"/>
      <stop offset="100%" stop-color="#020408"/>
    </radialGradient>

    <!-- Magenta/Green Reflection Arc -->
    <linearGradient id="arCoatingSheen" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#e040fb" stop-opacity="0.6"/>
      <stop offset="50%" stop-color="#00e5ff" stop-opacity="0.4"/>
      <stop offset="100%" stop-color="#00e676" stop-opacity="0.1"/>
    </linearGradient>
  </defs>

  <!-- Drop Shadow -->
  <circle cx="32" cy="34" r="28" fill="#000000" opacity="0.7" filter="blur(2px)"/>

  <!-- Outer PL Mount Base Ring -->
  <circle cx="32" cy="32" r="28" fill="url(#plFlangeGrad)" stroke="#5e5e6c" stroke-width="0.8"/>

  <!-- Rotating Bayonet Assembly Group (Targeted by CSS / JS for 180-deg Snap) -->
  <g id="plRotatingAssembly" style="transform-origin: 32px 32px; transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);">
    
    <!-- Three Locking Lugs (120-degree Cine PL Mount Standard) -->
    <!-- Top Lug (0 deg) -->
    <path d="M 27 3.5 H 37 L 35.5 8.5 H 28.5 Z" fill="url(#chromeLugGrad)" stroke="#222228" stroke-width="0.6"/>
    <!-- Bottom Right Lug (120 deg) -->
    <path d="M 54.5 40 L 49.5 49 L 45.5 45.5 L 49.5 38 Z" fill="url(#chromeLugGrad)" stroke="#222228" stroke-width="0.6"/>
    <!-- Bottom Left Lug (240 deg) -->
    <path d="M 9.5 40 L 14.5 49 L 18.5 45.5 L 14.5 38 Z" fill="url(#chromeLugGrad)" stroke="#222228" stroke-width="0.6"/>

    <!-- Inner PL Stainless Flange Ring with Precision Screws -->
    <circle cx="32" cy="32" r="22.5" fill="#1c1c22" stroke="#888896" stroke-width="1.2"/>
    
    <!-- Four Lens Mount Torx Screws -->
    <circle cx="21" cy="21" r="1.3" fill="#666672" stroke="#111116" stroke-width="0.4"/>
    <circle cx="43" cy="21" r="1.3" fill="#666672" stroke="#111116" stroke-width="0.4"/>
    <circle cx="21" cy="43" r="1.3" fill="#666672" stroke="#111116" stroke-width="0.4"/>
    <circle cx="43" cy="43" r="1.3" fill="#666672" stroke="#111116" stroke-width="0.4"/>

    <!-- Dual Curved Bidirectional Rotation Guide Arrows -->
    <path d="M 22 15 A 16 16 0 0 1 42 15 M 39 12 L 43 15 L 39 18" fill="none" stroke="#f5a623" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" opacity="0.9"/>
    <path d="M 42 49 A 16 16 0 0 1 22 49 M 25 52 L 21 49 L 25 46" fill="none" stroke="#f5a623" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" opacity="0.9"/>

    <!-- Center Multi-Coated Rear Lens Element (Glass Core) -->
    <circle cx="32" cy="32" r="12.5" fill="url(#coatedOpticsGrad)" stroke="#3a2846" stroke-width="0.8"/>
    
    <!-- AR Coating Curved Reflection Flare (Magenta / Cyan) -->
    <path d="M 23 28 A 10 10 0 0 1 36 22" fill="none" stroke="url(#arCoatingSheen)" stroke-width="2.2" stroke-linecap="round" opacity="0.85"/>
    <circle cx="27" cy="26" r="1.5" fill="#ffffff" opacity="0.65"/>
  </g>

  <!-- Static FLIP / Focal Length Micro Typography Tag -->
  <text x="32" y="34.5" fill="#ffffff" font-family="'SF Mono', monospace" font-size="5" font-weight="900" letter-spacing="0.5" text-anchor="middle" opacity="0.9" style="text-shadow: 0 1px 2px #000;">1x</text>
</svg>"""
}

def main():
    dirs = ["preview/icons", "app/src/main/assets/icons"]
    for d in dirs:
        os.makedirs(d, exist_ok=True)

    for name, svg in REALISTIC_SVGS.items():
        svg_content = svg.strip() + "\n"
        for d in dirs:
            p = os.path.join(d, f"{name}.svg")
            with open(p, "w", encoding="utf-8") as f:
                f.write(svg_content)

    print(f"Successfully generated {len(REALISTIC_SVGS)} hyper-realistic skeuomorphic cinema shutter SVGs!")

if __name__ == "__main__":
    main()
