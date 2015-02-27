package com.joemarshall.flipgame;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

public class SoundFX
{
    private SoundPool m_Pool;
    HashMap<String, Integer> m_FXMap;

    public SoundFX(Context context, String[] samples, int[] resIDs)
    {
        m_Pool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        m_FXMap = new HashMap<String, Integer>();
        for(int c = 0; c < samples.length; c++)
        {
            int id = m_Pool.load(context, resIDs[c], 1);
            m_FXMap.put(samples[c], id);
        }
    }

    public void playSound(String name)
    {
        playSound(name, 1);
    }

    public void playSound(String name, int priority)
    {
        m_Pool.play(m_FXMap.get(name), 1, 1, 0, 0, 1.0f);
    }

    public void stopSounds()
    {
        m_Pool.autoPause();
    }
}
