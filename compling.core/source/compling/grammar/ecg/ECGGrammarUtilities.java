package compling.grammar.ecg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

// import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;


import java_cup.runtime.Symbol;
import compling.context.ContextModel;
import compling.grammar.GrammarException;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.MapPrimitive;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.Grammar.Situation;
import compling.grammar.ecg.ecgreader.ECGReader;
import compling.grammar.ecg.ecgreader.Yylex;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.ontology.OWLOntology;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileUtils;
import compling.util.fileutil.TextFileLineIterator;

public class ECGGrammarUtilities {

  public static abstract class ConstructionParameters {

    // public ConstructionParameters(Construction c);

    public abstract double constituentLocalCost(String constituentName);

    public abstract double constituentOmittedCost(String constituentName);

    public abstract double constituentNonLocalCost(String constituentName);

    public abstract double constituentTypeFillerCost(String constituentName, String fillerType);

  }

  public static interface ECGGrammarFormatter {

    public String format(Grammar g);

    public String format(Construction c);

    public String format(Schema s);

    public String format(MapPrimitive m);

    public String format(Situation s);
  }

  public static class SimpleGrammarPrinter implements ECGGrammarFormatter {

    public String format(Grammar g) {
      StringBuffer sb = new StringBuffer();
      for (Schema schema : g.getAllSchemas()) {
        sb.append(formatSchema(schema)).append("\n");
      }
      for (Construction cxn : g.getAllConstructions()) {
        sb.append(formatCxn(cxn)).append("\n");
      }
      return sb.toString();
    }

    public String format(Construction c) {
      return formatCxn(c).toString();
    }

    private StringBuffer formatCxn(Construction c) {
      StringBuffer sb = new StringBuffer();
      String kind = c.getKind().equals(ECGConstants.ABSTRACT) ? "GENERAL " : "";
      sb.append(kind).append("CONSTRUCTION ");
      sb.append(c.getName()).append("\n");
      if (c.getParents().size() > 0) {
        sb.append("  ").append("subcase of ");
        for (String parent : c.getParents()) {
          sb.append(parent).append(" ");
        }
        sb.append("\n");
      }
      if (c.getConstructionalBlock() != null) {
        sb.append(formatBlock(c.getConstructionalBlock(), c.getName()));
      }
      if (c.getFormBlock() != null) {
        sb.append(formatBlock(c.getFormBlock(), c.getName()));
      }
      if (c.getMeaningBlock() != null) {
        sb.append(formatBlock(c.getMeaningBlock(), c.getName()));
      }
      return sb;
    }

    public String format(Schema s) {
      return formatSchema(s).toString();
    }

    public String format(MapPrimitive m) {
      return formatMap(m).toString();
    }

    public String format(Situation s) {
      return formatSituation(s).toString();
    }

    private StringBuffer formatSchema(Schema s) {
      StringBuffer sb = new StringBuffer("schema ");
      sb.append(s.getName()).append("\n");
      if (s.getParents().size() > 0) {
        sb.append("  ").append("subcase of ");
        for (String parent : s.getParents()) {
          sb.append(parent).append(" ");
        }
        sb.append("\n");
      }
      if (s.getContents() != null) {
        sb.append(formatBlock(s.getContents(), s.getName()));
      }
      return sb;
    }

    private StringBuffer formatMap(MapPrimitive m) {
      StringBuffer sb = new StringBuffer("map ");
      sb.append(m.getName()).append("\n");
      if (m.getParents().size() > 0) {
        sb.append("  ").append("subcase of ");
        for (String parent : m.getParents()) {
          sb.append(parent).append(" ");
        }
        sb.append("\n");
      }
      if (m.getContents() != null) {
        sb.append(formatBlock(m.getContents(), m.getName()));
      }
      return sb;
    }

    private StringBuffer formatSituation(Situation s) {
      StringBuffer sb = new StringBuffer("situation ");
      sb.append(s.getName()).append("\n");
      if (s.getParents().size() > 0) {
        sb.append("  ").append("subcase of ");
        for (String parent : s.getParents()) {
          sb.append(parent).append(" ");
        }
        sb.append("\n");
      }
      if (s.getContents() != null) {
        sb.append(formatBlock(s.getContents(), s.getName()));
      }
      return sb;
    }

