import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

class InputWrapper {
    Input input;
    ExpectedOutput expectedOutput;
}

class Input {
    List<Proc> processes;
    // Scheduling parameters
    int contextSwitch;
    int rrQuantum;
    int agingInterval; 
}

class Proc {
    String name;
    int arrival;
    int burst;
    int priority;
    int quantum;
}

class ExpectedOutput {
    List<String> executionOrder;
    List<ExpectedProcessResult> processResults;
    double averageWaitingTime;
    double averageTurnaroundTime;
    // Results for other algorithms
    ExpectedOutputRR SJF;
    ExpectedOutputRR RR;
    ExpectedOutputRR Priority;
}

class ExpectedProcessResult {
    String name;
    int waitingTime;
    int turnaroundTime;
    List<Integer> quantumHistory;
}

class ProcessOutcome {
    String name;
    int waitingTime;
    int turnaroundTime;
    List<Integer> quantumHistory;
}

class AgResult {
    List<String> executionOrder;
    List<ProcessOutcome> processResults;
    double averageWaitingTime;
    double averageTurnaroundTime;
}

class process {

    protected String name;
    protected int arrival_time;
    protected int burst_time;
    protected int priority;
    protected int time_in;
    protected int time_out;
    protected int waiting_time;
    protected int turnaround_time;
    protected int original_burst_time;

