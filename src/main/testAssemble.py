def assemble_string(properties):
    ont = properties['type']
    attributes = ""
    for key, value in properties.items():   # Creates string of properties
        if key == "color" or key == "size":
            attributes += " " + value
        elif key == "locationDescriptor":
            attributes += str(ont) + " " + value["relation"] + " the" + assemble_string(value['objectDescriptor'])
    return str(attributes) + " " + str(ont)


s = {'objectDescriptor': {'givenness': 'uniquelyIdentifiable', 'locationDescriptor': {'objectDescriptor': {'color': 'green', 'givenness': 'uniquelyIdentifiable', 'type': 'box'}, 'relation': 'near'}, 'color': 'red', 'type': 'box'}}