    private StringBuffer formatBlock(Block b, String containerName) {
      StringBuffer sb = new StringBuffer();
      if (b.getKind().equals(ECGConstants.CONTENTS) == false) {
        sb.append("  ").append(b.getKind());
        if (b.getType() != null) {
          sb.append(": ").append(b.getType());
          if (b.getBlockTypeTypeSystem() != null) {
            sb.append("@").append(b.getBlockTypeTypeSystem().getName());
          }
        }
        sb.append("\n");
      }
      for (Role r : b.getEvokedElements()) {
        sb.append("    evokes ").append(r.getTypeConstraint()).append(" as ").append(r.getName());
        if (!r.getSource().equals(containerName)) {
          sb.append("  \t\t\t//inherited from ").append(r.getSource());
        }
        sb.append("\n");
      }
      // if (b.getEvokedElements().size() > 0){sb.append("\n");}
      if (b.getElements().size() > 0) {
        if (b.getKind().equals(ECGConstants.CONSTRUCTIONAL)) {
          sb.append("    constituents\n");
        }
        if (b.getKind().equals(ECGConstants.MEANING) || b.getKind().equals(ECGConstants.CONTENTS)) {
          sb.append("    roles\n");
        }
        for (Role r : b.getElements()) {
          sb.append("        ");
          if (r.getSpecialField() == ECGConstants.OPTIONAL) {
            sb.append("optional ");
          }
          sb.append(r.getName());
          if (r.getTypeConstraint() != null) {
            sb.append(": ").append(r.getTypeConstraint());
          }
          if (r.getSpecialField() != ECGConstants.OPTIONAL && r.getSpecialField() != "") {
            sb.append(" [").append(r.getSpecialField()).append("] ");
          }
          if (!r.getSource().equals(containerName)) {
            sb.append("  \t\t\t//inherited from ").append(r.getSource());
          }
          sb.append("\n");
        }
      }
      if (b.getConstraints().size() > 0) {
        sb.append("    constraints\n");
        for (Constraint c : b.getConstraints()) {
          sb.append("        ");
          if (c.overridden()) {
            sb.append("ignore ");
          }
          sb.append(c);
          if (!c.getSource().equals(containerName)) {
            sb.append("  \t\t\t//inherited from ").append(c.getSource());
          }
          sb.append("\n");
        }
      }
      return sb;
    }

  }

  public static class TexGrammarPrinter implements ECGGrammarFormatter {
    public String format(Grammar g) {
      StringBuffer sb = new StringBuffer();
      for (Schema schema : g.getAllSchemas()) {
        sb.append(formatSchema(schema)).append("\n");
      }
      for (Construction cxn : g.getAllConstructions()) {
        sb.append(formatCxn(cxn)).append("\n");
      }
      return sb.toString();
    }

    public String format(Construction c) {
      return formatCxn(c).toString();
    }

    public StringBuffer formatCxn(Construction c) {
      StringBuffer sb = new StringBuffer();
      if (c.getKind() == ECGConstants.ABSTRACT) {
        sb.append("\\abscxndef{");
      }
      else {
        sb.append("\\cxndef{");
      }
      sb.append(c.getName()).append("}{\n");
      if (c.getParents().size() > 0) {
        sb.append("  ").append("\\subcaseof{ ");
        for (String parent : c.getParents()) {
          sb.append(parent).append(" ");
        }
        sb.append("}\n");
      }
      if (c.getConstructionalBlock() != null) {
        sb.append(formatBlock(c.getConstructionalBlock(), c.getName()));
      }
      if (c.getFormBlock() != null) {
        sb.append(formatBlock(c.getFormBlock(), c.getName()));
      }
      if (c.getMeaningBlock() != null) {
        sb.append(formatBlock(c.getMeaningBlock(), c.getName()));
      }
      sb.append("}\n\n");
      return sb;
    }

