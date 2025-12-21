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
    public int get_turnaround_time(){return this.turnaround_time;}

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
abstract class Schedule{
    protected List<Process> processes;
    protected List<String> executionOrder;
    protected int contextSwitchTime;

    public Schedule(List<Process> processes, int contextSwitchTime) {
        this.processes = processes;
        this.contextSwitchTime = contextSwitchTime;
        this.executionOrder = new ArrayList<>();
    }

    // template
    public final void execute(){
        validateInput();
        runSchedule();
        calculateMetrics();
    }

    protected void validateInput(){
        if(processes.isEmpty() || processes == null)
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

    public void printProcessStats(){
        System.out.println("Name\tWaiting\tTurnaround");
        for(Process p : processes)
            System.out.println(p.get_name()+"\t\t"
            + p.get_waiting_time()+"\t\t"
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
}

//  ==============================================     1. SRJF    ============================================    //

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
    public int get_RemainingTime() {return remainingTime;}
    public boolean isStarted() {return started;}
    public void setStarted(boolean started) {this.started = started;}
    public int getTotal_waiting_time() {return total_waiting_time;}


    // build again
    public void executeOneUnit(int currentTime) {
        if(!started) {
            this.started = true;
            time_in = currentTime;
        }
        remainingTime--;
        lastRunTime = currentTime;
    }

    // call when a process waiting
    public void updateWaitingTime(int currentTime) {
        if(started && lastRunTime != -1 && remainingTime > 0)
            this.waiting_time += (currentTime - lastRunTime);
    }

    // when a process finished
    public void finish(int finishTime) {
        this.time_out = finishTime;
        this.turnaround_time = finishTime - arrival_time;
    }
}

class SJF_Schedule extends Schedule {
    private List<SJF_process> sjf_processes;
    private PriorityQueue<SJF_process> readyQ;
    private Map<String, SJF_process> processMap; // to map back to original processes

    public SJF_Schedule(List<Process> processes, int contextSwitchTime) {
        super(processes, contextSwitchTime);
        this.sjf_processes =  new ArrayList<>();
        this.processMap = new HashMap<>();

        for(Process p : processes) {
            SJF_process sjf = new SJF_process(p.get_name(), p.get_arrival_time(), p.get_burst_time());
            sjf_processes.add(sjf);
            processMap.put(p.get_name(), sjf);
        }

        this.readyQ = new PriorityQueue<>(
                Comparator.comparing(SJF_process::get_RemainingTime)
                        .thenComparingInt(p -> p.get_arrival_time())
        );
    }

// MAIN FNC
    @Override
    protected void runSchedule() {
        int currentTime = 0;
        SJF_process currentRunning = null;
        int completed = 0;
        while(completed < this.sjf_processes.size()) {
            /* load ready processes it the current time
             -> all processes that has arrival time <= currentTime
             and not completed*/
            for(SJF_process p : sjf_processes)
                if(p.arrival_time <= currentTime
                   && p.get_RemainingTime() > 0
                   && !readyQ.contains(p))
                        readyQ.offer(p);

            SJF_process nextToRun = null;
            if(!readyQ.isEmpty())
                nextToRun = readyQ.peek();

            // if p was running already, or it's the first process in the readyQ then currentTime will be the same
            // else currentTime += context switch cs
            if(currentRunning != nextToRun && currentRunning != null) {
                currentRunning.updateWaitingTime(currentTime);
                currentTime += contextSwitchTime;  // context switch overhead
                executionOrder.add("CS");
            }

            // if no process is ready -> advance time to the next arrival
            if(readyQ.isEmpty() && completed < sjf_processes.size()) {
                int nextArrival = Integer.MAX_VALUE;
                for(SJF_process p:sjf_processes)
                    if(p.get_RemainingTime() > 0)
                        nextArrival = Math.min(p.get_arrival_time(), nextArrival);

                // we found a process that hasn't finished -> else there is a logical error
                if(nextArrival != Integer.MAX_VALUE)
                    currentTime = nextArrival;
                continue;
            }

            // shortest process to run
            currentRunning = readyQ.poll();
            executionOrder.add(currentRunning.get_name());
            currentRunning.executeOneUnit(currentTime);
            ++currentTime;

            // check if p is completed to calc turnaround time and other metrics if available
            if(currentRunning.get_RemainingTime() == 0) {
                currentRunning.finish(currentTime);
                ++completed;
                currentRunning = null;
            }
            else // put it back to the readyQ
                readyQ.offer(currentRunning);
        }
        // while all processes haven't completed yet:
            // choose minimum remaining of them to run (let we call it p) -> if two have the same remaining time then compare with arrival time
            // executionOrder.add(p.get_name())

            // execute p for one time unit
    }

    @Override
    protected void calculateMetrics() {
        // update waiting time for all processes
        for (SJF_process sp : sjf_processes) {
            Process original = null;
            for (Process p : processes) {
                if (p.get_name().equals(sp.get_name())) {
                    original = p;
                    break;
                }
            }
            if (original != null) {
                // Waiting time = Turnaround time - Burst time
                original.waiting_time = sp.get_turnaround_time() - sp.get_burst_time();
                original.turnaround_time = sp.get_turnaround_time();
            }
        }
    }
}

public class scheduling {
    public static void main(String[] args) {
        List<Process> processes = new ArrayList<>(Arrays.asList(
               new Process("P1", 0,8,3),
                new Process("P2", 1,4,3),
                new Process("P3", 2,2,3),
                new Process("P4", 3,1,3),
                new Process("P5", 4,3,3))
        );

        SJF_Schedule schedule = new SJF_Schedule(processes, 1);
        schedule.execute();
        schedule.printExecutionOrder();
        schedule.printProcessStats();
        schedule.printAverages();

    }
}