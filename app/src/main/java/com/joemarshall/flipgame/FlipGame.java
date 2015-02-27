package com.joemarshall.flipgame;

import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;

import com.smp.soundtouchandroid.OnProgressChangedListener;
import com.smp.soundtouchandroid.SoundStreamAudioPlayer;

import java.io.IOException;
import java.util.Random;

import controlP5.Button;
import controlP5.ControlFont;
import controlP5.ControlP5;
import controlP5.Slider;
import processing.core.PApplet;
import processing.core.PFont;

public class FlipGame extends PApplet implements OnProgressChangedListener
{
    SensorReader m_SensorReader;

    ControlP5 m_Cp5;
    boolean m_Recording = false;

    DataSender m_DataSender;
    FileSaver m_FileSaver;

    PFont bigFont, littleFont;
    GameState m_State = GameState.INITIAL_STATE;
    GameState m_NextState = GameState.FRONT_SCREEN;
    GamePreferences m_Prefs;

    TTS m_TTS;
    SoundFX m_SoundFX;
    SoundStreamAudioPlayer m_Music;

    WakeLock m_WakeLock;
    WifiLock m_WifiLock;

    @Override
    public void onCreate(Bundle arg0)
    {
        super.onCreate(arg0);
        try
        {
            final AssetFileDescriptor musicfd = getResources()
                    .openRawResourceFd(R.raw.jason_shaw_tech_talk);
            m_Music = new SoundStreamAudioPlayer(0, musicfd.getFileDescriptor(),
                                                 musicfd.getStartOffset(), musicfd.getLength(),
                                                 1.0f, 0.0f);
            new Thread(m_Music).start();
            m_Music.setOnProgressChangedListener(this);
            m_Music.setLoopEnd(28009000L);
            m_Music.setLoopStart(12005000L);
            m_Music.setVolume(0f, 0f);
        } catch(IOException e)
        {
            println("Music failed to load");
        }

    }

    public void setup()
    {
        String[] soundNames = {"[tick]", "[fast_tick]", "[bang]", "[ping]",
                "[levelbreak]"};
        int[] soundIDs = {R.raw.tick, R.raw.fast_tick, R.raw.bang, R.raw.ping,
                R.raw.levelbreak};

        m_TTS = new TTS(this, soundNames, soundIDs);
        m_SoundFX = new SoundFX(this, soundNames, soundIDs);
        m_Prefs = new GamePreferences(this);
        m_FileSaver = new FileSaver(this);
        m_DataSender = new DataSender(this);
        m_FileSaver.scanMediaFiles();
        m_Cp5 = new ControlP5(this);
        bigFont = createFont("Arial", displayHeight / 25, true); // use
        // true/false
        // for
        // smooth/no-smooth
        littleFont = createFont("Arial", displayHeight / 30, true); // use
        // true/false
        // for
        // smooth/no-smooth
        ControlFont font = new ControlFont(bigFont, displayHeight / 25);
        m_Cp5.setFont(font);
        changeState(GameState.FRONT_SCREEN);

        grabWakeLocks();
        m_SensorReader = new SensorReader(this);

        thread("checkConnectionThread");

    }

    public void changeState(GameState newState)
    {
        m_NextState = newState;
    }

    public void internalChangeState(GameState newState)
    {
        switch(m_State)
        {
            case DEBUG_SCREEN:
                leaveDebugScreen();
                break;
            case FRONT_SCREEN:
                leaveFrontScreen();
                break;
            case IN_GAME:
                leaveGameScreen();
                break;
            case PREFS_SCREEN:
                leavePrefsScreen();
                break;
        }
        m_State = newState;
        switch(m_State)
        {
            case DEBUG_SCREEN:
                enterDebugScreen();
                break;
            case FRONT_SCREEN:
                enterFrontScreen();
                break;
            case IN_GAME:
                record(1);
                enterGameScreen();
                break;
            case PREFS_SCREEN:
                enterPrefsScreen();
                break;
        }
    }

    ButtonGrid m_TimeMultiplierGrid;
    Slider m_NumberOfMovesSlider;
    Slider m_Level1TimeSlider;
    Slider m_LevelMultiplierSlider;
    Button m_PrefsDefaultButton;

