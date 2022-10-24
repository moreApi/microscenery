#version 450 core
#extension GL_ARB_separate_shader_objects: enable

layout(location = 0) out vec4 FragColor;


layout(location = 0) in VertexData {
    vec4 Position;
    vec3 Normal;
    vec2 TexCoord;
    vec3 FragPosition;
} Vertex;

layout(set = 0, binding = 0) uniform VRParameters {
    mat4 projectionMatrices[2];
    mat4 inverseProjectionMatrices[2];
    mat4 headShift;
    float IPD;
    int stereoEnabled;
} vrParameters;

const int MAX_NUM_LIGHTS = 1024;

layout(set = 1, binding = 0) uniform LightParameters {
    mat4 ViewMatrices[2];
    mat4 InverseViewMatrices[2];
    mat4 ProjectionMatrix;
    mat4 InverseProjectionMatrix;
    vec3 CamPosition;
};

layout(push_constant) uniform currentEye_t {
    int eye;
} currentEye;


const int NUM_OBJECT_TEXTURES = 6;

struct Light {
    float Linear;
    float Quadratic;
    float Intensity;
    vec4 Position;
    vec4 Color;
};


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
    return Material.Metallic + Material.Roughness * v;
}

vec4 sampleVolume( vec2 texCoord )
{
    float rawsample = convert(texture(ObjectTextures[1], texCoord).r);
    float tf = texture(ObjectTextures[2], vec2(rawsample + 0.001f, 0.5f)).r;
    vec3 cmapplied = texture(ObjectTextures[0], vec2(rawsample + 0.001f, 0.5f)).rgb;

    //int intransparent = int( slicing && isInSlice) ;
    int intransparent = 0;
    return vec4(cmapplied*tf, 1) * intransparent + vec4(cmapplied, tf) * (1-intransparent);
}

void main()
{
    vec4 v = sampleVolume(Vertex.TexCoord);
    v.xyz = pow(v.xyz, vec3(1/2.2));
    FragColor = v;
}
