package com.example.easywedding.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

/**
 * This class represents a wedding feature
 */
public class Feature implements Parcelable {

    private String name;
    private String supplierName;
    private String supplierPhone;
    private String supplierEmail;
    private String advancePayment;
    private String paymentBalance;
    private String freeText;
    private String quantity;

    public Feature(){
// required for db
    }
    public Feature(String name, String freeText, String advancePayment
            , String paymentBalance, String quantity
            , String supplierName, String supplierEmail
            , String supplierPhone ) {
        this.name = name;
        this.supplierName = supplierName.trim();
        this.supplierEmail = supplierEmail;
        this.supplierPhone = supplierPhone;
        this.freeText = freeText;
        this.advancePayment = advancePayment;
        this.paymentBalance = paymentBalance;
        this.quantity = quantity;
    }


    protected Feature(Parcel in) {
        name = in.readString();
        supplierName = in.readString();
        supplierPhone = in.readString();
        supplierEmail = in.readString();
        advancePayment = in.readString();
        paymentBalance = in.readString();
        freeText = in.readString();
        quantity = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(supplierName);
        dest.writeString(supplierPhone);
        dest.writeString(supplierEmail);
        dest.writeString(advancePayment);
        dest.writeString(paymentBalance);
        dest.writeString(freeText);
        dest.writeString(quantity);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Feature> CREATOR = new Creator<Feature>() {
        @Override
        public Feature createFromParcel(Parcel in) {
            return new Feature(in);
        }

        @Override
        public Feature[] newArray(int size) {
            return new Feature[size];
        }
    };

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierPhone() {
        return supplierPhone;
    }

    public void setSupplierPhone(String supplierPhone) {
        this.supplierPhone = supplierPhone;
    }

    public String getSupplierEmail() {
        return supplierEmail;
    }

    public void setSupplierEmail(String supplierEmail) {
        this.supplierEmail = supplierEmail;
    }

    public String getName() {
        return name;
    }

    public String getQuantity() {
        return quantity;
    }


    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }


    public String getFreeText() {
        return freeText;
    }


    public String getAdvancePayment() {
        return advancePayment;
    }

    public String getPaymentBalance() {
        return paymentBalance;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFreeText(String freeText) {
        this.freeText = freeText;
    }


    public void setAdvancePayment(String advancePayment) {
        this.advancePayment = advancePayment;
    }

    public void setPaymentBalance(String paymentBalance) {
        this.paymentBalance = paymentBalance;
    }

    @Exclude
    public String[] arrayValues(){
        return new String[] {name, supplierName, supplierPhone, supplierEmail,
        advancePayment, paymentBalance, quantity, freeText};

    }
}
