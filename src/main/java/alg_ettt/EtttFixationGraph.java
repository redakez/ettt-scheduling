package alg_ettt;

import model.EtJob;
import model.TtJob;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static util.SchedulingPolicies.edfFpPolicyMaxRelease;

/**
 * A heuristic algorithm which finds a set of start times for TT jobs.
 * It has a chance to return a false negative but will never return a false positive.
 */
public class EtttFixationGraph {

    public int lastNodeId = 0;

    class Node {
        int id;
        int[] ttTaskPeriods;
        int[] etTaskPeriods;
        ArrayList<Node> parents;
        ArrayList<Node> children;

        public Node(int[] ttTaskPeriods, int[] etTaskPeriods) {
            this.id = lastNodeId++;
            this.etTaskPeriods = etTaskPeriods;
            this.ttTaskPeriods = ttTaskPeriods;
            this.parents = new ArrayList<>();
            this.children = new ArrayList<>();
        }

        public EtJob[] getCurEtJobs() {
            EtJob[] curJobs = new EtJob[etJobs.length];
            for (int i = 0; i < etJobs.length; i++) {
                int curTaskPeriod = this.etTaskPeriods[i];
                if (etJobs[i].size() != curTaskPeriod) {
                    curJobs[i] = etJobs[i].get(curTaskPeriod);
                }
            }
            return curJobs;
        }

        public TtJob[] getCurTtJobs() {
            TtJob[] curJobs = new TtJob[ttJobs.length];
            for (int i = 0; i < ttJobs.length; i++) {
                int curTaskPeriod = this.ttTaskPeriods[i];
                if (ttJobs[i].size() != curTaskPeriod) {
                    curJobs[i] = ttJobs[i].get(curTaskPeriod);
                }
            }
            return curJobs;
        }

        @Override
        public String toString() {
            StringBuilder bobTheStringBuilder = new StringBuilder();
            bobTheStringBuilder.append("TT:[");
            for (int i = 0; i < ttTaskPeriods.length; i++) {
                bobTheStringBuilder.append(ttTaskPeriods[i]);
                if (i != ttTaskPeriods.length-1) {
                    bobTheStringBuilder.append(',');
                }
            }
            bobTheStringBuilder.append("]-ET:[");
            for (int i = 0; i < etTaskPeriods.length; i++) {
                bobTheStringBuilder.append(etTaskPeriods[i]);
                if (i != etTaskPeriods.length-1) {
                    bobTheStringBuilder.append(',');
                }
            }
            bobTheStringBuilder.append(']');
            return bobTheStringBuilder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Arrays.equals(ttTaskPeriods, node.ttTaskPeriods) && Arrays.equals(etTaskPeriods, node.etTaskPeriods);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(ttTaskPeriods);
            result = 31 * result + Arrays.hashCode(etTaskPeriods);
            return result;
        }
    }

    //Regular node
    class EtNode extends Node {

        int min;
        int max;
        boolean causedDeadlineMiss;

        public EtNode(int[] ttTaskPeriods, int[] etTaskPeriods, int min, int max) {
            this(ttTaskPeriods, etTaskPeriods, min, max, false);
        }

        public EtNode(int[] ttTaskPeriods, int[] etTaskPeriods, int min, int max, boolean causedDeadlineMiss) {
            super(ttTaskPeriods, etTaskPeriods);
            this.min = min;
            this.max = max;
            this.causedDeadlineMiss = causedDeadlineMiss;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public EtNode getNewNodeForEtJob(EtJob job, int minRelease, int maxRelease) {
            int[] newEtTaskPeriods = Arrays.copyOf(etTaskPeriods, etTaskPeriods.length);
            newEtTaskPeriods[job.getTaskId() - ttJobs.length]++;
            EtNode nextNode = new EtNode(ttTaskPeriods, newEtTaskPeriods,
                    minRelease+job.getExecutionTimeMin(),
                    maxRelease+job.getExecutionTimeMax(),
                    maxRelease+job.getExecutionTimeMax() > job.getDeadline());
            nextNode.parents.add(this);
            return nextNode;
        }

        public void mergeNodeWith(EtNode toMergeNode) {
            min = Math.min(min, toMergeNode.min);
            max = Math.max(max, toMergeNode.max);
            causedDeadlineMiss = causedDeadlineMiss || toMergeNode.causedDeadlineMiss;
            this.parents.addAll(toMergeNode.parents);
            toMergeNode.parents = null;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return "(" + min + "|" + max + ")-" + super.toString();
        }
    }

    //Decision node
    class TtNode extends Node {

        int time;
        CombinationEnumerator enumerator;

