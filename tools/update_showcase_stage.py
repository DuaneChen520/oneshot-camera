#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import re

CSS_TO_ADD = """
    /* ==========================================================================
       HYPER-REALISTIC SKEUOMORPHIC CONTROLS SHOWCASE STYLES
       ========================================================================== */
    .gallery-slide-btn {
      width: 52px;
      height: 52px;
      cursor: pointer;
      position: relative;
      perspective: 400px;
      user-select: none;
      flex-shrink: 0;
    }
    .slide-mount-frame {
      width: 100%;
      height: 100%;
      background: linear-gradient(145deg, #2c2824 0%, #171512 60%, #0d0c0a 100%);
      border: 1px solid #4a443c;
      border-radius: 7px;
      box-shadow: 0 6px 14px rgba(0, 0, 0, 0.75), inset 0 1px 1.5px rgba(255, 255, 255, 0.15);
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding: 3px;
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;
      overflow: hidden;
    }
    .gallery-slide-btn:hover .slide-mount-frame {
      transform: translateY(-2px) scale(1.05);
      border-color: var(--color-amber-grain);
      box-shadow: 0 10px 20px rgba(0, 0, 0, 0.9), 0 0 12px rgba(245, 166, 35, 0.25);
    }
    .gallery-slide-btn:active .slide-mount-frame {
      transform: translateY(2px) scale(0.92);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.9);
    }
    .slide-sprocket-strip {
      height: 8px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 2px;
    }
    .slide-sprocket-strip .sprocket-hole {
      width: 5px;
      height: 4px;
      background: #060608;
      border: 0.5px solid #2e2820;
      border-radius: 1px;
      box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.9);
    }
    .sprocket-kodak-code {
      font-family: var(--font-mono);
      font-size: 5px;
      font-weight: 800;
      color: var(--color-amber-grain);
      letter-spacing: 0.2px;
      opacity: 0.9;
    }
    .slide-emulsion-window {
      flex: 1;
      margin: 2px 0;
      border-radius: 3px;
      position: relative;
      overflow: hidden;
      background: #000;
      border: 0.75px solid #363028;
      box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.9);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .slide-fallback-graphic {
      width: 100%;
      height: 100%;
      position: absolute;
      top: 0;
      left: 0;
      background: linear-gradient(180deg, #ff8c42 0%, #a03c58 55%, #2a162d 100%);
    }
    .slide-fallback-graphic .fallback-sun {
      position: absolute;
      top: 4px;
      left: 6px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #ffec99;
      box-shadow: 0 0 6px #ffbe3b;
    }
    .slide-fallback-graphic .fallback-peaks {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 12px;
      background: #140b17;
      clip-path: polygon(0% 100%, 0% 60%, 25% 20%, 55% 70%, 75% 30%, 100% 60%, 100% 100%);
    }
    .slide-specular-glare {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 3;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.45) 0%, rgba(255, 255, 255, 0.08) 35%, transparent 50%, rgba(255, 255, 255, 0.15) 100%);
      pointer-events: none;
    }
    .slide-frame-stamp {
      position: absolute;
      bottom: 1px;
      right: 2px;
      z-index: 4;
      font-family: var(--font-mono);
      font-size: 5px;
      font-weight: 900;
      color: #fff;
      text-shadow: 0 1px 2px #000;
      opacity: 0.85;
    }

    .motion-shutter-assembly {
      width: 54px;
      height: 54px;
      cursor: pointer;
      position: relative;
      flex-shrink: 0;
      user-select: none;
    }
    .motion-knurled-bezel {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: radial-gradient(circle at 35% 35%, #ffd27d 0%, #f5a623 35%, #9e5b02 75%, #422602 100%);
      padding: 3px;
      box-shadow: 0 8px 18px rgba(0, 0, 0, 0.8), inset 0 1px 2px rgba(255, 255, 255, 0.4);
      transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;
    }
    .motion-shutter-assembly:hover .motion-knurled-bezel {
      transform: scale(1.05);
      box-shadow: 0 10px 24px rgba(0, 0, 0, 0.9), 0 0 15px rgba(245, 166, 35, 0.35);
    }
    .motion-shutter-assembly:active .motion-knurled-bezel {
      transform: scale(0.91);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.9);
    }
    .motion-stepped-chamber {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: radial-gradient(circle, #241c0e 0%, #140f06 70%, #080603 100%);
      border: 1.2px solid rgba(245, 166, 35, 0.7);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      position: relative;
      box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.9);
    }
    .motion-live-core {
      width: 14px;
      height: 14px;
      border-radius: 50%;
      background: radial-gradient(circle at 35% 35%, #ffffff 0%, #ffcc66 35%, #f5a623 70%, #b86700 100%);
      box-shadow: 0 0 10px rgba(245, 166, 35, 0.8), inset 0 1px 2px rgba(255, 255, 255, 0.8);
      position: relative;
      transition: transform 0.15s ease;
    }
    .motion-acoustic-wave {
      position: absolute;
      border-radius: 50%;
      border: 1px dashed rgba(245, 166, 35, 0.35);
      pointer-events: none;
    }
    .motion-sub-tag {
      font-family: var(--font-mono);
      font-size: 6.5px;
      font-weight: 900;
      color: var(--color-amber-grain);
      letter-spacing: 0.8px;
      margin-top: 2px;
      text-shadow: 0 1px 2px #000;
    }

    .decisive-shutter-assembly {
      width: 76px;
      height: 76px;
      position: relative;
      cursor: pointer;
      user-select: none;
      flex-shrink: 0;
    }
    .decisive-knurled-outer-collar {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: radial-gradient(circle at 35% 35%, #464650 0%, #2a2a32 45%, #141418 80%, #0a0a0c 100%);
      border: 1.5px solid #5e5e6e;
      padding: 4px;
      box-shadow: 0 12px 28px rgba(0, 0, 0, 0.9), inset 0 1.5px 2px rgba(255, 255, 255, 0.3);
      transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .decisive-shutter-assembly:hover .decisive-knurled-outer-collar {
      transform: scale(1.04);
      border-color: #8c8c9e;
      box-shadow: 0 14px 32px rgba(0, 0, 0, 0.95), 0 0 16px rgba(255, 255, 255, 0.12);
    }
    .decisive-shutter-assembly:active .decisive-knurled-outer-collar {
      transform: translateY(3px) scale(0.92);
      box-shadow: 0 3px 8px rgba(0, 0, 0, 0.9);
    }
    .decisive-mirror-chrome-ring {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: linear-gradient(135deg, #ffffff 0%, #9e9ea8 25%, #44444e 50%, #e2e2e8 75%, #767682 100%);
      padding: 2.5px;
      box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.8), 0 2px 5px rgba(0, 0, 0, 0.6);
    }
    .decisive-recessed-chamber {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: #0a0a0c;
      padding: 3px;
      box-shadow: inset 0 4px 8px rgba(0, 0, 0, 0.95);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .decisive-convex-piston {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: radial-gradient(circle at 35% 35%, #ffffff 0%, #e0e0e6 40%, #9e9ea8 75%, #585864 100%);
      border: 1px solid #ffffff;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.7), inset 0 2px 4px rgba(255, 255, 255, 0.9);
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
    }
    .decisive-cable-socket-pit {
      width: 15px;
      height: 15px;
      border-radius: 50%;
      background: radial-gradient(circle, #08080a 0%, #000000 85%, #2a2a30 100%);
      border: 1px solid #3c3c44;
      box-shadow: inset 0 3px 6px rgba(0, 0, 0, 0.95), 0 1px 1px rgba(255, 255, 255, 0.5);
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .cable-socket-inner-threads {
      width: 9px;
      height: 9px;
      border-radius: 50%;
      border: 0.75px dashed #f5a623;
      opacity: 0.8;
    }
    .cable-socket-guide-slit {
      position: absolute;
      width: 11px;
      height: 1px;
      background: #18181c;
    }

    .video-shutter-assembly {
      width: 54px;
      height: 54px;
      cursor: pointer;
      position: relative;
      flex-shrink: 0;
      user-select: none;
    }
    .video-protective-guard {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: radial-gradient(circle at 35% 35%, #3c3434 0%, #201818 55%, #100a0a 100%);
      border: 1.2px solid #582e2e;
      padding: 3px;
      box-shadow: 0 8px 18px rgba(0, 0, 0, 0.8), inset 0 1px 2px rgba(255, 255, 255, 0.2);
      transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .video-shutter-assembly:hover .video-protective-guard {
      transform: scale(1.05);
      border-color: var(--color-darkroom-red);
      box-shadow: 0 10px 24px rgba(0, 0, 0, 0.9), 0 0 14px rgba(224, 62, 62, 0.35);
    }
    .video-shutter-assembly:active .video-protective-guard {
      transform: scale(0.91);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.9);
    }
    .video-trench-moat {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: #080303;
      padding: 3px;
      box-shadow: inset 0 3px 6px rgba(0, 0, 0, 0.95);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .video-fresnel-lens-core {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: radial-gradient(circle at 40% 35%, #ff5252 0%, #e03e3e 30%, #991414 70%, #400404 100%);
      border: 1px solid #ff7a7a;
      box-shadow: 0 0 10px rgba(224, 62, 62, 0.65), inset 0 2px 4px rgba(255, 255, 255, 0.6);
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .fresnel-prism-ring {
      position: absolute;
      border-radius: 50%;
      border: 0.6px solid rgba(255, 200, 200, 0.35);
      pointer-events: none;
    }
    .fresnel-prism-ring.ring-outer { width: 30px; height: 30px; }
    .fresnel-prism-ring.ring-mid   { width: 20px; height: 20px; }
    .fresnel-prism-ring.ring-inner { width: 10px; height: 10px; }
    .fresnel-lens-glare {
      position: absolute;
      top: 5px;
      left: 7px;
      width: 7px;
      height: 5px;
      border-radius: 50%;
      background: #ffffff;
      opacity: 0.8;
      filter: blur(0.5px);
      pointer-events: none;
    }
    .tally-rec-state-icon {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #ffffff;
      box-shadow: 0 0 6px #ffffff;
      transition: all 0.25s ease;
    }
    .video-shutter-assembly.is-recording .video-fresnel-lens-core {
      background: radial-gradient(circle at 45% 40%, #ffffff 0%, #ff6b6b 25%, #e03e3e 60%, #8c0a0a 100%);
      box-shadow: 0 0 24px rgba(224, 62, 62, 0.95), 0 0 8px #ffffff;
      animation: tallyPulseGlow 0.8s infinite alternate ease-in-out;
    }
    .video-shutter-assembly.is-recording .tally-rec-state-icon {
      width: 9px;
      height: 9px;
      border-radius: 2px;
    }

    .lens-mount-assembly {
      width: 50px;
      height: 50px;
      cursor: pointer;
      position: relative;
      flex-shrink: 0;
      user-select: none;
      perspective: 400px;
    }
    .pl-mount-flange-base {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: radial-gradient(circle at 35% 35%, #3c3c46 0%, #202026 50%, #101014 100%);
      border: 1.2px solid #525260;
      padding: 3px;
      box-shadow: 0 8px 18px rgba(0, 0, 0, 0.8), inset 0 1px 2px rgba(255, 255, 255, 0.25);
      transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;
    }
    .lens-mount-assembly:hover .pl-mount-flange-base {
      transform: scale(1.05);
      border-color: #88889a;
      box-shadow: 0 10px 24px rgba(0, 0, 0, 0.9), 0 0 12px rgba(255, 255, 255, 0.15);
    }
    .lens-mount-assembly:active .pl-mount-flange-base {
      transform: scale(0.91);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.9);
    }
    .pl-mount-rotating-barrel {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: transform 0.45s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    }
    .pl-locking-lug {
      position: absolute;
      background: linear-gradient(135deg, #ffffff 0%, #9a9aa4 50%, #44444e 100%);
      border: 0.5px solid #222228;
      border-radius: 1px;
      box-shadow: 0 1px 2px rgba(0,0,0,0.8);
    }
    .pl-locking-lug.lug-top {
      top: -2.5px;
      left: 50%;
      transform: translateX(-50%);
      width: 11px;
      height: 4px;
    }
    .pl-locking-lug.lug-bottom-right {
      bottom: 6px;
      right: 0px;
      width: 9px;
      height: 4px;
      transform: rotate(-60deg);
    }
    .pl-locking-lug.lug-bottom-left {
      bottom: 6px;
      left: 0px;
      width: 9px;
      height: 4px;
      transform: rotate(60deg);
    }
    .pl-flange-face {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: #18181f;
      border: 1px solid #727282;
      box-shadow: inset 0 2px 4px rgba(0,0,0,0.9);
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .pl-torx-screw {
      position: absolute;
      width: 2.2px;
      height: 2.2px;
      border-radius: 50%;
      background: #727282;
      border: 0.4px solid #111;
    }
    .pl-torx-screw.s1 { top: 3px; left: 7px; }
    .pl-torx-screw.s2 { top: 3px; right: 7px; }
    .pl-torx-screw.s3 { bottom: 3px; left: 7px; }
    .pl-torx-screw.s4 { bottom: 3px; right: 7px; }
    .pl-optical-glass-core {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: radial-gradient(circle at 35% 35%, #180d28 0%, #0c1420 50%, #030810 100%);
      border: 0.8px solid #362248;
      box-shadow: inset 0 2px 5px rgba(0,0,0,0.95);
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
    }
    .ar-coating-reflection {
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      border-radius: 50%;
      background: linear-gradient(135deg, rgba(224, 64, 251, 0.4) 0%, rgba(0, 229, 255, 0.35) 50%, transparent 80%);
      pointer-events: none;
    }
    .pl-focal-label {
      font-family: var(--font-mono);
      font-size: 7.5px;
      font-weight: 900;
      color: #ffffff;
      text-shadow: 0 1px 2px #000;
      position: relative;
      z-index: 2;
    }
"""