    private void enterPrefsScreen()
    {

        m_TimeMultiplierGrid = new ButtonGrid(m_Cp5,
                                              GameMovement.values().length, 4, width / 10,
                                              height / 10, width
                - width / 5, height / 2 - height / 10);

        m_TimeMultiplierGrid.makeCol(0, "0");
        m_TimeMultiplierGrid.makeCol(1, "1x");
        m_TimeMultiplierGrid.makeCol(2, "1.5x");
        m_TimeMultiplierGrid.makeCol(3, "2x");
        int row = 0;
        for(GameMovement g : GameMovement.values())
        {
            m_TimeMultiplierGrid.setRowLabel(row, g.m_Name);
            float multiplier = m_Prefs.m_MoveTimeMultipliers.get(g);
            if(multiplier == 0)
            {
                m_TimeMultiplierGrid.setValue(row, 0);
            } else if(multiplier == 1.5)
            {
                m_TimeMultiplierGrid.setValue(row, 2);
            } else if(multiplier == 2)
            {
                m_TimeMultiplierGrid.setValue(row, 3);
            } else
            {
                m_TimeMultiplierGrid.setValue(row, 1);
            }
            row += 1;
        }
        int yPos = height / 2;
        m_NumberOfMovesSlider = m_Cp5.addSlider("Moves / level")
                .setPosition(width / 10, yPos)
                .setSize(width - width / 5, height / 10)
                .setNumberOfTickMarks(16).setMax(20).setMin(5)
                .align(CENTER, CENTER, CENTER, CENTER);
        m_NumberOfMovesSlider.getCaptionLabel().setFont(littleFont);
        yPos += height / 10;
        m_NumberOfMovesSlider.setValue(m_Prefs.m_MovesPerLevel);
        m_Level1TimeSlider = m_Cp5.addSlider("Secs / move")
                .setPosition(width / 10, yPos)
                .setSize(width - width / 5, height / 10).setMin(5).setMax(20)
                .align(CENTER, CENTER, CENTER, CENTER);
        m_Level1TimeSlider.getCaptionLabel().setFont(littleFont);

        yPos += height / 10;
        m_Level1TimeSlider.setValue(m_Prefs.m_Level1TimePerMove);
        m_LevelMultiplierSlider = m_Cp5.addSlider("Speed/level")
                .setPosition(width / 10, yPos)
                .setSize(width - width / 5, height / 10).setMin(0.5f)
                .setMax(0.95f).align(CENTER, CENTER, CENTER, CENTER);
        m_LevelMultiplierSlider.setValue(m_Prefs.m_TimeMultiplyPerLevel);
        m_LevelMultiplierSlider.getCaptionLabel().setFont(littleFont);
        yPos += height / 10;
        yPos += height / 20;
        m_PrefsDefaultButton = m_Cp5.addButton("setDefaultPrefs").setPosition(width / 10, yPos)
                .setSize(width - width / 5, height / 10).setCaptionLabel("Defaults");

    }

    public void setDefaultPrefs()
    {
        for(int c = 0; c < GameMovement.values().length; c++)
        {
            m_TimeMultiplierGrid.setValue(c, 1);
        }
        m_LevelMultiplierSlider.setValue(0.8f);
        m_Level1TimeSlider.setValue(10.0f);
        m_NumberOfMovesSlider.setValue(10.0f);
    }

    private void leavePrefsScreen()
    {
        int row = 0;
        for(GameMovement g : GameMovement.values())
        {
            int pos = m_TimeMultiplierGrid.getValue(row);
            float val = 1f;
            switch(pos)
            {
                case 0:
                    val = 0f;
                    break;
                case 1:
                    val = 1f;
                    break;
                case 2:
                    val = 1.5f;
                    break;
                case 3:
                    val = 2f;
                    break;
            }
            println(g + ":" + val);
            m_Prefs.m_MoveTimeMultipliers.put(g, val);
            row += 1;
        }
        m_Prefs.m_Level1TimePerMove = m_Level1TimeSlider.getValue();
        m_Prefs.m_MovesPerLevel = (int) m_NumberOfMovesSlider.getValue();
        m_Prefs.m_TimeMultiplyPerLevel = m_LevelMultiplierSlider.getValue();
        m_Prefs.savePrefs();
        m_TimeMultiplierGrid.destroy();
        m_Cp5.remove(m_Level1TimeSlider.getName());
        m_Cp5.remove(m_NumberOfMovesSlider.getName());
        m_Cp5.remove(m_LevelMultiplierSlider.getName());
        m_Cp5.remove(m_PrefsDefaultButton.getName());
        background(0);
    }

