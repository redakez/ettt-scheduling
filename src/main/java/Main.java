import alg_et.EtInstanceBruteForceTest;
import alg_et.SimpleScheduleGraphCreator;
import alg_ettt.EtttBruteForceScheduler;
import model.EtTask;
import model.SchedulingPolicy;
import model.TtTask;
import ui.ScheduleVisualizerWindow;
import util.*;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws Exception {
        ArgParser ap = new ArgParser(args);

        //Print help if there is a -h flag
        if (ap.presentFlags.contains('h')) {
            printHelp();
            return;
        }

        if (ap.inputFilePath == null) {
            throw new IllegalArgumentException("Input file path not specified");
        }

        //Parse the input
        InstanceParser ip = new InstanceParser();
        ip.parseInput(ap.inputFilePath);
        ArrayList<TtTask> ttTasks = ip.getTtTasks();
        ArrayList<EtTask> etTasks = ip.getEtTasks();

        //Show extra info
        if (ap.presentFlags.contains('i')) {
            JobTaskUtils.printInstanceStatisticsDetailed(ttTasks, etTasks);
        }

        if (ap.presentFlags.contains('v')) {
            ScheduleVisualizerWindow svw = new ScheduleVisualizerWindow(ttTasks, etTasks);
        }

        //Determine the scheduling policy
        SchedulingPolicy policy = SchedulingPolicy.EDFFP;
        if (ap.presentArgs.containsKey('p')) {
            String stringPolicy = ap.presentArgs.get('p');
            switch (stringPolicy) {
                case "EDF-FP": policy = SchedulingPolicy.EDFFP; break;
                case "P-RM": policy = SchedulingPolicy.PRM; break;
                case "CP": policy = SchedulingPolicy.CP; break;
                case "CW": policy = SchedulingPolicy.CW; break;
                default: throw new IllegalArgumentException("Unknown policy: " + stringPolicy);
            }
        }

        //Determine output arguments
        boolean saveGraph = ap.presentFlags.contains('g');
        boolean saveStartTimes = ap.presentFlags.contains('s');
        boolean generateFullGraph = ap.presentFlags.contains('f');
        String fileOutPath = ap.inputFilePath;

        //Determine the algorithm and run it
        Boolean schedulable = null;
        if (ap.presentArgs.containsKey('a')) {
            String algorithmName = ap.presentArgs.get('a');
            if (algorithmName.equals("ETBF")) {
                schedulable = EtInstanceBruteForceTest.launchTest(etTasks, policy, false);
            } else if (algorithmName.equals("ETSG")) {
                SimpleScheduleGraphCreator sgc = new SimpleScheduleGraphCreator(etTasks);
                schedulable = sgc.generateGraphTest(!generateFullGraph, policy, saveGraph);
                if (saveGraph) {
                    sgc.saveGraphToFile(fileOutPath + ".sg.dot", false);
                }
            } else if (algorithmName.startsWith("ETTTBF")) {
                EtttBruteForceScheduler ebfs = new EtttBruteForceScheduler(ttTasks, etTasks, policy);
                if (algorithmName.equals("ETTTBF")) {
                    schedulable = ebfs.processTasks(true);
                } else if (algorithmName.equals("ETTTBF-NJ")) {
                    schedulable = ebfs.processTasksNoJitter(true);
                } else {
                    throw new IllegalArgumentException("Unknown algorithm: " + algorithmName);
                }
                if (schedulable && saveStartTimes) {
                    int[][] startTimes = ebfs.getStartTimes();
                    OutputUtils.writeStartTimesToFile(fileOutPath + ".st.csv", startTimes);
                }
            } else if (algorithmName.equals("ETTTSG")) {
                throw new RuntimeException("Not implemented yet");
            } else {
                throw new IllegalArgumentException("Unknown algorithm: " + algorithmName);
            }
        }

        //Printing the result
        if (schedulable != null) {
            System.out.println("Result: " + (schedulable ? "" : "non-") + "schedulable");
        }
    }

    public static void printHelp() {
        String jarName = new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
        System.out.println("Usage: java -jar " + jarName + " [OPTIONS]... INSTANCE_PATH...\n");
        System.out.println("Implements various ET+TT scheduling related algorithms");
        System.out.println("Optional parameters:");
        System.out.println("   -a      Specify the algorithm (ETBF, ETSG, ETTTBF, ETTTBF-NJ, ETTTSG)");
        System.out.println("   -p      Specify the policy (EDF-FP, P-RM, CP, CW)");
        System.out.println();
        System.out.println("   -g      Save a schedule graph, if the specified algorithm uses it");
        System.out.println("   -s      Saves start times into a file, if the specified algorithm generates them");
        System.out.println("   -f      Creates the entire schedule graph even if there is a deadline miss");
        System.out.println();
        System.out.println("   -v      Visualize the instance");
        System.out.println("   -i      Print information about the instance");
        System.out.println("   -h      Print this help");
    }

}