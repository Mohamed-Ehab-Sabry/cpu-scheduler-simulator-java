import java.util.*;

// ===============      GENERAL PART *FOR ALL*      ===============
class Process {

    protected String name;
    protected int arrival_time;
    protected int burst_time;
    protected int priority;
    protected int time_in = -1;
    protected int time_out = -1;
    protected int waiting_time;
    protected int turnaround_time;

    public Process(String name, int arrival_time, int burst_time, int priority) {

        this.name = name;
        this.arrival_time = arrival_time;
        this.burst_time = burst_time;
        this.priority = priority;
        this.time_out = arrival_time;

        this.waiting_time = 0;
        this.turnaround_time = 0;
    }

    public String get_name() {
        return name;
    }

    public int get_arrival_time() {
        return arrival_time;
    }

    public int get_burst_time() {
        return burst_time;
    }

    public int get_priority() {
        return priority;
    }

    public int get_time_in() {
        return time_in;
    }

    public int get_time_out() {
        return time_out;
    }

    public int get_waiting_time() {
        return waiting_time;
    }

    public int get_turnaround_time() {
        return this.turnaround_time;
    }

    public void set_time_in(int time_in) {
        this.time_in = time_in;
    }

    public void set_time_out(int time_out) {
        this.time_out = time_out;
    }

    public void add_to_waiting_time() {
        this.waiting_time += Math.abs(this.time_out - this.time_in);
    }

}

// SCHEDULE IN GENERAL
abstract class Schedule {
    protected List<Process> processes;
    protected List<String> executionOrder;
    protected int contextSwitchTime;

    public Schedule(List<Process> processes, int contextSwitchTime) {
        this.processes = processes;
        this.contextSwitchTime = contextSwitchTime;
        this.executionOrder = new ArrayList<>();
    }

    // template
    public final void execute() {
        validateInput();
        runSchedule();
        calculateMetrics();
    }

    protected void validateInput() {
        if (processes.isEmpty() || processes == null)
            throw new IllegalArgumentException("No processes to schedule");
    }

    protected abstract void runSchedule();

    protected abstract void calculateMetrics();

    public void printExecutionOrder() {
        System.out.print("Execution Order: ");
        for (String name : executionOrder) {
            System.out.print(name + " ");
        }
        System.out.println();
    }

    public void printProcessStats() {
        System.out.println("Name\tWaiting\tTurnaround");
        for (Process p : processes)
            System.out.println(p.get_name() + "\t\t"
                    + p.get_waiting_time() + "\t\t"
                    + p.get_turnaround_time());
    }

    public void printAverages() {
        double avgWaiting = processes.stream()
                .mapToInt(Process::get_waiting_time)
                .average()
                .orElse(0.0);

        double avgTurnaround = processes.stream()
                .mapToInt(Process::get_turnaround_time)
                .average()
                .orElse(0.0);

        System.out.printf("\nAverage Waiting Time: %.2f\n", avgWaiting);
        System.out.printf("Average Turnaround Time: %.2f\n", avgTurnaround);
    }

    // Add a process name to the execution order, inserting a "CS" marker
    // when a context switch occurs between two different processes.
    protected int addExecutionEntry(String name, int ct) {
        if(executionOrder.isEmpty())
            executionOrder.add(name);
        String last = executionOrder.get(executionOrder.size() - 1);
        if (this.contextSwitchTime > 0) {
        /*if (!last.equals(name) && !last.equals("CS")) {
            executionOrder.add("CS");
            ++ct;
            }
        }*/
        if (!last.equals(name) && !last.equals("CS"))
            executionOrder.add(name);}

        return ct;
    }
}

// ============================================== 1. SRJF   ============================================ //

class SJF_process extends Process {
    protected int remainingTime = 0;
    protected boolean started = false;
    protected int total_waiting_time = 0;
    protected int lastRunTime = -1;

    public SJF_process(String name, int arrival_time, int burst_time) {
        super(name, arrival_time, burst_time, 0); // ignore the priority -> make it 0
        this.remainingTime = burst_time;
        this.started = false;
        this.total_waiting_time = 0;
    }

    // SETTERS & GETTERS
    public int get_RemainingTime() {
        return remainingTime;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public int getTotal_waiting_time() {
        return total_waiting_time;
    }

    public int executeOneUnit(int currentTime) {
        if (!started) {
            this.started = true;
            time_in = currentTime;
        }
        remainingTime--;
        ++currentTime;
        return currentTime;
    }

    // call when a process waiting
    public void updateWaitingTime(int currentTime) {
        if (started && lastRunTime != -1 && remainingTime > 0)
            this.waiting_time += (currentTime - lastRunTime);
    }

    // when a process finished
    public void finish(int finishTime) {
        this.time_out = finishTime;
        this.turnaround_time = finishTime - arrival_time;
        this.waiting_time = turnaround_time - burst_time;
    }
}

class SJF_Schedule extends Schedule {
    private List<SJF_process> sjf_processes;
    private PriorityQueue<SJF_process> readyQ;
    private Map<String, SJF_process> processMap; // to map back to original processes

