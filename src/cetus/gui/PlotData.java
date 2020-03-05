/**
 * 3/19/13 Zhengxiao (Tony) Li
 *  - Graph class: PlotData.java
  *  Close behavior: JFrame.DISPOSE_ON_CLOSE
  *  Constructor to draw graph, no method needed
  *  All fonts and bar sizes automatically adjusted when graph resized
 */

package cetus.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class PlotData extends JPanel {

	int[] xData;
	double[] yData;
	String xLabel;
	String yLabel;
	String title;

	public PlotData(int[] xData, double[] yData, String xLabel,
			String yLabel, String title) {
		this.xData = xData;
		this.yData = yData;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		this.title = title;

		JFrame f = new JFrame(title);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);// EXIT_ON_CLOSE
		f.getContentPane().add(this);
		f.setSize(400, 400);
		
		//random location
		int randLocationX = (int) (Math.random()*200);
		int randLocationY = (int) (Math.random()*200);
		f.setLocation(randLocationX, randLocationY);
		f.setVisible(true);

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		java.awt.Rectangle rect = g.getClipBounds();
		int width = rect.width;
		int height = rect.height;

		// title

		int leftMargin = width / 8, topMargin = height / 8;
		int rightMargin = width / 8, bottomMargin = height / 6;
		int topStringSpace = height / 16;
		// int stringSpace = 20;
		int chartWidth = width - leftMargin - rightMargin;
		int chartHeight = height - topMargin - bottomMargin;
		int barWidth = chartWidth / xData.length / 2;
		double maxY = yData[0];
		for (int i = 1; i < yData.length; i++) {
			if (maxY < yData[i])
				maxY = yData[i];
		}
		double yScale = (chartHeight - topStringSpace) / maxY;
		int fontSize = width / 22 / ((int)Math.log(xData.length)+1);

		Font font = new Font("Courier New", Font.BOLD, fontSize);
		g.setFont(font);
		for (int i = 0; i < xData.length; i++) {
			int barHeight = (int) (yData[i] * yScale);
			g.setColor(Color.BLUE);
			g.fillRect(i * barWidth * 2 + leftMargin, 
					chartHeight - barHeight	+ topMargin, 
					barWidth, 
					barHeight);
			g.setColor(Color.RED);
			g.drawString(xData[i]+"", 
					i * barWidth * 2 + leftMargin + barWidth/2, 
					height - bottomMargin 
					+ topStringSpace/((int)Math.log(xData.length)+1));//X
			g.drawString(String.format("%1$,.2f", yData[i]), 
					i * barWidth * 2 + leftMargin, 
					chartHeight - barHeight + topMargin	
					- topStringSpace/5); //Y
		}
		
		fontSize = width / 35;
		font = new Font("Courier New", Font.BOLD, fontSize);
		g.setFont(font);
		g.setColor(Color.BLACK);
		g.drawString("X Axis: " + xLabel, 
				width / 2 - xLabel.length() * width / 100, 
				height - bottomMargin + 2*topStringSpace);
		g.drawString("Y Axis: " + yLabel, 
				topStringSpace, 
				topStringSpace);

		//System.out.println(rect);

	}

//	public static void main(String[] args) {
//		PlotData bc = new PlotData((new int[] { 0, 2, 4 }), (new double[] {
//				8.3, 5.2, 3.1 }), "Number of Threads (0 means Sequential)",
//				"Running time (seconds)", "Running Time");
//
//	}

}
