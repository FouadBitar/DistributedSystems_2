# comp512-project

To run the RMI resource manager:

```
cd Server/
./run_server.sh [<rmi_name>] # starts a single ResourceManager
./run_servers.sh # convenience script for starting multiple resource managers
```

To run the RMI client:

```
cd Client
./run_client.sh [<server_hostname> [<server_rmi_name>]]
```

## How to run
### Run the Resource Managers
- open 3 separate terminals on machines 1,2,3
- change to the /Server directory
- run "make" to compile the Server
- then run "./run_server.sh Flights" "./run_server.sh Cars" "./run_server.sh Rooms" for each server (i.e. Flights for terminal 1)

### Run the middleware
- open another terminal
- change to the /Server directory
- run "make" to compile the Server
- then run "./run_middleware.sh machine1 machine2 machine 3" where machine1-flights machine2-cars machine3-rooms

### Run the client
- open another terminal
- change to the /Client directory
- run "make"
- run "./run_client.sh host" where host is the host address of the middleware machine

## Fouad Commands
- list the running processes and their pid
    lsof -i -n -P | grep TCP 
- kill process 
    kill -9 pid

## Notes
- changed the port because it was in use - to 1095



