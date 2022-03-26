package util;

/**
 * Class which computes lower common multiplier of given numbers.
 * It uses euclidean algorithm.
 */
public class LcmCalculator {

    private int currentLcm;

    public LcmCalculator() {
        this.currentLcm = 1;
    }

    private static long LCM(long a, long b) {
        return (a * b) / GCF(a, b);
    }

    private static long GCF(long a, long b) {
        return b == 0 ? a : GCF(b, a % b);
    }

    /**
     * Adds a number from which the LCM should be calculated
     * @param num number to be added
     */
    public void addNumber(int num) {
        currentLcm = (int)LCM(currentLcm, num);
    }

    /**
     * @return LCM of all numbers added so far
     */
    public int getCurrentLcm() {
        return currentLcm;
    }
}
