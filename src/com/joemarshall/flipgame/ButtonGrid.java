package com.joemarshall.flipgame;

import controlP5.*;
import processing.core.PApplet;

class ButtonGrid
{
	ControlP5 m_CP5;
	Toggle[] m_Toggles;
	Textlabel[] m_RowLabels;
	int m_Rows, m_Cols;
	int m_X, m_Y, m_Width, m_Height;

	ButtonGrid(ControlP5 cp5, int rows, int cols, int x, int y, int width,
			int height)
	{
		m_CP5 = cp5;
		m_Toggles = new Toggle[rows * cols];
		m_RowLabels = new Textlabel[rows];
		m_Rows = rows;
		m_Cols = cols;
		m_X = x;
		m_Y = y;
		m_Width = width;
		m_Height = height;
	}

	boolean inGridClicked = false;

	void gridClicked(int row, int col)
	{
		if (!inGridClicked)
		{
			inGridClicked = true;
			PApplet.println("click: " + row + "," + col);
			for (int c = 0; c < 4; c++)
			{
				// if(m_Toggles[c+row*m_Cols].getState()!=(c==col))
				// {
				m_Toggles[c + row * m_Cols].setState(c == col);
				// }
			}
			inGridClicked = false;
		}
	}

	void setValue(int row, int col)
	{
		m_Toggles[row * m_Cols + col].setState(true);
	}
	
	int getValue(int row)
	{
		for(int c=0;c<m_Cols;c++)
		{
			if(m_Toggles[row*m_Cols+c].getState())
			{
				return c;
			}
		}
		return -1;
	}

	void destroy()
	{
		for (Toggle t : m_Toggles)
		{
			m_CP5.remove(t.getName());
		}
		for (Textlabel t : m_RowLabels)
		{
			m_CP5.remove(t.getName());
		}

	}

	void makeCol(int col, String txt)
	{
		for (int row = 0; row < m_Rows; row++)
		{
			makeToggle(row, col, txt);
		}
	}

	void setRowLabel(int row, String label)
	{
		Textlabel t = m_CP5.addTextlabel(label + row)
				.setPosition(m_X, m_Y + (m_Height / m_Rows) * row)
				.setSize(m_Width / (m_Cols + 1), m_Height / m_Rows)
				.setText(label);
		t.align(PApplet.CENTER, PApplet.CENTER, PApplet.CENTER, PApplet.CENTER);

		m_RowLabels[row] = t;
	}

	void makeToggle(final int row, final int col, final String txt)
	{
		Toggle t = m_CP5
				.addToggle("Toggle" + row + "," + col)
				.setPosition(m_X + (m_Width / (m_Cols + 1)) * (col + 1),
						m_Y + (m_Height / m_Rows) * row)
				.setSize(m_Width / (m_Cols + 1), m_Height / m_Rows)
				.setLabel(txt);
		t.getCaptionLabel().alignX(PApplet.CENTER);
		t.getCaptionLabel().alignY(PApplet.CENTER);
		t.addListener(new ControlListener()
		{
			public void controlEvent(ControlEvent theEvent)
			{
				gridClicked(row, col);
			}
		});
		m_Toggles[row * m_Cols + col] = t;
	}
}