        public TtNode(int[] ttTaskPeriods, int[] etTaskPeriods, int time) {
            super(ttTaskPeriods, etTaskPeriods);
            this.time = time;
            this.enumerator = new CombinationEnumerator(this);
        }

        public EtNode generateNextChild() {
            EtNode nextNode = this.enumerator.getNextCombination();
            if (nextNode != null) {
                nextNode.parents.add(this);
                this.children.add(nextNode);
            }
            return nextNode;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return "(" + time + ")-" + super.toString();
        }
    }

    //The modified Bratley's algorithm
    public class CombinationEnumerator {

        int lastBratleyNodeId = 0;

        class BratleyNode {
            int[] extraTaskPeriods;
            int t;
            int id; //Used only in visualizations
            BratleyNode bestCandidateParent;
            ArrayList<BratleyNode> parents;
            ArrayList<BratleyNode> children;

            public BratleyNode(int[] extraTaskPeriods, int t) {
                this.extraTaskPeriods = extraTaskPeriods;
                this.t = t;
                this.id = lastBratleyNodeId++;
                this.parents = new ArrayList<>();
                this.children = new ArrayList<>();
            }

            public ArrayList<BratleyNode> getNextNodes() {
                ArrayList<BratleyNode> ret = new ArrayList<>();
                boolean deadlineMiss = false;

                for (int i = 0; i < ttJobs.length; i++) {
                    int curRepetition = parent.ttTaskPeriods[i] + extraTaskPeriods[i];
                    if (ttJobs[i].size() == curRepetition) {
                        continue;
                    }
                    TtJob curJob = ttJobs[i].get(curRepetition);
                    if (Math.max(curJob.getReleaseTime(), t) + curJob.getExecutionTime() > curJob.getDeadline()) {
                        deadlineMiss = true;
                        break;
                    }
                }
                if (deadlineMiss) {
                    return ret;
                }
                for (int i = 0; i < ttJobs.length; i++) {
                    int curRepetition = parent.ttTaskPeriods[i] + extraTaskPeriods[i];
                    if (ttJobs[i].size() == curRepetition) {
                        continue;
                    }
                    TtJob curJob = ttJobs[i].get(curRepetition);
                    if (t < curJob.getReleaseTime() && curJob.getReleaseTime() > nextEtJobRelease) {
                        continue;
                    }
                    int nextTime = Math.max(t, curJob.getReleaseTime()) + curJob.getExecutionTime();
                    int[] nextTaskPeriods = Arrays.copyOf(extraTaskPeriods, extraTaskPeriods.length);
                    nextTaskPeriods[i]++;
                    BratleyNode newNode = new BratleyNode(nextTaskPeriods, nextTime);
                    this.children.add(newNode);
                    newNode.parents.add(this);
                    ret.add(newNode);
                }
                return ret;
            }

            public boolean isViable() {
                //Has all the needed TT jobs completed
                for (int i = 0; i < ttJobs.length; i++) {
                    if (extraTaskPeriods[i] < minimalExtraTaskPeriods[i]) {
                        return false;
                    }
                }
                //There are no TT to execute before the next ET release
                for (int i = 0; i < ttJobs.length; i++) {
                    int curRepetition = parent.ttTaskPeriods[i] + extraTaskPeriods[i];
                    if (ttJobs[i].size() == curRepetition) {
                        continue;
                    }
                    TtJob curJob = ttJobs[i].get(curRepetition);
                    if (Math.max(this.t, curJob.getReleaseTime()) + curJob.getExecutionTime() <= nextEtJobRelease) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                BratleyNode node = (BratleyNode) o;
                return Arrays.equals(extraTaskPeriods, node.extraTaskPeriods);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(extraTaskPeriods);
            }

            @Override
            public String toString() {
                StringBuilder bobTheStringBuilder = new StringBuilder();
                bobTheStringBuilder.append("T:" + this.t + "-EXT:[");
                for (int i = 0; i < extraTaskPeriods.length; i++) {
                    bobTheStringBuilder.append(extraTaskPeriods[i]);
                    if (i != extraTaskPeriods.length-1) {
                        bobTheStringBuilder.append(',');
                    }
                }
                bobTheStringBuilder.append(']');
                return bobTheStringBuilder.toString();
            }

        }

        private final TtNode parent;
        private final int[] minimalExtraTaskPeriods;
        private final int nextEtJobRelease;
        private boolean noSolutions;
        private final BratleyNode rootBratley;
        private BratleyNode lastReturnedNode;
        private ArrayList<BratleyNode> curLevelNodes;
        private int curLevelNodesIndex;

