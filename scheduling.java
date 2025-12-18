import java.util.*;

// ===============      GENERAL PART *FOR ALL* ===============
class Process {
    protected String name;
    protected int arrival_time;
    protected int burst_time;
    protected int priority;
    protected int time_in = -1;
    protected int time_out = -1;
    protected int waiting_time;
    protected int turnaround_time;
    // Added original_burst to track progress accurately across all algorithms
    protected int original_burst_time; 

    public Process(String name, int arrival_time, int burst_time, int priority) {
        this.name = name;
        this.arrival_time = arrival_time;
        this.burst_time = burst_time;
        this.original_burst_time = burst_time;
        this.priority = priority;
        this.time_out = 0;
        this.waiting_time = 0;
        this.turnaround_time = 0;
    }

    // Getters & Setters
    public String get_name() { return name; }
    public int get_arrival_time() { return arrival_time; }
    public int get_burst_time() { return burst_time; }
    public int get_priority() { return priority; }
    public int get_time_in() { return time_in; }
    public int get_time_out() { return time_out; }
    public int get_waiting_time() { return waiting_time; }
    public int get_turnaround_time(){ return turnaround_time; }

    public void set_time_in(int time_in) { this.time_in = time_in; }
    public void set_time_out(int time_out) { this.time_out = time_out; }
    
    // Decrement burst time (helper for simulation)
    public void reduce_burst(int amount) {
        this.burst_time -= amount;
        if(this.burst_time < 0) this.burst_time = 0;
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

    public final void execute() {
        validateInput();
        runSchedule();
        calculateMetrics();
    }

    protected void validateInput() {
        if(processes.isEmpty() || processes == null)
            throw new IllegalArgumentException("No processes to schedule");
    }

    protected abstract void runSchedule();
    
    // Default metric calculation (can be overridden)
    protected void calculateMetrics() {
        for(Process p : processes) {
            p.turnaround_time = p.time_out - p.arrival_time;
            p.waiting_time = p.turnaround_time - p.original_burst_time;
        }
    }

    public void printExecutionOrder() {
        System.out.print("Execution Order: [ ");
        for (String name : executionOrder) {
            System.out.print(name + " ");
        }
        System.out.println("]");
    }

    public void printProcessStats() {
        System.out.println("Name\tWaiting\tTurnaround");
        // Sort by name for cleaner output
        processes.sort(Comparator.comparing(Process::get_name));
        for(Process p : processes)
            System.out.println(p.get_name()+"\t"+ p.get_waiting_time()+"\t"+ p.get_turnaround_time());
    }

    public void printAverages() {
        double avgWaiting = processes.stream().mapToInt(Process::get_waiting_time).average().orElse(0.0);
        double avgTurnaround = processes.stream().mapToInt(Process::get_turnaround_time).average().orElse(0.0);
        System.out.printf("Average Waiting Time: %.2f\n", avgWaiting);
        System.out.printf("Average Turnaround Time: %.2f\n", avgTurnaround);
    }
}

// ================== 1. SRJF (From hisscheduling) ==================
class SJF_process extends Process {
    protected int remainingTime;
    protected boolean started = false;

    public SJF_process(Process p) {
        super(p.name, p.arrival_time, p.burst_time, p.priority);
        this.remainingTime = p.burst_time;
    }

    public int get_RemainingTime() { return remainingTime; }
    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }
    
    public void executeOneUnit() { remainingTime--; }
}

class SJF_Schedule extends Schedule {
    private List<SJF_process> sjf_processes;
    private PriorityQueue<SJF_process> readyQ;

    public SJF_Schedule(List<Process> processes, int contextSwitchTime) {
        super(processes, contextSwitchTime);
        this.sjf_processes = new ArrayList<>();
        // Convert generic processes to SJF processes
        for(Process p : processes) sjf_processes.add(new SJF_process(p));
        
        this.readyQ = new PriorityQueue<>(
                Comparator.comparing(SJF_process::get_RemainingTime)
                        .thenComparingInt(Process::get_arrival_time));
    }

