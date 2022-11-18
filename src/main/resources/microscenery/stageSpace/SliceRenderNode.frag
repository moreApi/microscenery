#version 450 core
#extension GL_ARB_separate_shader_objects: enable

layout(location = 0) out vec4 FragColor;

layout(location = 0) in VertexData {
    vec4 Position;
    vec3 Normal;
    vec2 TexCoord;
    vec3 FragPosition;
} Vertex;

const int NUM_OBJECT_TEXTURES = 6;

struct MaterialInfo {
vec3 Ka;
vec3 Kd;
vec3 Ks;
float Roughness;
float Metallic;
float Opacity;
};

layout(set = 3, binding = 0) uniform MaterialProperties {
    int materialType;
    MaterialInfo Material;
};

layout(set = 4, binding = 0) uniform sampler2D ObjectTextures[NUM_OBJECT_TEXTURES];
//0 = Ambient = Colormap
//1 = Diffuse = Data
//2 = Specular = TransferFunction


float convert( float v )
{
    float tfOffset = Material.Metallic;
    float tfScale = Material.Roughness;
    return tfOffset + tfScale * v;
}

vec4 sampleVolume( vec2 texCoord )
{
    float rawsample = convert(texture(ObjectTextures[1], texCoord).r);
    float tf = texture(ObjectTextures[2], vec2(rawsample + 0.001f, 0.5f)).r;
    vec3 cmapplied = texture(ObjectTextures[0], vec2(rawsample + 0.001f, 0.5f)).rgb;

    int intransparent = 0;
    return vec4(cmapplied*tf, 1) * intransparent + vec4(cmapplied, tf) * (1-intransparent);
}

void main()
{
    vec4 v = sampleVolume(Vertex.TexCoord);
    v.xyz = pow(v.xyz, vec3(1/2.2));
    FragColor = v;
}
