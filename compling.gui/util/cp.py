'''
Created on Apr 27, 2011

@author: lucag
'''

from java.lang import System

CP = 'java.class.path'

def setclasspath():	
	cp = System.getProperty(CP).split(';')
	cp.insert(0, 'c:\\workspace\\compling.gui.grammargui\\bin')
	System.setProperty(CP, ';'.join(cp))
	
if __name__ == '__main__':
	setclasspath()
