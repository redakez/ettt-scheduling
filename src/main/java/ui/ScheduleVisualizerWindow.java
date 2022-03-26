package ui;

import model.EtTask;
import model.Task;
import model.TtTask;
import util.JobTaskUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Class visualizing a loose gantt chart for a given collection of tasks
 * with the ability to interactively move each job.
 */
public class ScheduleVisualizerWindow extends JFrame {

    //How much should the chart be stretched vertically, it is always a power of 2
    private int verticalScale;

    //How much should the chart be stretched horizontally, it is always a power of 2
    private int horizontalScale;

    //Hyperperiod of given tasks
    private final int hyperperiod;

    //Time triggered tasks to be visualized
    private final Collection<TtTask> ttTasks;

    //Event triggered tasks to be visualized
    private final Collection<EtTask> etTasks;

    //Shift each job relative to its period start, changed during interaction
    private final HashMap<Integer, ArrayList<Integer>> releaseShifts;

    //Task (line) which is currently selected and can be moved
    private int selectedTask = 0;

    //Period of a job (index of a job in a line/task) which is currently selected and can be moved
    private int selectedJob = 0;

    //Order of task IDs in a given collection of time and event triggered tasks (time triggered tasks start first)
    private final ArrayList<Integer> taskIdOrder;

    //Map of all tasks for easier access
    private final HashMap<Integer, Task> allTasks;

    //Colors

    private static final Color BACKGROUND_COLOR = new Color(255,255,255);
    private static final Color OUTLINE_COLOR = new Color(0,0,0);
    private static final Color TEXT_COLOR = new Color(0,0,0);
    private static final Color TT_EXECUTION_COLOR = new Color(0,255,255);
    private static final Color EXECUTION_COLOR = new Color(255,0,0);
    private static final Color MAX_EXECUTION_COLOR = new Color(150,0,0);
    private static final Color EARLY_TIME_WINDOW_COLOR = new Color(128,128,128);
    private static final Color TIME_WINDOW_COLOR = new Color(96,96,96);
    private static final Color TT_TIME_WINDOW_COLOR = new Color(64,64,64);
    private static final Color PERIOD_COLOR = new Color(0,255,0);
    private static final Color SELECTED_JOB_COLOR = new Color(255,255,0);
    private static final int LEFT_SHIFT = 90;

    //Constructors

    public ScheduleVisualizerWindow(Collection<TtTask> ttTasks, Collection<EtTask> etTasks) {
        this(ttTasks, etTasks, JobTaskUtils.getHyperperiodForTasks(ttTasks, etTasks));
    }

    public ScheduleVisualizerWindow(Collection<TtTask> ttTasks, Collection<EtTask> etTasks, int hyperperiod) {
        this(4, ttTasks, etTasks, hyperperiod);
    }

    public ScheduleVisualizerWindow(int scale, Collection<TtTask> ttTasks, Collection<EtTask> etTasks) {
        this(scale, ttTasks, etTasks, JobTaskUtils.getHyperperiodForTasks(ttTasks, etTasks));
    }

