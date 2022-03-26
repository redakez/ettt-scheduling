package model;

/**
 * An ET job used in brute force algorithms.
 * It has defined release and execution time
 */
public class ExtendedEtJob {
    private EtJob job;
    private int releaseTime = -1;
    private int executionTime = -1;

    public ExtendedEtJob(EtJob job) {
        this.job = job;
    }

    public ExtendedEtJob(EtJob job, int releaseTime, int executionTime) {
        this.job = job;
        this.releaseTime = releaseTime;
        this.executionTime = executionTime;
    }

    @Override
    public String toString() {
        return "ExtendedEtJob - EtJob: [" + job.toString() + "], R: " + releaseTime + ", C: " + executionTime;
    }

    public EtJob getJob() {
        return job;
    }

    public int getReleaseTime() {
        return releaseTime;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public void setReleaseTime(int releaseTime) {
        this.releaseTime = releaseTime;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }

}
