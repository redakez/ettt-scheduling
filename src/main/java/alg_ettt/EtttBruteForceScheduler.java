package alg_ettt;

import alg_et.EtScheduleGraphTest;
import model.*;
import util.IntervalTree;

import java.util.ArrayList;

import static util.JobTaskUtils.*;

/**
 * Algorithm which tries to fix time triggered jobs in all possible combinations.
 * It guarantees to find a solution if it exists. Also contains a variant using
 * interval tree to avoid instances with guaranteed deadline misses between time
 * triggered jobs.
 */
public class EtttBruteForceScheduler {

    private final int hyperperiod;
    private final int ttTaskNum;

    private final ArrayList<EtJob>[] allJobs;
    private final SchedulingPolicy policy;

    public int scheduleGraphCalls = 0; //For benchmarking

    /**
     * @param ttTasks map of time triggered tasks (TaskId -> Task)
     * @param etTasks map of event triggered tasks (TaskId -> Task)
     * @param policy policy under which the scheduling should be done
     */
    public EtttBruteForceScheduler(ArrayList<TtTask> ttTasks, ArrayList<EtTask> etTasks, SchedulingPolicy policy) {
        this.policy = policy;
        this.hyperperiod = getHyperperiodForTasks(ttTasks,etTasks);
        this.ttTaskNum = ttTasks.size();

        ArrayList<TtJob>[] ttJobs2D = getTtJobsFromTtTasksAs2dArray(ttTasks, hyperperiod);
        ArrayList<EtJob>[] etJobs2D = getEtJobsFromEtTasksAs2dArray(etTasks, hyperperiod);
        allJobs = new ArrayList[ttJobs2D.length + etJobs2D.length];
        for (int i = 0; i < ttJobs2D.length; i++) {
            allJobs[i] = new ArrayList<>();
            for (int j = 0; j < ttJobs2D[i].size(); j++) {
                allJobs[i].add(new EtJob(ttJobs2D[i].get(j), 0));
            }
        }
        for (int iAll = ttJobs2D.length, iET = 0; iAll < allJobs.length; iAll++, iET++) {
            allJobs[iAll] = etJobs2D[iET];
        }
    }

    /**
     * Attempts to find a solution by trying each combination of fixed time triggered jobs
     * @return if a solution has been found
     */
    public boolean processTasks(boolean useIntervalTree) {
        return scheduleJobsRec(0,0, useIntervalTree ? new IntervalTree() : null);
    }

    /**
     * Attempts to find a solution by trying each combination of fixed time triggered jobs with no jitter between TT jobs
     * @return if a solution has been found
     */
    public boolean processTasksNoJitter(boolean useIntervalTree) {
        return scheduleJobsRecNoJitter(0,useIntervalTree ? new IntervalTree() : null);
    }

    /**
     * Recursive procedure which assigns a time triggered job to all possible positions.
     * Once all jobs are fixed, a schedule graph analysis is executed.
     * @return if a solution has been found
     */
    private boolean scheduleJobsRec(int curTaskId, int curJobRepetition, IntervalTree it) {
        if (curTaskId == ttTaskNum) {
            if (Thread.currentThread().isInterrupted()) {
                return true;
            }
            EtScheduleGraphTest sgc = new EtScheduleGraphTest(allJobs);
            scheduleGraphCalls++;
            return sgc.generateGraphTest(true, this.policy,false);
        }
        EtJob curJob = allJobs[curTaskId].get(curJobRepetition);
        int curTaskJobCount = hyperperiod / allJobs[curTaskId].get(0).getPeriod();
        int ogRelease = curJob.getReleaseTimeMin();
        int ogDeadline = curJob.getDeadline();
        for (int release = ogRelease, deadline = ogRelease + curJob.getExecutionTimeMax(); release <= ogDeadline - curJob.getExecutionTimeMax(); release++, deadline++) {
            curJob.setReleaseTimeMin(release);
            curJob.setReleaseTimeMax(release);
            curJob.setDeadline(deadline);

            int nextTaskId, nextJobRepetition;

            if (curTaskJobCount == curJobRepetition+1) {
                nextTaskId = curTaskId+1;
                nextJobRepetition = 0;
            } else {
                nextTaskId = curTaskId;
                nextJobRepetition = curJobRepetition+1;
            }

            if (it != null) {
                if (it.intersects(release, deadline-1)) {
                    continue;
                }
                it.add(release, deadline-1);
            }
            if (scheduleJobsRec(nextTaskId, nextJobRepetition, it)) {
                return true;
            }
            if (it != null) {
                it.remove(release, deadline - 1);
            }
        }
        curJob.setReleaseTimeMin(ogRelease);
        curJob.setReleaseTimeMax(ogRelease);
        curJob.setDeadline(ogDeadline);
        return false;
    }