    /**
     * Creates window which runs independently of the rest of the program until the program ends.
     * @param scale how zoomed in the visualization should be on launch (recommended values: 3-8)
     * @param ttTasks collection of time triggered tasks
     * @param etTasks collection of event triggered tasks
     * @param hyperperiod hyperperiod of the tasks
     */
    public ScheduleVisualizerWindow(int scale, Collection<TtTask> ttTasks, Collection<EtTask> etTasks, int hyperperiod) {
        this.verticalScale = 2 << scale;
        this.horizontalScale = this.verticalScale;
        this.ttTasks = ttTasks;
        this.etTasks = etTasks;
        this.hyperperiod = hyperperiod;
        this.releaseShifts = new HashMap<>();
        this.taskIdOrder = new ArrayList<>();
        this.allTasks = new HashMap<>();

        //Initializing allTasks, releaseShifts and taskIdOrder
        for (TtTask tt : ttTasks) {
            allTasks.put(tt.getId(), tt);
            ArrayList<Integer> releases = new ArrayList<>();
            for (int i = 0; i < hyperperiod; i += tt.getPeriod()) {
                releases.add(tt.getReleaseTime());
            }
            releaseShifts.put(tt.getId(), releases);
            taskIdOrder.add(tt.getId());
        }
        for (EtTask et : etTasks) {
            allTasks.put(et.getId(), et);
            ArrayList<Integer> releases = new ArrayList<>();
            for (int i = 0; i < hyperperiod; i += et.getPeriod()) {
                releases.add(et.getReleaseTimeMax());
            }
            releaseShifts.put(et.getId(), releases);
            taskIdOrder.add(et.getId());
        }

        //Window structure and content
        this.setTitle("ET and TT jobs visualized");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        int canvasYSize = this.verticalScale * (ttTasks.size() + etTasks.size() + 2);
        this.setPreferredSize(new Dimension((Math.min(hyperperiod * this.horizontalScale, 1600)+80+LEFT_SHIFT), canvasYSize+40));

        DrawCanvas canvas = new DrawCanvas();
        canvas.setPreferredSize(new Dimension(hyperperiod*this.horizontalScale+40+LEFT_SHIFT, canvasYSize));
        JScrollPane scrPane = new JScrollPane(canvas);
        this.add(scrPane);
        this.setVisible(true);
        this.pack();
        this.setLocationRelativeTo(null);
        canvas.repaint();

        //Interactive actions
        Action scaleUpAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scaleUp();
                canvas.repaint();
            }
        };

        Action scaleDownAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scaleDown();
                canvas.repaint();
            }
        };

        Action scaleLeftAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scaleLeft();
                canvas.repaint();
            }
        };

        Action scaleRightAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scaleRight();
                canvas.repaint();
            }
        };

        Action selectLeftAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectLeft();
                canvas.repaint();
            }
        };

        Action selectRightAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectRight();
                canvas.repaint();
            }
        };

        Action selectDownAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectDown();
                canvas.repaint();
            }
        };

        Action selectUpAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectUp();
                canvas.repaint();
            }
        };

        Action moveLeftAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelectedTaskLeft();
                canvas.repaint();
            }
        };

        Action moveRightAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelectedTaskRight();
                canvas.repaint();
            }
        };

        InputMap inputMap = canvas.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = canvas.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "scaleUp");
        actionMap.put("scaleUp", scaleUpAction);
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "scaleDown");
        actionMap.put("scaleDown", scaleDownAction);
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "scaleLeft");
        actionMap.put("scaleLeft", scaleLeftAction);
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "scaleRight");
        actionMap.put("scaleRight", scaleRightAction);

        inputMap.put(KeyStroke.getKeyStroke("A"), "selectLeft");
        actionMap.put("selectLeft", selectLeftAction);
        inputMap.put(KeyStroke.getKeyStroke("D"), "selectRight");
        actionMap.put("selectRight", selectRightAction);
        inputMap.put(KeyStroke.getKeyStroke("W"), "selectUp");
        actionMap.put("selectUp", selectUpAction);
        inputMap.put(KeyStroke.getKeyStroke("S"), "selectDown");
        actionMap.put("selectDown", selectDownAction);
        inputMap.put(KeyStroke.getKeyStroke("Q"), "moveLeft");
        actionMap.put("moveLeft", moveLeftAction);
        inputMap.put(KeyStroke.getKeyStroke("E"), "moveRight");
        actionMap.put("moveRight", moveRightAction);
    }

    private void scaleUp() {
        if (verticalScale >= 8) {
            verticalScale = verticalScale >> 1;
        }
    }

    private void scaleDown() {
        verticalScale = verticalScale << 1;
    }

    private void scaleLeft() {
        if (horizontalScale >= 2) {
            horizontalScale = horizontalScale >> 1;
        }
    }

    private void scaleRight() {
        horizontalScale = horizontalScale << 1;
    }

    private void selectLeft() {
        if (selectedJob > 0) {
            selectedJob--;
        }
    }

    private void selectRight() {
        Task curTask = allTasks.get(taskIdOrder.get(selectedTask));
        if (hyperperiod/curTask.getPeriod() > selectedJob + 1) {
            selectedJob++;
        }
    }

    private void selectUp() {
        if (selectedTask > 0) {
            selectedTask--;
        }
        selectedJob = 0;
    }

    private void selectDown() {
        if (selectedTask + 1 < allTasks.size()) {
            selectedTask++;
        }
        selectedJob = 0;
    }

    private void moveSelectedTaskLeft() {
        ArrayList<Integer> releases = releaseShifts.get(taskIdOrder.get(selectedTask));
        releases.set(selectedJob,releases.get(selectedJob)-1);
    }

    private void moveSelectedTaskRight() {
        ArrayList<Integer> releases = releaseShifts.get(taskIdOrder.get(selectedTask));
        releases.set(selectedJob,releases.get(selectedJob)+1);
    }

    /**
     * Canvas on which the loose gantt chart is drawn
     */
    private class DrawCanvas extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            //White background
            g.setColor(BACKGROUND_COLOR);
            g.fillRect(0,0,
                    (hyperperiod + LEFT_SHIFT) * horizontalScale * 3,
                    (ttTasks.size() + etTasks.size() + 2) * verticalScale * 3);
            //TT tasks
            int level = 0;
            for (TtTask tt : ttTasks) {
                //Left label
                g.setColor(TEXT_COLOR);
                g.drawString("ID:" + String.format("%4d",tt.getId()) + ", P: " + 0,
                        4,
                        level * verticalScale + verticalScale/2 + 2);
                //Jobs
                for (int i = 0; i < hyperperiod; i+=tt.getPeriod()) {
                    //Time window
                    g.setColor(TT_TIME_WINDOW_COLOR);
                    g.fillRect((i+tt.getReleaseTime()) * horizontalScale + LEFT_SHIFT,
                            level* verticalScale,
                            (tt.getDeadline()-tt.getReleaseTime())* horizontalScale,
                            verticalScale);
                    //Execution time
                    g.setColor(TT_EXECUTION_COLOR);
                    g.fillRect((i+ releaseShifts.get(tt.getId()).get(i/tt.getPeriod())) * horizontalScale  + LEFT_SHIFT,
                            level* verticalScale + verticalScale /4,
                            tt.getExecutionTime()* horizontalScale,
                            verticalScale /2);
                    //Period line
                    g.setColor(PERIOD_COLOR);
                    g.fillRect((i+tt.getPeriod())* horizontalScale -1  + LEFT_SHIFT,
                            level* verticalScale,
                            2,
                            verticalScale);
                }
                level++;
            }
            //ET tasks
            for (EtTask et : etTasks) {
                //Left label
                g.setColor(TEXT_COLOR);
                g.drawString("ID:" + String.format("%4d",et.getId()) + ", P: " + et.getPriority(),
                        4,
                        level * verticalScale + verticalScale/2 + 2);
                //Jobs
                for (int i = 0; i < hyperperiod; i+=et.getPeriod()) {
                    //Time window min release
                    g.setColor(EARLY_TIME_WINDOW_COLOR);
                    g.fillRect((i+et.getReleaseTimeMin()) * horizontalScale + LEFT_SHIFT,
                            level * verticalScale,
                            (et.getReleaseTimeMax() - et.getReleaseTimeMin()) * horizontalScale,
                            verticalScale);
                    //Time window max release
                    g.setColor(TIME_WINDOW_COLOR);
                    g.fillRect((i+et.getReleaseTimeMax()) * horizontalScale + LEFT_SHIFT,
                            level * verticalScale,
                            (et.getDeadline()-et.getReleaseTimeMax())* horizontalScale,
                            verticalScale);
                    //Execution time Min
                    g.setColor(EXECUTION_COLOR);
                    int releaseShift = releaseShifts.get(et.getId()).get(i/et.getPeriod());
                    g.fillRect((i+releaseShift) * horizontalScale + LEFT_SHIFT,
                            level * verticalScale + verticalScale /4,
                            (et.getExecutionTimeMin())* horizontalScale,
                            verticalScale/2);
                    //Execution time max
                    g.setColor(MAX_EXECUTION_COLOR);
                    g.fillRect((i + releaseShift + et.getExecutionTimeMin()) * horizontalScale + LEFT_SHIFT,
                            level * verticalScale + verticalScale /4,
                            (et.getExecutionTimeMax() - et.getExecutionTimeMin()) * horizontalScale,
                            verticalScale/2);
                    //Period
                    g.setColor(PERIOD_COLOR);
                    g.fillRect((i + et.getPeriod()) * horizontalScale - 1 + LEFT_SHIFT,
                            level * verticalScale,
                            2,
                            verticalScale);
                }
                level++;
            }

            //X axis labels
            if (horizontalScale >= 32) {
                for (int i = 0; i <= hyperperiod; i++) {
                    g.setColor(OUTLINE_COLOR);
                    g.fillRect(i * horizontalScale - 1 + LEFT_SHIFT,
                            level * verticalScale,
                            2,
                            verticalScale / 2);
                    g.setColor(TEXT_COLOR);
                    g.drawString(Integer.toString(i),
                            i * horizontalScale - 5 + LEFT_SHIFT,
                            level * verticalScale + verticalScale);
                }
            } else {
                int extraScale = 2 * 16/horizontalScale;
                for (int i = 0; i <= hyperperiod/extraScale; i++) {
                    g.setColor(OUTLINE_COLOR);
                    g.fillRect(i * extraScale * horizontalScale - 1 + LEFT_SHIFT,
                            level * verticalScale,
                            2,
                            verticalScale / 2);
                    g.setColor(TEXT_COLOR);
                    g.drawString(Integer.toString(i * extraScale),
                            i * extraScale * horizontalScale - 5 + LEFT_SHIFT,
                            level * verticalScale + verticalScale);
                }
            }

            //Bottom line and left Line
            g.fillRect(LEFT_SHIFT,level* verticalScale, hyperperiod * horizontalScale,2);
            g.fillRect(LEFT_SHIFT-1,
                    0,
                    2,
                    verticalScale*level);

            //Selected job
            g.setColor(SELECTED_JOB_COLOR);
            Task curSelectedTask = allTasks.get(taskIdOrder.get(selectedTask));
            int selectedJobReleaseShift = releaseShifts.get(curSelectedTask.getId()).get(selectedJob);
            g.fillRect((selectedJob * curSelectedTask.getPeriod() + selectedJobReleaseShift) * horizontalScale + LEFT_SHIFT,
                    selectedTask* verticalScale + 3*verticalScale /8,
                    horizontalScale/4,
                    verticalScale /4);
        }
    }


}