    @Override
    protected void runSchedule() {
        int currTime = 0, completed = 0;
        SJF_process current_process = null;
        
        // Use a copy list to manage arrivals
        List<SJF_process> incoming = new ArrayList<>(sjf_processes);
        incoming.sort(Comparator.comparingInt(Process::get_arrival_time));

        while(completed < sjf_processes.size()) {
            // Add arrivals
            while(!incoming.isEmpty() && incoming.get(0).arrival_time <= currTime){
                readyQ.add(incoming.remove(0));
            }

            // Check if current process is done
            if(current_process != null && current_process.get_RemainingTime() == 0){
                completed++;
                current_process.set_time_out(currTime);
                current_process = null;
            }

            // Context switch / Preemption logic
            if(!readyQ.isEmpty()){
                SJF_process next = readyQ.peek();
                if(current_process == null || next.get_RemainingTime() < current_process.get_RemainingTime()){
                    // Context Switch penalty would go here
                    if(current_process != null && current_process.get_RemainingTime() > 0) {
                        readyQ.add(current_process);
                    }
                    current_process = readyQ.poll();
                    executionOrder.add(current_process.get_name());
                }
            }

            if(current_process != null) {
                current_process.executeOneUnit();
                currTime++;
            } else {
                // Time skip if idle
                if(!incoming.isEmpty()) currTime = incoming.get(0).arrival_time;
                else currTime++;
            }
        }

        // Map results back to original list for reporting
        for(int i=0; i<processes.size(); i++) {
            Process orig = processes.get(i);
            // Find matching SJF process (simplified by assuming order/name match)
            for(SJF_process sjf : sjf_processes) {
                if(sjf.name.equals(orig.name)) {
                    orig.time_out = sjf.time_out;
                    break;
                }
            }
        }
    }
}

// ================== 2. ROUND ROBIN (Adapted from Myscheduling) ==================
class RR_Schedule extends Schedule {
    private int quantum;

    public RR_Schedule(List<Process> processes, int contextSwitchTime, int quantum) {
        super(deepCopy(processes), contextSwitchTime); // Work on copy to not ruin original data
        this.quantum = quantum;
    }

    // Helper to deep copy list so we can modify burst times safely
    private static List<Process> deepCopy(List<Process> list) {
        List<Process> copy = new ArrayList<>();
        for(Process p : list) copy.add(new Process(p.name, p.arrival_time, p.burst_time, p.priority));
        return copy;
    }

    @Override
    protected void runSchedule() {
        Queue<Process> readyQueue = new LinkedList<>();
        List<Process> pending = new ArrayList<>(processes);
        pending.sort(Comparator.comparingInt(Process::get_arrival_time));
        
        int currentTime = 0;
        Process lastProcess = null;
        int completed = 0;

        while(completed < processes.size()) {
            // Add newly arrived processes
            while(!pending.isEmpty() && pending.get(0).arrival_time <= currentTime) {
                readyQueue.add(pending.remove(0));
            }

            if(readyQueue.isEmpty()) {
                if(!pending.isEmpty()) currentTime = pending.get(0).arrival_time;
                else currentTime++;
                continue;
            }

            Process p = readyQueue.poll();

            // Handle Context Switch
            if(lastProcess != null && lastProcess != p && contextSwitchTime > 0) {
                currentTime += contextSwitchTime;
                // Check arrivals during CS
                while(!pending.isEmpty() && pending.get(0).arrival_time <= currentTime) {
                    readyQueue.add(pending.remove(0));
                }
            }

            // Execute
            executionOrder.add(p.name);
            int timeSlice = Math.min(quantum, p.burst_time);
            currentTime += timeSlice;
            p.reduce_burst(timeSlice);
            lastProcess = p;

            // Check arrivals during execution
            while(!pending.isEmpty() && pending.get(0).arrival_time <= currentTime) {
                readyQueue.add(pending.remove(0));
            }

            if(p.burst_time > 0) {
                readyQueue.add(p);
            } else {
                p.set_time_out(currentTime);
                completed++;
            }
        }
    }
}

// ================== 3. AG SCHEDULING (Adapted from Myscheduling) ==================

// Specialized Process for AG
class AG_Process extends Process {
    int quantum;
    List<Integer> quantum_history = new ArrayList<>();

    public AG_Process(Process p, int initQuantum) {
        super(p.name, p.arrival_time, p.burst_time, p.priority);
        this.quantum = initQuantum;
        this.quantum_history.add(initQuantum);
    }
}
class AG_Schedule extends Schedule {
    private List<AG_Process> ag_processes;
    private int initQuantum;

