/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ksfx.web.services.dbmigration.impl;

import ch.ksfx.web.services.dbmigration.DbMigration;
import com.carbonfive.db.migration.DataSourceMigrationManager;
import io.ebean.Ebean;
import io.ebean.Transaction;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;


public class DbMigrationImpl implements DbMigration
{
    private Logger logger;

    public DbMigrationImpl(Logger logger) throws Exception
    {
        this.logger = logger;
    }

    @Override
    public void updateAllChangelogs() throws Exception
    {
        logger.info("Updating changelogs");

        try {
            ClasspathMigrationResolver rmr = new ClasspathMigrationResolver("dbmigration/");

            DataSourceMigrationManager migrationManager = new DataSourceMigrationManager(
                    new SimpleDataSource());
            migrationManager.setMigrationResolver(rmr);
            migrationManager.migrate();
        } catch (RuntimeException ex) {
            logger.error("Error while executing migrations", ex);
        }

        logger.info("Changelogs updated");
    }

    private class SimpleDataSource implements DataSource
    {
        public SimpleDataSource()
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
}
