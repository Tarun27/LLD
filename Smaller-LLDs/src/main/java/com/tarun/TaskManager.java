package com.tarun;

import java.util.*;
import java.util.stream.Collectors;

public class TaskManager {

    Map<String, Task> taskMap = new HashMap<>();

    boolean createTask(String taskId, int timestamp, String description,String creatorId) {
        if (this.taskMap.containsKey(taskId) || Objects.isNull(description)
                || description.isEmpty() || timestamp < 0
                || Objects.isNull(creatorId) || creatorId.isEmpty()) {
            return false;
        }
        Task task = new Task(taskId, timestamp, description, TaskStatus.PENDING, creatorId);
        taskMap.put(taskId, task);
        return true;
    }

    Optional<String> updateTaskDescription(String taskId, int timestamp, String newDescription) {

        if (!this.taskMap.containsKey(taskId) || Objects.isNull(newDescription)
                || newDescription.isEmpty()) {
            return Optional.empty();
        }
        Task task = taskMap.get(taskId);

        if (task.status == TaskStatus.COMPLETED) {
            return Optional.empty();
        }
        String oldDescription = task.description;
        task.description = newDescription;
        task.timestamp = timestamp;
        return Optional.of(oldDescription);

    }

    Optional<Boolean> completeTask(String taskId, int timestamp) {

        if (!this.taskMap.containsKey(taskId)) {
            return Optional.empty();
        }

        if(taskMap.get(taskId).dependencies.stream().anyMatch(t -> taskMap.get(t)
                .status == TaskStatus.PENDING)
        )  return Optional.of(false);

        Task task = taskMap.get(taskId);

        if (Objects.nonNull(task)) {
            if (task.status == TaskStatus.COMPLETED) {
                return Optional.empty();
            }
            task.timestamp = timestamp;
            task.status = TaskStatus.COMPLETED;
            return Optional.of(true);
        }

        return Optional.of(false);
    }

    List<String> getPendingTasks() {

        return taskMap.values().stream()
                .filter(task -> task.status == TaskStatus.PENDING)
                .map(task -> task.taskId).sorted().toList();
    }

    List<String> getTasksByTimeRange(int startTime, int endTime) {
        return taskMap.values().stream()
                .filter(task -> task.timestamp >= startTime && task.timestamp <= endTime)
                .sorted((task1, task2) ->{
                    int compare = Integer.compare(task1.timestamp, task2.timestamp);
                    if (compare == 0) {
                        return task1.taskId.compareTo(task2.taskId);
                    }
                    return compare;
                })
                .map(task -> task.taskId)
                .toList();
    }

    List<String> getTopContributors(int n){

        return taskMap.values().stream()
               .collect(Collectors.groupingBy(task -> task.creatorId, Collectors.counting()))
               .entrySet().stream()
               .sorted((entry1, entry2) -> {
                     int compare = Long.compare(entry2.getValue(), entry1.getValue());
                     if (compare == 0) {
                          return entry1.getKey().compareTo(entry2.getKey());
                     }
                     return compare;
                })
               .limit(n)
               .map(entry -> new String(entry.getKey() + "(" + entry.getValue()) + ")")
               .toList();
    }

    Map<TaskStatus, Long> getTaskStats() {
        return taskMap.values().stream()
                .collect(Collectors.groupingBy(task -> task.status, Collectors.counting()));
    }

    boolean addDependency(String taskId, String dependsOnTaskId){
        if(!this.taskMap.containsKey(taskId) || !this.taskMap.containsKey(dependsOnTaskId)
        || taskId.equals(dependsOnTaskId) || this.taskMap.get(taskId).status == TaskStatus.COMPLETED){
            return false;
        }

        // Check for circular dependency: would dependsOnTaskId eventually lead back to taskId?
        if (wouldCreateCycle(taskId, dependsOnTaskId)) {
            return false;
        }

        taskMap.get(taskId).dependencies.add(dependsOnTaskId);
        return true;
    }

    List<String> getBlockingTasks(String taskId){
        if(!this.taskMap.containsKey(taskId) || this.taskMap.get(taskId).dependencies.isEmpty()
        || this.taskMap.get(taskId).status == TaskStatus.COMPLETED){
            return Collections.emptyList();
        }

        return taskMap.get(taskId).dependencies.stream()
                .filter(t -> taskMap.get(t).status != TaskStatus.COMPLETED)
                .sorted().toList();

    }

