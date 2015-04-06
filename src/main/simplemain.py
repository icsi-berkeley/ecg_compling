from utils import update, Struct
from feature import StructJSONEncoder 
from os.path import basename
from specializerTools import *
from solver import NullProblemSolver, MorseProblemSolver, XnetProblemSolver, MockProblemSolver, ClarificationError
from specializer import *
from analyzerClass import Analyzer
from json import dumps, loads

analyzer = Analyzer('http://localhost:8090')
specializer = RobotSpecializer()
solver = MorseProblemSolver()



u = "robot1, move to the blue box"
def solve_loop(analyses):
	for fs in analyses:
		ntuple = specializer.specialize(fs)
		json_ntuple = dumps(ntuple, cls=StructJSONEncoder, indent=2)
		solver.solve(json_ntuple)
		break

while True:
	utterance = input("> ")
	if utterance == "q":
		solver.close()
		break
	analyses = analyzer.parse(utterance)
	solve_loop(analyses)

