package ControlCenter;

public class Manager {

    public static final int NUM_ELEVATORS = 4;   // Team 10 PFD: elevators 1-4
    public static final int NUM_FLOORS = 10;     // Team 10 PFD: floors 1-10
    public static final int NO_ELEVATOR = -1;    // returned by selectElevator when none is available

    private static final long CYCLE_MS = 100;    // time between control cycles

    private final FireAlarm fireAlarm;
    private final CabinCalls cabinCalls;
    private final FloorPanels floorPanels;
    private final Elevators elevators;

    // Creates the Manager with the components it works with.
    public Manager(FireAlarm fireAlarm, CabinCalls cabinCalls,
                   FloorPanels floorPanels, Elevators elevators) {
        this.fireAlarm = fireAlarm;
        this.cabinCalls = cabinCalls;
        this.floorPanels = floorPanels;
        this.elevators = elevators;
    }

    // Starts the control loop. Runs one cycle every CYCLE_MS.
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            cycle();
            try {
                Thread.sleep(CYCLE_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // One control cycle, in the order of the SAD logic.
    private void cycle() {
        // 1. fire alarm overrides normal operation
        if (fireAlarm.isFireActive()) {
            handleFire();
            return;
        }
        // 2. elevator state
        for (int e = 1; e <= NUM_ELEVATORS; e++) {
            readElevatorState(e);
        }
        // 3. cabin calls
        for (int e = 1; e <= NUM_ELEVATORS; e++) {
            if (cabinCalls.hasPendingRequests(e)) {
                serveCabinCalls(e, cabinCalls.getPendingRequests(e));
            }
        }
        // 4. floor panel calls
        if (floorPanels.hasPendingRequests()) {
            for (String request : floorPanels.getPendingRequests()) {
                String[] parts = request.trim().split("\\s+");   // "3 UP" -> floor 3, UP
                serveFloorCall(Integer.parseInt(parts[0]), Direction.valueOf(parts[1]));
            }
        }
    }

    // fire alarm is active.
    private void handleFire() {
        // TODO algorithm
    }

    // read getCurrentFloor, getDirection, isPoweredOn for one elevator.
    private void readElevatorState(int elevator) {
        // TODO algorithm
    }

    //cabin calls are always served by the elevator they were made in.
    private void serveCabinCalls(int elevator, int[] floors) {
        // TODO algorithm
    }

    // floor panel call; selectElevator picks the elevator.
    private void serveFloorCall(int floor, Direction direction) {
        // TODO algorithm
    }

    // move an elevator to a floor.
    private void sendTo(int elevator, int floor) {
        elevators.moveToFloor(elevator, floor);
    }

    // clear a served cabin call (turns off the cabin button).
    private void clearCabinCall(int elevator, int floor) {
        cabinCalls.cancelRequest(elevator, floor);
        cabinCalls.clearButton(elevator, floor);
    }

    //clear a served floor panel call (turns off the hall button light).
    private void clearFloorCall(int floor, Direction direction) {
        floorPanels.cancelRequest(floor, direction);
        floorPanels.updateIndicator(floor, direction, false);
    }

    // Local service policy returns the elevator that should answer a floor panel call,
    // or NO_ELEVATOR if none is available.

    private int selectElevator(int floor, Direction direction) {
        // TODO algorithm
        return NO_ELEVATOR;
    }
}
