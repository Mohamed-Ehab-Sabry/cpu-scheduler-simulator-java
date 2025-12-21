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

    }

    public void updateWaitingTime() {

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
                        .thenComparingInt(SJF_process::get_arrival_time)
        );
    }

// MAIN FNC
    @Override
    protected void runSchedule() {

    }

    @Override
    protected void calculateMetrics() {} // already calculated
}

public class scheduling {
    public static void main(String[] args) {
        // test 4
        // BUILDING PROCESSES IN GENERAL
        //Scanner in = new Scanner(System.in);
        int agingInterval = 6;
        int contextSwitchTime = 2;
        int rrQuantum = 5;

        List<Process> general_processes = new ArrayList<>();
        int[] arrivals = {0,4,8,12,16,20};
        int[] bursts = {12,9,15,6,11,5};
        int[] priorities = {2,3,1,4,2,5};

        for(int i = 0; i < agingInterval; i++){
            String name = "P" + String.valueOf(i+1);
            /*System.out.print("Process: "+ name);
            System.out.print("\nArrival Time:\t");
            System.out.print("\nBurst Time:\t");
            System.out.print("\nPriority:\t");*/
            general_processes.add(new Process(name, arrivals[i], bursts[i], priorities[i]));
        }
        //in.close();

        System.out.println("\n===========     1. SRJF    =========\n");
        SJF_Schedule sjfSchedule = new SJF_Schedule(new ArrayList<>(general_processes), contextSwitchTime);
        sjfSchedule.execute();
        sjfSchedule.printExecutionOrder();
        sjfSchedule.printProcessStats();
        sjfSchedule.printAverages();
    }
}