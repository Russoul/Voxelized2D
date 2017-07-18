//#version 330 core
in vec2 TexCoord;
in vec3 Color;
uniform sampler2D textureID;


uniform vec3 extraColor;

void main()
{
    vec4 c = texture2D(textureID, TexCoord);

    c.x *= extraColor.x * Color.x;
    c.y *= extraColor.y * Color.y;
    c.z *= extraColor.z * Color.z;
    if(c.a < 0.1)
        discard;

    gl_FragColor = c;
}
