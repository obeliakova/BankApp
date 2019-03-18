package ru.bank;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.db.DataHandler;


public class Bank extends UnicastRemoteObject implements BankInterface {

    private static final Logger LOG = LogManager.getLogger(ru.bank.Bank.class.getName());

    public Bank() throws RemoteException {
    }

    public Boolean checkPin(String cardNumber, String enteredPin) throws RemoteException, SQLException {
        DataHandler dh = new DataHandler();
        String pinFromDB = dh.getPinForUserCardFromDB(cardNumber);
        if (pinFromDB.equals(enteredPin)) {
            LOG.info("Correct pin was entered");
            return true;
        } else {
            LOG.error("Incorrect pin was entered");
            return false;
        }
    }

    public Boolean checkCardNumber(String cardNumber) throws RemoteException, SQLException {
        DataHandler dh = new DataHandler();
        Integer count = dh.getCountCardFromDB(cardNumber);
        if (count == 1) {
            LOG.info("It is card our bank");
            return true;
        } else {
            LOG.error("It is card not our bank");
            return false;
        }
    }

    public Boolean checkValidDateCard(String cardNumber) throws RemoteException, SQLException, ParseException {
        DataHandler dh = new DataHandler();
        Timestamp validDate = dh.getValidDateCard(cardNumber);
        return validDate.compareTo(new Timestamp(System.currentTimeMillis())) >= 0;
    }

    public Float getMoneyAmountOnUserAccount(String cardNumber) throws RemoteException, SQLException {
        DataHandler dh = new DataHandler();
        return Float.parseFloat(dh.getAccountBalance(cardNumber));
    }

    public Boolean updateBalanceAccount(String cardNumber, String sum, String operation) throws RemoteException, SQLException {
        if (!operation.equals("+") && !operation.equals("-")){
            LOG.error("Incorrect parameter \"operation\"");
            return false;
        }
        DataHandler dh = new DataHandler();
        Float balanceBeforeChanges = Float.parseFloat(dh.getAccountBalance(cardNumber));
        if (!dh.updateAccountBalance(cardNumber, sum, operation)){
            return false;
        }
        Float balanceAfterChanges = Float.parseFloat(dh.getAccountBalance(cardNumber));
        switch (operation){
            case "+":
                if (balanceAfterChanges.compareTo(balanceBeforeChanges + Float.parseFloat(sum))!= 0){
                    LOG.error("Balance after update does not match with balance before update plus the sum");
                    return false;
                }
                break;
            case "-":
                if (balanceAfterChanges.compareTo(balanceBeforeChanges - Float.parseFloat(sum))!= 0){
                    LOG.error("Balance after update does not match with balance before update minus the sum");
                    return false;
                }
                break;
            default:
                LOG.error("Error of operation");
                return false;
        }

        return true;
    }

    public HashMap<Integer, Integer> getAmountsOfBanknotes() throws RemoteException, SQLException {
        DataHandler dh = new DataHandler();
        HashMap<Integer, Integer> list = dh.getAmountsOfBanknotes();
        if (list == null) {
            LOG.error("An error has occurred in banknote counting method");
        }
        return list;
    }

    public Boolean updateBanknoteCount(Integer type, Integer count) throws RemoteException, SQLException {
        DataHandler dh = new DataHandler();
        if (!dh.updateBanknoteCount(type.toString(), count.toString())){
            return false;
        }
        return true;
    }


    public static void main(String[] args) {
        try {
            Registry localReg = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            localReg.rebind(BankInterface.NAME, new Bank());

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

}
