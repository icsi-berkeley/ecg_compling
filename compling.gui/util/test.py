'''
Created on Apr 26, 2011

@author: lucag
'''

from compling.grammar.unificationgrammar import FeatureStructureSet, TypeSystem 
from compling.grammar.unificationgrammar.FeatureStructureSet import Slot
from compling.grammar.unificationgrammar.UnificationGrammar import SlotChain, TypeConstraint 

def test1():
   #		FeatureStructureSet dpk = new FeatureStructureSet();
   #		dpk.coindex(new SlotChain("m"), new SlotChain("s.m"));
   #		dpk.coindex(new SlotChain("m"), new SlotChain("k.m"));
   #		System.out.println("Original dpk:\n" + dpk); 
   
   dpk = FeatureStructureSet();
   dpk.coindex(SlotChain('m'), SlotChain('s.m'))	
   dpk.coindex(SlotChain('m'), SlotChain('k.m'))	
   
   print 'dpk:\n', dpk


def test2():
   dpk = FeatureStructureSet();
   dpk1 = dpk
   dpk2 = dpk.clone()
   
   print id(dpk), id(dpk1), id(dpk2)


def test3():
#      FeatureStructureSet dpk = new FeatureStructureSet();
#      dpk.coindex(new SlotChain("m"), new SlotChain("s.m"));
#      dpk.coindex(new SlotChain("m"), new SlotChain("k.m"));
#      System.out.println("Original dpk:\n" + dpk); 
   dpk = FeatureStructureSet()
   dpk.coindex(SlotChain('m'), SlotChain('s.m'))
   dpk.coindex(SlotChain('m'), SlotChain('k.m'))
   print 'Original dpk:\n', dpk
   
   rd1 = FeatureStructureSet()
   ref = SlotChain('referent')
   oc = SlotChain('ontological-category')
   rd1.coindex(ref, oc)
   print 'rd1:\n', rd1

   tc = TypeConstraint('RD', TypeSystem('Schema'))
   slot = dpk.getSlot(SlotChain('m'))
   slot.setTypeConstraint(tc)

   dpk.coindexAcrossFeatureStructureSets(SlotChain('m'), SlotChain(''), rd1)
   print 'dpk after coindexation with rd1:\n', dpk 

   rd2 = FeatureStructureSet()
   
   oc1 = SlotChain('m.ontological-category')
   rd2.addSlot(oc1)
   rd2.getSlot(oc1).setTypeConstraint(TypeConstraint('ball', TypeSystem('Ontology')))
   rd2.addSlot(SlotChain('m.referent'))
   print 'rd2 original:\n', rd2 
   
   dpk.coindexAcrossFeatureStructureSets(SlotChain(''), SlotChain(''), rd2)
   print 'dpk after coindexation with rd1:\n', dpk 
   
   
if __name__ == '__main__':
#	test1()
#	test2()
	test3()

