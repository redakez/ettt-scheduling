package model;

/**
 * EtTask (Event triggered Task) represents an infinite set of event triggered jobs.
 */
public class EtTask extends Task {

    private final int releaseTimeMin;
    private final int releaseTimeMax;
    private final int executionTimeMin;
    private final int executionTimeMax;
    private int priority;

    /**
     * See Task class for other parameters
     * @param releaseTimeMin minimal release time relative to the period
     * @param releaseTimeMax maximal release time relative to the period
     * @param executionTimeMin minimal execution time of each job
     * @param executionTimeMax maximal execution time of each job
     * @param priority priority of each job
     */
    public EtTask(int id, int period, int deadline, int releaseTimeMin, int releaseTimeMax,
                  int executionTimeMin, int executionTimeMax, int priority) {
        super(id, period, deadline);
        this.releaseTimeMin = releaseTimeMin;
        this.releaseTimeMax = releaseTimeMax;
        this.executionTimeMin = executionTimeMin;
        this.executionTimeMax = executionTimeMax;
        this.priority = priority;
    }

    /**
     * Constructor which transforms time triggered task to event triggered task where:
     *     minimal release time = maximal release time
     *     minimal execution time = maximal execution time
     * @param task task from which to create the event triggered task
     * @param priority what priority should be given to the new event triggered task
     */
    public EtTask(TtTask task, int priority) {
        super(task.getId(), task.getPeriod(), task.getDeadline());
        this.releaseTimeMin = task.getReleaseTime();
        this.releaseTimeMax = task.getReleaseTime();
        this.executionTimeMin = task.getExecutionTime();
        this.executionTimeMax = task.getExecutionTime();
        this.priority = priority;
    }

    public EtTask(TtTask task) {
        this(task,0);
    }

    public EtJob getNthRepetition(int n) {
        return new EtJob(getId(), n, this.getPeriod(), getDeadline() + n*getPeriod(), getReleaseTimeMin() + n*getPeriod(),
                getReleaseTimeMax() + n*getPeriod(), getExecutionTimeMin(), getExecutionTimeMax(), getPriority());
    }


    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getReleaseTimeMin() {
        return releaseTimeMin;
    }

    public int getReleaseTimeMax() {
        return releaseTimeMax;
    }

    public int getExecutionTimeMin() {
        return executionTimeMin;
    }

    public int getExecutionTimeMax() {
        return executionTimeMax;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Job) {
            return super.equals(o);
        }
        return false;
    }

}
