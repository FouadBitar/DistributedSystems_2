package Server.Exception;

public class InvalidTransactionException extends Exception {

	private static final long serialVersionUID = 1L;
    
    private int m_xid = 0;

	public InvalidTransactionException(int xid, String msg)
	{
		super("The transaction " + xid + " is invalid:" + msg);
		m_xid = xid;
	}

	int getXId()
	{
		return m_xid;
	}
}