    public String format(Schema s) {
      return formatSchema(s).toString();
    }

    public String format(MapPrimitive m) {
      return formatMap(m).toString();
    }

    public String format(Situation s) {
      return formatSituation(s).toString();
    }

    private StringBuffer formatSchema(Schema s) {
      StringBuffer sb = new StringBuffer("\\schemadef{ ");
      sb.append(s.getName()).append(" }{\n");
      if (s.getParents().size() > 0) {
        sb.append("  ").append("\\subcaseof{ ");
        for (String parent : s.getParents()) {
          sb.append(parent).append(" ");
        }
        sb.append("}\n");
      }
      if (s.getContents() != null) {
        sb.append(formatBlock(s.getContents(), s.getName()));
      }
      sb.append("\n");
      return sb;
    }

    private StringBuffer formatMap(MapPrimitive m) {
      StringBuffer sb = new StringBuffer("\\mapdef{ ");
      sb.append(m.getName()).append(" }{\n");
      if (m.getParents().size() > 0) {
        sb.append("  ").append("\\subcaseof{ ");
        for (String parent : m.getParents()) {
          sb.append(parent).append(" ");
        }
        sb.append("}\n");
      }
      if (m.getContents() != null) {
        sb.append(formatBlock(m.getContents(), m.getName()));
      }
      sb.append("\n");
      return sb;
    }

    private StringBuffer formatSituation(Situation s) {
      StringBuffer sb = new StringBuffer("\\situationdef{ ");
      sb.append(s.getName()).append(" }{\n");
      if (s.getParents().size() > 0) {
        sb.append("  ").append("\\subcaseof{ ");
        for (String parent : s.getParents()) {
          sb.append(parent).append(" ");
        }
        sb.append("}\n");
      }
      if (s.getContents() != null) {
        sb.append(formatBlock(s.getContents(), s.getName()));
      }
      sb.append("\n");
      return sb;
    }

    private boolean hasLocalStructure(Block b, String containerName) {
      for (Role r : b.getEvokedElements()) {
        if (r.getSource().equals(containerName)) {
          return true;
        }
      }
      for (Role r : b.getElements()) {
        if (r.getSource().equals(containerName)) {
          return true;
        }
      }
      for (Constraint r : b.getConstraints()) {
        if (r.getSource().equals(containerName)) {
          return true;
        }
      }
      return false;
    }

