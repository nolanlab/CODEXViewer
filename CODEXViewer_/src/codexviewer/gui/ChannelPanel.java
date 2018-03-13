package codexviewer.gui;

import java.awt.Panel;
import javax.swing.BoxLayout;

/**
 * 
 * @author Vishal
 *
 */
public class ChannelPanel extends Panel {
    
	private ChannelControl channelControl;
    private ChannelNames channelNames;
    
	public ChannelPanel(ViewerWindow win) {
		
        channelControl = new ChannelPicker(win);
        channelNames = new ChannelNames(win, (ChannelPicker)channelControl);
        
        
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.add(channelControl);
        this.add(channelNames);
        this.revalidate();
        win.pack();
	}
	
	public ChannelControl getChannelPicker() {
		return channelControl;
	}
	
	public ChannelNames getChannelNamesPanel() {
		return channelNames;
	}

}
