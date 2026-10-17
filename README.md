# OneShot Film

> **“一次快门，一次成像。”**  
> *One Shutter, One Instant. Pure Chemistry, Modern Optics.*

OneShot Film 是一款专为摄影与电影创作者打造的 Android 严肃物理拟物相机。针对后计算摄影时代算法暴力锐化、涂抹与扁平化的问题，OneShot Film 将摄影归还给光学与直出色彩。

---

## 核心设计特性

### 1. 电影级实时码流数据遥测 (Cinema-Grade Bitstream Telemetry)
在进行视频录制时，底部专业 HUD 模块会实时动态采集并输出：
- **高精度录制时间码 (`REC MM:SS`)**：配合微秒级 Tally 录像指示灯呼吸频闪；
- **物理已写入文件大小实时监控 (`| XX.X MB`)**：直接连通底层 MediaMuxer 写入字节流，告别盲盒式录像，让摄影师在实拍现场对存储介质余量、码率吞吐与成片体积具备最直观、精准的掌控力。

### 2. 纯物理拟物操控阵列 (Tactile Triple-Shutter Architecture)
- **Leica M 沉降式机械快门**：88 齿 CNC 冷钢固态滚花套筒，真 Z 轴纵深按压沉降与反光板瞬黑遮断，零等待、零多余转圈动画，带来清脆机械击发质感。
- **Live 动态照片快门**：青铜滚花双态快门，一键定格前后动态影像与高保真环境立体声。
- **ARRI/RED 电影机螺纹录像按键**：双层菲涅尔透镜与微晶红宝石核心，带防误触保护环。

### 3. 硬件级 3D LUT 胶卷色彩直出
- 内置经典胶卷色彩预设：Kodak Portra 400、Cine 250、Flat CineLog 等。
- 支持 `.cube` 工业标准 3D LUT 导入与自定义管理，实现所见即所得的直接色彩烧录，告别漫长后期。

### 4. 零延迟系统相册直跳 (Native OEM Gallery Integration)
- 点击左下角取景器底片视窗，精准、免弹窗、无缝直跳小米相册、Google 相册、三星相册等 OEM 原厂相册应用，无需多余应用内半成品弹窗。

### 5. 纯粹、无干扰取景器体验
- 4:3 Open Gate 光学毛玻璃取景视窗、经典微棱镜测距微环与三分构图微刻度。
- 全量清除非必要的 Web 风格进度条、冗余曝光标尺与干扰标语，保持视窗纯粹通透。

---

## 构建与运行

- **运行环境**：Android 11+ (API 30+)
- **推荐硬件**：支持 CameraX / Camera2 硬件级别的 Android 旗舰设备（如 Xiaomi 15 等）
- **编译工具**：Gradle 8.x + JDK 17
