package org.example;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        String desktopPath = fileSystemView.getHomeDirectory().getPath();
        final String FILE_PATH_JAVA = desktopPath + "\\File_Change\\Java\\";
        final String FILE_PATH_OTHER = desktopPath + "\\File_Change\\Other\\";
        BasicConfigurator.configure();
        // Đường dẫn đến thư mục repository
        String repositoryPath = System.getProperty("user.dir");

        // Tạo đối tượng Git từ đường dẫn repository
        try (Git git = Git.open(new File(repositoryPath))) {
            // Lấy ra ObjectId của commit trên nhánh master
            ObjectId masterCommitId = git.getRepository().resolve("master");

            // Lấy ra ObjectId của commit trên nhánh khác
            ObjectId otherCommitId = git.getRepository().resolve("HEAD");

            // Lấy ra danh sách các tệp thay đổi giữa hai commit
            List<DiffEntry> diffEntries = git.diff()
                    .setOldTree(prepareTreeParser(git.getRepository(), masterCommitId))
                    .setNewTree(prepareTreeParser(git.getRepository(), otherCommitId))
                    .call();
            // Sao chép các tệp thay đổi vào một thư mục
            for (DiffEntry diffEntry : diffEntries) {
                if (diffEntry.getNewPath() == "\\dev\\null") {
                    continue;
                }
                String[] path = diffEntry.getNewPath().split("/");
                String pathFile = path[path.length - 1];
                if (!pathFile.endsWith(".java")) {
                    copyFileOtherJava(pathFile, diffEntry.getNewPath(), repositoryPath, FILE_PATH_OTHER);
                    continue;
                }
                File file = new File(FILE_PATH_JAVA + pathFile);
                File fileBefore = new File(FILE_PATH_JAVA + "before" + pathFile);
                file.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(fileBefore)) {
                    DiffFormatter diffFormatter = new DiffFormatter(fos);
                    diffFormatter.setRepository(git.getRepository());
                    diffFormatter.format(diffEntry);
                }

                BufferedReader reader = new BufferedReader(new FileReader(fileBefore));
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));

                String line;
                int lineNumber = 0;
                StringBuilder content = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("+")) {
                        line = line.substring(1);
                    }
                    lineNumber++;
                    if (lineNumber <= 6) {
                        continue;
                    }
                    writer.write(line);
                    writer.newLine();
                }
                reader.close();
                writer.close();
            }
            String folderPath = FILE_PATH_JAVA;
            File folder = new File(folderPath);

            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();

                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().contains("before")) {
                            file.delete();
                        }
                    }
                }
            }
        }
    }

    private static CanonicalTreeParser prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = repository.newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }

    }
    private static void copyFileOtherJava(String fileName,String pathFile, String pathRoot, String pathSaveRoot){
        try {
            Path sourceFile = Path.of(pathRoot + "\\" +pathFile);
            Path destinationFile =  Path.of(pathSaveRoot + fileName);
            // Create parent directories of the destination file if they don't exist
            Files.createDirectories(destinationFile.getParent());

            // Create the destination file if it doesn't exist
            if (!Files.exists(destinationFile)) {
                Files.createFile(destinationFile);
            }

            try (InputStream inputStream = Files.newInputStream(sourceFile);
                 OutputStream outputStream = Files.newOutputStream(destinationFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to copy the file.");
        }
    }


}