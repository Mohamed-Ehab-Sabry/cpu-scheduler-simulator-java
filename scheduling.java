import static org.junit.jupiter.api.Assertions.*;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import com.google.gson.*;

class Unit_test {

    // ================== HELPER METHOD ==================
    private JsonObject loadJson(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(new File(filePath).toPath()));
        return JsonParser.parseString(content).getAsJsonObject();
    }

    private List<Process> parseProcesses(JsonObject json) {
        List<Process> processes = new ArrayList<>();
        JsonArray procs = json.getAsJsonObject("input").getAsJsonArray("processes");
        for (JsonElement proc : procs) {
            JsonObject p = proc.getAsJsonObject();
            processes.add(new Process(
                    p.get("name").getAsString(),
                    p.get("arrival").getAsInt(),
                    p.get("burst").getAsInt(),
                    p.get("priority").getAsInt()));
        }
        return processes;
    }

    private Map<String, Integer> parseQuanta(JsonObject json) {
        Map<String, Integer> quanta = new HashMap<>();
        JsonArray procs = json.getAsJsonObject("input").getAsJsonArray("processes");
        for (JsonElement proc : procs) {
            JsonObject p = proc.getAsJsonObject();
            quanta.put(p.get("name").getAsString(), p.get("quantum").getAsInt());
        }
        return quanta;
    }

    // ================== PARAMETERIZED TESTS ==================

    void testAGFiles(String filePath) throws Exception {
        // Arrange
        JsonObject json = loadJson(filePath);
        List<Process> processes = parseProcesses(json);
        Map<String, Integer> quanta = parseQuanta(json);

        // Act
        AG_Schedule scheduler = new AG_Schedule(scheduling.copyList(processes), quanta, 4);
        scheduler.execute();

        // Assert
        JsonObject expected = json.getAsJsonObject("expectedOutput");

        // Execution order
        JsonArray expectedOrderJson = expected.getAsJsonArray("executionOrder");
        List<String> expectedOrder = new ArrayList<>();
        expectedOrderJson.forEach(e -> expectedOrder.add(e.getAsString()));
        assertEquals(expectedOrder, scheduler.getExecutionOrder());

        // Process results
        Map<String, Process> actualProcesses = scheduling.mapByName(scheduler.getProcesses());
        JsonArray processResults = expected.getAsJsonArray("processResults");

        for (JsonElement elem : processResults) {
            JsonObject result = elem.getAsJsonObject();
            String name = result.get("name").getAsString();
            int expectedWaiting = result.get("waitingTime").getAsInt();
            int expectedTurnaround = result.get("turnaroundTime").getAsInt();

            Process actual = actualProcesses.get(name);
            assertNotNull(actual);
            assertEquals(expectedWaiting, actual.get_waiting_time());
            assertEquals(expectedTurnaround, actual.get_turnaround_time());
        }

        // Average times
        assertEquals(expected.get("averageWaitingTime").getAsDouble(),
                scheduling.averageWaiting(scheduler.getProcesses()), 0.01);
        assertEquals(expected.get("averageTurnaroundTime").getAsDouble(),
                scheduling.averageTurnaround(scheduler.getProcesses()), 0.01);
    }

    void testOtherSchedulerFiles(String filePath) throws Exception {
        // Arrange
        JsonObject json = loadJson(filePath);
        JsonObject input = json.getAsJsonObject("input");

        int contextSwitch = input.get("contextSwitch").getAsInt();
        int rrQuantum = input.get("rrQuantum").getAsInt();
        int agingInterval = input.get("agingInterval").getAsInt();

        List<Process> processes = parseProcesses(json);
        JsonObject expectedOutput = json.getAsJsonObject("expectedOutput");

        // ---- SJF ----
        SJF_Schedule sjf = new SJF_Schedule(scheduling.copyList(processes), contextSwitch);
        sjf.execute();
        assertProcessResults(sjf, expectedOutput.getAsJsonObject("SJF"));

        // ---- RR ----
        RR_Schedule rr = new RR_Schedule(scheduling.copyList(processes), contextSwitch, rrQuantum);
        rr.execute();
        assertProcessResults(rr, expectedOutput.getAsJsonObject("RR"));

        // ---- Priority ----
        PriorityWithAgingSchedule priority = new PriorityWithAgingSchedule(scheduling.copyList(processes),
                contextSwitch, agingInterval);
        priority.execute();
        assertProcessResults(priority, expectedOutput.getAsJsonObject("Priority"));
    }

    // ================== HELPER ASSERT METHOD ==================
    private void assertProcessResults(Schedule scheduler, JsonObject expected) {
        JsonArray expectedOrderJson = expected.getAsJsonArray("executionOrder");
        List<String> expectedOrder = new ArrayList<>();
        expectedOrderJson.forEach(e -> expectedOrder.add(e.getAsString()));
        assertEquals(expectedOrder, scheduler.getExecutionOrder());

        Map<String, Process> actualProcesses = scheduling.mapByName(scheduler.getProcesses());
        JsonArray processResults = expected.getAsJsonArray("processResults");

        for (JsonElement elem : processResults) {
            JsonObject result = elem.getAsJsonObject();
            String name = result.get("name").getAsString();
            int expectedWaiting = result.get("waitingTime").getAsInt();
            int expectedTurnaround = result.get("turnaroundTime").getAsInt();

            Process actual = actualProcesses.get(name);
            assertNotNull(actual);
            assertEquals(expectedWaiting, actual.get_waiting_time());
            assertEquals(expectedTurnaround, actual.get_turnaround_time());
        }

        assertEquals(expected.get("averageWaitingTime").getAsDouble(),
                scheduling.averageWaiting(scheduler.getProcesses()), 0.01);
        assertEquals(expected.get("averageTurnaroundTime").getAsDouble(),
                scheduling.averageTurnaround(scheduler.getProcesses()), 0.01);
    }
}