    public process(String name, int arrival_time, int burst_time, int priority) {

        this.name = name;
        this.arrival_time = arrival_time;
        this.burst_time = burst_time;
        this.original_burst_time = burst_time;
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

    public int get_original_burst_time() {
        return original_burst_time;
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

    public void sub_burst_time_by_one() {
        this.burst_time -= 1;
    }

    public void set_time_in(int time_in) {
        this.time_in = time_in;
    }

    public void set_time_out(int time_out) {
        this.time_out = time_out;
    }

    public void set_turnaround_time() {
        this.turnaround_time = this.time_out - this.arrival_time;
    }

    public void calc_waiting_time() {
        this.waiting_time = this.turnaround_time - this.original_burst_time;
    }

}

class ag_process extends process {

    protected int quantum;
    protected List<Integer> quantum_hist = new ArrayList<>();

    public ag_process(String name, int arrival_time, int burst_time, int priority, int quantum) {

        super(name, arrival_time, burst_time, priority);
        this.quantum = quantum;
    }

    public int get_quantum() {

        return quantum;
    }

    public List<Integer> get_qunatm_hist() {

        return quantum_hist;

    }

    public Integer get_lastest_qunatm() {

        return quantum_hist.get(quantum_hist.size() - 1);

    }

    public void set_quantum(int quantum) {

        this.quantum = quantum;

    }

    public void add_to_qunatm_hist() {

        quantum_hist.add(this.quantum);

    }
}

enum State {

    FCFS,
    PRIORITY,
    SJF

}

public class scheduling {

    public InputWrapper loadTestCase(String path) throws IOException {
        String json = Files.readString(Path.of(path));
        return new Gson().fromJson(json, InputWrapper.class);
    }

    public List<ag_process> toAgProcesses(List<Proc> procs) {
        List<ag_process> list = new ArrayList<>();
        for (Proc p : procs) {
            list.add(new ag_process(p.name, p.arrival, p.burst, p.priority, p.quantum));
        }
        return list;
    }

    public AgResult ag_schedule(List<ag_process> processes) {

        List<ag_process> pending = new ArrayList<>(processes);
        List<ag_process> arrived_processes = new ArrayList<>();
        List<ag_process> finished_processes = new ArrayList<>();
        List<String> execution_order = new ArrayList<>();
        int current_time = 0;
        State state = State.FCFS;

        int time_executed = 0;
        ag_process curr_p;
        while (!pending.isEmpty() || !arrived_processes.isEmpty()) {

            if (!pending.isEmpty()) {

                for (int i = 0; i < pending.size(); i++) {
                    ag_process p = pending.get(i);
                    if (p.get_arrival_time() <= current_time) {
                        arrived_processes.add(p);
                        pending.remove(i);
                        --i;
                    }

                }

            }

            if (!arrived_processes.isEmpty()) {

                if (state == State.FCFS) {
                    curr_p = arrived_processes.get(0);
                    int cycle_period = current_time + (int) Math.ceil(curr_p.get_quantum() * 0.25);
                    curr_p.set_time_in(current_time);
                    execution_order.add(curr_p.get_name());
                    curr_p.add_to_qunatm_hist();
                    time_executed = 0;
                    while (current_time < cycle_period && curr_p.get_burst_time() > 0) {
                        current_time += 1;
                        time_executed += 1;
                        curr_p.sub_burst_time_by_one();

                    }

                    if (curr_p.get_burst_time() == 0) {
                        curr_p.set_time_out(current_time);
                        curr_p.set_quantum(0);
                        curr_p.add_to_qunatm_hist();
                        curr_p.set_turnaround_time();
                        arrived_processes.remove(0);
                        finished_processes.add(curr_p);

                    } else if (current_time == cycle_period) {

                        state = State.PRIORITY;
                    }

                } else if (state == State.PRIORITY) {

                    curr_p = arrived_processes.get(0);
                    ag_process highest_pri_p = arrived_processes.get(0);

                    for (ag_process p : arrived_processes) {
                        if (p.get_priority() < highest_pri_p.get_priority()) {
                            highest_pri_p = p;
                        }
                    }
                    if (curr_p != highest_pri_p) {
                        state = State.FCFS;
                        curr_p.set_time_out(current_time);
                        int latest_qunatum = curr_p.get_lastest_qunatm();
                        Integer add_to_qunatum = (int) Math.ceil((curr_p.get_quantum() - time_executed) / 2.0);
                        curr_p.set_quantum(latest_qunatum + add_to_qunatum);
                        arrived_processes.remove(0);
                        arrived_processes.remove(highest_pri_p);
                        arrived_processes.add(0, highest_pri_p);
                        arrived_processes.add(curr_p);
                        time_executed = 0;
                    } else if (curr_p == highest_pri_p) {

                        curr_p = arrived_processes.get(0);
                        int cycle_period = current_time + (int) Math.ceil(curr_p.get_quantum() * 0.25);
                        execution_order.add(curr_p.get_name());
                        while (current_time < cycle_period && curr_p.get_burst_time() > 0) {
                            current_time += 1;
                            time_executed += 1;
                            curr_p.sub_burst_time_by_one();
                        }
                        if (curr_p.get_burst_time() == 0) {
                            curr_p.set_time_out(current_time);
                            curr_p.set_quantum(0);
                            curr_p.add_to_qunatm_hist();
                            curr_p.set_turnaround_time();
                            arrived_processes.remove(0);
                            finished_processes.add(curr_p);
                            state = State.FCFS;

                        } else if (current_time == cycle_period) {

                            state = State.SJF;
                        }
                    }

                } else if (state == State.SJF) {

                    curr_p = arrived_processes.get(0);
                    ag_process shortest_j_p = arrived_processes.get(0);

                    for (ag_process p : arrived_processes) {
                        if (p.get_burst_time() < shortest_j_p.get_burst_time()) {

                            shortest_j_p = p;

                        }
                    }
                    if (curr_p != shortest_j_p) {
                        state = State.FCFS;
                        curr_p.set_time_out(current_time);
                        int latest_qunatum = curr_p.get_lastest_qunatm();
                        Integer add_to_qunatum = (int) Math.ceil(curr_p.get_quantum() - time_executed);
                        curr_p.set_quantum(latest_qunatum + add_to_qunatum);
                        arrived_processes.remove(0);
                        arrived_processes.remove(shortest_j_p);
                        arrived_processes.add(0, shortest_j_p);
                        arrived_processes.add(curr_p);

                    } else if (curr_p == shortest_j_p) {

                        curr_p = arrived_processes.get(0);
                        int cycle_period = current_time + (curr_p.get_quantum() - time_executed);
                        execution_order.add(curr_p.get_name());
                        current_time += 1;
                        time_executed += 1;
                        curr_p.sub_burst_time_by_one();

                        if (curr_p.get_burst_time() == 0) {
                            curr_p.set_time_out(current_time);
                            curr_p.set_quantum(0);
                            curr_p.add_to_qunatm_hist();
                            curr_p.set_turnaround_time();
                            arrived_processes.remove(0);
                            finished_processes.add(curr_p);
                            state = State.FCFS;

                        } else if (current_time == cycle_period) {

                            state = State.FCFS;
                            curr_p.set_time_out(current_time);
                            int latest_qunatum = curr_p.get_lastest_qunatm();
                            Integer add_to_qunatum = 2;
                            curr_p.set_quantum(latest_qunatum + add_to_qunatum);
                            arrived_processes.remove(0);
                            arrived_processes.add(curr_p);
                        }

                    }

                }
            } else {

                current_time += 1;
            }
        }

        for (ag_process p : finished_processes) {
            p.calc_waiting_time();
        }

        AgResult result = new AgResult();
        result.executionOrder = execution_order;
        result.processResults = new ArrayList<>();
        double waitingSum = 0;
        double turnaroundSum = 0;
        for (ag_process p : finished_processes) {
            ProcessOutcome o = new ProcessOutcome();
            o.name = p.get_name();
            o.waitingTime = p.get_waiting_time();
            o.turnaroundTime = p.turnaround_time;
            o.quantumHistory = new ArrayList<>(p.get_qunatm_hist());
            result.processResults.add(o);
            waitingSum += o.waitingTime;
            turnaroundSum += o.turnaroundTime;
        }
        int n = finished_processes.size();
        result.averageWaitingTime = n == 0 ? 0 : waitingSum / n;
        result.averageTurnaroundTime = n == 0 ? 0 : turnaroundSum / n;
        return result;

    }

    public RRResult roundRobinSchedule(
            List<process> processes,
            int quantum,
            int contextSwitch) {
        List<process> pending = new ArrayList<>(processes);
        List<process> readyQueue = new ArrayList<>();
        List<process> finished = new ArrayList<>();
        List<String> executionOrder = new ArrayList<>();

        int currentTime = 0;
        process lastProcess = null;

        while (!pending.isEmpty() || !readyQueue.isEmpty()) {

            // move arrived processes
            for (int i = 0; i < pending.size(); i++) {
                if (pending.get(i).arrival_time <= currentTime) {
                    readyQueue.add(pending.get(i));
                    pending.remove(i);
                    i--;
                }
            }

            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            process p = readyQueue.remove(0);

            // context switch
            if (lastProcess != null && lastProcess != p) {
                currentTime += contextSwitch;
            }

            executionOrder.add(p.get_name());
            p.set_time_in(currentTime);

            int execTime = Math.min(quantum, p.get_burst_time());

            currentTime += execTime;
            p.burst_time -= execTime;

            // check arrivals during execution
            for (int i = 0; i < pending.size(); i++) {
                if (pending.get(i).arrival_time <= currentTime) {
                    readyQueue.add(pending.get(i));
                    pending.remove(i);
                    i--;
                }
            }

            if (p.get_burst_time() > 0) {
                readyQueue.add(p);
            } else {
                p.set_time_out(currentTime);
                p.set_turnaround_time();
                finished.add(p);
            }

            lastProcess = p;
        }

        double waitSum = 0, tatSum = 0;
        List<ProcessOutcome> results = new ArrayList<>();

        for (process p : finished) {
            p.calc_waiting_time();
            ProcessOutcome o = new ProcessOutcome();
            o.name = p.get_name();
            o.waitingTime = p.get_waiting_time();
            o.turnaroundTime = p.turnaround_time;
            results.add(o);

            waitSum += o.waitingTime;
            tatSum += o.turnaroundTime;
        }

        RRResult result = new RRResult();
        result.executionOrder = executionOrder;
        result.processResults = results;
        result.averageWaitingTime = Math.round((waitSum / finished.size()) * 100.0) / 100.0;
        result.averageTurnaroundTime = Math.round((tatSum / finished.size()) * 100.0) / 100.0;

        return result;
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Pass path to AG_test*.json");
            return;
        }
        scheduling s = new scheduling();
        InputWrapper data = s.loadTestCase(args[0]);
        List<ag_process> items = s.toAgProcesses(data.input.processes);
        AgResult result = s.ag_schedule(items);
        System.out.println("Execution order: " + result.executionOrder);
        for (ProcessOutcome o : result.processResults) {
            System.out.println(o.name + " wait=" + o.waitingTime + " tat=" + o.turnaroundTime + " q=" + o.quantumHistory);
        }
        System.out.println("avg wait=" + result.averageWaitingTime + " avg tat=" + result.averageTurnaroundTime);
    }
}

    // Round Robin Scheduler
    class RRResult {
        List<String> executionOrder;
        List<ProcessOutcome> processResults;
        double averageWaitingTime;
        double averageTurnaroundTime;
    }
    
    class ExpectedOutputRR {
    List<String> executionOrder;
    List<ExpectedProcessResult> processResults;
    double averageWaitingTime;
    double averageTurnaroundTime;
}