    private StringBuffer formatBlock(Block b, String containerName) {
      StringBuffer sb = new StringBuffer();
      boolean hasLocalStructure = hasLocalStructure(b, containerName);
      if (!hasLocalStructure && b.getType().equals(ECGConstants.UNTYPED)) {
        // do nothing
      }
      else if (!hasLocalStructure && !b.getType().equals(ECGConstants.UNTYPED)) {
        if (b.getKind().equals(ECGConstants.CONSTRUCTIONAL)) {
          sb.append("  \\typedconstructionaltag{" + b.getType() + "}\n");
        }
        else if (b.getKind().equals(ECGConstants.FORM)) {
          sb.append("  \\typedformtag{" + b.getType() + "}\n");
        }
        else if (b.getKind().equals(ECGConstants.MEANING)) {
          sb.append("  \\typedmeaningtag{" + b.getType() + "}\n");
        }
      }
      else { // hasLocalStructure
        if (b.getKind().equals(ECGConstants.CONSTRUCTIONAL)) {
          if (b.getType().equals(ECGConstants.UNTYPED)) {
            sb.append("  \\cxnalblock{\n");
          }
          else {
            sb.append("  \\typedcxnalblock{" + b.getType() + "}{\n");
          }

        }
        else if (b.getKind().equals(ECGConstants.FORM)) {
          if (b.getType().equals(ECGConstants.UNTYPED)) {
            sb.append("  \\formblock{\n");
          }
          else {
            sb.append("  \\typedformblock{" + b.getType() + "}{\n");
          }

        }
        else if (b.getKind().equals(ECGConstants.MEANING)) {
          if (b.getType().equals(ECGConstants.UNTYPED)) {
            sb.append("  \\meaningblock{\n");
          }
          else {
            sb.append("  \\typedmeaningblock{" + b.getType() + "}{\n");
          }
        }

        for (Role r : b.getEvokedElements()) {
          if (r.getSource().equals(containerName)) {
            sb.append("    \\evokes{ ").append(r.getTypeConstraint().getType()).append(" }{ ").append(r.getName())
                    .append(" } \n");
          }
        }
        if (b.getElements().size() > 0) {
          boolean hasLocalRoles = false;
          for (Role r : b.getElements()) {
            if (r.getSource().equals(containerName)) {
              hasLocalRoles = true;
            }
          }
          if (hasLocalRoles) {
            if (b.getKind().equals(ECGConstants.CONSTRUCTIONAL)) {
              sb.append("    \\constituentsblock{\n");
            }
            if (b.getKind().equals(ECGConstants.MEANING) || b.getKind().equals(ECGConstants.CONTENTS)) {
              sb.append("    \\rolesblock{\n");
            }
            for (Role r : b.getElements()) {
              if (!r.getSource().equals(containerName)) {
                continue;
              }
              sb.append("        ");
              if (r.getSpecialField() == ECGConstants.OPTIONAL) {
                sb.append("optional ");
              }

              if (r.getTypeConstraint() != null) {
                sb.append("\\typect{ ").append(r.getName()).append(" }{ ").append(r.getTypeConstraint().getType())
                        .append(" }");
              }
              else {
                sb.append("\\role{ ").append(r.getName()).append(" } ");
              }
              sb.append("\n");
            }
            sb.append("    }\n");
          }
        }
        if (b.getConstraints().size() > 0) {
          boolean hasLocalConstraints = false;
          for (Constraint c : b.getConstraints()) {
            if (c.getSource().equals(containerName)) {
              hasLocalConstraints = true;
            }
          }
          if (hasLocalConstraints) {
            sb.append("    \\constraintsblock{\n");
            for (Constraint c : b.getConstraints()) {
              if (!c.getSource().equals(containerName)) {
                continue;
              }
              sb.append("      ");
              if (c.overridden()) {
                sb.append("\\override{");
              }
              sb.append("\\constraint{ ");
              sb.append(c.getArguments().get(0));
              if (c.getOperator().equals(ECGConstants.IDENTIFY)) {
                sb.append(" \\idop ");
              }
              else if (c.getOperator().equals(ECGConstants.ASSIGN)) {
                sb.append(" \\fillop ");
              }
              else if (c.getOperator().equals(ECGConstants.MEETS)) {
                sb.append(" \\meets ");
              }
              else if (c.getOperator().equals(ECGConstants.BEFORE)) {
                sb.append(" \\before ");
              }
              else {
                sb.append(" ").append(c.getOperator()).append(" ");
              }
              if (c.isAssign()) {
                sb.append(c.getValue());
              }
              else {
                sb.append(c.getArguments().get(1));
              }
              sb.append(" }"); // \constraint
              if (c.overridden()) {
                sb.append("}");
              } // \override
              sb.append("\n");
            }
            sb.append("    }\n"); // \constraintsblock
          }
        }
        sb.append("  }\n"); // \cxnalblock, \formblock, etc.
      }
      return sb;
    }
  }

  public static class GrammarGraphPrinter implements ECGGrammarFormatter {

    private boolean printCxn = true;
    private boolean printLex = false;

    public GrammarGraphPrinter() {
    }

    public GrammarGraphPrinter(boolean printConstructionHierarchy, boolean printLexicalCxns) {
      printCxn = printConstructionHierarchy;
      printLex = printLexicalCxns;
    }