    public SJF_Schedule(List<Process> processes, int contextSwitchTime) {
        super(processes, contextSwitchTime);
        this.sjf_processes = new ArrayList<>();
        this.processMap = new HashMap<>();

        for (Process p : processes) {
            SJF_process sjf = new SJF_process(p.get_name(), p.get_arrival_time(), p.get_burst_time());
            sjf_processes.add(sjf);
            processMap.put(p.get_name(), sjf);
        }

        this.readyQ = new PriorityQueue<>(
                Comparator.comparing(SJF_process::get_RemainingTime)
                        .thenComparingInt(p -> p.get_arrival_time())
                        .thenComparing(p -> p.get_name())
        );
    }

    // MAIN FNC
    @Override
    protected void runSchedule() {
        int currentTime = 0;
        SJF_process currentRunning = null;
        int completed = 0;
        // while all processes haven't completed yet:
        // ✅ load ready processes it the current time -> all processes that has arrival time <= currentTime and not completed
        // ✅ choose minimum remaining of them to run (let we call it p) -> if two have the same remaining time then compare with arrival time
        // ✅ executionOrder.add(p.get_name())
        // ✅ if p was running already, or it's the first process in the readyQ then currentTime will be the same
        // ✅ else currentTime += context switch cs
        // ✅ execute p for one time unit
        // ✅ check if p is completed to calc turnaround time and other metrics if available
        while(completed < sjf_processes.size()) {

            // load ready processes
            for(SJF_process p : sjf_processes)
                if(p.remainingTime > 0 && p.arrival_time <= currentTime)
                    readyQ.offer(p);

            // if no process is ready and all process not completed yet
            if(readyQ.isEmpty() && completed < sjf_processes.size()) {
                int nxt_arrival = Integer.MAX_VALUE;
                for (SJF_process p : sjf_processes)
                    if(p.remainingTime > 0)
                        nxt_arrival = Math.min(nxt_arrival, p.get_arrival_time());
                if(nxt_arrival != Integer.MAX_VALUE)
                    currentTime = nxt_arrival;
                continue;
            }

            // choose the minimum remaining time
            currentRunning = readyQ.poll();

            // add cs if only the last running process differ from the current
            // add p to the exec order

            if (!executionOrder.isEmpty()) {
                String last = executionOrder.get(executionOrder.size() - 1);
                if (!last.equals(currentRunning.get_name())) {
                    currentTime += contextSwitchTime;
                    executionOrder.add(currentRunning.get_name());
                }
            } else
                    executionOrder.add(currentRunning.get_name());
            // execute p for only one time unit
            currentTime = currentRunning.executeOneUnit(currentTime);

            if(currentRunning.remainingTime == 0){
                currentRunning.finish(currentTime);
                ++completed;
            }

        }

    }
    @Override
    protected void calculateMetrics() {
        // Copy metrics from SJF_process to original Process objects
        for (SJF_process sjfProc : sjf_processes) {
            // Find the corresponding original process
            for (Process original : processes) {
                if (original.get_name().equals(sjfProc.get_name())) {
                    original.waiting_time = sjfProc.get_waiting_time();
                    original.turnaround_time = sjfProc.get_turnaround_time();
                    original.time_in = sjfProc.get_time_in();
                    original.time_out = sjfProc.get_time_out();
                    break;
                }
            }
        }
    }
    }


public class scheduling {
    public static void main(String[] args) {
        // test1
        /*List<Process> processes = new ArrayList<>(Arrays.asList(
                new Process("P1", 0, 8, 3),
                new Process("P2", 1, 4, 3),
                new Process("P3", 2, 2, 3),
                new Process("P4", 3, 1, 3),
                new Process("P5", 4, 3, 3)));
        */
        //test2
        /*List<Process> processes = new ArrayList<>(Arrays.asList(
                new Process("P1", 0, 6, 3),
                new Process("P2", 0, 3, 3),
                new Process("P3", 0, 8, 3),
                new Process("P4", 0, 4, 3),
                new Process("P5", 0, 2, 3)));
*/
        //test3
        /*List<Process> processes = new ArrayList<>(Arrays.asList(
                new Process("P1", 0, 10, 3),
                new Process("P2", 2, 5, 3),
                new Process("P3", 5, 3, 3),
                new Process("P4", 8, 7, 3),
                new Process("P5", 10, 2, 3)));
*/
        //test4
        /*List<Process> processes = new ArrayList<>(Arrays.asList(
                new Process("P1", 0, 12, 3),
                new Process("P2", 4, 9, 3),
                new Process("P3", 8, 15, 3),
                new Process("P4", 12, 6, 3),
                new Process("P5", 16, 11, 3),
                new Process("P6", 20, 5, 3)
        ));*/

        //test5
        /*List<Process> processes = new ArrayList<>(Arrays.asList(
                new Process("P1", 0, 3, 3),
                new Process("P2", 1, 2, 3),
                new Process("P3", 2, 4, 3),
                new Process("P4", 3, 1, 3),
                new Process("P5", 4, 3, 3)
        ));*/

        // test6
        List<Process> processes = new ArrayList<>(Arrays.asList(
                new Process("P1", 0, 14, 3),
                new Process("P2", 3, 7, 3),
                new Process("P3", 6, 10, 3),
                new Process("P4", 9, 5, 3),
                new Process("P5", 12, 8, 3),
                new Process("P6", 15, 4, 3)
        ));

        SJF_Schedule schedule = new SJF_Schedule(processes, 1);
        schedule.execute();
        schedule.printExecutionOrder();
        schedule.printProcessStats();
        schedule.printAverages();

    }
}