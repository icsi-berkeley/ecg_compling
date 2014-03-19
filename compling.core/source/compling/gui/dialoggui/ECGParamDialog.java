package compling.gui.dialoggui;

//import java.util.Vector;

//import javax.swing.JTextField;

import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.UIManager;
import compling.gui.ComplingUI;


public class ECGParamDialog extends ElementDialog {

	private static final long serialVersionUID = 1L;
    protected Preferences preferences;

	public ECGParamDialog(JFrame frame) {
        super(frame, "ComplingUI parameters");
        preferences = Preferences.userNodeForPackage(ComplingUI.class);
    }

    /**
    * Create GUI components for each parameter; don't hardcode names?
    */
    protected void setupParameters() {
       
      

    }

    //public double getUtilityInteraction() {
    public void updateParams() {

    }
    
    public static void createAndShowGUI() {

       try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
       } catch (Exception e) {
          System.err.println("Couldn't use system look and feel.");
       }
       
       // Create and set up the window.
       JFrame frame = new JFrame("Welcome to the compling package");
       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

       // Create and set up the content pane.
       ECGParamDialog dialogue = new ECGParamDialog(frame);
       frame.setContentPane(dialogue);

       // Display the window.
       frame.pack();
       frame.setVisible(true);
    }

}