        public CombinationEnumerator(TtNode parent) {
            this.noSolutions = false;
            this.parent = parent;
            this.nextEtJobRelease = getNextEtJobRelease();
            int nextExpectedDecisionTime = this.getNextExpectedDecisionTime();
            if (nextExpectedDecisionTime == Integer.MAX_VALUE) {
                this.minimalExtraTaskPeriods = getMaximalTtTaskPeriods();
            } else {
                this.minimalExtraTaskPeriods = getMinimalTtTaskPeriodsForTime(nextExpectedDecisionTime);
            }
            this.rootBratley = new BratleyNode(new int[ttJobs.length], parent.time);
            this.curLevelNodes = new ArrayList<>();
            curLevelNodes.add(rootBratley);
            this.curLevelNodesIndex = 0;
        }

        private int getNextEtJobRelease() {
            EtJob[] curEtJobs = parent.getCurEtJobs();
            int minRelease = Integer.MAX_VALUE;
            for (EtJob ej : curEtJobs) {
                if (ej == null) {
                    continue;
                }
                minRelease = Math.min(minRelease, ej.getReleaseTimeMin());
            }
            return Math.max(parent.time, minRelease);
        }

        private int getNextExpectedDecisionTime() {
            EtJob[] curEtJobs = parent.getCurEtJobs();
            int earliestCrRelease = Integer.MAX_VALUE;
            for (EtJob ej : curEtJobs) {
                if (ej == null) {
                    continue;
                }
                earliestCrRelease = Math.min(earliestCrRelease, ej.getReleaseTimeMax());
            }
            int t = Math.max(parent.time, earliestCrRelease);
            EtJob[] appEtJobs = parent.getCurEtJobs();
            while (true) {
                EtJob pickedJob = edfFpPolicyMaxRelease(t, appEtJobs);
                if (pickedJob == null) { //No ET job is released at this time
                    return t;
                }
                t += pickedJob.getExecutionTimeMax();
                if (t > pickedJob.getDeadline()) {
                    this.noSolutions = true;
                    return Integer.MAX_VALUE;
                }
                int curTaskId = pickedJob.getTaskId()  - ttJobs.length;
                int nextPeriod = appEtJobs[curTaskId].getRepetition()+1;
                if (nextPeriod == etJobs[curTaskId].size()) {
                    appEtJobs[curTaskId] = null;
                } else {
                    appEtJobs[curTaskId] = etJobs[curTaskId].get(nextPeriod);
                }
            }
        }

        //Case when all ET jobs are completed...
        private int[] getMaximalTtTaskPeriods() {
            int[] ret = new int[ttJobs.length];
            for (int i = 0; i < ttJobs.length; i++) {
                while (true) {
                    int curRepetition = parent.ttTaskPeriods[i] + ret[i];
                    if (ttJobs[i].size() == curRepetition) {
                        break;
                    }
                    ret[i]++;
                }
            }
            return ret;
        }

        private int[] getMinimalTtTaskPeriodsForTime(int t) {
            int[] ret = new int[ttJobs.length];
            for (int i = 0; i < ttJobs.length; i++) {
                while (true) {
                    int curRepetition = parent.ttTaskPeriods[i] + ret[i];
                    if (ttJobs[i].size() == curRepetition) {
                        break;
                    }
                    TtJob curTtJob = ttJobs[i].get(curRepetition);
                    if (t + curTtJob.getExecutionTime() <= curTtJob.getDeadline()) {
                        break;
                    }
                    ret[i]++;
                }
            }
            return ret;
        }

        private EtNode getEtNodeForBratleyNode(BratleyNode bn) {
            int[] ttTaskPeriods = new int[ttJobs.length];
            for (int i = 0; i < ttJobs.length; i++) {
                ttTaskPeriods[i] = bn.extraTaskPeriods[i] + parent.ttTaskPeriods[i];
            }
            return new EtNode(ttTaskPeriods, parent.etTaskPeriods, bn.t, bn.t);
        }

