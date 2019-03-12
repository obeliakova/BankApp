package ru.client;

/**
 * ru.client.Client can:
 *  - withdraw money from the account
 *  - put the money into an account
 * *- make a payment
 */
public class Client {
    private String firstname;
    private String lastname;
    private Card card;


    public Integer withdrawMoneyFromAccount(String cardNumber, Integer sum){

        return 0;
    }

    public Boolean putMoneyOnAccount(String cardNumber, Integer sum){

        return true;
    }


}
