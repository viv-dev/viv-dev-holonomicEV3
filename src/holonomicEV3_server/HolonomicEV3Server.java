package holonomicEV3_server;

import java.io.*;
import java.net.*;
import lejos.hardware.*;
import lejos.hardware.motor.*;
import lejos.hardware.port.*;
import lejos.hardware.sensor.MindsensorsAbsoluteIMU;
import lejos.robotics.navigation.OmniPilot;
import lejos.robotics.GyroscopeAdapter;
import lejos.robotics.SampleProvider;
import lejos.hardware.ev3.LocalEV3;

@SuppressWarnings("deprecation")
public class HolonomicEV3Server
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
	
	private Socket client;
	private static boolean running = true;
	private ServerSocket server;

	private OmniPilot pilot;
	static Power battery = LocalEV3.get().getPower();
	InputStream in;
	
	public static void main(String[] args) throws IOException
	{
		new HolonomicEV3Server();
	}
	
	public HolonomicEV3Server() throws IOException
	{
		server = new ServerSocket(PORT);
		Button.ESCAPE.addKeyListener(new EscapeListener());

		while (running)
		{
			pilot = new OmniPilot(16.0f, 5.0f, Motor.B, Motor.A, Motor.C, true, true, battery);
			pilot.setLinearSpeed(80.0);
			System.out.println("Awaiting client..");
			this.client = server.accept();
			this.run();
		}
	}
	
	public void run()
	{
		System.out.println("CLIENT CONNECT");
		try
		{
			in = client.getInputStream();
			DataInputStream dIn = new DataInputStream(in);
			while (client != null)
			{
				String receivedNumString = dIn.readUTF();
				String[] numstrings = receivedNumString.split("@@");
				int[] myMessageArray = new int[numstrings.length];
				int indx = 0;

				for (String numStr : numstrings)
				{
					myMessageArray[indx++] = Integer.parseInt(numStr);
				}

				if (myMessageArray[0] == CLOSE)
				{
					// close command
					client.close();
					client = null;
				}
				else if (myMessageArray[0] == 2)
				{
					carAction(myMessageArray[1]);
				}
				else if (myMessageArray[0] == 1)
				{
					carAction(myMessageArray[1], myMessageArray[2], myMessageArray[3]);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void carAction(int command)
	{
		switch (command)
		{
			case FORWARD:
				pilot.travel(20.0, 0.0);
				break;
			case BACKWARD:
				pilot.travel(20, 180.0);
				break;
			case RIGHT:
				pilot.travel(20, 90.0);
				break;
			case LEFT:
				pilot.travel(20, 270.0);
				break;
			case ROT_LEFT:
				pilot.rotate(-30.0);
				break;
			case ROT_RIGHT:
				pilot.rotate(30.0);
				break;
		}
	}
	
	// driving car with spaceNavigator
	@SuppressWarnings("deprecation")
	public void carAction(int linearSpeed, int angle, int angularSpeed)
	{
		if(angularSpeed == 0)
			pilot.moveStraight(linearSpeed, (int) angle);
		else
			pilot.spinningMove(linearSpeed, angularSpeed, angle);
		
//		pilot.spinningMove(0, angularSpeed, 0);
//		pilot.moveStraight(linearSpeed, (int) angle);
		
		// The spinning move method is more sophisticated but difficult to control
		//pilot.spinningMove(linearSpeed, angularSpeed, angle);
	}
	
	public class EscapeListener implements KeyListener
	{

		public void keyPressed(Key k)
		{
			running = false;
			System.out.println("Exiting...");
			System.exit(0);
		}

		public void keyReleased(Key k)
		{
		}
	}
}
