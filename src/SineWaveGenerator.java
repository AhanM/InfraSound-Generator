import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public class SineWaveGenerator extends JFrame {

    private SampleThread thread;
    private JSlider Slider;
    private JTextField InputFreqField, InputTempField,tf;
    private JLabel tempLabel, freqLabel, inputVelLabel;
    private double temp = 0;
    private double CurrentVel = 0, velSound = 331.3;


    //Run the application
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        SineWaveGenerator frame = new SineWaveGenerator();
                        frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    public SineWaveGenerator()
    {
        //User-Interface Components
        addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    thread.exit();
                    System.exit(0);
                }
            });

        setTitle("Sine Wave Generator");
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700,250);
        setLocationRelativeTo(null);
        FlowLayout Layout = new FlowLayout();
        Layout.setHgap(100);
        Layout.setVgap(10);
        getContentPane().setLayout(Layout);

        JLabel lblAdjustfreq = new JLabel("Adjust Frequency(Hz)");
        lblAdjustfreq.setHorizontalAlignment(SwingConstants.CENTER);
        lblAdjustfreq.setFont(new Font("Tahoma", Font.PLAIN, 18));
        getContentPane().add(lblAdjustfreq);

        Slider = new JSlider();
        Slider.setName("");
        Slider.setMinimum(0);
        Slider.setPaintLabels(true);
        Slider.setPaintTicks(true);
        Slider.setMajorTickSpacing(50);
        Slider.setMaximum(200);
        Slider.setValue(100);
        Slider.setSize(1000,1000);
        getContentPane().add(Slider);

        freqLabel = new JLabel("Enter Frequency(integers only)");
        freqLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        getContentPane().add(freqLabel);

        InputFreqField = new JTextField("0",20);
        InputFreqField.addActionListener(new FrequencyHandler());
        getContentPane().add(InputFreqField);

        tempLabel = new JLabel("Enter Temprature(in Celsius)    ");
        tempLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        getContentPane().add(tempLabel);

        InputTempField = new JTextField("0",20);
        InputTempField.addActionListener(new TempHandler());

        getContentPane().add(InputTempField);

        inputVelLabel = new JLabel("Input Current Velocity(km/hr)  ");
        inputVelLabel.setHorizontalAlignment(SwingConstants.CENTER);
        inputVelLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        getContentPane().add(inputVelLabel);

        tf = new JTextField("0",20);
        tf.addActionListener(new VelocityHandler());
        getContentPane().add(tf);

        // Threading
        thread = new SampleThread();
        thread.start();
    }

    double CalVelSound(double Temprature)
    {
        velSound = (331.3 + 0.606*Temprature);
        return velSound;
    }

    // Adjusting frequency to nullify Doppler Effect
    double CalDoppler(double freq, double velSource, double VelocitySound)
    {
        double result = freq*((VelocitySound-velSource)/VelocitySound);
        return result;
    }

    class TempHandler implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent evt) {
            temp = Double.parseDouble(InputTempField.getText());
        }
    }

    // Stores Velocity from Input Field
    class VelocityHandler implements ActionListener
    {

        @Override
        public void actionPerformed(ActionEvent evt) {

            CurrentVel = Integer.parseInt(tf.getText())* 5 / 18; // converting km/hr to m/s
            if(CurrentVel!=0) {
                System.out.println("FREQUENCY AFTER ADJUSTING DOPPLER Effect: "
                        +CalDoppler(Slider.getValue(),CurrentVel,velSound));
            }
            System.out.println(tf.getText());
        }

    }

    // Takes Frequency through Field and changes Slider Value accordingly
    class FrequencyHandler implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent evt)
        {
            Slider.setValue((Integer.parseInt(InputFreqField.getText())));;
        }
    }

    class SampleThread extends Thread {

        final static public int SAMPLING_RATE = 44100;
        final static public int SAMPLE_SIZE = 2;                 //Sample size in bytes
        final static public double BUFFER_DURATION = 0.100;      //100ms buffer


        // Size in bytes of sine wave samples
        final static public int SINE_PACKET_SIZE = (int)(BUFFER_DURATION*SAMPLING_RATE*SAMPLE_SIZE);

        SourceDataLine line;
        public double freq;                                    //Set from Slider or from TextField
        public boolean exitThread = false;

        //Get the number of queued samples in the SourceDataLine buffer
        private int getLineSampleCount() {
            return line.getBufferSize() - line.available();
        }

        //Continually fill the audio output buffer whenever it begins to get empty, SINE_PACKET_SIZE/2
        //samples at a time, until exit the thread
        public void run() {
            //Position through the sine wave as a percentage (0-1 <=> 0-2*PI)
            double freqCyclePos = 0;

            //Open up the audio output, using a sampling rate of 44100hz, 16 bit samples, mono
            // Need a buffer size of at least twice the packet size
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, SINE_PACKET_SIZE*2);

                if (!AudioSystem.isLineSupported(info))
                    throw new LineUnavailableException();

                line = (SourceDataLine)AudioSystem.getLine(info);
                line.open(format);
                line.start();
            }
            catch (LineUnavailableException e) {
                System.out.println("Line of that type is not available");
                e.printStackTrace();
                System.exit(-1);
            }

            System.out.println("Requested line buffer size = " + SINE_PACKET_SIZE*2);
            System.out.println("Actual line buffer size = " + line.getBufferSize());

            ByteBuffer buff = ByteBuffer.allocate(SINE_PACKET_SIZE);
            //On each cycle main loop fills the available free space in the audio buffer
            //Main loop creates audio samples for sine wave, runs until the thread is exited
            //Each sample is spaced 1/SAMPLING_RATE apart in time
            while (exitThread==false) {
                freq = CalDoppler(Slider.getValue(),CurrentVel, CalVelSound(temp));

                if(!InputFreqField.isFocusOwner())
                    InputFreqField.setText(""+Slider.getValue());

                double freqCycleInc = freq/SAMPLING_RATE;   //Fraction of cycle between samples

                buff.clear();                             //Toss out samples from previous cycle

                //Generate SINE_PACKET_SIZE samples based on the current freqCycleInc from freq
                for (int i=0; i < SINE_PACKET_SIZE/SAMPLE_SIZE; i++) {
                    buff.putShort((short)(Short.MAX_VALUE * Math.sin(2*Math.PI * freqCyclePos)));

                    freqCyclePos += freqCycleInc;
                    if (freqCyclePos > 1)
                        freqCyclePos -= 1;
                }

                // Write sine samples to the line buffer
                line.write(buff.array(), 0, buff.position());

                //Wait here until there are less than SINE_PACKET_SIZE samples in the buffer
                //(Buffer size is 2*SINE_PACKET_SIZE at least, so there will be room for
                // at least SINE_PACKET_SIZE samples when this is true)
                try {
                    while (getLineSampleCount() > SINE_PACKET_SIZE)
                        Thread.sleep(1);                          // Give the UI time to run
                }
                catch (InterruptedException e) {                // Handle the unwanted Exception
                }
            }

            line.drain();
            line.close();
        }

        public void exit() {
            exitThread=true;
        }
    }
}
