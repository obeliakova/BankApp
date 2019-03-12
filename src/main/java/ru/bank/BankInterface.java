package ru.bank;

import ru.client.Client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BankInterface extends Remote{

  String NAME = "Bank";

  public Boolean checkPin(String cardNumber, String pin) throws RemoteException, SQLException;

  public Boolean checkCardNumber(String cardNumber) throws RemoteException, SQLException;

  public Client getClientInfo() throws RemoteException;

  public Boolean checkMoneyAmountOnUserAccount() throws RemoteException;

  public List<HashMap<String,Object>> getAmountsOfBanknotes() throws RemoteException, SQLException;
}