import alg_et.EtInstanceBruteForceTest;
import alg_et.EtScheduleGraphTest;
import alg_ettt.EtttBruteForceScheduler;
import alg_ettt.EtttFixationGraph;
import model.*;
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
            if (algorithmName.equals("ET-BF")) {
                if (!ttTasks.isEmpty()) {
                    throw new IllegalArgumentException("Input instance contains TT tasks, but the algorithm takes only ET tasks");
                }
                schedulable = EtInstanceBruteForceTest.launchTest(etTasks, policy, false);
            } else if (algorithmName.equals("ET-SG")) {
                if (!ttTasks.isEmpty()) {
                    throw new IllegalArgumentException("Input instance contains TT tasks, but the algorithm takes only ET tasks");
                }
                EtScheduleGraphTest sgc = new EtScheduleGraphTest(etTasks);
                schedulable = sgc.generateGraphTest(!generateFullGraph, policy, saveGraph);
                if (saveGraph) {
                    sgc.saveGraphToFile(fileOutPath + ".sg.dot", false);
                }
            } else if (algorithmName.startsWith("ETTT-BF")) {
                EtttBruteForceScheduler ebfs = new EtttBruteForceScheduler(ttTasks, etTasks, policy);
                if (algorithmName.equals("ETTT-BF-WJ")) {
                    schedulable = ebfs.processTasks(true);
                } else if (algorithmName.equals("ETTT-BF-NJ")) {
                    schedulable = ebfs.processTasksNoJitter(true);
                } else {
                    throw new IllegalArgumentException("Unknown algorithm: " + algorithmName);
                }
                if (schedulable && saveStartTimes) {
                    int[][] startTimes = ebfs.getStartTimes();
                    OutputUtils.writeStartTimesToFile(fileOutPath + ".st.csv", startTimes);
                }
            } else if (algorithmName.equals("ETTT-FG")) {
                int hyperperiod = JobTaskUtils.getHyperperiodForTasks(ttTasks, etTasks);
                ArrayList<TtJob>[] ttJobs = JobTaskUtils.getTtJobsFromTtTasksAs2dArray(ttTasks, hyperperiod);
                ArrayList<EtJob>[] etJobs = JobTaskUtils.getEtJobsFromEtTasksAs2dArray(etTasks, hyperperiod);
                EtttFixationGraph esgh = new EtttFixationGraph(ttJobs, etJobs);
                schedulable = esgh.createStartTimeGraphNoIip();
                if (schedulable && saveStartTimes) {
                    int[][] startTimes = esgh.getStartTimesFromGraph();
                    OutputUtils.writeStartTimesToFile(fileOutPath + ".st.csv", startTimes);
                }
                if (saveGraph) {
                    esgh.saveGraphToFile(ap.inputFilePath + ".fg.dot", true, true);
                }
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
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("   -a      Specify the algorithm (ET-BF, ET-SG, ETTT-BF-WJ, ETTT-BF-NJ, ETTT-FG)");
        System.out.println("                 ET-BF: brute force algorithm which finds if a set of ET tasks is schedulable");
        System.out.println("                 ET-SG: schedule graph based algorithm which finds if a set of ET tasks is schedulable");
        System.out.println("                 ETTT-BF-WJ: brute force algorithm which finds start times for TT tasks with jitter");
        System.out.println("                 ETTT-BF-NJ: brute force algorithm which finds start times for TT tasks with zero jitter");
        System.out.println("                 ETTT-FG: fixation graph generation algorithm which finds start times for TT tasks with jitter (work only for the EDF-FP policy)");
        System.out.println();
        System.out.println("   -p      Specify the policy (EDF-FP (default), P-RM, CP, CW)");
        System.out.println("                 EDF-FP: Earliest deadline first Fixed priority");
        System.out.println("                 P-RM: Precatious-Rate monotonic");
        System.out.println("                 CP: Critical point");
        System.out.println("                 CW: Critical window");
        System.out.println();
        System.out.println("   -g      Save a schedule/fixation graph, if the specified algorithm uses it");
        System.out.println("   -f      Creates the entire schedule graph even if there is a deadline miss");
        System.out.println("   -s      Saves start times into a file, if the specified algorithm generates them");
        System.out.println();
        System.out.println("   -v      Visualize the instance (interactable with arrows keys and W,A,S,D,Q,E keys)");
        System.out.println("   -i      Print general information about the instance");
        System.out.println("   -h      Print this help");
    }

}