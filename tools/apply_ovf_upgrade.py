#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Apply Optical Viewfinder (OVF) Gallery Button upgrade to:
- preview/index.html
- preview/icons_showcase.html
"""

OVF_CSS = """    /* ==========================================================================
       1. SKEUOMORPHIC OPTICAL VIEWFINDER (OVF) GALLERY BUTTON (NO BULKY BORDERS)
       ========================================================================== */
    .gallery-ovf-btn {
      width: 52px;
      height: 52px;
      cursor: pointer;
      position: relative;
      user-select: none;
      flex-shrink: 0;
      transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .gallery-ovf-btn:hover {
      transform: translateY(-2px) scale(1.06);
    }
    .gallery-ovf-btn:active {
      transform: translateY(2px) scale(0.92);
    }
    .ovf-eyepiece-chassis {
      width: 100%;
      height: 100%;
      border-radius: 9px;
      background: linear-gradient(145deg, #32302d 0%, #1c1b19 45%, #0e0d0c 100%);
      border: 1px solid #4a4640;
      box-shadow: 0 8px 18px rgba(0, 0, 0, 0.8),
                  inset 0 1px 1.5px rgba(255, 255, 255, 0.2),
                  inset 0 -1px 2px rgba(0, 0, 0, 0.8);
      padding: 2px; /* Ultra-thin precision bezel: zero bulky borders, pure glass */
      position: relative;
      overflow: hidden;
      display: flex;
    }
    .gallery-ovf-btn:hover .ovf-eyepiece-chassis {
      border-color: var(--color-amber-grain);
      box-shadow: 0 10px 22px rgba(0, 0, 0, 0.9), 0 0 14px rgba(245, 166, 35, 0.28);
    }
    .ovf-glass-window {
      flex: 1;
      border-radius: 7px;
      position: relative;
      overflow: hidden;
      background: #060608;
      box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.95);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    /* Fallback Scenic Landscape inside OVF when no photo taken */
    .ovf-fallback-scenic {
      width: 100%;
      height: 100%;
      position: absolute;
      top: 0;
      left: 0;
      background: linear-gradient(180deg, #0e1220 0%, #261c33 30%, #682635 60%, #b85222 85%, #f59e0b 100%);
    }
    .ovf-fallback-scenic .ovf-scenic-sun {
      position: absolute;
      top: 13px;
      left: 50%;
      transform: translateX(-50%);
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: #ffea9f;
      box-shadow: 0 0 8px #ffb300, 0 0 16px rgba(255, 179, 0, 0.4);
    }
    .ovf-fallback-scenic .ovf-scenic-peaks {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 22px;
      background: #140d18;
      clip-path: polygon(0% 100%, 0% 55%, 22% 20%, 48% 65%, 72% 25%, 100% 50%, 100% 100%);
    }
    /* Real captured photo inside OVF */
    .ovf-captured-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      position: absolute;
      top: 0;
      left: 0;
      z-index: 2;
      transition: opacity 0.3s ease;
    }
    /* Multi-Coating Anti-Reflective Optical Sheen (AR Coating) */
    .ovf-ar-coating {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 3;
      background: linear-gradient(135deg, rgba(168, 85, 247, 0.28) 0%, rgba(6, 182, 212, 0.12) 40%, rgba(245, 158, 11, 0.1) 70%, transparent 100%);
      mix-blend-mode: screen;
      pointer-events: none;
    }
    /* 45-degree Optical Glass Glare */
    .ovf-specular-glare {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 4;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.38) 0%, rgba(255, 255, 255, 0.08) 28%, transparent 45%, rgba(255, 255, 255, 0.14) 100%);
      pointer-events: none;
    }
    /* Optical Brightlines (Leica M / Cinema OVF Etched Framelines) */
    .ovf-frameline {
      position: absolute;
      z-index: 5;
      pointer-events: none;
      filter: drop-shadow(0 0 1px rgba(0, 0, 0, 0.8));
    }
    .ovf-frameline-tl {
      top: 6px;
      left: 6px;
      width: 7px;
      height: 7px;
      border-top: 1.2px solid rgba(255, 255, 255, 0.88);
      border-left: 1.2px solid rgba(255, 255, 255, 0.88);
    }
    .ovf-frameline-tr {
      top: 6px;
      right: 6px;
      width: 7px;
      height: 7px;
      border-top: 1.2px solid rgba(255, 255, 255, 0.88);
      border-right: 1.2px solid rgba(255, 255, 255, 0.88);
    }
    .ovf-frameline-bl {
      bottom: 6px;
      left: 6px;
      width: 7px;
      height: 7px;
      border-bottom: 1.2px solid rgba(255, 255, 255, 0.88);
      border-left: 1.2px solid rgba(255, 255, 255, 0.88);
    }
    .ovf-frameline-br {
      bottom: 6px;
      right: 6px;
      width: 7px;
      height: 7px;
      border-bottom: 1.2px solid rgba(255, 255, 255, 0.88);
      border-right: 1.2px solid rgba(255, 255, 255, 0.88);
    }
    /* Rangefinder Center Reticle */
    .ovf-center-reticle {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 6px;
      height: 6px;
      z-index: 5;
      pointer-events: none;
      opacity: 0.75;
    }
    .ovf-center-reticle::before,
    .ovf-center-reticle::after {
      content: '';
      position: absolute;
      background: rgba(255, 255, 255, 0.88);
    }
    .ovf-center-reticle::before {
      top: 2.5px;
      left: 0;
      width: 6px;
      height: 1px;
    }
    .ovf-center-reticle::after {
      top: 0;
      left: 2.5px;
      width: 1px;
      height: 6px;
    }
    /* Micro Optical HUD Overlay */
    .ovf-hud-tag {
      position: absolute;
      top: 3px;
      left: 5px;
      z-index: 6;
      font-family: var(--font-mono);
      font-size: 5.5px;
      font-weight: 800;
      color: var(--color-amber-grain);
      letter-spacing: 0.4px;
      text-shadow: 0 1px 2px #000;
      pointer-events: none;
    }
    .ovf-ready-led {
      position: absolute;
      top: 3.5px;
      right: 5px;
      width: 3.5px;
      height: 3.5px;
      border-radius: 50%;
      background: #10b981;
      box-shadow: 0 0 4px #10b981;
      z-index: 6;
      pointer-events: none;
    }
    .ovf-frame-stamp {
      position: absolute;
      bottom: 2.5px;
      right: 5px;
      z-index: 6;
      font-family: var(--font-mono);
      font-size: 5.5px;
      font-weight: 800;
      color: #fff;
      text-shadow: 0 1px 2px #000;
      opacity: 0.9;
      pointer-events: none;
    }
