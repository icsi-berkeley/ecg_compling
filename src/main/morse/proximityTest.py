from pymorse import Morse
simu = Morse()
r = simu.robot1_instance
p = r.proximity

r.motion.publish({'x' : -10.0, 'y': 0.0, 'z': 0.0,
                  'tolerance' : 0.5, 'speed' : 1.0})

r.motion.publish({'x' : 10.0, 'y': 0.0, 'z': 0.0,
                  'tolerance' : 0.5, 'speed' : 1.0})

r.motion.stop()