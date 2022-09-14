#version 300 es
precision mediump float;

in vec2 vTexCoord;
uniform sampler2D vTexture;

layout(location = 0) out vec4 outcolor;

void main() {
    outcolor = texture(vTexture,vTexCoord);
}