    boolean m_InGame = false;

    private void enterGameScreen()
    {
        m_InGame = true;
        // start game thread
        thread("inGameThread");

    }

    private void leaveGameScreen()
    {
        Thread t = m_InGameThread;
        boolean inGame = m_InGame;
        if(inGame && t != null)
        {
            // stop game thread when it next hits a sleep
            t.interrupt();
        }
    }

    private void drawGameScreen()
    {
        background(0);
        fill(255);
        textFont(bigFont);
        if(m_SensedMove != null)
        {
            text("Act:" + m_SensedMove.m_Name, 0, 0f);
            text(m_GameDebugText, 0, 0.5f * height);
        }
        if(!m_InGame)
        {
            changeState(GameState.FRONT_SCREEN);
        }
    }

    String m_GameDebugText = "";

    Thread m_InGameThread;

    // all game logic happens in this thread
    public void inGameThread()
    {
        m_InGameThread = Thread.currentThread();
        m_InGame = true;
        try
        {
            m_GameDebugText = "Set volume to full\nLock the phone\nPut it away";
            int gameScore = 0;
            long startTime = System.currentTimeMillis();
            pauseGame(4000);
            m_TTS.say("Get ready to start");
            m_TTS.waitForSpeech();
            startMusic();

            pauseGameUntilMusic(12000);
            Random randGen = new Random();
            boolean alive = true;
            int level = 1;
            float timePerMove = m_Prefs.m_Level1TimePerMove;
            int movesLeft = m_Prefs.m_MovesPerLevel;
            GameMovement lastMove = m_SensedMove;
            m_Music.setRate((float) 0.75 * m_Prefs.m_Level1TimePerMove
                                    / (float) timePerMove);
            while(alive)
            {
                if(movesLeft <= 0)
                {
                    // next level
                    level += 1;
                    timePerMove *= m_Prefs.m_TimeMultiplyPerLevel;
                    movesLeft = m_Prefs.m_MovesPerLevel;
                    fadeMusic();
                    // say level BLAH
                    m_TTS.say("Level complete, rest");
                    m_TTS.waitForSpeech();
                    startMusic();
                    // wait for music to get to 10 seconds
                    pauseGameUntilMusic(10000);
                    // m_TTS.waitForSpeech();
                    m_TTS.say("Get ready for level " + level);
                    // pause a bit so it can say it
                    // m_TTS.waitForSpeech();
                    lastMove = m_SensedMove;
                    // pauseGame(1000);
                    pauseGameUntilMusic(12000);
                    m_Music.setRate((float) 0.75 * m_Prefs.m_Level1TimePerMove
                                            / (float) timePerMove);
                }
                // choose a move
                GameMovement thisMove = GameMovement.values()[randGen
                        .nextInt(GameMovement.values().length)];
                while(thisMove == lastMove || m_Prefs.m_MoveTimeMultipliers.get(thisMove) == 0f)
                {
                    thisMove = GameMovement.values()[randGen.nextInt(GameMovement
                                                                             .values().length)];
                }
                lastMove = thisMove;
                // say the name
                if(thisMove == GameMovement.MOVE_TAPTAP)
                {
                    m_TapFilter.resetTapCount();
                }
                m_TTS.say(thisMove.m_Name);
                m_TTS.waitForSpeech();
                // pauseGame(1000);

                long moveTime = (long) (timePerMove
                        * m_Prefs.m_MoveTimeMultipliers.get(thisMove) * 1000f);
                // countdown
                long targetTime = System.currentTimeMillis() + moveTime;
                boolean hitMove = false;

                long buzzStep = moveTime / 10;
                long nextBuzz = 0;
                int buzzCount = 0;
                long curTime = System.currentTimeMillis();
                while(curTime < targetTime && !hitMove)
                {
                    m_GameDebugText = "Score:" + gameScore + "\nMoves left:"
                            + movesLeft + "\nLevel:" + level + "\nTarget:"
                            + thisMove.m_Name + "\nactual:" + m_SensedMove.m_Name;
                    if(curTime >= nextBuzz || nextBuzz == 0)
                    {
                        println(curTime - startTime);
                        if(nextBuzz == 0)
                        {
                            nextBuzz = curTime;
                        } else if(buzzCount >= 8)
                        {
                            m_SoundFX.playSound("[fast_tick]");
                            //						m_TTS.say("[fast_tick]",false);
                        } else
                        {
                            m_SoundFX.playSound("[tick]");
                            //						m_TTS.say("[tick]",false);
                        }
                        buzzCount += 1;
                        nextBuzz += buzzStep;
                    }
                    if((thisMove == GameMovement.MOVE_TAPTAP && m_TapFilter.m_Taps > 2)
                            || m_SensedMove == thisMove)
                    {
                        hitMove = true;
                        gameScore +=
                                (int) (1000.0f * (float) (targetTime - curTime) / (float) moveTime);
                    }
                    pauseGame(10);
                    curTime = System.currentTimeMillis();
                }
                // Hit or missed
                if(hitMove)
                {
                    m_SoundFX.playSound("[ping]");
                    //				m_TTS.say("[ping]");
                    //				m_TTS.waitForSpeech();
                } else
                {
                    m_SoundFX.stopSounds();
                    m_TTS.say("[bang], GAME OVER. Score " + gameScore);
                    fadeMusic();
                    m_TTS.waitForSpeech();
                    alive = false;
                }
                movesLeft -= 1;
            }
        } catch(InterruptedException e)
        {
            // game thread interrupted, quit game
            m_Music.stop();
        }
        m_InGame = false;
    }