    public AG_Schedule(List<Process> processes, int initQuantum) {
        super(processes, 0); // Context switch is usually 0 for AG
        this.initQuantum = initQuantum;
        this.ag_processes = new ArrayList<>();
        // Convert to AG Processes
        for(Process p : processes) {
            ag_processes.add(new AG_Process(p, initQuantum));
        }
    }

    enum State { FCFS, PRIORITY, SJF }

    @Override
    protected void runSchedule() {
        List<AG_Process> pending = new ArrayList<>(ag_processes);
        List<AG_Process> readyQueue = new ArrayList<>(); 
        List<AG_Process> finished = new ArrayList<>();
        
        int currentTime = 0;
        State state = State.FCFS;
        int time_executed = 0;
        AG_Process curr_p = null;

        // Sort pending by arrival initially to match your logic
        pending.sort(Comparator.comparingInt(Process::get_arrival_time));

        while (!pending.isEmpty() || !readyQueue.isEmpty()) {
            
            // 1. Check Arrivals (Exact match to your logic: only check at start of loop)
            for (int i = 0; i < pending.size(); i++) {
                if (pending.get(i).get_arrival_time() <= currentTime) {
                    readyQueue.add(pending.get(i));
                    pending.remove(i);
                    i--; // adjust index after removal
                }
            }

            if (!readyQueue.isEmpty()) {
                // === STATE: FCFS ===
                if (state == State.FCFS) {
                    curr_p = readyQueue.get(0); 
                    
                    // Run for 25% of Quantum
                    int cycle_limit = currentTime + (int) Math.ceil(curr_p.quantum * 0.25);
                    executionOrder.add(curr_p.name);
                    curr_p.quantum_history.add(curr_p.quantum); // Log history
                    time_executed = 0;

                    // Execute without checking arrivals (matching your original inner loop)
                    while (currentTime < cycle_limit && curr_p.burst_time > 0) {
                        currentTime++;
                        time_executed++;
                        curr_p.reduce_burst(1);
                    }

                    if (curr_p.burst_time == 0) {
                        handleFinish(curr_p, currentTime, readyQueue, finished);
                        state = State.FCFS; 
                    } else {
                        state = State.PRIORITY; // Move to next state
                    }

                // === STATE: PRIORITY ===
                } else if (state == State.PRIORITY) {
                    curr_p = readyQueue.get(0);
                    AG_Process best_p = getBestPriority(readyQueue);
                    
                    if (best_p != curr_p) {
                        // PREEMPTION CASE (Specific AG Rule: Add ceil(remaining / 2))
                        curr_p.time_out = currentTime; // update generic stat
                        int addedQuantum = (int) Math.ceil((curr_p.quantum - time_executed) / 2.0);
                        curr_p.quantum += addedQuantum;
                        
                        // Reorder queue: Move best to front, keep current (but preempted)
                        // Note: Your original code removed both and re-added them in order
                        readyQueue.remove(curr_p); 
                        readyQueue.remove(best_p);
                        readyQueue.add(0, best_p); 
                        readyQueue.add(curr_p); 

                        state = State.FCFS; // Reset cycle
                        time_executed = 0;  // Reset execution counter
                    } else {
                        // Continue current process until 50%
                        int cycle_limit = currentTime + (int) Math.ceil(curr_p.quantum * 0.25); // Run another 25%
                        executionOrder.add(curr_p.name);
                        
                        while (currentTime < cycle_limit && curr_p.burst_time > 0) {
                            currentTime++;
                            time_executed++;
                            curr_p.reduce_burst(1);
                        }

                        if (curr_p.burst_time == 0) {
                            handleFinish(curr_p, currentTime, readyQueue, finished);
                            state = State.FCFS;
                        } else {
                            state = State.SJF;
                        }
                    }

                // === STATE: SJF ===
                } else if (state == State.SJF) {
                    curr_p = readyQueue.get(0);
                    AG_Process best_p = getShortestJob(readyQueue);

                    if (best_p != curr_p) {
                        // PREEMPTION CASE (Specific AG Rule: Add remaining quantum)
                        curr_p.time_out = currentTime;
                        int addedQuantum = curr_p.quantum - time_executed;
                        curr_p.quantum += addedQuantum;

                        readyQueue.remove(curr_p);
                        readyQueue.remove(best_p);
                        readyQueue.add(0, best_p);
                        readyQueue.add(curr_p);
                        
                        state = State.FCFS;
                    } else {
                        // Run rest of quantum
                        int quantum_end_time = currentTime + (curr_p.quantum - time_executed);
                        executionOrder.add(curr_p.name);
                        
                        // Execute unit-by-unit to catch quantum exhaustion exactly
                        // Note: Your original code ran "while current < cycle_period" here too
                        while (currentTime < quantum_end_time && curr_p.burst_time > 0) {
                            currentTime++;
                            time_executed++;
                            curr_p.reduce_burst(1);
                        }

                        if (curr_p.burst_time == 0) {
                            handleFinish(curr_p, currentTime, readyQueue, finished);
                            state = State.FCFS;
                        } else {
                            // Quantum Exhausted
                            state = State.FCFS;
                            curr_p.time_out = currentTime;
                            curr_p.quantum += 2; // Rule: Add 2
                            // Move to back
                            readyQueue.remove(0);
                            readyQueue.add(curr_p);
                        }
                    }
                }
            } else {
                // Idle CPU
                currentTime++;
            }
        }

        // Map final stats back to original process objects
        for(Process orig : processes) {
            for(AG_Process ag : finished) {
                if(orig.name.equals(ag.name)) {
                    orig.time_out = ag.time_out;
                }
            }
        }
    }

