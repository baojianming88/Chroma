## Chroma_V2.0.1

### 实现功能

- 内置背景与相册背景选择功能
- 绿幕抠图预览模式和实景预览模式
  - 使用camera2+surfaceView预览绿幕模式，textureView预览实景模式
- 反转相机功能
- 拍照功能
- 摄像录制功能

### UI界面

- UI界面为UI2版本
- 增加了美颜和滤镜按钮
- 增加直播按钮

### 优化记录

- chroma 预览和实景预览都使用camera2 + surfaceView实现预览，其中实景预览也使用了OpenGL渲染
- chroma 预览和实景预览不再分别new一个fragment，而是选择放入到同一个fragment中。两个surface+两个cameraHelper对象分别完成chroma模式和实景模式下的渲染。
- 优化了多开预览内存上涨现象，但是还是多开还是存在少量上涨

### 未来规划

- 增加滤镜功能
- 增加美颜功能
- 增加直播推流功能