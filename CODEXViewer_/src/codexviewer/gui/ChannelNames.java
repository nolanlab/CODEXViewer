package codexviewer.gui;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import codexviewer.i5d.Image5D;
import ij.IJ;
import ij.ImageJ;

/**
 * 
 * @author Vishal
 *
 */
public class ChannelNames extends Panel {
	private Label channelName;
	private ViewerWindow win;
	private Image5D i5d;
	private ImageJ ij;
	private ChannelPicker channelPicker;

	public ChannelNames(ViewerWindow win, ChannelPicker channelPicker) {
		this.win = win;
		this.i5d = (Image5D)win.getImagePlus();
		this.ij = IJ.getInstance();
		this.channelPicker = channelPicker;
		
		initComponents();
		win.pack();
	}
	
	public void initComponents() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		List<String> chNames = new ArrayList<>();
		chNames = getChannelNames();
		boolean chNamesExist = false;
		if(chNames != null && chNames.size() == i5d.getNChannels()) {
			chNamesExist = true;
		}
		
		Label top = new Label();
		top.setText("Channel names");
		Dimension l = new Dimension();
		Dimension chLabelSize = getChannelPicker().getChannelParametersLabel().getPreferredSize();
		l.setSize(chLabelSize.getWidth()+chLabelSize.getWidth(), chLabelSize.getHeight()+chLabelSize.getHeight()+5);
		top.setPreferredSize(l);
		this.add(top);
		
		for(int i=0; i<i5d.getNChannels(); i++) {			
			channelName = new Label();
			if(chNamesExist) {
				channelName.setText(chNames.get(i));
			}
			else {
				channelName.setText("Channel: "+ (i+1));
			}
			this.add(channelName);
		}
		
		if(getChannelPicker().getOpacityLabel() != null && getChannelPicker().getOpacitySlider() != null) {
			Label dummy = new Label();
			Dimension d = new Dimension();
			Dimension opacityLabelSize = getChannelPicker().getOpacityLabel().getPreferredSize();
			Dimension opacitySliderSize = getChannelPicker().getOpacitySlider().getPreferredSize();
			d.setSize(opacityLabelSize.getWidth()+opacitySliderSize.getWidth(), opacityLabelSize.getHeight()+opacitySliderSize.getHeight()+15);
			dummy.setPreferredSize(d);
			dummy.setText(" ");
			this.add(dummy);
		}
	}
	
	/**
	 * List the channel names from channelNames.txt file
	 * @param i
	 */
	private List<String> getChannelNames() {
		File dir = new File(i5d.getFileLocation());
		if(dir != null) {
			String chNames = "channelnames.txt";
			File[] chFile = dir.getParentFile().listFiles(t -> t.getName().equalsIgnoreCase(chNames));
			if(chFile != null && chFile.length == 1) {
				Path filePath = chFile[0].toPath();
				Charset charset = Charset.defaultCharset();        
				try {
					List<String> stringList = Files.readAllLines(filePath, charset);
					return stringList;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public void setChannelPicker(ChannelPicker channelPicker) {
		this.channelPicker = channelPicker;
	}
	
	public ChannelPicker getChannelPicker() {
		return channelPicker;
	}
}
