# Combination of time-triggered and event-triggered scheduling

This repository contains implementations of algorithms used in scheduling event-triggered and time-triggered task
where an event-triggered task is a task with fixed priority, release jitter, and execution time variation
and time-triggered task with no release jitter or execution time variation.
We assume that an upper and lower bound on the execution time and release time of ET tasks is known a priori.

## How to build

You will need the following software:
*  Java 8 (JDK, JVM, and JRE)
*  Maven 3 or newer

Run the following commands in the root of the repository:
* `mvn clean`
* `mvn package`

This should create a file at `./target/ettt_scheduler.jar` which contains all the scheduling algorithms.

## How to use

To launch the program on some instance `instance.csv` use command: `java -jar ./target/ettt_scheduler.jar instance.csv`.
This command will not do anything because the command does not specify what should the program do with the instance.
Use command `java -jar ./target/ettt_scheduler.jar -h` to display help.

### Input instance format

The input of the program is a set of time and event triggered tasks.
These tasks are described in a .csv file where each line corresponds to one task (excluding the first line, which is a header).
Task's values are in the following order:
* **Task ID**: ID of the task
* **Period**: period of the task
* **Minimal release time**: lower bound on the release time, this value is ignored for TT tasks
* **Maximal release time**: upper bound on the release time, this value is set as the release time for TT tasks
* **Minimal execution time**: lower bound on the execution time, this value is ignored for TT tasks
* **Maximal execution time**: upper bound on the execution time, this value is set as the execution time for TT tasks
* **Deadline**: deadline of the task
* **Priority**: priority of the task, if it is set to 0 the task is considered to be a TT task, otherwise it is an ET task

The instance file must also follow by these rules:
* Every value is a positive integer or 0 (including ignored values)
* All TT tasks are defined before ET tasks
* Task IDs are always 0, 1, 2, 3... and so on based on the number of tasks 

There are two example instances in directory `./instances`.   

### Output

The program outputs simple information into stdout.
If the program saves something in a file, the file is located in the same directory as the input file and under the same name with a different postfix.

## Examples

### ET task schedulability

Let us say that we wish to know the schedulability of the instance specified in file `./instances/example_ET_instance.csv` under the CP policy.
This can be done with command: `java -jar ./target/ettt_scheduler.jar ./instances/example_ET_instance.csv -a ET-SG -p CP`.
In this case, the instance is not schedulable.

To see why the instance is not schedulable, we can save the generated schedule graph into a file.
This can be done with command: `java -jar ./target/ettt_scheduler.jar ./instances/example_ET_instance.csv -a ET-SG -p CP -g`.
The graph is saved in a graphviz format at `./target/example_ET_instance.csv.sg.dot`. 

### Finding start times for TT tasks

Let us say that we wish to know start times with jitter of the instance specified in file `./instances/example_ETTT_instance.csv`.
This can be done with command: `java -jar ./target/ettt_scheduler.jar ./instances/example_ETTT_instance.csv -a ETTT-BF-WJ -s`.
The start times are saved in csv format at `./target/example_ETTT_instance.csv.st.csv`.

## Instances used in benchmarks

Folder *instances* also contains instances used in benchmarking the application's algorithms.