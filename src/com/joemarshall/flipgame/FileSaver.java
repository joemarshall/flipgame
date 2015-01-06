package com.joemarshall.flipgame;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;

import android.text.format.Time;
import processing.core.PApplet;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;

public class FileSaver
{
	PApplet m_Parent;

	BufferedWriter m_LogFile = null;
	File m_LogFilePath = null;
	long m_FirstTimestamp = -123456;

	public FileSaver(PApplet parent)
	{
		m_Parent = parent;
	}

	public void openLogFile()
	{
		try
		{
			Time time = new Time();
			time.setToNow();

			m_LogFilePath = new File(m_Parent.getExternalFilesDir(null),
					time.format("sensors-%Y%m%d-%H%M%S.sense"));
			m_LogFile = new BufferedWriter(new FileWriter(m_LogFilePath));
			m_LogFile.write("sensor,timestamp,x,y,z\n");
		} catch (IOException e)
		{
			m_LogFilePath = null;
			m_LogFile = null;
		}
	}

	String logFileName()
	{
		if (m_LogFilePath == null)
		{
			return "";
		} else
		{
			return m_LogFilePath.toString();
		}
	}

	public void writeSensorData(int sensor, float x, float y, float z,
			long timestamp)
	{
		if (m_LogFile != null)
		{
			if (m_FirstTimestamp == -123456)
			{
				m_FirstTimestamp = timestamp;
			}
			timestamp -= m_FirstTimestamp;
			try
			{
				m_LogFile.write(String.format("%d,%f,%f,%f,%f\n", sensor,
						((double) timestamp) / 1000000000.0, x, y, z));
			} catch (IOException e)
			{
				PApplet.println("Error writing");
				m_LogFile = null;
			}
		}
	}

	volatile Boolean m_WaitForScan = false;

	public void closeLogFile()
	{
		if (m_LogFile == null)
			return;
		try
		{
			if (m_LogFile != null)
			{
				BufferedWriter temp = m_LogFile;
				m_LogFile = null;
				temp.close();
			}
		} catch (IOException e)
		{
			PApplet.println("Couldn't close logfile");
		}
		PApplet.println("Closed");

		String[] str = new String[1];
		str[0] = m_LogFilePath.getAbsolutePath();
		m_WaitForScan = true;
		MediaScannerConnection.scanFile(m_Parent.getApplicationContext(), str,
				null, new OnScanCompletedListener()
				{
					@Override
					public void onScanCompleted(String path, Uri uri)
					{
						PApplet.println("MediaScan:" + path
								+ " was scanned seccessfully: " + uri);
						m_WaitForScan = false;
					}
				});
	}

	public boolean isClosed()
	{
		if (m_WaitForScan == false && m_LogFile == null)
		{
			return true;
		}
		return false;
	}

	public void scanMediaFiles()
	{
		File dir = m_Parent.getExternalFilesDir(null);
		PApplet.println(dir);
		File[] allFiles = dir.listFiles(new FileFilter()
		{

			@Override
			public boolean accept(File pathname)
			{
				if (pathname.isFile())
				{
					return true;
				}
				return false;
			}
		});
		if (allFiles != null)
		{
			String[] strs = new String[allFiles.length];
			for (int c = 0; c < allFiles.length; c++)
			{
				strs[c] = allFiles[c].getAbsolutePath();
			}

			MediaScannerConnection.scanFile(m_Parent.getApplicationContext(),
					strs, null, new OnScanCompletedListener()
					{
						@Override
						public void onScanCompleted(String path, Uri uri)
						{
							// println("MediaScan: " + path
							// + " was scanned seccessfully: " + uri);
						}
					});
		}
	}
};
