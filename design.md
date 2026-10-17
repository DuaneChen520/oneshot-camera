# OneShot Film — 交互与物理拟真设计工程规范 (Design Specification)

> **“一次快门，一次成像。”**  
> *One Shutter, One Instant. Pure Chemistry, Modern Optics.*

---

## 目录
1. [设计哲学与总纲](#一-设计哲学与总纲-design-philosophy)
2. [物理拟真三大定律](#二-物理拟真三大定律-three-laws-of-skeuomorphism)
3. [Compose 绘图避坑与图形学准则](#三-compose-绘图避坑与图形学准则-canvas-engineering)
4. [快门交互集群设计标准](#四-快门交互集群设计标准-shutter-cluster-spec)
5. [色彩体系与工业材质映射](#五-色彩体系与工业材质映射-tokens--materials)
6. [界面布局与硬件物理避让](#六-界面布局与硬件物理避让-safe-areas--chassis)
7. [声触多模态交互规范](#七-声触多模态交互规范-tactile--haptics)

---

## 一、 设计哲学与总纲 (Design Philosophy)

在移动操作系统充斥着“无质感平铺”、“轻浮毛玻璃”与“过度计算摄影”的当下，[OneShot Film](file:///e:/personalwork/oneshot/BRAND.md) 的设计基调致力于**重拾经典光学机械的厚重感与决断感**。

- **反对“塑料假拟物”**：拟物不是随意的渐变贴图与刺眼的浮雕阴影，而是对光线、金属公差、机械间隙与阻尼行程的敬畏。
- **反对“虚无的扁平”**：严肃的影像创作需要明确的物理确认。每一次按压，都必须通过光影遮蔽、机械沉降与触觉微动，给予用户如同击发机械快门般的笃定心智。
- **克制且精准的科技美学**：界面完全依托黑底暗房与专业电影机语言，HUD（平视显示）所有读数排版严谨，绝不喧宾夺主干扰取景。

---

## 二、 物理拟真三大定律 (Three Laws of Skeuomorphism)

在 [OneShot Film](file:///e:/personalwork/oneshot/BRAND.md) 的 UI 组件库中（尤其是快门、旋钮与拨轮），所有动效与渲染必须严格遵守以下三大物理定律：

```
                [ 错误实现: 违背物理 ]                           [ 正确实现: 刚体沉降 ]
       ┌──────────────────────────────┐                ┌──────────────────────────────┐
       │   外圈套筒随按压整体缩小变细    │                │   外圈套筒固定在机身，零缩放   │
       │   缝隙变形扩张或收窄          │                │   公差缝隙恒定，仅产生阴影遮挡 │
       │   光斑静止不变，宛如纸片贴纸    │                │   压盘深入套筒，倒角高光自然消隐│
       └──────────────────────────────┘                └──────────────────────────────┘
```

### 1. 刚体不变性 (Rigid Body Invariance)
- **机身与套筒绝不缩放**：安装在相机铸铝或钛合金机身上的机械部件（如外圈防滑滚花、防护环、固定螺栓），在手指按下时**绝对不能缩放（`scale` 必须为 1.0）**。
- **缝隙公差恒定**：活动按键与套筒之间的机械间隙（Moat Gap，通常为 1.4dp ~ 1.8dp）在按压时尺寸恒定，不能随按键缩放而扩大或收窄。深度感必须通过“遮蔽阴影（Cavity Shadow）”体现，而非形变。

### 2. 全局光照与遮蔽一致性 (Global Lighting & Specular Extinction)
- **统一光源向量**：全软件默认统一使用**左上方（约 225° / -135°）主光源**。高光落在左上方倒角，阴影落在右下方背光面。
- **套筒入井消隐 (Specular Extinction)**：当按键沿 Z 轴下沉到套筒内部时，周围凸起的套筒壁会物理遮挡来自左上方的入射光线。因此，按键顶部的 45° 倒角高光必须随按压深度**线性衰减 70%**，同时凹面压盘光斑在垂直方向微移 2dp，体现“沉入暗处”的光学真实。

### 3. 声触瞬时同步性 (Instant Haptic Snap)
- **触碰即段落 (Touch-Down Snap)**：机械微动开关的导通是在弹片压下的第 1 毫秒完成的。因此，微震反馈必须绑定在 `Touch-Down`（手指接触屏幕初态），严禁等待手指抬起（`Touch-Up` 或 `onClick`）才震动，确保零输入延迟。

---

## 三、 Compose 绘图避坑与图形学准则 (Canvas Engineering)

在 Android Compose 的 `Canvas` 中进行高质感高帧率绘制时，需规避以下典型陷阱：

### 3.1 `sweepGradient` 坐标系陷阱
- **角度定义**：Compose 中的 `Brush.sweepGradient` 角度分布不同于直角几何常规习惯：
  - `0.00f` 对应 **3 点钟方向（X 轴正向）**；
  - `0.25f` 对应 **6 点钟方向（正下方）**；
  - `0.50f` 对应 **9 点钟方向（X 轴负向）**；
  - `0.65f ~ 0.70f` 对应 **10 点钟 ~ 11 点钟方向（左上方高光区）**。
- **避坑要点**：如果不显式声明 `colorStops`，渐变会均匀铺散，导致刺眼的高光色出现在 `0.25f`（正下方 6 点钟），形成物理上根本不存在的“底部反常白弧”。底部必须显式设为暗色背光或 `Transparent`。

### 3.2 严禁在圆形边缘使用非闭合 `drawArc`
- **断头切口瑕疵**：若在圆形轮廓边缘使用 `startAngle = 160f, sweepAngle = 220f` 绘制局部弧线，即便使用 `StrokeCap.Round`，在浅色金属背景上也会在 160° 与 20° 处留下明显的“毛刺断层”。
- **规避原则**：金属边缘必须是 **360° 完整闭合的同心圆**。高光的衰减应通过 `sweepGradient` 的 Alpha 渐变自然过渡，严禁机械式截断。

### 3.3 零 Overdraw 与轻量化计算
- **禁用重度着色器**：放弃昂贵的高斯模糊（`blur`）或复杂的实时着色器，全部拟真光影均由三层几何数学渐变组合完成：
  1. `Brush.radialGradient`：模拟机身外圈与井深暗腔的深度下凹；
  2. `Brush.linearGradient`：模拟金属压盘受环境定向光照射的明暗面；
  3. `Brush.sweepGradient`：模拟 45° CNC 倒角的各向异性反光与高光。
- **性能结果**：单按键绘制开销小于 0.2ms，在 120Hz 高刷屏上保持绝对满帧无卡顿。

---

## 四、 快门交互集群设计标准 (Shutter Cluster Spec)

快门交互群位于屏幕底部，由三枚不同材质与机械结构的快门及一枚侧边镜头卡口翻转键组成，代码位于 [`ShutterCluster.kt`](file:///e:/personalwork/oneshot/app/src/main/kotlin/dev/oneshot/ui/ShutterCluster.kt)。

```
  ┌──────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────┐
  │  [相册]  │   │  左：Live键  │   │  中：机械键  │   │  右：电影REC │   │ [PL卡口] │
  │ 胶卷缩略 │   │  钛金属压盘  │   │ 徕卡M凹面软放 │   │  暗房红珐琅  │   │ 镜头翻转 │
  └──────────┘   └──────────────┘   └──────────────┘   └──────────────┘   └──────────┘
```

### 4.1 左快门：Live 动态实况照片键 ([`LiveMotionShutterButton`](file:///e:/personalwork/oneshot/app/src/main/kotlin/dev/oneshot/ui/ShutterCluster.kt#L487))
- **视觉意向**：微动定格，捕捉决断瞬间前后的动态微胶卷。
- **装配结构**：
  1. **暗钛金属固定外套圈**：外沿采用 45° 钻石倒角微光；
  2. **机械公差井深槽**：1.4dp 恒定环形暗槽；
  3. **钛合金活动压盘**：盘面受压下沉，倒角高光消隐；
  4. **中央微雕图腾**：激光微雕实心核心圆点 + 外围 12 段断开式呼吸光环（去除拥挤的实线双环，为金属底色留出通透感）；
- **色彩跃迁**：待机状态为纯净胶卷银白（`0xFFDCDAE6`），击发抓拍时切换为温润的柯达琥珀黄（`AmberGrain`）。

### 4.2 中央主快门：徕卡 M 凹面机械总成 ([`MechanicalStillShutterButton`](file:///e:/personalwork/oneshot/app/src/main/kotlin/dev/oneshot/ui/ShutterCluster.kt#L671))
- **视觉意向**：向经典旁轴相机致敬的“决定性瞬间”（Decisive Frame）核心快门。
- **装配结构**：
  1. **88 齿高密度 CNC 滚花外环**：每颗齿根据 -135° 主光源计算高光反射强度，机身固定不缩放；
  2. **冷轧钢外套筒壁**：固定套筒边缘，内嵌 1.8dp 恒定机械沉孔（Borehole Well）；
  3. **凹面软释放压盘 (Concave Soft-Release Plunger)**：
     - 盘面为内陷凹面，暗部在上、亮部在下，收集下方反光；
     - 微米级 CD 纹（7 层微车削同心环）与各向异性拉丝光泽，按压时随机械阻尼微旋 4.5°；
     - 外圈 45° 抛光倒角在下沉时高光消隐 70%；
  4. **M3 螺纹机械快门线插孔**：压盘正中央同心微雕黄铜衬套（Brass Bushing）与双层机械内螺牙。

### 4.3 右快门：电影机录制开关 ([`CinemaVideoShutterButton`](file:///e:/personalwork/oneshot/app/src/main/kotlin/dev/oneshot/ui/ShutterCluster.kt#L948))
- **视觉意向**：ARRI/RED 专业电影机防护型物理 REC 按钮。
- **装配结构**：
  1. **防误触加高外保护圈**：高出机身平面的坚固金属护圈；
  2. **枪黑色实体金属压盘**：非低廉的泛红发光塑料块，而是厚重枪黑冷钢按钮；
  3. **缝隙 Tally 呼吸光晕 (Moat Tally Halo)**：
     - 待机时：呈现 1.4dp 纯净暗黑机械缝隙；
     - 录制时：红光不浮于表面，而是从金属压盘与外套筒之间的机械缝隙中呈现双层（外层漫反射 + 内层高透光红宝石光丝）向外幽幽呼吸漫溢；
  4. **沉孔暗房红珐琅点**：压盘中心微凹处填涂经典暗房红（待机深沉暗红，录制鲜活电影红）。

### 4.4 右侧辅助件：机身直嵌式双挡机械滑动开关 ([`MechanicalSquircleSlideSwitch`](file:///e:/personalwork/oneshot/app/src/main/kotlin/dev/oneshot/ui/ShutterCluster.kt#L476))
- **设计哲学升华**：彻底摆脱传统手机扁平、干瘪的“自拍翻转”按键心智，吸取经典便携胶片相机（Contax T2、Olympus μ、Leica Minilux、Ricoh GR）正统工业语汇，采用**机身内嵌式沉槽与精密冷轧钛合金防滑推钮 (Recessed Mechanical Slide Switch)**——左档位对应“风景 (Landscape / 后摄)”，右档位对应“人像 (Portrait / 前摄)”，彻底摒弃旋转拨杆带来的钟表小指针卡通感。
- **50dp 隐形容器与对齐铁律 (Strict 1:1 Layout & Touch Target Mirroring)**：
  - **50dp 布局占位与绝对水平轴对齐**：根容器尺寸严格保持 `50dp x 50dp`（不可见透明容器），圆心坐标 `(cx, cy)` 严格位于 `(25dp, 25dp)`，与左侧超椭圆相册视窗处于**绝对同一水平轴线，布局间距与点击热区 1:1 镜像对称**；
  - **纯粹机身直嵌（去外框）**：剔除外层冗余的超椭圆装饰框，避免“框中套框”，滑槽与微雕图腾直接与相机暗黑机身融为一体，利落干脆；整体容器在按压时具有整体纯 Z 轴透视沉降（`scale: 1.0 -> 0.95`，零 Y 轴滑动）。
- **沉腔内部机械构造 (Internal Mechanical Anatomy)**：
  1. **上半区：双模式机雕图腾与物理 LED 漫射背光 (Upper Stencil Stage)**：
     - **左侧（风景模式 / 后摄）**：精密实体阿尔卑斯双峰（迎光主峰、背阴峰、右次峰与小太阳，基线宽 `8.4dp`），后摄激活时透出纯净冰青白 LED 漫射散光；
     - **右侧（人像模式 / 前摄）**：古典浮雕人像胸像剪影（Cameo Bust，头部直径加大至 `3.3dp`，肩宽扩展至 `8.4dp` 与左侧严格平齐，共享基线）；前摄激活时透出温润琥珀金 LED 漫射散光；
### 4.4 快门集群金字塔阶梯体系 (Pyramid Shutter Hierarchy: 36dp -> 52dp -> 76dp -> 52dp -> 36dp)
- **设计哲学**：彻底摒弃伪等大微小阶差，确立主次悬殊、结构严密的纯正机械金字塔层级：
  - **最外两翼辅助件 (`36dp`)**：左翼相册光学视窗（`36dp`）与右翼翻转滑动开关（`36dp`），无外框直嵌机身，收敛退后为精致的机身附件；
  - **次级快门 (`52dp`)**：左二实况照片快门与右二电影录像快门，外壳固定套筒，相比两翼陡峭提升 `+16dp (+44%)`；
  - **核心主快门巅峰 (`76dp`)**：中心决定性瞬间主快门，88 齿高密度滚花环，相比次级快门再次陡峭提升 `+24dp (+46%)`。

### 4.5 右翼：经典机械滑动开关 ([`MechanicalSquircleSlideSwitch`](file:///e:/personalwork/oneshot/app/src/main/kotlin/dev/oneshot/ui/ShutterCluster.kt#L472))
- **视觉意向**：受 Contax T2、Olympus μ、Leica Minilux 机械推钮启发，采用直嵌机身的经典两档滑轨。
- **物理与装配规范**：
  1. **纯平面滑动物理力学 (Strict Planar Motion)**：
     - 滑动开关属于机加表面滑轨机构，**严格剔除任何 Z 轴按压缩放（Zero Scale / Zero Depression）**；
     - 切换触发时在滑块滑行到位落锁（Ball Detent）瞬间触发清脆硬件震动（`HapticFeedbackConstants.CONFIRM`）。
  2. **机加滑道与滑块规格**：
     - **滑道规格**：长 **`36dp`**，高 **`13.5dp`**，横向占满两翼空间，与左侧相册视窗视觉宽度 1:1 绝对齐平；
     - **滑块规格**：宽 **`14dp`**，高 **`11dp`**，表面铣出 4 道防滑槽与三角准星；
     - **滑动行程**：左右行程扩充至 **`±10dp`**，两端留有 1dp 防撞微间隙。
  3. **双模式激光微雕图腾**：
     - 左侧高耸雪山与太阳（`cx - 10dp`，后置镜头）与右侧古典 Cameo 实心胸像（`cx + 10dp`，前置人像）；
     - 未选中态采用 `0xFF9492A2` 冷钛实心雕刻，选中态带有柔和漫射背光。

### 4.6 左翼：超椭圆富士光学取景视窗 ([`GallerySquircleOpticalHatch`](file:///e:/personalwork/oneshot/app/src/main/kotlin/dev/oneshot/ui/ShutterCluster.kt#L254))
- **视觉意向**：微型一次性光学取景镜片，紧凑精致（`36dp x 36dp`），超椭圆 G2 连续曲率 ($n=3.0$)。
- **装配结构与光学拟真**：
  1. **无多余外框线条，高光直做于玻璃表面**：彻底删除外圈多余机身线框底座，高光倒角（Lens Bezel Highlight）直接以超椭圆连续曲线绘制在玻璃镜片上；
  2. **富士胶片取景器四角漫雾与虚焦渐晕**：中心通透，四角青蓝漫散射与光学暗角；
  3. **微动弹性按压手感**：独立镜片按压时产生 4% Z 轴微动透视沉降。

---

## 五、 色彩体系与工业材质映射 (Tokens & Materials)

全局色彩定义统一维护于 [`OneShotTheme.kt`](file:///e:/personalwork/oneshot/app/src/main/kotlin/dev/oneshot/ui/theme/OneShotTheme.kt)：

| Token 变量名 | 色值 (Hex) | 物理对应材质 / 工业映射 | 应用场景 |
| :--- | :--- | :--- | :--- |
| `ObsidianBlack` | `#0C0C0E` | 底片暗房黑 / 纯黑 OLED 吸收层 | 全局主背景、取景底色 |
| `ChassisDark` | `#141417` | 阳极氧化铝黑机身 | 控制栏托底、机身固定骨架 |
| `ChassisMilled` | `#1C1C20` | 滚花转盘铣削金属底 | HUD 卡片、机械底座 |
| `ChassisBorder` | `0x388E8E93` | 钛金发丝纹金属倒角 | 0.8dp 细微轮廓边线 |
| `AmberGrain` | `#F5A623` | 柯达经典琥珀黄 / 胶卷暖调色温 | 核心击发快门、活动指示、测光标尺 |
| `DarkroomRed` | `#E03E3E` | 传统暗房安全灯 / 电影机 Tally 红灯 | 电影录制键、REC 呼吸指示灯 |
| `TitaniumGray` | `#8E8E93` | 冷轧钛金 / 机械表盘精密刻线 | 辅助标尺、次级单位刻度 |
| `FilmSilver` | `#F4F4F6` | 纯银卤化银涂层 / 激光打标亮银刻字 | 核心曝光参数（快门角、ISO、EV） |
| `HardwareGreen`| `#00E676` | 专业电影机硬件就绪指示绿 | AF 锁焦确认、DCG 双增益 HDR 激活 |
| `AutoSkyBlue`  | `#64B5F6` | 自动测光模式天蓝色 | `AUTO` 曝光与白平衡标识 |

---

## 六、 界面布局与硬件物理避让 (Safe Areas & Chassis)

现代智能手机（如 Xiaomi 15/17 系列）具有极大的硬件屏幕圆角（四角半径高达 190px）与底部系统手势指示条。为了保证严肃工具的可靠性，UI 布局需执行以下物理避让标准：

1. **快门集群安全出血区 (Safety Margin)**：
   - 水平两侧内缩：`padding(start = 22.dp, end = 22.dp)`，彻底避开屏幕四角的大圆角切边；
   - 底部物理垫高：`navigationBarsPadding().padding(bottom = 18.dp)`，严防用户在触发快速快门时误触 Android 底栏返回/桌面手势；
2. **取景器绝对纯净度 (OLED True Viewfinder)**：
   - 4:3 黄金 Open Gate 画幅始终居中；
   - 取景画面周围黑边完全利用 OLED 像素熄灭特性，作为 Pro HUD 与控制栏的沉降区域，严禁浮层图标遮挡光学校准面。

---

## 七、 声触多模态交互规范 (Tactile & Haptics)

### 7.1 震动反馈等级体系
在 Compose 组件树中通过 `LocalView.current.performHapticFeedback` 实现底层精准触发：

- **确认级微动段落 (Confirm Snap)**：
  - 调用：`HapticFeedbackConstants.CONFIRM`
  - 场景：快门手指触碰按下、镜头卡口锁紧到位、物理拨轮落入刻度齿位；
- **轻微转盘齿跳 (Tick Detent)**：
  - 调用：`HapticFeedbackConstants.CLOCK_TICK`
  - 场景：EV 曝光滑块刻度跳格、快门角度步进调节；
- **警告与安全锁 (Reject / Barrier)**：
  - 调用：`HapticFeedbackConstants.REJECT`
  - 场景：存储空间不足、硬件不支持更高帧率、测光严重溢出警告。

### 7.2 弹簧力学模型配置 (Spring Physics)
快门按压与回弹严禁使用匀速或简单缓动曲线，必须使用真实的欠阻尼机械回弹：
- **快门按压弹簧 (Depression Spec)**：
  - `dampingRatio = 0.65f`（适度阻尼，模拟紧凑的机械行程，按到底部有轻微扎实的制动感）；
  - `stiffness = 750f`（高刚度，确保 50ms 内瞬间完成沉降响应，无软绵拖沓感）。
- **转盘旋转弹簧 (Knob Detent Spec)**：
  - `dampingRatio = 0.55f`；
  - `stiffness = 320f`（体现手轮拨动的惯性与吸附感）。
