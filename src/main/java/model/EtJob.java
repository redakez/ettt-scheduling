package model;

/**
 * EtJob (Event triggered job) represents one job in an event triggered task.
 * Unlike TtJob, its execution and release time may vary.
 */
public class EtJob extends Job implements Comparable<EtJob> {

    private int releaseTimeMin;
    private int releaseTimeMax;
    private final int executionTimeMin;
    private final int executionTimeMax;
    private final int priority;

    /**
     * See Job class for other parameters
     * @param releaseTimeMin minimal absolute release time
     * @param releaseTimeMax maximal absolute release time
     * @param executionTimeMin minimal execution time
     * @param executionTimeMax maximal execution time
     * @param priority priority of the job
     */
    public EtJob(int taskId, int repetition, int period, int deadline, int releaseTimeMin,
                 int releaseTimeMax, int executionTimeMin, int executionTimeMax, int priority) {
        super(taskId, repetition, period, deadline);
        this.releaseTimeMin = releaseTimeMin;
        this.releaseTimeMax = releaseTimeMax;
        this.executionTimeMin = executionTimeMin;
        this.executionTimeMax = executionTimeMax;
        this.priority = priority;
    }

    /**
     * Constructor which transforms time triggered job to event triggered job where:
     *     minimal release time = maximal release time
     *     minimal execution time = maximal execution time
     * @param job task from which to create the event triggered task
     * @param priority what priority should be given to the new event triggered job
     */
    public EtJob(TtJob job, int priority) {
        super(job.getTaskId(), job.getRepetition(), job.getPeriod(), job.getDeadline());
        this.releaseTimeMin = job.getReleaseTime();
        this.releaseTimeMax = job.getReleaseTime();
        this.executionTimeMin = job.getExecutionTime();
        this.executionTimeMax = job.getExecutionTime();
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

    public void setReleaseTimeMin(int releaseTimeMin) {
        this.releaseTimeMin = releaseTimeMin;
    }

    public void setReleaseTimeMax(int releaseTimeMax) {
        this.releaseTimeMax = releaseTimeMax;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "EtJob-ID:" + getTaskId() + "-R:" + releaseTimeMin + "|" + releaseTimeMax
                + "-E:" + executionTimeMin + "|" + executionTimeMax + "-P:" + priority + "-D:" + getDeadline();
    }

    //Comparing on the basis of EDF-FP priority
    @Override
    public int compareTo(EtJob other) {
        if (other.priority != priority) {
            return Integer.compare(priority, other.priority);
        }
        if (other.getDeadline() != getDeadline()) {
            return Integer.compare(getDeadline(), other.getDeadline());
        }
        return Integer.compare(getTaskId(), other.getTaskId());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Job) {
            return super.equals(o);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }


}
