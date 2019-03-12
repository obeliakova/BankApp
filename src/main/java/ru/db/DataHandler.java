package ru.db;

import java.sql.*;
import java.util.*;

import oracle.jdbc.pool.OracleDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataHandler {
    private static final Logger LOG = LogManager.getLogger(ru.db.DataHandler.class.getName());
    private String jdbcUrl;
    private String userId;
    private String password;

    public DataHandler() {
        jdbcUrl = "jdbc:oracle:thin:@localhost:1521/xe";
        userId = "BANKUSER";
        password = "123456";
    }

    private Connection getDBConnection() throws SQLException {
        Connection conn = null;
        try {
            Locale.setDefault(Locale.ENGLISH);
            OracleDataSource ds = new OracleDataSource();
            ds.setURL(jdbcUrl);
            conn = ds.getConnection(userId, password);
            if (conn == null){
                LOG.error("Cannot create connection to DB: " + jdbcUrl);
            }
            LOG.info("Connection to DB: " + jdbcUrl + " was created");
            return conn;
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return conn;
        }
    }

    private List<HashMap<String,Object>> executeQuery(String query) throws SQLException {
        try(Connection conn = getDBConnection();
            Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rset = stmt.executeQuery(query)){

            LOG.info("Query to DB: " + query);

            ResultSetMetaData md = rset.getMetaData();
            int columns = md.getColumnCount();
            List<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();

            while (rset.next()) {
                HashMap<String,Object> row = new HashMap<String, Object>(columns);
                for(int i=1; i<=columns; ++i) {
                    row.put(md.getColumnName(i),rset.getObject(i));
                }
                list.add(row);
            }
            return list;
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return null;
        }
    }

    public List<HashMap<String,Object>> getAmountsOfBanknotesFromDB() throws SQLException {
        String query = "SELECT * FROM bank_banknotes";
        return executeQuery(query);
    }

    public String getPinForUserCardFromDB(String cardNumber) throws SQLException {
        String query = "SELECT pin FROM bank_cards WHERE card_num = '"+ cardNumber +"'";
        List<HashMap<String,Object>> result = executeQuery(query);
        String pin = result.get(0).get("PIN").toString();
        return pin;
    }

    public Integer getCountCardFromDB(String cardNumber) throws SQLException {
        String query = "SELECT COUNT(*) FROM bank_cards WHERE card_num = '"+ cardNumber +"'";
        List<HashMap<String,Object>> result = executeQuery(query);
        Integer count = Integer.parseInt(result.get(0).get("COUNT(*)").toString());
        return count;
    }

}
