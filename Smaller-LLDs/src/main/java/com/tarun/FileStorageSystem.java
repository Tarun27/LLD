package com.tarun;

import java.util.*;
import java.util.stream.Collectors;

public class FileStorageSystem {

    Map<String, File> fileMap = new HashMap<>();
    Map<String, User> userMap = new HashMap<>();
    Map<String, Folder> folderMap = new HashMap<>();

    boolean uploadFile(String fileId, String fileName, int size, int timestamp) {

        if (Objects.isNull(fileId) || fileId.isEmpty() || Objects.isNull(fileName)
                || fileName.isEmpty() || size <= 0 || fileMap.containsKey(fileId)) {
            return false;
        }

        File file = new File(fileId, fileName, size, timestamp);
        fileMap.put(fileId, file);
        return true;
    }

    Optional<Integer> getFileSize(String fileId) {
        if (Objects.isNull(fileId) || fileId.isEmpty() || !fileMap.containsKey(fileId)) {
            return Optional.empty();
        }

        return Optional.of(fileMap.get(fileId).size);
    }

    boolean deleteFile(String fileId, int timestamp) {
        if (Objects.isNull(fileId) || fileId.isEmpty() || !fileMap.containsKey(fileId)) {
            return false;
        }

        fileMap.remove(fileId);
        return true;
    }

    Optional<Integer> copyFile(String sourceFileId, String newFileId, int timestamp) {
        if (Objects.isNull(sourceFileId) || sourceFileId.isEmpty() || Objects.isNull(newFileId)
                || newFileId.isEmpty() || !fileMap.containsKey(sourceFileId) || fileMap.containsKey(newFileId)) {
            return Optional.empty();
        }

        File sourceFile = fileMap.get(sourceFileId);

        File newFile = new File(newFileId, sourceFile.fileName, sourceFile.size, timestamp);
        fileMap.put(newFileId, newFile);
        return Optional.of(newFile.size);
    }

    List<String> getAllFiles() {

        if (fileMap.isEmpty()) return Collections.emptyList();

        return fileMap.keySet().stream().sorted().toList();
    }