    public String format(Grammar g) {
      LCPGrammarWrapper w = new LCPGrammarWrapper(g);
      StringBuffer sb = new StringBuffer();
      sb.append("digraph grammar\n{\n");

      if (printCxn) {
        for (Construction cxn : g.getAllConstructions()) {
          if (!printLex && w.isLexicalConstruction(cxn)) {
            // do nothing
          }
          else {
            for (String parent : cxn.getParents()) {
              sb.append(parent).append(" -> ").append(cxn.getName()).append(" [color=\"darkgreen\"];\n");
            }
          }
        }

      }
      else {
        for (Schema schema : g.getAllSchemas()) {
          for (String parent : schema.getParents()) {
            sb.append(parent).append(" -> ").append(schema.getName()).append(" [color=\"sienna\"];\n");
          }
        }
      }
      sb.append("}\n");

      return sb.toString();
    }

    public String format(Construction c) {
      return "";
    }

    public String format(Schema s) {
      return "";
    }

    public String format(MapPrimitive m) {
      return "";
    }

    public String format(Situation s) {
      return "";
    }
  }

  public static String getLexemeFromLexicalConstruction(Construction lexicalConstruction) {
    for (Constraint constraint : lexicalConstruction.getFormBlock().getConstraints()) {
      if (constraint.getOperator().equals(ECGConstants.ASSIGN)
              && constraint.getArguments().get(0).toString().indexOf("orth") != -1) {
        return constraint.getValue();
      }
    }

    // Look for an assignment to 'orth' in the schema
    if (lexicalConstruction.getFormBlock().getTypeConstraint().getType() != ECGConstants.UNTYPED) {
      for (Constraint constraint : lexicalConstruction.getSchemaTypeSystem()
              .get(lexicalConstruction.getFormBlock().getType()).getContents().getConstraints()) {
        if (constraint.getOperator().equals(ECGConstants.ASSIGN)
                && constraint.getArguments().get(0).toString().indexOf("orth") != -1) {
          return constraint.getValue();
        }
      }
    }
    throw new GrammarException("Not a lexical construction: " + lexicalConstruction.getName(), lexicalConstruction);
  }


  // @author: seantrott 11/13/14, used for generating Lemma-->Construction hashmap.
  public static String getLemmaFromLexicalConstruction(Construction lexicalConstruction) {
    for (Constraint constraint : lexicalConstruction.getFormBlock().getConstraints()) {
      if (constraint.getOperator().equals(ECGConstants.ASSIGN)
          && constraint.getArguments().get(0).toString().indexOf("lemma") != -1) {
        return constraint.getValue();
      }
    }
        // Look for an assignment to 'orth' in the schema
    if (lexicalConstruction.getFormBlock().getTypeConstraint().getType() != ECGConstants.UNTYPED) {
      for (Constraint constraint : lexicalConstruction.getSchemaTypeSystem()
              .get(lexicalConstruction.getFormBlock().getType()).getContents().getConstraints()) {
        if (constraint.getOperator().equals(ECGConstants.ASSIGN)
                && constraint.getArguments().get(0).toString().indexOf("lemma") != -1) {
          return constraint.getValue();
        }
      }
    }
    throw new GrammarException("Not a lexical construction: " + lexicalConstruction.getName(), lexicalConstruction);



  }

