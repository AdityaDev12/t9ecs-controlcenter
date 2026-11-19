package ControlCenter;

import SoftwareBus.Topics;
import SoftwareBus.Message;
import SoftwareBus.SoftwareBus;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.UUID;


public class Elevators {
    private final int[] currentFloor;
    private final int[] serviceFloor;
    private final Direction[] direction;
    private final boolean[] powered;
    private final NavigableSet<Integer>[] pendingStops;

    private final SoftwareBus bus;
    private final UUID id;

    public Elevators(SoftwareBus bus) {
        this.bus = bus;
        this.id = bus.register();

        int numberOfElevators = Manager.NUM_ELEVATORS;

        currentFloor = new int[numberOfElevators + 1];
        serviceFloor = new int[numberOfElevators + 1];
        direction = new Direction[numberOfElevators + 1];
        powered = new boolean[numberOfElevators + 1];
        pendingStops = new NavigableSet[numberOfElevators + 1];

        for (int i = 1; i <= numberOfElevators; i++) {
            bus.subscribe(id, Topics.ARRIVED, i);
            currentFloor[i] = 1;
            serviceFloor[i] = 1;
            direction[i] = Direction.STOPPED;
            powered[i] = true;
            pendingStops[i] = new TreeSet<>();
            bus.subscribe(id, Topics.ARRIVED, i);
        }
    }

    public void setPower(int elevator, boolean on) {
        if (!validElevator(elevator)) return;

        powered[elevator] = on;
    }

    //destination command
    public void moveToFloor(int elevator, int floor) {
        if (!validElevator(elevator) || !powered[elevator]) return;

        if (floor < 1 || floor > Manager.NUM_FLOORS) return;

        serviceFloor[elevator] = floor;

        if (floor > currentFloor[elevator]) {
            direction[elevator] = Direction.UP;
        } else if (floor < currentFloor[elevator]) {
            direction[elevator] = Direction.DOWN;
        } else {
            direction[elevator] = Direction.STOPPED;
        }

        Message message = new Message(
                Topics.GO_TO,
                elevator,
                new int[]{floor}
        );

        bus.publish(id, message);
    }

    public void addStop(int elevator, int floor) {
        if (!validElevator(elevator) || !powered[elevator]) {
            return;
        }
        if (floor < 1 || floor > Manager.NUM_FLOORS) {
            return;
        }
        // ignore duplicate requests for existing stop
        if (!pendingStops[elevator].add(floor)) {
            return;
        }
        // DEBUG PRINT
        System.out.println("Elevator " + elevator + ": Added floor " + floor + " to its route.");
        // if elevator is idle, move toward destination
        if (direction[elevator] == Direction.STOPPED) {
            dispatchNextStop(elevator);
            return;
        }
        // if moving UP, check if new stop is between current floor and destination
        if (direction[elevator] == Direction.UP
                && floor > currentFloor[elevator]
                && floor < serviceFloor[elevator]) {
            serviceFloor[elevator] = floor;
            publishGoTo(elevator, floor);
        }
        // if moving DOWN, check if new stop is between current floor and destination
        else if (direction[elevator] == Direction.DOWN
                && floor < currentFloor[elevator]
                && floor > serviceFloor[elevator]) {
            serviceFloor[elevator] = floor;
            publishGoTo(elevator, floor);
        }
    }

    private void publishGoTo(int elevator, int floor) {
        Message message = new Message(
                Topics.GO_TO,
                elevator,
                new int[]{floor}
        );
        bus.publish(id, message);
    }

