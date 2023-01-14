package edu.tudelft.serg.util;

import java.util.*;

public class MathUtils {

    /**
     * Generates a partition of a set uniformly at random
     *  Using the urn model given in:
     *  [A. J. Stam, 1982] "Generation of a Random Partition of a Finite Set by an Urn Model"
     */
    public static class SetPartition {

        // To save computation time, we memoize an array of Bell values
        private static final int[] bellNumbers = {1, 1, 2, 5, 15, 52, 203, 877, 4140};

        private static int computeBellNumber(int n) {
            int[][] bell = new int[n + 1][n + 1];
            bell[0][0] = 1;
            for (int i = 1; i <= n; i++) {
                bell[i][0] = bell[i - 1][i - 1];

                for (int j = 1; j <= i; j++)
                    bell[i][j] = bell[i - 1][j - 1] + bell[i][j - 1];
            }
            return bell[n][0];
        }

        private static int bellNumber(int n) {
            if (n < bellNumbers.length) return bellNumbers[n];
            else return computeBellNumber(n);
        }

        // Find a lower bound on the number of urns, whose total probability exceeds given prob
        // Then, we will randomly select an urn out of that number of urns
        // E.g. For a set of items |P|=n=4, the sum of the probabilities of 7 urns is greater than prob=0.99
        private static int minNumberOfUrns(int n, double prob) {
            double sumOfProbs = 0;
            int numUrns = 0;
            while (sumOfProbs < prob) {
                numUrns++;
                double p = probabilityOfMForN(n, numUrns);
                sumOfProbs += p;
                System.out.println("u:" + numUrns + " prob: " + p + "  Sum: " + sumOfProbs);
            }
            return numUrns;
        }

        // To save computation time, we hardcode the number of urns and their probabilities: u=7 urns for n=4, p=0.99
        // The probability of choosing u=1, u=2, ..., u=7 urns
        private static double[] urnProbabilitiesForN5P099 = {0.024525296078096157, 0.19620236862476925,
        0.3310914970542981, 0.261603158166359, 0.1277359170734175, 0.1277359170734175, 0.011683578548315253};
        // The cumulative probabilities for choosing u<=1, u<=2, ..., u<=7 urns
        //private static double[] cumUrnProbabilitiesForN4P099 = {0.024525296078096157, 0.22072766470286542,
        //        0.5518191617571635, 0.8134223199235224, 0.9411582369969399, 0.985303769937513, 0.9969873484858283};

        public static int randomNumberOfUrnsForN4(Random r) {
            double d = r.nextDouble();
            double cumulative = 0;

            for(int i = 0; i < urnProbabilitiesForN5P099.length; i++) {
                cumulative += urnProbabilitiesForN5P099[i];
                if(d <= cumulative) {
                    return i + 1; // index i keeps the number of urns (i+1)
                }
            }

            return urnProbabilitiesForN5P099.length;
        }

        // Computes the probability using the probability distribution for P(U = u)
        // [A. J. Stam, 1982] "Generation of a Random Partition of a Finite Set by an Urn Model" (Formula 1.4)
        public static double probabilityOfMForN(int n, int u) {
            return Math.pow(u, n) / (bellNumber(n) * factorial(u) * Math.exp(1));
        }

        // Generate a random partition of a set |P|
        public static List<List<Integer>> randomPartition(Random r, Collection<Integer> items) {
            int numberOfUrns = randomNumberOfUrnsForN4(r);

            // Create a partition: set of sets of size numberOfUrns (used lists to access indices)
            List<List<Integer>> urns = new ArrayList<>();
            for (int i = 0; i <= numberOfUrns; i++) {
                urns.add(new ArrayList<>());
            }

            // For each element, randomly add the element into an set(or urn)
            for (Integer i : items) {
                int u = r.nextInt(numberOfUrns);
                urns.get(u).add(i);
            }
            // Eliminate empty urns
            List<List<Integer>> nonEmptyUrns = new ArrayList<>();
            for (int i = 0; i <= numberOfUrns; i++) {
                if (!urns.get(i).isEmpty())
                    nonEmptyUrns.add(urns.get(i));
            }

            // The resulting partition is guaranteed to be uniform
            return nonEmptyUrns;
        }
    }

    // U
    public static class Subset {

        // use lists instead of sets to determinize the order
        public static List<Integer> randomSubsetOfSize(Random random, Collection<Integer> elements, int size) {
            List<Integer> allElements = new ArrayList<>(elements);
            List<Integer> selected = new ArrayList<>();

            while(selected.size() < size) {
                int r = random.nextInt(allElements.size());
                selected.add(allElements.get(r));
                allElements.remove(r);
            }

            return selected;
        }

        // sample a subset uniformly at random
        // TODO: Generalize - Currently memoized for #honest-prcesses = 4 (3 replicas and 1 client process)
        private static int numItems = 4;
        public static int[] numSubsetsOfN4SizeI = {/*1,*/ 4, 6, 4, 1}; // removed empty set

        // Used to corrupt messages from Byzantine process to a set of 4 processes
        public static List<Integer> randomSubset(Random random, Collection<Integer> elements) {
            int size = chooseSizeOfSubset(random, numItems);
            return randomSubsetOfSize(random, elements, size);
        }

        private static int chooseSizeOfSubset(Random random, int n) {
            int r = random.nextInt((int)Math.pow(2, n)) + 1;  // A random number between [1 - 2^n]
            int numSubsets = 0;

            for(int i = 0; i < numSubsetsOfN4SizeI.length; i++) {
                numSubsets += numSubsetsOfN4SizeI[i];
                if(r <= numSubsets) {
                    return i + 1; // the selected size of the subset is numSubsetsOfN4SizeI[i] + 1
                }
            }

            // does not reach here
            return numSubsetsOfN4SizeI.length;
        }
    }

    private static double factorial(double d) {
        int res = 1;
        for(int i = 1; i <= d; i++) res *= i;
        return res;
    }
}
