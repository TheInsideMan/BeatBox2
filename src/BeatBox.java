import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
public class BeatBox {
	
	JPanel mainPanel;
	ArrayList<JCheckBox> checkboxList;
	Sequencer sequencer;
	Sequence sequence;
	Track track;
	JFrame theFrame;
	
	String[] instrumentNames ={"Bass Drum", "Closed Hi-Hat","Open Hi-Hat","Acoustic Snare","Crash Cymbal",
			"Hand Clap","High Tom","Hi Bongo","Maracas","Whistle","Low Conga","Cowbell","Vibraslap",
			"Low-mid Tom","High Agogo","Open Hi Conga"};
	
	int[] instruments={35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
	
	public static void main(String[] args){
		new BeatBox().buildGUI();
	}
	
	public void buildGUI(){
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		checkboxList = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		
		JButton start = new JButton("Start");
		start.addActionListener(new MyStarterListener());
		buttonBox.add(start);
		
		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);
		
		JButton upTempo = new JButton("Tempo Up!");
		upTempo.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);
		
		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);
		
		JButton serializeIt = new JButton("Serialize it!");
		serializeIt.addActionListener(new MySendListener());
		buttonBox.add(serializeIt);
		
		JButton restoreIt = new JButton("Restore it!");
		restoreIt.addActionListener(new MyReadInListener());
		buttonBox.add(restoreIt);
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for(int i =0;i<16;i++){
			nameBox.add(new Label(instrumentNames[i]));
		}
		
		background.add(BorderLayout.EAST,buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		
		theFrame.getContentPane().add(background);
		
		GridLayout grid = new GridLayout(16,16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER,mainPanel);
		
		for(int i=0;i<256;i++){
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		
		setUpMidi();
		ImageIcon img = new ImageIcon("android-icon.jpg");
		theFrame.setIconImage(img.getImage());
		theFrame.setBounds(50,50,300,300);
		theFrame.pack();
		theFrame.setVisible(true);
	}
	public void setUpMidi(){
		try{
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ,4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e){e.printStackTrace();}
	}//close method
	public void buildTrackAndStart(){
		int[] trackList = null;
		
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for(int i =0;i<16;i++){ //loop for each beat in a track
			trackList = new int[16];
			
			int key = instruments[i];
			
			for(int j=0;j<16;j++){
				JCheckBox jc = checkboxList.get(j + 16*i);
				if(jc.isSelected()){
					trackList[j] = key;
				} else {
					trackList[j]=0;
				}
			}//close inner loop
			
			makeTracks(trackList);
			track.add(makeEvent(176,1,127,0,16));//listener event
		}//close outer
		track.add(makeEvent(192,9,1,0,15));
		try {
			sequencer.setSequence(sequence);
			//number of times the track loops
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch(Exception e){e.printStackTrace();}
	}//close buildsTracksAndStart method
	
	public class MyStarterListener implements ActionListener {
		public void actionPerformed(ActionEvent a){
			buildTrackAndStart();//lets kick it off
		}
	}
	
	public class MyStopListener implements ActionListener {
		public void actionPerformed(ActionEvent a){
			sequencer.stop();
		}
	}//close inner class
	
	public class MyUpTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent a){
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor*1.03));
		}
	}//close inner class
	
	public class MyDownTempoListener implements ActionListener{
		public void actionPerformed(ActionEvent a){
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor*.97));
		}
	}//close inner class
	public class MySendListener implements ActionListener{
		public void actionPerformed(ActionEvent a){
			boolean[] checkboxState = new boolean[256];
			for(int i =0;i<256;i++){
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(check.isSelected()){
					checkboxState[i]=true;
				}
			}//end of for loop
			try {
				JFileChooser fileSave = new JFileChooser();
				fileSave.showSaveDialog(theFrame);
				File file = (File)fileSave.getSelectedFile();
				//BufferedWriter savedFile = new BufferedWriter(new FileWriter(file));
				FileOutputStream fileStream = new FileOutputStream(file);
				ObjectOutputStream os = new ObjectOutputStream(fileStream);
				os.writeObject(checkboxState);
			} catch(Exception ex){
				ex.printStackTrace();
			}
		}//end of method
	}//end of class
	public class MyReadInListener implements ActionListener{
		public void actionPerformed(ActionEvent a){
			boolean[] checkboxState = null;
			try {
				JFileChooser fileSave = new JFileChooser();
				fileSave.showSaveDialog(theFrame);
				File file = (File)fileSave.getSelectedFile();
				FileInputStream fileIn = new FileInputStream(file);
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkboxState = (boolean[]) is.readObject();
			}catch(Exception ex){ex.printStackTrace();}
			
			//now restore the checkboxs
			for(int i=0;i<256;i++){
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(checkboxState[i]){
					check.setSelected(true);
				} else {
					check.setSelected(false);
				}
			}

			sequencer.stop();
			buildTrackAndStart();
		}//close method
	}//close class
	
	public void makeTracks(int[] list){
		for(int i=0;i<16;i++){
			int key = list[i];
			if(key !=0){
				//NOTE ON & NOTE OFF events
				track.add(makeEvent(144,9,key,100,i));
				track.add(makeEvent(128,9,key,100,i+1));
			}
		}
	}
	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick){
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(comd,chan,one,two);
			event = new MidiEvent(a,tick);
		} catch(Exception e){e.printStackTrace();}
		return event;
	}
}//close class