    void pauseGame(long ms) throws InterruptedException
    {
        Thread.sleep(ms);
    }

    void pauseGameUntilMusic(long milliseconds) throws InterruptedException
    {
        long microseconds = milliseconds * 1000;
        while(m_Music.getPlayedDuration() < microseconds)
        {
            pauseGame(10);
        }
    }

    Button m_NewGameButton;
    Button m_DebugScreenButton;
    Button m_PrefsScreenButton;

    private void enterFrontScreen()
    {
        m_NewGameButton = m_Cp5.addButton("createNewGame")
                .setPosition(0, (height) / 8).setSize(width, height / 5)
                .setCaptionLabel("Play");
        m_NewGameButton.getCaptionLabel().align(ControlP5.CENTER,
                                                ControlP5.CENTER);

        m_DebugScreenButton = m_Cp5.addButton("showDebugScreen")
                .setPosition(3 * width / 4, 3 * height / 4)
                .setSize(width / 4, height / 5).setCaptionLabel("Debug");
        m_DebugScreenButton.getCaptionLabel().align(ControlP5.CENTER,
                                                    ControlP5.CENTER);

        m_PrefsScreenButton = m_Cp5.addButton("showPrefsScreen")
                .setPosition(0, 3 * height / 4)
                .setSize(3 * width / 4, height / 5).setCaptionLabel("Prefs");
        m_PrefsScreenButton.getCaptionLabel().align(ControlP5.CENTER,
                                                    ControlP5.CENTER);

    }

    private void leaveFrontScreen()
    {
        m_Cp5.remove("createNewGame");
        m_Cp5.remove("showDebugScreen");
        m_Cp5.remove("showPrefsScreen");
    }

    private void drawFrontScreen()
    {
        // do nothing - we only have Control P5 controls on this screen

    }

    public void showDebugScreen()
    {
        changeState(GameState.DEBUG_SCREEN);
    }

    public void showPrefsScreen()
    {
        changeState(GameState.PREFS_SCREEN);
    }

    public void createNewGame()
    {
        changeState(GameState.IN_GAME);
    }

    // debug screen
    Button m_RecordButton;
    Button m_SendButton;

    private void enterDebugScreen()
    {
        m_RecordButton = m_Cp5.addButton("record").setPosition(width / 2, 0)
                .setSize(width / 2, height / 8);
        m_SendButton = m_Cp5.addButton("send")
                .setPosition(width / 2, height / 2)
                .setSize(width / 2, height / 8);

    }

    private void leaveDebugScreen()
    {
        m_Cp5.remove("record");
        m_Cp5.remove("send");
        m_RecordButton = null;
        m_SendButton = null;
    }

