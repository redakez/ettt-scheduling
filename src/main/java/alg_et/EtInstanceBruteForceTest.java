package alg_et;

import model.EtTask;
import model.ExtendedEtJob;
import model.SchedulingPolicy;
import util.JobTaskUtils;
import util.SchedulingPolicies;

import java.util.ArrayList;

/**
 * Brute force schedulability test for ET tasks
 */
public class EtInstanceBruteForceTest {

    public static boolean launchTest(ArrayList<EtTask> etTasks, SchedulingPolicy policy, boolean verbose) {
        int hyperperiod = JobTaskUtils.getHyperperiodForTasks(null, etTasks);
        ArrayList<ExtendedEtJob>[] extEtJobs = JobTaskUtils.getExtendedEtJobsFromEtTasksAs2dArray(etTasks, hyperperiod);
        return recursiveJobAssign(0,0, extEtJobs, policy, verbose);
    }

    private static boolean recursiveJobAssign(int curTaskId, int curJobId, ArrayList<ExtendedEtJob>[] extEtJob,
                                              SchedulingPolicy policy, boolean verbose) {
        if (curTaskId == extEtJob.length) {
            return simulateScenario(extEtJob, policy, verbose);
        }
        ExtendedEtJob curExtEtJob = extEtJob[curTaskId].get(curJobId);
        for (int et = curExtEtJob.getJob().getExecutionTimeMax(); et >= curExtEtJob.getJob().getExecutionTimeMin(); et--) {
            curExtEtJob.setExecutionTime(et);
            for (int rt = curExtEtJob.getJob().getReleaseTimeMax(); rt >= curExtEtJob.getJob().getReleaseTimeMin(); rt--) {
                curExtEtJob.setReleaseTime(rt);
                boolean result;
                if (extEtJob[curTaskId].size() == curJobId+1) {
                    result = recursiveJobAssign(curTaskId+1, 0, extEtJob, policy, verbose);
                } else {
                    result = recursiveJobAssign(curTaskId, curJobId+1, extEtJob, policy, verbose);
                }
                if (!result) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean simulateScenario(ArrayList<ExtendedEtJob>[] extEtJobs, SchedulingPolicy policy, boolean verbose) {
        if (verbose) {
            System.out.println("Execute Edf launched with policy: " + policy.toString());
            System.out.println("All fixed jobs:");
            for (int i = 0; i < extEtJobs.length; i++) {
                System.out.println(" + Task Id: " + i);
                for (ExtendedEtJob fj : extEtJobs[i]) {
                    System.out.println(" +++ " + fj.toString());
                }
            }
        }

        ExtendedEtJob[] appJobs = new ExtendedEtJob[extEtJobs.length];
        for (int i = 0; i < extEtJobs.length; i++) {
            appJobs[i] = extEtJobs[i].get(0);
        }

        //Main cycle
        int t = 0;
        int finishedTasks = 0; //After all jobs in a task are scheduled, this number is incremented
        while (finishedTasks < extEtJobs.length) {
            ExtendedEtJob pickedJob = null;
            if (verbose) {
                System.out.println(" --------- T:" + t + " --------- ");
                System.out.println(" - Applicable jobs:" );
                for (int i = 0; i < extEtJobs.length; i++) {
                    System.out.println(" -- Task ID: " + i + ", job: " + (appJobs[i] == null ? "null" : appJobs[i].toString()));
                }
            }
            if (policy == SchedulingPolicy.EDFFP) {
                pickedJob = SchedulingPolicies.edfFpPolicy(t, appJobs);
            } else if (policy == SchedulingPolicy.PRM) {
                pickedJob = SchedulingPolicies.prmPolicy(t, appJobs);
            } else if (policy == SchedulingPolicy.CP) {
                pickedJob = SchedulingPolicies.cpPolicy(t, appJobs);
            } else if (policy == SchedulingPolicy.CW) {
                pickedJob = SchedulingPolicies.cwPolicy(t, appJobs);
            }
            if (verbose) {
                System.out.println(" --------- T:" + t + " --------- ");
                System.out.println(" - Applicable jobs:" );
                for (int i = 0; i < extEtJobs.length; i++) {
                    System.out.println(" -- Task ID: " + i + ", job: " + (appJobs[i] == null ? "null" : appJobs[i].toString()));
                }
                System.out.println(" - Picked job: " + (pickedJob == null? "null" : pickedJob.toString()));
            }

            if (pickedJob == null) {
                int minR = Integer.MAX_VALUE;
                for (ExtendedEtJob appJob : appJobs) {
                    if (appJob == null || appJob.getReleaseTime() <= t) {
                        continue;
                    }
                    if (appJob.getReleaseTime() < minR) {
                        minR = appJob.getReleaseTime();
                    }
                }
                t = minR;
                //t++;
                continue;
            }
            if (t + pickedJob.getExecutionTime() > pickedJob.getJob().getDeadline()) {
                if (verbose) {
                    System.out.println(" ! Deadline miss");
                }
                return false;
            }
            t += pickedJob.getExecutionTime();

            int curTaskId = pickedJob.getJob().getTaskId();
            int nextPeriod = appJobs[curTaskId].getJob().getRepetition()+1;
            if (nextPeriod == extEtJobs[curTaskId].size()) {
                appJobs[curTaskId] = null;
                finishedTasks++;
                if (verbose) {
                    System.out.println(" - Task with ID: " + curTaskId + " is finished, total tasks finished: " + finishedTasks);
                }
            } else {
                appJobs[curTaskId] = extEtJobs[curTaskId].get(nextPeriod);
            }
        }
        return true;
    }

}
