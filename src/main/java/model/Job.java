package model;

import java.util.Objects;

/**
 * This class contains properties shared by event and time triggered jobs.
 */
public abstract class Job {

    private final int taskId;
    private final int repetition;
    private final int period;
    private int deadline;

    /**
     * @param taskId ID of Task the job is related to
     * @param repetition specifies to which period (of a Task) the job belongs to
     * @param period period of parent task
     * @param deadline job's absolute deadline
     */
    public Job(int taskId, int repetition, int period, int deadline) {
        this.taskId = taskId;
        this.repetition = repetition;
        this.period = period;
        this.deadline = deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getRepetition() {
        return repetition;
    }

    public int getPeriod() {
        return period;
    }

    public int getDeadline() {
        return deadline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return taskId == job.taskId && repetition == job.repetition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, repetition);
    }
}
