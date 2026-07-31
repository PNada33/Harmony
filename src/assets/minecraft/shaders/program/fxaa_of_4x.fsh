#version 120

#extension GL_EXT_gpu_shader4 : enable

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;

varying vec2 texCoord;



#ifndef FXAA_GREEN_AS_LUMA

















    #define FXAA_GREEN_AS_LUMA 0
#endif

#ifndef FXAA_DISCARD


    #define FXAA_DISCARD 0
#endif

/*============================================================================
                                API PORTING
============================================================================*/
    #define FxaaBool bool
    #define FxaaDiscard discard
    #define FxaaFloat float
    #define FxaaFloat2 vec2
    #define FxaaFloat3 vec3
    #define FxaaFloat4 vec4
    #define FxaaHalf float
    #define FxaaHalf2 vec2
    #define FxaaHalf3 vec3
    #define FxaaHalf4 vec4
    #define FxaaInt2 ivec2
    #define FxaaSat(x) clamp(x, 0.0, 1.0)
    #define FxaaTex sampler2D
/*--------------------------------------------------------------------------*/

    #define FxaaTexTop(t, p) texture2DLod(t, p, 0.0)

/*============================================================================
                   GREEN AS LUMA OPTION SUPPORT FUNCTION
============================================================================*/
#if (FXAA_GREEN_AS_LUMA == 0)

    FxaaFloat FxaaLuma(FxaaFloat4 rgba) { return dot(rgba.xyz, vec3(0.299, 0.587, 0.114)); }
#else
    FxaaFloat FxaaLuma(FxaaFloat4 rgba) { return rgba.y; }
#endif    

