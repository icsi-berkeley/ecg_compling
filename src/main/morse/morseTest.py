from pymorse import Morse

rob = Morse().pr2
rob.motion2.goto(5.0, 4.0, 0.0)
"""
rob.motion2.publish({'x' : 10.0, 'y': 5.0, 'z': 0.0,
                             'tolerance' : 0.5, 'speed' : 1.0})

"""