#version 330 core
in vec2 TexCoords;
out vec4 outColor;

uniform sampler2D textTexture;
uniform vec4 textColor;

void main() {
    float alpha = texture(textTexture, TexCoords).a;

    if (alpha <= 0.001) {
        discard;
    }

    outColor = vec4(textColor.rgb, textColor.a * alpha);
}