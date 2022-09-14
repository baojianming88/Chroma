#version 300 es
precision mediump float;

uniform mat4 u_Matrix;
uniform vec2 vTextureSize;
layout (location = 0) in vec4 vPosition;
layout (location = 1) in vec4 aTextureCoord;

//输出纹理坐标(s,t)
out vec2 vSize;
out vec2 vTexCoord;
void main() {
    gl_Position  =u_Matrix * vPosition;
    vTexCoord = (aTextureCoord).xy;
    vSize = vTextureSize;
}
