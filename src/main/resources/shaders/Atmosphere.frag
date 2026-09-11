#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform sampler2D m_Texture;
uniform sampler2D m_DepthTexture;
uniform mat4 m_InverseViewProjection;
uniform vec3 m_CameraPosition;
uniform vec3 m_BeamPosition;
uniform vec4 m_FogColor;
uniform float m_Time;
uniform float m_Damage;
uniform float m_Exposure;
varying vec2 texCoord;

vec3 filmic(vec3 v) { return clamp((v*(2.51*v+0.03))/(v*(2.43*v+0.59)+0.14),0.0,1.0); }
void main() {
    vec3 color=texture2D(m_Texture,texCoord).rgb;
    float depth=texture2D(m_DepthTexture,texCoord).r;
    vec4 world=m_InverseViewProjection*vec4(texCoord*2.0-1.0,depth*2.0-1.0,1.0);
    world/=world.w;
    vec3 ray=world.xyz-m_CameraPosition;
    float distanceToSurface=min(length(ray),100.0);
    vec3 direction=normalize(ray);
    float integrated=0.0;
    float shafts=0.0;
    // Bounded cost: twelve samples stop at the nearest opaque surface.
    for(int i=0;i<12;i++) {
        vec3 samplePos=m_CameraPosition+direction*distanceToSurface*(float(i)+0.5)/12.0;
        float turbulence=0.82+0.18*sin(samplePos.x*.37+m_Time*.12)*sin(samplePos.z*.29-m_Time*.08);
        float density=exp(-max(samplePos.y,0.0)*.42)*turbulence;
        integrated+=density*distanceToSurface/12.0;
        vec2 radial=samplePos.xz-m_BeamPosition.xz;
        shafts+=exp(-dot(radial,radial)*.26)*step(samplePos.y,6.5)*distanceToSurface/12.0;
    }
    float fog=1.0-exp(-integrated*.021);
    color=mix(color,m_FogColor.rgb*1.7,fog);
    color+=vec3(.23,.39,.48)*shafts*.023;
    color=filmic(color*m_Exposure);
    vec2 uv=texCoord-.5;
    float edge=smoothstep(.18,.72,length(uv));
    color*=1.0-edge*.27;
    color=mix(color,vec3(.65,.045,.02),edge*m_Damage);
    gl_FragColor=vec4(color,1.0);
}
