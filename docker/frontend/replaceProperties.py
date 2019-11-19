#!/usr/bin/python

import argparse
import errno
import re
import signal
import sys
import os

def replaceProperties(file, prefix):
    for line in file:
            x = re.search("\$\{([^}]+)\}", line)
            if x != None:
                varName = x.group(1)
                varName = varName.upper()
                varName = re.sub('\.', '_', varName)
                varName = prefix + varName
                if varName in props:
                    line = re.sub(r'\$\{[^}]+\}', props[varName], line.rstrip())
                    print(line)
                else:
                    print line,
            else:
                print line,

def ctrlcHandler(sig, frame):
    sys.exit(0)

'''
Main
'''

DEFAULT_PREFIX = 'CODEDEF_CFG_'
parser = argparse.ArgumentParser(description='Replace config properties (${property}) with environment variables.')
parser.add_argument('file', help='file path to replace configuration', nargs='?')
parser.add_argument('-p', '--prefix', help='environment var prefix. Default: '+DEFAULT_PREFIX, nargs='?', default=DEFAULT_PREFIX)

if len(sys.argv)==1:
    parser.print_help(sys.stderr)
    sys.exit(1)
args=parser.parse_args()

'''
Filter and collect environment variables
'''
props = {}
for k in os.environ.keys():
    if k.startswith(args.prefix):
        value = os.environ[k]
        props[k] = value

if args.file != None:
    try:
        f = open(args.file, 'ro')
        replaceProperties(f, args.prefix)
        f.close()
    except IOError as e:
        if e.errno == errno.ENOENT:
            print('File %s does not exist' % (args.file))
        elif e.errno == errno.EACCES:
            print('You do not have permissions to open %s' % (args.file))
        else:
            raise
else:
    signal.signal(signal.SIGINT, ctrlcHandler)
    replaceProperties(sys.stdin, args.prefix)