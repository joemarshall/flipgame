package com.joemarshall.flipgame;

import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.view.KeyEvent;
import android.view.View;
import android.content.Context;
import oscP5.*;
import netP5.*;
import controlP5.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class FlipGame extends PApplet
{
	SensorReader m_SensorReader;

	ControlP5 m_Cp5;
	boolean m_Recording = false;

	DataSender m_DataSender;
	FileSaver m_FileSaver;

	PFont bigFont, littleFont;
	GameState m_State=GameState.INITIAL_STATE;
	GamePreferences m_Prefs;

	TTS tts;
	
	public void setup()
	{
		String[] soundNames = { "[tick]", "[fast_tick]", "[bang]","[ping]","[levelbreak]" };
		int[] soundIDs = { R.raw.tick,R.raw.fast_tick,R.raw.bang,R.raw.ping,R.raw.levelbreak };
		tts=new TTS(this,soundNames,soundIDs);
		m_Prefs=new GamePreferences(this);
		m_FileSaver = new FileSaver(this);
		m_DataSender = new DataSender(this);
		m_FileSaver.scanMediaFiles();
		m_Cp5 = new ControlP5(this);
		bigFont = createFont("Arial", displayHeight / 20, true); // use
																	// true/false
																	// for
																	// smooth/no-smooth
		littleFont = createFont("Arial", displayHeight / 30, true); // use
																	// true/false
																	// for
																	// smooth/no-smooth
		ControlFont font = new ControlFont(bigFont, displayHeight / 20);
		m_Cp5.setFont(font);
		changeState(GameState.FRONT_SCREEN);


		grabWakeLocks();
		m_SensorReader = new SensorReader(this);

		thread("checkConnectionThread");


	}
	
	public void changeState(GameState newState)
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
		m_State=newState;
		switch(m_State)
		{
		case DEBUG_SCREEN:
			enterDebugScreen();
			break;
		case FRONT_SCREEN:
			enterFrontScreen();
			break;
		case IN_GAME:
			enterGameScreen();
			break;
		case PREFS_SCREEN:
			enterPrefsScreen();
			break;
		}
	}


	private void enterPrefsScreen()
	{
		// TODO Auto-generated method stub
		
	}
	private void leavePrefsScreen()
	{
		// TODO Auto-generated method stub
		
	}
	
	boolean m_InGame=false;

	private void enterGameScreen()
	{
		m_InGame=true;
		// start game thread
		thread("inGameThread");
		
	}

	private void leaveGameScreen()
	{
		
	}

	private void drawGameScreen()
	{
		background(0);
		fill(255);
		textFont(bigFont);
		if(m_SensedMove!=null)
		{
			text("Act:" + m_SensedMove.m_Name, 0, 0f);
			text(m_GameDebugText,0,0.5f*height);
		}
		if(!m_InGame)
		{
			changeState(GameState.FRONT_SCREEN);
		}
	}
	
	String m_GameDebugText="";
	
	// all game logic happens in this thread
	public void inGameThread()
	{
		int gameScore=0;
		long startTime=System.currentTimeMillis();
		
		m_Prefs.m_TimeMultiplyPerLevel=0.8f;
		m_Prefs.savePrefs();
		pauseGame(1000);
		tts.say("Get ready to start");
		pauseGame(5000);
		Random randGen = new Random(); 
		boolean alive=true;
		int level=1;
		float timePerMove=m_Prefs.m_Level1TimePerMove;
		int movesLeft=m_Prefs.m_MovesPerLevel;
		GameMovement lastMove=m_SensedMove;
		while(alive)
		{
			if(movesLeft<=0)
			{
				// next level
				level+=1;
				timePerMove*=m_Prefs.m_TimeMultiplyPerLevel;
				movesLeft=m_Prefs.m_MovesPerLevel;
				// say level BLAH
				tts.say("Level complete, rest [levelbreak]");
				tts.waitForSpeech();
				tts.say("Get ready for level "+level);
				// pause a bit so it can say it
				tts.waitForSpeech();
				lastMove=m_SensedMove;
				pauseGame(1000);
			}
			// choose a move
			GameMovement thisMove=GameMovement.values()[randGen.nextInt(GameMovement.values().length)];
			while(thisMove==lastMove)
			{
				thisMove=GameMovement.values()[randGen.nextInt(GameMovement.values().length)];
			}			
			lastMove=thisMove;
			// say the name
			tts.say(thisMove.m_Name);
			tts.waitForSpeech();
//			pauseGame(1000);
			
			long moveTime=(long)(timePerMove*m_Prefs.m_MoveTimeMultipliers.get(thisMove)*1000f);
			// countdown
			long targetTime=System.currentTimeMillis()+moveTime;
			boolean hitMove=false;									

			long buzzStep=moveTime/10;
			long nextBuzz=0;
			long curTime=System.currentTimeMillis();
			while(curTime<targetTime && !hitMove)
			{
				m_GameDebugText="Score:"+gameScore+"\nMoves left:"+movesLeft+"\nLevel:"+level+"\nTarget:"+thisMove.m_Name+"\nactual:"+m_SensedMove.m_Name;
				if(curTime>=nextBuzz || nextBuzz==0)
				{
					println(curTime-startTime);
					if(nextBuzz==0)
					{
						nextBuzz=curTime;
					}
					if(nextBuzz>=targetTime-2000)
					{
						tts.say("[fast_tick]");
					}else
					{
						tts.say("[tick]");						
					}
					nextBuzz+=buzzStep;
				}
				if(m_SensedMove==thisMove)
				{
					hitMove=true;
					gameScore+=(int)(1000.0f*(float)(targetTime-curTime)/ (float)moveTime);
				}
				pauseGame(10);	
				curTime=System.currentTimeMillis();
			}
			// Hit or missed
			if(hitMove)
			{
				tts.say("[ping]");
			}else
			{
				tts.say("[bang], GAME OVER. Score "+gameScore);
				alive=false;
			}
			movesLeft-=1;
		}
		m_InGame=false;
	}
	
	void pauseGame(long ms)
	{
		try
		{
			Thread.sleep(ms);
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	Button m_NewGameButton;
	Button m_DebugScreenButton;

	
	private void enterFrontScreen()
	{
		m_NewGameButton = m_Cp5.addButton("createNewGame")
				.setPosition(0, (height) / 8).setSize(width, height / 5)
				.setCaptionLabel("Play");
		m_NewGameButton.getCaptionLabel().align(ControlP5.CENTER,
				ControlP5.CENTER);

		m_DebugScreenButton = m_Cp5.addButton("showDebugScreen")
				.setPosition(0, 3 * height / 4).setSize(width, height / 5)
				.setCaptionLabel("Debug");
		m_DebugScreenButton.getCaptionLabel().align(ControlP5.CENTER,
				ControlP5.CENTER);
		
	}

	private void leaveFrontScreen()
	{
		m_Cp5.remove("createNewGame");
		m_Cp5.remove("showDebugScreen");
	}

	private void drawFrontScreen()
	{
		// do nothing - we only have Control P5 controls on this screen
		
	}
	
	public void showDebugScreen()
	{
		changeState(GameState.DEBUG_SCREEN);
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
		m_RecordButton=null;
		m_SendButton=null;
	}

	public void drawDebugScreen()
	{
		background(0);
		int step = height / (m_Values.length + 4);
		int curY=0;
		fill(255);
		textFont(bigFont);
		for (int c = 0; c < m_Values.length; c++)
		{
			curY+=step;
			text((c + 1) + ":" + nf(m_Values[c], 2, 5), 0, curY);
		}
		curY+=step;
		if(m_SensedMove!=null)
		{
			text("Act:" + m_SensedMove, 0, curY);
		}			
		curY+=step;
		text(m_GameDebugText,0,curY);

		textFont(littleFont);
		String name = m_FileSaver.logFileName();
		if (name.length() > 40)
		{
			name = name.substring(name.length() - 40);
		}
		curY+=step;
		text("Rec:" + name, 0, curY);
	}
	
	
	float[] m_Values = new float[3];
	GameMovement m_SensedMove = null;

	public void draw()
	{
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
			break;
		default:
			break;
		
		}
	}
	


	public void onSensorEvent(int sensorType, float x, float y, float z,
			long timestamp, int accuracy)
	{
		if (m_Recording)
		{
			m_FileSaver.writeSensorData(sensorType, x, y, z, timestamp);

			if (m_FileSaver.logFileName() == null)
			{
				m_Recording = false;
				if(m_RecordButton!=null)
				{
					m_RecordButton.setColorBackground(0xff7f7f7f);
				}
			}
		}
		if (m_DataSender.isConnected())
		{
			if(m_SendButton!=null)
			{
				m_SendButton.setColorBackground(0xff00ff00);
			}
			m_DataSender.sendSensorData(sensorType, x, y, z, timestamp);
		} else
		{
			if(m_SendButton!=null)
			{
				m_SendButton.setColorBackground(0xff101010);
			}
		}
		switch (sensorType)
		{
		case Sensor.TYPE_ROTATION_VECTOR:
			m_Values[0] = x;
			m_Values[1] = y;
			m_Values[2] = z;
			updateOrientation(m_Values[1], m_Values[2]);
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

	
	private void updateOrientation(float pitch, float roll)
	{
		// pitch can go from -0.5*PI to +0.5*PI
		int pitchOri = 0;
		if (pitch < -0.25 * PI)
		{
			pitchOri = -1;
		} else if (pitch > 0.25 * PI)
		{
			pitchOri = 1;
		} else
		{
			pitchOri = 0;
		}
		// roll can go from -PI to +PI
		int rollOri = 0;
		if (roll < -0.25 * PI)
		{
			if (roll < -0.75 * PI)
			{
				rollOri = 2;
			} else
			{
				rollOri = -1;
			}
		} else if (roll > 0.25 * PI)
		{
			if (roll < 0.75 * PI)
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

		for(GameMovement g:GameMovement.values())
		{
			if((rollOri==g.m_RollOri || -5==g.m_RollOri) && pitchOri==g.m_PitchOri)
			{
				m_SensedMove=g;
			}
		}
		if(m_State==GameState.DEBUG_SCREEN)
		{
			m_GameDebugText="P:"+pitchOri+"R:"+rollOri;
		}
		
	}

	WakeLock m_WakeLock;
	WifiLock m_WifiLock;

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
		if (!m_Recording)
		{
			m_FileSaver.openLogFile();
			if (m_FileSaver.logFileName() != null)
			{
				m_Recording = true;
				m_RecordButton.setColorBackground(0xffff0000);
			}
		}
	}

	public void send(int value)
	{
		println("Start sending data");
		m_DataSender.manualConnect();
	}

	public boolean dispatchKeyEvent(android.view.KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			if (event.getAction() == KeyEvent.ACTION_DOWN && !handleBackKey())
			{
				return true;
//				return super.dispatchKeyEvent(event);
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
		// return true if you want it to not quit
		m_SensorReader.stop();
		print("Stopped sensors");
		m_DataSender.stop();
		m_FileSaver.closeLogFile();
		println("closing log");
		final Handler h=new Handler();
		h.postDelayed(new Runnable(){

			@Override
			public void run()
			{
				if(m_FileSaver.isClosed())
				{
					PApplet.println("logfile closed successfully");
					finish();
				}else
				{
					PApplet.println("Waiting for logfile close");
					h.postDelayed(this, 100);
				}
			}}, 100);
		return true;
	}

	public void onDestroy()
	{
	}
	
	// if keyPressed worked reliably on android this would be great, but as it
	// is, I use the above code instead
	public void keyPressed() { 
		println("Key:"+keyCode); 
	
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

}