  public static Grammar read(AnalyzerPrefs preferences) throws IOException, TypeSystemException {

    File base = preferences.getBaseDirectory();

    String ext = preferences.getSetting(AP.GRAMMAR_EXTENSIONS);
    if (ext == null) {
      ext = "ecg cxn sch grm";
    }
    
    String packageName = preferences.getSetting(AP.PACKAGE_NAME);
    System.out.println(packageName);
    
    /** Added by @seantrott for adding imported packages. Testing. */
    List<String> importPaths = preferences.getList(AP.IMPORT_PATHS);
    List<File> importFiles = FileUtils.getFilesUnder(base, importPaths, new ExtensionFileFilter(ext));
    Grammar grammarImport = new Grammar();
    System.out.println(importFiles);
    if (!importFiles.isEmpty()) {

	    ext = preferences.getSetting(AP.ONTOLOGY_EXTENSIONS);
	    if (ext == null) {
	      ext = "def inst ont";
	    }
	    String[] extsI = ext.split(" ");
	    String encSettingI = preferences.getSetting(AP.FILE_ENCODING);
	    Charset encodingI = Charset.forName(encSettingI != null ? encSettingI : ECGConstants.DEFAULT_ENCODING);
	    String ontologyImport = preferences.getSetting(AP.ONTOLOGY_TYPE);
	    if (ontologyImport != null && ontologyImport.equalsIgnoreCase(AnalyzerPrefs.OWL_TYPE)) {
	      grammarImport = read(importFiles, OWLOntology.fromPreferences(preferences).getTypeSystem(), encodingI);
	    }
	    else {
	      List<String> ontPaths = preferences.getList(AP.ONTOLOGY_PATHS);
	      List<File> ontFiles = FileUtils.getFilesUnder(base, ontPaths, new ExtensionFileFilter(ext));
	
	      ContextModel contextModel;
	      if (ontFiles.size() == 1) {
	        contextModel = new ContextModel(ontFiles.get(0).getAbsolutePath());
	      }
	      else {
	        contextModel = new ContextModel(ontFiles, extsI[0], extsI[1]);
	      }
	      grammarImport = read(importFiles, contextModel, encodingI);
	      grammarImport.update();
	    }
    }
    // 
    
    ext = preferences.getSetting(AP.GRAMMAR_EXTENSIONS);
    if (ext == null) {
      ext = "ecg cxn sch grm";
    }
    
    List<String> grammarPaths = preferences.getList(AP.GRAMMAR_PATHS);
    List<File> grammarFiles = FileUtils.getFilesUnder(base, grammarPaths, new ExtensionFileFilter(ext));
    System.out.println(grammarFiles);

    ext = preferences.getSetting(AP.ONTOLOGY_EXTENSIONS);
    if (ext == null) {
      ext = "def inst ont";
    }
    String[] exts = ext.split(" ");

    String encSetting = preferences.getSetting(AP.FILE_ENCODING);
    Charset encoding = Charset.forName(encSetting != null ? encSetting : ECGConstants.DEFAULT_ENCODING);

    Grammar grammar;
    String ontologyType = preferences.getSetting(AP.ONTOLOGY_TYPE);
    if (ontologyType != null && ontologyType.equalsIgnoreCase(AnalyzerPrefs.OWL_TYPE)) {
      grammar = read(grammarFiles, OWLOntology.fromPreferences(preferences).getTypeSystem(), encoding);
    }
    else {
      List<String> ontPaths = preferences.getList(AP.ONTOLOGY_PATHS);
      List<File> ontFiles = FileUtils.getFilesUnder(base, ontPaths, new ExtensionFileFilter(ext));

      ContextModel contextModel;
      if (ontFiles.size() == 1) {
        contextModel = new ContextModel(ontFiles.get(0).getAbsolutePath());
      }
      else {
        contextModel = new ContextModel(ontFiles, exts[0], exts[1]);
      }
      grammar = read(grammarFiles, contextModel, encoding);
      if (!importFiles.isEmpty()) {
    	  grammar = addGrammar(grammar, grammarImport);  // @seantrott, testing. Function to add imported packages.
      }
      grammar.update();
    }

    List<String> paramPaths = preferences.getList(AP.GRAMMAR_PARAMS_PATHS);
    if (!paramPaths.isEmpty()) {
      ext = preferences.getSetting(AP.GRAMMAR_PARAMS_LOCALITY_EXTENSION);
      if (ext == null) {
        ext = "loc";
      }
      List<File> locParamFiles = FileUtils.getFilesUnder(base, paramPaths, new ExtensionFileFilter(ext));
      if (!locParamFiles.isEmpty()) {
        updateLocality(grammar, locParamFiles);
      }
    }
    grammar.setPrefs(preferences);
    return grammar;
  }

  private static Grammar addGrammar(Grammar g, Grammar gImport) {
	g.setContextModel(gImport.getContextModel());
	//grammar.setOntologyTypeSystem(grammar2.getOntologyTypeSystem());
	for (Grammar.Schema schema : gImport.getAllSchemas()) {
		if (g.getImport().contains(schema.getPackage())) {
			g.addSchema(g.new Schema(schema.getName(), schema.getKind(), schema.getParents(), schema.getContents()));
		}
	}
	for (Grammar.Construction cxn : gImport.getAllConstructions()) {
		if (g.getImport().contains(cxn.getPackage())) {
			g.addConstruction(g.new Construction(cxn.getName(), cxn.getKind(), cxn.getParents(), 
															 cxn.getFormBlock(), cxn.getMeaningBlock(), cxn.getConstructionalBlock()));
		}

	}
	return g;
  }
  
