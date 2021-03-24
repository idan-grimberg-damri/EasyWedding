package com.example.easywedding.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.Objects;

/**
 * This class represents a wedding guest
 */
public class Guest implements Parcelable {

    private String name;
    private String phoneNumber;
    private int priority;
    private String joiners;
    private boolean arrive;
    private boolean invited;
    private String email;
    private boolean submitted; //submitted for arrival confirmation.


    public Guest() {
        // Required for db
    }

    public Guest(String name, String phoneNumber, int priority,
                String email,  boolean arrive, boolean invited, String joiners, boolean submitted) {
        this.name = name.trim();
        this.phoneNumber = phoneNumber;
        this.priority = priority;
        this.arrive = arrive;
        this.invited = invited;
        this.joiners = joiners;
        this.submitted = submitted;
        this.email = email;

    }
    public Guest(String name){
        this.name = name;
    }


    protected Guest(Parcel in) {
        name = in.readString();
        phoneNumber = in.readString();
        priority = in.readInt();
        joiners = in.readString();
        arrive = in.readByte() != 0;
        invited = in.readByte() != 0;
        email = in.readString();
        submitted = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(phoneNumber);
        dest.writeInt(priority);
        dest.writeString(joiners);
        dest.writeByte((byte) (arrive ? 1 : 0));
        dest.writeByte((byte) (invited ? 1 : 0));
        dest.writeString(email);
        dest.writeByte((byte) (submitted ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Guest> CREATOR = new Creator<Guest>() {
        @Override
        public Guest createFromParcel(Parcel in) {
            return new Guest(in);
        }

        @Override
        public Guest[] newArray(int size) {
            return new Guest[size];
        }
    };

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getJoiners() {
        return joiners;
    }

    public void setJoiners(String joiners) {
        this.joiners = joiners;
    }

    public boolean isInvited() {
        return invited;
    }

    public void setInvited(boolean invited) {
        this.invited = invited;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isArrive() {
        return arrive;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setArrive(boolean arrive) {
        this.arrive = arrive;
    }

}
