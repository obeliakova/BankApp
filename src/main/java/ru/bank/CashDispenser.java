package ru.bank;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.client.Card;
import ru.client.Client;

import javax.print.attribute.HashPrintJobAttributeSet;

public class CashDispenser {

    private static final Logger LOG = LogManager.getLogger(CashDispenser.class.getName());

    private final int MAX_NUMBER_OF_BANKNOTES = 40;
    private final int MULTIPLICITY = 100;
    private final int COUNT_PIN_ATTEMPTS = 3;
    private HashMap<Integer,Integer> banknotes;
    private Boolean isWaitingState;

    public CashDispenser(BankInterface bank) throws RemoteException, SQLException {
        this.isWaitingState = true;
        this.banknotes = bank.getAmountsOfBanknotes();
    }

    private Boolean isPinValid(Scanner scanner, BankInterface bank, String cardNumber) throws RemoteException, SQLException {
        int i = 1;
        Boolean checkPinResult = false;
        Boolean cancelOperation = false;
        do {
            System.out.print("Input pin or press 0 to cancel: ");
            String pin = scanner.nextLine();
            if (!pin.equals("0")){
                checkPinResult = bank.checkPin(cardNumber, pin);
                if (!checkPinResult) {
                    LOG.error("Incorrect pin!");
                }
                i++;
            }
            else {
                cancelOperation = true;
            }
        } while(i <= COUNT_PIN_ATTEMPTS && !checkPinResult && !cancelOperation);
        return checkPinResult;
    }

    private Boolean isValidCard(BankInterface bank, String cardNumber) throws ParseException, RemoteException, SQLException {
        if(!bank.checkValidDateCard(cardNumber)){
            LOG.error("Your card is not valid");
            return false;
        } else {
            return true;
        }
    }

    private Boolean putMoneyToAccount(Scanner scanner, BankInterface bank, String cardNumber) throws ParseException, RemoteException, SQLException {
        Boolean isSumCorrect;
        String sum = "";
        do {
            isSumCorrect = true;
            System.out.print("Put money: input sum of money (enter 0 for cancel): ");
            sum = scanner.nextLine();
            if(!sum.equals("0")) {
                if (!sum.replaceAll("\\s", "").matches("[0-9]{0,6}") || sum.equals("")) {
                    LOG.error("Incorrect sum");
                    isSumCorrect = false;
                } else {
                    if (!bank.updateBalanceAccount(cardNumber, sum, "+")) {
                        LOG.error("Error in update of balance");
                        return false;
                    }
                    LOG.info("Operation completed successfully");
                }
            }
        } while (!isSumCorrect && !sum.equals("0"));
        return true;
    }

    private HashMap<Integer, Integer> exchangeMoney(List<Integer> sortedBanknotes, Integer amount){
        int indexOfStartBanknote = sortedBanknotes.size() - 1;
        while (indexOfStartBanknote != 0 && sortedBanknotes.get(indexOfStartBanknote) > amount){
                indexOfStartBanknote = indexOfStartBanknote - 1; //find min available banknote
        }

        int minBanknote = sortedBanknotes.get(0);
        HashMap<Integer,Integer> neededBanknotes = new HashMap<Integer, Integer>();

        while (indexOfStartBanknote >= 0){
            int sum = amount;
            int banknotesCounter = 0;

            for (int indexOfCurBanknote = indexOfStartBanknote; (indexOfCurBanknote >= 0) && (sum > 0); --indexOfCurBanknote){

                int curBanknote = sortedBanknotes.get(indexOfCurBanknote);
                int maxNumberOfBanknotes = (int)(sum/curBanknote);

                if (this.banknotes.get(curBanknote) - maxNumberOfBanknotes < 0)
                    maxNumberOfBanknotes = this.banknotes.get(curBanknote);

                int remainSum = sum - (curBanknote * maxNumberOfBanknotes);

                if (0 < remainSum && remainSum < minBanknote)
                    maxNumberOfBanknotes = maxNumberOfBanknotes - 1;

                if (maxNumberOfBanknotes > 0){
                    neededBanknotes.put(curBanknote,maxNumberOfBanknotes);
                    sum = sum - (curBanknote * maxNumberOfBanknotes);
                    banknotesCounter = banknotesCounter + maxNumberOfBanknotes;
                }
            }

            if ((sum != 0) || (banknotesCounter > this.MAX_NUMBER_OF_BANKNOTES)){
                neededBanknotes.clear();
                indexOfStartBanknote = indexOfStartBanknote - 1;
            }
            else
                return neededBanknotes;
        }
        return null;
    }

