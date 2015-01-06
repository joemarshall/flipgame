package com.joemarshall.flipgame;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.widget.EditText;
import android.content.Context;
import android.content.DialogInterface;
import processing.core.PApplet;
import oscP5.*;
import netP5.*;

import java.net.InetAddress;
import java.io.IOException;

import android.net.wifi.WifiManager;
import android.net.DhcpInfo;

import java.net.SocketException;

class DataSender
{
	PApplet m_Parent;

	volatile boolean m_EndConnectionThread = false;
	volatile boolean m_EndedConnectionThread = false;

	OscP5 m_TcpOscClient;
	long m_FirstSentTimestamp = -123456;
	int CONNECTION_PORT = 12457;
	String connectAddress = null;

	public DataSender(PApplet parent)
	{
		m_Parent = parent;
	}

	boolean connectDataSender(String address, int port)
	{
		m_FirstSentTimestamp = -123456;
		PApplet.println("Connect:" + address + ":" + port);
		m_TcpOscClient = new OscP5(this, address, port, OscP5.TCP);
		try
		{
			if (m_TcpOscClient.tcpClient() != null
					&& m_TcpOscClient.tcpClient().socket() != null)
			{
				m_TcpOscClient.tcpClient().socket().setTcpNoDelay(true);
			}
			connectAddress = address;
		} catch (SocketException e)
		{
			PApplet.println("Couldn't set nodelay:" + e);
			return false;
		}
		return isConnected();
	}

	public boolean isConnected()
	{
		if (m_TcpOscClient != null && m_TcpOscClient.tcpClient() != null
				&& m_TcpOscClient.tcpClient().socket() != null
				&& m_TcpOscClient.tcpClient().socket().isConnected())
		{
			// println("Connected");
			return true;
		} else
		{
			// println("Not connected");
			return false;
		}
	}

	OscMessage m = new OscMessage("/sensor/");

	public void sendSensorData(int sensor, float x, float y, float z,
			long timestamp)
	{
		if (m_FirstSentTimestamp == -123456)
		{
			m_FirstSentTimestamp = timestamp;
		}
		timestamp -= m_FirstSentTimestamp;

		if (m_TcpOscClient != null)
		{
			m.clearArguments();
			m.add(sensor);
			m.add((float) (((double) timestamp) / 1000000000.0));
			m.add(x);
			m.add(y);
			m.add(z);
			m_TcpOscClient.send(m);
			/*
			 * new AsyncTask<Void, Void, Void>() { protected Void
			 * doInBackground(Void... params) { return null; } } .execute();
			 */
		}
	}

	InetAddress getWIFIAddress()
	{
		try
		{
			WifiManager wifi = (WifiManager) (m_Parent
					.getSystemService(Context.WIFI_SERVICE));
			DhcpInfo dhcp = wifi.getDhcpInfo();
			int ourAddress = dhcp.ipAddress;
			byte[] quads = new byte[4];
			for (int k = 0; k < 4; k++)
				quads[k] = (byte) ((ourAddress >>> (k * 8)) & 0xFF);
			return InetAddress.getByAddress(quads);
		} catch (IOException e)
		{
			return null;
		}
	}

	void showManualConnectDialog()
	{

		String currentAddress = getWIFIAddress().getHostAddress();
		AlertDialog.Builder alert = new AlertDialog.Builder(m_Parent);

		alert.setTitle("Title");
		alert.setMessage("Message");

		// Set an EditText view to get user input
		final EditText input = new EditText(m_Parent);
		alert.setView(input);
		int dotIndex = currentAddress.lastIndexOf(".");
		if (dotIndex != -1)
		{
			currentAddress = currentAddress.substring(0, dotIndex + 1);
		}
		input.setText(currentAddress);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				final String target = input.getText().toString();
				// can't do network stuff on main thread
				new AsyncTask<Void, Void, Void>()
				{
					protected Void doInBackground(Void... params)
					{
						connectDataSender(target, CONNECTION_PORT);
						return null;
					}
				}.execute();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				// Canceled.
			}
		});

		alert.show();
	}

	void manualConnect()
	{
		m_Parent.runOnUiThread(new Runnable()
		{
			public void run()
			{
				showManualConnectDialog();
			}
		});
	}

	void checkConnection()
	{
		while (!m_EndConnectionThread)
		{
			m_Parent.delay(1000);
			if (connectAddress != null)
			{
				if (!isConnected())
				{
					connectDataSender(connectAddress, CONNECTION_PORT);
					PApplet.println("try reconnect");
				}
			}
		}
		m_EndedConnectionThread=true;
	}

	public void stop()
	{
		m_EndConnectionThread=true;
		while(!m_EndedConnectionThread)
		{
			try
			{
				Thread.sleep(10);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		PApplet.println("Stopped datasender");
	}
}
