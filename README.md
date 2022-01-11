# Polarr Photo Editor Android SDK

The Polarr Photo Editor Android SDK is an extremely portable (<200kb) library that exposes a subset of the native OpenGL rendering APIs used by [Polarr Photo Editor](https://play.google.com/store/apps/details?id=photo.editor.polarr). 

This SDK includes a starter project (co.polarr.polarrrenderdemo) that calls the Android SDK and users can try out the available editing tools. This repo is intended to demonstrate a subset of the capabilities of the full Android SDK.

The minimum Android API Level is 14 (4.0.3).

## License
The SDK included in this repository must not be used for any commercial purposes without the direct written consent of Polarr, Inc. The current version of the SDK expires on December 31, 2022. For pricing and more info regarding the full license SDK, please email [hello@polarr.ai](mailto:hello@polarr.ai).

## Functionalities
The current SDK includes everything as seen in Polarr Photo Editor's global adjustment panel

![sdk](https://user-images.githubusercontent.com/806199/27817613-efbf57be-6046-11e7-915c-7d8a48c4a716.jpg)

Starter project<br>
![Starter project](https://user-images.githubusercontent.com/5923363/28439929-bcdd097a-6dd6-11e7-8456-beef54bfaac8.gif)

# Sample Usage

Below are code samples and function calls to use the SDK

## Add dependencies to Gradle
### Required
```groovy
// render sdk
compile (name: 'renderer-release', ext: 'aar')
```
### Optional
```groovy
// face detection
compile(name: 'dlib-release', ext: 'aar')
// qr code scanner and decoder
compile (name: 'qrcode-release', ext: 'aar')
 
// qr code
compile 'com.google.zxing:core:3.2.1'
```
## Init PolarrRender in GL thread
```java
PolarrRender polarrRender = new PolarrRender();
@Override
public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // call in gl thread
    boolean fastMode = false; // true for camera app
    polarrRender.initRender(getResources(), getWidth(), getHeight(), fastMode);
}
```
## Create or Set an input texture
### Create a texture and bind
```java
// only need call one time.
polarrRender.createInputTexture();
// bind a bitmap to sdk
int inputTexture = polarrRender.getTextureId();
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);

// call after input texture changed
polarrRender.updateInputTexture();
```
### Set an input texture
```java
// GL_TEXTURE_2D by default
polarrRender.setInputTexture(inputTexture);

// call after input texture changed
polarrRender.updateInputTexture();
```
### Set a different type of input texture
```java
polarrRender.setInputTexture(inputTexture, textureType); // PolarrRender.TEXTURE_2D, PolarrRender.EXTERNAL_OES
// call after input texture changed
polarrRender.updateInputTexture();
```
## set output texture (not required)
If don't set the output texture, it will create an output texture. see[Get output texture](#get-output-texture)
```java
//  texture type should be GL_TEXTURE_2D
polarrRender.setOutputTexture(outputTexture);
```
## Update render size, need rebind input texture.
```java
// call in gl thread
polarrRender.updateSize(width, height);
```
## Global adjustments
Adjust a specific adjustment with value. The value `from -1.0f to +1.0f`.
More adjustment lables, see [Basic global adjustments](#basic-global-adjustments)
### Update by a state map
```java
String label = "contrast";
float adjustmentValue = 0.5f;
Map<String,Object> stateMap = new HashMap<>();
stateMap.put(label, adjustmentValue);

// call in gl thread
polarrRender.updateStates(stateMap);
```
### Update from a state json
```java
String stateJson = "{\"contrast\" : 0.5}";

// call in gl thread
polarrRender.updateStates(stateJson);
```
## Draw frame
```java
@Override
public void onDrawFrame(GL10 gl) {
    // call in GL thread
    polarrRender.drawFrame();
}
```
## Quickly apply effects to a bitmap
### Start a polarr render thread
```java
PolarrRenderThread polarrRenderThread = new PolarrRenderThread(getResources());
polarrRenderThread.start();
```
### Render bitmaps
```java
Bitmap inputImage;
List<Map<String, Object> filterStates; // filter state array
  
polarrRenderThread.renderBitmap(inputImage, filterStates, new RenderCallback() {
    @Override
    public void onRenderBitmap(List<Bitmap> bitmapList) {
    }
});
```
### Interrupt the render thread with resource released
```java
polarrRenderThread.interrupt();
```
### Render bitmaps in current thread
It should create a GL context. It shouldn't call in gl thread.
```java
Bitmap inputImage;
List<String> filterIdList; // filter state array
List<Bitmap> bitmapList = PolarrRender.renderBitmaps(getResources(), inputImage, filterIdList);
```
## Auto enhance
### Global auto enhance
return the changed adjustments
```java
// call in gl thread
float percent = 0.5f; // (0,1) perfact: 0.5
Map<String, Object> changedStates = polarrRender.autoEnhanceGlobal(percent);
```
### Global auto enhance if faces included
return the changed adjustments
```java
// call in gl thread
float percent = 0.5f; // (0,1) perfact: 0.5
Map<String, Object> changedStates = polarrRender.autoEnhanceGlobalForFace(percent);
```
### Face auto enhance
Need do face detection first, [Face Detection](##Face Detection)
```java
// it includes face datas. Just face datas or all stats with face datas
Map<String, Object> faceStates;
// face index, -1 means apply to all faces
int faceIndex = 0;
float percent = 0.5f; // (0,1) perfact: 0.5
boolean needReduceGlobal = true; // reduce effects from global enhancement
// do face auto enhance and update the input map, call in gl thread
polarrRender.autoEnhanceFace(faceStates, faceIndex, percent, needReduceGlobal);
// update to render call in gl thread
polarrRender.updateStates(faceStates);
```
## Face Adjustments
Need do face detection first, [Face Detection](##Face Detection)
### Adjust render adjustments of a face
```java
// Face detected data
Map<String, Object> faceStates;
// Get face adjustment
List<FaceItem> faces = (List<FaceItem>) faceStates.get("faces");
FaceItem faceItem = faces.get(index);
FaceState faceAdjustments = faceItem.adjustments;

faceAdjustments.skin_smoothness = 0; // (-1f,+1f)
faceAdjustments.skin_tone = 0; // (-1f,+1f)
faceAdjustments.skin_hue = 0; // (-1f,+1f)
faceAdjustments.skin_saturation = 0;  // (-1f,+1f)
faceAdjustments.skin_shadows = 0; // (-1f,+1f)
faceAdjustments.skin_highlights = 0; // (-1f,+1f)
faceAdjustments.teeth_whitening = 0; // (0f,+1f)
faceAdjustments.teeth_brightness = 0; // (0f,+1f)
faceAdjustments.eyes_brightness = 0; // (0f,+1f)
faceAdjustments.eyes_contrast = 0; //  (0f,+1f)
faceAdjustments.eyes_clarity = 0; // (0f,+1f)
faceAdjustments.lips_brightness = 0; // (0f,+1f)
faceAdjustments.lips_saturation = 0; // (-1f,+1f)
```
### Adjust each part sizes of a face
```java
// Face detected data
Map<String, Object> faceStates;
// Get face features
List<FaceFeaturesState> faceFeaturesStates = (List<FaceFeaturesState>) faceStates.get("face_features");
FaceFeaturesState featureSate = faceFeaturesStates.get(index);

featureSate.eye_size = {0, 0};  // {(-1f,+1f),(-1f,+1f)}
featureSate.face_width = 0; // (-1f,+1f)
featureSate.forehead_height = 0; // (-1f,+1f)
featureSate.chin_height = 0; // (-1f,+1f)
featureSate.nose_width = 0; // (-1f,+1f)
featureSate.nose_height = 0; // (-1f,+1f)
featureSate.mouth_width = 0; // (-1f,+1f)
featureSate.mouth_height = 0; // (-1f,+1f)
featureSate.smile = 0; // (-1f,+1f)
```
## Local masks
```java
Adjustment localMask = new Adjustment();
```
### Local masks colorful adjustments
```java
LocalState maskAdjustment = localMask.adjustments;
 
maskAdjustment.blur = 0.5f; // (0f, +1.5f)
maskAdjustment.exposure = 0.5f; // (-1f, +1f)
maskAdjustment.gamma = 0; // (-1f, +1f)
maskAdjustment.temperature = 0.5f; // (-1f, +1f)
maskAdjustment.tint = 0; // (-1f, +1f)
maskAdjustment.saturation = 0; // (-1f, +1f)
maskAdjustment.vibrance = 0; // (-1f, +1f)
maskAdjustment.contrast = 0.3f; // (-1f, +1f)
maskAdjustment.highlights = 0; // (-1f, +1f)
maskAdjustment.shadows = -0.8f; // (-1f, +1f)
maskAdjustment.clarity = 1f; // (-1f, +1f)
maskAdjustment.mosaic_size = 0.2f; // (0, +1f)
maskAdjustment.mosaic_pattern = "square";// "square","hexagon","dot","triangle","diamond"
maskAdjustment.shadows_hue = 0; // For blending color (0, +1f)
maskAdjustment.shadows_saturation = 0; // For blending color (0, +1f)
maskAdjustment.dehaze = -0.2f; // (-1f, +1f)
```
### Radial mask
```java
Adjustment radialMask = new Adjustment();
 
radialMask.type = "radial";
radialMask.position = new float[]{0f, 0f}; // (-0.5f, +0.5f) from center of photo
radialMask.size = new float[]{0.608f, 0.45f}; // (0f, +1f) width, height
radialMask.feather = 0.1f;  // edge feather (0, +1f)
radialMask.invert = true;
 
radialMask.disabled = false; // if true, the mask won't be rendered
 
// Need set the colorful adjustments
LocalState maskAdjustment = radialMask.adjustments;
maskAdjustment.blur = 0.5f;
...

```
### Gradient mask
```java
Adjustment gradientMask = new Adjustment();
 
gradientMask.type = "gradient";
gradientMask.startPoint = new float[]{0.12f, -0.36f}; // (-0.5f, +0.5f) from center
gradientMask.endPoint = new float[]{-0.096f, 0.26f}; // (-0.5f, +0.5f) from center
gradientMask.reflect = true;
gradientMask.invert = false;
 
gradientMask.disabled = false; // if true, the mask won't be rendered
 
// Need set the colorful adjustments
LocalState maskAdjustment = gradientMask.adjustments;
maskAdjustment.blur = 0.5f;
...
```
### Brush mask
#### Mask brush
The maximum conut of mask brushes is 4.
```java
Adjustment brushMask = new Adjustment();
 
brushMask.type = "brush";
BrushItem brushItem = new BrushItem();
brushMask.brush.add(brushItem);
 
brushItem.blend = false;
brushItem.erase = false;
brushItem.channel = new float[]{1f, 0f, 0f, 0f}; // rgba same as brushMask.channel
brushItem.flow = 0.5f; // (0, +1f)
brushItem.hardness = 0.5f; // (0, +1f)
brushItem.size = 0.5f; // (0, +1f)
brushItem.spacing = 0.5f; // key points spacing (0, +1f)
  
List<PointF> touchPoints; // (0,1)
brushItem.touchPoints.addAll(touchPoints);
  
// update the real points. won't render texture.
polarrRender.updateBrushPoints(brushItem);
 
brushMask.channel = new float[]{1f, 0f, 0f, 0f}; //rgba same as brushItem.channel
brushMask.invert = false;
 
brushMask.disabled = false; // if true, the mask won't be rendered
 
// Need set the colorful adjustments
LocalState maskAdjustment = brushMask.adjustments;
maskAdjustment.exposure = 0.6f; // (-1f, +1f)
maskAdjustment.temperature = -0.8f; // (-1f, +1f)
maskAdjustment.mosaic_size = 0.05f; // (0, +1f)
maskAdjustment.mosaic_pattern = "dot";// "square","hexagon","dot","triangle","diamond"
...
```
#### Paint brush
The maximum conut of paint brushes is 4.
```java
Adjustment brushMask = new Adjustment();
 
brushMask.type = "brush";
brushMask.brush_mode = "paint"; // mask, paint
BrushItem brushItem = new BrushItem();
brushMask.brush.add(brushItem);

brushItem.flow = 0.8f; // (0, +1f)
brushItem.size = 0.5f; // (0, +1f)
brushItem.spacing = 0.5f; // key points spacing (0, +1f)
brushItem.hardness = 1f; // (0, +1f)
brushItem.interpolate = false;
brushItem.randomize = 0.25f; // (0, +1f)
brushItem.erase = false;

brushItem.mode = "paint"; // mask, paint
brushItem.texture = "stroke_1"; // "stroke_3","stroke_4","stroke_5","stroke_6","dot","speckles","chalk"
  
List<PointF> touchPoints; // (0,1)
brushItem.touchPoints.addAll(touchPoints);
  
// update the real points. won't render texture.
polarrRender.updateBrushPoints(brushItem);
  
// add a new point. won't render texture.
PointF point;
polarrRender.addBrushPathPoint(brushItem, point);
  
brushMask.disabled = false;
 
// Need set the colorful adjustments
LocalState maskAdjustment = brushMask.adjustments;
maskAdjustment.exposure = -0.6f; // (-1f, +1f)
...
```
### Set local masks
```java
// need create a mask first follow the above steps.
Adjustment localMask;
List<Adjustment> localMasks = new ArrayList<>();
localMasks.add(localMask);
localStateMap.put("local_adjustments", localMasks);

renderView.updateStates(localStateMap);
```
## Brush paint
A independence's feature. No need call `polarrRender.drawFrame();`
### Start a bursh path
```java
BrushItem brushItem = new BrushItem();
brushItem.flow = 0.8f; // (0, +1f)
brushItem.size = 0.5f; // (0, +1f)
brushItem.spacing = 0.5f; // (0, +1f)
brushItem.hardness = 1f; // (0, +1f)
brushItem.randomize = 0.25f; // (0, +1f)
brushItem.texture = "stroke_3"; // "stroke_3","stroke_4","stroke_5","stroke_6","dot","speckles","chalk","blur","mosaic","eraser"

// call in GL thread
polarrRender.brushStart(paintBrushItem);
```
### Add touch event points
A rendering texture id is returned. It is used to preview the paint path. After `polarrRender.brushFinish()` being called, the texture becomes useless.
```java
List<PointF> points = new ArrayList<>();
  
// call in GL thread
int currentRenderTexture = polarrRender.brushPaintAdd(points);
```
### Finish a brush path
```java
// call in GL thread
polarrRender.brushPaintFinish();
```
### Get the paint layer as a texture
```java
// call in GL thread
int paintTexture = polarrRender.setBrushLastPaintingTex();
```
### Set a specific texture to the paint layer
```java
// call in GL thread
int paintTexture;
polarrRender.setBrushLastPaintingTex(paintTexture);
```
### Combine the paint texture and the original texture
```java
// call in GL thread
int paintTexture;
int outTexture;
polarrRender.combine(paintTexture, outTexture);
```
## Reset all state
Reset image to original.
```java
stateMap.clear();
// if need reset face states
FaceUtil.ResetFaceStates(faceStates);
// call in gl thread
polarrRender.updateStates(stateMap);
```
## Get output texture
```java
int out = polarrRender.getOutputId();
```
## Release all resource
It releases both [OpenGL Resources](#relese-opengl-resources) and [Non-OpenGL Resources](#relese-non-opengl-resources)
```java
// call in GL thread
polarrRender.release();
```
### Relese openGl resources
```java
// call in GL thread
polarrRender.releaseGLRes();
```
### Relese non-openGl resources
```java
polarrRender.releaseNonGLRes();
```
## Magic eraser
Apply magic eraser to a texture
```java
List<PointF> points; // mask points (0.0f, 1.0f)
MagicEraserPath path = new MagicEraserPath();
path.points = new ArrayList<>();
path.points.addAll(points);
path.radius = 20; // radius of each points，pixel
  
int targetTextureId;
// On GL thread
PolarrRender.magicEraserOneTime(resources, targetTextureId, texWidth, texHeight, path);
```
### Magic eraser (Compatible mode)
```java
int compatibleLevel = 0; // Range[0,3], default: 0. Bigger number for lower CPU.
// On GL thread
PolarrRender.magicEraserOneTimeCompatible(resources, targetTextureId, texWidth, texHeight, path, compatibleLevel);
```
## Basic global adjustments

| Properties | Range |
|-----|:-------:|
| exposure | -1, +1 |
| gamma | -1, +1 |
| contrast | -1, +1 |
| saturation | -1, +1 |
| vibrance | -1, +1 |
| distortion_horizontal | -1, +1 |
| distortion_vertical | -1, +1 |
| distortion_amount | -1, +1 |
| fringing | -1, +1 |
| color_denoise | 0, +1 |
| luminance_denoise | 0, +1 |
| dehaze | -1, +1 |
| diffuse | 0, +1 |
| temperature | -1, +1 |
| tint | -1, +1 |
| highlights | -1, +1 |
| shadows | -1, +1 |
| whites | -1, +1 |
| blacks | -1, +1 |
| clarity | -1, +1 |
| sharpen | 0, +1 |
| highlights_hue | 0, +1 |
| highlights_saturation | 0, +1 |
| shadows_hue | 0, +1 |
| shadows_saturation | 0, +1 |
| balance | -1, +1 |\
|  |  |
| hue_red | -1, +1 |
| hue_orange | -1, +1 |
| hue_yellow | -1, +1 |
| hue_green | -1, +1 |
| hue_aqua | -1, +1 |
| hue_blue | -1, +1 |
| hue_purple | -1, +1 |
| hue_magenta | -1, +1 |
|  |  |
| saturation_red | -1, +1 |
| saturation_orange | -1, +1 |
| saturation_yellow | -1, +1 |
| saturation_green | -1, +1 |
| saturation_aqua | -1, +1 |
| saturation_blue | -1, +1 |
| saturation_purple | -1, +1 |
| saturation_magenta | -1, +1 |
|  |  |
| luminance_red | -1, +1 |
| luminance_orange | -1, +1 |
| luminance_yellow | -1, +1 |
| luminance_green | -1, +1 |
| luminance_aqua | -1, +1 |
| luminance_blue | -1, +1 |
| luminance_purple | -1, +1 |
| luminance_magenta | -1, +1 |
|  |  |
| grain_amount | 0, +1 |
| grain_size | 0, +1 |
|  |  |
| mosaic_size | 0, +1 |
| mosaic_pattern | "square","hexagon","dot","triangle","diamond" |
## Face Detection
```groovy
dependencies {
    // face detection
    compile(name: 'dlib-release', ext: 'aar')
}
```
### Get face datas for rendering
Get better performance on ARGB8888, width or height less than 500px bitmap. Better runing in the async thread.
```java
Bitmap scaledBitmap; // better performance on ARGB8888, width or height less than 500px
// Init the face util
FaceUtil.InitFaceUtil(context);
// Do face detection
Map<String, Object> faces = FaceUtil.DetectFace(scaledBitmap);
// Release face util
FaceUtil.Release();

// set face datas to local states, and set to render.
localStateMap.putAll(faces);
renderView.updateStates(localStateMap);
```
### Reset face datas
```java
// no need init the face util
FaceUtil.ResetFaceStates(faceStates);
```
### Use 3rd part face detection lib
```java
// Detected points with multiple faces. Each item includes 106 points.
List<FaceUtil.FaceDetItem> faceDetItems = new ArrayList<>();
FaceUtil.FaceDetItem faceItem = new FaceUtil.FaceDetItem();
faceItem.points = detectedFacePoints;
faceItem.rect = detectedFaceRect;
int detectWidth = 720;
int detectHeight = 960;
  
Map<String, Object> faces = FaceUtil.GetFaceFeaturesWithPoints(faceDetItems, detectWidth, detectHeight);
  
renderStates.putAll(faces);
```
## Filter tools
The filter raw datas are built in renderer module.
### Get filter list
```java
// get filter packages
List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
// get a filter
FilterItem filterItem = filterPackage.filters.get(0);
```
### Apply a filter
```java
renderView.updateStates(filterItem.state);
```
### Adjustment a filter
the best value is 0.5. Above 0.5 means enhance effects, below means reduce effects.
```java
float adjustmentValue = 0.5f; // (0f, 1f)
Map<String, Object> interpolateStates = FilterPackageUtil.GetFilterStates(filterItem, adjustmentValue);
```
## QR code
### QR code request from a url
```java
// run on asyncronized thread
String statesString = QRCodeUtil.requestQRJson("http://www.polaxiong.com/users/custom_filter/1557497");
renderView.updateShaderWithStatesJson(statesString);
```
### QR code import from a image
```java
String qrImagePath;
String qrCodeData = QRUtils.decodeImageQRCode(context, qrImagePath);
  
// run on asyncronized thread
String statesString = QRCodeUtil.requestQRJson(qrCodeData);
renderView.updateShaderWithStatesJson(statesString);
```
### QR code scan and request
```java
Intent intent = new Intent(this, QRScannerActivity.class);
startActivityForResult(intent, ACTIVITY_RESULT_QR_SCANNER);
 
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (ACTIVITY_RESULT_QR_SCANNER == requestCode && resultCode == RESULT_OK) {
        if (data == null || data.getStringExtra("value") == null) {
            return;
        }
        final String urlString = data.getStringExtra("value");

        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                String statesString = QRCodeUtil.requestQRJson(urlString);
                renderView.updateShaderWithStatesJson(statesString);
            }
        });
    }
}
```
