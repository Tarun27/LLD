package com.tarun.service;

public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendEmail(String recipient, String subject, String message) {
        System.out.println("[EMAIL] To: " + recipient + " | Subject: " + subject + " | Message: " + message);
    }

    @Override
    public void sendSMS(String phoneNumber, String message) {
        System.out.println("[SMS] To: " + phoneNumber + " | Message: " + message);
    }
}
