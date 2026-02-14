package com.tarun;

public class FileStorageSystemTest {
    public static void main(String[] args) {
        FileStorageSystem storage = new FileStorageSystem();
        System.out.println("--- FileStorageSystem Tests ---");

        // Test uploadFile
        boolean upload1 = storage.uploadFile("f1", "file1.txt", 100, 1);
        System.out.println("Upload file1: " + upload1);
        boolean upload2 = storage.uploadFile("f1", "file1.txt", 100, 1);
        System.out.println("Upload duplicate file1: " + upload2);
        boolean upload3 = storage.uploadFile("f2", "file2.txt", -10, 2);
        System.out.println("Upload file2 with negative size: " + upload3);

        // Test getFileSize
        System.out.println("File f1 size: " + storage.getFileSize("f1").orElse(-1));
        System.out.println("File f2 size (not uploaded): " + storage.getFileSize("f2").orElse(-1));

        // Test deleteFile
        boolean delete1 = storage.deleteFile("f1", 3);
        System.out.println("Delete file f1: " + delete1);
        boolean delete2 = storage.deleteFile("f1", 4);
        System.out.println("Delete file f1 again: " + delete2);

        // Test copyFile
        storage.uploadFile("f3", "file3.txt", 50, 5);
        System.out.println("Copy file f3 to f4: " + storage.copyFile("f3", "f4", 6).orElse(-1));
        System.out.println("Copy file f3 to f4 again (duplicate): " + storage.copyFile("f3", "f4", 7).orElse(null));
        System.out.println("Copy non-existent file f5: " + storage.copyFile("f5", "f6", 8).orElse(null));
    }
}

