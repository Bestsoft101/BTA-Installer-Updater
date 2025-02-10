package b100.installer.updater;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class Window {
	
	public JFrame frame;
	public JPanel panel;
	
	private JLabel label;
	private JProgressBar progressBar;
	
	private Listener listener = new Listener();
	
	private GridBagConstraints c = new GridBagConstraints();
	
	private boolean initialized = false;
	private boolean closed = false;
	
	public Window(String text) {
		frame = new JFrame("Updater");
		
		panel = new JPanel();
//		panel.setPreferredSize(new Dimension(256, 96));
		panel.setLayout(new GridBagLayout());
		
		int padding = 8;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(padding, padding, padding, padding);
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		label = new JLabel(text);
		panel.add(label, c);

		progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(240, 16));
		padding = 8;
		c.insets = new Insets(0, padding, padding, padding);
		c.weighty = 0.0;
		c.gridy = 1;
		panel.add(progressBar, c);
		
		if(initialized) {
			panel.validate();
			frame.validate();
		}
		
		frame.add(panel);
		frame.pack();
		frame.setMinimumSize(new Dimension(frame.getWidth(), frame.getHeight()));
		frame.setLocationRelativeTo(null);
		
		// Attempting to close the window will call the windowClosing() method,
		// which lets the updater know to stop running.
		// After the updater is done, the window will be disposed.
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(listener);
		
		frame.setVisible(true);
		
		initialized = true;
	}
	
	public JProgressBar setText(String text) {
		label.setText(text);
		progressBar.setValue(0);
		return progressBar;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	public void dispose() {
		frame.dispose();
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	private class Listener implements WindowListener {
		
		@Override
		public void windowClosing(WindowEvent e) {
			System.out.println("Window closed!");
			closed = true;
		}

		@Override
		public void windowOpened(WindowEvent e) {}

		@Override
		public void windowClosed(WindowEvent e) {}

		@Override
		public void windowIconified(WindowEvent e) {}

		@Override
		public void windowDeiconified(WindowEvent e) {}

		@Override
		public void windowActivated(WindowEvent e) {}

		@Override
		public void windowDeactivated(WindowEvent e) {}
	}
}
