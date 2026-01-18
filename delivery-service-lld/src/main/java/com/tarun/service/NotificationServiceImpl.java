package com.tarun.service;

public class NotificationServiceImpl implements NotificationService {

    /**
     * Sends an email notification (mock implementation - prints to console)
     */
    @Override
    public void sendEmail(String recipient, String subject, String message) {
        System.out.println("\n[EMAIL] To: " + recipient + " | Subject: " + subject + " | Message: " + message);
    }

    /**
     * Sends an SMS notification (mock implementation - prints to console)
     */
    @Override
    public void sendSMS(String phoneNumber, String message) {
        System.out.println("\n[SMS] To: " + phoneNumber + " | Message: " + message);
    }
}