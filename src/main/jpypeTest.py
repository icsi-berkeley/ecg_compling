import sys

dll = {'linux': '/jre/lib/amd64/server/libjvm.so', 
       'darwin': '/jre/lib/server/libjvm.dylib',
       'win32': '/jre/bin/server/jvm.dll'}
import jpype, os  # @UnresolvedImport
from jpype import *
jpype.startJVM(os.environ['JAVA_HOME'] + dll[sys.platform],
               '-ea', '-Xmx5g', '-Djava.class.path=lib/compling.core.jar')
compling = jpype.JPackage('compling')
SlotChain = getattr(compling.grammar.unificationgrammar, 'UnificationGrammar$SlotChain')
getParses = compling.gui.util.Utils.getParses
ParserException = jpype.JException(compling.parser.ParserException)  # @UnusedVariable
ECGAnalyzer = compling.parser.ecgparser.ECGAnalyzer
getDfs = compling.grammar.unificationgrammar.FeatureStructureUtilities.getDfs  # @UnusedVariable