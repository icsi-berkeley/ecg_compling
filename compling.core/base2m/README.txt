===================================
 base2m : syntactic+morphological grammar of English
===================================

14 June 2009, Nathan Schneider (nschneid@cs.cmu.edu).

Expanded from the 'base2' English syntactic grammar to include morphological constructions as well. For the purposes of the syntactic vs. morphological parsers, this is actually split up into two overlapping grammars. Loaded separately into the ECG Workbench, both grammars should have no errors, though only the syntactic one can be used for parsing in the Workbench. For the most part, changes from base2 are marked with "nschneid" in the file header or inline comments.

For more information, see written description of the base2m grammar, "Towards an ECG implementation of morphological compositionality and (sub)regularity in English." The morphological analyzer has not yet been released; contact the author for more information.


Grammar files/directories:
---------------------

base2m-morph.prefs : Points to grammar files for the morphological parser, which operates on individual words.

base2m-phrasal.prefs : Points to grammar files for the syntactic parser, which operates on sentences and cannot handle morphologically complex constructions.

base2m-core/ : Files shared by both grammars.

base2m-morphonly/ : Files used by the morphological grammar only. Including these in the other grammar causes the syntactic parser to break. A list of morphological construction names (organized by file) is in morphcxnindex.txt.

base2m-phrasalonly/ : Files used by the syntactic grammar only. Includes generatedwords.grm, which contains syntactic parser-compatible versions of the analyses produced by the morphological analyzer for certain words.


Diagrams:
--------

Included along with grammar files are diagrams showing some portions of the construction and schema hierarchies. These are specified in .dotgraph files; from these, the Graphviz 'dot' tool was used to generate .pdf files for visualization.

base2m-core/
   grammaticalschemas_AgreementFeatures.{dotgraph,pdf}
   morphschemas_MorphForm.{dotgraph,pdf}
   morphschemas.{dotgraph,pdf}
   verb_FiniteOrNonFinite.{dotgraph,pdf}

base2m-morphonly/
   nounmorphology.{dotgraph,pdf}
   verbmorphology.{dotgraph,pdf}
   verbmorphology-small.{dotgraph,pdf}
