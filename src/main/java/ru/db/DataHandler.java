package ru.db;

import oracle.jdbc.pool.OracleDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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

    private Boolean executeUpdate(String query) throws SQLException {
        try (Connection conn = getDBConnection();
             Statement stmt = conn.createStatement();) {

            int result = stmt.executeUpdate(query);
            LOG.info("Query to DB: " + query);
            if (result == 1){
                LOG.info("Successfully update");
                return true;
            } else{
                LOG.error("Update with error");
                return false;
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return false;
        }
    }


    public HashMap<Integer,Integer> getAmountsOfBanknotes() throws SQLException {
        String query = "SELECT * FROM bank_banknotes";
        List<HashMap<String,Object>> result = executeQuery(query);
        HashMap<Integer,Integer> banknotes = new HashMap<Integer, Integer>();
        if (result != null && result.size() != 0){
            result.forEach(b -> banknotes.put(Integer.parseInt(b.get("TYPE").toString()),
                                              Integer.parseInt(b.get("COUNT").toString())));
        }
        return banknotes;
    }

    public String getPinForUserCardFromDB(String cardNumber) throws SQLException {
        String query = "SELECT pin FROM bank_cards WHERE card_num = '"+ cardNumber +"'";
        List<HashMap<String,Object>> result = executeQuery(query);
        String pin = "";
        if (result != null && result.size() != 0)
            pin = result.get(0).get("PIN").toString();
        return pin;
    }

    public Integer getCountCardFromDB(String cardNumber) throws SQLException {
        String query = "SELECT COUNT(*) FROM bank_cards WHERE card_num = '"+ cardNumber +"'";
        List<HashMap<String,Object>> result = executeQuery(query);
        Integer count = 0;
        if (result != null && result.size() != 0)
            count = Integer.parseInt(result.get(0).get("COUNT(*)").toString());
        return count;
    }

    public Timestamp getValidDateCard(String cardNumber) throws SQLException, ParseException {
        String query = "SELECT valid_date FROM bank_cards WHERE card_num = '"+ cardNumber +"'";
        List<HashMap<String,Object>> result = executeQuery(query);
        Timestamp date = new Timestamp(System.currentTimeMillis());
        if (result != null && result.size() != 0) {
            date = Timestamp.valueOf(result.get(0).get("VALID_DATE").toString());
        }
        return date;
    }

    public String getAccountBalance(String cardNumber) throws SQLException {
        String query = "SELECT balance from bank_accounts " +
                        "WHERE account_num = (SELECT account_num FROM bank_cards where card_num = '"+ cardNumber +"')";
        List<HashMap<String,Object>> result = executeQuery(query);
        String balance = "";
        if (result != null && result.size() != 0)
            balance = result.get(0).get("BALANCE").toString();
        return balance;
    }

    public Boolean updateAccountBalance(String cardNumber, String sum, String operation) throws SQLException {
        String query = "UPDATE bank_accounts SET balance = balance " + operation + " " + sum +
                " WHERE account_num = (SELECT account_num FROM bank_cards where card_num = '"+ cardNumber + "')";
        return executeUpdate(query);
    }

    public Boolean updateBanknoteCount(String type, String count) throws SQLException {
        String query = "UPDATE bank_banknotes SET count = count - " + count + " WHERE type = " + type;
        return executeUpdate(query);
    }

}
