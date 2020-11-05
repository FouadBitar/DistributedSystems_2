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

## Fouad Commands
- list the running processes and their pid
    lsof -i -n -P | grep TCP 
- kill process 
    kill -9 pid

## Notes
- changed the port because it was in use - to 1095



