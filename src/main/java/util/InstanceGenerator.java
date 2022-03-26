package util;

import model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Generators of random instances
 */
public class InstanceGenerator {

    /**
     * Generates an instance only with ET tasks. Recommended values are in brackets ()
     * @param taskNum number of tasks of the instance (2 - 30)
     * @param targetHyperperiod maximum hyperperiod of the instance (10 - 100 000 000)
     * @param minimalPeriod minimum period a task can have ((0.001 - 1) * targetHyperperiod)
     * @param utilization utilization ratio which to aim for (0.1 - 0.9)
     * @param utilizationSwaps number of swaps of utilization between tasks (0 - 1000)
     * @param utilizationSwapAmount how big should a chunk of utilization be during swapping (0.01 - 0.9)
     * @param releaseJitterIntensity amount of release jitter (0 - 1)
     * @param executionTimeVariationIntensity amount of execution time variation (0 - 1)
     * @param releaseShiftIntensity amount of extra time before a job gets released (needed for release jitter) (0 - 1)
     * @param earlyDeadlineIntensity distance of the deadline from period (0 - 1)
     * @param randomShiftIntensity how much should all previous intensities randomly vary, 0 means not at all (0 - 1)
     * @param minPriority minimum priority a job can have (1 - 4)
     * @param maxPriority maximum priority a job can have (2 - 5)
     * @param cachedPeriods optional parameter for faster generation on multiple instances with large hyperperiods
     * @param taskIdShift what ID should be first task have
     * @param seed seed of the instance
     * @return the generated instance as an arrayList of ET tasks
     */
    public static ArrayList<EtTask> generateEtTasks(int taskNum, int targetHyperperiod, int minimalPeriod, double utilization,
                                                    int utilizationSwaps, double utilizationSwapAmount,
                                                    double releaseJitterIntensity, double executionTimeVariationIntensity,
                                                    double releaseShiftIntensity, double earlyDeadlineIntensity,
                                                    double randomShiftIntensity, int minPriority, int maxPriority, long seed,
                                                    ArrayList<Integer> cachedPeriods, int taskIdShift) {

        // Input checking
        if (taskNum < 0 || targetHyperperiod < 1 || minimalPeriod < 1 || utilization <= 0.0 || utilization > 1.0 ||
                utilizationSwaps < 0 || utilizationSwapAmount < 0 || releaseJitterIntensity < 0 ||
                releaseJitterIntensity > 1 || executionTimeVariationIntensity < 0 ||
                executionTimeVariationIntensity > 1 || releaseShiftIntensity < 0 ||
                releaseShiftIntensity > 1 || earlyDeadlineIntensity < 0 || earlyDeadlineIntensity > 1 ||
                randomShiftIntensity < 0 || randomShiftIntensity > 1 || minPriority < 0 || maxPriority < minPriority) {
            throw new RuntimeException("Invalid instance generation argument");
        }

        if (taskNum == 0) {
            return new ArrayList<>();
        }

        //Initializations
        Random r = new Random(seed);
        ArrayList<EtTask> etTasks = new ArrayList<>();

        //Select a period of each task
        ArrayList<Integer> possiblePeriods;
        if (cachedPeriods == null) {
            possiblePeriods = getAllDivisorsOfNumberBiggerThan(targetHyperperiod,minimalPeriod);
        } else {
            possiblePeriods = cachedPeriods;
        }

        ArrayList<Integer> taskPeriods = new ArrayList<>();
        for (int i = 0; i < taskNum; i++) {
            taskPeriods.add(possiblePeriods.get(r.nextInt(possiblePeriods.size())));
        }

        //Determine the utilization of each task
        ArrayList<Double> taskUtilizations = getTaskUtilizations(r, taskNum, utilization, utilizationSwaps, utilizationSwapAmount);

        //Determine maximal execution times
        ArrayList<Integer> maxExecutionTimes = getMaxExecutionTimes(taskNum, taskPeriods, taskUtilizations);

        //Determine other parameters
        for (int i = 0; i < taskNum; i++) {
            int maxExecution = maxExecutionTimes.get(i);
            int period = taskPeriods.get(i);
            int minExecution = getIntFromIntervalWithRandomShift(r,1,maxExecution,1-executionTimeVariationIntensity,randomShiftIntensity);
            int crRelease = getIntFromIntervalWithRandomShift(r,0,(period-maxExecution)/2,releaseShiftIntensity, randomShiftIntensity);
            int urRelease = getIntFromIntervalWithRandomShift(r,0, crRelease, 1-releaseJitterIntensity, randomShiftIntensity);
            int deadline = getIntFromIntervalWithRandomShift(r,(period+maxExecution)/2, period, 1-earlyDeadlineIntensity, randomShiftIntensity);
            int priority = r.nextInt(maxPriority-minPriority+1) + minPriority;
            etTasks.add(new EtTask(i+taskIdShift,period,deadline,urRelease,crRelease,minExecution,maxExecution,priority));
        }

        return etTasks;
    }