    /**
     * Recursive procedure which assigns a time triggered job to all possible positions.
     * Once all jobs are fixed, a schedule graph analysis is executed.
     * @return if a solution has been found
     */
    private boolean scheduleJobsRecNoJitter(int curTaskId, IntervalTree it) {
        if (curTaskId == ttTaskNum) {
            if (Thread.currentThread().isInterrupted()) {
                return true;
            }
            EtScheduleGraphTest sgc = new EtScheduleGraphTest(allJobs);
            scheduleGraphCalls++;
            return sgc.generateGraphTest(true, this.policy,true);
        }
        EtJob firstJob = allJobs[curTaskId].get(0);
        int curTaskPeriod = allJobs[curTaskId].get(0).getPeriod();
        int curTaskJobCount = hyperperiod / curTaskPeriod;
        int ogRelease = firstJob.getReleaseTimeMin();
        int ogDeadline = firstJob.getDeadline();
        for (int release = ogRelease, deadline = ogRelease + firstJob.getExecutionTimeMax();
                release <= ogDeadline - firstJob.getExecutionTimeMax();
                release++, deadline++) {
            if (it != null) {
                boolean intersection = false;
                for (int i = 0; i < curTaskJobCount; i++) {
                    if (it.intersects(release + i*curTaskPeriod, deadline-1 + i*curTaskPeriod)) {
                        intersection = true;
                        break;
                    }
                }
                if (intersection) {
                    continue;
                }
                for (int i = 0; i < curTaskJobCount; i++) {
                    it.add(release + i*curTaskPeriod, deadline-1 + i*curTaskPeriod);
                }
            }
            for (int i = 0; i < curTaskJobCount; i++) {
                EtJob curJob = allJobs[curTaskId].get(i);
                curJob.setReleaseTimeMax(release + i*curTaskPeriod);
                curJob.setReleaseTimeMin(release + i*curTaskPeriod);
                curJob.setDeadline(deadline + i*curTaskPeriod);
            }
            if (scheduleJobsRecNoJitter(curTaskId+1, it)) {
                return true;
            }
            if (it != null) {
                for (int i = 0; i < curTaskJobCount; i++) {
                    it.remove(release + i*curTaskPeriod, deadline-1 + i*curTaskPeriod);
                }
            }
        }
        for (int i = 0; i < curTaskJobCount; i++) {
            EtJob curJob = allJobs[curTaskId].get(i);
            curJob.setReleaseTimeMax(ogRelease + i*curTaskPeriod);
            curJob.setReleaseTimeMin(ogRelease + i*curTaskPeriod);
            curJob.setDeadline(ogDeadline + i*curTaskPeriod);
        }
        return false;
    }

    public int[][] getStartTimes() {
        int[][] ret = new int[ttTaskNum][];
        for (int i = 0; i < ttTaskNum; i++) {
            int curTaskJobCount = hyperperiod / allJobs[i].get(0).getPeriod();
            ret[i] = new int[curTaskJobCount];
        }
        for (int i = 0; i < ttTaskNum; i++) {
            for (int j = 0; j < ret[i].length; j++) {
                ret[i][j] = allJobs[i].get(j).getReleaseTimeMax();
            }
        }
        return ret;
    }

    public void printResult() {
        System.out.println("Resulting start times for TT jobs:");
        for (int i = 0; i < ttTaskNum; i++) {
            int curTaskJobCount = hyperperiod / allJobs[i].get(0).getPeriod();
            System.out.println("TT task ID: " + i);
            for (int j = 0; j < curTaskJobCount; j++) {
                EtJob curJob = allJobs[i].get(j);
                System.out.println(" - Repetition number: " + j + ", start time: " + curJob.getReleaseTimeMax());
            }
        }
    }

}
