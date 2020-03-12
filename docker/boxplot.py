#!python3
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import sys

def print_usage():
    print("""USAGE: boxplot.py BENCHMARK_CSV BAR_CHART_PDF [WIDTH HEIGHT]
    BENCHMARK_CSV  path to csv file produced by benchmark code
    BAR_CHART_PDF  path to pdf file to produce
    WIDTH          width of bar chart in inches                    (default: 15)
    HEIGHT         height of bar chart in inches                   (default: 5)""")

# Check command line arguments
if(len(sys.argv) == 0 or '-h' in sys.argv or '--help' in sys.argv):
    print_usage()
    exit(0)
if(len(sys.argv) not in [3, 5]):
    print_usage()
    exit(1)

# Settings
if(len(sys.argv) < 5):
    plt.rcParams['figure.figsize'] = 12, 0.5
else:
    plt.rcParams['figure.figsize'] = float(sys.argv[3]), float(sys.argv[4])

# Load file, index by SHA-1 and changeset size
df = pd.read_csv(sys.argv[1], index_col=[0,1])
# Drop commits with changeset size 0
df = df.drop(index=0, level='changeset size (no. of files)')

# Some derived numbers and tweaked numbers
num_scale = 1000000000
x_label_scale = 1

plot = plt.boxplot(df['Stratego compile time (ns)'], vert=False, whis='range')
plt.xlabel('Time (s)')
plt.xticks(np.arange(89, 98) * num_scale, np.arange(89, 98) * x_label_scale)
plt.ylim(0.9, 1.1)
plt.yticks([])

plt.savefig(sys.argv[2], bbox_inches='tight', pad_inches=0.01, transparent=True)