        private void generateNextLevel() {
            HashMap<BratleyNode, ArrayList<BratleyNode>> bins = new HashMap<>();
            ArrayList<BratleyNode> nextLevelNodes = new ArrayList<>();
            for (BratleyNode bn : curLevelNodes) {
                ArrayList<BratleyNode> expandedNodes = bn.getNextNodes();
                for (BratleyNode nextLevelBn : expandedNodes) {
                    ArrayList<BratleyNode> curBin = bins.get(nextLevelBn);
                    if (curBin == null) {
                        curBin = new ArrayList<>();
                        curBin.add(nextLevelBn);
                        bins.put(nextLevelBn, curBin);
                    } else {
                        curBin.add(nextLevelBn);
                    }
                }
            }
            for (ArrayList<BratleyNode> list : bins.values()) {
                if (list.size() == 0) {
                    continue;
                }
                BratleyNode bestCandidate = list.get(0);
                int bestCandidateIndex = 0;
                for (int i = 1; i < list.size(); i++) {
                    BratleyNode nextCandidate = list.get(i);
                    if (bestCandidate.t > nextCandidate.t) {
                        bestCandidate = nextCandidate;
                        bestCandidateIndex = i;
                    }
                }
                ArrayList<BratleyNode> newParents = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    if (i == bestCandidateIndex) {
                        continue;
                    }
                    //This piece of code below is never called for the root node as it is always the best candidate
                    BratleyNode curNode = list.get(i);
                    BratleyNode curNodeParent = curNode.parents.get(0);
                    curNodeParent.children.remove(curNode);
                    curNode.parents.clear();
                    curNodeParent.children.add(bestCandidate);
                    newParents.add(curNodeParent);
                }
                if (bestCandidate.parents.isEmpty()) {
                    bestCandidate.bestCandidateParent = null;
                } else {
                    bestCandidate.bestCandidateParent = bestCandidate.parents.get(0);
                }
                bestCandidate.parents.addAll(newParents);
                nextLevelNodes.add(bestCandidate);
            }
            this.curLevelNodes = nextLevelNodes;
        }

        public EtNode getNextCombination() {
            if (noSolutions || curLevelNodes.size() == 0) {
                return null;
            }
            while (true) {
                if (curLevelNodesIndex == curLevelNodes.size()) {
                    generateNextLevel();
                    curLevelNodesIndex = 0;
                    if (curLevelNodes.size() == 0) {
                        return null;
                    }
                }
                BratleyNode curBratleyNode = curLevelNodes.get(curLevelNodesIndex);
                curLevelNodesIndex++;
                if (curBratleyNode.isViable()) {
                    lastReturnedNode = curBratleyNode;
                    return getEtNodeForBratleyNode(curBratleyNode);
                }
            }
        }

        public ArrayList<int[]> getCurStartTimes() {
            ArrayList<int[]> ret = new ArrayList<>();
            BratleyNode curBratley = this.lastReturnedNode;
            while (true) {
                BratleyNode parentBratley = curBratley.bestCandidateParent;
                if (parentBratley == null) {
                    break;
                }
                for (int i = 0; i < parentBratley.extraTaskPeriods.length; i++) {
                    if (parentBratley.extraTaskPeriods[i] != curBratley.extraTaskPeriods[i]) {
                        int[] startTime = new int[3];
                        startTime[0] = i;
                        startTime[1] = parentBratley.extraTaskPeriods[i] + parent.ttTaskPeriods[i];
                        TtJob curTtJob = ttJobs[startTime[0]].get(startTime[1]);
                        startTime[2] = Math.max(parentBratley.t, curTtJob.getReleaseTime());
                        ret.add(startTime);
                        break;
                    }
                }
                curBratley = parentBratley;
            }
            return ret;
        }

        public void saveGraphToFile(String filepath, boolean includeTaskPeriods) throws IOException {
            FileWriter fw = new FileWriter(filepath);
            fw.write("digraph {\n\n");
            Queue<BratleyNode> queue = new LinkedList<>();
            queue.add(rootBratley);
            while (!queue.isEmpty()) {
                //Write node information
                BratleyNode curNode = queue.poll();
                String label = "B" + curNode.id;
                String nodeRecord = label + "[label=\"" + label + ": [" + curNode.t + "]\\n";
                StringBuilder nodeTaskRecord = new StringBuilder();
                if (includeTaskPeriods) {
                    nodeTaskRecord = new StringBuilder("[");
                    for (int i = 0; i < curNode.extraTaskPeriods.length; i++) {
                        nodeTaskRecord.append(curNode.extraTaskPeriods[i]);
                        if (i != curNode.extraTaskPeriods.length-1) {
                            nodeTaskRecord.append(',');
                        }
                    }
                    nodeTaskRecord.append("]");
                }

                fw.write(nodeRecord + nodeTaskRecord.toString() + "\", shape=box" + (curNode.isViable() ? ", color=\"green\"" : "") + "];\n");

                //Write edges
                for (BratleyNode nn : curNode.children) {
                    fw.write(label + " -> B" + nn.id + ";\n");
                    if (queue.stream().noneMatch(node -> node.id == nn.id)) {
                        queue.add(nn);
                    }
                }
            }
            fw.write("\n}");
            fw.close();
        }