  protected static void updateLocality(Grammar grammar, List<File> localityParamFiles) {
    for (File localityParamFile : localityParamFiles) {
      TextFileLineIterator lines = new TextFileLineIterator(localityParamFile);
      updateLocality(grammar, lines);
    }
  }

  public static void updateLocality(Grammar grammar, TextFileLineIterator lines) {
    while (lines.hasNext()) {
      String line = lines.next();
      StringTokenizer st = new StringTokenizer(line);
      String tmp = st.nextToken();
      String[] structureAndRole = tmp.split("\\.");

      Construction cxn = grammar.getConstruction(structureAndRole[0]);
      Role role = getRole(grammar, structureAndRole[0], structureAndRole[1]);

      if (cxn != null && role != null) {

        List<Integer> params = new ArrayList<Integer>();
        while (st.hasMoreTokens()) {
          params.add(Integer.valueOf(st.nextToken()));
        }
        if (params.size() != 4) {
          throw new GrammarException("Error in parameter file format: insufficient locality parameters provided for "
                  + structureAndRole[0] + "." + structureAndRole[1]);
        }

        String probString = calculateLocalityProbabilities(cxn, role, params.get(0), params.get(1), params.get(2),
                params.get(3));
        if (probString != "") {
          String probability = "[ " + probString + "]";
          if (role.getSpecialField() != null && role.getSpecialField().length() > 0) {
            String existingField = role.getSpecialField();
            role.setSpecialField(existingField.replaceFirst("\\[.*\\]", probability.toString()));
          }
          else {
            role.setSpecialField(probability.toString());
          }
        }
      }
    }
  }

  protected static String calculateLocalityProbabilities(Construction cxn, Role role, int local, int nonlocal,
          int omitted, int unfilled) {

    if (cxn.getOptionals().contains(role)) {
      if ((local + unfilled) == 0) {
        return "";
      }
      double expressedP = ((double) local) / (local + unfilled);
      return String.valueOf(expressedP);
    }
    else {
      double expressedP = (local + nonlocal + omitted) == 0 ? (1 - ECGConstants.DEFAULTOMISSIONPROBABILITY)
              : ((double) local + nonlocal) / (local + nonlocal + omitted);
      double localP = (local + nonlocal) == 0 ? ECGConstants.DEFAULTLOCALPROBABILITY : ((double) local)
              / (local + nonlocal);

      if (expressedP != (1 - ECGConstants.DEFAULTOMISSIONPROBABILITY) || localP != ECGConstants.DEFAULTLOCALPROBABILITY) {
        return String.valueOf(expressedP) + " " + String.valueOf(localP);
      }
      return "";
    }
  }

  protected static Role getRole(Grammar grammar, String cxnName, String roleName) {
    Construction cxn = grammar.getConstruction(cxnName);
    if (cxn == null) {
      return null;
    }
    else {
      for (Role role : cxn.getConstructionalBlock().getElements()) {
        if (role.getName().equals(roleName)) {
          return role;
        }
      }
    }
    return null;
  }

  public static Grammar read(String prefsFile) throws IOException, TypeSystemException {
    return read(new AnalyzerPrefs(prefsFile, Charset.forName(ECGConstants.DEFAULT_ENCODING)));
  }

  public static Grammar read(String path, String extensions, String ontologyFile) {
    ContextModel contextModel = null;
    if (ontologyFile != null && !ontologyFile.equals("")) {
      contextModel = new ContextModel(ontologyFile);
    }
    List<File> files = FileUtils.getFilesUnder(path, new ExtensionFileFilter(extensions));

    return read(files, contextModel);
  }

