package Server.TransactionManager;


import Server.TransactionManager.Message.MessageType;

import java.util.Date;
import java.util.concurrent.*;

public class TimeToLiveMechanism extends Thread {

    protected int transactionId;
    protected Date latest_operation_date = new Date();
    protected static int TIMEOUT = 1000000;

    BlockingQueue<Message> in_queue = new LinkedBlockingQueue<Message>();
    BlockingQueue<Message> out_queue = new LinkedBlockingQueue<Message>();

    public TimeToLiveMechanism(int xid) {
        transactionId = xid;
    }

    //if the transaction is committed or aborted, then this thread is terminated
    public void run() {

        long timeBlocked = 0;
        Thread thisThread = Thread.currentThread();

        
        try {
            while(true) {
                synchronized (thisThread) {

                    thisThread.wait(TimeToLiveMechanism.TIMEOUT - timeBlocked);

                    //transaction succeeeded, close the timer
                    if(!in_queue.isEmpty() && (in_queue.poll().getMessage() == MessageType.CLOSE_TIMER)) {
                        break;
                    } 

                    //transaction operation occured, reset the timer
                    else if(!in_queue.isEmpty() && (in_queue.poll().getMessage() == MessageType.RESET_TIMER)) {
                        latest_operation_date = new Date();
                    } 

                    //no message, transaction timed out, send message to abort
                    else {
                        Date currTime = new Date();
                        timeBlocked = currTime.getTime() - latest_operation_date.getTime();
                        // Check if the transaction has been waiting for a period greater than the timeout period 
                        if (timeBlocked >= TimeToLiveMechanism.TIMEOUT) {
                            //communicate to abort the transaction
                            out_queue.add(new Message(MessageType.ABORT_TRANSACTION));
                            break;
                            
                        }
                    } 
                } 
            }
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted");
        }
    }
    
}