        public void writeCluster(FileWriter fw, String parentNodeLabel, boolean includeTaskPeriods) throws IOException {
            fw.write("\nsubgraph cluster_" + parentNodeLabel + " {\n");
            fw.write("\tcolor=black;\n\n");
            Queue<BratleyNode> queue = new LinkedList<>();
            queue.add(rootBratley);
            while (!queue.isEmpty()) {
                //Write node information
                BratleyNode curNode = queue.poll();
                String label = parentNodeLabel + "B" + curNode.id;
                String nodeRecord = label + "[label=\"" + label + ": [" + curNode.t + "]\\n";
                StringBuilder nodeTaskRecord = new StringBuilder();
                if (includeTaskPeriods) {
                    nodeTaskRecord = new StringBuilder("[");
                    for (int i = 0; i < curNode.extraTaskPeriods.length; i++) {
                        nodeTaskRecord.append(curNode.extraTaskPeriods[i]);
                        if (i != curNode.extraTaskPeriods.length-1) {
                            nodeTaskRecord.append(',');
                        }
                    }
                    nodeTaskRecord.append("]");
                }

                fw.write("\t" + nodeRecord + nodeTaskRecord.toString() + "\", shape=box" + (curNode.isViable() ? ", color=\"green\"" : "") + "];\n");

                //Write edges
                for (BratleyNode nn : curNode.children) {
                    fw.write("\t" + label + " -> " + parentNodeLabel + "B" + nn.id + ";\n");
                    if (queue.stream().noneMatch(node -> node.id == nn.id)) {
                        queue.add(nn);
                    }
                }
            }
            fw.write("}\n");
            fw.write(parentNodeLabel + " -> " + parentNodeLabel + "B0:n[arrowhead=\"crow\"];\n\n");
        }

    }

    ArrayList<TtJob>[] ttJobs;
    ArrayList<EtJob>[] etJobs;
    EtNode rootNode;
    Stack<TtNode> decisionNodes;

    public EtttFixationGraph(ArrayList<TtJob>[] ttJobs, ArrayList<EtJob>[] etJobs) {
        this.ttJobs = ttJobs;
        this.etJobs = etJobs;
        this.rootNode = new EtNode(new int[ttJobs.length], new int[etJobs.length], 0, 0);
    }

    private ArrayList<EtNode> expansionPhase(EtNode curNode) {
        //Initialize array with relevant job period for each task
        ArrayList<EtNode> ret = new ArrayList<>();
        int earliestCrTime = Integer.MAX_VALUE;
        EtJob[] curJobs = new EtJob[etJobs.length];
        for (int i = 0; i < etJobs.length; i++) {
            int curTaskPeriod = curNode.etTaskPeriods[i];
            if (etJobs[i].size() != curTaskPeriod) {
                curJobs[i] = etJobs[i].get(curTaskPeriod);
                earliestCrTime = Math.min(earliestCrTime, curJobs[i].getReleaseTimeMax());
            }
        }
        if (earliestCrTime == Integer.MAX_VALUE) { //No next jobs are available
            return ret;
        }
        int actualMax = Math.max(earliestCrTime, curNode.max);

        EtJob[] relevantJobs = new EtJob[etJobs.length];
        int relevantJobsTotal = 0;
        //ArrayList<EtJob> jobsSortedByCr = new ArrayList<>();
        for (int i = 0; i < curJobs.length; i++) {
            if (curJobs[i] != null && curJobs[i].getReleaseTimeMin() <= actualMax) {
                relevantJobs[relevantJobsTotal++] = curJobs[i];
            }
        }
        EtJob[] jobsSortedByCr = new EtJob[relevantJobsTotal];
        System.arraycopy(relevantJobs,0,jobsSortedByCr,0,relevantJobsTotal);
        Arrays.sort(jobsSortedByCr, Comparator.comparingInt(EtJob::getReleaseTimeMax));
        EtJob[] jobsSortedByUr = new EtJob[relevantJobsTotal];
        System.arraycopy(jobsSortedByCr, 0, jobsSortedByUr, 0, relevantJobsTotal);
        Arrays.sort(jobsSortedByUr, Comparator.comparingInt(EtJob::getReleaseTimeMin));

        //Main cycle, keep track of the best job candidates and create new nodes if needed
        EtJob bestCrJob = null;
        ArrayList<EtJob> bestUrJobs = new ArrayList<>();
        int t;
        int crIndex = 0, urIndex = 0;
        boolean lastCycle = false;
        boolean newCrs, newUrs;
        while (!lastCycle) {

            //Determine time of the next event and its properties
            newCrs = false;
            newUrs = false;
            int actualCrTime = crIndex >= relevantJobsTotal ? Integer.MAX_VALUE : Math.max(curNode.min, jobsSortedByCr[crIndex].getReleaseTimeMax());
            int actualUrTime = urIndex >= relevantJobsTotal ? Integer.MAX_VALUE : Math.max(curNode.min, jobsSortedByUr[urIndex].getReleaseTimeMin());
            int newT = Math.min(actualCrTime,actualUrTime);
            if (newT == Integer.MAX_VALUE) {
                t = actualMax+1;
            } else {
                t = newT;
                if (newT == actualCrTime) {
                    newCrs = true;
                }
                if (newT == actualUrTime) {
                    newUrs = true;
                }
            }
            t = Math.min(t,actualMax+1);

            EtJob newBestCrJob = null;
            if (t > actualMax) {
                newBestCrJob = new EtJob(-1,-1,-1,-1,-1,-1,-1,-1,-1);
                lastCycle = true;
            } else {
                if (newCrs) {
                    while (crIndex < relevantJobsTotal) {
                        EtJob newCandidate = jobsSortedByCr[crIndex];
                        if (newCandidate.getReleaseTimeMax() > t) {
                            break;
                        } else {
                            crIndex++;
                        }
                        if (newBestCrJob == null || newBestCrJob.compareTo(newCandidate) > 0) { //There is a better CR job candidate
                            newBestCrJob = newCandidate;
                        }
                    }
                    if (newBestCrJob != null && bestCrJob != null && newBestCrJob.compareTo(bestCrJob) > 0) {
                        newBestCrJob = null;
                    }
                }
            }

            //Updating stuff if a better CR job candidate has been found
            if (newBestCrJob != null) {
                //The new CR job was in UR job list and needs to be moved
                bestUrJobs.remove(newBestCrJob);
                if (bestCrJob != null) { //If this is not the first CR job to be assigned, the previous one needs to be made into a node
                    EtNode nextNode = curNode.getNewNodeForEtJob(bestCrJob, Math.max(curNode.min, bestCrJob.getReleaseTimeMin()), t-1);
                    ret.add(nextNode);
                }
                bestCrJob = newBestCrJob;
                for (int j = 0; j < bestUrJobs.size();) { //Take out UR jobs that are no longer a better candidate than the new CR job
                    EtJob curJob = bestUrJobs.get(j);
                    if (curJob.compareTo(bestCrJob) > 0) { //The job is worse than the new candidate, remove it and create new node
                        //Node nextNode = curNode.getNewNodeForJob(curJob, bestUrJobsStartTimes.get(j), t-1);
                        EtNode nextNode = curNode.getNewNodeForEtJob(curJob, Math.max(curNode.min, curJob.getReleaseTimeMin()), t-1);
                        ret.add(nextNode);
                        bestUrJobs.remove(j);
                    } else {
                        j++;
                    }
                }
            }

            if (newUrs && !lastCycle) {
                while (urIndex < relevantJobsTotal) {
                    EtJob newUrJob = jobsSortedByUr[urIndex];
                    if (newUrJob.getReleaseTimeMin() > t) {
                        break;
                    } else {
                        urIndex++;
                    }
                    if (bestCrJob == null || newUrJob.compareTo(bestCrJob) < 0) { //New UR job is available and better than the CR candidate (also there "has" to be < in case r_max = r_min for a job)
                        bestUrJobs.add(newUrJob);
                    }
                }
            }

        }

        return ret;
    }

