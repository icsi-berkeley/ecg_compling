""" A simple class to read in the MAPPING file and generate a data structure
of mappings between the language ontology and application ontology.

@author: <seantrott.icsi.berkeley.edu>

For now, the mapping_path variable is hard-coded. However, this should be generated automatically when the user 
passes in the path of the preferences file. E.g., 'compRobots.prefs'. """

mapping_path = "/Users/seantrott/icsi/compling/compRobots/robot.mappings"

class MappingReader(object):
    def __init__(self):
        self.mappings = dict()

    def read_file(self, filepath):
        lines = [line.strip() for line in open(filepath, "r")]
        for line in lines:
            contents = line.split(" :: ")
            ling_value = contents[0]
            app_value = contents[1]
            if "@" in ling_value:
                ling_value = ling_value.replace("@", "")
            if "$" in app_value:
                app_value = app_value.replace("$", "")
            self.mappings[ling_value] = app_value

    def get_mappings(self):
        return self.mappings

    









