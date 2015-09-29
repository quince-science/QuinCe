package uk.ac.exeter.QuinCe.web.system;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

/**
 * Utility class for handling connection pools
 * @author Steve Jones
 *
 */
public class Pools implements ServletContextListener {

	private static final String ATTRIBUTE_NAME = "pools";
	
	private DataSource dbDataSource;
	
	@Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        String databaseName = servletContext.getInitParameter("database.name");
        try {
        	dbDataSource = (DataSource) new InitialContext().lookup(databaseName);
        } catch (NamingException e) {
            throw new RuntimeException("Config failed: datasource not found", e);
        }
        servletContext.setAttribute(ATTRIBUTE_NAME, this);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // NOOP.
    }

    public DataSource getDBDataSource() {
        return dbDataSource;
    }

    public static Pools getInstance(ServletContext servletContext) {
        return (Pools) servletContext.getAttribute(ATTRIBUTE_NAME);
    }
}