    private EtNode fixationPhase(ArrayList<EtNode> startingNodes) {
        boolean deadlineMiss = false;
        for (EtNode curNode : startingNodes) {
            if (curNode.causedDeadlineMiss) {
                deadlineMiss = true;
                break;
            }
        }
        //If a deadline miss is found, get the next combination of the last decision node
        if (!deadlineMiss) {
            boolean canDoDecisions = true;
            int biggestMax = Integer.MIN_VALUE;
            int earliestEtRelease = Integer.MAX_VALUE;
            if (startingNodes.size() == 1 && startingNodes.get(0).min == startingNodes.get(0).max) {
                biggestMax = startingNodes.get(0).max;
                EtJob[] curEtJobs = startingNodes.get(0).getCurEtJobs();
                for (int j = 0; j < etJobs.length; j++) {
                    EtJob curJob = curEtJobs[j];
                    if (curJob == null) {
                        continue;
                    }
                    earliestEtRelease = Math.min(earliestEtRelease, curJob.getReleaseTimeMin());
                }
            } else { //Finding an idle interval
                for (int i = 0; i < startingNodes.size(); i++) {
                    EtNode curNode = startingNodes.get(i);
                    biggestMax = Math.max(biggestMax, curNode.max);
                    EtJob[] curEtJobs = curNode.getCurEtJobs();
                    for (int j = 0; j < etJobs.length; j++) {
                        EtJob curJob = curEtJobs[j];
                        if (curJob == null) {
                            continue;
                        }
                        earliestEtRelease = Math.min(earliestEtRelease, curJob.getReleaseTimeMin());
                        if (curJob.getReleaseTimeMin() < curNode.max) {
                            canDoDecisions = false;
                            break;
                        }
                    }
                    if (!canDoDecisions) {
                        break;
                    }
                }
            }
            //Making sure that a TT job releases before (or at the same time as) an ET job
            if (canDoDecisions) {
                TtJob[] curTtJobs = startingNodes.get(0).getCurTtJobs();
                boolean ttReleaseBeforeEt = false;
                for (TtJob tj : curTtJobs) {
                    if (tj == null) {
                        continue;
                    }
                    if (tj.getReleaseTime() <= earliestEtRelease) {
                        ttReleaseBeforeEt = true;
                        break;
                    }
                }
                if (!ttReleaseBeforeEt) {
                    canDoDecisions = false;
                }
            }

            if (canDoDecisions) {
                EtNode parentEtNode = startingNodes.get(0);
                TtNode decisionNode = new TtNode(parentEtNode.ttTaskPeriods, parentEtNode.etTaskPeriods, biggestMax);
                if (decisionNodes == null) {
                    decisionNodes = new Stack<>();
                }
                decisionNodes.push(decisionNode);
                decisionNode.parents.addAll(startingNodes);
                for (EtNode parent : startingNodes) {
                    parent.children.add(decisionNode);
                }
                EtNode nextNode = decisionNode.generateNextChild();
                if (nextNode != null) {
                    return nextNode;
                }
            } else {
                return null;
            }
        }

        if (decisionNodes == null) {
            decisionNodes = new Stack<>();
            return null;
        }
        while (true) {
            TtNode curDecNode;
            try {
                curDecNode = decisionNodes.peek();
            } catch (EmptyStackException e) {
                return null; //All combinations tried
            }
            curDecNode.children = new ArrayList<>(); //Remove dead vertices
            EtNode nextNode = curDecNode.generateNextChild();
            if (nextNode != null) {
                return nextNode;
            }
            decisionNodes.pop();
        }
    }