/*============================================================================
                         FXAA3 CONSOLE - PC VERSION
============================================================================*/
/*--------------------------------------------------------------------------*/
FxaaFloat4 FxaaPixelShader(




    FxaaFloat2 pos,





    FxaaFloat4 fxaaConsolePosPos,





    FxaaTex tex,











    FxaaFloat4 fxaaConsoleRcpFrameOpt,








    FxaaFloat4 fxaaConsoleRcpFrameOpt2,













    FxaaFloat fxaaConsoleEdgeSharpness,













    FxaaFloat fxaaConsoleEdgeThreshold,

















    FxaaFloat fxaaConsoleEdgeThresholdMin
) {
/*--------------------------------------------------------------------------*/
    FxaaFloat lumaNw = FxaaLuma(FxaaTexTop(tex, fxaaConsolePosPos.xy));
    FxaaFloat lumaSw = FxaaLuma(FxaaTexTop(tex, fxaaConsolePosPos.xw));
    FxaaFloat lumaNe = FxaaLuma(FxaaTexTop(tex, fxaaConsolePosPos.zy));
    FxaaFloat lumaSe = FxaaLuma(FxaaTexTop(tex, fxaaConsolePosPos.zw));
/*--------------------------------------------------------------------------*/
    FxaaFloat4 rgbyM = FxaaTexTop(tex, pos.xy);
    #if (FXAA_GREEN_AS_LUMA == 0)

        FxaaFloat lumaM = FxaaLuma(rgbyM);
    #else
        FxaaFloat lumaM = rgbyM.y;
    #endif
/*--------------------------------------------------------------------------*/
    FxaaFloat lumaMaxNwSw = max(lumaNw, lumaSw);
    lumaNe += 1.0/384.0;
    FxaaFloat lumaMinNwSw = min(lumaNw, lumaSw);
/*--------------------------------------------------------------------------*/
    FxaaFloat lumaMaxNeSe = max(lumaNe, lumaSe);
    FxaaFloat lumaMinNeSe = min(lumaNe, lumaSe);
/*--------------------------------------------------------------------------*/
    FxaaFloat lumaMax = max(lumaMaxNeSe, lumaMaxNwSw);
    FxaaFloat lumaMin = min(lumaMinNeSe, lumaMinNwSw);
/*--------------------------------------------------------------------------*/
    FxaaFloat lumaMaxScaled = lumaMax * fxaaConsoleEdgeThreshold;
/*--------------------------------------------------------------------------*/
    FxaaFloat lumaMinM = min(lumaMin, lumaM);
    FxaaFloat lumaMaxScaledClamped = max(fxaaConsoleEdgeThresholdMin, lumaMaxScaled);
    FxaaFloat lumaMaxM = max(lumaMax, lumaM);
    FxaaFloat dirSwMinusNe = lumaSw - lumaNe;
    FxaaFloat lumaMaxSubMinM = lumaMaxM - lumaMinM;
    FxaaFloat dirSeMinusNw = lumaSe - lumaNw;
    if(lumaMaxSubMinM < lumaMaxScaledClamped) 
    {
      #if (FXAA_DISCARD == 1)
        FxaaDiscard;
      #else
        return rgbyM;
      #endif
    }
/*--------------------------------------------------------------------------*/
    FxaaFloat2 dir;
    dir.x = dirSwMinusNe + dirSeMinusNw;
    dir.y = dirSwMinusNe - dirSeMinusNw;
/*--------------------------------------------------------------------------*/
    FxaaFloat2 dir1 = normalize(dir.xy);
    FxaaFloat4 rgbyN1 = FxaaTexTop(tex, pos.xy - dir1 * fxaaConsoleRcpFrameOpt.zw);
    FxaaFloat4 rgbyP1 = FxaaTexTop(tex, pos.xy + dir1 * fxaaConsoleRcpFrameOpt.zw);
/*--------------------------------------------------------------------------*/
    FxaaFloat dirAbsMinTimesC = min(abs(dir1.x), abs(dir1.y)) * fxaaConsoleEdgeSharpness;
    FxaaFloat2 dir2 = clamp(dir1.xy / dirAbsMinTimesC, -2.0, 2.0);
/*--------------------------------------------------------------------------*/
    FxaaFloat2 dir2x = dir2 * fxaaConsoleRcpFrameOpt2.zw;
    FxaaFloat4 rgbyN2 = FxaaTexTop(tex, pos.xy - dir2x);
    FxaaFloat4 rgbyP2 = FxaaTexTop(tex, pos.xy + dir2x);
/*--------------------------------------------------------------------------*/
    FxaaFloat4 rgbyA = rgbyN1 + rgbyP1;
    FxaaFloat4 rgbyB = ((rgbyN2 + rgbyP2) * 0.25) + (rgbyA * 0.25);
/*--------------------------------------------------------------------------*/
    #if (FXAA_GREEN_AS_LUMA == 0)

      float lumaB = FxaaLuma(rgbyB);
    #else
      float lumaB = rgbyB.y;
    #endif
    if((lumaB < lumaMin) || (lumaB > lumaMax)) 
      rgbyB.xyz = rgbyA.xyz * 0.5;

    return rgbyB; 
}
/*==========================================================================*/

void main() 
{

    vec4 posPos;
    posPos.xy = texCoord - (0.6 / OutSize);
    posPos.zw = texCoord + (0.6 / OutSize);

    vec4 rcpFrameOpt;
    rcpFrameOpt.xy = vec2(-0.50, -0.50) / OutSize;
    rcpFrameOpt.zw = vec2( 0.50,  0.50) / OutSize;

    vec4 rcpFrameOpt2;
    rcpFrameOpt2.xy = vec2(-2.0, -2.0) / OutSize;
    rcpFrameOpt2.zw = vec2( 2.0,  2.0) / OutSize;

    float edgeSharpness = 8.0;

    float edgeThreshold = 0.125;

    float edgeThresholdMin = 0.05;

    vec4 fxaaCol = FxaaPixelShader(texCoord, posPos, DiffuseSampler, rcpFrameOpt, rcpFrameOpt2, edgeSharpness, edgeThreshold, edgeThresholdMin);
    gl_FragColor = vec4(fxaaCol.xyz, 1.0);
}