    /**
     * Generates an instance only with TT tasks. Recommended values are in brackets ()
     * @param taskNum number of tasks of the instance (2 - 30)
     * @param targetHyperperiod maximum hyperperiod of the instance (10 - 100 000 000)
     * @param minimalPeriod minimum period a task can have ((0.001 - 1) * targetHyperperiod)
     * @param utilization utilization ratio which to aim for (0.1-0.9)
     * @param utilizationSwaps number of swaps of utilization between tasks (0 - 1000)
     * @param utilizationSwapAmount how big should a chunk of utilization be during swapping (0.01 - 0.9)
     * @param releaseShiftIntensity amount of extra time before a job gets released (needed for release jitter) (0 - 1)
     * @param earlyDeadlineIntensity distance of the deadline from period (0 - 1)
     * @param randomShiftIntensity how much should all previous intensities randomly vary (0 - 1)
     * @param seed seed of the instance
     * @return the generated instance as an arrayList of TT tasks
     */
    public static ArrayList<TtTask> generateTtTasks(int taskNum, int targetHyperperiod, int minimalPeriod, double utilization,
                                                    int utilizationSwaps, double utilizationSwapAmount,
                                                    double releaseShiftIntensity, double earlyDeadlineIntensity,
                                                    double randomShiftIntensity, long seed,
                                                    ArrayList<Integer> cachedPeriods) {
        if (taskNum == 0) {
            return new ArrayList<>();
        }
        // Input checking
        if (taskNum < 0 || targetHyperperiod < 1 || minimalPeriod < 1 || utilization <= 0.0 || utilization > 1.0 ||
                utilizationSwaps < 0 || utilizationSwapAmount < 0 || releaseShiftIntensity < 0 ||
                releaseShiftIntensity > 1 || earlyDeadlineIntensity < 0 || earlyDeadlineIntensity > 1 ||
                randomShiftIntensity < 0 || randomShiftIntensity > 1) {
            throw new RuntimeException("Invalid instance generation argument");
        }

        //Initializations
        Random r = new Random(seed);
        ArrayList<TtTask> ttTasks = new ArrayList<>();

        //Select a period of each task
        ArrayList<Integer> possiblePeriods;
        if (cachedPeriods == null) {
            possiblePeriods = getAllDivisorsOfNumberBiggerThan(targetHyperperiod,minimalPeriod);
        } else {
            possiblePeriods = cachedPeriods;
        }

        ArrayList<Integer> taskPeriods = new ArrayList<>();
        for (int i = 0; i < taskNum; i++) {
            taskPeriods.add(possiblePeriods.get(r.nextInt(possiblePeriods.size())));
        }

        //Determine the utilization of each task
        ArrayList<Double> taskUtilizations = getTaskUtilizations(r, taskNum, utilization, utilizationSwaps, utilizationSwapAmount);

        //Determine maximal execution times
        ArrayList<Integer> maxExecutionTimes = getMaxExecutionTimes(taskNum, taskPeriods, taskUtilizations);

        //Determine other parameters
        for (int i = 0; i < taskNum; i++) {
            int executionTime = maxExecutionTimes.get(i);
            int period = taskPeriods.get(i);
            int release = getIntFromIntervalWithRandomShift(r,0,(period-executionTime)/2,releaseShiftIntensity, randomShiftIntensity);
            int deadline = getIntFromIntervalWithRandomShift(r,(period+executionTime)/2, period, 1-earlyDeadlineIntensity, randomShiftIntensity);
            ttTasks.add(new TtTask(i,period,deadline,release, executionTime));
        }

        return ttTasks;
    }

    private static ArrayList<Double> getTaskUtilizations(Random r, int taskNum, double utilization, int utilizationSwaps, double utilizationSwapAmount) {
        ArrayList<Double> ret = new ArrayList<>();
        for (int i = 0; i < taskNum; i++) {
            ret.add(utilization / taskNum);
        }
        if (taskNum != 1) {
            for (int i = 0; i < utilizationSwaps; i++) {
                int source = r.nextInt(taskNum);
                int destination = r.nextInt(taskNum);
                if (source == destination) {
                    i--;
                    continue;
                }
                double transferAmount = ret.get(source)*utilizationSwapAmount;
                if (ret.get(destination) + transferAmount > 1) {
                    continue;
                }
                ret.set(source, ret.get(source) - transferAmount);
                ret.set(destination, ret.get(destination) + transferAmount);
            }
        }
        return ret;
    }

    private static ArrayList<Integer> getMaxExecutionTimes(int taskNum, ArrayList<Integer> taskPeriods, ArrayList<Double> taskUtilizations) {
        ArrayList<Integer> ret = new ArrayList<>();
        for (int i = 0; i < taskNum; i++) {
            int exTime = (int) Math.round(taskPeriods.get(i) * taskUtilizations.get(i));
            ret.add(exTime == 0 ? 1 : exTime);
        }
        return ret;
    }

    public static int getIntFromIntervalWithRandomShift(Random r, int min, int max, double idealPercentage, double randomShift) {
        int mid = min + (int)Math.round((max - min) * idealPercentage);
        if (mid == min || mid == max) {
            return mid;
        }
        if (r.nextBoolean()) { //Left
            int minRange = (int)Math.round((mid-min) * (1-randomShift)) + min;
            return mid - r.nextInt(mid-minRange+1);
        } else { //Right
            int maxRange = (int)Math.round((max-mid) * randomShift) + mid;
            return mid + r.nextInt(maxRange-mid+1);
        }
    }

    private static ArrayList<Integer> getAllDivisorsOfNumber(int number) {
        ArrayList<Integer> ret = new ArrayList<>();
        for (int i = 2; i < number; i++) {
            if (number % i == 0) {
                ret.add(i);
            }
        }
        ret.add(number);
        return ret;
    }

    public static ArrayList<Integer> getAllDivisorsOfNumberBiggerThan(int number, int periodLimit) {
        ArrayList<Integer> periods = getAllDivisorsOfNumber(number);
        Collections.reverse(periods);
        for (int i = 0; i < periods.size();) {
            if (periods.get(i) < periodLimit) {
                periods.remove(i);
            } else {
                i++;
            }
        }
        return periods;
    }

}

