

import ij.CompositeImage;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.process.LUT;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * @author Vishal
 *
 */
public class ChannelPicker extends ChannelControl {

	boolean[] alreadySet;
	protected Panel subPanel;
	private Checkbox redCb;
	private Checkbox greenCb;
	private Checkbox blueCb;
	private RangeSlider slider;
	private Label channelName;

	public ChannelPicker(ViewerWindow win) {
		super();

		this.win = win;
		this.i5d = (Image5D) win.getImagePlus();
		this.ij = IJ.getInstance();
		
		currentChannel = i5d.getCurrentChannel();
		nChannels = i5d.getNChannels();

		//Control from ChannelControl
		displayChoice = new Choice();
		displayChoice.add(displayModes[ONE_CHANNEL_GRAY]);
		displayChoice.add(displayModes[ONE_CHANNEL_COLOR]);
		displayChoice.add(displayModes[OVERLAY]);
		displayChoice.add(displayModes[TILED]);

		//Default it to OVERLAY mode for CODEXViewer
		displayChoice.select(OVERLAY);
		displayMode = OVERLAY;

		i5d.setDisplayMode(OVERLAY);
		
		//Initialize all channels to black color when loading the i5d image on viewer. 
		for (int i = 0; i < i5d.getNChannels(); i++) {
			setChannelLut(Color.black, i);
		}

		//Handling of Key events
		addKeyListener(win);
		addKeyListener(ij);
		addKeyListener(win);

		//Useless control from ChannelControl created and removed
		uselessControlInit();

		setDisplayMode(IJ.COMPOSITE);
		alreadySet = new boolean[i5d.getNChannels()];
		Checkbox arr[][] = new Checkbox[i5d.getNChannels()][3];
		
		//Layout preferences
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		//Initialize all components & overlay if present
		initComponents(arr);
		initOverlay();

		this.invalidate();
		win.pack();
	}

	/**
	 * Override non used method from ChannelControl and do nothing
	 */
	void drawLUT() {

	}
	
	/**
	 * If overlay is present, add to the image5d object
	 * Create a new slider to adjust opacity of overlay object
	 */
	private void initOverlay() {

		Overlay overlay = new Overlay();
		File dir = new File(i5d.getFileLocation());
		if(dir != null) {		  
			File[] regFiles = dir.getParentFile().listFiles(t -> t.getName().contains("regions_"+FilenameUtils.removeExtension(dir.getName())));
			if(regFiles != null && regFiles.length != 0) {
				
				Label opacityLabel = new Label();
				opacityLabel.setText("Adjust overlay opacity");
				this.add(opacityLabel);
				JSlider opacitySlider = new JSlider();
				opacitySlider.setToolTipText("Overlay opacity");
				opacitySlider.setMinimum(0);
				opacitySlider.setMaximum(10);
				opacitySlider.setValue(10);
				for(int z = 0; z < regFiles.length; z++) {
					ImagePlus im2 = IJ.openImage(regFiles[z].getPath());
					ImageRoi imgRoi = new ImageRoi(0, 0, im2.getProcessor());
					imgRoi.setNonScalable(true);
					imgRoi.setZeroTransparent(true);
					imgRoi.setOpacity(1);
					imgRoi.setPosition(z+1);
					overlay.add(imgRoi);
				}
				i5d.setOverlay(overlay);
				
				opacitySlider.addChangeListener(e -> {
					JSlider sl = (JSlider) e.getSource();
					es.submit(new Runnable() {
						@Override
						public void run() {
							Overlay ov = i5d.getOverlay();
							for(int z=0; z<i5d.getNSlices(); z++) {
								ImageRoi imgRoi = (ImageRoi)ov.get(z+1);
								imgRoi.setOpacity((double)(sl.getValue())/100);
								ov.add(imgRoi);
							}
							i5d.setOverlay(ov);
							//i5d.updateAndRepaintWindow();
						}
					});
				});
				this.add(opacitySlider);
		  	}
		}
	}
	
	/**
	 * Useless controls from the parent ChannelControl class. These are to be created and then removed
	 */
	private void uselessControlInit() {
		colorButton = new Button();
		subPanel = new Panel();
		selectorPanel = new Panel();
		cColorChooser = new ChannelColorChooser(this);
		scrollbarWL = new ScrollbarWithLabel(Scrollbar.VERTICAL, 1, 1, 1, nChannels + 1, i5d.getDimensionLabel(2));
		channelSelectorOverlay = new ChannelSelectorOverlay(this);
	}

	@Override
	public synchronized void setDisplayMode(int mode) {
		super.setDisplayMode(IJ.COMPOSITE);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(350, i5d.getNChannels() * 30);
	}

	ExecutorService es = Executors.newFixedThreadPool(1);

