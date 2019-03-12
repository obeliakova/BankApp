package ru.bank;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CashDispenser {

    private static final Logger LOG = LogManager.getLogger(CashDispenser.class.getName());

    private final int MAX_NUMBER_OF_BANKNOTES = 40;
    private final int MULTIPLICITY = 100;
    private List<HashMap<String,Object>> banknotesMap;
    private final static int countOfInputPinAttempts = 3;
    private Boolean isWaitingState;

    public CashDispenser() {
        isWaitingState = false;
    }

    public Boolean giveMoney(String cardNumber, Integer sum) {
        return true;
    }

    public String getResultOfOperation() {
        return "";
    }

    public Boolean checkEnoughBanknotes() {
        return true;
    }

    private static void checkPIN(Scanner scanner, BankInterface bank, String cardNumber) throws RemoteException, SQLException {
        int i = 1;
        Boolean checkPinResult = false;
        do {
            System.out.print("Input pin:");
            String pin = scanner.next();
            checkPinResult = bank.checkPin(cardNumber, pin);
            if (!checkPinResult) {
                LOG.error("Incorrect pin!");
            }
            i++;
        } while(i < countOfInputPinAttempts && !checkPinResult);
    }


    public static void main(String[] args) throws RemoteException, NotBoundException {
        Scanner scanner = new Scanner(System.in);
        try {
            Registry reg = LocateRegistry.getRegistry("localhost");
            BankInterface bank = (BankInterface) reg.lookup(BankInterface.NAME);
            CashDispenser cashDispenser = new CashDispenser();
            String cardNumber = "";

            //String txt = JOptionPane.showInputDialog("Input your pin:");
            //Boolean response = bank.checkPin(txt);
            //JOptionPane.showMessageDialog(null, response);

            while (!cashDispenser.isWaitingState) {

                System.out.println("Welcome!");

                //Check card number
                System.out.print("Input card number without spaces:");
                cardNumber = scanner.next();
                if (!cardNumber.matches("[0-9]{16}")) {
                    LOG.error("Incorrect card number. Number should contain only 16 digits.");
                }
                else {
                    if (!bank.checkCardNumber(cardNumber)) {
                        LOG.error("Your card is card other bank. Session couldn't be continued. Good bye!");
                    } else {
                        cashDispenser.isWaitingState = true;
                    }
                }
            }

            //Check PIN
            checkPIN(scanner, bank, cardNumber);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
