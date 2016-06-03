package com.github.elifarley.kotlin

import java.io.Serializable;

public class EmailMessageOriginal implements Serializable {

    private final String toAddress;
    private final String subject;
    private final String body;
    private String myMutableField;

    public EmailMessageOriginal(String toAddress, String subject, String body, String myMutableField) {
        this.toAddress = toAddress;
        this.subject = subject;
        this.body = body;
        this.myMutableField = myMutableField;
    }

    public String getToAddress() {
        return toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getMyMutableField() {
        return myMutableField;
    }

    public void setMyMutableField(String newValue) {
        myMutableField = newValue;
    }

    @Override
    public String toString() {
        return "EmailMessageOriginal{" +
                "subject='" + subject + '\'' +
                ", toAddress='" + toAddress + '\'' +
                ", body='" + body + '\'' +
                ", myMutableField='" + myMutableField + '\'' +
                '}';
    }

    public String component1() {
        return toAddress;
    }

    public String component2() {
        return subject;
    }

    public String component3() {
        return body;
    }

    public String component4() {
        return myMutableField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EmailMessageOriginal that = (EmailMessageOriginal) o;

        if (toAddress != null ? !toAddress.equals(that.toAddress) : that.toAddress != null) return false;
        if (subject != null ? !subject.equals(that.subject) : that.subject != null) return false;
        if (body != null ? !body.equals(that.body) : that.body != null) return false;
        return myMutableField != null ? myMutableField.equals(that.myMutableField) : that.myMutableField == null;

    }

    @Override
    public int hashCode() {
        int result = toAddress != null ? toAddress.hashCode() : 0;
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (myMutableField != null ? myMutableField.hashCode() : 0);
        return result;
    }

    public EmailMessageOriginal copy(String toAddress, String subject, String body, String myMutableField) {
        return new EmailMessageOriginal(toAddress == null ? this.toAddress : toAddress,
                subject == null ? this.subject : subject, body == null ? this.body : body,
                myMutableField == null ? this.myMutableField : myMutableField);
    }

}