SECTION_TO_ADD = """
    <!-- Category 6: 底部拟真机械硬件控制展台 (Skeuomorphic Hardware Controls) -->
    <section class="category-section" style="margin-top: 56px;">
      <div class="category-header">
        <h2 class="category-title">
          <span>六、 底部五大拟真机械硬件控制展台</span>
          <span class="category-badge" style="background:rgba(224,62,62,0.15); color:var(--color-darkroom-red); border-color:rgba(224,62,62,0.35);">PHYSICAL SKEUOMORPHIC CONTROLS</span>
        </h2>
        <span class="category-desc">高保真纯矢量与机加工物理质感重绘 · 真实的 35mm 齿孔胶片、冷钢螺纹快门线、暗房红 Tally 与 PL 电影卡口</span>
      </div>

      <!-- Live Interactive Shutter Deck -->
      <div style="background: radial-gradient(circle at 50% 30%, #202028 0%, #101014 100%); border: 1.5px solid var(--color-chassis-border); border-radius: 18px; padding: 36px 24px; box-shadow: 0 24px 60px rgba(0,0,0,0.85); margin-bottom: 30px;">
        <div style="text-align: center; margin-bottom: 24px;">
          <span style="font-family: var(--font-mono); font-size: 11px; font-weight: 700; color: var(--color-amber-grain); letter-spacing: 1.5px;">ONESHOT FILM — PHYSICAL CONSOLE BENCH (可直接在下方点击互动)</span>
        </div>

        <div style="display: flex; align-items: center; justify-content: center; gap: 28px; flex-wrap: wrap;">
          <!-- 1. 35mm Slide Negative Mount Gallery Button -->
          <div style="display:flex; flex-direction:column; align-items:center; gap:12px;">
            <div class="gallery-slide-btn" id="demoBtnGallery" style="width: 76px; height: 76px;" title="点击体验胶片夹沉降">
              <div class="slide-mount-frame" style="padding: 4px; border-radius: 9px;">
                <div class="slide-sprocket-strip top-strip" style="height: 12px;">
                  <span class="sprocket-hole" style="width: 7.5px; height: 6px;"></span>
                  <span class="sprocket-hole" style="width: 7.5px; height: 6px;"></span>
                  <span class="sprocket-kodak-code" style="font-size: 7px;">KODAK 400</span>
                  <span class="sprocket-hole" style="width: 7.5px; height: 6px;"></span>
                </div>
                <div class="slide-emulsion-window" style="border-radius: 4px;">
                  <div class="slide-fallback-graphic">
                    <div class="fallback-sun" style="width: 14px; height: 14px;"></div>
                    <div class="fallback-peaks" style="height: 20px;"></div>
                  </div>
                  <div class="slide-specular-glare"></div>
                  <span class="slide-frame-stamp" id="demoFrameStamp" style="font-size: 7.5px;">12A ▸</span>
                </div>
                <div class="slide-sprocket-strip bottom-strip" style="height: 12px;">
                  <span class="sprocket-hole" style="width: 7.5px; height: 6px;"></span>
                  <span class="sprocket-kodak-code" style="font-size: 7px;">SAFETY FILM</span>
                  <span class="sprocket-hole" style="width: 7.5px; height: 6px;"></span>
                  <span class="sprocket-hole" style="width: 7.5px; height: 6px;"></span>
                </div>
              </div>
            </div>
            <span style="font-family:var(--font-mono); font-size:11.5px; font-weight:700; color:var(--color-amber-grain);">35mm 冲洗底片相册</span>
          </div>

          <!-- 2. Motion Photo Shutter -->
          <div style="display:flex; flex-direction:column; align-items:center; gap:12px;">
            <div class="motion-shutter-assembly" id="demoBtnMotion" style="width: 76px; height: 76px;" title="点击体验同心声光扩散">
              <div class="motion-knurled-bezel">
                <div class="motion-stepped-chamber">
                  <div class="motion-acoustic-wave wave-1" style="width: 38px; height: 38px;"></div>
                  <div class="motion-acoustic-wave wave-2" style="width: 54px; height: 54px;"></div>
                  <div class="motion-live-core" id="demoLiveCore" style="width: 22px; height: 22px;"></div>
                  <span class="motion-sub-tag" style="font-size: 8.5px; margin-top: 4px;">LIVE</span>
                </div>
              </div>
            </div>
            <span style="font-family:var(--font-mono); font-size:11.5px; font-weight:700; color:var(--color-film-silver);">黄铜动态照片快门</span>
          </div>

          <!-- 3. Decisive Mechanical Shutter -->
          <div style="display:flex; flex-direction:column; align-items:center; gap:12px;">
            <div class="decisive-shutter-assembly" id="demoBtnDecisive" style="width: 100px; height: 100px;" title="点击体验冷钢机械沉孔与曝光闪光">
              <div class="decisive-knurled-outer-collar" style="padding: 6px;">
                <div class="decisive-mirror-chrome-ring" style="padding: 4px;">
                  <div class="decisive-recessed-chamber" style="padding: 4px;">
                    <div class="decisive-convex-piston">
                      <div class="decisive-cable-socket-pit" style="width: 24px; height: 24px;">
                        <div class="cable-socket-inner-threads" style="width: 15px; height: 15px;"></div>
                        <div class="cable-socket-guide-slit" style="width: 18px;"></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <span style="font-family:var(--font-mono); font-size:11.5px; font-weight:700; color:#ffffff;">冷钢决断机械快门总成</span>
          </div>

          <!-- 4. Cinema Video Tally Switch -->
          <div style="display:flex; flex-direction:column; align-items:center; gap:12px;">
            <div class="video-shutter-assembly" id="demoBtnVideo" style="width: 76px; height: 76px;" title="点击切换 Tally 呼吸录制状态">
              <div class="video-protective-guard">
                <div class="video-trench-moat">
                  <div class="video-fresnel-lens-core">
                    <div class="fresnel-prism-ring ring-outer" style="width: 44px; height: 44px;"></div>
                    <div class="fresnel-prism-ring ring-mid" style="width: 30px; height: 30px;"></div>
                    <div class="fresnel-prism-ring ring-inner" style="width: 16px; height: 16px;"></div>
                    <div class="fresnel-lens-glare" style="width: 11px; height: 8px;"></div>
                    <div class="tally-rec-state-icon" style="width: 13px; height: 13px;"></div>
                  </div>
                </div>
              </div>
            </div>
            <span style="font-family:var(--font-mono); font-size:11.5px; font-weight:700; color:var(--color-darkroom-red);">暗房红菲涅尔 Tally 快门</span>
          </div>

          <!-- 5. PL Cinema Lens Mount Flip -->
          <div style="display:flex; flex-direction:column; align-items:center; gap:12px;">
            <div class="lens-mount-assembly" id="demoBtnFlip" style="width: 74px; height: 74px;" title="点击体验 PL 卡口 180° 旋转锁止">
              <div class="pl-mount-flange-base">
                <div class="pl-mount-rotating-barrel" id="demoPlBarrel">
                  <div class="pl-locking-lug lug-top" style="width: 16px; height: 5px; top: -3px;"></div>
                  <div class="pl-locking-lug lug-bottom-right" style="width: 13px; height: 5px; bottom: 8px;"></div>
                  <div class="pl-locking-lug lug-bottom-left" style="width: 13px; height: 5px; bottom: 8px;"></div>
                  <div class="pl-flange-face" style="width: 54px; height: 54px;">
                    <span class="pl-torx-screw s1" style="width: 3.5px; height: 3.5px;"></span>
                    <span class="pl-torx-screw s2" style="width: 3.5px; height: 3.5px;"></span>
                    <span class="pl-torx-screw s3" style="width: 3.5px; height: 3.5px;"></span>
                    <span class="pl-torx-screw s4" style="width: 3.5px; height: 3.5px;"></span>
                    <div class="pl-optical-glass-core" style="width: 36px; height: 36px;">
                      <div class="ar-coating-reflection"></div>
                      <span class="pl-focal-label" id="demoFocalLabel" style="font-size: 10.5px;">1x MAIN</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <span style="font-family:var(--font-mono); font-size:11.5px; font-weight:700; color:var(--color-film-silver);">PL 电影卡口 (180° 机械旋动)</span>
          </div>
        </div>
      </div>
    </section>
"""

