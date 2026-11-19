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
    // handles one destination at a time
    // services the floor, clears the request, sends to pending destination
    private void serviceArrivedElevator(int elevator) {
        if (!elevators.isPoweredOn(elevator)) {
            return;
        }
        int current = elevators.getCurrentFloor(elevator);
        int destination = elevators.getServiceFloor(elevator);
        // elevator hasn't reached destination
        if (current != destination || elevators.getDirection(elevator) != Direction.STOPPED) {
            return;
        }
        boolean hasCabinRequest = false;
        for (int floor : cabinCalls.getPendingRequests(elevator)) {
            if (floor == current) {
                hasCabinRequest = true;
                break;
            }
        }
        // check if elevator was assigned a hall request
        boolean hasFloorRequest = false;
        for (Map.Entry<String, Integer> entry : assignedFloorCalls.entrySet()) {
            if (entry.getValue() == elevator) {
                String[] parts = entry.getKey().split("\\s+");
                int requestedFloor = Integer.parseInt(parts[0]);
                if (requestedFloor == current) {
                    hasFloorRequest = true;
                    break;
                }
            }
        }
        if (!hasCabinRequest && !hasFloorRequest) {
            return;
        }
        // open elevator doors
        elevators.serviceAtFloor(elevator, current);
        // clear any cabin request at this floor
        if (hasCabinRequest) {
            clearCabinCall(elevator, current);
        }
        // clear any hall requests assigned to this elevator at this floor
        var iterator = assignedFloorCalls.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (entry.getValue() == elevator) {
                String[] parts = entry.getKey().split("\\s+");
                int requestedFloor = Integer.parseInt(parts[0]);
                if (requestedFloor == current) {
                    Direction direction = Direction.valueOf(parts[1]);
                    clearFloorCall(current, direction);
                    iterator.remove();
                }
            }
        }
    }

    //cabin calls are always served by the elevator they were made in.
    private void serveCabinCalls(int elevator, int[] floors) {
        // don't assign requests to powered off elevator
        if (!elevators.isPoweredOn(elevator)) {
            return;
        }
        // don't interrupt an elevator already moving
        if (elevators.getDirection(elevator) != Direction.STOPPED) {
            return;
        }

        int currentFloor = elevators.getCurrentFloor(elevator);
        int closestFloor = -1;
        int shortestDistance = Integer.MAX_VALUE;

        // find closest pending cabin request
        for (int floor : floors) {
            int distance = Math.abs(floor - currentFloor);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestFloor = floor;
            }
        }
        // no valid request found
        if (closestFloor == -1) {
            return;
        }

        // if request is on current floor, service immediately
        if (closestFloor == currentFloor) {
            elevators.serviceAtFloor(elevator, closestFloor);
            clearCabinCall(elevator, currentFloor);
            return;
        }

        // send elevator to requested floor
        sendTo(elevator, closestFloor);
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
