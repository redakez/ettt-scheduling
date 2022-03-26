package model;

/**
 * This class contains properties shared by event and time triggered tasks.
 * Both types of tasks represent an infinite set of repeating jobs.
 * See also EtTask and TtTask classes
 */
public abstract class Task {

    private int id;
    private final int period;
    private final int deadline;

    /**
     * @param id unique identifier of a task
     * @param period period after which a job repeats
     * @param deadline deadline relative to a job period
     */
    public Task(int id, int period, int deadline) {
        this.id = id;
        this.period = period;
        this.deadline = deadline;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPeriod() {
        return period;
    }

    public int getDeadline() {
        return deadline;
    }
}
