# 泼辣修图 Android SDK 实时自动增强部分
最低版本限制 Android API Level 14 (4.0.3)

## 版权限制
包含本SDK在内的所有版本库中的内容，属于Polarr, Inc.版权所有。未经允许均不得用于商业目的。当前版本的示例SDK失效时间为2018年12月31日。如需要获取完整授权等更多相关信息，请联系我们[info@polarr.co](mailto:info@polarr.co)

## 功能模块
本SDK包含了泼辣修图App里面的全局调整功能。以下是泼辣修图的全局调整面板：

# 易用性
几行代码即可接入本SDK

## 增加 dependencies 到 Gradle文件
```groovy
// render sdk
compile (name: 'renderer-rt_ae-release', ext: 'aar')
```
## 在GL线程中初始化 PolarrRender
```java
PolarrRender polarrRender = new PolarrRender();
@Override
public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // 需要在OpenGL线程中调用
    boolean fastMode = false; // true 为视频滤镜SDK
    polarrRender.initRender(getResources(), getWidth(), getHeight(), fastMode);
}
```
## 为实时自动增强优化
```java
// 需要在OpenGL线程中调用
polarrRender.enableRealTimeAutoEnhancement(true);
```
## 创建或传入Texture
### 创建Texture
```java
// 需要在OpenGL线程中调用
// 只需要调用一次
polarrRender.createInputTexture();
// bind a bitmap to sdk
int inputTexture = polarrRender.getTextureId();
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);

// 输入Texture变化后需要调用
polarrRender.updateInputTexture();
```
### 传入一个输入Texture
创建或传入Texture选一即可
```java
// 需要在OpenGL线程中调用
//  默认为GL_TEXTURE_2D格式
polarrRender.setInputTexture(inputTexture);
// 输入Texture变化后需要调用
polarrRender.updateInputTexture();
```
### 传入不同类型的输入Texture
```java
// 需要在OpenGL线程中调用
polarrRender.setInputTexture(inputTexture, textureType); // PolarrRender.TEXTURE_2D, PolarrRender.EXTERNAL_OES
// 输入Texture变化后需要调用
polarrRender.updateInputTexture();
```
## 设置输出Texture (非必须)
如果不设置输出Texture，SDK将会创建一个输出Texture。通过[获取输出的Texture](#获取输出的Texture)获取
```java
//  必须为GL_TEXTURE_2D格式
polarrRender.setOutputTexture(outputTexture);
```
## 更新渲染尺寸。更新后需要更新输入Texture
```java
// 需要在OpenGL线程中调用
polarrRender.updateSize(width, height);
```
## 渲染
```java
@Override
public void onDrawFrame(GL10 gl) {
    // 需要在OpenGL线程中调用
    polarrRender.drawFrame();
}
```
## 实时自动增强
开启或关闭实时自动增强
```java
// 需要在OpenGL线程中调用
polarrRender.fastAutoEnhancement(isEnable);
```
## 获取输出的Texture
```java
int out = polarrRender.getOutputId();
```
## 释放资源
此方法将释放全部资源，包括：[释放OpenGl资源](#释放OpenGl资源)和[释放非OpenGL资源](#释放非OpenGL资源)
```java
// call in GL thread
polarrRender.release();
```
### 释放OpenGl资源
```java
// call in GL thread
polarrRender.releaseGLRes();
```
### 释放非OpenGL资源
```java
polarrRender.releaseNonGLRes();
```