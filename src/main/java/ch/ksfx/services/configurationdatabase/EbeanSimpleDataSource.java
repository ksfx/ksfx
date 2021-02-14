package ch.ksfx.services.configurationdatabase;

import io.ebean.Ebean;
import io.ebean.Transaction;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class EbeanSimpleDataSource implements DataSource
{
    public EbeanSimpleDataSource()
    {

    }

    public Connection getConnection() throws SQLException
    {
        Transaction t = Ebean.getServer(null).createTransaction();

        Connection c = t.getConnection();
        c.setAutoCommit(true);

        return c;
    }

    public Connection getConnection(String string, String string1) throws SQLException
    {
        Transaction t = Ebean.getServer(null).createTransaction();
        Connection c = t.getConnection();
        c.setAutoCommit(true);

        return c;
    }

    public PrintWriter getLogWriter() throws SQLException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setLogWriter(PrintWriter writer) throws SQLException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setLoginTimeout(int i) throws SQLException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getLoginTimeout() throws SQLException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T unwrap(Class<T> type) throws SQLException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> type) throws SQLException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
