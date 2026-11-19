# ECS Control Center - Team 9

Control Center for the Elevator Control System (CS 460, Team 9).
The Control Center decides where the elevators go. See the SAD for the full design.

## Structure
- `src/ControlCenter/Main.java` - entry point. Creates the Software Bus, Fire Alarm, Cabin Calls, Floor Panels, Elevators, and the Manager, and starts the Manager.
- `src/ControlCenter/Manager.java` - holds the components and runs the control loop. Each step of the SAD logic has its own method.

## Setup
1. Clone the repo and open the folder in IntelliJ.
2. Right-click `src` and choose Mark Directory as > Sources Root.
3. Run `ControlCenter.Main`.
   - No arguments: local Software Bus (testing)
   - `<router-ip> <router-port>`: connect to a Software Bus router

## Team
- Santiago Rivera (manager)
- Michelle Flores
- Sang Nguyen
- Swastika Dawadi
- Aditya Dev
