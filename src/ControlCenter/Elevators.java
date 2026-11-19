package ControlCenter;

import SoftwareBus.Topics;
import SoftwareBus.Message;
import SoftwareBus.SoftwareBus;
import java.util.UUID;

public class Elevators {
    private final int[] currentFloor;
    private final int[] serviceFloor;
    private final Direction[] direction;
    private final boolean[] powered;

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

        for (int i = 1; i <= numberOfElevators; i++) {
            bus.subscribe(id, Topics.ARRIVED, i);
            currentFloor[i] = 1;
            serviceFloor[i] = 1;
            direction[i] = Direction.STOPPED;
            powered[i] = true;
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

    //updates the elevator's position when movement information is received
    public void updateCurrentFloor(int elevator, int floor) {
        if (!validElevator(elevator)) return;

        if (floor < 1 || floor > Manager.NUM_FLOORS) return;

        currentFloor[elevator] = floor;

        if (floor == serviceFloor[elevator]) {
            direction[elevator] = Direction.STOPPED;
        }
    }

    private boolean validElevator(int elevator) {
        return elevator > 0 && elevator < currentFloor.length;
    }
}
