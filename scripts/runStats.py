#!/usr/bin/python3

import os
import sys
import subprocess


# Process the output of the baseline tests
subprocess.call(['sh', './checkProperties.sh', "../out/baseline", "../stats/baseline", "baseline"])


# Process the output of the ByzzFuzz tests
baseInputFolderName = "../out/tests-D"
baseOutputFolderName = "../stats/D"
baseOutputFileName = "D"

for drops in [0, 1, 2]:

	for corrs in [0, 1, 2]:

		if (drops == 0) and (corrs == 0):
			continue;

		if(corrs == 0):
			inputFolderName = baseInputFolderName + str(drops) + "-C0"  
			outputFolderName = baseOutputFolderName + str(drops)  + "-C0"
			outputFileName = baseOutputFileName + str(drops) + "-C0" 

			print("Processing folder: " + inputFolderName)
			subprocess.call(['sh', './checkProperties.sh', inputFolderName, outputFolderName, outputFileName])

		else:
			for mutations in ["as", "ss"]:
				inputFolderName = baseInputFolderName + str(drops) + "-C" + str(corrs) + "-" + mutations
				outputFolderName = baseOutputFolderName + str(drops)  + "-C" + str(corrs) + "-" + mutations
				outputFileName = baseOutputFileName + str(drops) + "-C" + str(corrs) + "-" + mutations

				print("Processing folder: " + inputFolderName)
				subprocess.call(['sh', './checkProperties.sh', inputFolderName, outputFolderName, outputFileName])


