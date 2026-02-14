package com.tarun;

public class TaskManagerTest {
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        System.out.println("--- TaskManager Tests ---");

        // Test createTask
        boolean create1 = manager.createTask("t1", 1, "First task", "u1");
        System.out.println("Create task t1: " + create1);
        boolean create2 = manager.createTask("t1", 2, "Duplicate task", "u1");
        System.out.println("Create duplicate task t1: " + create2);
        boolean create3 = manager.createTask("t2", -1, "Negative timestamp", "u1");
        System.out.println("Create task t2 with negative timestamp: " + create3);

        // Test updateTaskDescription
        System.out.println("Update t1 description: " + manager.updateTaskDescription("t1", 3, "Updated task").orElse("fail"));
        System.out.println("Update t2 description (not created): " + manager.updateTaskDescription("t2", 4, "desc").orElse("fail"));
        System.out.println("Update t1 with empty description: " + manager.updateTaskDescription("t1", 5, "").orElse("fail"));

        // Test completeTask
        System.out.println("Complete t1: " + manager.completeTask("t1", 6).orElse(null));
        System.out.println("Complete t2 (not created): " + manager.completeTask("t2", 7).orElse(null));
    }
}

