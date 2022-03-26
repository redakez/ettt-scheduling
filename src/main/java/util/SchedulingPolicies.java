package util;

import model.EtJob;
import model.ExtendedEtJob;
import java.util.Arrays;

public class SchedulingPolicies {

    //EDF-FP policy if all jobs released at their minimum release times
    public static EtJob edfFpPolicyMinRelease(int time, EtJob[] applicableJobs) {
        EtJob ret = null;
        for (EtJob eej : applicableJobs) {
            if (eej == null || eej.getReleaseTimeMin() > time) {
                continue;
            }
            if (ret == null || ret.compareTo(eej) > 0) {
                ret = eej;
            }
        }
        return ret;
    }

    public static ExtendedEtJob edfFpPolicy(int time, ExtendedEtJob[] applicableJobs) {
        ExtendedEtJob ret = null;
        for (ExtendedEtJob eej : applicableJobs) {
            if (eej == null || eej.getReleaseTime() > time) {
                continue;
            }
            if (ret == null || ret.getJob().compareTo(eej.getJob()) > 0) {
                ret = eej;
            }
        }
        return ret;
    }

    public static ExtendedEtJob prmPolicy(int time, ExtendedEtJob[] applicableJobs) {
        ExtendedEtJob ret = null;
        EtJob cJob = null;
        boolean anyJob = false;
        //Determine what is the critical job first
        for (ExtendedEtJob eej : applicableJobs) {
            if (eej == null) {
                continue;
            }
            anyJob = true;
            if (eej.getJob().getPriority() != 0) {
                continue;
            }
            if (cJob == null || eej.getJob().getExecutionTimeMax() < cJob.getExecutionTimeMax()
                    || (cJob.getExecutionTimeMax() == eej.getJob().getExecutionTimeMax() && cJob.getTaskId() > eej.getJob().getTaskId())) {
                cJob = eej.getJob();
            }
        }
        if (!anyJob) { //All elements in applicable jobs are null
            return null;
        }
        int LFT = cJob == null ? Integer.MAX_VALUE : cJob.getDeadline() - cJob.getExecutionTimeMax();
        //Find jobs worthy of being scheduled
        for (ExtendedEtJob eej : applicableJobs) {
            if (eej == null || eej.getReleaseTime() > time ||
                    (!eej.getJob().equals(cJob) && eej.getJob().getExecutionTimeMax() + time > LFT)) {
                continue;
            }
            if (ret == null || ret.getJob().compareTo(eej.getJob()) > 0) {
                ret = eej;
            }
        }
        return ret;
    }

    public static ExtendedEtJob cpPolicy(int time, ExtendedEtJob[] applicableJobs) {
        ExtendedEtJob ret = null;
        EtJob cJob = null;
        //Determine what is the critical job first
        for (ExtendedEtJob eej : applicableJobs) {
            if (eej == null) {
                continue;
            }
            if (cJob == null || cJob.getDeadline() > eej.getJob().getDeadline() ||
                    (cJob.getDeadline() == eej.getJob().getDeadline() && cJob.getTaskId() > eej.getJob().getTaskId())) {
                cJob = eej.getJob();
            }
        }
        if (cJob == null) { //All elements in applicable jobs are null
            return null;
        }
        int LFT = cJob.getDeadline() - cJob.getExecutionTimeMax();
        //Find jobs worthy of being scheduled
        for (ExtendedEtJob eej : applicableJobs) {
            if (eej == null || eej.getReleaseTime() > time ||
                    (!eej.getJob().equals(cJob) && eej.getJob().getExecutionTimeMax() + time > LFT)) {
                continue;
            }
            if (ret == null || ret.getJob().compareTo(eej.getJob()) > 0) {
                ret = eej;
            }
        }
        return ret;
    }

    public static ExtendedEtJob cwPolicy(int time, ExtendedEtJob[] applicableJobs) {
        ExtendedEtJob ret = null;
        //Stack the jobs by deadline
        ExtendedEtJob[] sortedAppJobs = new ExtendedEtJob[applicableJobs.length];
        System.arraycopy(applicableJobs, 0, sortedAppJobs, 0, applicableJobs.length);
        //Sort by deadline, then by id, leave null elements at the end of the array
        Arrays.sort(sortedAppJobs, (et1, et2) -> {
            if (et1 == null) {
                if (et2 == null) {
                    return 0;
                }
                return 1;
            } else if (et2 == null) {
                return -1;
            }
            if (et1.getJob().getDeadline() < et2.getJob().getDeadline()) {
                return 1;
            } else if (et1.getJob().getDeadline() > et2.getJob().getDeadline()) {
                return -1;
            } else {
                if (et1.getJob().getTaskId() < et2.getJob().getTaskId()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        EtJob cJob = null;
        int cwTime = Integer.MAX_VALUE;
        for (int i = 0; i < applicableJobs.length; i++) {
            if (sortedAppJobs[i] == null) {
                break;
            }
            cJob = sortedAppJobs[i].getJob();
            if (cJob.getDeadline() < cwTime) {
                cwTime = cJob.getDeadline() - cJob.getExecutionTimeMax();
            } else {
                cwTime -= cJob.getExecutionTimeMax();
            }
        }
        if (cJob == null) { //All elements in applicable jobs are null
            return null;
        }
        int LFT = cwTime;
        //Find jobs worthy of being scheduled
        for (ExtendedEtJob eej : applicableJobs) {
            if (eej == null || eej.getReleaseTime() > time ||
                    (!eej.getJob().equals(cJob) && eej.getJob().getExecutionTimeMax() + time > LFT)) {
                continue;
            }
            if (ret == null || ret.getJob().compareTo(eej.getJob()) > 0) {
                ret = eej;
            }
        }
        return ret;
    }

}
