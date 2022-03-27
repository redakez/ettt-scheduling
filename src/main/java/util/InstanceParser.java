package util;

import model.EtTask;
import model.TtTask;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Parses ET and TT tasks from a file
 */
public class InstanceParser {

    private static final int NUMBER_OF_CELLS = 8;

    private ArrayList<TtTask> ttTasks;
    private ArrayList<EtTask> etTasks;

    /**
     * Parses tasks from an input file and stores them into class variables ttTasks and etTasks
     * @param inputFilePath path to the input file
     * @throws IOException thrown when the input file is in an incorrect format or inaccessible
     */
    public void parseInput(String inputFilePath) throws IOException {
        ttTasks = new ArrayList<>();
        etTasks = new ArrayList<>();

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath)));
        String line = br.readLine();
        if (line == null) {
            throw new IOException("The input file is empty");
        }

        int curTaskId = 0;
        boolean warned = false;
        boolean etStarted = false;
        while ((line = br.readLine()) != null) {
            String[] stringCells = line.split(",");
            if (stringCells.length != NUMBER_OF_CELLS) {
                throw new IOException("Incorrect input file format (wrong number of cells)");
            }
            int[] cells = new int[NUMBER_OF_CELLS];
            for (int i = 0; i < NUMBER_OF_CELLS; i++) {
                cells[i] = Integer.parseInt(stringCells[i]);
            }
            if (cells[0] != curTaskId && !warned) {
                System.err.println("Warning: the input instances IDs differs from the used format (they will be ignored)");
                warned = true;
            }
            if (cells[7] == 0) { //If is a TT task
                if (etStarted) {
                    throw new IOException("Incorrect input file format (TT tasks must precede ET tasks)");
                }
                ttTasks.add(new TtTask(curTaskId, cells[1], cells[6], cells[3], cells[5]));
            } else {
                etStarted = true;
                etTasks.add(new EtTask(curTaskId, cells[1], cells[6], cells[2], cells[3], cells[4], cells[5], cells[7]));
            }
            curTaskId++;
        }

        for (EtTask et : etTasks) {
            if (et.getExecutionTimeMin() > et.getExecutionTimeMax() || et.getReleaseTimeMin() > et.getReleaseTimeMax()) {
                throw new IOException("On some event triggered task the min value is bigger than the corresponding max value in the input instance");
            }
            if (et.getPeriod() < 0 ||
                    et.getReleaseTimeMin() < 0 || et.getReleaseTimeMax() < 0 ||
                    et.getExecutionTimeMax() < 0 || et.getExecutionTimeMin() < 0 ||
                    et.getPriority() < 0 || et.getDeadline() < 0) {
                throw new IOException("Some ET task has a negative variable in the input instance");
            }
        }

        for (TtTask tt : ttTasks) {
            if (tt.getPeriod() < 0 ||
                    tt.getReleaseTime() < 0 ||
                    tt.getExecutionTime() < 0 ||
                    tt.getDeadline() < 0) {
                throw new IOException("Some TT task has a negative variable in the input instance");
            }
        }

    }

    public ArrayList<TtTask> getTtTasks() {
        return ttTasks;
    }

    public ArrayList<EtTask> getEtTasks() {
        return etTasks;
    }

}
