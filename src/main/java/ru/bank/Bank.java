package ru.bank;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.client.Client;
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

    public Client getClientInfo() throws RemoteException {
        return null;
    }

    public Boolean checkMoneyAmountOnUserAccount() throws RemoteException {
        return null;
    }

    public List<HashMap<String, Object>> getAmountsOfBanknotes() throws RemoteException, SQLException {
        DataHandler dh = new DataHandler();
        List<HashMap<String, Object>> list = dh.getAmountsOfBanknotesFromDB();
        if (list == null) {
            LOG.error("An error has occurred in banknote counting method");
        }
        return list;
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
