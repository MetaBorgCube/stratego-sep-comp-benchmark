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
df = df[['Stratego compile time (ns)', 'Java compile time (ns)', "Frontend time", "Frontend tasks",
         "Backend time", "Backend tasks", "Lib tasks", "Lib time", "Shuffle time",
         "Shuffle lib time", "Static check time", "Shuffle backend time"]]
# TODO: add backend shuffle time once that becomes a reasonable number
df = df.assign(pie_overhead=lambda row:
    row['Stratego compile time (ns)'] - (row['Frontend time'] + row['Backend time'] + row['Lib time']
        + row['Shuffle time'] + row['Shuffle lib time' + row['Shuffle backend time']
        + row['Static check time']))
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

# Sort by changeset size, then mean stratego compile time, then mean java compile time to smooth the graph
sorted = df.sort_values(by=['changeset size (no. of files)', ('Stratego compile time (ns)', 'mean'),
                            ('Java compile time (ns)', 'mean')], ascending=[False, False, False])
java_values = sorted['Java compile time (ns)']
lib_values = sorted['Lib time']
lib_start = java_values.loc[:, 'mean']
shuffle_lib_values = sorted['Shuffle lib time']
shuffle_lib_start = lib_start + lib_values.loc[:, 'mean']
shuffle_front_values = sorted['Shuffle time']
shuffle_front_start = shuffle_lib_start + shuffle_lib_values.loc[:, 'mean']
shuffle_back_values = sorted['Shuffle backend time']
shuffle_back_start = shuffle_front_start + shuffle_front_values.loc[:, 'mean']
check_values = sorted['Static check time']
check_start = shuffle_back_start + shuffle_back_values.loc[:, 'mean']
front_values = sorted['Frontend time']
front_start = check_start + check_values.loc[:, 'mean']
back_values = sorted['Backend time']
back_start = front_start + front_values.loc[:, 'mean']
pie_values = sorted['pie_overhead']
pie_start = back_start + back_values.loc[:, 'mean']

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
java_bars          = plt.bar(x, java_values['mean'], 0.7,
                             color='#ffffbf', linewidth=0, yerr=java_values['std'])
lib_bars           = plt.bar(x, lib_values['mean'], 0.7, lib_start,
                             color='#fee090', linewidth=0, yerr=lib_values['std'])
shuffle_lib_bars   = plt.bar(x, shuffle_lib_values['mean'], 0.7, shuffle_lib_start,
                             color='#e0f3f8', linewidth=0, yerr=shuffle_lib_values['std'])
shuffle_front_bars = plt.bar(x, shuffle_front_values['mean'], 0.7, shuffle_front_start,
                             color='#fdae61', linewidth=0, yerr=shuffle_front_values['std'])
shuffle_back_bars = plt.bar(x, shuffle_back_values['mean'], 0.7, shuffle_back_start,
                             color='#abd9e9', linewidth=0, yerr=shuffle_back_values['std'])
check_bars         = plt.bar(x, check_values['mean'], 0.7, check_start,
                             color='#f46d43', linewidth=0, yerr=check_values['std'])
front_bars         = plt.bar(x, front_values['mean'], 0.7, front_start,
                             color='#74add1', linewidth=0, yerr=front_values['std'])
back_bars          = plt.bar(x, back_values['mean'], 0.7, back_start,
                             color='#d73027', linewidth=0, yerr=back_values['std'])
pie_bars           = plt.bar(x, pie_values['mean'], 0.7, pie_start,
                             color='#4575b4', linewidth=0, yerr=pie_values['std'])

plt.ylabel('Time (s)')
plt.title('Incremental compilation times across the history of the repository')
plt.xticks(x, xticks, rotation='vertical')
plt.yticks(np.arange(top+1) * num_scale, np.arange(top+1) * y_label_scale)
plt.xlim(-0.5, bars-0.5)
plt.ylim(0, ylimit)
plt.legend(
    [java_bars[0], lib_bars[0], shuffle_lib_bars[0], shuffle_front_bars[0], check_bars[0], front_bars[0], back_bars[0], pie_bars[0]],
    ['Java compile time', 'Library time', 'Library shuffle time', 'Frontend shuffle time', 'Static check time', 'Frontend time', 'Backend time', 'PIE overhead'])
plt.tight_layout()

plt.savefig(sys.argv[2], bbox_inches='tight', pad_inches=0, transparent=True)
