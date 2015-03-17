package com.joemarshall.flipgame;

public enum GameMovement
{
    MOVE_HEAD("Head", 1, -5,0),
    MOVE_FEET("Feet", -1, -5,1),//;//,
    MOVE_LEFT("Left", 0, 1,2),
    MOVE_RIGHT("Right", 0, -1,3),
    MOVE_BACK("Back", 0, 0,4),
    MOVE_FRONT("Front", 0, 2,5),
    MOVE_TAPTAP("Tap tap tap", -10, -10,6);

    public final String m_Name;
    public final int m_ID;
    public int m_RollOri;
    public int m_PitchOri;

    private GameMovement(String name, int pitchOri, int rollOri,int id)
    {
        m_Name = name;
        m_RollOri = rollOri;
        m_PitchOri = pitchOri;
        m_ID=id;

    }


}
