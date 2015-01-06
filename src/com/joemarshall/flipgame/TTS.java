package com.joemarshall.flipgame;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import processing.core.PApplet;
import android.os.Vibrator;
import android.content.Context;

import java.util.HashMap;

public class TTS implements TextToSpeech.OnInitListener
{
	private TextToSpeech m_TTS;

	int[] m_EarconIDs;
	String[] m_Earcons;
	PApplet m_Parent;

	UtteranceProgressListener m_Listener;
	Vibrator m_Vibrator;

	public TTS(PApplet parent, String[] earcons, int[] resIDs)
	{
		m_Parent = parent;
		m_TTS = new TextToSpeech(parent, this);
		m_EarconIDs = resIDs;
		m_Earcons = earcons;
		m_Vibrator = (Vibrator) parent
				.getSystemService(Context.VIBRATOR_SERVICE);
		m_Listener = new UtteranceProgressListener()
		{
			public void onDone(String utteranceId)
			{
			}

			public void onError(String utteranceId)
			{
			}

			public void onStart(String utteranceId)
			{
				// PApplet.println("Vibrate "+utteranceId);
				// vibrate for 100ms
				m_Vibrator.vibrate(Integer.parseInt(utteranceId));
			}
		};
		m_TTS.setOnUtteranceProgressListener(m_Listener);
	}

	void say(String text)
	{
		PApplet.println("say:" + text);
		if (m_TTS != null)
		{
			int speakMode = TextToSpeech.QUEUE_FLUSH;

			int earconPos = text.indexOf("[");
			while (earconPos != -1)
			{
				int earconEnd = text.indexOf("]", earconPos);
				if (earconEnd != -1)
				{
					String textBeforeEarcon = text.substring(0, earconPos);
					String earcon = text.substring(earconPos, earconEnd + 1);
					String textAfterEarcon = text.substring(earconEnd + 1,
							text.length());
					if (textBeforeEarcon.length() != 0)
					{
						PApplet.println("Before:" + textBeforeEarcon);
						m_TTS.speak(textBeforeEarcon, speakMode, null);
						speakMode = TextToSpeech.QUEUE_ADD;
					}
					if (earcon.indexOf("[vib") == 0)
					{
						// vibration - just add silences to the TTS engine, and
						// the listener will do the actual vibration
						for (int c = 0; c < earcon.length(); c++)
						{
							char thisChar = earcon.charAt(c);
							if (thisChar == '-')
							{
								// a 100ms silence, no vibration
								m_TTS.playSilence(100, speakMode, null);
							} else if (thisChar == '.')
							{
								// 100ms of vibration - the listener handles the
								// vibration
								HashMap<String, String> params = new HashMap<String, String>();
								params.put(
										TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
										"100");
								m_TTS.playSilence(100, speakMode, params);
							} else if (thisChar == '*')
							{
								// 200ms of vibration - the listener handles the
								// vibration
								HashMap<String, String> params = new HashMap<String, String>();
								params.put(
										TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
										"200");
								m_TTS.playSilence(200, speakMode, params);
							}
						}
					} else
					{
						// a named sound (from resources)
						m_TTS.playEarcon(earcon, speakMode, null);
						speakMode = TextToSpeech.QUEUE_ADD;
					}
					text = textAfterEarcon;
					earconPos = text.indexOf("[");
				} else
				{
					break;
				}
			}
			m_TTS.speak(text, speakMode, null);
		}
	}

	public void waitForSpeech()
	{
		while(m_TTS.isSpeaking())
		{
			try
			{
				Thread.sleep(1);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void onInit(int status)
	{

		if (status == TextToSpeech.SUCCESS)
		{
			for (int c = 0; c < m_Earcons.length; c++)
			{
				m_TTS.addEarcon(m_Earcons[c], m_Parent.getPackageName(),
						m_EarconIDs[c]);
			}

		} else
		{
			PApplet.println("TTS Initilization Failed!");
			m_TTS.stop();
			m_TTS.shutdown();
		}
	}

	void destroy()
	{
		if (m_TTS != null)
		{
			m_TTS.stop();
			m_TTS.shutdown();
			m_TTS = null;
		}
	}
}
