#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 vTexCoord;
uniform samplerExternalOES cameraSource;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = texture(cameraSource,vTexCoord);
}