// =============== GENERAL PART *FOR ALL* ===============
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

// ============================================== 1. SRJF  ============================================ //

class SJF_process extends Process {
    protected int remainingTime = 0;
    protected boolean started = false;

    public SJF_process(String name, int arrival_time, int burst_time) {
        super(name, arrival_time, burst_time, 0); // ignore the priority -> make it 0
        this.remainingTime = burst_time;
        this.started = false;
    }

    // SETTERS & GETTERS
    public int get_RemainingTime() {        return remainingTime;}
    public boolean isStarted() {return started;}
    public void setStarted(boolean started) {this.started = started;}
}

class SJF_Schedule extends Schedule {
    private List<SJF_process> sjf_processes;

    public SJF_Schedule(List<Process> processes, int contextSwitchTime) {
        super(processes, contextSwitchTime);
        this.sjf_processes = new ArrayList<>();

        for (Process p : processes) {
            SJF_process sjf = new SJF_process(p.get_name(), p.get_arrival_time(), p.get_burst_time());
            sjf_processes.add(sjf);
        }
    }

    // MAIN FNC
    @Override
    protected void runSchedule() {
        int currentTime = 0;
        SJF_process currentRunning = null;
        int completed = 0;

        while (completed < sjf_processes.size()) {
            // Load ready processes - those that have arrived and have remaining time
            List<SJF_process> ready = new ArrayList<>();
            for (SJF_process p : sjf_processes)
                if (p.arrival_time <= currentTime && p.remainingTime > 0)
                    ready.add(p);

            // If no process is ready, jump to next arrival time
            if (ready.isEmpty()) {
                int nextArrival = Integer.MAX_VALUE;
                for (SJF_process p : sjf_processes)
                    if (p.remainingTime > 0)
                        nextArrival = Math.min(nextArrival, p.arrival_time);

                if (nextArrival != Integer.MAX_VALUE)
                    currentTime = nextArrival;

                continue;
            }

            // choose process with minimum remaining time
            SJF_process next = ready.stream()
                    .min(Comparator.comparingInt(SJF_process::get_RemainingTime)
                            .thenComparingInt(p -> p.get_arrival_time())
                            .thenComparing(p -> p.get_name()))
                    .get();

            // log execution order only when process changes, and add context switch time if
            // switching
            if (currentRunning != next) {
                executionOrder.add(next.get_name());
                if (currentRunning != null) // not first process
                    currentTime += contextSwitchTime;
            }

            currentRunning = next;

            // Execute for one time unit
            if (!next.isStarted()) {
                next.setStarted(true);
                next.set_time_in(currentTime);
            }
            next.remainingTime--;
            currentTime++;

            // if process completes, record completion time and calculate metrics
            if (next.remainingTime == 0) {
                next.set_time_out(currentTime);
                next.turnaround_time = currentTime - next.arrival_time;
                next.waiting_time = next.turnaround_time - next.original_burst_time;
                completed++;
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

// ==================== PREEMPTIVE PRIORITY WITH AGING ==================

class PriorityProcess extends Process {
    int remainingTime;
    int tempArrivalTime; // For aging calculation (when process was last preempted/resumed)
    int lastAgedTime; // Track when this process was last aged

    PriorityProcess(String name, int arrival, int burst, int priority) {
        super(name, arrival, burst, priority);
        this.remainingTime = burst;
        this.tempArrivalTime = arrival;
        this.lastAgedTime = arrival;
    }

    void CheckAge(int currentTime, int agingInterval) {
        if (currentTime > tempArrivalTime &&
                (currentTime - tempArrivalTime) % agingInterval == 0) {
            if (priority > 1) {
                priority--;
            }
        }
    }
}

class PriorityWithAgingSchedule extends Schedule {

    private List<PriorityProcess> all;
    private List<PriorityProcess> finished;
    private PriorityQueue<PriorityProcess> readyQueue;
    private int time;
    private int arrivalIndex;
    private int agingInterval;
    private String lastProcessName; // Track last process for context switch detection

    PriorityWithAgingSchedule(List<Process> processes, int contextSwitch, int agingInterval) {
        super(processes, contextSwitch);
        this.agingInterval = agingInterval;
        this.time = 0;
        this.arrivalIndex = 0;
        this.finished = new ArrayList<>();
        this.lastProcessName = "";

        all = new ArrayList<>();
        for (Process p : processes) {
            all.add(new PriorityProcess(p.get_name(), p.get_arrival_time(),
                    p.get_burst_time(), p.get_priority()));
        }

        all.sort(Comparator.comparingInt(Process::get_arrival_time));

        readyQueue = new PriorityQueue<>(
                Comparator.comparingInt(PriorityProcess::get_priority)
                        .thenComparingInt(PriorityProcess::get_arrival_time)
                        .thenComparing(PriorityProcess::get_name));
    }

    @Override
    protected void runSchedule() {
        // Add initial arrivals
        while (arrivalIndex < all.size() && all.get(arrivalIndex).get_arrival_time() <= time) {
            PriorityProcess p = all.get(arrivalIndex++);
            readyQueue.add(p);
        }

        while (finished.size() < all.size()) {
            PriorityProcess current = null;
            String currentName = "Null";

            // Get next process to run
            if (!readyQueue.isEmpty()) {
                current = readyQueue.poll();
                currentName = current.get_name();
                // Only add to execution order if process changed from last execution
                if (executionOrder.isEmpty() || !currentName.equals(executionOrder.get(executionOrder.size() - 1))) {
                    executionOrder.add(currentName);
                }
            }

            // Check for context switch (process change)
            if (!lastProcessName.isEmpty() && !lastProcessName.equals(currentName)) {
                lastProcessName = currentName;

                // If we have a process, put it back (we'll handle it after context switch)
                if (current != null) {
                    readyQueue.add(current);
                    current = null;
                }

                // context switch
                for (int cs = 0; cs < contextSwitchTime; cs++) {
                    time++;

                    // Apply aging to waiting processes during context switch
                    List<PriorityProcess> temp = new ArrayList<>();
                    while (!readyQueue.isEmpty()) {
                        PriorityProcess p = readyQueue.poll();
                        p.CheckAge(time, agingInterval);
                        temp.add(p);
                    }
                    readyQueue.addAll(temp);

                    // عشان لو حاجه وصلت خلال وقت الكونتكست سويتش
                    while (arrivalIndex < all.size() && all.get(arrivalIndex).get_arrival_time() <= time) {
                        PriorityProcess p = all.get(arrivalIndex++);
                        readyQueue.add(p);
                    }
                }
                continue;
            }

            // update last process name and remaining time
            if (current != null) {
                lastProcessName = currentName;
                current.remainingTime--;
            }

            time++;

            // aging
            List<PriorityProcess> temp = new ArrayList<>();
            while (!readyQueue.isEmpty()) {
                PriorityProcess p = readyQueue.poll();
                p.CheckAge(time, agingInterval);
                temp.add(p);
            }
            readyQueue.addAll(temp);

            // checking for new arrivals
            while (arrivalIndex < all.size() && all.get(arrivalIndex).get_arrival_time() <= time) {
                PriorityProcess p = all.get(arrivalIndex++);
                readyQueue.add(p);
            }

            // handle current process after execution
            if (current != null) {
                if (current.remainingTime > 0) { // Update tempArrivalTime for aging calculation
                    current.tempArrivalTime = time;
                    readyQueue.add(current);
                } else { // process finished
                    current.time_out = time;
                    current.turnaround_time = current.time_out - current.arrival_time;
                    current.waiting_time = current.turnaround_time - current.burst_time;
                    finished.add(current);
                }
            }

            // If no process was running and queue is empty but arrivals pending
            if (current == null && readyQueue.isEmpty() && arrivalIndex < all.size()) {
                // Jump to next arrival time
                time = all.get(arrivalIndex).get_arrival_time();
                while (arrivalIndex < all.size() && all.get(arrivalIndex).get_arrival_time() <= time) {
                    PriorityProcess p = all.get(arrivalIndex++);
                    readyQueue.add(p);
                }
            }
        }

        // Update original Process objects
        for (Process p : processes) {
            for (PriorityProcess done : finished) {
                if (p.get_name().equals(done.get_name())) {
                    p.waiting_time = done.waiting_time;
                    p.turnaround_time = done.turnaround_time;
                    p.time_out = done.time_out;
                    break;
                }
            }
        }
    }

    @Override
    protected void calculateMetrics() {
    }
}

// ================== MAINF CLASS ==================
public class scheduling {
    public static void main(String[] args) {
        try {
            System.out.println("========================================");
            System.out.println(" CPU SCHEDULER - JSON-based Tests");
            System.out.println("========================================\n");

            Unit_test ut = new Unit_test();
            ut.testAGFiles("test_cases_v5/AG/AG_test1.json");
            ut.testAGFiles("test_cases_v5/AG/AG_test2.json");
            ut.testAGFiles("test_cases_v5/AG/AG_test3.json");
            ut.testAGFiles("test_cases_v5/AG/AG_test4.json");
            ut.testAGFiles("test_cases_v5/AG/AG_test5.json");
            ut.testAGFiles("test_cases_v5/AG/AG_test6.json");

            ut.testOtherSchedulerFiles("test_cases_v5/Other_Schedulers/test_1.json");
            ut.testOtherSchedulerFiles("test_cases_v5/Other_Schedulers/test_2.json");
            ut.testOtherSchedulerFiles("test_cases_v5/Other_Schedulers/test_3.json");
            ut.testOtherSchedulerFiles("test_cases_v5/Other_Schedulers/test_4.json");
            ut.testOtherSchedulerFiles("test_cases_v5/Other_Schedulers/test_5.json");
            ut.testOtherSchedulerFiles("test_cases_v5/Other_Schedulers/test_6.json");

            System.out.println("\n========================================");
            System.out.println(" All Tests Completed");
            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("Error loading tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper to refresh data for each run
    public static List<Process> copyList(List<Process> original) {
        List<Process> copy = new ArrayList<>();
        for (Process p : original) {
            copy.add(new Process(p.name, p.arrival_time, p.burst_time, p.priority));
        }
        return copy;
    }

    public static Map<String, Process> mapByName(List<Process> processes) {
        Map<String, Process> map = new HashMap<>();
        for (Process p : processes) {
            map.put(p.get_name(), p);
        }
        return map;
    }

    public static double averageWaiting(List<Process> processes) {
        return processes.stream().mapToInt(Process::get_waiting_time).average().orElse(0.0);
    }

    public static double averageTurnaround(List<Process> processes) {
        return processes.stream().mapToInt(Process::get_turnaround_time).average().orElse(0.0);
    }
}
