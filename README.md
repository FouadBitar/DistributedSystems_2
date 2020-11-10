# comp512-project-part2


## How to run
### Run the Resource Managers
- open 3 separate terminals on three separate machines
- change to the /Server directory
- run "make" to compile the Server
- then run "./run_server.sh Flights" "./run_server.sh Cars" "./run_server.sh Rooms" for each server (i.e. Flights for terminal 1)

### Run the middleware
- open another terminal
- change to the /Server directory
- run "make" to compile the Server
- then run "./run_middleware.sh machine1 machine2 machine 3" where machine1 is the machine for flights, machine2 for cars, and machine3 for rooms

### Run the client
- open another terminal
- change to the /Client directory
- run "make" to compile the client
- run "./run_client.sh host" where host is the machine hosting the middleware

## Commands
- list the running processes and their pid
    lsof -i -n -P | grep TCP 
- kill process 
    kill -9 pid



