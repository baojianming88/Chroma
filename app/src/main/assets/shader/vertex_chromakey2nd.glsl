#version 300 es
precision mediump float;

uniform mat4 u_Matrix;
layout (location = 0) in vec4 vPosition;
layout (location = 1) in vec4 aTextureCoord;

//输出纹理坐标(s,t)
out vec2 vTexCoord;
void main() {
    gl_Position  = vPosition;
    vTexCoord = (aTextureCoord).xy;
}