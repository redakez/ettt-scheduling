package model;

/**
 * TtJob (Time triggered job) represents one job in a time triggered task.
 * It always has the same release and execution time.
 */
public class TtJob extends Job {

    private final int releaseTime;
    private final int executionTime;

    /**
     * See Job class for other parameters
     * @param deadline job's absolute deadline
     * @param releaseTime job's absolute release time
     * @param executionTime execution time of the job
     */
    public TtJob(int taskId, int repetition, int period, int deadline, int releaseTime, int executionTime) {
        super(taskId, repetition, period, deadline);
        this.releaseTime = releaseTime;
        this.executionTime = executionTime;
    }

    public int getReleaseTime() {
        return releaseTime;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    @Override
    public String toString() {
        return "EtJob-ID:" + getTaskId() + "-R:" + releaseTime + "-E:" + executionTime + "-D:" + getDeadline();
    }

}
