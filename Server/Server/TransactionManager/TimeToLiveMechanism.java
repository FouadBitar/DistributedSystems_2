package Server.TransactionManager;

import Server.Common.*;

import java.rmi.RemoteException;
import java.util.Date;

public class TimeToLiveMechanism extends Thread {

    //every time a transaction is started
    //  a timetolivemechanism object starts a thread that counts
    //  once the timeout is reached it then sends a message to the resource manager to abort the transaction
    //and every time a transaction has an operation
    //  the timer is reset
    protected int transactionId;
    protected boolean stopTimeout = false;
    protected ResourceManager rm = null;
    protected Date latest_operation_date = new Date();
    protected static int TIMEOUT = 10000;

    public TimeToLiveMechanism(int xid, ResourceManager rm) {
        if(xid > 0) {
            transactionId = xid;
            this.rm = rm;
        }
    }

    public void refreshTimer() {
        latest_operation_date = new Date();
    }

    public void stopTimeout() {
        stopTimeout = true;
        System.out.println("entered stop timeout for " + Thread.currentThread().hashCode());
    }

    //if the transaction is committed or aborted, then this thread is terminated
    public void run() {

        long timeBlocked = 0;
        Thread thisThread = Thread.currentThread();
        //when the timeout is reached, break and abort the transaction
        
            
        try {
            while(!stopTimeout) {
                synchronized (thisThread) {
                    thisThread.wait(TimeToLiveMechanism.TIMEOUT - timeBlocked);
                   
                    //check if timeout was stopped alread
                    if(stopTimeout) break;


                    Date currTime = new Date();
                    timeBlocked = currTime.getTime() - latest_operation_date.getTime();
                    // Check if the transaction has been waiting for a period greater than the timeout period 
                    if (timeBlocked >= TimeToLiveMechanism.TIMEOUT) {
                        //abort the transaction and stop the timeout
                        stopTimeout = true;
                        try {
                            if(rm == null) System.out.println("the rm is null");
                            rm.abort(transactionId);
                            break;
                        } 
                        catch (RemoteException er) {} 
                        catch (InvalidTransactionException ei) {}
                        
                    }
                } 
            }
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted");
        }
    }
    
}
