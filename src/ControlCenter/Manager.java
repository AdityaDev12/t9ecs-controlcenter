package ControlCenter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Manager {

    public static final int NUM_ELEVATORS = 4;   // Team 10 PFD: elevators 1-4
    public static final int NUM_FLOORS = 10;     // Team 10 PFD: floors 1-10
    public static final int NO_ELEVATOR = -1;    // returned by selectElevator when none is available

    private static final long CYCLE_MS = 100;    // time between control cycles

    private final FireAlarm fireAlarm;
    private final CabinCalls cabinCalls;
    private final FloorPanels floorPanels;
    private final Elevators elevators;

    // tracks which elevator is assigned to each hall request
    private final Map<String, Integer> assignedFloorCalls = new HashMap<>();
    private final Set<Integer> fireServiced = new HashSet<>();

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
        // 1. receive incoming messages
        fireAlarm.pollFireStatus();
        elevators.pollArrivals();
        cabinCalls.pollRequests();
        floorPanels.pollRequests();

        // 2. fire alarm overrides normal operation
        if (fireAlarm.isFireActive()) {
            handleFire();
            return;
        }

        // fire alarm has cleared; reset fire service tracking
        fireServiced.clear();

        // 3. check if elevators have arrived at destinations
        for (int e = 1; e <= NUM_ELEVATORS; e++) {
            serviceArrivedElevator(e);
        }

        // 4. handle cabin requests
        for (int e = 1; e <= NUM_ELEVATORS; e++) {
            if (cabinCalls.hasPendingRequests(e)) {
                serveCabinCalls(e, cabinCalls.getPendingRequests(e));
            }
        }
        // 5. handle floor panel requests
        for (String request : floorPanels.getPendingRequests()) {
            String[] parts = request.trim().split("\\s+");   // "3 UP" -> floor 3, UP
            int floor = Integer.parseInt(parts[0]);
            Direction direction = Direction.valueOf(parts[1]);
            serveFloorCall(floor, direction);
        }
    }

    // fire alarm is active.
    private void handleFire() {
        for (int e = 1; e <= NUM_ELEVATORS; e++) {
            if (!elevators.isPoweredOn(e)) {
                continue;
            }
            int current = elevators.getCurrentFloor(e);
            int destination = elevators.getServiceFloor(e);
            // elevator reached floor 1
            if (current == 1 && elevators.getDirection(e) == Direction.STOPPED) {
                if (!fireServiced.contains(e)) {
                    elevators.serviceAtFloor(e, 1);
                    fireServiced.add(e);
                }
            } else if (destination != 1) {
                // send elevator to floor 1
                sendTo(e,1);
            }
        }
    }

    // arrival and service implementation
    // assumes SERVICE command initiates opening the door
    private void serviceArrivedElevator(int elevator) {
        if (!elevators.isAtStop(elevator)) {
            return;
        }
        int floor = elevators.getCurrentFloor(elevator);
        boolean needsService = false;
        // check for cabin request at this floor
        for (int requestedFloor : cabinCalls.getPendingRequests(elevator)) {
            if (requestedFloor == floor) {
                 needsService = true;
                 clearCabinCall(elevator, floor);
                 break;
            }
        }
        // check for hall requests assigned to this elevator
        var iterator = assignedFloorCalls.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() == elevator) {
                String[] parts = entry.getKey().split("\\s+");
                int requestedFloor = Integer.parseInt(parts[0]);
                Direction requestedDirection = Direction.valueOf(parts[1]);
                if (requestedFloor == floor) {
                    needsService = true;
                    clearFloorCall(floor, requestedDirection);
                    iterator.remove();
                }
            }
        }
        // open doors if there's a request at this floor
        if (needsService) {
            elevators.serviceAtFloor(elevator, floor);
        }
        // remove this stop and dispatch next
        elevators.completeStop(elevator);
    }

    //cabin calls are always served by the elevator they were made in.
    private void serveCabinCalls(int elevator, int[] floors) {
        // don't assign requests to powered off elevator
        if (!elevators.isPoweredOn(elevator)) {
            return;
        }
        // add every pending cabin request to this elevator's route
        for (int floor : floors) {
            elevators.addStop(elevator, floor);
        }
    }

    // floor panel request; selectElevator picks the elevator.
    private void serveFloorCall(int floor, Direction direction) {
        // validate request
        if (floor < 1 || floor > NUM_FLOORS) {
            return;
        }
        if (direction == Direction.STOPPED) {
            return;
        }
        String request = floor + " " + direction;
        // don't assign request that already has an elevator
        if (assignedFloorCalls.containsKey(request)) {
            return;
        }
        // find available elevator
        int elevator = selectElevator(floor, direction);
        if (elevator == NO_ELEVATOR) {
            return;
        }
        // remember which elevator is responsible for this request
        assignedFloorCalls.put(request, elevator);
        // send elevator to requested floor
        sendTo(elevator, floor);
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
        int selectedElevator = NO_ELEVATOR;
        int shortestDistance = Integer.MAX_VALUE;
        for (int e = 1; e <= NUM_ELEVATORS; e++) {
            if (!elevators.isPoweredOn(e)) {
                continue;
            }
            // only assigns idle elevators
            if (elevators.getDirection(e) != Direction.STOPPED) {
                continue;
            }
            int current = elevators.getCurrentFloor(e);
            int distance = Math.abs(floor - current);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                selectedElevator = e;
            }
        }
        return selectedElevator;
    }
}
