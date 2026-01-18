package com.tarun.service;

/**
 * Interface for sending notifications to customers and drivers
 */
public interface NotificationService {
    /**
     * Sends an email notification
     */
    void sendEmail(String recipient, String subject, String message);

    /**
     * Sends an SMS notification
     */
    void sendSMS(String phoneNumber, String message);
}
