package com.example.easywedding.model;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Map;

@IgnoreExtraProperties
// POJO that represent a message in a chat.
public class User {

    private String displayName;
    private String email;
    private String dataId;
    private String owner;

    //required for db
    public User(){

    }
    public User(String displayName, String email, String dataId){
        this.displayName = displayName;
        this.email = email;
        this.dataId = dataId;
    }
    public User(String displayName, String email,String dataId, String owner){
        this.displayName = displayName;
        this.email = email;
        this.dataId = dataId;
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwnerE(String owner) {
        this.owner = owner;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
