package com.joemarshall.flipgame;

public class TapFilter
{
    float m_HpfMag = 9.81f;
    float m_LastMag = 9.81f;
    int m_Taps = 0;
    int m_DoubleTaps = 0;

    boolean inTap = false;
    long tapStartTime = 0;
    long tapEndTime = 0;

    public void resetTapCount()
    {
        m_Taps = 0;
        m_DoubleTaps = 0;
    }

    public void update(float x, float y, float z, long timestamp)
    {
        final float HPF_COEFFICIENT = 0.05f;
        final float TAP_THRESHOLD = 0.1f;
        final long TAP_MAX_TIME = 50000000;// 20th of a second maximum tap length
        final long TAP_LATENCY = 50000000;// taps must be 20th of a second apart to be noticed
        float mag = (float) Math.sqrt(x * x + y * y + z * z);
        m_HpfMag = HPF_COEFFICIENT * (m_HpfMag + mag - m_LastMag);
        m_LastMag = mag;

        if(Math.abs(m_HpfMag) > TAP_THRESHOLD)
        {
            if(!inTap)
            {
                tapStartTime = timestamp;
                inTap = true;
            }
        } else
        {
            if(inTap)
            {
                // is this tap short enough
                if(timestamp - tapStartTime < TAP_MAX_TIME &&
                        tapStartTime - tapEndTime > TAP_LATENCY)
                {
                    m_Taps += 1;
                }
                tapEndTime = timestamp;
                inTap = false;
            }
        }
    }


}
