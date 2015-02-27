package com.joemarshall.flipgame;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.HashMap;

public class GamePreferences
{
    private SharedPreferences m_Prefs = null;

    public int m_MovesPerLevel;
    public float m_Level1TimePerMove;
    public float m_TimeMultiplyPerLevel;
    public HashMap<GameMovement, Float> m_MoveTimeMultipliers = new HashMap<GameMovement, Float>();

    GamePreferences(Context ctx)
    {
        m_Prefs = ctx.getSharedPreferences("gameprefs", 0);
        loadPrefs();
    }

    private void loadPrefs()
    {
        // movement multipliers (time is multiplied by this for each move, if 0 then move isn't used)
        for(GameMovement g : GameMovement.values())
        {
            float multiplier = m_Prefs.getFloat("moveMult_" + g.m_Name, 1.0f);
            m_MoveTimeMultipliers.put(g, multiplier);
        }
        m_Level1TimePerMove = m_Prefs.getFloat("level1TimePerMove", 10.0f);
        m_MovesPerLevel = m_Prefs.getInt("movesPerLevel", 10);
        m_TimeMultiplyPerLevel = m_Prefs.getFloat("timeMultiplyPerLevel", 0.9f);

    }

    public void savePrefs()
    {
        Editor edit = m_Prefs.edit();
        for(GameMovement g : GameMovement.values())
        {
            float mult = 1.0f;
            if(m_MoveTimeMultipliers.containsKey(g))
            {
                mult = m_MoveTimeMultipliers.get(g);
                edit.putFloat("moveMult_" + g.m_Name, mult);
            }
        }
        edit.putFloat("level1TimePerMove", m_Level1TimePerMove);
        edit.putInt("movesPerLevel", m_MovesPerLevel);
        edit.putFloat("timeMultiplyPerLevel", m_TimeMultiplyPerLevel);

        edit.commit();
    }


}
