package alg_et;

import model.EtJob;
import model.EtTask;
import model.SchedulingPolicy;
import util.JobTaskUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class EtScheduleGraphTest {

    public int nextNodeId = 1;

    private class Node {
        int id;
        int min; //earliest finish time
        int max; //latest finish time
        int[] taskPeriods; //Indices of the the applicable jobs
        boolean causedDeadlineMiss;
        ArrayList<Node> parents;
        ArrayList<Node> children;

        public Node(int min, int max, int[] taskPeriods, Node parent, boolean causedDeadlineMiss) {
            this.min = min;
            this.max = max;
            this.taskPeriods = taskPeriods;
            this.parents = new ArrayList<>();
            if (parent != null) {
                parents.add(parent);
            }
            this.causedDeadlineMiss = causedDeadlineMiss;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public Node expandNodeWithJob(EtJob job, int earliestEligibleTime, int latestEligibleTime) {
            int[] newTaskPeriods = Arrays.copyOf(taskPeriods, taskPeriods.length);
            newTaskPeriods[job.getTaskId()]++;
            return new Node(earliestEligibleTime+job.getExecutionTimeMin(),
                    latestEligibleTime+job.getExecutionTimeMax(),
                    newTaskPeriods,
                    this,
                    latestEligibleTime+job.getExecutionTimeMax() > job.getDeadline());
        }

        public void mergeNodeWith(Node toMergeNode) {
            min = Math.min(min, toMergeNode.min);
            max = Math.max(max, toMergeNode.max);
            causedDeadlineMiss = causedDeadlineMiss || toMergeNode.causedDeadlineMiss;
            this.parents.addAll(toMergeNode.parents);
            toMergeNode.parents = null;
        }

        public EtJob[] getJobList() {
            EtJob[] ret = new EtJob[etJobs.length];
            for (int i = 0; i < etJobs.length; i++) {
                int curTaskPeriod = taskPeriods[i];
                if (etJobs[i].size() != curTaskPeriod) {
                    ret[i] = etJobs[i].get(curTaskPeriod);
                }
            }
            return ret;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Arrays.equals(taskPeriods, node.taskPeriods);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(taskPeriods);
        }

        @Override
        public String toString() {
            StringBuilder bobTheStringBuilder = new StringBuilder();
            bobTheStringBuilder.append("" + min + '|' + max + "-[");
            for (int i = 0; i < taskPeriods.length; i++) {
                bobTheStringBuilder.append(taskPeriods[i]);
                if (i != taskPeriods.length-1) {
                    bobTheStringBuilder.append(',');
                }
            }
            bobTheStringBuilder.append(']');
            return bobTheStringBuilder.toString();
        }
    }

    ArrayList<EtJob>[] etJobs;
    Node rootNode;

    public EtScheduleGraphTest(ArrayList<EtTask> etTasks) {
        int hyperperiod = JobTaskUtils.getHyperperiodForTasks(null, etTasks);
        etJobs = JobTaskUtils.getEtJobsFromEtTasksAs2dArray(etTasks, hyperperiod);
    }

    public EtScheduleGraphTest(ArrayList<EtJob>[] etJobs) {
        this.etJobs = etJobs;
    }

    /**
     * Expansion phase for the EDF-FP policy
     * @param curNode node which should be expanded
     * @return a list of expanded nodes
     */
    private ArrayList<Node> expansionPhaseEdffp(Node curNode) {
        //Initialize array with relevant job period for each task
        ArrayList<Node> ret = new ArrayList<>();
        int earliestCrTime = Integer.MAX_VALUE;
        EtJob[] curJobs = new EtJob[etJobs.length];
        for (int i = 0; i < etJobs.length; i++) {
            int curTaskPeriod = curNode.taskPeriods[i];
            if (etJobs[i].size() != curTaskPeriod) {
                curJobs[i] = etJobs[i].get(curTaskPeriod);
                earliestCrTime = Math.min(earliestCrTime, curJobs[i].getReleaseTimeMax());
            }
        }
        if (earliestCrTime == Integer.MAX_VALUE) {
            return ret; //All jobs are finished
        }
        int extendedMax = Math.max(earliestCrTime, curNode.max);

        // Create an array of applicable jobs and sort them by Certain and Possible Release (CR & PR)
        EtJob[] appJobs = new EtJob[etJobs.length];
        int appJobsTotal = 0;
        for (EtJob ej : curJobs) {
            if (ej != null && ej.getReleaseTimeMin() <= extendedMax) {
                appJobs[appJobsTotal++] = ej;
            }
        }
        EtJob[] jobsSortedByCr = new EtJob[appJobsTotal];
        System.arraycopy(appJobs,0,jobsSortedByCr,0,appJobsTotal);
        Arrays.sort(jobsSortedByCr, Comparator.comparingInt(EtJob::getReleaseTimeMax));
        EtJob[] jobsSortedByPr = new EtJob[appJobsTotal];
        System.arraycopy(jobsSortedByCr, 0, jobsSortedByPr, 0, appJobsTotal);
        Arrays.sort(jobsSortedByPr, Comparator.comparingInt(EtJob::getReleaseTimeMin));

        //Main cycle, keep track of the best job candidates and create new nodes
        EtJob ceJob = null; // Certainly-eligible job
        ArrayList<EtJob> peJobs = new ArrayList<>(); // Possibly-eligible job
        int t, crIndex = 0, prIndex = 0;
        boolean newCrs, newUrs, lastCycle = false;
        while (!lastCycle) {
            //Determine time of the next event (time at which any job becomes certainly or possibly released) and its properties
            newCrs = false;
            newUrs = false;
            int actualCrTime = crIndex >= appJobsTotal ? Integer.MAX_VALUE : Math.max(curNode.min, jobsSortedByCr[crIndex].getReleaseTimeMax());
            int actualUrTime = prIndex >= appJobsTotal ? Integer.MAX_VALUE : Math.max(curNode.min, jobsSortedByPr[prIndex].getReleaseTimeMin());
            int newT = Math.min(actualCrTime,actualUrTime);
            if (newT == Integer.MAX_VALUE) {
                t = extendedMax+1;
            } else {
                t = newT;
                if (newT == actualCrTime) {
                    newCrs = true;
                }
                if (newT == actualUrTime) {
                    newUrs = true;
                }
            }
            t = Math.min(t,extendedMax+1);

            //Find the certainly-eligible job at time t
            EtJob newCeJob = null;
            if (t > extendedMax) {
                newCeJob = new EtJob(-1,-1,-1,-1,-1,-1,-1,-1,-1);
                lastCycle = true;
            } else {
                if (newCrs) {
                    while (crIndex < appJobsTotal) {
                        EtJob newCandidate = jobsSortedByCr[crIndex];
                        if (newCandidate.getReleaseTimeMax() > t) {
                            break;
                        } else {
                            crIndex++;
                        }
                        if (newCeJob == null || newCeJob.compareTo(newCandidate) > 0) {
                            newCeJob = newCandidate;
                        }
                    }
                    if (newCeJob != null && ceJob != null && newCeJob.compareTo(ceJob) > 0) {
                        newCeJob = null;
                    }
                }
            }

            //If a new certainly released job is found, change the certainly-eligible job and the list of possibly-eligible jobs
            if (newCeJob != null) {
                peJobs.remove(newCeJob);
                if (ceJob != null) { //Create a new node from the previous certainly-eligible job
                    Node nextNode = curNode.expandNodeWithJob(ceJob, Math.max(curNode.min, ceJob.getReleaseTimeMin()), t-1);
                    ret.add(nextNode);
                }
                ceJob = newCeJob;
                //Take out possibly-eligible jobs that are no longer eligible
                for (int j = 0; j < peJobs.size();) {
                    EtJob curJob = peJobs.get(j);
                    if (curJob.compareTo(ceJob) > 0) {
                        Node nextNode = curNode.expandNodeWithJob(curJob, Math.max(curNode.min, curJob.getReleaseTimeMin()), t-1);
                        ret.add(nextNode);
                        peJobs.remove(j);
                    } else {
                        j++;
                    }
                }
            }

            //Go through newly released possibly released jobs and see if they are also eligible
            if (newUrs && !lastCycle) {
                while (prIndex < appJobsTotal) {
                    EtJob newUrJob = jobsSortedByPr[prIndex];
                    if (newUrJob.getReleaseTimeMin() > t) {
                        break;
                    } else {
                        prIndex++;
                    }
                    if (ceJob == null || newUrJob.compareTo(ceJob) < 0) {
                        peJobs.add(newUrJob);
                    }
                }
            }

        }

        return ret;
    }

    /**
     * Expansion phase for the EDF-FP policy
     * @param curNode node which should be expanded
     * @param policy policy which should be used during the expansion
     * @return a list of expanded nodes
     */
    private ArrayList<Node> expansionPhaseAnyPolicy(Node curNode, SchedulingPolicy policy) {
        //Initialize array with applicable jobs and find the CW
        ArrayList<Node> ret = new ArrayList<>();
        int cTime = Integer.MAX_VALUE; //Critical time
        EtJob cJob = null; //Critical job
        EtJob[] appJobs = new EtJob[etJobs.length];

        switch(policy) {
            case EDFFP:
                for (int i = 0; i < etJobs.length; i++) {
                    int curTaskPeriod = curNode.taskPeriods[i];
                    if (etJobs[i].size() != curTaskPeriod) {
                        EtJob curJob = etJobs[i].get(curTaskPeriod);
                        appJobs[i] = curJob;
                        //Select any job as critical job
                        if (cJob == null) {
                            cJob = curJob;
                        }
                    }
                }
                break;
            case PRM:
                EtJob anyJob = null;
                for (int i = 0; i < etJobs.length; i++) {
                    int curTaskPeriod = curNode.taskPeriods[i];
                    if (etJobs[i].size() != curTaskPeriod) {
                        EtJob curJob = etJobs[i].get(curTaskPeriod);
                        appJobs[i] = curJob;
                        //Define the critical job
                        if (anyJob == null) {
                            anyJob = curJob;
                        }
                        if (curJob.getPriority() == 0 && (cJob == null || cJob.getExecutionTimeMax() > curJob.getExecutionTimeMax()
                                || (cJob.getExecutionTimeMax() == curJob.getExecutionTimeMax() && cJob.getTaskId() > curJob.getTaskId()))) {
                            cTime = curJob.getDeadline() - curJob.getExecutionTimeMax();
                            cJob = curJob;
                        }
                    }
                }
                if (cJob == null) {
                    cJob = anyJob;
                }
                break;
            case CP:
                for (int i = 0; i < etJobs.length; i++) {
                    int curTaskPeriod = curNode.taskPeriods[i];
                    if (etJobs[i].size() != curTaskPeriod) { //There are still jobs left for task i
                        //Assign applicable job for task i to the array
                        EtJob curJob = etJobs[i].get(curTaskPeriod);
                        appJobs[i] = curJob;
                        //Define the critical window
                        if (cJob == null || cJob.getDeadline() > curJob.getDeadline()
                                || (cJob.getDeadline() == curJob.getDeadline() && cJob.getTaskId() > curJob.getTaskId())) {
                            cTime = curJob.getDeadline() - curJob.getExecutionTimeMax();
                            cJob = curJob;
                        }
                    }
                }
                break;
            case CW:
                //Fill the appJobs array according to the curNode first
                for (int i = 0; i < etJobs.length; i++) {
                    int curTaskPeriod = curNode.taskPeriods[i];
                    if (etJobs[i].size() != curTaskPeriod) {
                        appJobs[i] = etJobs[i].get(curTaskPeriod);
                    }
                }
                //Sort by jobs deadline, then by id, leave null elements at the end of the array
                EtJob[] sortedAppJobs = new EtJob[appJobs.length];
                System.arraycopy(appJobs,0,sortedAppJobs,0,appJobs.length);
                Arrays.sort(sortedAppJobs, (et1, et2) -> {
                    if (et1 == null) {
                        if (et2 == null) {
                            return 0;
                        }
                        return 1;
                    } else if (et2 == null) {
                        return -1;
                    }
                    if (et1.getDeadline() < et2.getDeadline()) {
                        return 1;
                    } else if (et1.getDeadline() > et2.getDeadline()) {
                        return -1;
                    } else {
                        if (et1.getTaskId() < et2.getTaskId()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                });
                //Get the critical job and critical time
                cJob = null;
                int cwTime = Integer.MAX_VALUE;
                for (EtJob sortedAppJob : sortedAppJobs) {
                    if (sortedAppJob == null) {
                        break;
                    }
                    cJob = sortedAppJob;
                    if (cJob.getDeadline() < cwTime) {
                        cwTime = cJob.getDeadline() - cJob.getExecutionTimeMax();
                    } else {
                        cwTime -= cJob.getExecutionTimeMax();
                    }
                }
                cTime = cwTime;
                break;
        }

        if (cJob == null) {
            return ret; //All jobs are finished
        }

        //Find the first time a certainly-eligible job exists
        int earliestCr = Integer.MAX_VALUE;
        for (EtJob ej : appJobs) {
            if (ej == null) {
                continue;
            }
            if (ej.equals(cJob) || Math.max(ej.getReleaseTimeMax(), curNode.min) + ej.getExecutionTimeMax() <= cTime) {
                earliestCr = Math.min(earliestCr, ej.getReleaseTimeMax());
            }
        }

        int actualMax;
        if (earliestCr > curNode.max) { //If you would put "if (false)" as the condition, the algorithm would still work, this condition is here just to make the algorithm faster
            actualMax = earliestCr;
        } else {
            //Find the earliest CR job which does not violate critical time (even outside the curNode.max boundary)
            EtJob earliestCrNonViolatingJob = cJob;
            for (EtJob ej : appJobs) {
                if (ej == null || ej.equals(cJob)) {
                    continue;
                }
                if (Math.max(ej.getReleaseTimeMax(), curNode.max) + ej.getExecutionTimeMax() <= cTime
                        && ej.getReleaseTimeMax() < earliestCrNonViolatingJob.getReleaseTimeMax()) {
                    earliestCrNonViolatingJob = ej;
                }
            }
            actualMax = Math.max(curNode.max, earliestCrNonViolatingJob.getReleaseTimeMax());
        }

        //Event class definition
        class Event {
            final int t;
            ArrayList<EtJob> newCr = null;
            ArrayList<EtJob> newPr = null;
            boolean newCm = false;

            public Event(int t) {
                this.t = t;
            }

            void addNewCr(EtJob ej) {
                if (newCr == null) {
                    newCr = new ArrayList<>();
                }
                newCr.add(ej);
            }

            void addNewUr(EtJob ej) {
                if (newPr == null) {
                    newPr = new ArrayList<>();
                }
                newPr.add(ej);
            }

            @Override
            public String toString() {
                return "CwEvent, " + "T=" + t + ", newCm?: " + newCm
                        + ", newCR: " + (newCr == null ? "[]" : newCr.toString())
                        + ", newPR: " + (newPr == null ? "[]" : newPr.toString());
            }
        }

        //Create a list of events, where an event is any time where a job:
        // - becomes certainly released
        // - becomes possibly released
        // - may no longer be eligible due to critical time
        HashMap<Integer, Event> hashedEvents = new HashMap<>();
        for (int i = 0; i < etJobs.length; i++) {
            EtJob curJob = appJobs[i];
            if (curJob == null) {
                continue;
            }

            int actualPr = Math.max(curNode.min, curJob.getReleaseTimeMin());
            int actualCr = Math.max(curNode.min, curJob.getReleaseTimeMax());
            if (actualPr + curJob.getExecutionTimeMax() > cTime && !curJob.equals(cJob)) {
                continue;
            }

            //Adding PR event (PR event will not be added if CR is at the same time)
            if (actualPr != actualCr && actualPr <= actualMax) {
                if (hashedEvents.containsKey(actualPr)) {
                    Event curEvent = hashedEvents.get(actualPr);
                    curEvent.addNewUr(curJob);
                } else {
                    Event newEvent = new Event(actualPr);
                    newEvent.addNewUr(curJob);
                    hashedEvents.put(actualPr, newEvent);
                }
            }

            //Adding CR event (cannot be added if it violates critical time at its max release)
            if (actualCr <= actualMax && (curJob.getReleaseTimeMax() + curJob.getExecutionTimeMax() <= cTime || curJob.equals(cJob))) {
                if (hashedEvents.containsKey(actualCr)) {
                    Event curEvent = hashedEvents.get(actualCr);
                    curEvent.addNewCr(curJob);
                } else {
                    Event newEvent = new Event(actualCr);
                    newEvent.addNewCr(curJob);
                    hashedEvents.put(actualCr, newEvent);
                }
            }

            //Adding CM (can be added only if it is within node range)
            int actualCm = Math.max(curNode.min, cTime - curJob.getExecutionTimeMax() + 1);
            if (actualCm <= actualMax && !curJob.equals(cJob)) {
                if (hashedEvents.containsKey(actualCm)) {
                    Event curEvent = hashedEvents.get(actualCm);
                    curEvent.newCm = true;
                } else {
                    Event newEvent = new Event(actualCm);
                    newEvent.newCm = true;
                    hashedEvents.put(actualCm, newEvent);
                }
            }
        }

        Event finishingEvent = new Event(actualMax+1);
        finishingEvent.addNewCr(new EtJob(-1,-1,-1,-1,-1,-1,-1,-1,-1));
        hashedEvents.put(actualMax+1, finishingEvent);

        ArrayList<Event> events = new ArrayList<>(hashedEvents.values());
        events.sort(Comparator.comparingInt(ce -> ce.t));

        //Main cycle, keep track of the eligible jobs and create new nodes if needed
        PriorityQueue<EtJob> crJobs = new PriorityQueue<>(); //Certainly released jobs which do not violate critical time sorted by priority
        int ceJobStartTime = -1;
        ArrayList<EtJob> prJobs = new ArrayList<>(); //List of possibly released jobs
        ArrayList<Integer> prJobsStartTimes = new ArrayList<>(); //Times, when a possibly released job became possibly eligible -1 if the job is not possibly-eligible at the moment
        int eventsIndex = 0;
        //Go through all events
        while (events.size() > eventsIndex) {
            Event curEvent = events.get(eventsIndex++);
            EtJob previousBestCr = crJobs.peek();

            //Remove CR jobs that are no longer viable due to CW
            if (curEvent.newCm) {
                while (!crJobs.isEmpty()) { //Keep removing jobs from the CR heap until a viable one is found
                    EtJob curCrJob = crJobs.peek();
                    if (curEvent.t + curCrJob.getExecutionTimeMax() > cTime && !curCrJob.equals(cJob)) {
                        crJobs.poll();
                    } else {
                        break;
                    }
                }
                //Disable PR jobs due to critical time
                for (int j = 0; j < prJobs.size(); j++) {
                    EtJob curJob = prJobs.get(j);
                    if (curJob.getExecutionTimeMax() + curEvent.t > cTime
                            && prJobsStartTimes.get(j) != -1 && !curJob.equals(cJob)) {
                        //curJob was active and now it needs to be deactivated because it violates critical time
                        Node nextNode = curNode.expandNodeWithJob(curJob, prJobsStartTimes.get(j), curEvent.t-1);
                        ret.add(nextNode);
                        prJobsStartTimes.set(j,-1);
                    }
                }
            }

            //Add a new certainly released job if there is one
            int newBestCrJobStartTime = -1;
            if (curEvent.newCr != null) {
                crJobs.addAll(curEvent.newCr); //Add new CR jobs
                for (EtJob ej : curEvent.newCr) {
                    if (prJobs.contains(ej)) {
                        //Job ej was in prJobs and needs to be removed from it
                        int jobIndex = prJobs.indexOf(ej);
                        if (ej.equals(crJobs.peek())) {
                            newBestCrJobStartTime = prJobsStartTimes.get(jobIndex);
                        } else if (prJobsStartTimes.get(jobIndex) != -1) { //Job ej has been possibly-eligible and no longer is
                            Node nextNode = curNode.expandNodeWithJob(ej, prJobsStartTimes.get(jobIndex), curEvent.t-1);
                            ret.add(nextNode);
                        }
                        prJobs.remove(jobIndex);
                        prJobsStartTimes.remove(jobIndex);
                    }
                }
            }
            if (newBestCrJobStartTime == -1) {
                newBestCrJobStartTime = curEvent.t;
            }
            EtJob newBestCrJob = crJobs.peek();

            //If the certainly released job changed, add a new node and activate/deactivate possibly released jobs
            if (previousBestCr != newBestCrJob) {
                //If this is not the first certainly-eligible job, create a new node (using the previous CE job)
                if (previousBestCr != null) {
                    Node nextNode = curNode.expandNodeWithJob(previousBestCr, ceJobStartTime, curEvent.t-1);
                    ret.add(nextNode);
                }
                ceJobStartTime = newBestCrJobStartTime;

                //Activate/deactivate PR jobs based on the new certainly-eligible job
                for (int j = 0; j < prJobs.size(); j++) {
                    EtJob curJob = prJobs.get(j);
                    if ((newBestCrJob == null || curJob.compareTo(newBestCrJob) < 0) && (curEvent.t + curJob.getExecutionTimeMax() <= cTime || curJob.equals(cJob))) { //The PR job is better than the new CR job
                        if (prJobsStartTimes.get(j) == -1) { //The job was deactivated and needs to be activated
                            prJobsStartTimes.set(j,curEvent.t);
                        }
                    } else {
                        if (prJobsStartTimes.get(j) != -1) { //The job was active and now it needs to be deactivated
                            Node nextNode = curNode.expandNodeWithJob(curJob, prJobsStartTimes.get(j), curEvent.t-1);
                            ret.add(nextNode);
                            prJobsStartTimes.set(j,-1);
                        }
                    }
                }
            }

            //Add new PR jobs
            if (curEvent.newPr != null) {
                for (EtJob ej : curEvent.newPr) {
                    prJobs.add(ej);
                    if (newBestCrJob == null || ej.compareTo(newBestCrJob) < 0) {
                        prJobsStartTimes.add(curEvent.t);
                    } else {
                        prJobsStartTimes.add(-1);
                    }
                }
            }

        }
        return ret;
    }

    /**
     * Generate a schedule graph for a given policy
     * @param terminateAfterDeadlineMiss do not keep building the schedule graph if a deadline miss is found
     * @param policy policy under which the schedule graph should be generated
     * @param saveGraph should the graph be saved into memory (so that it can be saved to file later)
     * @return if the Et tasks are schedulable under the given policy
     */
    public boolean generateGraphTest(boolean terminateAfterDeadlineMiss, SchedulingPolicy policy, boolean saveGraph) {
        ArrayList<Node> curLevelNodes = new ArrayList<>();
        HashMap<Node, ArrayList<Node>> nextLevelNodes = new HashMap<>();

        Node root = new Node(0,0, new int[etJobs.length],null, false);
        root.id = 0;
        if (saveGraph) {
            this.rootNode = root;
        }
        curLevelNodes.add(root);

        boolean deadlineMissFound = false;
        while (!curLevelNodes.isEmpty()) {
            //Expansion phase
            for (Node curNode : curLevelNodes) {
                ArrayList<Node> curNodeChildren;
                if (policy == SchedulingPolicy.EDFFP) {
                    curNodeChildren = expansionPhaseEdffp(curNode);
                } else {
                    curNodeChildren = expansionPhaseAnyPolicy(curNode,policy);
                }
                if (curNodeChildren.isEmpty()) {
                    continue;
                }
                for (Node child : curNodeChildren) {
                    //Try to detect deadline miss
                    if (child.causedDeadlineMiss) {
                        deadlineMissFound = true;
                    }
                    //Add the next nodes to the appropriate datastructures
                    ArrayList<Node> sameFinishedJobsArray = nextLevelNodes.get(child);
                    if (sameFinishedJobsArray == null) {
                        sameFinishedJobsArray = new ArrayList<>();
                        sameFinishedJobsArray.add(child);
                        nextLevelNodes.put(child,sameFinishedJobsArray);
                    } else {
                        sameFinishedJobsArray.add(child);
                    }
                }
            }

            if (!saveGraph) {
                for (Node n : curLevelNodes) {
                    n.parents = null;
                }
            }
            curLevelNodes.clear();

            //Merging phase
            for (ArrayList<Node> sameFinishedJobsArray : nextLevelNodes.values()) {
                sameFinishedJobsArray.sort(Comparator.comparingInt(Node::getMin));
                for (int i = 1; i < sameFinishedJobsArray.size();) {
                    Node leftNode = sameFinishedJobsArray.get(i-1);
                    Node rightNode = sameFinishedJobsArray.get(i);
                    if (leftNode.max >= rightNode.min) {
                        leftNode.max = Math.max(leftNode.max,rightNode.max);
                        leftNode.mergeNodeWith(rightNode);
                        sameFinishedJobsArray.remove(i);
                    } else {
                        i++;
                    }
                }
                curLevelNodes.addAll(sameFinishedJobsArray);
            }

            //Gives the merged nodes IDs and redirects/removes some edges
            for (Node n : curLevelNodes) {
                n.id = nextNodeId++;
                for (Node parent : n.parents) {
                    if (parent.children == null) {
                        parent.children = new ArrayList<>();
                    }
                    parent.children.add(n);
                }
            }

            nextLevelNodes.clear();
            if (terminateAfterDeadlineMiss && deadlineMissFound) {
                return false;
            }
        }
        return !deadlineMissFound;
    }

    public void saveGraphToFile(String filepath, boolean includeTaskPeriods) throws IOException {
        FileWriter fw = new FileWriter(filepath);
        fw.write("digraph {\n\n");

        Queue<Node> queue = new LinkedList<>();
        queue.add(rootNode);
        while (!queue.isEmpty()) {
            //Write node information
            Node curNode = queue.poll();
            if (curNode == null) {
                continue;
            }
            String label = "S" + curNode.id;
            String nodeRecord = label + "[label=\"" + label + ": [" + curNode.min + ", " + curNode.max + "]\\n";
            StringBuilder nodeTaskRecord = new StringBuilder();
            if (includeTaskPeriods) {
                nodeTaskRecord = new StringBuilder("[");
                for (int i = 0; i < curNode.taskPeriods.length; i++) {
                    nodeTaskRecord.append(curNode.taskPeriods[i]);
                    if (i != curNode.taskPeriods.length-1) {
                        nodeTaskRecord.append(',');
                    }
                }
                nodeTaskRecord.append("]");
            }
            fw.write(nodeRecord + nodeTaskRecord.toString() + "\"" + (curNode.causedDeadlineMiss ? ",color=Red" : "") + "];\n");

            //Write all next edges
            if (curNode.children == null) {
                continue;
            }
            for (Node nn : curNode.children) {
                EtJob actuallyPickedJob = null;
                for (int i = 0; i < nn.taskPeriods.length; i++) {
                    if (curNode.taskPeriods[i] != nn.taskPeriods[i]) {
                        actuallyPickedJob = etJobs[i].get(curNode.taskPeriods[i]);
                    }
                }
                fw.write(label + " -> S" + nn.id +
                        "[label=\"T" + actuallyPickedJob.getTaskId() + " J" + actuallyPickedJob.getRepetition() + "\\n"
                        + "D=" + actuallyPickedJob.getDeadline() + "\\n"
                        + "R=" + actuallyPickedJob.getReleaseTimeMin() + "|" + actuallyPickedJob.getReleaseTimeMax() + "\\n"
                        + "C=" + actuallyPickedJob.getExecutionTimeMin() + "|" + actuallyPickedJob.getExecutionTimeMax() + "\\n"
                        + "P=" + actuallyPickedJob.getPriority() + "\\n"
                        + "\""
                        + ", fontsize=6"
                        + "];\n");
                if (queue.stream().noneMatch(node -> node.id == nn.id)) {
                    queue.add(nn);
                }
            }
        }
        fw.write("\n}");
        fw.close();
    }

}