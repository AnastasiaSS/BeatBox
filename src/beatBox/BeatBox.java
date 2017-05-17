package beatBox;

        import javax.sound.midi.*;
        import javax.swing.*;
        import java.awt.*;
        import java.awt.event.ActionEvent;
        import java.awt.event.ActionListener;
        import java.io.FileInputStream;
        import java.io.FileOutputStream;
        import java.io.ObjectInputStream;
        import java.io.ObjectOutputStream;
        import java.util.ArrayList;

/**
 * Created by Настя on 31.01.2017.
 */
public class BeatBox {
    JFrame frame;
    JPanel mainPanel;
    ArrayList<JCheckBox> boxList;
    Sequence sequence;
    Sequencer sequencer;
    Track track;
    String[] instrumentsNames = {"Bass Dram", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open High Conga"};
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        BeatBox beatBox = new BeatBox();
        beatBox.createGUI();
    }

    public void createGUI() {
        frame = new JFrame("Beat Box");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout borderLayout = new BorderLayout();
        JPanel panel = new JPanel(borderLayout);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        boxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton tempoUp = new JButton("Tempo Up");
        tempoUp.addActionListener(new MyTempoUpListener());
        buttonBox.add(tempoUp);

        JButton tempoDown = new JButton("Tempo Down");
        tempoDown.addActionListener(new MyTempoDownListener());
        buttonBox.add(tempoDown);

        JButton serializeIt = new JButton("Serialize it");
        serializeIt.addActionListener(new MySendListener());
        buttonBox.add(serializeIt);

        JButton restore = new JButton("Restore");
        restore.addActionListener(new MyReadInListener());
        buttonBox.add(restore);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < instrumentsNames.length; i++) {
            nameBox.add(new Label(instrumentsNames[i]));
        }

        panel.add(BorderLayout.WEST, nameBox);
        panel.add(BorderLayout.EAST, buttonBox);

        frame.getContentPane().add(panel);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setVgap(2);
        mainPanel = new JPanel(grid);
        //mainPanel.setBackground(Color.lightGray);
        panel.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < (instrumentsNames.length * instrumentsNames.length); i++) {
            JCheckBox box = new JCheckBox();
            box.setSelected(false);
            boxList.add(box);
            mainPanel.add(box);
        }

        setUpMidi();

        frame.setBounds(50, 50, 300, 300);
        frame.pack();
        frame.setVisible(true);
    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public void buildTrack() {
        int[] trackList = null;

        //Вытираем старый трек из отметок и создаём его с чистого листа
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < instruments.length; i++) {
            trackList = new int[16];

            int key = instruments[i];

            for (int j = 0; j < instruments.length; j++) {
                JCheckBox box = boxList.get(j + i * 16);
                if (box.isSelected()) {
                    trackList[j] = key;
                } else
                    trackList[j] = 0;
            }
            makeTrack(trackList);
            track.add(makeEvent(176, 1, 127, 0, 16));
        }
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void makeTrack(int[] seq) {
        for (int i = 0; i < seq.length; i++) {
            int key = seq[i];
            if (key != 0) {
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }

    public static MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage message = new ShortMessage();
            message.setMessage(comd, chan, one, two);
            event = new MidiEvent(message, tick);
        } catch (Exception e) {
        }
        return event;
    }

    class MyStartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            buildTrack();
        }
    }

    class MyStopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            sequencer.stop();
        }
    }

    class MyTempoUpListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            float f = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (f * 1.03));
        }
    }

    class MyTempoDownListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            float f = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (f * 0.97));
        }
    }

    class MySendListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            boolean[] checkBoxState = new boolean[256];
            for (int i = 0; i < checkBoxState.length; i++) {
                JCheckBox box = boxList.get(i);
                if (box.isSelected())
                    checkBoxState[i] = true;
            }
            try {
                FileOutputStream fos = new FileOutputStream("D:\\BeatBox.ser");
                ObjectOutputStream os = new ObjectOutputStream(fos);
                os.writeObject(checkBoxState);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class MyReadInListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            boolean[] checkBoxState = null;
            try {
                FileInputStream fis = new FileInputStream("D:\\BeatBox.ser");
                ObjectInputStream is = new ObjectInputStream(fis);
                checkBoxState = (boolean[]) is.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < checkBoxState.length; i++) {
                JCheckBox box = boxList.get(i);
                if (checkBoxState[i])
                    box.setSelected(true);
                else
                    box.setSelected(false);
            }
            sequencer.stop();
            buildTrack();
        }
    }
}

