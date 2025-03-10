package com.business.techassist.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class ChatroomModel {
    String chatroomID;
    List<String> userIDs;
    Timestamp lastMessageTimestamp;
    String lastMessageSenderID;

    public ChatroomModel() {
    }

    public ChatroomModel(String chatroomID, List<String> userIDs, Timestamp lastMessageTimestamp, String lastMessageSenderID) {
        this.chatroomID = chatroomID;
        this.lastMessageSenderID = lastMessageSenderID;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.userIDs = userIDs;
    }

    public String getChatroomID() {
        return chatroomID;
    }

    public void setChatroomID(String chatroomID) {
        this.chatroomID = chatroomID;
    }

    public String getLastMessageSenderID() {
        return lastMessageSenderID;
    }

    public void setLastMessageSenderID(String lastMessageSenderID) {
        this.lastMessageSenderID = lastMessageSenderID;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public List<String> getUserIDs() {
        return userIDs;
    }

    public void setUserIDs(List<String> userIDs) {
        this.userIDs = userIDs;
    }
}
