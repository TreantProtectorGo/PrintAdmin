package com.ethan.printadmin.exception;

public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException() { super("This print job would exceed the user's monthly page quota."); }
}
