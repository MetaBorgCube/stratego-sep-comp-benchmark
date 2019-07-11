#!python3
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import sys

def print_usage():
    print("""USAGE: gen.py BENCHMARK_CSV BAR_CHART_PDF [WIDTH HEIGHT [YMAX]]
    BENCHMARK_CSV  path to csv file produced by benchmark code
    BAR_CHART_PDF  path to pdf file to produce
    WIDTH          width of bar chart in inches                    (default: 25)
    HEIGHT         height of bar chart in inches                   (default: 10)
    YMAX           cut-off point on the Y axis in tens of seconds  (default:  8)""")

# Check command line arguments
if(len(sys.argv) == 0 or '-h' in sys.argv or '--help' in sys.argv):
    print_usage()
    exit(0)
if(len(sys.argv) not in [3, 5, 6]):
    print_usage()
    exit(1)

# Settings
if(len(sys.argv) < 5):
    plt.rcParams['figure.figsize'] = 25, 10
else:
    plt.rcParams['figure.figsize'] = float(sys.argv[3]), float(sys.argv[4])
plt.rcParams['errorbar.capsize'] = 1

# Load file, index by SHA-1 and changeset size
df = pd.read_csv(sys.argv[1], index_col=[0,1])
# Select only the interesting values
df = df[['Stratego compile time (ns)', 'Java compile time (ns)']]
# Group by SHA-1 first, then changeset size to keep that as an index
df = df.groupby(['commit (SHA-1)', 'changeset size (no. of files)'], sort=False)
# Aggregrate grouped values by mean and standard deviation
df = df.agg(['mean', 'std'])
# Drop commits with changeset size 0
df = df.drop(index=0, level='changeset size (no. of files)')

# Some derived numbers and tweaked numbers
bars = len(df.index)
x = np.arange(bars)
top = 8 if len(sys.argv) < 6 else float(sys.argv[5])
num_scale = 10000000000
y_label_scale = 10
ylimit = top * num_scale

# Sort by changeset size
sorted = df.sort_values('changeset size (no. of files)', ascending=False)
# Then sort by value to make the graph look smoother
java_values = sorted['Java compile time (ns)'].sort_values('mean', ascending=False)
str_values = sorted['Stratego compile time (ns)'].sort_values('mean', ascending=False)
# Finally use changeset size as label
xticks = sorted.index.get_level_values(1).to_frame()
xticks['duplicated'] = xticks.duplicated()
xticks = xticks.apply(lambda r:
    str(r['changeset size (no. of files)']) if not r['duplicated'] else '', axis='columns')
# else:
#     # Already sorted chronologically
#     java_values = df['Java compile time (ns)']
#     str_values = df['Stratego compile time (ns)']
#     # Use the first 7 characters of the hash
#     xticks = df.index.get_level_values(0).map(lambda sha: sha[:7])

# Actually set graph the bars. Java goes on the bottom because of its lower variance
java_bars = plt.bar(x, java_values['mean'], 0.7,
                    color='#f1a340', linewidth=0, yerr=java_values['std'])
str_bars  = plt.bar(x, str_values['mean'], 0.7, java_values['mean'],
                    color='#998ec3', linewidth=0, yerr=str_values['std'])

plt.ylabel('Time (s)')
plt.title('Incremental compilation times across the history of the repository')
plt.xticks(x, xticks, rotation='vertical')
plt.yticks(np.arange(top+1) * num_scale, np.arange(top+1) * y_label_scale)
plt.xlim(-0.5, bars-0.5)
plt.ylim(0, ylimit)
plt.legend([str_bars[0], java_bars[0]], ['Stratego compile time', 'Java compile time'])
plt.tight_layout()

plt.savefig(sys.argv[2], bbox_inches='tight', pad_inches=0, transparent=True)
