//=============================================================================
//File        : ComplingUI.java
//Author      : emok
//Change Log  : Created on Dec 6, 2006
//=============================================================================

package compling.gui;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import compling.annotation.AnnotationException;
import compling.context.ContextModel;
import compling.grammar.GrammarException;
import compling.grammar.ecg.Grammar;
import compling.gui.LearnerPrefs.LP;
import compling.gui.grammargui.GrammarBrowser;
import compling.learner.ECGLearner;
import compling.learner.LearnerException;
import compling.learner.featurestructure.LCAException;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.AnalysisUtilities;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.simulator.Simulator;
import compling.simulator.SimulatorException;


//=============================================================================

public class ComplingUI {

   ContextModel contextModel = null;
   Simulator simulator = null;
   ECGLearner learner = null;
   ECGAnalyzer analyzer = null;

   Grammar grammar = null;

   List<File> transcripts = null;
   List<String> sentences = null;
   List<File> xmls = null;

   private long runTime;
   private long startTime = 0, stopTime = 0;

   private boolean useGui, analyze, learn;

   public ComplingUI(String preferenceFilePath) throws Exception {

      LearnerPrefs preferences = new LearnerPrefs(preferenceFilePath);
      changeGlobalLoggingHandler(new LoggingHandler());
      Map<ComplingPackage, Level> loggingLevels = new HashMap<ComplingPackage, Level>();
      loggingLevels.put(ComplingPackage.GLOBAL, Level.INFO);

      Map<ComplingPackage, String> loggingLevelNames = preferences.getLoggingLevels();
      for (ComplingPackage pkg : loggingLevelNames.keySet()) {
         try {
            loggingLevels.put(pkg, Level.parse(loggingLevelNames.get(pkg)));
         } catch (IllegalArgumentException iae) {
            loggingLevels.put(pkg, Level.INFO);
         }
      }

      setLoggingLevel(loggingLevels);

      useGui = Boolean.valueOf(preferences.getSetting(LP.USE_GUI));
      analyze = Boolean.valueOf(preferences.getSetting(LP.ANALYZE));
      learn = Boolean.valueOf(preferences.getSetting(LP.LEARN));

      if (analyze || learn) {
         learner = new ECGLearner(preferenceFilePath);
      }
   }


   public void run() throws IOException {
      if (useGui) {
         javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               GrammarBrowser.createAndShowGUI(grammar);
            }
         });
      } else {
         startTime = System.currentTimeMillis();
         learner.run();
         stopTime = System.currentTimeMillis();
      }
   }

   protected static void outputErrorMessage(Exception e) {
      System.err.println(e.getMessage());
      if (e.getCause() != null) {
         System.err.println(e.getCause().getMessage());
      }
      e.printStackTrace(System.err);
   }


   public long getRunTime() {
      if (stopTime == 0) { stopTime = System.currentTimeMillis(); }
      runTime = stopTime - startTime;
      return runTime;
   }


   public long getAnalyzerConstructorTime() {
      return learner.getAnalyzerConstructorTime();
   }

   public ECGLearner getLearner() {
      return learner;
   }

   public void changeGlobalLoggingHandler(LoggingHandler handler) {
      Logger rootLogger = LogManager.getLogManager().getLogger("");
      for (Handler existing : rootLogger.getHandlers()) {
         rootLogger.removeHandler(existing);
      }
      rootLogger.addHandler(handler);
   }

   public void setLoggingLevel(Map<ComplingPackage, Level> levels) {
      for (ComplingPackage loggable : ComplingPackage.values()) {
         if (levels.containsKey(loggable)) {
            Logger logger = Logger.getLogger(loggable.getPackageName());
            logger.setLevel(levels.get(loggable));
         }
      }
   }


   public static void main(String[] argv) throws Exception {

      if (argv.length != 1) {
         throw new GUIException("Must be called with the path of the prefs file");
      }

      ComplingUI ui = null;
      try {
         ui = new ComplingUI(argv[0]);
         Analysis.setFormatter(new AnalysisUtilities.DefaultAnalysisInContextFormatter());
         ui.run();
      } catch (GUIException ge) {
         exitWithError();
      } catch (LearnerException le) {
         outputErrorMessage(le);
      } catch (IOException ioe) {
         outputErrorMessage(ioe);
      } catch (AnnotationException ae) {
         outputErrorMessage(ae);
      } catch (SimulatorException se) {
         outputErrorMessage(se);
      } catch (LCAException lcae) {
         outputErrorMessage(lcae);
      } catch (GrammarException ge) {
         outputErrorMessage(ge);
      } finally {
         if (ui != null) {
            System.out.println("\n\nparser constructor build time = " + ui.getAnalyzerConstructorTime());
            System.out.println("total run time = " + ui.getRunTime());
            if (ui.getLearner() != null) {
               System.out.println(ui.getLearner().getFinalOutput());
               try {
                  ui.getLearner().outputResults();
               } catch (IOException ioe) {
                  outputErrorMessage(ioe);
               }
            }
         }
      }

   }


   private static void exitWithError() {
      System.err.println("Usage: ComplingUI \n\t -p: input parameters \n\t -g: graphical interface \n\t -c: clear preferences");
      System.exit(1);
   }

}