    public void drawDebugScreen()
    {
        background(0);
        int step = height / (m_Values.length + 4);
        int curY = 0;
        fill(255);
        textFont(littleFont);
        for(int c = 0; c < m_Values.length; c++)
        {
            curY += step;
            text((c + 1) + ":" + nf(m_Values[c], 2, 5), 0, curY);
        }
        curY += step;
        if(m_SensedMove != null)
        {
            text("Act:" + m_SensedMove, 0, curY);
        }
        curY += step;
        text(m_GameDebugText, 0, curY);

        textFont(littleFont);
        String name = m_FileSaver.logFileName();
        if(name.length() > 40)
        {
            name = name.substring(name.length() - 40);
        }
        curY += step;
        text("Rec:" + name, 0, curY);
    }

    float[] m_Values = new float[9];
    GameMovement m_SensedMove = null;

    public void draw()
    {
        if(m_NextState != m_State)
        {
            internalChangeState(m_NextState);
        }
        switch(m_State)
        {
            case DEBUG_SCREEN:
                drawDebugScreen();
                break;
            case FRONT_SCREEN:
                drawFrontScreen();
                break;
            case INITIAL_STATE:
                break;
            case IN_GAME:
                drawGameScreen();
                break;
            case PREFS_SCREEN:
                drawPrefsScreen();
                break;
            default:
                break;

        }
    }

    private void drawPrefsScreen()
    {
        background(0);

    }

    public void onSensorEvent(int sensorType, float x, float y, float z,
                              long timestamp, int accuracy)
    {
        if(m_Recording)
        {
            m_FileSaver.writeSensorData(sensorType, x, y, z, timestamp);

            if(m_FileSaver.logFileName() == null)
            {
                m_Recording = false;
                if(m_RecordButton != null)
                {
                    m_RecordButton.setColorBackground(0xff7f7f7f);
                }
            }
        }
        if(m_DataSender.isConnected())
        {
            if(m_SendButton != null)
            {
                m_SendButton.setColorBackground(0xff00ff00);
            }
            m_DataSender.sendSensorData(sensorType, x, y, z, timestamp);
        } else
        {
            if(m_SendButton != null)
            {
                m_SendButton.setColorBackground(0xff101010);
            }
        }
        switch(sensorType)
        {
            case Sensor.TYPE_ROTATION_VECTOR:
                m_Values[0] = x;
                m_Values[1] = y;
                m_Values[2] = z;
                updateOrientation(m_Values[1], m_Values[2]);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                m_Values[3] = x;
                m_Values[4] = y;
                m_Values[5] = z;
                updateTapFilter(x, y, z, timestamp);
                break;
        /*
         * case Sensor.TYPE_ACCELEROMETER: values[0] = x; values[1] = y;
		 * values[2] = z; break; case Sensor.TYPE_ROTATION_VECTOR: values[3] =
		 * x; values[4] = y; values[5] = z; break; case
		 * Sensor.TYPE_LINEAR_ACCELERATION: values[6] = x; values[7] = y;
		 * values[8] = z; break; case Sensor.TYPE_GYROSCOPE: values[9] = x;
		 * values[10] = y; values[11] = z; break; case
		 * Sensor.TYPE_MAGNETIC_FIELD: values[12] = x; values[13] = y;
		 * values[14] = z; break;
		 */
        }
    }


    TapFilter m_TapFilter = new TapFilter();

    private void updateTapFilter(float x, float y, float z, long timestamp)
    {
        m_TapFilter.update(x, y, z, timestamp);
        m_Values[6] = m_TapFilter.m_HpfMag;
        m_Values[7] = m_TapFilter.m_Taps;
        m_Values[8] = m_TapFilter.m_DoubleTaps;
    }

    private void updateOrientation(float pitch, float roll)
    {
        // pitch can go from -0.5*PI to +0.5*PI
        int pitchOri = 0;
        if(pitch < -0.25 * PI)
        {
            pitchOri = -1;
        } else if(pitch > 0.25 * PI)
        {
            pitchOri = 1;
        } else
        {
            pitchOri = 0;
        }
        // roll can go from -PI to +PI
        int rollOri = 0;
        if(roll < -0.25 * PI)
        {
            if(roll < -0.75 * PI)
            {
                rollOri = 2;
            } else
            {
                rollOri = -1;
            }
        } else if(roll > 0.25 * PI)
        {
            if(roll < 0.75 * PI)
            {
                rollOri = 1;
            } else
            {
                rollOri = 2;
            }
        } else
        {
            rollOri = 0;
        }

        for(GameMovement g : GameMovement.values())
        {
            if((rollOri == g.m_RollOri || -5 == g.m_RollOri)
                    && pitchOri == g.m_PitchOri)
            {
                m_SensedMove = g;
            }
        }
        if(m_State == GameState.DEBUG_SCREEN)
        {
            m_GameDebugText = "P:" + pitchOri + "R:" + rollOri;
        }

    }

