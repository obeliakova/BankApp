package ru.client;

public class Card {
    private String number;
    private Boolean isValid;

    public Card(String number) {
        this.number = number;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
