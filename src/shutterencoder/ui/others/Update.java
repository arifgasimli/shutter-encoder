/*******************************************************************************************
* Copyright (C) 2026 PACIFICO PAUL
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
* 
********************************************************************************************/

package shutterencoder.ui.others;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;


import com.formdev.flatlaf.extras.FlatSVGIcon;

import shutterencoder.ui.main.Shutter;
import shutterencoder.ui.renderers.AntiAliasedRoundRectangle;
import shutterencoder.utils.Utils;

public class Update {

	public static JDialog frame;
	private final static JProgressBar progressBar = new JProgressBar();
	public static JLabel lblNewVersion = new JLabel(Shutter.language.getProperty("lblNewVersion"));
	
	private static JPanel topPanel;
	private static JLabel quit;
	
	private static boolean cancelled = false;
			
	private static int MousePositionY;
	
	public Update() {
		
		frame = new JDialog();
		frame.setFont(new Font(Shutter.boldFont, Font.PLAIN, 12));
		frame.setResizable(false);
		frame.setModal(false);
		frame.setAlwaysOnTop(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(346, 59);
		frame.getContentPane().setBackground(Utils.background);
		frame.setTitle(Shutter.language.getProperty("frameUpdate"));
		frame.setForeground(Color.WHITE);
		frame.getContentPane().setLayout(null);
		
		if (frame.isUndecorated() == false) //Evite un bug lors de la seconde ouverture
		{
			frame.setUndecorated(true);
			frame.setShape(new AntiAliasedRoundRectangle(0, 0, frame.getWidth() + 15, frame.getHeight(), 15, 15));
			
			if (System.getProperty("os.name").contains("Mac") == false)
				frame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("resources/icon.png")).getImage());
		}
		
		content();		
				
		GraphicsConfiguration config = Shutter.frame.getGraphicsConfiguration();
		GraphicsDevice myScreen = config.getDevice();
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] allScreens = env.getScreenDevices();
		int screenIndex = -1;
		for (int i = 0; i < allScreens.length; i++) {
		    if (allScreens[i].equals(myScreen))
		    {
		    	screenIndex = i;
		        break;
		    }
		}
		
		if (System.getProperty("os.name").contains("Windows"))
			frame.setLocation(allScreens[screenIndex].getDefaultConfiguration().getBounds().x + allScreens[screenIndex].getDefaultConfiguration().getBounds().width - frame.getSize().width, allScreens[screenIndex].getDefaultConfiguration().getBounds().height - 99);	
		else
			frame.setLocation(allScreens[screenIndex].getDefaultConfiguration().getBounds().x + allScreens[screenIndex].getDisplayMode().getWidth() - frame.getSize().width, allScreens[screenIndex].getDisplayMode().getHeight() - 99);	
		
		
		frame.setVisible(true);		
		
   	}
	
	private void content() {
		topPanel = new JPanel();		
		topPanel.setLayout(null);
		topPanel.setBackground(Utils.background);
		
		quit = new JLabel(new FlatSVGIcon("resources/quit.svg", 15, 15));
		quit.setHorizontalAlignment(SwingConstants.CENTER);
		quit.setBounds(frame.getSize().width - 20, 3, 15, 15);
		topPanel.add(quit);
		topPanel.setBounds(0, 0, 346, 59);
		
		quit.addMouseListener(new MouseListener(){

			private boolean accept = false;

			@Override
			public void mouseClicked(MouseEvent e) {			
			}

			@Override
			public void mousePressed(MouseEvent e) {		
				
				quit.setIcon(new FlatSVGIcon("resources/quit.svg", 15, 15).setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
				    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
				    float newBrightness = Math.min(1.0f, hsb[2] * 0.9f); 				    
				    return Color.getHSBColor(hsb[0], hsb[1], newBrightness);
				})));
				accept = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {	
				
				if (accept)			
				{
					cancelled = true;
					frame.dispose();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {			
				quit.setIcon(new FlatSVGIcon("resources/quit.svg", 15, 15).setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
				    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
				    float newBrightness = Math.min(1.0f, hsb[2] * 1.1f); 				    
				    return Color.getHSBColor(hsb[0], hsb[1], newBrightness);
				})));
			}

			@Override
			public void mouseExited(MouseEvent e) {		
				quit.setIcon(new FlatSVGIcon("resources/quit.svg", 15, 15));
				accept = false;
			}

						
		});
						
		progressBar.setBounds(6, 29, 334, 23);		
		topPanel.add(progressBar);
		
		lblNewVersion.setFont(new Font(Shutter.boldFont, Font.PLAIN, 12));
		lblNewVersion.setBounds(6, 11, 295, 16);		
		topPanel.add(lblNewVersion);
		
		topPanel.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent down) {
			}

			@Override
			public void mousePressed(MouseEvent down) {	
				MousePositionY = down.getPoint().y;					
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {					
			}

			@Override
			public void mouseExited(MouseEvent e) {				
			}		

		 });
		
		topPanel.addMouseMotionListener(new MouseMotionListener(){

			@Override
			public void mouseDragged(MouseEvent e) {
				frame.setLocation(frame.getLocation().x,MouseInfo.getPointerInfo().getLocation().y - MousePositionY);
				
			}

			@Override
			public void mouseMoved(MouseEvent arg0) {
			}
			
		});
		
		frame.getContentPane().add(topPanel);
	}

	@SuppressWarnings("deprecation")
	public static void HTTPDownload(String address, String destination) {
	
		cancelled = false;
		
		OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
		 try {
	        	URL url = new URL(address.replace(" ", "%20"));
	            out = new BufferedOutputStream(new FileOutputStream(destination));
	            conn = url.openConnection();
	            in = conn.getInputStream();
	            byte[] buffer = new byte[1024];
	            
	            progressBar.setMaximum(100);
	            
	            int numRead;
	            long numWritten = 0;
	            long fileSize = conn.getContentLength();

	            while ((numRead = in.read(buffer)) != -1)
	            {
	                out.write(buffer, 0, numRead);
	                numWritten += numRead;
	               
	                progressBar.setValue((int) ((float) ((float) numWritten * 100) / fileSize));
	                
	                if (cancelled)
	                	break;
            	}
		 	}
            catch (Exception exception)
		 	{ 
                exception.printStackTrace();
                
				JOptionPane.showMessageDialog(Shutter.frame, Shutter.language.getProperty("downloadFailed"), Shutter.language.getProperty("downloadError"), JOptionPane.ERROR_MESSAGE);	 
				
				try {
                    if (in != null)
                        in.close();
                    if (out != null)
                        out.close();
            	} catch (IOException io) {}
				
				try {
					File toDelete = new File(destination);
					toDelete.delete();
				} catch (Exception e) {}					
                         
            } 
		 	finally {
		 		
		 		try {
                    if (in != null)
                        in.close();
                    if (out != null)
                        out.close();
            	} catch (IOException io) {}
		 		
		 		if (cancelled)
	            {
	            	try {
						File toDelete = new File(destination);
						toDelete.delete();
					} catch (Exception e) {}	
	            }
		 	}
	}
}