package Server.LockManager;

/* The transaction requested a lock that it already had. */ 

public class RedundantLockRequestException extends Exception
{
	private static final long serialVersionUID = 1L;
	protected int m_xid = 0;

	public RedundantLockRequestException(int xid, String msg)
	{
		super(msg);
		m_xid = xid;
	}

	public int getXId()
	{
		return m_xid;
	}
}
