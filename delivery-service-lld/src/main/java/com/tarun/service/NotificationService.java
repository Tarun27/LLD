package com.tarun.service;

public interface NotificationService {
    void sendEmail(String recipient, String subject, String message);
    void sendSMS(String phoneNumber, String message);
}