	private void initComponents(Checkbox arr[][]) {
		List<String> chNames = new ArrayList<>();
		chNames = getChannelNames();
		this.add(new Label("Channel Info"));

		for (int i = 0; i < i5d.getNChannels(); i++) {
			JPanel smallP = new JPanel();
			
			//Checkboxes for RGB
			redCb = new Checkbox();
			greenCb = new Checkbox();
			blueCb = new Checkbox();

			redCb.setLabel("R");
			greenCb.setLabel("G");
			blueCb.setLabel("B");

			//Number of checkboxes based on input image channels
			arr[i][0] = redCb;
			arr[i][1] = greenCb;
			arr[i][2] = blueCb;

			final int j = i;
			
			//Item listeners to capture select and deselect of RGB checkboxes
			redCb.addItemListener(e -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					adjustRowColors(arr, j, 0);
					adjustColumnColors(arr, j, 0);
					setChannelLut(Color.red, j);
				}
				if (e.getStateChange() != ItemEvent.SELECTED) {
					setChannelLut(Color.BLACK, j);
				}
				i5d.updateAndRepaintWindow();
			});

			greenCb.addItemListener(e -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					adjustRowColors(arr, j, 1);
					adjustColumnColors(arr, j, 1);
					setChannelLut(Color.green, j);
				}
				if (e.getStateChange() != ItemEvent.SELECTED) {
					setChannelLut(Color.BLACK, j);
				}
				i5d.updateAndRepaintWindow();
			});

			blueCb.addItemListener(e -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					adjustRowColors(arr, j, 2);
					adjustColumnColors(arr, j, 2);
					setChannelLut(Color.blue, j);
				}
				if (e.getStateChange() != ItemEvent.SELECTED) {
					setChannelLut(Color.BLACK, j);
				}
				i5d.updateAndRepaintWindow();
			});
			
			//Add a range slider after checkbox
			slider = new RangeSlider();
			slider.setMinimum(0);
			//slider.setMaximum((int) Math.pow(2, i5d.getBitDepth()) - 1);
			slider.setMaximum((int) i5d.getDisplayRangeMax());
			i5d.setPosition(j + 1, i5d.getSlice(), i5d.getFrame());
			//i5d.resetDisplayRange();
			slider.setValue(0);
			slider.setUpperValue((int) i5d.getDisplayRangeMax());

			slider.addChangeListener(e -> {
//				if (slider.getValueIsAdjusting())
//					return;
				RangeSlider rs = (RangeSlider) e.getSource();
				es.submit(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						i5d.setChannel(j + 1);
						i5d.setDisplayRange(rs.getValue(), rs.getUpperValue());
						// i5d.updateAndDraw();
					}
				});
			});

			//Display channel name if channelNames.txt is present
			channelName = new Label();
			if(chNames != null) {
				channelName.setText(chNames.get(i));
			}
			else {
				channelName.setText("Channel: "+ (i+1));
			}

			smallP.setLayout(new BoxLayout(smallP, BoxLayout.X_AXIS));
			smallP.add(redCb);
			smallP.add(greenCb);
			smallP.add(blueCb);
			smallP.add(slider);
			smallP.add(channelName);

			this.add(smallP);
		}
	}

	/**
	 * Update the values of checkbox selection based on row
	 * @param matrix
	 * @param r
	 * @param c
	 */
	public void adjustRowColors(Checkbox[][] matrix, int r, int c) {
		for (int k = 0; k < i5d.getNChannels(); k++) {
			if (matrix[k][c].getState() && r != k) {
				matrix[k][c].setState(false);
				setChannelLut(Color.BLACK, k);
			}
		}
	}

	/**
	 * Update the values of checkbox selection based on col 
	 * @param matrix
	 * @param r
	 * @param c
	 */
	public void adjustColumnColors(Checkbox[][] matrix, int r, int c) {
		for (int k = 0; k < 3; k++) {
			if (matrix[r][k].getState() && c != k) {
				matrix[r][k].setState(false);
			}
		}
	}

	/**
	 * Override non used method from ChannelControl and do nothing
	 */
	public void updateFromI5D() {

	}

	public void setChannelLut(Color color, int ch) {
		final int ch2 = ch + 1;
		es.submit(new Runnable() {

			@Override
			public void run() {
				synchronized (i5d) {
					i5d.storeChannelProperties(ch2);
					i5d.getChannelDisplayProperties(ch2)
							.setColorModel(ChannelDisplayProperties.createModelFromColor(color));
					i5d.restoreChannelProperties(ch2);
				}
			}
		});
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

	/**
	 * Override non used method from ChannelControl and do nothing
	 */
	public void setColor(Color c) {

	}
}
