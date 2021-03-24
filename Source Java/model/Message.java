package com.example.easywedding.model;



import java.util.Date;

/**
 * This class represents a chat message
 */
public class Message {

    private String text;
    private String name;
    private String senderId;
    private Long time;
    private String photoUrl;


    //required for db
    public Message(){}

    public Message(String text, String name,  Long time, String senderId, String photoUrl){
        this.text = text;
        this.name = name;
        this.time = time;
        this.senderId = senderId;
        this.photoUrl = photoUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Long getTime() {
        return time;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public void setTime(Long time) {
        this.time = time;
    }



    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }
}
