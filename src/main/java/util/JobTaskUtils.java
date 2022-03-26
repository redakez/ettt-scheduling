package util;

import model.*;

import java.util.ArrayList;
import java.util.Collection;

public class JobTaskUtils {

    /**
     * @param ttTasks collection of time triggered tasks, can be null
     * @param etTasks collection of event triggered tasks, can be null
     * @return hyperperiod of given tasks
     */
    public static int getHyperperiodForTasks(Collection<TtTask> ttTasks, Collection<EtTask> etTasks) {
        LcmCalculator lcmCalc = new LcmCalculator();
        if (etTasks != null) {
            for (EtTask t : etTasks) {
                lcmCalc.addNumber(t.getPeriod());
            }
        }
        if (ttTasks != null) {
            for (TtTask t : ttTasks) {
                lcmCalc.addNumber(t.getPeriod());
            }
        }
        return lcmCalc.getCurrentLcm();
    }

    /**
     * Creates a list of time triggered jobs for a given time triggered tasks and a hyperperiod.
     * The output is an array of array lists, each array list contains jobs of one type of task.
     * @param ttTasks collection of time triggered tasks, can be null
     * @param hyperperiod period up to which the jobs should be created
     * @return list of time triggered jobs
     */
    public static ArrayList<TtJob>[] getTtJobsFromTtTasksAs2dArray(Collection<TtTask> ttTasks, int hyperperiod) {
        ArrayList<TtJob>[] ttJobs = new ArrayList[ttTasks.size()];
        for (int i = 0; i < ttTasks.size(); i++) {
            ttJobs[i] = new ArrayList<>();
        }
        int curTaskIndex = 0;
        for (TtTask task : ttTasks) {
            for (int i = 0; i < hyperperiod / task.getPeriod(); i++) {
                ttJobs[curTaskIndex].add(task.getNthRepetition(i));
            }
            curTaskIndex++;
        }
        return ttJobs;
    }

    /**
     * Creates a list of event triggered jobs for a given event triggered tasks and a hyperperiod.
     * The output is an array of array lists, each array list contains jobs of one type of task.
     * @param etTasks collection of event triggered tasks, can be null
     * @param hyperperiod period up to which the jobs should be created
     * @return list of event triggered jobs
     */
    public static ArrayList<EtJob>[] getEtJobsFromEtTasksAs2dArray(Collection<EtTask> etTasks, int hyperperiod) {
        ArrayList<EtJob>[] etJobs = new ArrayList[etTasks.size()];
        for (int i = 0; i < etTasks.size(); i++) {
            etJobs[i] = new ArrayList<>();
        }
        int curTaskIndex = 0;
        for (EtTask task : etTasks) {
            for (int i = 0; i < hyperperiod / task.getPeriod(); i++) {
                etJobs[curTaskIndex].add(task.getNthRepetition(i));
            }
            curTaskIndex++;
        }
        return etJobs;
    }

    /**
     * Creates a list of extended event triggered jobs for a given event triggered tasks and a hyperperiod.
     * The output is an array of array lists, each array list contains jobs of one type of task.
     * @param etTasks collection of event triggered tasks, can be null
     * @param hyperperiod period up to which the jobs should be created
     * @return list of extended event triggered jobs
     */
    public static ArrayList<ExtendedEtJob>[] getExtendedEtJobsFromEtTasksAs2dArray(Collection<EtTask> etTasks, int hyperperiod) {
        ArrayList<ExtendedEtJob>[] etJobs = new ArrayList[etTasks.size()];
        for (int i = 0; i < etTasks.size(); i++) {
            etJobs[i] = new ArrayList<>();
        }
        int curTaskIndex = 0;
        for (EtTask task : etTasks) {
            for (int i = 0; i < hyperperiod / task.getPeriod(); i++) {
                EtJob curJob = task.getNthRepetition(i);
                etJobs[curTaskIndex].add(new ExtendedEtJob(curJob));
            }
            curTaskIndex++;
        }
        return etJobs;
    }