    private void dispatchNextStop(int elevator) {
        if (!validElevator(elevator) || !powered[elevator]) {
            return;
        }
        // no pending stops; elevator is idle
        if (pendingStops[elevator].isEmpty()) {
            direction[elevator] = Direction.STOPPED;
            serviceFloor[elevator] = currentFloor[elevator];
            return;
        }
        int current = currentFloor[elevator];
        Integer next = null;

        if (direction[elevator] == Direction.UP) {
            // continue UP if there's requests above current floor
            next = pendingStops[elevator].higher(current);
            // reverse and serve highest remaining floor below
            if (next == null) {
                next = pendingStops[elevator].lower(current);
            }
        } else if (direction[elevator] == Direction.DOWN) {
            // continue DOWN if there's requests below current floor
            next = pendingStops[elevator].lower(current);
            // reverse and serve lowest remaining floor above
            if (next == null) {
                next = pendingStops[elevator].higher(current);
            }
        } else {
            // idle elevator; choose closest pending stop
            Integer above = pendingStops[elevator].higher(current);
            Integer below = pendingStops[elevator].lower(current);
            if (above == null) {
                next = below;
            } else if (below == null) {
                next = above;
            } else {
                int upDistance = above - current;
                int downDistance = current - below;
                next = (upDistance <= downDistance) ? above : below;
            }
        }
        if (next != null) {
            serviceFloor[elevator] = next;
            // update direction based on selected destination
            if (next > current) {
                direction[elevator] = Direction.UP;
            } else if (next < current) {
                direction[elevator] = Direction.DOWN;
            }
            // DEBUG PRINT
            System.out.println("Elevator " + elevator
                    + ": Moving " + direction[elevator]
                    + " from floor " + current
                    + " to floor " + next);
            publishGoTo(elevator, next);
        }
    }

    // commands elevator to open doors at current floor
    public void serviceAtFloor(int elevator, int floor) {
        if (!validElevator(elevator) || !powered[elevator]) {
            return;
        }
        if (floor < 1 || floor > Manager.NUM_FLOORS) {
            return;
        }
        if (currentFloor[elevator] != floor) {
            return;
        }
        Message message = new Message (
                Topics.SERVICE,
                elevator,
                new int[]{floor}
        );
        bus.publish(id, message);
    }

    public void completeStop(int elevator) {
        if (!validElevator(elevator)) {
            return;
        }
        int arrivedFloor = currentFloor[elevator];
        // remove floor that elevator just reached
        pendingStops[elevator].remove(arrivedFloor);
        //DEBUG PRINT
        System.out.println("Elevator " + elevator
                + ": Completed stop at floor " + arrivedFloor);
        // publish next destination
        dispatchNextStop(elevator);
    }

    // receive arrival messages from the Elevator Control Board
    public void pollArrivals() {
        for (int elevator = 1; elevator <= Manager.NUM_ELEVATORS; elevator++) {
            Message message;
            while((message = bus.getMsg(id, Topics.ARRIVED, elevator)) != null) {
                int[] body = message.body();

                if (body.length > 0) {
                    int floor = body[0];
                    updateCurrentFloor(elevator, floor);
                }
            }
        }
    }

    public int getCurrentFloor(int elevator) {
        return validElevator(elevator) ? currentFloor[elevator] : -1;
    }

    public int getServiceFloor(int elevator) {
        return validElevator(elevator) ? serviceFloor[elevator] : -1;
    }

    public Direction getDirection(int elevator) {
        return validElevator(elevator) ? direction[elevator] : Direction.STOPPED;
    }

    public boolean isPoweredOn(int elevator) {
        return validElevator(elevator) && powered[elevator];
    }

    public boolean isAtStop(int elevator) {
        if (!validElevator(elevator)) {
            return false;
        }
        return currentFloor[elevator] == serviceFloor[elevator]
                && pendingStops[elevator].contains(currentFloor[elevator]);
    }

    //updates the elevator's position when movement information is received
    public void updateCurrentFloor(int elevator, int floor) {
        if (!validElevator(elevator)) return;

        if (floor < 1 || floor > Manager.NUM_FLOORS) return;

        currentFloor[elevator] = floor;
        // DEBUG PRINT
        System.out.println("Elevator " + elevator + ": Arrived at floor " + floor);
    }

    private boolean validElevator(int elevator) {
        return elevator > 0 && elevator < currentFloor.length;
    }
}
