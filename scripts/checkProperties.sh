#!/bin/bash

INPUTFOLDER=$1
OUTFOLDER=$2
OUTFILE=$3

mkdir -p $OUTFOLDER

# Check for Termination violations (No decision is made until timeout - bounded termination)
grep -nlr "Timeout" $INPUTFOLDER > $OUTFOLDER/$OUTFILE.txt
grep -nlr "Timeout" $INPUTFOLDER > $OUTFOLDER/$OUTFILE-1.txt
numTimeout="$(wc -l <"$OUTFOLDER/$OUTFILE-1.txt")"
echo "Termination violations in " $INPUTFOLDER " : " $numTimeout

# Check for Validity violations
grep -nlr "Violation of VALIDITY" $INPUTFOLDER >> $OUTFOLDER/$OUTFILE.txt
grep -nlr "Violation of VALIDITY" $INPUTFOLDER > $OUTFOLDER/$OUTFILE-2.txt
numValidity="$(wc -l <"$OUTFOLDER/$OUTFILE-2.txt")"
echo "Validity violations in " $INPUTFOLDER " : " $numValidity

# Check for Integrity violations
grep -nlr "Violation of INTEGRITY" $INPUTFOLDER >> $OUTFOLDER/$OUTFILE.txt
grep -nlr "Violation of INTEGRITY" $INPUTFOLDER > $OUTFOLDER/$OUTFILE-3.txt
numIntegrity="$(wc -l <"$OUTFOLDER/$OUTFILE-3.txt")"
echo "Integrity violations in " $INPUTFOLDER " : " $numIntegrity

# Check for Agreement violations
grep -nlr "Violation of AGREEMENT" $INPUTFOLDER >> $OUTFOLDER/$OUTFILE.txt
grep -nlr "Violation of AGREEMENT" $INPUTFOLDER > $OUTFOLDER/$OUTFILE-4.txt
numAgreement="$(wc -l <"$OUTFOLDER/$OUTFILE-4.txt")"
echo "Agreement violations in " $INPUTFOLDER " : " $numAgreement

numAllUnique="$(sort "$OUTFOLDER/$OUTFILE.txt" | uniq | wc -l)"
echo "All in " $INPUTFOLDER " : " $numAllUnique
echo ""