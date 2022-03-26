package util;

import model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * Class with static function that write out results
 */
public class OutputUtils {

    /**
     * Creates an input file of tasks for given time and event triggered tasks.
     * @param filePath file path where the file should be created, includes the file name
     * @param ttTasks collection of time triggered tasks, can be null
     * @param etTasks collection of event triggered tasks, can be null
     * @throws IOException file could not be created or was removed during the writing process
     */
    public static void writeTasksToFile(String filePath, Collection<TtTask> ttTasks,
                                        Collection<EtTask> etTasks) throws IOException {
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
        f.getParentFile().mkdirs(); //Create specified directory if it does not exist
        f.createNewFile();
        FileWriter fw = new FileWriter(filePath);

        fw.write("Task ID,Period,Release Min,Release Max,Execution Min,Execution Max,Deadline,Priority\n");
        if (ttTasks != null) {
            writeTtTasksToFile(fw, ttTasks);
        }
        if (etTasks != null) {
            writeEtTasksToFile(fw, etTasks);
        }
        fw.close();
    }

    /**
     * Writes all time triggered tasks into a file.
     * @param fw file writer to use when writing to file
     * @param ttTasks collection of time triggered tasks
     * @throws IOException file was removed during the writing process or the file writer is closed
     */
    private static void writeTtTasksToFile(FileWriter fw, Collection<TtTask> ttTasks) throws IOException {
        for (TtTask tt : ttTasks) {
            writeLineOfIntsToCsv(
                    fw,
                    tt.getId(),
                    tt.getPeriod(),
                    tt.getReleaseTime(), tt.getReleaseTime(),
                    tt.getExecutionTime(), tt.getExecutionTime(),
                    tt.getDeadline(),
                    0);
        }
    }

    /**
     * Writes all event triggered tasks into a file.
     * @param fw file writer to use when writing to file
     * @param etTasks collection of event triggered tasks
     * @throws IOException file was removed during the writing process or the file writer is closed
     */
    private static void writeEtTasksToFile(FileWriter fw, Collection<EtTask> etTasks) throws IOException {
        for (EtTask et : etTasks) {
            writeLineOfIntsToCsv(
                    fw,
                    et.getId(),
                    et.getPeriod(),
                    et.getReleaseTimeMin(), et.getReleaseTimeMax(),
                    et.getExecutionTimeMin(), et.getExecutionTimeMax(),
                    et.getDeadline(),
                    et.getPriority());
        }
    }

    public static void writeLineOfIntsToCsv(FileWriter fw, int... args) throws IOException {
        for (int i = 0; i < args.length; i++) {
            fw.write(String.valueOf(args[i]));
            if (i == args.length-1) {
                fw.write('\n');
            } else {
                fw.write(',');
            }
        }
    }

    public static void writeLineOfStringsToCsv(FileWriter fw, String... args) throws IOException {
        for (int i = 0; i < args.length; i++) {
            fw.write(String.valueOf(args[i]));
            if (i == args.length-1) {
                fw.write('\n');
            } else {
                fw.write(',');
            }
        }
    }

    /**
     * Writes start times to a file
     * @param filepath file into where the jobs should be written to
     * @throws IOException thrown when the file could not be created
     */
    public static void writeStartTimesToFile(String filepath, int[][] startTimes) throws IOException {
        FileWriter fw = new FileWriter(filepath);
        fw.write("Task ID,Job repetition,Start time\n");
        for (int i = 0; i < startTimes.length; i++) {
            for (int j = 0; j < startTimes[i].length; j++) {
                OutputUtils.writeLineOfIntsToCsv(fw, i, j, startTimes[i][j]);
            }
        }
        fw.close();
    }

}
