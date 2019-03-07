#!/usr/bin/env python2
import sys
import itertools

repeat = sys.argv[1] if len(sys.argv) >= 1 else 2
for a, b, c in itertools.product([1, 2, 4, 8, 16, 32], repeat=3):
   print "single 128 100 {} {} {} {}".format(a, b, c, repeat)