    List<String> findFilesByPrefix(String prefix) {
        if (Objects.isNull(prefix) || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        return fileMap.values().stream()
                .filter(file -> file.fileName.startsWith(prefix))
                .map(file -> file.fileId)
                .sorted()
                .toList();
    }

    List<String> findFilesByExtension(String extension) {
        if (Objects.isNull(extension) || extension.isEmpty()) {
            return Collections.emptyList();
        }

        return fileMap.values().stream()
                .filter(file -> file.fileName.endsWith(extension))
                .map(file -> file.fileId)
                .sorted()
                .toList();
    }

    List<String> getLargestFiles(int n) {
        if (n <= 0 || fileMap.isEmpty()) {
            return Collections.emptyList();
        }

        return fileMap.values().stream()
                .sorted((f1, f2) -> {
                    if (f2.size == f1.size) {
                        return f1.fileId.compareTo(f2.fileId);
                    }
                    return Integer.compare(f2.size, f1.size);
                })
                .limit(n)
                .map(file -> file.fileId + "(" + file.size + ")")
                .toList();
    }

    long getTotalStorageUsed() {
        return fileMap.values().stream()
                .mapToLong(file -> file.size)
                .sum();
    }

    Map<String, Long> getStorageByExtension() {
        return fileMap.values().stream()
                .collect(Collectors.groupingBy(
                        file -> {
                            int idx = file.fileName.lastIndexOf(".");
                            return idx <= 0 ? "no_extension" : file.fileName.substring(idx + 1);
                        },
                        Collectors.summingLong(file -> file.size)
                ));

        // idx <= 0 means no extension or hidden file, we can categorize them as "no_extension"
    }

    boolean createUser(String userId, long storageQuota) {
        if (Objects.isNull(userId) || userId.isEmpty() || storageQuota <= 0) {
            return false;
        }
        User user = new User(userId, storageQuota);
        userMap.put(userId, user);
        return true;
    }

    boolean uploadFileByUser(String userId, String fileId, String fileName, int size, int timestamp) {
        if (Objects.isNull(userId) || userId.isEmpty() || Objects.isNull(fileId) || fileId.isEmpty()
                || Objects.isNull(fileName) || fileName.isEmpty() || size <= 0 ||
                fileMap.containsKey(fileId) || !userMap.containsKey(userId) ||
                userMap.get(userId).storageUsed + size > userMap.get(userId).storageQuota) {
            return false;
        }

        uploadFile(fileId, fileName, size, timestamp);

        File file = fileMap.get(fileId);
        file.userId = userId;
        User user = userMap.get(userId);
        user.storageUsed += size;
        return true;

    }

    Optional<Long> getRemainingQuota(String userId) {
        if (Objects.isNull(userId) || userId.isEmpty() || !userMap.containsKey(userId)) {
            return Optional.empty();
        }

        User user = userMap.get(userId);
        return Optional.of(user.storageQuota - user.storageUsed);
    }

    boolean createFolder(String folderId, String folderName) {
        if (Objects.isNull(folderId) || folderId.isEmpty() || Objects.isNull(folderName)
                || folderName.isEmpty() || folderMap.containsKey(folderId)) {
            return false;
        }

        Folder folder = new Folder(folderId, folderName);
        folderMap.put(folderId, folder);
        return true;
    }

    boolean moveFileToFolder(String fileId, String folderId) {
        if (Objects.isNull(fileId) || fileId.isEmpty() || Objects.isNull(folderId) || folderId.isEmpty()
                || !fileMap.containsKey(fileId) || !folderMap.containsKey(folderId)) {
            return false;
        }

        Folder folder = folderMap.get(folderId);
        if (folder.fileIds.contains(fileId)) {
            return true; // File already in the folder
        }

        folderMap.values().stream().filter(f -> f.fileIds.contains(fileId))
                .forEach(f -> f.fileIds.remove(fileId)); // Remove file from any other folder

        folder.fileIds.add(fileId);
        return true;
    }

    List<String> getFilesInFolder(String folderId) {
        if (Objects.isNull(folderId) || folderId.isEmpty() || !folderMap.containsKey(folderId)) {
            return Collections.emptyList();
        }

        Folder folder = folderMap.get(folderId);
        return folder.fileIds.stream()
                .sorted()
                .toList();
    }

    Optional<Integer> deleteFileByUser(String userId, String fileId, int timestamp) {
        if (Objects.isNull(userId) || userId.isEmpty() || Objects.isNull(fileId) || fileId.isEmpty()
                || !fileMap.containsKey(fileId) || !userMap.containsKey(userId) ||
                !userId.equals(fileMap.get(fileId).userId)) {
            return Optional.empty();
        }

        File file = fileMap.get(fileId);
        if (deleteFile(fileId, timestamp)) {
            User user = userMap.get(userId);
            user.storageUsed -= file.size;
            folderMap.values().forEach(f -> f.fileIds.remove(fileId)); // Remove file from any folder
            return Optional.of(file.size);
        }

        return Optional.empty();
    }

    String uploadNewVersion(String fileId, int newSize, int timestamp) {
        if (Objects.isNull(fileId) || fileId.isEmpty() || newSize <= 0 || !fileMap.containsKey(fileId)) {
            return null;
        }

        File oldFile = fileMap.get(fileId);
        String userId = oldFile.userId;
        if (Objects.nonNull(userId) && userMap.containsKey(userId)) {
            User user = userMap.get(userId);
            long newStorageUsed = user.storageUsed - oldFile.size + newSize;
            if (newStorageUsed > user.storageQuota) {
                return null;
            }
            user.storageUsed = newStorageUsed;
        }

        oldFile.versionCount++;
        String newVersionId = fileId + "_v" + oldFile.versionCount;
        oldFile.versionHistory.put(newVersionId, newSize);
        oldFile.size = newSize;
        oldFile.timestamp = timestamp;
        oldFile.originalSize = newSize; // Update original size for compression purposes
        return newVersionId;
    }


    List<String> getVersionHistory(String fileId) {
        if (Objects.isNull(fileId) || fileId.isEmpty() || !fileMap.containsKey(fileId)) {
            return Collections.emptyList();
        }

        File file = fileMap.get(fileId);
        return file.versionHistory.keySet().stream()
                .map(versionId -> versionId + "(" + file.versionHistory.get(versionId) + ")")
                .toList();
    }

    Optional<Integer> rollbackToVersion(String fileId, String versionId, int timestamp) {

        if (Objects.isNull(fileId) || fileId.isEmpty() || Objects.isNull(versionId) || versionId.isEmpty()
                || !fileMap.containsKey(fileId)) {
            return Optional.empty();
        }

        File file = fileMap.get(fileId);
        if (!file.versionHistory.containsKey(versionId)) {
            return Optional.empty();
        }
            int newSize = file.versionHistory.get(versionId);
            String userId = file.userId;

            if (Objects.nonNull(userId) && userMap.containsKey(userId)) {
                User user = userMap.get(userId);
                long newStorageUsed = user.storageUsed - file.size + newSize;
                if (newStorageUsed > user.storageQuota) {
                    return Optional.empty();
                }
                user.storageUsed = newStorageUsed;
            }

            file.size = newSize;
            file.timestamp = timestamp;
            return Optional.of(newSize);

    }

        boolean compressFile (String fileId){

            if (Objects.isNull(fileId) || fileId.isEmpty() || !fileMap.containsKey(fileId)) {
                return false;
            }

            File file = fileMap.get(fileId);

            if (file.isCompressed) {
                return false;  // ✅ Can't compress twice
            }
            file.originalSize = file.size; // Store original size before compression
            int compressedSize = file.size / 2; // Simulate compression by reducing size by 50%

            String userId = file.userId;
            if (Objects.nonNull(userId) && userMap.containsKey(userId)) {
                User user = userMap.get(userId);
                user.storageUsed = user.storageUsed - file.size + compressedSize;
            }

            file.size = compressedSize;
            file.isCompressed = true;

            return true;
        }

        boolean decompressFile (String fileId){
            if (Objects.isNull(fileId) || fileId.isEmpty() || !fileMap.containsKey(fileId)) {
                return false;
            }

            File file = fileMap.get(fileId);
            if (!file.isCompressed) {
                return false; // File is not compressed
            }

            String userId = file.userId;
            if (Objects.nonNull(userId) && userMap.containsKey(userId)) {
                User user = userMap.get(userId);
                long newStorageUsed = user.storageUsed - file.size + file.originalSize;
                if (newStorageUsed > user.storageQuota) {
                    return false; // Cannot decompress due to quota limits
                }
                user.storageUsed = newStorageUsed;
            }

            file.size = file.originalSize;
            file.isCompressed = false;

            return true;
        }

        Map<String, Map<String, Long>> getStorageReport () {
            Map<String, Map<String, Long>> report = new HashMap<>();

            for (String userId : userMap.keySet()) {

                long fileCount = (int) fileMap.values().stream().filter(f -> userId.equals(f.userId)).count();

                if (fileCount == 0) continue;

                long totalStorageUsed = userMap.get(userId).storageUsed;

                long compressedFileCount = (int) fileMap.values().stream()
                        .filter(f -> userId.equals(f.userId) && f.isCompressed).count();

                report.put(userId, Map.of(
                        "totalUsed", totalStorageUsed,
                        "fileCount", fileCount,
                        "compressedCount", compressedFileCount
                ));


            }

            return report;
        }

    }

    class File {
        String fileId;
        String fileName;
        int size;
        int timestamp;
        String userId;
        int versionCount;
        boolean isCompressed;
        int originalSize;

        Map<String, Integer> versionHistory; // versionId -> size

        File(String fileId, String fileName, int size, int timestamp) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.size = size;
            this.timestamp = timestamp;
            this.versionCount = 0; // Initial version
            this.versionHistory = new LinkedHashMap<>();
            this.isCompressed = false;
            this.originalSize = size;
        }
    }

    class User {
        String userId;
        long storageQuota;
        long storageUsed;

        User(String userId, long storageQuota) {
            this.userId = userId;
            this.storageQuota = storageQuota;
            this.storageUsed = 0;

        }
    }

    class Folder {
        String folderId;
        String folderName;
        List<String> fileIds;

        Folder(String folderId, String folderName) {
            this.folderId = folderId;
            this.folderName = folderName;
            this.fileIds = new ArrayList<>();
        }
    }
