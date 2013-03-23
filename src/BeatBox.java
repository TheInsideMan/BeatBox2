import java.awt.*;
import javax.swing.*;
import java.io.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.event.*;


//not sure why this isn't working!
public class BeatBox {
	JFrame theFrame;
	JPanel mainPanel;
	JList incomingList;
	JTextField userMessage;
	ArrayList<JCheckBox> checkboxList;
	int nextNum;
	Vector<String> listVector = new Vector<String>();
	String userName;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
	
	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;
	
	
	String[] instrumentNames ={"Bass Drum", "Closed Hi-Hat","Open Hi-Hat","Acoustic Snare","Crash Cymbal",
			"Hand Clap","High Tom","Hi Bongo","Maracas","Whistle","Low Conga","Cowbell","Vibraslap",
			"Low-mid Tom","High Agogo","Open Hi Conga"};
	
	int[] instruments={35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
	
	public static void main(String[] args){
		new BeatBox().startUp(args[0]);//args[0] is the user name
	}
	
	public void startUp(String name){
		userName = name;
		//open connection to the server
		try {
			Socket sock = new Socket("127.0.0.1",4242);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread remote = new Thread(new RemoteReader());
			remote.start();
		} catch(Exception ex){
			System.out.println("Couldn't connect - you'll have to play alone");
		}
		setUpMidi();
		buildGUI();
	}
	
	public void buildGUI(){
		theFrame = new JFrame("Cyber BeatBox");
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
		/*
		JButton serializeIt = new JButton("Serialize it!");
		serializeIt.addActionListener(new MySendListener());
		buttonBox.add(serializeIt);
		
		JButton restoreIt = new JButton("Restore it!");
		restoreIt.addActionListener(new MyReadInListener());
		buttonBox.add(restoreIt);
		*/
		JButton sendIt = new JButton("Send it!");
		sendIt.addActionListener(new MySendListener());
		buttonBox.add(sendIt);
		
		userMessage = new JTextField();
		buttonBox.add(userMessage);
		
		incomingList = new JList();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector);//no data to start with
		
		
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
		ArrayList<Integer> trackList = null;
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for(int i =0;i<16;i++){ //loop for each beat in a track
			trackList = new ArrayList<Integer>();
			
			for(int j=0;j<16;j++){
				JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
				if(jc.isSelected()){
					int key = instruments[i];
					trackList.add(new Integer(key));
				} else {
					trackList.add(null);//because this slot should be empty in the track
				}
			}//close inner loop
			
			makeTracks(trackList);
		
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
			
			String messageToSend = null;
			try {
				out.writeObject(userName + nextNum++ + ": "+userMessage.getText());
				out.writeObject(checkboxState);
			} catch (Exception ex){
				System.out.println("Sorry dude. could not send the beat to the server");
			}
			userMessage.setText("");
			/*
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
			*/
		}//end of method
	}//end of class
	
	public class MyListSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent le){
			if(!le.getValueIsAdjusting()){
				String selected = (String) incomingList.getSelectedValue();
				if(selected != null){
					//go to the map and change the sequence
					boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					changeSequence(selectedState);
					sequencer.stop();
					buildTrackAndStart();
				}
			}
		}
	}//end of class
	/*
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
	*/
	public class RemoteReader implements Runnable {
		boolean[] checkboxState = null;
		String nameToShow = null;
		Object obj = null;
		public void run(){
			try {
				while((obj=in.readObject()) != null){
					System.out.println("got an object from the server.");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;
					checkboxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow,checkboxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}//close while
			} catch (Exception ex){ex.printStackTrace();}
		}//close run
	}//close inner class
	
	public class MyPlayMineListener implements ActionListener {
		public void actionPerformed(ActionEvent a){
			if(mySequence !=null){
				sequence = mySequence;
			}
		}//close action performed
	}//close inner class
	public void changeSequence(boolean[] checkboxState){
		for(int i=0; i<256;i++){
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if(checkboxState[i]){
				check.setSelected(true);
			} else {
				check.setSelected(false);
			}
		}
	}
	public void makeTracks(ArrayList list){
		Iterator it = list.iterator();
		for(int i=0;i<16;i++){
			Integer num = (Integer) it.next();
			if(num !=null){
				//NOTE ON & NOTE OFF events
				int numKey = num.intValue();
				track.add(makeEvent(144,9,numKey,100,i));
				track.add(makeEvent(128,9,numKey,100,i+1));
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

/*
Some comments here for the sake of a git test

yet  more comments
*/