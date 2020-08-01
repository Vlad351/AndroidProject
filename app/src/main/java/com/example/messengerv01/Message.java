package com.example.messengerv01;


import java.util.Date;


//Message class used for sending new message in Chat class
public class Message {
    private String userName;
    private String textMessage;
    private long messageTime;

    public Message(){}

    public Message(String userName, String textMessage) {
        this.userName = userName;
        this.textMessage = textMessage;
        this.messageTime = new Date().getTime();
    }

    public String getUserName() {
        return userName;
    }


    public String getTextMessage() {
        return textMessage;
    }


    public long getMessageTime() {
        return messageTime;
    }

}

