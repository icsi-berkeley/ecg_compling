'''
Created on Feb 28, 2011

@author: lucag
'''

from sys import argv


def do_make(input, output):
    output.write('<sentences>\n')
    for i, l in enumerate(input):
        if (len(l) > 1):
            output.write(' <sentence id="%d">%s</sentence>\n' % (i, l[0:-1]))

    output.write('</sentences>\n')


HEADING = '<?xml version="1.0" encoding="UTF-8"?>'

if __name__ == '__main__':

    print argv

    print 'Converting ...',
    output = open(argv[2], 'wb')
    output.write(HEADING + '\n')
    do_make(open(argv[1], 'rb'), output)
    print 'done.'