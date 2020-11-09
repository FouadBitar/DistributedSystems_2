all: java.policy compile-server-rmi

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: Server/Interface/IResourceManager.java Server/Interface/IMiddleware.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/IResourceManager.java Server/Interface/IMiddleware.java
	jar cvf RMIInterface.jar Server/Interface/IResourceManager.class Server/Interface/IMiddleware.class

MyExceptions.jar: Server/Exception/TransactionAbortedException.java Server/Exception/InvalidTransactionException.java
	@echo "Compiling exceptions"
	javac Server/Exception/TransactionAbortedException.java Server/Exception/InvalidTransactionException.java
	jar cvf MyExceptions.jar Server/Exception/TransactionAbortedException.class Server/Exception/InvalidTransactionException.class

compile-server-rmi: Server/RMI/*.java Server/Interface/*.java Server/Common/*.java Server/LockManager/*.java Server/TransactionManager/*.java Server/Exception/*.java
	javac Server/RMI/*.java Server/Interface/*.java Server/Common/*.java Server/LockManager/*.java Server/TransactionManager/*.java Server/Exception/*.java

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class Server/LockManager/*.class Server/TransactionManager/*.class Server/Exception/*.class
	rm -f RMIInterface.jar
	rm -f MyExceptions.jar
	rm -f java.policy 