    String scheduleTask(String taskId, String creatorId, int timestamp, String description, int executeTime){
        if(Objects.isNull(taskId) || taskId.isEmpty() || Objects.isNull(creatorId) || creatorId.isEmpty()
        || Objects.isNull(description) || description.isEmpty() || timestamp < 0 || executeTime < 0
        || timestamp > executeTime || taskMap.containsKey(taskId)){
            return null;
        }

        Task task = new Task(taskId, timestamp, description, TaskStatus.SCHEDULED, creatorId);
        task.executeTime = executeTime;
        taskMap.put(taskId, task);

        return taskId;

    }

    int processScheduledTasks(int currentTimestamp){

        List<Task> tasksToProcess = taskMap.values().stream()
                .filter(task -> task.status == TaskStatus.SCHEDULED && task.executeTime <= currentTimestamp)
                .toList();

        tasksToProcess.forEach(task -> {
            task.status = TaskStatus.PENDING;
            task.timestamp = currentTimestamp;
        });

        return tasksToProcess.size();
    }

    private boolean wouldCreateCycle(String taskId, String dependsOnTaskId) {
        // DFS: check if we can reach taskId starting from dependsOnTaskId's dependencies
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(dependsOnTaskId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(taskId)) {
                return true;  // Found cycle!
            }
            if (visited.add(current)) {
                queue.addAll(taskMap.get(current).dependencies);
            }
        }
        return false;
    }

    boolean setTaskPriority(String taskId, int priority){
        if(!this.taskMap.containsKey(taskId) || this.taskMap.get(taskId).status == TaskStatus.COMPLETED
        || priority < 1 || priority > 5){
            return false;
        }

        taskMap.get(taskId).priority = priority;
        return true;
    }

    List<String> getTasksByPriority(int n){

     return   taskMap.values().stream().filter(task -> task.status == TaskStatus.PENDING)
             .sorted((task1,task2) -> {
            int compare = Integer.compare(task1.priority, task2.priority);
            if (compare == 0) {
                int time =  Integer.compare(task1.timestamp, task2.timestamp);
                if(time==0){
                    return task1.taskId.compareTo(task2.taskId);
                }
                return time;
            }
            return compare;
        }).map(task -> task.taskId+"("+task.priority+")").limit(n).toList();

    }


    boolean mergeTask(String targetTaskId, String sourceTaskId){
        if(!this.taskMap.containsKey(targetTaskId) || !this.taskMap.containsKey(sourceTaskId)
        || targetTaskId.equals(sourceTaskId) || this.taskMap.get(targetTaskId).status == TaskStatus.COMPLETED
        || this.taskMap.get(sourceTaskId).status == TaskStatus.COMPLETED
        || this.taskMap.get(sourceTaskId).dependencies.contains(targetTaskId)){
            return false;
        }

        Task targetTask = taskMap.get(targetTaskId);
        Task sourceTask = taskMap.get(sourceTaskId);

        targetTask.priority = Math.min(targetTask.priority, sourceTask.priority);

        Set<String> dependenciesSet = new HashSet<>(targetTask.dependencies);
        dependenciesSet.addAll(sourceTask.dependencies);

        targetTask.dependencies.clear();
        targetTask.dependencies.addAll(dependenciesSet);

        String newDescription = targetTask.description + " | " + sourceTask.description;
        targetTask.description = newDescription;

        taskMap.remove(sourceTaskId);

        // After removing source, update all tasks that depended on source
        for (Task task : taskMap.values()) {
            if (task.dependencies.remove(sourceTaskId)) {
                if (!task.taskId.equals(targetTaskId)) {  // Avoid self-dependency
                    task.dependencies.add(targetTaskId);
                }
            }
        }


        return true;
    }

    int bulkComplete(List<String> taskIds, int timestamp){
        int completedCount = 0;
        for(String taskId : taskIds){
            Optional<Boolean> result = completeTask(taskId, timestamp);
            if(result.isPresent() && result.get()){
                completedCount++;
            }
        }
        return completedCount;
    }

    Map<String, List<String>> getDependencyGraph(){
      return  taskMap.keySet().stream().filter(k-> !taskMap.get(k).dependencies.isEmpty())
                .map(k -> Map.entry(k, taskMap.get(k).dependencies.stream()
                        .sorted()))
                . collect(Collectors
                        .toMap(entry -> entry.getKey(),
                                entry -> entry.getValue().toList()));
    }

}

class Task{

    String taskId;
    int timestamp;
    String description;
    TaskStatus status;
    String creatorId;
    int executeTime;
    int priority;
    List<String> dependencies;

    Task(String taskId, int timestamp, String description, TaskStatus status, String creatorId) {
        this.taskId = taskId;
        this.timestamp = timestamp;
        this.description = description;
        this.status =  status;
        this.creatorId = creatorId;
        this.dependencies = new ArrayList<>();
        this.priority = 3; // Default priority
    }
}


enum TaskStatus{
    PENDING,
    COMPLETED,
    SCHEDULED
}