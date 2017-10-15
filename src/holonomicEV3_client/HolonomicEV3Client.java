package holonomicEV3_client;


import java.io.*;
import java.net.Socket;

import java.awt.*;
import java.awt.event.*;


public class HolonomicEV3Client extends Frame
{
	public static final int KEYBOARD = 2;
	public static final int JOYSTICK = 3;
	
	public static final int FORWARD = 1;
	public static final int LEFT = 1;
	public static final int BACK = 1;
	public static final int RIGHT = 1;
	public static final int DOWN = 1;
	public static final int ROT_LEFT = 1;
	public static final int ROT_RIGHT = 1;
		
	public static final int CLOSE = 0;
	
	public static final int PORT = 7360;
	
	public static void main(String[] args)
	{
		HolonomicEV3Client client = new HolonomicEV3Client();
	}
	
	// UI Elements
	private Button connectButton;
	private TextField txtIPAddress;
	private TextArea statusMessage;
	
	// Message sending
	private Socket socket;
	private DataOutputStream outStream;
	
	public HolonomicEV3Client()
	{
		super("HolonomicEV3Client");
		this.setSize(400, 300);
		
		this.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Ending Robot Client");
				disconnect();
				System.exit(0);
			}
		});
		
		buildGui("127.0.0.1");
		
		this.setVisible(true);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		
		this.addKeyListener(new KeyboardListener());
	}
	
	public void buildGui(String ip)
	{
		Panel mainPanel = new Panel(new BorderLayout());
		ControlListener controlListener = new ControlListener();

		connectButton = new Button("Connect");
		connectButton.addActionListener(controlListener);

		txtIPAddress = new TextField(ip, 16);

		statusMessage = new TextArea("STATUS: DISCONNECTED");
		statusMessage.setEditable(false);

		Panel north = new Panel(new FlowLayout(FlowLayout.LEFT));
		north.add(connectButton);
		north.add(txtIPAddress);

		Panel center = new Panel(new GridLayout(5, 1));
		center.add(new Label("A-S-D to steer, W-X to move"));

		Panel center4 = new Panel(new FlowLayout(FlowLayout.LEFT));
		center4.add(statusMessage);
		
		center.add(center4);

		mainPanel.add(north, "North");
		mainPanel.add(center, "Center");
		this.add(mainPanel);
	}
	
	
	
	
	private void connect()
	{
		try
		{
			statusMessage.setText("STATUS: Attempting to connect...");
			socket = new Socket(txtIPAddress.getText(), PORT);
			outStream = new DataOutputStream(socket.getOutputStream());
			statusMessage.setText("STATUS: CONNECTED");
			connectButton.setLabel("Disconnect");
			//looping = true;
		}
		catch (Exception exc)
		{
			String statusMsg = "STATUS: FAILURE - Error establishing connection with server.\n";
			statusMessage.setText(statusMsg);
			System.out.println("Error: " + exc);
		}	
	}
	
	private void disconnect()
	{
		try
		{
			//sendCommand(CLOSE, 0, 0, 0);
			socket.close();
			connectButton.setLabel("Connect");
			statusMessage.setText("STATUS: DISCONNECTED");
		}
		catch (Exception exc)
		{
			statusMessage.setText("STATUS: FAILURE Error closing connection with server.");
			System.out.println("Error: " + exc);
		}	
	}
	
	private class KeyboardListener implements KeyListener
	{
		@Override
		public void keyPressed(KeyEvent e)
		{
			switch(e.getKeyCode())
			{
				default:
					System.out.println(e.getKeyChar());
			}
			
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyTyped(KeyEvent e)
		{
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/** Listens for click events on the connect/disconnect button */
	private class ControlListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			String command = e.getActionCommand();
			if (command.equals("Connect"))
			{
				connect();
			}
			else if (command.equals("Disconnect"))
			{
				disconnect();
			}
		}
	}


}
