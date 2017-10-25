package holonomicEV3_client;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.ArrayList;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class HolonomicEV3Client extends JFrame
{
	public static final int CLOSE = 0;
	public static final int JOYSTICK = 1;
	public static final int KEYBOARD = 2;

	public static final int FORWARD = 10;
	public static final int LEFT = 11;
	public static final int RIGHT = 12;
	public static final int BACKWARD = 13;
	public static final int ROT_LEFT = 14;
	public static final int ROT_RIGHT = 15;

	public static final int PORT = 7360;

	private final double MAX_VALUE = 1.0;
	private final int MAX_SPEED = 32;
	private final int MAX_SPIN = 64;
	private final int SAMPLE_RATE = 200;

	public static void main(String[] args)
	{
		HolonomicEV3Client client = new HolonomicEV3Client();
	}

	// UI Elements
	private JButton connectButton;
	private JTextField txtIPAddress;
	private JTextArea statusMessage;
	private JComboBox inputChoice;

	// Message sending
	private Socket socket;
	private DataOutputStream outStream;
	private boolean connected;

	private Controller currentController;
	private int currentControllerIndex;
	private int prevControllerIndex;

	// Input handling
	private ArrayList<Controller> foundControllers;
	Map<String, Double> inputs = new HashMap<String, Double>();

	public HolonomicEV3Client()
	{
		super("HolonomicEV3Client");

		buildGUI("10.0.1.1");

		currentController = null;
		currentControllerIndex = 0;
		prevControllerIndex = 0;

		foundControllers = new ArrayList<>();
		searchForControllers();

		initInputMap();
		processInputs();

	}
	
	private void initInputMap()
	{
		inputs.put("X Axis", 0.0);
		inputs.put("Y Axis", 0.0);
		inputs.put("Z Axis", 0.0);
		inputs.put("X Rotation", 0.0);
		inputs.put("Y Rotation", 0.0);
		inputs.put("Z Rotation", 0.0);	
	}

	public void buildGUI(String ip)
	{
		this.setSize(475, 225);

		this.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Ending EV3 Client");
				disconnect();
				System.exit(0);
			}
		});

		JPanel mainPanel = new JPanel(new BorderLayout());
		ControlListener controlListener = new ControlListener();

		connectButton = new JButton("Connect");
		connectButton.addActionListener(controlListener);
		connectButton.addKeyListener(new KeyboardListener());

		txtIPAddress = new JTextField(ip, 20);

		statusMessage = new JTextArea("STATUS: DISCONNECTED");
		statusMessage.setPreferredSize(new Dimension(450, 100));
		statusMessage.setEditable(false);

		inputChoice = new JComboBox();
		inputChoice.addItem("Keyboard");

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
		north.add(connectButton);
		north.add(txtIPAddress);

		JPanel center = new JPanel(new BorderLayout(2, 1));

		JPanel choiceList = new JPanel(new FlowLayout(FlowLayout.LEFT));
		choiceList.add(new Label("Pick input source:"));
		choiceList.add(inputChoice);

		JPanel message = new JPanel(new FlowLayout(FlowLayout.LEFT));
		message.add(statusMessage);

		center.add(choiceList, "North");
		center.add(message, "Center");

		mainPanel.add(north, "North");
		mainPanel.add(center, "Center");

		this.add(mainPanel);

		this.setVisible(true);
		this.setResizable(true);
		this.setLocationRelativeTo(null);

	}

	private void processInputs()
	{
		while (true)
		{
			updateCurrentController();

			while (connected)
			{
				updateCurrentController();
				if (currentController != null)
				{
					updateFromController();
				} // Else use key listener commands

				try
				{
					Thread.sleep(SAMPLE_RATE);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private void updateFromController()
	{
		if (!currentController.poll())
		{
			currentController = null;
			displayDisconnected();
			return;
		}

		Component[] components = currentController.getComponents();

		for (Component component : components)
		{
			double temp = (double)component.getPollData();
			
			// Apply dead zone to noisy center of controller
			if(temp > 0.0 && temp < 0.3)
				temp = 0.0;
				
			if(temp < 0.0 && temp > - 0.3)
				temp = 0.0;
						
			switch (component.getName())
			{
				case "X Axis":
					inputs.put("X Axis", temp);
					break;
				case "Y Axis":
					inputs.put("Y Axis", temp);
					break;
				case "Z Axis":
					inputs.put("Z Axis", temp);
					break;
				case "X Rotation":
					inputs.put("X Rotation", temp);
					break;
				case "Y Rotation":
					inputs.put("Y Rotation", temp);
					break;
				case "Z Rotation":
					inputs.put("Z Rotation", temp);
					break;
				default:
					break;
			}
		}

		//This needs to be configured per controller type...
		double x = inputs.get("X Axis") * MAX_VALUE;
		double y = inputs.get("Y Axis") * MAX_VALUE;
		double spinning = -(inputs.get("X Rotation") * MAX_VALUE * MAX_SPIN);
		double speed = Math.sqrt(x * x + y * y) * MAX_SPEED;
		
		double theta = Math.atan2(y, x); // direction
		double angle = Math.toDegrees(theta) + 90;

		if (angle < 0.0d)
			angle += 360.0;
		
		if(angle >= 360.0)
			angle -= 360.0;
		
		if (speed > MAX_SPEED)
		{
			speed = MAX_SPEED;
		}
		
		if (spinning > MAX_SPIN)
		{
			spinning = MAX_SPIN;
		}
		;
		if (spinning < -MAX_SPIN)
		{
			spinning = -MAX_SPIN;
		}
		sendCommand(JOYSTICK, (int) speed, (int) angle, (int) spinning);
	}

	private void sendCommand(int source, int angle, int r, int rotate)
	{
		if(connected) 
		{
			// Send coordinates to Server:
			statusMessage.setText("status: SENDING command.");
			String message = source + "@@" + angle + "@@" + r + "@@" + rotate;
			try
			{
				outStream.writeUTF(message);
			}
			catch (IOException e)
			{
				statusMessage.setText("status: ERROR Problems occurred sending data.");
			}
			statusMessage.setText("status: Command SENT.");
		}
	}

	private void searchForControllers()
	{
		Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

		for (int i = 0; i < controllers.length; i++)
		{
			Controller controller = controllers[i];

			if (controller.getType() == Controller.Type.GAMEPAD)
			{
				// Add new controller to the list of all controllers.
				foundControllers.add(controller);

				// Add new controller to the list on the window.
				addControllerName(controller.getName() + " - " + controller.getType().toString() + " type");
			}
		}

	}

	private void addControllerName(String controllerName)
	{
		inputChoice.addItem(controllerName);
	}

	private void updateCurrentController()
	{
		prevControllerIndex = currentControllerIndex;
		currentControllerIndex = getSelectedController();

		if (currentControllerIndex != prevControllerIndex)
		{
			if (currentControllerIndex == 0)
			{
				currentController = null;
			}
			else if (currentControllerIndex > 0 && ((currentControllerIndex-1) < foundControllers.size()))
			{
				currentController = foundControllers.get(currentControllerIndex-1);
				System.out.println("Reading input from: " + currentController.getName());
			}
			else
			{
				System.out.println("Invalid controller index!");
			}
		}

	}

	private int getSelectedController()
	{
		return inputChoice.getSelectedIndex();
	}

	public void displayDisconnected()
	{
		inputChoice.removeAllItems();
		inputChoice.addItem("Controller disconnected!");
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
			connected = true;
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
			sendCommand(CLOSE, 0, 0, 0);
			socket.close();
			connectButton.setLabel("Connect");
			statusMessage.setText("STATUS: DISCONNECTED");
			connected = false;
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
			if (currentController == null)
			{
				switch (e.getKeyCode())
				{
					case 87: // W
						sendCommand(KEYBOARD, FORWARD, 0, 0);
						System.out.println("Pressed " + e.getKeyChar());
						break;
					case 65: // A
						sendCommand(KEYBOARD, LEFT, 0, 0);
						System.out.println("Pressed " + e.getKeyChar());
						break;
					case 83: // S
						sendCommand(KEYBOARD, BACKWARD, 0, 0);
						System.out.println("Pressed " + e.getKeyChar());
						break;
					case 68: // D
						sendCommand(KEYBOARD, RIGHT, 0, 0);
						System.out.println("Pressed " + e.getKeyChar());
						break;
					case 81: // Q
						sendCommand(KEYBOARD, ROT_LEFT, 0, 0);
						System.out.println("Pressed " + e.getKeyChar());
						break;
					case 69: // E
						sendCommand(KEYBOARD, ROT_RIGHT, 0, 0);
						System.out.println("Pressed " + e.getKeyChar());
						break;
					default:
						System.out.println(e.getKeyChar());
				}
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
