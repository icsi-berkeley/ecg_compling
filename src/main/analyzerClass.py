""" Proxy for Analyzer. Used by "main" to connect with Analyzer object.
"""
try:
    # Python 2?
    from xmlrpclib import ServerProxy, Fault  # @UnresolvedImport @UnusedImport
except:
    # Therefore it must be Python 3.
    from xmlrpc.client import ServerProxy, Fault

from feature import StructJSONEncoder, as_featurestruct

class Analyzer(object):
    """A proxy for the Analyzer. 
    Note: It assumes the server is running with the right grammar
    """
    def __init__(self, url):
        self.analyzer = ServerProxy(url, encoding='utf-8') 
        
    def parse(self, sentence):        
        return [as_featurestruct(r, s) for r, s in self.analyzer.parse(sentence)]
    
    def issubtype(self, typesystem, child, parent):
        return self.analyzer.issubtype(typesystem, child, parent)