"""

OVF_HTML_INDEX = """          <!-- 1. Skeuomorphic Optical Viewfinder (OVF) Gallery Button (Clean Bezel, No Bulky Borders) -->
          <div class="gallery-ovf-btn" id="btnGallery" title="查看最近底片 (光学取景器 OVF)">
            <div class="ovf-eyepiece-chassis">
              <div class="ovf-glass-window">
                <!-- Fallback Scenic View if no photo taken yet -->
                <div class="ovf-fallback-scenic" id="galleryFallbackGraphic">
                  <div class="ovf-scenic-sun"></div>
                  <div class="ovf-scenic-peaks"></div>
                </div>
                <!-- Real Injected Captured Photo -->
                <img src="" class="ovf-captured-img" id="galleryThumbnailImg" style="display:none;" alt="Latest Film" />
                <!-- Multi-Coating AR Optical Sheen -->
                <div class="ovf-ar-coating"></div>
                <!-- 45-degree Specular Glass Glare -->
                <div class="ovf-specular-glare"></div>
                <!-- Minimalist Optical Brightlines (Leica / Cinema Framelines) -->
                <div class="ovf-frameline ovf-frameline-tl"></div>
                <div class="ovf-frameline ovf-frameline-tr"></div>
                <div class="ovf-frameline ovf-frameline-bl"></div>
                <div class="ovf-frameline ovf-frameline-br"></div>
                <div class="ovf-center-reticle"></div>
                <!-- Micro Optical HUD -->
                <span class="ovf-hud-tag">OVF 35</span>
                <span class="ovf-ready-led"></span>
                <span class="ovf-frame-stamp" id="galleryFrameStamp">12A ▸</span>
              </div>
            </div>
          </div>"""

OVF_HTML_SHOWCASE = """          <!-- 1. Optical Viewfinder (OVF) Gallery Button -->
          <div style="display:flex; flex-direction:column; align-items:center; gap:12px;">
            <div class="gallery-ovf-btn" id="demoBtnGallery" style="width: 76px; height: 76px;" title="点击查看光学取景器底片">
              <div class="ovf-eyepiece-chassis" style="border-radius: 12px; padding: 2.5px;">
                <div class="ovf-glass-window" style="border-radius: 10px;">
                  <div class="ovf-fallback-scenic">
                    <div class="ovf-scenic-sun" style="width: 16px; height: 16px; top: 18px;"></div>
                    <div class="ovf-scenic-peaks" style="height: 30px;"></div>
                  </div>
                  <div class="ovf-ar-coating"></div>
                  <div class="ovf-specular-glare"></div>
                  <div class="ovf-frameline ovf-frameline-tl" style="width: 10px; height: 10px; top: 9px; left: 9px; border-width: 1.5px;"></div>
                  <div class="ovf-frameline ovf-frameline-tr" style="width: 10px; height: 10px; top: 9px; right: 9px; border-width: 1.5px;"></div>
                  <div class="ovf-frameline ovf-frameline-bl" style="width: 10px; height: 10px; bottom: 9px; left: 9px; border-width: 1.5px;"></div>
                  <div class="ovf-frameline ovf-frameline-br" style="width: 10px; height: 10px; bottom: 9px; right: 9px; border-width: 1.5px;"></div>
                  <div class="ovf-center-reticle" style="width: 10px; height: 10px;"></div>
                  <span class="ovf-hud-tag" style="font-size: 7.5px; top: 4px; left: 7px;">OVF 35mm</span>
                  <span class="ovf-ready-led" style="width: 5px; height: 5px; top: 5px; right: 7px;"></span>
                  <span class="ovf-frame-stamp" id="demoFrameStamp" style="font-size: 7.5px; bottom: 4px; right: 7px;">12A ▸</span>
                </div>
              </div>
            </div>
            <span style="font-family:var(--font-mono); font-size:11.5px; font-weight:700; color:var(--color-amber-grain);">光学取景器视窗 (OVF)</span>
          </div>"""

def update_index_html():
    file_path = "preview/index.html"
    with open(file_path, "r", encoding="utf-8") as f:
        c = f.read()

    # 1. Update CSS
    idx1 = c.find("/* 1. SKEUOMORPHIC 35MM SLIDE NEGATIVE MOUNT GALLERY BUTTON */")
    idx2 = c.find("/* 2. SKEUOMORPHIC MOTION PHOTO SHUTTER")
    if idx1 != -1 and idx2 != -1:
        c = c[:idx1] + OVF_CSS.strip() + "\n\n    " + c[idx2:]
        print("Updated CSS in preview/index.html")

    # 2. Update HTML
    h1 = c.find("<!-- 1. Skeuomorphic 35mm Slide Negative Mount Gallery Button -->")
    h2 = c.find("<!-- 2. Skeuomorphic Motion Photo Shutter")
    if h1 != -1 and h2 != -1:
        c = c[:h1] + OVF_HTML_INDEX.strip() + "\n\n          " + c[h2:]
        print("Updated HTML in preview/index.html")

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(c)

def update_showcase_html():
    file_path = "preview/icons_showcase.html"
    with open(file_path, "r", encoding="utf-8") as f:
        c = f.read()

    # 1. Update CSS
    idx1 = c.find(".gallery-slide-btn {")
    idx2 = c.find(".motion-shutter-assembly {")
    if idx1 != -1 and idx2 != -1:
        c = c[:idx1] + OVF_CSS.strip() + "\n\n    " + c[idx2:]
        print("Updated CSS in preview/icons_showcase.html")

    # 2. Update HTML
    h1 = c.find("<!-- 1. 35mm Slide Negative Mount Gallery Button -->")
    h2 = c.find("<!-- 2. Motion Photo Shutter -->")
    if h1 != -1 and h2 != -1:
        c = c[:h1] + OVF_HTML_SHOWCASE.strip() + "\n\n          " + c[h2:]
        print("Updated HTML in preview/icons_showcase.html")

    # 3. Update JS Toast
    if "35mm 幻灯片胶卷夹已取出" in c:
        c = c.replace("35mm 幻灯片胶卷夹已取出", "光学取景器视窗 (OVF): 开启暗房底片库")

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(c)

def main():
    update_index_html()
    update_showcase_html()
    print("Optical Viewfinder upgrade applied successfully!")

if __name__ == "__main__":
    main()
