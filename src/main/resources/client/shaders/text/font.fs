#version 330 core
in vec2 TexCoords;
out vec4 outColor;

uniform sampler2D textTexture;
uniform vec4 textColor;

void main() {
    // Sample texture alpha directly to tint standard uniforms safely
    vec4 sampled = texture(textTexture, TexCoords);
    outColor = textColor * vec4(1.0, 1.0, 1.0, sampled.a);
}
