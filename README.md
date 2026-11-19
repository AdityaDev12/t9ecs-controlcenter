# ECS Control Center - Team 9

Control Center for the Elevator Control System (CS 460, Team 9).
The Control Center decides where the elevators go. See the SAD for the full design.

## Structure
- `src/ControlCenter/Main.java` - entry point. Creates the Software Bus, Fire Alarm, Cabin Calls, Floor Panels, Elevators, and the Manager, and starts the Manager.
- `src/ControlCenter/Manager.java` - holds the components and runs the control loop. Each step of the SAD logic has its own method.

## Team
- Santiago Rivera (manager)
- Michelle Flores
- Sang Nguyen
- Swastika Dawadi
- Aditya Dev