    private boolean detectDone(ArrayList<EtNode> startingNodes) {
        for (EtNode curNode : startingNodes) {
            if (curNode.causedDeadlineMiss) {
                return false;
            }
            for (int j = 0; j < ttJobs.length; j++) {
                if (curNode.ttTaskPeriods[j] < ttJobs[j].size()) {
                    return false;
                }
            }
            for (int j = 0; j < etJobs.length; j++) {
                if (curNode.etTaskPeriods[j] < etJobs[j].size()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean createStartTimeGraphNoIip() {
        ArrayList<EtNode> startingNodes = new ArrayList<>();
        HashMap<EtNode, ArrayList<EtNode>> nextLevelNodes = new HashMap<>();
        startingNodes.add(this.rootNode);

        while (true) {
            //Deadline miss detection, backtracking and decision phase

            //Detect if all jobs have been finished without deadline misses
            boolean done = detectDone(startingNodes);
            if (done) {
                return true;
            }

            EtNode fixationPhaseResult = fixationPhase(startingNodes);
            ArrayList<EtNode> nodesForExpansion;
            if (fixationPhaseResult == null) {
                if (this.decisionNodes == null || !this.decisionNodes.isEmpty()) {
                    nodesForExpansion = startingNodes;
                } else {
                    return false;
                }
            } else {
                nodesForExpansion = new ArrayList<>();
                nodesForExpansion.add(fixationPhaseResult);
            }

            //Expansion phase
            for (EtNode en : nodesForExpansion) {
                ArrayList<EtNode> curNodeChildren = expansionPhase(en);
                for (EtNode child : curNodeChildren) {
                    //Add the next nodes to the appropriate datastructures
                    ArrayList<EtNode> sameFinishedJobsArray = nextLevelNodes.get(child);
                    if (sameFinishedJobsArray == null) {
                        sameFinishedJobsArray = new ArrayList<>();
                        sameFinishedJobsArray.add(child);
                        nextLevelNodes.put(child,sameFinishedJobsArray);
                    } else {
                        sameFinishedJobsArray.add(child);
                    }
                }
                
            }

            startingNodes.clear();

            //Merge phase
            for (ArrayList<EtNode> sameFinishedJobsArray : nextLevelNodes.values()) {
                sameFinishedJobsArray.sort(Comparator.comparingInt(EtNode::getMin));
                for (int i = 1; i < sameFinishedJobsArray.size();) {
                    EtNode leftNode = sameFinishedJobsArray.get(i-1);
                    EtNode rightNode = sameFinishedJobsArray.get(i);
                    if (leftNode.max >= rightNode.min) {
                        leftNode.max = Math.max(leftNode.max,rightNode.max);
                        leftNode.mergeNodeWith(rightNode);
                        sameFinishedJobsArray.remove(i);
                    } else {
                        i++;
                    }
                }
                startingNodes.addAll(sameFinishedJobsArray);
            }

            //Gives the merged nodes IDs and redirects/removes some edges
            for (EtNode n : startingNodes) {
                //n.id = nextNodeId++;
                for (Node parent : n.parents) {
                    if (parent.children == null) {
                        parent.children = new ArrayList<>();
                    }
                    parent.children.add(n);
                }
            }

            nextLevelNodes.clear();
        }
    }

    public int[][] getStartTimesFromGraph() {
        int[][] ret = new int[ttJobs.length][];
        for (int i = 0; i < ttJobs.length; i++) {
            ret[i] = new int[ttJobs[i].size()];
        }
        int i = 0;
        while (!this.decisionNodes.isEmpty()) {
            TtNode curTtNode = decisionNodes.pop();
            ArrayList<int[]> curStartTimes = curTtNode.enumerator.getCurStartTimes();
            for (int[] startTime : curStartTimes) {
                ret[startTime[0]][startTime[1]] = startTime[2];
            }
        }
        return ret;
    }

    public void saveGraphToFile(String filepath, boolean includeClusters, boolean includeTaskPeriods) throws IOException {
        FileWriter fw = new FileWriter(filepath);
        fw.write("digraph {\n\n");
        Queue<Node> queue = new LinkedList<>();
        queue.add(rootNode);
        while (!queue.isEmpty()) {
            //Write node information
            Node curNode = queue.poll();
            String curNodeLabel = "V" + curNode.id;
            String nodeRecord;
            if (curNode instanceof EtNode) {
                EtNode curEtNode = (EtNode) curNode;
                nodeRecord = curNodeLabel + "[label=\"" + curNodeLabel + ": [" + curEtNode.min + ", " + curEtNode.max + "]\\n";
            } else {
                TtNode curTtNode = (TtNode) curNode;
                nodeRecord = curNodeLabel + "[shape=\"invhouse\", label=\"" + curNodeLabel + ": [" + curTtNode.time + "]\\n";
            }
            StringBuilder nodeTaskRecord = new StringBuilder();
            if (includeTaskPeriods) {
                nodeTaskRecord = new StringBuilder("[");
                for (int i = 0; i < curNode.ttTaskPeriods.length; i++) {
                    nodeTaskRecord.append(curNode.ttTaskPeriods[i]);
                    if (i != curNode.ttTaskPeriods.length-1) {
                        nodeTaskRecord.append(',');
                    }
                }
                nodeTaskRecord.append("]\\n[");
                for (int i = 0; i < curNode.etTaskPeriods.length; i++) {
                    nodeTaskRecord.append(curNode.etTaskPeriods[i]);
                    if (i != curNode.etTaskPeriods.length-1) {
                        nodeTaskRecord.append(',');
                    }
                }
                nodeTaskRecord.append("]");
            }
            fw.write(nodeRecord + nodeTaskRecord.toString() + "\"];\n");

            //Write in the cluster if TT node
            if (includeClusters && curNode instanceof TtNode) {
                ((TtNode) curNode).enumerator.writeCluster(fw, curNodeLabel, includeTaskPeriods);
            }

            //Write edges
            for (Node nn : curNode.children) {
                if (nn instanceof EtNode && curNode instanceof EtNode) {
                    EtJob actuallyPickedEtJob = null;
                    for (int i = 0; i < nn.etTaskPeriods.length; i++) {
                        if (curNode.etTaskPeriods[i] != nn.etTaskPeriods[i]) {
                            actuallyPickedEtJob = etJobs[i].get(curNode.etTaskPeriods[i]);
                        }
                    }
                    if (actuallyPickedEtJob == null) {
                        fw.write(curNodeLabel + " -> V" + nn.id + "[label=\"null\"];\n");
                    } else {
                        fw.write(curNodeLabel + " -> V" + nn.id +
                                "[label=\"T" + actuallyPickedEtJob.getTaskId() + " J" + actuallyPickedEtJob.getRepetition() + "\\n"
                                + "D=" + actuallyPickedEtJob.getDeadline() + "\\n"
                                + "R=" + actuallyPickedEtJob.getReleaseTimeMin() + "|" + actuallyPickedEtJob.getReleaseTimeMax() + "\\n"
                                + "C=" + actuallyPickedEtJob.getExecutionTimeMin() + "|" + actuallyPickedEtJob.getExecutionTimeMax() + "\\n"
                                + "P=" + actuallyPickedEtJob.getPriority() + "\\n"
                                + "\""
                                + ", fontsize=6"
                                + "];\n");
                    }
                } else {
                    fw.write(curNodeLabel + " -> V" + nn.id + "[arrowhead=\"vee\"];\n");
                }
                if (queue.stream().noneMatch(node -> node.id == nn.id)) {
                    queue.add(nn);
                }
            }
        }
        fw.write("\n}");
        fw.close();
    }

}