JS_TO_ADD = """
    // Interactive Demo Bindings for Category 6 Hardware Deck
    let demoFocalIdx = 0;
    const demoFocals = ['1x MAIN', '2x TELE', '0.5x ULTRA'];
    let demoRotation = 0;

    document.getElementById('demoBtnFlip').addEventListener('click', () => {
      demoRotation += 180;
      document.getElementById('demoPlBarrel').style.transform = `rotate(${demoRotation}deg)`;
      demoFocalIdx = (demoFocalIdx + 1) % demoFocals.length;
      document.getElementById('demoFocalLabel').textContent = demoFocals[demoFocalIdx];
      showToast(`PL 镜头卡口转动锁止: ${demoFocals[demoFocalIdx]}`);
    });

    document.getElementById('demoBtnVideo').addEventListener('click', () => {
      const btn = document.getElementById('demoBtnVideo');
      btn.classList.toggle('is-recording');
      const isRec = btn.classList.contains('is-recording');
      showToast(isRec ? 'Tally 红灯呼吸录制启动 (REC ON)' : 'Tally 录制停止 (STANDBY)');
    });

    let demoFrameNum = 12;
    document.getElementById('demoBtnDecisive').addEventListener('click', () => {
      demoFrameNum++;
      document.getElementById('demoFrameStamp').textContent = `${demoFrameNum}A ▸`;
      showToast(`机械快门帘幕击发！已定格底片 #${demoFrameNum}A`);
    });

    document.getElementById('demoBtnMotion').addEventListener('click', () => {
      const core = document.getElementById('demoLiveCore');
      core.style.transform = 'scale(1.4)';
      core.style.boxShadow = '0 0 24px #fff';
      setTimeout(() => {
        core.style.transform = '';
        core.style.boxShadow = '';
      }, 300);
      showToast('Live Emulsion 动态照片抓拍完成！');
    });

    document.getElementById('demoBtnGallery').addEventListener('click', () => {
      showToast('35mm 幻灯片胶卷夹已取出');
    });
"""

def main():
    file_path = "preview/icons_showcase.html"
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # 1. Inject CSS before </style>
    if "HYPER-REALISTIC SKEUOMORPHIC CONTROLS SHOWCASE STYLES" not in content:
        content = content.replace("</style>", CSS_TO_ADD + "\n  </style>")

    # 2. Inject Category 6 before <!-- Comparison Section: Emoji vs Cinema Vectors -->
    if "六、 底部五大拟真机械硬件控制展台" not in content:
        content = content.replace(
            "<!-- Comparison Section: Emoji vs Cinema Vectors -->",
            SECTION_TO_ADD + "\n    <!-- Comparison Section: Emoji vs Cinema Vectors -->"
        )

    # 3. Inject JS before window.addEventListener('DOMContentLoaded', initShowcase);
    if "demoBtnFlip" not in content:
        content = content.replace(
            "window.addEventListener('DOMContentLoaded', initShowcase);",
            JS_TO_ADD + "\n    window.addEventListener('DOMContentLoaded', initShowcase);"
        )

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

    print("Successfully enhanced preview/icons_showcase.html with interactive Skeuomorphic Hardware Stage!")

if __name__ == "__main__":
    main()