  public static Grammar read(String path, String extensions, ContextModel contextModel) {
    List<File> files = FileUtils.getFilesUnder(path, new ExtensionFileFilter(extensions));
    return read(files, contextModel);
  }

  public static Grammar read(List<File> files, ContextModel contextModel) {
    return read(files, contextModel, Charset.forName(ECGConstants.DEFAULT_ENCODING));
  }

  public static Grammar read(List<File> files, ContextModel contextModel, Charset encoding) {
    Grammar g = new Grammar();
    g.setContextModel(contextModel);

    read(files, g, encoding);
    return g;
  }

  public static Grammar read(List<File> files, TypeSystem<? extends TypeSystemNode> ontologyTypeSystem, Charset encoding) {
    Grammar grammar = new Grammar();
    grammar.setOntologyTypeSystem(ontologyTypeSystem);

    read(files, grammar, encoding);

    return grammar;
  }

  private static void read(List<File> files, Grammar grammar, Charset encoding) {
    for (File file : files) {
      try {
        Yylex scanner = new Yylex(new InputStreamReader(new FileInputStream(file), encoding));

        // Scan the grammar file (use ECG encoding for the file)
        String filename = file.getName();

        // System.out.println("processing " + filename);

        scanner.file = filename;
        ECGReader reader = new ECGReader(scanner);
        reader.file = filename;
        reader.setGrammar(grammar);
        try {
          reader.parse();
        }
        catch (GrammarException e) {
          throw e;
        }
        catch (Exception e) {
          throw new GrammarException(
                  "There were fatal errors while reading in the grammar:\n"
                          + "-----------------------------------------------------------------------------------------------------\n"
                          + reader.getErrorLog() + "\n\n" + scanner.getScannerErrors());
        }
        if (reader.getErrorLog().length() > 0) {
          // this is the case where the errors were structurally
          // recoverable, but the grammar is still broken
          throw new GrammarException(
                  "The constructions and schemas in your grammar had these errors:"
                          + "\n-----------------------------------------------------------------------------------------------------\n"
                          + reader.getErrorLog() + "\n\n" + scanner.getScannerErrors());
        }
        if (scanner.getScannerErrors().length() > 0) {
          // this is the case where the only errors were scanner issues
          throw new GrammarException(
                  "The constructions and schemas in your grammar had these errors:"
                          + "\n-----------------------------------------------------------------------------------------------------\n"
                          + scanner.getScannerErrors());
        }

      }
      catch (FileNotFoundException e1) {
        // e1.printStackTrace();
        throw new GrammarException("Grammar File Not Found:\n", e1);
      }
      // catch (UnsupportedEncodingException e2) {
      // throw new GrammarException("Grammar file encoding not supported:\n",
      // e2);
      // }
    }
  }

  public static Grammar read(StringBuffer grammar, ContextModel contextModel) {
    Grammar g = new Grammar();
    g.setContextModel(contextModel);

    ECGReader ecgr;
    Yylex scanner;
    scanner = new Yylex(new BufferedReader(new StringReader(grammar.toString())));
    scanner.file = "StringBuffer";
    ecgr = new ECGReader(scanner);
    ecgr.file = "StringBuffer";
    ecgr.setGrammar(g);
    try {
      ecgr.parse();
    }
    catch (Exception e) {
      throw new GrammarException("Terminal Error: Cannot read grammar.", e);
    }
    return g;
  }

  public static void main(String[] args) throws IOException, TypeSystemException {
    String ontFile = null;
    Grammar grammar = null;
    if (args.length > 1) {
      ontFile = args[1];
      grammar = read(args[0], "ecg cxn sch grm", ontFile);
    }
    else if (args.length == 1) {
      grammar = read(args[0]);
    }

    System.out.println(grammar);
    // System.out.println(new TexGrammarPrinter().format(grammar));

    /*
     * String existingField = "OPTIONAL [ 0.5 0.7 ]"; existingField =
     * existingField.replaceFirst("\\[.*\\]", "[ 0.77 0.88 ]");
     * System.out.println(existingField);
     */
  }

}