    // Helper to finalize a process
    private void handleFinish(AG_Process p, int time, List<AG_Process> ready, List<AG_Process> finished) {
        p.time_out = time;
        p.quantum = 0;
        p.quantum_history.add(0);
        p.turnaround_time = p.time_out - p.arrival_time;
        // recalculate wait time based on your formula
        p.waiting_time = p.turnaround_time - p.original_burst_time; 
        ready.remove(p);
        finished.add(p);
    }

    private AG_Process getBestPriority(List<AG_Process> ready) {
        AG_Process best = ready.get(0);
        for(AG_Process p : ready) {
            if(p.priority < best.priority) best = p;
        }
        return best;
    }

    private AG_Process getShortestJob(List<AG_Process> ready) {
        AG_Process best = ready.get(0);
        for(AG_Process p : ready) {
            if(p.burst_time < best.burst_time) best = p;
        }
        return best;
    }
}

// ================== MAINF CLASS ==================
public class scheduling {
    public static void main(String[] args) {
        // Setup inputs
        int rrQuantum = 4;
        int agQuantum = 4;
        int contextSwitch = 0; // Set to 0 for standard AG

        List<Process> procs = new ArrayList<>();
        // Name, Arrival, Burst, Priority
        procs.add(new Process("P1", 0, 17, 4));
        procs.add(new Process("P2", 3, 6, 9));
        procs.add(new Process("P3", 4, 10, 3));
        procs.add(new Process("P4", 29, 4, 8));

        // 1. SJF
        System.out.println("\n=========== 1. SRJF (Shortest Remaining Job First) =========");
        // Pass a COPY of list because scheduling modifies internal states
        Schedule sjf = new SJF_Schedule(copyList(procs), contextSwitch);
        sjf.execute();
        sjf.printExecutionOrder();
        sjf.printProcessStats();
        sjf.printAverages();

        // 2. Round Robin
        System.out.println("\n=========== 2. Round Robin =========");
        Schedule rr = new RR_Schedule(copyList(procs), contextSwitch, rrQuantum);
        rr.execute();
        rr.printExecutionOrder();
        rr.printProcessStats();
        rr.printAverages();

        // 3. AG Scheduling
        System.out.println("\n=========== 3. AG Scheduling =========");
        Schedule ag = new AG_Schedule(copyList(procs), agQuantum);
        ag.execute();
        ag.printExecutionOrder();
        ag.printProcessStats();
        ag.printAverages();
    }

    // Helper to refresh data for each run
    public static List<Process> copyList(List<Process> original) {
        List<Process> copy = new ArrayList<>();
        for(Process p : original) {
            copy.add(new Process(p.name, p.arrival_time, p.burst_time, p.priority));
        }
        return copy;
    }
}