package com.example.messengerv01;

//chat class with only field "chatName", used for creating new chats in MainActivity
public class ChatClass {
    private String chatName;

    ChatClass(){}
    ChatClass( String chatName) {
        this.chatName = chatName;
    }

    public String getChatName() {
        return this.chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }


}
