"""
.. The "main" module runs the whole program. It selects the Analyzer and Specializer to use,
    as well as the Problem Solver.

.. moduleauthor:: Sean Trott <seantrott.icsi.berkeley.edu>

"""



import sys, traceback
import copy
from copy import deepcopy
import pickle
import time
import json
from pprint import pprint
from feature import as_featurestruct
from json import dumps
from itertools import chain

try:
    # Python 2?
    from xmlrpclib import ServerProxy, Fault  # @UnresolvedImport @UnusedImport
except:
    # Therefore it must be Python 3.
    from xmlrpc.client import ServerProxy, Fault #@UnusedImport @UnresolvedImport @Reimport

from utils import update, Struct
from feature import StructJSONEncoder 
from os.path import basename
from specializerTools import *
from solver import NullProblemSolver, MorseProblemSolver, XnetProblemSolver, MockProblemSolver, ClarificationError
from specializer import *
from analyzerClass import Analyzer


def main_loop(analyzer, solver=NullProblemSolver(), specializer=RobotSpecializer(), 
              filter_predicate=None):
    """REPL-like thing. Should be reusable.
    """

    def handle_debug():
        debugging = open('src/main/specializer_debug_output.txt', 'a')
        #debugging.truncate()
        specializer.set_debug()
        print("Debug mode is", specializer.debug_mode)
        return debugging

    def prompt():
        while True:
            ans = input('Press q/Q to quit, d/D for Debug mode> ') # Python 3
            specialize = True
            if ans.lower() == 'd':
                specializer._output = handle_debug()
                specialize = False
            elif ans.lower() == 'names':
                solver.names()
                specialize = False
            if ans.lower() == 'q':
                solver.close()
                return
            elif ans.lower()== 'h':
                solver.test()
            elif ans and specialize:
                specializer._sentence = ans
                try:
                    yield analyzer.parse(ans)#this is a generator
                except Fault as err:
                    print('Fault', err)
                    if err.faultString == 'compling.parser.ParserException':
                        print("No parses found for '%s'" % ans)

    def write_file():
        sentence = specializer._sentence.replace(" ", "_").replace(",", "").replace("!", "").replace("?", "")
        t = str(time.time())
        generated = "src/main/json_tuples/" + sentence
        f = open(generated, "w")
        f.write(json_ntuple)

    count = 1
    for analyses in prompt():

        for fs in filter(filter_predicate, analyses):
            try:
                #resolve = specializer.reference_resolution(fs)          
                ntuple = specializer.specialize(fs)
                json_ntuple = dumps(ntuple, cls=StructJSONEncoder, indent=2)
                if specializer.debug_mode:
                    write_file()
                if specializer.needs_solve and ntuple != None:
                    while True:
                        try:
                            solver.solve(json_ntuple)
                            break
                        except ClarificationError as ce:
                            new_input = input(ce.message + " > ")
                            if new_input == "q":
                                solver.close()
                                return
                            specialized = specializer.specialize_np(analyzer.parse(new_input)[0], ce.ntuple, ce.cue) 
                            json_ntuple = dumps(specialized, cls=StructJSONEncoder, indent=2)
                            #solver.solve(json_ntuple)
                break
            except:
                print('Problem solving SemSpec #: %s' % count)
                print(analyses)#[count-1])
                #print(fs)
                #traceback.print_exc()
                if count == len(analyses):
                    print("Unable to solve any of the SemSpecs.")
                count += 1

            #break

"""        
def usage(args):
    print('Usage: %s [-s <problem solver>] [-a <all server URL>]' % basename(args[0]))
    sys.exit(-1)
"""

def usage(args, message):
    if message == "app":
        print("No Application specified. Apps are:")
        print(list(specializers.keys())) 
    elif message == "nonexistent":
        print("Application <" + str(args[0]) + "> does not exist. Apps are:")
        print(list(specializers.keys())) 
    elif message == "solverType":
        print("Solver <" + args[1] + "> for <" + args[0] + "> application does not exist. Solvers are:")
        print(list(solvers[args[0]].keys()))
    sys.exit(-1)

specializers = {'robot': RobotSpecializer}
solvers = {'robot': 
            {'mock': MockProblemSolver, 'morse': MorseProblemSolver}}
    
if __name__ == '__main__':
    # These contain default values for the options
    options = {'-s': 'null', 
               '-a': 'http://localhost:8090'}
    #options.update(dict(a.split() for a in sys.argv[1:] if a.startswith('-')))
               
    #if not all(o[1] in 'sa' for (o, _) in options.items()):
    #    usage(sys.argv)

    args = sys.argv[1:]
    if len(args) < 1:
        usage(args, 'app')
    elif not args[0] in specializers.keys():
        usage(args, "nonexistent")
    elif len(args) >= 2:
        solverType = args[1]
        if solverType in solvers[args[0]].keys():
            solver = solvers[args[0]][args[1]]
        else:
            usage(args, "solverType")
    else:
        solver = solvers[args[0]]['mock']   # This assumes there is always a "mock problem solver", which is text-based. May not be the case.

    analyzer = Analyzer('http://localhost:8090')
    main_loop(analyzer, specializer=specializers[args[0]](), solver=solver())
    
    #solver = dict(null=MockProblemSolver, morse=MorseProblemSolver, xnet=XnetProblemSolver)
    #main_loop(Analyzer(options['-a']), specializer=RobotSpecializer(), solver=solver[options['-s']]())
    sys.exit(0)