    /**
     * Fixes TT jobs using a set of start times
     * @param ttJobs TT jobs to be fixed
     * @param startTimes the start times used to fix the TT tasks
     * @return list of fixed TT jobs
     */
    public static ArrayList<EtJob>[] fixTtJobsWithStartTimes(ArrayList<TtJob>[] ttJobs, int[][] startTimes) {
        ArrayList<EtJob>[] fixedJobs = new ArrayList[ttJobs.length];
        for (int i = 0; i < fixedJobs.length; i++) {
            fixedJobs[i] = new ArrayList<>();
        }
        for (int i = 0; i < startTimes.length; i++) {
            for (int j = 0; j < startTimes[i].length; j++) {
                TtJob curTtJob = ttJobs[i].get(j);
                EtJob fixedJob = new EtJob(curTtJob, 0);
                fixedJob.setReleaseTimeMax(startTimes[i][j]);
                fixedJob.setReleaseTimeMin(startTimes[i][j]);
                fixedJob.setDeadline(startTimes[i][j] + curTtJob.getExecutionTime());
                fixedJobs[i].add(fixedJob);
            }
        }
        return fixedJobs;
    }

    public static int getTotalNumberOfJobsInTasks(ArrayList<TtTask> ttTasks, ArrayList<EtTask> etTasks, int hyperperiod) {
        int ret = 0;
        if (ttTasks != null) {
            for (TtTask tt : ttTasks) {
                ret += hyperperiod / tt.getPeriod();
            }
        }
        if (etTasks != null) {
            for (EtTask et : etTasks) {
                ret += hyperperiod / et.getPeriod();
            }
        }
        return ret;
    }

    public static void printInstanceStatistics(ArrayList<TtTask> ttTasks, ArrayList<EtTask> etTasks) {
        int hyperperiod = JobTaskUtils.getHyperperiodForTasks(ttTasks,etTasks);
        int ttJobNum = JobTaskUtils.getTotalNumberOfJobsInTasks(ttTasks, null, hyperperiod);
        int etJobNum = JobTaskUtils.getTotalNumberOfJobsInTasks(null, etTasks, hyperperiod);

        double utilizationTt = 0;
        double utilizationEt = 0;
        for (TtTask tt : ttTasks) {
            utilizationTt += (double)tt.getExecutionTime() / tt.getPeriod();
        }
        for (EtTask et : etTasks) {
            utilizationEt += (double)et.getExecutionTimeMax() / et.getPeriod();
        }
        System.out.println("Instance statistics:");
        System.out.println(" - TtTasks total: " + ttTasks.size());
        System.out.println(" - TtJobs total: " + ttJobNum);
        System.out.println(" - EtTasks total: " + etTasks.size());
        System.out.println(" - EtJobs total: " + etJobNum);
        System.out.println(" - Utilization TT: " + utilizationTt);
        System.out.println(" - Utilization ET: " + utilizationEt);
        System.out.println(" - Utilization total: " + (utilizationTt + utilizationEt));
        System.out.println(" - Hyperperiod: " + hyperperiod);
    }

    public static void printInstanceTasks(ArrayList<TtTask> ttTasks, ArrayList<EtTask> etTasks) {
        System.out.println("All tasks:");
        System.out.println("   ID          Period |    Release min     Release max |           WCET            BCET |       Deadline  Priority  Utilization");
        for (TtTask tt : ttTasks) {
            System.out.printf("%5d %15d |%15d %15d |%15d %15d |%15d %9d %12f\n", tt.getId(), tt.getPeriod(),
                    tt.getReleaseTime(), tt.getReleaseTime(), tt.getExecutionTime(), tt.getExecutionTime(),
                    tt.getDeadline(), 0, (double)tt.getExecutionTime()/tt.getPeriod());
        }
        for (EtTask et : etTasks) {
            System.out.printf("%5d %15d |%15d %15d |%15d %15d |%15d %9d %12f\n", et.getId(), et.getPeriod(),
                    et.getReleaseTimeMin(), et.getReleaseTimeMax(), et.getExecutionTimeMin(), et.getExecutionTimeMax(),
                    et.getDeadline(), et.getPriority(), (double)et.getExecutionTimeMax()/et.getPeriod());
        }
    }

    public static void printInstanceStatisticsDetailed(ArrayList<TtTask> ttTasks, ArrayList<EtTask> etTasks) {
        printInstanceStatistics(ttTasks, etTasks);
        printInstanceTasks(ttTasks, etTasks);
    }

}
