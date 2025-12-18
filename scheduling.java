import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

// PROCESS IN GENERAL
class process {

    protected String name;
    protected int arrival_time;
    protected int burst_time;
    protected int priority;
    protected int time_in;
    protected int time_out;
    protected int waiting_time;
    protected int turnaround_time;

    public process(String name, int arrival_time, int burst_time, int priority) {

        this.name = name;
        this.arrival_time = arrival_time;
        this.burst_time = burst_time;
        this.priority = priority;
        this.time_out = arrival_time;
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
    protected List<process> processes;
    protected List<String> executionOrder;
    protected int contextSwitchTime;

    public Schedule(List<process> processes, int contextSwitchTime) {
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

    public void printProcessStats(){
        System.out.println("Name\tWaiting\tTurnaround");
        for(process p : processes)
            System.out.println(p.get_name()+"\t"
            + p.get_waiting_time()+"\t"
            + p.get_turnaround_time());
    }

    public void printAverages() {
        double avgWaiting = processes.stream()
                .mapToInt(process::get_waiting_time)
                .average()
                .orElse(0.0);

        double avgTurnaround = processes.stream()
                .mapToInt(process::get_turnaround_time)
                .average()
                .orElse(0.0);

        System.out.printf("\nAverage Waiting Time: %.2f\n", avgWaiting);
        System.out.printf("Average Turnaround Time: %.2f\n", avgTurnaround);
    }
}

class ag_process extends process {

    protected int quantum = 0;
    protected List<Integer> quantum_hist = new ArrayList<>();

    public ag_process(String name, int arrival_time, int burst_time, int priority, int quantum) {

        super(name, arrival_time, burst_time, priority);
    }

    public int get_quantum() {

        return quantum;
    }

    public List<Integer> get_qunatm_hist() {

        return quantum_hist;

    }

    public void set_quantum(int quantum) {

        this.quantum = quantum;

    }

    public void add_to_qunatm_hist(int quantum) {

        quantum_hist.add(quantum);

    }
}

//  ==============================================     SRJF    ============================================    //
class SJF_process extends process {
    protected int remainingTime = 0;
    protected int turnaroundTime =0;
    protected boolean started = false;
    protected static int contextSwitchTime = 0;
    protected static int current_time = 0;

// LISTS
    protected List<SJF_process> processes = new ArrayList<>();
    protected List<String> ExecOrder = new ArrayList<>();

    private SJF_process(String name, int arrival_time, int burst_time) {
        super(name, arrival_time, burst_time, 0); // ignore the priority -> make it 0
        this.remainingTime = burst_time;
        this.turnaroundTime = 0;
    }
// SETTERS & GETTERS
    public int get_RemainingTime() {return remainingTime;}
    public int get_turnaroundTime() {return turnaroundTime;}
    public void setStarted(boolean started) {this.started = started;}
    public boolean isStarted() {return started;}
    public void setContextSwitchTime(int contextSwitchTime) {this.contextSwitchTime = contextSwitchTime;}
    public int getContextSwitchTime() {return contextSwitchTime;}
    public void addProcess(String name, int arrival_time, int burst_time){
        processes.add(new SJF_process(name,arrival_time,burst_time));
    }
// HELPERS
    private boolean thereIsExistProcess(List<SJF_process> processes){
        for (SJF_process p : processes)
            if(p.remainingTime != 0)
                return true;
        return false;
    }

    private List<SJF_process> availableProcesses(){
        List<SJF_process> res = new ArrayList<>();
        for(SJF_process p : processes)
            // if the process didn't finished yet and it's already arrived
            if(p.arrival_time <= this.current_time && p.remainingTime != 0)
                res.add(p);

        return res;
    }
// MAIN METHODS
    public void run_schedule() {

        while(thereIsExistProcess(this.processes)) {
            List<SJF_process> RemainingProcesses = availableProcesses(); // processes that arrived and didn't finished yet
            List<SJF_process> MinTwo_processes = availableProcesses().stream() // 2 minimum burstTime processes from remains
                    .sorted(Comparator.comparing(SJF_process::get_burst_time))
                    .limit(2)
                    .toList();

            SJF_process toBeExec = MinTwo_processes.get(0); // process with minimum burst time
            SJF_process scndMinimum_P = MinTwo_processes.get(1);
            ExecOrder.add(toBeExec.name);

            int endProcess = Math.min(toBeExec.burst_time, scndMinimum_P.arrival_time);
            toBeExec.remainingTime -= endProcess;
            this.current_time += endProcess;


        }
    }

    public void PrintExecOrder() {
        for(String name : this.ExecOrder) {
            System.out.print(name+'\t');
        }
    }

    public double AvgWaitingTime() {
        return  0; // tmp
    }

    public double AvgTurnaroundTime(){
        return  0; // tmp
    }
}


public class scheduling {

    List<ag_process> processes = new ArrayList<>();
    List<ag_process> arrived_processes = new ArrayList<>();

    public void ag_schedule() {

        int current_time = 0;
        List<String> execution_order = new ArrayList<>();

        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("How many processes do you want to schedule: ");
            int num_of_processes = sc.nextInt();
            sc.nextLine();
            for (int i = 0; i < num_of_processes; i++) {

                System.out.println("Process " + i + 1 + " name: ");
                String name = sc.next();

                System.out.println("Arrival time: ");
                int arrival = sc.nextInt();

                System.out.println("burst time: ");
                int burst = sc.nextInt();

                System.out.println("priority: ");
                int priority = sc.nextInt();

                System.out.println("quantum: ");
                int quantum = sc.nextInt();

                ag_process p = new ag_process(name, arrival, burst, priority, quantum);
                processes.add(p);

            }
            processes.sort(java.util.Comparator.comparingInt(p -> p.arrival_time));
        }

        while (!processes.isEmpty() || !arrived_processes.isEmpty()) {

            for (ag_process p : processes) {

                if (p.get_arrival_time() >= current_time) {
                    arrived_processes.add(p);
                }
            }
            if (!arrived_processes.isEmpty()) {

                ag_process curr_p = arrived_processes.get(0);
                curr_p.set_time_in(current_time);
                curr_p.add_to_waiting_time();
                int time_spent = (int) Math.ceil(curr_p.get_quantum() / 0.25);
                current_time += time_spent;
                int curr_p_quantum = curr_p.get_quantum();
                curr_p.add_to_qunatm_hist(curr_p_quantum);
                curr_p.set_quantum(curr_p_quantum - time_spent);
            }
        }

    }
}