    private Boolean getMoneyFromAccount(Scanner scanner, BankInterface bank, String cardNumber) throws RemoteException, SQLException {
        Boolean isSumCorrect;
        String sum = "";
        do {
            isSumCorrect = true;
            System.out.print("Get money: input sum of money (enter 0 for cancel): ");
            sum = scanner.nextLine();
            if (!sum.equals("0")) {
                if (!sum.replaceAll("\\s", "").matches("[0-9]{0,6}") || sum.equals("")) {
                    LOG.error("Incorrect sum");
                    isSumCorrect = false;
                } else {

                    if (Integer.parseInt(sum) % this.MULTIPLICITY != 0) {
                        LOG.error("Incorrect sum. Sum should be a multiple of " + String.valueOf(this.MULTIPLICITY));
                        isSumCorrect = false;
                    } else {

                        Float balance = bank.getMoneyAmountOnUserAccount(cardNumber);
                        if (balance.compareTo(Float.parseFloat(sum)) < 0) {
                            LOG.error("Insufficient funds");
                            isSumCorrect = false;
                        } else {
                            List sortedBanknotes = new ArrayList(this.banknotes.keySet());
                            Collections.sort(sortedBanknotes);

                            HashMap<Integer, Integer> neededBanknotes = exchangeMoney(sortedBanknotes, Integer.parseInt(sum));
                            if (neededBanknotes == null) {
                                LOG.error("Error in calculating required banknotes");
                                return false;
                            } else {
                                neededBanknotes.forEach((t, c) -> {
                                    try {
                                        bank.updateBanknoteCount(t, c);
                                    } catch (RemoteException | SQLException e) {
                                        e.printStackTrace();
                                    }
                                });

                            }
                            if (!bank.updateBalanceAccount(cardNumber, sum, "-")) {
                                LOG.error("Error in update of balance");
                                return false;
                            }
                            LOG.info("Operation completed successfully");
                        }
                    }
                }
            }
        } while (!isSumCorrect && !sum.equals("0"));
        return true;
    }


    public static void main(String[] args) throws RemoteException, NotBoundException {
        try (Scanner scanner = new Scanner(System.in);){
            Registry reg = LocateRegistry.getRegistry("localhost");
            BankInterface bank = (BankInterface) reg.lookup(BankInterface.NAME);
            CashDispenser cashDispenser = new CashDispenser(bank);
            String cardNumber = "";

            while (cashDispenser.isWaitingState) {

                System.out.println("Welcome!");

                //Check card number
                System.out.print("Input card number without spaces: ");
                cardNumber = scanner.nextLine();
                if (!cardNumber.replaceAll("\\s","").matches("[0-9]{16}")) {
                    LOG.error("Incorrect card number. Number should contain only 16 digits.");
                }
                else {
                    Client client = new Client(new Card(cardNumber));
                    if (!bank.checkCardNumber(client.getCard().getNumber())) {
                        LOG.error("Your card is card other bank. Session couldn't be continued. Good bye!");
                    } else {
                        //Check PIN
                        if (cashDispenser.isPinValid(scanner, bank, client.getCard().getNumber())){
                            //Check data valid of card
                            if (cashDispenser.isValidCard(bank, client.getCard().getNumber())){
                                client.getCard().setIsValid(true);
                                String operation = "";
                                do {
                                    System.out.println("Enter number of operation:\n" +
                                            "0 - end the session\n" +
                                            "1 - get money\n" +
                                            "2 - put money");
                                    operation = scanner.nextLine();
                                    switch (operation) {
                                        case ("0"):
                                            cashDispenser.isWaitingState = true;
                                            break;
                                        case ("1"):
                                            cashDispenser.getMoneyFromAccount(scanner, bank, client.getCard().getNumber());
                                            break;
                                        case ("2"):
                                            cashDispenser.putMoneyToAccount(scanner, bank, client.getCard().getNumber());
                                            break;
                                        default:
                                            LOG.error("Incorrect code of the operation");
                                            break;
                                    }
                                } while (!operation.equals("0"));
                            }
                            else{
                                cashDispenser.isWaitingState = true;
                            }
                        } else {
                            cashDispenser.isWaitingState = true;
                        }
                    }
                }
            }

        } catch (SQLException | ParseException e) {
            e.printStackTrace();
        }
    }
}
