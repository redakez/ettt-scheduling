package model;

/**
 * TtTask (Time triggered Task) represents an infinite set of time triggered jobs.
 */
public class TtTask extends Task {

    private final int releaseTime;
    private final int executionTime;

    /**
     * See Task class for other parameters
     * @param releaseTime job release time relative to the period
     * @param executionTime execution time of each job
     */
    public TtTask(int id, int period, int deadline, int releaseTime, int executionTime) {
        super(id, period, deadline);
        this.releaseTime = releaseTime;
        this.executionTime = executionTime;
    }

    public TtJob getNthRepetition(int n) {
        return new TtJob(getId(), n, this.getPeriod(), getDeadline() + n*getPeriod(),
                getReleaseTime() + n*getPeriod(), getExecutionTime());

    }

    public int getReleaseTime() {
        return releaseTime;
    }

    public int getExecutionTime() {
        return executionTime;
    }
}
