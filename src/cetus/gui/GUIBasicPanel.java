package cetus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public abstract class GUIBasicPanel extends JPanel implements ActionListener{
	protected JButton[] buttons;
	protected JTextArea textArea, statusArea;
	protected JLabel label1;
	protected int i, j, numButtons, 
		buttonHeight = CetusGUITools.buttonHeight, 
		buttonWidth = CetusGUITools.buttonWidth, 
		buttonTop = CetusGUITools.buttonTop, 
		buttonGap = CetusGUITools.buttonGap,
		labelHeight = CetusGUITools.labelHeight;
	protected JScrollPane scrollPane;
	public static final String shortDashSeparator = "\n-------------\n";
	
	GUIBasicPanel(String[] guiStringArray){
		
		numButtons = Integer.parseInt(guiStringArray[0]);
		buttons = new JButton[numButtons];
		
		setLayout(new BorderLayout(5, 5));
		label1 = new JLabel(" ");
		label1.setPreferredSize(new Dimension(-1,labelHeight));
		add(label1,BorderLayout.PAGE_START);
		
		for (i=0;i<numButtons;i++){
			buttons[i] = new JButton(guiStringArray[2*i+1]);
			buttons[i].setBounds(i*buttonGap+i*buttonWidth, buttonTop, buttonWidth, buttonHeight);
			buttons[i].setToolTipText(guiStringArray[2*i+2]);
			buttons[i].addActionListener(this);
			add(buttons[i]);	
		}
		
		i = 2*i; //current index in guiStringArray;
		
		textArea = new JTextArea();
		textArea.setLineWrap(Boolean.parseBoolean(guiStringArray[++i]));
		textArea.setWrapStyleWord(Boolean.parseBoolean(guiStringArray[++i]));
		textArea.setEditable(Boolean.parseBoolean(guiStringArray[++i]));
		textArea.setText(guiStringArray[++i]);
		scrollPane = new JScrollPane(textArea);
		add(scrollPane);
		
		statusArea = new JTextArea();
		statusArea.setLineWrap(Boolean.parseBoolean(guiStringArray[++i]));
		statusArea.setWrapStyleWord(Boolean.parseBoolean(guiStringArray[++i]));
		statusArea.setEditable(Boolean.parseBoolean(guiStringArray[++i]));
		statusArea.setBackground(Color.lightGray);
		statusArea.setText(guiStringArray[++i]);
		add(statusArea,BorderLayout.PAGE_END);
	}
	
    public void actionPerformed(ActionEvent evt) {
        Object source = evt.getSource();
        
        for (i=0;i<numButtons;i++){
        	if (source==buttons[i]) {
        		pressButton(i);
        		break;
        	}
        }
        
    }
    
    public abstract void pressButton (int buttonIndex);

}
