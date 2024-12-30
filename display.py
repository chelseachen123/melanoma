import pandas as pd

import matplotlib.pyplot as plt

# Read the data from both files
mel_data = pd.read_csv('mel_ratios.txt', header=None, names=['image', 'ratio'])
nv_data = pd.read_csv('nv_ratios.txt', header=None, names=['image', 'ratio'])

# Create the plot
plt.figure(figsize=(10, 6))
plt.hist(mel_data['ratio'], bins=50, alpha=0.5, color='red', label='Melanoma', range=(0, 0.05))
plt.hist(nv_data['ratio'], bins=50, alpha=0.5, color='blue', label='Normal', range=(0, 0.05))

# Customize the plot
plt.xlim(0, 0.05)
plt.xlabel('Ratio')
plt.ylabel('Frequency')
plt.title('Distribution of Ratios: Melanoma vs Normal')
plt.legend()

# Save the plot instead of showing it
plt.savefig('ratio_distribution.png')
plt.close()