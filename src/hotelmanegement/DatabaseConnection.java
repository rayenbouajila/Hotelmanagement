package hotelmanegement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521/freexdb";
    private static final String DB_USER = "SYS as SYSDBA";
    private static final String DB_PASSWORD = "rayen2004";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            
            java.util.Properties props = new java.util.Properties();
            props.put("user", DB_USER);
            props.put("password", DB_PASSWORD);
            props.put("internal_logon", "sysdba");
            
            return DriverManager.getConnection(DB_URL, props);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC Driver not found", e);
        }
    }
}