    public void grabWakeLocks()
    {

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        m_WakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                              "flipGameStayAwake");
        m_WakeLock.acquire();

        WifiManager wifi = (WifiManager) (getSystemService(WIFI_SERVICE));
        m_WifiLock = wifi.createWifiLock("flipGameStayAwakeWIFI");
        m_WifiLock.acquire();
    }

    public void record(int value)
    {
        println("Start saving data");
        if(!m_Recording)
        {
            m_FileSaver.openLogFile();
            if(m_FileSaver.logFileName() != null)
            {
                m_Recording = true;
                m_RecordButton.setColorBackground(0xffff0000);
            }
        }
    }

    public void stopRecording()
    {
        println("stop saving data");
        if(m_Recording)
        {
            m_Recording = false;
            m_FileSaver.closeLogFile();
        }
    }

    public void send(int value)
    {
        println("Start sending data");
        m_DataSender.manualConnect();
    }

    public boolean dispatchKeyEvent(android.view.KeyEvent event)
    {
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK)
        {
            if(event.getAction() == KeyEvent.ACTION_DOWN && !handleBackKey())
            {
                return true;
            } else
            {
                return true;
            }
        } else
        {
            return super.dispatchKeyEvent(event);
        }
    }

    public boolean handleBackKey()
    {
        switch(m_State)
        {
            case DEBUG_SCREEN:
                changeState(GameState.FRONT_SCREEN);
                break;
            case IN_GAME:
                changeState(GameState.FRONT_SCREEN);
                break;
            case PREFS_SCREEN:
                changeState(GameState.FRONT_SCREEN);
                break;
            case FRONT_SCREEN:
            {
                m_SensorReader.stop();
                print("Stopped sensors");
                m_DataSender.stop();
                m_FileSaver.closeLogFile();
                println("closing log");
                final Handler h = new Handler();
                h.postDelayed(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        if(m_FileSaver.isClosed())
                        {
                            PApplet.println("logfile closed successfully");
                            finish();
                        } else
                        {
                            PApplet.println("Waiting for logfile close");
                            h.postDelayed(this, 100);
                        }
                    }
                }, 100);

            }
            break;

            default:
                break;

        }
        return true;
    }

    public void onDestroy()
    {
        m_Music.stop();
        m_TTS.destroy();
        super.onDestroy();
    }

    // if keyPressed worked reliably on android this would be great, but as it
    // is, I use the above code instead
    public void keyPressed()
    {
        println("Key:" + keyCode);

    }

	/*
     * void keyPressed() { println("Key:"+keyCode); // doing other things here,
	 * and then: if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
	 * println("woo"); keyCode = 0; // don't quit by default s.stop();
	 * print("Stopped sensors"); m_FileSaver.closeLogFile();
	 * println("closed log"); } }
	 */

    public void checkConnectionThread()
    {
        m_DataSender.checkConnection();
    }

    @Override
    public void onProgressChanged(int track, double currentPercentage,
                                  long position)
    {
    }

    public void startMusic() throws InterruptedException
    {
        m_Music.setVolume(0f, 0f);
        m_Music.setRate(1.0f);
        m_Music.seekTo(0);
        m_Music.start();
        for(int c = 0; c < 10; c++)
        {
            pauseGame(25);
            float vol = 0.05f * (float) (c + 1);
            m_Music.setVolume(vol, vol);
        }
    }

    public void fadeMusic() throws InterruptedException
    {
        for(int c = 0; c < 10; c++)
        {
            float vol = 0.5f - 0.05f * (float) c;
            m_Music.setVolume(vol, vol);
            pauseGame(25);
        }
        m_Music.pause();
    }

    @Override
    public void onTrackEnd(int track)
    {
    }

    @Override
    public void onExceptionThrown(String string)
    {

    }

}
