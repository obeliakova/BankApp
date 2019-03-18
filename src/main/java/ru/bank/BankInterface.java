package ru.bank;

import ru.client.Client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

public interface BankInterface extends Remote{

  String NAME = "Bank";

  public Boolean checkPin(String cardNumber, String pin) throws RemoteException, SQLException;

  public Boolean checkCardNumber(String cardNumber) throws RemoteException, SQLException;

  public Boolean checkValidDateCard(String cardNumber) throws RemoteException, SQLException, ParseException;

  public Boolean updateBalanceAccount(String cardNumber, String sum, String operation) throws RemoteException, SQLException;

  public Boolean updateBanknoteCount(Integer type, Integer count) throws RemoteException, SQLException;

  public Float getMoneyAmountOnUserAccount(String cardNumber) throws RemoteException, SQLException;

  public HashMap<Integer, Integer> getAmountsOfBanknotes() throws RemoteException, SQLException;
}