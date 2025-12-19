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
        return turnaround_time;
    }

    public void set_time_in(int time_in) {
        this.time_in = time_in;
    }

    public void set_time_out(int time_out) {
        this.time_out = time_out;
    }

    // Decrement burst time (helper for simulation)
    public void reduce_burst(int amount) {
        this.burst_time -= amount;
        if (this.burst_time < 0)
            this.burst_time = 0;
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
        if (processes.isEmpty() || processes == null)
            throw new IllegalArgumentException("No processes to schedule");
    }

    protected abstract void runSchedule();

    // Default metric calculation (can be overridden)
    protected void calculateMetrics() {
        for (Process p : processes) {
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

    public List<String> getExecutionOrder() {
        return new ArrayList<>(executionOrder);
    }

    public List<Process> getProcesses() {
        return processes;
    }

    public void printProcessStats() {
        System.out.println("Name\tWaiting\tTurnaround");
        // Sort by name for cleaner output
        processes.sort(Comparator.comparing(Process::get_name));
        for (Process p : processes)
            System.out.println(p.get_name() + "\t" + p.get_waiting_time() + "\t" + p.get_turnaround_time());
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

    public int get_RemainingTime() {
        return remainingTime;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public void executeOneUnit() {
        remainingTime--;
    }
}

class SJF_Schedule extends Schedule {
    private List<SJF_process> sjf_processes;
    private PriorityQueue<SJF_process> readyQ;

    public SJF_Schedule(List<Process> processes, int contextSwitchTime) {
        super(processes, contextSwitchTime);
        this.sjf_processes = new ArrayList<>();
        // Convert generic processes to SJF processes
        for (Process p : processes)
            sjf_processes.add(new SJF_process(p));

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

        while (completed < sjf_processes.size()) {
            // Add arrivals
            while (!incoming.isEmpty() && incoming.get(0).arrival_time <= currTime) {
                readyQ.add(incoming.remove(0));
            }

            // Check if current process is done
            if (current_process != null && current_process.get_RemainingTime() == 0) {
                completed++;
                current_process.set_time_out(currTime);
                current_process = null;
            }

            // Context switch / Preemption logic
            if (!readyQ.isEmpty()) {
                SJF_process next = readyQ.peek();
                if (current_process == null || next.get_RemainingTime() < current_process.get_RemainingTime()) {
                    // Context Switch penalty would go here
                    if (current_process != null && current_process.get_RemainingTime() > 0) {
                        readyQ.add(current_process);
                    }
                    current_process = readyQ.poll();
                    executionOrder.add(current_process.get_name());
                }
            }

            if (current_process != null) {
                current_process.executeOneUnit();
                currTime++;
            } else {
                // Time skip if idle
                if (!incoming.isEmpty())
                    currTime = incoming.get(0).arrival_time;
                else
                    currTime++;
            }
        }

        // Map results back to original list for reporting
        for (int i = 0; i < processes.size(); i++) {
            Process orig = processes.get(i);
            // Find matching SJF process (simplified by assuming order/name match)
            for (SJF_process sjf : sjf_processes) {
                if (sjf.name.equals(orig.name)) {
                    orig.time_out = sjf.time_out;
                    break;
                }
            }
        }
    }
}

// ================== 2. ROUND ROBIN (Adapted from Myscheduling)
// ==================
class RR_Schedule extends Schedule {
    private int quantum;

    public RR_Schedule(List<Process> processes, int contextSwitchTime, int quantum) {
        super(deepCopy(processes), contextSwitchTime); // Work on copy to not ruin original data
        this.quantum = quantum;
    }

    // Helper to deep copy list so we can modify burst times safely
    private static List<Process> deepCopy(List<Process> list) {
        List<Process> copy = new ArrayList<>();
        for (Process p : list)
            copy.add(new Process(p.name, p.arrival_time, p.burst_time, p.priority));
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

        while (completed < processes.size()) {
            // Add newly arrived processes
            while (!pending.isEmpty() && pending.get(0).arrival_time <= currentTime) {
                readyQueue.add(pending.remove(0));
            }

            if (readyQueue.isEmpty()) {
                if (!pending.isEmpty())
                    currentTime = pending.get(0).arrival_time;
                else
                    currentTime++;
                continue;
            }

            Process p = readyQueue.poll();

            // Handle Context Switch
            if (lastProcess != null && lastProcess != p && contextSwitchTime > 0) {
                currentTime += contextSwitchTime;
                // Check arrivals during CS
                while (!pending.isEmpty() && pending.get(0).arrival_time <= currentTime) {
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
            while (!pending.isEmpty() && pending.get(0).arrival_time <= currentTime) {
                readyQueue.add(pending.remove(0));
            }

            if (p.burst_time > 0) {
                readyQueue.add(p);
            } else {
                p.set_time_out(currentTime);
                completed++;
            }
        }
    }
}

// ================== 3. AG SCHEDULING ==================

// Specialized Process for AG
class AG_Process extends Process {
    int quantum;
    List<Integer> quantum_history = new ArrayList<>();

    public AG_Process(Process p, int initQuantum) {
        super(p.name, p.arrival_time, p.burst_time, p.priority);
        this.quantum = initQuantum;
        // this.quantum_history.add(initQuantum);
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
        for (Process p : processes) {
            ag_processes.add(new AG_Process(p, initQuantum));
        }
    }

    // Overload that accepts per-process initial quanta; falls back to default when
    // missing
    public AG_Schedule(List<Process> processes, Map<String, Integer> initialQuanta, int defaultQuantum) {
        super(processes, 0);
        this.initQuantum = defaultQuantum;
        this.ag_processes = new ArrayList<>();
        for (Process p : processes) {
            int q = initialQuanta.getOrDefault(p.name, defaultQuantum);
            ag_processes.add(new AG_Process(p, q));
        }
    }

    enum State {
        FCFS, PRIORITY, SJF
    }

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
                        state = State.FCFS; // can be optimized
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
                        time_executed = 0; // Reset execution counter

                    } else {
                        // Continue current process until 50%
                        int cycle_limit = currentTime + (int) Math.ceil(curr_p.quantum * 0.25); // Run another 25%

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

                        // premptive SJF
                        currentTime++;
                        time_executed++;
                        curr_p.reduce_burst(1);

                        if (curr_p.burst_time == 0) {
                            handleFinish(curr_p, currentTime, readyQueue, finished);
                            state = State.FCFS;
                        } else if (currentTime >= quantum_end_time) {
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
        for (Process orig : processes) {
            for (AG_Process ag : finished) {
                if (orig.name.equals(ag.name)) {
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
        for (AG_Process p : ready) {
            if (p.priority < best.priority)
                best = p;
        }
        return best;
    }

    private AG_Process getShortestJob(List<AG_Process> ready) {
        AG_Process best = ready.get(0);
        for (AG_Process p : ready) {
            if (p.burst_time < best.burst_time)
                best = p;
        }
        return best;
    }

    public Map<String, List<Integer>> getQuantumHistories() {
        Map<String, List<Integer>> histories = new HashMap<>();
        for (AG_Process p : ag_processes) {
            histories.put(p.get_name(), new ArrayList<>(p.quantum_history));
        }
        return histories;
    }
}

// ============================================== PREEMPTIVE PRIORITY WITH AGING
// ============================================ //

class PriorityProcess extends Process {
    int remainingTime;
    int originalPriority;
    int waitingSince; // time when process entered ready queue
    int ageCount;

    PriorityProcess(String name, int arrival, int burst, int priority) {
        super(name, arrival, burst, priority);
        this.remainingTime = burst;
        this.originalPriority = priority;
        this.waitingSince = arrival;
        this.ageCount = 0;
    }

    void age(int currentTime) {
        if (priority > 1) {
            priority--;
            ageCount++;
        }
        waitingSince = currentTime;
    }
}

class PriorityWithAgingSchedule extends Schedule {

    private List<PriorityProcess> all;
    private List<PriorityProcess> finished;
    private PriorityQueue<PriorityProcess> readyQueue;
    private PriorityProcess running;
    private int time;
    private int arrivalIndex;
    private int agingInterval;

    PriorityWithAgingSchedule(List<Process> processes, int contextSwitch, int agingInterval) {
        super(processes, contextSwitch);
        this.agingInterval = agingInterval;
        this.time = 0;
        this.arrivalIndex = 0;
        this.finished = new ArrayList<>();

        all = new ArrayList<>();
        for (Process p : processes) {
            all.add(new PriorityProcess(
                    p.get_name(),
                    p.get_arrival_time(),
                    p.get_burst_time(),
                    p.get_priority()));
        }

        all.sort(Comparator.comparingInt(Process::get_arrival_time));

        readyQueue = new PriorityQueue<>(
                Comparator.comparingInt(Process::get_priority)
                        .thenComparingInt(Process::get_arrival_time));
    }

    private void applyAging() {
        if (readyQueue.isEmpty())
            return;

        List<PriorityProcess> temp = new ArrayList<>();
        while (!readyQueue.isEmpty()) {
            PriorityProcess p = readyQueue.poll();
            if (time - p.waitingSince >= agingInterval) {
                p.age(time);
            }
            temp.add(p);
        }
        readyQueue.addAll(temp);
    }

    private void handleContextSwitch() {
        for (int i = 0; i < contextSwitchTime; i++) {
            time++;

            // لو حاجه معاد وصولها في الوقت ده
            while (arrivalIndex < all.size() && all.get(arrivalIndex).get_arrival_time() <= time) {
                PriorityProcess p = all.get(arrivalIndex++);
                p.waitingSince = time;
                readyQueue.add(p);
            }

            applyAging();
        }
    }

    @Override
    protected void calculateMetrics() {
    }

    private void updateOriginalProcesses() {
        for (Process p : processes) {
            for (PriorityProcess done : finished) {
                if (p.get_name().equals(done.get_name())) {
                    p.waiting_time = done.waiting_time;
                    p.turnaround_time = done.turnaround_time;
                    p.time_out = done.time_out;
                }
            }
        }
    }

    @Override
    protected void runSchedule() {
        while (finished.size() < all.size()) {

            // arrivals: checks for any newly arriving processes
            // -->in other words, processes that should arrive now or have arrived and are
            // not yet taken
            while (arrivalIndex < all.size() && all.get(arrivalIndex).get_arrival_time() <= time) {
                PriorityProcess p = all.get(arrivalIndex++);
                p.waitingSince = time;
                readyQueue.add(p);
            }

            // aging
            applyAging();

            // preemption
            if (running != null && !readyQueue.isEmpty()) {
                PriorityProcess top = readyQueue.peek();
                if (top.get_priority() < running.get_priority()) {
                    running.waitingSince = time;
                    readyQueue.add(running);
                    running = null;
                    handleContextSwitch();
                }
            }

            // dispatch
            if (running == null && !readyQueue.isEmpty()) {
                running = readyQueue.poll();
                if (executionOrder.isEmpty() || !executionOrder.get(executionOrder.size() - 1)
                        .equals(running.get_name())) {
                    executionOrder.add(running.get_name());
                }
            }

            // execute
            if (running != null) {
                running.remainingTime--;
                if (running.remainingTime == 0) {
                    running.time_out = time + 1;
                    running.turnaround_time = running.time_out - running.arrival_time;
                    running.waiting_time = running.turnaround_time - running.burst_time;
                    finished.add(running);
                    running = null;
                    handleContextSwitch();
                }
            }

            time++;
        }

        updateOriginalProcesses();
    }

}

// ================== MAINF CLASS ==================
public class scheduling {
    public static void main(String[] args) {
        runAllHardcodedTests();
    }

    // Helper to refresh data for each run
    public static List<Process> copyList(List<Process> original) {
        List<Process> copy = new ArrayList<>();
        for (Process p : original) {
            copy.add(new Process(p.name, p.arrival_time, p.burst_time, p.priority));
        }
        return copy;
    }

    // Encapsulates one AG test case (process list + per-process quantum map)
    private static class AgTestCase {
        final String name;
        final List<Process> processes;
        final Map<String, Integer> initialQuanta;

        final ExpectedResult expected;

        AgTestCase(String name, List<Process> processes, Map<String, Integer> initialQuanta,
                ExpectedResult expected) {
            this.name = name;
            this.processes = processes;
            this.initialQuanta = initialQuanta;
            this.expected = expected;
        }
    }

    private static class ExpectedProcessResult {
        final String name;
        final int waitingTime;
        final int turnaroundTime;
        final List<Integer> quantumHistory;

        ExpectedProcessResult(String name, int waitingTime, int turnaroundTime) {
            this(name, waitingTime, turnaroundTime, null);
        }

        ExpectedProcessResult(String name, int waitingTime, int turnaroundTime, List<Integer> quantumHistory) {
            this.name = name;
            this.waitingTime = waitingTime;
            this.turnaroundTime = turnaroundTime;
            this.quantumHistory = quantumHistory == null ? null : new ArrayList<>(quantumHistory);
        }
    }

    private static class ExpectedResult {
        final List<String> executionOrder;
        final List<ExpectedProcessResult> processResults;
        final double averageWaitingTime;
        final double averageTurnaroundTime;

        ExpectedResult(List<String> executionOrder, List<ExpectedProcessResult> processResults,
                double averageWaitingTime, double averageTurnaroundTime) {
            this.executionOrder = new ArrayList<>(executionOrder);
            this.processResults = new ArrayList<>(processResults);
            this.averageWaitingTime = averageWaitingTime;
            this.averageTurnaroundTime = averageTurnaroundTime;
        }
    }

    private static void addAgProcess(List<Process> processes, Map<String, Integer> initialQuanta,
            String name, int arrival, int burst, int priority, int quantum) {
        processes.add(new Process(name, arrival, burst, priority));
        initialQuanta.put(name, quantum);
    }

    private static List<AgTestCase> buildAgTests() {
        List<AgTestCase> tests = new ArrayList<>();

        // Test 1
        {
            List<Process> procs = new ArrayList<>();
            Map<String, Integer> quanta = new HashMap<>();
                addAgProcess(procs, quanta, "P1", 0, 17, 4, 7);
                addAgProcess(procs, quanta, "P2", 2, 6, 7, 9);
                addAgProcess(procs, quanta, "P3", 5, 11, 3, 4);
                addAgProcess(procs, quanta, "P4", 15, 4, 6, 6);

                ExpectedResult expected = new ExpectedResult(
                    Arrays.asList("P1", "P2", "P3", "P2", "P1", "P3", "P4", "P3", "P1", "P4"),
                    Arrays.asList(
                        new ExpectedProcessResult("P1", 19, 36, Arrays.asList(7, 10, 14, 0)),
                        new ExpectedProcessResult("P2", 4, 10, Arrays.asList(9, 12, 0)),
                        new ExpectedProcessResult("P3", 10, 21, Arrays.asList(4, 6, 8, 0)),
                        new ExpectedProcessResult("P4", 19, 23, Arrays.asList(6, 8, 0))),
                    13.0, 22.5);

                tests.add(new AgTestCase("AG_test1", procs, quanta, expected));
        }

        // Test 2
        {
            List<Process> procs = new ArrayList<>();
            Map<String, Integer> quanta = new HashMap<>();
            addAgProcess(procs, quanta, "P1", 0, 10, 3, 4);
            addAgProcess(procs, quanta, "P2", 0, 8, 1, 5);
            addAgProcess(procs, quanta, "P3", 0, 12, 2, 6);
            addAgProcess(procs, quanta, "P4", 0, 6, 4, 3);
            addAgProcess(procs, quanta, "P5", 0, 9, 5, 4);

                ExpectedResult expected = new ExpectedResult(
                    Arrays.asList("P1", "P2", "P3", "P2", "P4", "P3", "P4", "P3", "P5", "P1", "P4",
                        "P1", "P5", "P4", "P5"),
                    Arrays.asList(
                        new ExpectedProcessResult("P1", 25, 35, Arrays.asList(4, 6, 8, 0)),
                        new ExpectedProcessResult("P2", 3, 11, Arrays.asList(5, 7, 0)),
                        new ExpectedProcessResult("P3", 11, 23, Arrays.asList(6, 8, 12, 0)),
                        new ExpectedProcessResult("P4", 33, 39, Arrays.asList(3, 4, 6, 8, 0)),
                        new ExpectedProcessResult("P5", 36, 45, Arrays.asList(4, 6, 8, 0))),
                    21.6, 30.6);

                tests.add(new AgTestCase("AG_test2", procs, quanta, expected));
        }

        // Test 3
        {
            List<Process> procs = new ArrayList<>();
            Map<String, Integer> quanta = new HashMap<>();
            addAgProcess(procs, quanta, "P1", 0, 20, 5, 8);
            addAgProcess(procs, quanta, "P2", 3, 4, 3, 6);
            addAgProcess(procs, quanta, "P3", 6, 3, 4, 5);
            addAgProcess(procs, quanta, "P4", 10, 2, 2, 4);
            addAgProcess(procs, quanta, "P5", 15, 5, 6, 7);
            addAgProcess(procs, quanta, "P6", 20, 6, 1, 3);

                ExpectedResult expected = new ExpectedResult(
                    Arrays.asList("P1", "P2", "P1", "P4", "P3", "P1", "P6", "P5", "P6", "P1", "P5"),
                    Arrays.asList(
                        new ExpectedProcessResult("P1", 17, 37, Arrays.asList(8, 12, 17, 23, 0)),
                        new ExpectedProcessResult("P2", 1, 5, Arrays.asList(6, 0)),
                        new ExpectedProcessResult("P3", 7, 10, Arrays.asList(5, 0)),
                        new ExpectedProcessResult("P4", 1, 3, Arrays.asList(4, 0)),
                        new ExpectedProcessResult("P5", 20, 25, Arrays.asList(7, 10, 0)),
                        new ExpectedProcessResult("P6", 3, 9, Arrays.asList(3, 5, 0))),
                    8.17, 14.83);

                tests.add(new AgTestCase("AG_test3", procs, quanta, expected));
        }

        // Test 4
        {
            List<Process> procs = new ArrayList<>();
            Map<String, Integer> quanta = new HashMap<>();
            addAgProcess(procs, quanta, "P1", 0, 3, 2, 10);
            addAgProcess(procs, quanta, "P2", 2, 4, 3, 12);
            addAgProcess(procs, quanta, "P3", 5, 2, 1, 8);
            addAgProcess(procs, quanta, "P4", 8, 5, 4, 15);
            addAgProcess(procs, quanta, "P5", 12, 3, 5, 9);

                ExpectedResult expected = new ExpectedResult(
                    Arrays.asList("P1", "P2", "P3", "P2", "P4", "P5"),
                    Arrays.asList(
                        new ExpectedProcessResult("P1", 0, 3, Arrays.asList(10, 0)),
                        new ExpectedProcessResult("P2", 3, 7, Arrays.asList(12, 17, 0)),
                        new ExpectedProcessResult("P3", 1, 3, Arrays.asList(8, 0)),
                        new ExpectedProcessResult("P4", 1, 6, Arrays.asList(15, 0)),
                        new ExpectedProcessResult("P5", 2, 5, Arrays.asList(9, 0))),
                    1.4, 4.8);

                tests.add(new AgTestCase("AG_test4", procs, quanta, expected));
        }

        // Test 5
        {
            List<Process> procs = new ArrayList<>();
            Map<String, Integer> quanta = new HashMap<>();
            addAgProcess(procs, quanta, "P1", 0, 25, 3, 5);
            addAgProcess(procs, quanta, "P2", 1, 18, 2, 4);
            addAgProcess(procs, quanta, "P3", 3, 22, 4, 6);
            addAgProcess(procs, quanta, "P4", 5, 15, 1, 3);
            addAgProcess(procs, quanta, "P5", 8, 20, 5, 7);
            addAgProcess(procs, quanta, "P6", 12, 12, 6, 4);

                ExpectedResult expected = new ExpectedResult(
                    Arrays.asList("P1", "P2", "P1", "P4", "P3", "P4", "P2", "P4", "P5", "P2", "P1",
                        "P2", "P6", "P1", "P3", "P1", "P5", "P3", "P6", "P3", "P5", "P6", "P5",
                        "P6"),
                    Arrays.asList(
                        new ExpectedProcessResult("P1", 40, 65, Arrays.asList(5, 7, 10, 14, 16, 0)),
                        new ExpectedProcessResult("P2", 25, 43, Arrays.asList(4, 6, 8, 10, 0)),
                        new ExpectedProcessResult("P3", 63, 85, Arrays.asList(6, 8, 11, 16, 0)),
                        new ExpectedProcessResult("P4", 7, 22, Arrays.asList(3, 5, 7, 0)),
                        new ExpectedProcessResult("P5", 77, 97, Arrays.asList(7, 10, 14, 16, 0)),
                        new ExpectedProcessResult("P6", 88, 100, Arrays.asList(4, 6, 8, 11, 0))),
                    50.0, 68.67);

                tests.add(new AgTestCase("AG_test5", procs, quanta, expected));
        }

        // Test 6
        {
            List<Process> procs = new ArrayList<>();
            Map<String, Integer> quanta = new HashMap<>();
            addAgProcess(procs, quanta, "P1", 0, 14, 4, 6);
            addAgProcess(procs, quanta, "P2", 4, 9, 2, 8);
            addAgProcess(procs, quanta, "P3", 7, 16, 5, 5);
            addAgProcess(procs, quanta, "P4", 10, 7, 1, 10);
            addAgProcess(procs, quanta, "P5", 15, 11, 3, 4);
            addAgProcess(procs, quanta, "P6", 20, 5, 6, 7);
            addAgProcess(procs, quanta, "P7", 25, 8, 7, 9);

                ExpectedResult expected = new ExpectedResult(
                    Arrays.asList("P1", "P2", "P1", "P4", "P3", "P2", "P1", "P5", "P6", "P5", "P6",
                        "P3", "P5", "P7", "P1", "P3", "P7", "P3", "P7"),
                    Arrays.asList(
                        new ExpectedProcessResult("P1", 39, 53, Arrays.asList(6, 8, 11, 15, 0)),
                        new ExpectedProcessResult("P2", 11, 20, Arrays.asList(8, 10, 0)),
                        new ExpectedProcessResult("P3", 45, 61, Arrays.asList(5, 7, 10, 14, 0)),
                        new ExpectedProcessResult("P4", 4, 11, Arrays.asList(10, 0)),
                        new ExpectedProcessResult("P5", 19, 30, Arrays.asList(4, 6, 8, 0)),
                        new ExpectedProcessResult("P6", 13, 18, Arrays.asList(7, 10, 0)),
                        new ExpectedProcessResult("P7", 37, 45, Arrays.asList(9, 12, 17, 0))),
                    24.0, 34.0);

                tests.add(new AgTestCase("AG_test6", procs, quanta, expected));
        }

        return tests;
    }

            private static class GeneralTestCase {
            final String name;
            final int contextSwitch;
            final int rrQuantum;
            final int agingInterval;
            final List<Process> processes;
            final ExpectedResult sjfExpected;
            final ExpectedResult rrExpected;
            final ExpectedResult priorityExpected;

            GeneralTestCase(String name, int contextSwitch, int rrQuantum, int agingInterval, List<Process> processes,
                ExpectedResult sjfExpected, ExpectedResult rrExpected, ExpectedResult priorityExpected) {
                this.name = name;
                this.contextSwitch = contextSwitch;
                this.rrQuantum = rrQuantum;
                this.agingInterval = agingInterval;
                this.processes = processes;
                this.sjfExpected = sjfExpected;
                this.rrExpected = rrExpected;
                this.priorityExpected = priorityExpected;
            }
            }

            private static GeneralTestCase buildGeneralTest1() {
            List<Process> procs = new ArrayList<>();
            procs.add(new Process("P1", 0, 8, 3));
            procs.add(new Process("P2", 1, 4, 1));
            procs.add(new Process("P3", 2, 2, 4));
            procs.add(new Process("P4", 3, 1, 2));
            procs.add(new Process("P5", 4, 3, 5));

            ExpectedResult sjf = new ExpectedResult(
                Arrays.asList("P1", "P2", "P4", "P3", "P2", "P5", "P1"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 16, 24),
                    new ExpectedProcessResult("P2", 7, 11),
                    new ExpectedProcessResult("P3", 4, 6),
                    new ExpectedProcessResult("P4", 1, 2),
                    new ExpectedProcessResult("P5", 9, 12)),
                7.4, 11.0);

            ExpectedResult rr = new ExpectedResult(
                Arrays.asList("P1", "P2", "P3", "P1", "P4", "P5", "P2", "P1", "P5", "P1"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 19, 27),
                    new ExpectedProcessResult("P2", 14, 18),
                    new ExpectedProcessResult("P3", 4, 6),
                    new ExpectedProcessResult("P4", 9, 10),
                    new ExpectedProcessResult("P5", 17, 20)),
                12.6, 16.2);

            ExpectedResult priority = new ExpectedResult(
                Arrays.asList("P1", "P2", "P1", "P4", "P1", "P3", "P1", "P5", "P1"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 18, 26),
                    new ExpectedProcessResult("P2", 1, 5),
                    new ExpectedProcessResult("P3", 12, 14),
                    new ExpectedProcessResult("P4", 6, 7),
                    new ExpectedProcessResult("P5", 16, 19)),
                10.6, 14.2);

            return new GeneralTestCase("Test_1", 1, 2, 5, procs, sjf, rr, priority);
            }

            private static GeneralTestCase buildGeneralTest2() {
            List<Process> procs = new ArrayList<>();
            procs.add(new Process("P1", 0, 6, 3));
            procs.add(new Process("P2", 0, 3, 1));
            procs.add(new Process("P3", 0, 8, 2));
            procs.add(new Process("P4", 0, 4, 4));
            procs.add(new Process("P5", 0, 2, 5));

            ExpectedResult sjf = new ExpectedResult(
                Arrays.asList("P5", "P2", "P4", "P1", "P3"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 12, 18),
                    new ExpectedProcessResult("P2", 3, 6),
                    new ExpectedProcessResult("P3", 19, 27),
                    new ExpectedProcessResult("P4", 7, 11),
                    new ExpectedProcessResult("P5", 0, 2)),
                8.2, 12.8);

            ExpectedResult rr = new ExpectedResult(
                Arrays.asList("P1", "P2", "P3", "P4", "P5", "P1", "P3", "P4", "P3"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 16, 22),
                    new ExpectedProcessResult("P2", 4, 7),
                    new ExpectedProcessResult("P3", 23, 31),
                    new ExpectedProcessResult("P4", 24, 28),
                    new ExpectedProcessResult("P5", 16, 18)),
                16.6, 21.2);

            ExpectedResult priority = new ExpectedResult(
                Arrays.asList("P2", "P3", "P1", "P4", "P3", "P5", "P4"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 11, 17),
                    new ExpectedProcessResult("P2", 0, 3),
                    new ExpectedProcessResult("P3", 15, 23),
                    new ExpectedProcessResult("P4", 25, 29),
                    new ExpectedProcessResult("P5", 24, 26)),
                15.0, 19.6);

            return new GeneralTestCase("Test_2", 1, 3, 5, procs, sjf, rr, priority);
            }

            private static GeneralTestCase buildGeneralTest3() {
            List<Process> procs = new ArrayList<>();
            procs.add(new Process("P1", 0, 10, 5));
            procs.add(new Process("P2", 2, 5, 1));
            procs.add(new Process("P3", 5, 3, 2));
            procs.add(new Process("P4", 8, 7, 1));
            procs.add(new Process("P5", 10, 2, 3));

            ExpectedResult sjf = new ExpectedResult(
                Arrays.asList("P1", "P2", "P3", "P5", "P4", "P1"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 22, 32),
                    new ExpectedProcessResult("P2", 1, 6),
                    new ExpectedProcessResult("P3", 4, 7),
                    new ExpectedProcessResult("P4", 8, 15),
                    new ExpectedProcessResult("P5", 3, 5)),
                7.6, 13.0);

            ExpectedResult rr = new ExpectedResult(
                Arrays.asList("P1", "P2", "P1", "P3", "P4", "P2", "P5", "P1", "P4"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 21, 31),
                    new ExpectedProcessResult("P2", 18, 23),
                    new ExpectedProcessResult("P3", 10, 13),
                    new ExpectedProcessResult("P4", 20, 27),
                    new ExpectedProcessResult("P5", 16, 18)),
                17.0, 22.4);

            ExpectedResult priority = new ExpectedResult(
                Arrays.asList("P1", "P2", "P4", "P3", "P4", "P1", "P5", "P1"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 24, 34),
                    new ExpectedProcessResult("P2", 1, 6),
                    new ExpectedProcessResult("P3", 9, 12),
                    new ExpectedProcessResult("P4", 6, 13),
                    new ExpectedProcessResult("P5", 13, 15)),
                10.6, 16.0);

            return new GeneralTestCase("Test_3", 1, 4, 4, procs, sjf, rr, priority);
            }

            private static GeneralTestCase buildGeneralTest4() {
            List<Process> procs = new ArrayList<>();
            procs.add(new Process("P1", 0, 12, 2));
            procs.add(new Process("P2", 4, 9, 3));
            procs.add(new Process("P3", 8, 15, 1));
            procs.add(new Process("P4", 12, 6, 4));
            procs.add(new Process("P5", 16, 11, 2));
            procs.add(new Process("P6", 20, 5, 5));

            ExpectedResult sjf = new ExpectedResult(
                Arrays.asList("P1", "P4", "P6", "P2", "P5", "P3"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 0, 12),
                    new ExpectedProcessResult("P2", 25, 34),
                    new ExpectedProcessResult("P3", 45, 60),
                    new ExpectedProcessResult("P4", 2, 8),
                    new ExpectedProcessResult("P5", 24, 35),
                    new ExpectedProcessResult("P6", 2, 7)),
                16.33, 26.0);

            ExpectedResult rr = new ExpectedResult(
                Arrays.asList("P1", "P2", "P1", "P3", "P4", "P2", "P5", "P1", "P6", "P3", "P4",
                    "P5", "P3", "P5"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 38, 50),
                    new ExpectedProcessResult("P2", 26, 35),
                    new ExpectedProcessResult("P3", 58, 73),
                    new ExpectedProcessResult("P4", 49, 55),
                    new ExpectedProcessResult("P5", 57, 68),
                    new ExpectedProcessResult("P6", 32, 37)),
                43.33, 53.0);

            ExpectedResult priority = new ExpectedResult(
                Arrays.asList("P1", "P3", "P1", "P2", "P3", "P5", "P4", "P2", "P6", "P5", "P2"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 14, 26),
                    new ExpectedProcessResult("P2", 65, 74),
                    new ExpectedProcessResult("P3", 16, 31),
                    new ExpectedProcessResult("P4", 38, 44),
                    new ExpectedProcessResult("P5", 48, 59),
                    new ExpectedProcessResult("P6", 44, 49)),
                37.5, 47.17);

            return new GeneralTestCase("Test_4", 2, 5, 6, procs, sjf, rr, priority);
            }

            private static GeneralTestCase buildGeneralTest5() {
            List<Process> procs = new ArrayList<>();
            procs.add(new Process("P1", 0, 3, 3));
            procs.add(new Process("P2", 1, 2, 1));
            procs.add(new Process("P3", 2, 4, 2));
            procs.add(new Process("P4", 3, 1, 4));
            procs.add(new Process("P5", 4, 3, 5));

            ExpectedResult sjf = new ExpectedResult(
                Arrays.asList("P1", "P4", "P2", "P5", "P3"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 0, 3),
                    new ExpectedProcessResult("P2", 5, 7),
                    new ExpectedProcessResult("P3", 11, 15),
                    new ExpectedProcessResult("P4", 1, 2),
                    new ExpectedProcessResult("P5", 5, 8)),
                4.4, 7.0);

            ExpectedResult rr = new ExpectedResult(
                Arrays.asList("P1", "P2", "P3", "P1", "P4", "P5", "P3", "P5"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 7, 10),
                    new ExpectedProcessResult("P2", 2, 4),
                    new ExpectedProcessResult("P3", 12, 16),
                    new ExpectedProcessResult("P4", 8, 9),
                    new ExpectedProcessResult("P5", 13, 16)),
                8.4, 11.0);

            ExpectedResult priority = new ExpectedResult(
                Arrays.asList("P1", "P2", "P1", "P3", "P1", "P4", "P5", "P1"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 17, 20),
                    new ExpectedProcessResult("P2", 1, 3),
                    new ExpectedProcessResult("P3", 5, 9),
                    new ExpectedProcessResult("P4", 10, 11),
                    new ExpectedProcessResult("P5", 11, 14)),
                8.8, 11.4);

            return new GeneralTestCase("Test_5", 1, 2, 3, procs, sjf, rr, priority);
            }

            private static GeneralTestCase buildGeneralTest6() {
            List<Process> procs = new ArrayList<>();
            procs.add(new Process("P1", 0, 14, 4));
            procs.add(new Process("P2", 3, 7, 2));
            procs.add(new Process("P3", 6, 10, 5));
            procs.add(new Process("P4", 9, 5, 1));
            procs.add(new Process("P5", 12, 8, 3));
            procs.add(new Process("P6", 15, 4, 6));

            ExpectedResult sjf = new ExpectedResult(
                Arrays.asList("P1", "P2", "P4", "P6", "P5", "P3", "P1"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 40, 54),
                    new ExpectedProcessResult("P2", 1, 8),
                    new ExpectedProcessResult("P3", 26, 36),
                    new ExpectedProcessResult("P4", 3, 8),
                    new ExpectedProcessResult("P5", 11, 19),
                    new ExpectedProcessResult("P6", 3, 7)),
                14.0, 22.0);

            ExpectedResult rr = new ExpectedResult(
                Arrays.asList("P1", "P2", "P1", "P3", "P4", "P2", "P5", "P1", "P6", "P3", "P4",
                    "P5", "P1", "P3"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 44, 58),
                    new ExpectedProcessResult("P2", 18, 25),
                    new ExpectedProcessResult("P3", 45, 55),
                    new ExpectedProcessResult("P4", 36, 41),
                    new ExpectedProcessResult("P5", 35, 43),
                    new ExpectedProcessResult("P6", 24, 28)),
                33.67, 41.67);

            ExpectedResult priority = new ExpectedResult(
                Arrays.asList("P1", "P2", "P4", "P2", "P1", "P5", "P3", "P1", "P6", "P1"),
                Arrays.asList(
                    new ExpectedProcessResult("P1", 43, 57),
                    new ExpectedProcessResult("P2", 8, 15),
                    new ExpectedProcessResult("P3", 31, 41),
                    new ExpectedProcessResult("P4", 1, 6),
                    new ExpectedProcessResult("P5", 16, 24),
                    new ExpectedProcessResult("P6", 36, 40)),
                22.5, 30.5);

            return new GeneralTestCase("Test_6", 1, 4, 5, procs, sjf, rr, priority);
            }

            private static void runAgTestSuiteWithAssertions() {
            List<AgTestCase> tests = buildAgTests();
            int defaultQuantum = 4;

            for (AgTestCase tc : tests) {
                System.out.println("\n=========== Running " + tc.name + " =========");
                AG_Schedule ag = new AG_Schedule(copyList(tc.processes), tc.initialQuanta, defaultQuantum);
                ag.execute();
                verifyAgResult(tc, ag);
            }
            }

            private static void runGeneralTestSuite() {
            List<GeneralTestCase> tests = Arrays.asList(
                buildGeneralTest1(),
                buildGeneralTest2(),
                buildGeneralTest3(),
                buildGeneralTest4(),
                buildGeneralTest5(),
                buildGeneralTest6());

            for (GeneralTestCase tc : tests) {
                System.out.println("\n=========== Running " + tc.name + " =========");

                SJF_Schedule sjf = new SJF_Schedule(copyList(tc.processes), tc.contextSwitch);
                sjf.execute();
                verifyGeneralResult("SJF", tc.name, sjf, tc.sjfExpected, null);

                RR_Schedule rr = new RR_Schedule(copyList(tc.processes), tc.contextSwitch, tc.rrQuantum);
                rr.execute();
                verifyGeneralResult("RR", tc.name, rr, tc.rrExpected, null);

                PriorityWithAgingSchedule priority = new PriorityWithAgingSchedule(copyList(tc.processes),
                    tc.contextSwitch, tc.agingInterval);
                priority.execute();
                verifyGeneralResult("Priority", tc.name, priority, tc.priorityExpected, null);
            }
            }

            private static void runAllHardcodedTests() {
            runAgTestSuiteWithAssertions();
            runGeneralTestSuite();
            }

            private static void verifyAgResult(AgTestCase tc, AG_Schedule schedule) {
            ExpectedResult expected = tc.expected;
            boolean pass = true;

            pass &= compareLists("Execution order", tc.name, expected.executionOrder, schedule.getExecutionOrder());

            Map<String, Process> actualProcesses = mapByName(schedule.getProcesses());
            Map<String, List<Integer>> quantumHistories = schedule.getQuantumHistories();

            for (ExpectedProcessResult e : expected.processResults) {
                Process actual = actualProcesses.get(e.name);
                if (actual == null) {
                System.out.println(tc.name + " missing process " + e.name);
                pass = false;
                continue;
                }

                if (actual.get_waiting_time() != e.waitingTime) {
                System.out.println(tc.name + " (" + e.name + ") waiting mismatch: expected " + e.waitingTime
                    + " got " + actual.get_waiting_time());
                pass = false;
                }

                if (actual.get_turnaround_time() != e.turnaroundTime) {
                System.out.println(tc.name + " (" + e.name + ") turnaround mismatch: expected "
                    + e.turnaroundTime + " got " + actual.get_turnaround_time());
                pass = false;
                }

                if (e.quantumHistory != null) {
                List<Integer> actualHistory = quantumHistories.getOrDefault(e.name, Collections.emptyList());
                if (!e.quantumHistory.equals(actualHistory)) {
                    System.out.println(tc.name + " (" + e.name + ") quantum history mismatch: expected "
                        + e.quantumHistory + " got " + actualHistory);
                    pass = false;
                }
                }
            }

            pass &= compareDouble("Average waiting", tc.name, expected.averageWaitingTime,
                averageWaiting(schedule.getProcesses()));
            pass &= compareDouble("Average turnaround", tc.name, expected.averageTurnaroundTime,
                averageTurnaround(schedule.getProcesses()));

            if (pass) {
                System.out.println(tc.name + " PASSED");
            }
            }

            private static void verifyGeneralResult(String scheduler, String testName, Schedule schedule, ExpectedResult expected,
                Map<String, List<Integer>> quantumHistories) {
            boolean pass = true;

            pass &= compareLists("Execution order", testName + " " + scheduler, expected.executionOrder,
                schedule.getExecutionOrder());

            Map<String, Process> actualProcesses = mapByName(schedule.getProcesses());
            for (ExpectedProcessResult e : expected.processResults) {
                Process actual = actualProcesses.get(e.name);
                if (actual == null) {
                System.out.println(testName + " " + scheduler + " missing process " + e.name);
                pass = false;
                continue;
                }

                if (actual.get_waiting_time() != e.waitingTime) {
                System.out.println(testName + " " + scheduler + " (" + e.name + ") waiting mismatch: expected "
                    + e.waitingTime + " got " + actual.get_waiting_time());
                pass = false;
                }

                if (actual.get_turnaround_time() != e.turnaroundTime) {
                System.out.println(testName + " " + scheduler + " (" + e.name + ") turnaround mismatch: expected "
                    + e.turnaroundTime + " got " + actual.get_turnaround_time());
                pass = false;
                }

                if (e.quantumHistory != null && quantumHistories != null) {
                List<Integer> actualHistory = quantumHistories.getOrDefault(e.name, Collections.emptyList());
                if (!e.quantumHistory.equals(actualHistory)) {
                    System.out.println(testName + " " + scheduler + " (" + e.name
                        + ") quantum history mismatch: expected " + e.quantumHistory + " got " + actualHistory);
                    pass = false;
                }
                }
            }

            pass &= compareDouble("Average waiting", testName + " " + scheduler, expected.averageWaitingTime,
                averageWaiting(schedule.getProcesses()));
            pass &= compareDouble("Average turnaround", testName + " " + scheduler, expected.averageTurnaroundTime,
                averageTurnaround(schedule.getProcesses()));

            if (pass) {
                System.out.println(testName + " " + scheduler + " PASSED");
            }
            }

            private static boolean compareLists(String label, String testName, List<String> expected, List<String> actual) {
            if (!expected.equals(actual)) {
                System.out.println(testName + " " + label + " mismatch:\n expected: " + expected + "\n actual  : "
                    + actual);
                return false;
            }
            return true;
            }

            private static boolean compareDouble(String label, String testName, double expected, double actual) {
            if (Math.abs(expected - actual) > 0.01) {
                System.out.println(testName + " " + label + " mismatch: expected " + expected + " got " + actual);
                return false;
            }
            return true;
            }

            private static Map<String, Process> mapByName(List<Process> processes) {
            Map<String, Process> map = new HashMap<>();
            for (Process p : processes) {
                map.put(p.get_name(), p);
            }
            return map;
            }

            private static double averageWaiting(List<Process> processes) {
            return processes.stream().mapToInt(Process::get_waiting_time).average().orElse(0.0);
            }

            private static double averageTurnaround(List<Process> processes) {
            return processes.stream().mapToInt(Process::get_turnaround_time).average().orElse(0.0);
            }
}
