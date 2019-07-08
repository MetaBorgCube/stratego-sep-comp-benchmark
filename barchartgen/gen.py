#!python3

import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import sys

df = pd.read_csv(sys.argv[1])
bars = len(df.index)
x = np.arange(bars)
top = 7
num_scale = 10000000000
y_label_scale = 10
ylimit = top * num_scale

plt.rcParams['figure.figsize'] = 22, 10

str_bars  = plt.bar(x, df['Stratego compile time (ns)'], 0.7,
                    color='#f1a340', linewidth=0)
java_bars = plt.bar(x, df['Java compile time (ns)'], 0.7, df['Stratego compile time (ns)'],
                    color='#998ec3', linewidth=0)

plt.ylabel('Time (s)')
plt.title('Incremental compilation times across the history of the repository')
plt.xticks(x, df['commit (SHA-1)'].apply(lambda sha: sha[:7]), rotation='vertical')
plt.yticks(np.arange(top+1) * num_scale, np.arange(top+1) * y_label_scale)
plt.xlim(-0.5, bars-0.5)
plt.ylim(0, ylimit)
plt.legend([str_bars[0], java_bars[0]], ['Stratego compile time', 'Java compile time'])
plt.tight_layout()

plt.savefig(sys.argv[2], bbox_inches='tight', pad_inches=0, transparent=True)
