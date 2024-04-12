package org.niclic;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class Main {

    // global variables
    private static JComboBox<String> comboBoxVersion;
    public static File infoFile = new File("utils/info.txt");
    public static String lastName;
    public static String lastVersion;
    public static String version;

    public static void main(String[] args) {
        // Frame settings
        JFrame frame = new JFrame("elLauncher");
        frame.setSize(250, 150);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        JPanel panel = new JPanel();
        frame.add(panel);

        JPanel panelTop = new JPanel();
        panelTop.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Scan Info
        if (infoFile.exists()) {
            Scanner scanner = null;
            try {
                scanner = new Scanner(infoFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            lastName = scanner.nextLine();
            lastVersion = scanner.nextLine();
            scanner.close();
        } else {
            System.out.println("File info.txt not found!");
        }

        JLabel labelNickname = new JLabel("Name:");
        JTextField textFieldNickname = new JTextField(15);
        textFieldNickname.setText(lastName);

        JLabel labelVersion = new JLabel("Version:");
        comboBoxVersion = new JComboBox<>(new String[]{"ForgeOptiFine-1.12.2-SM", "ForgeOptiFine-1.12.2"});
        comboBoxVersion.setSelectedItem(lastVersion);

        JButton buttonStart = new JButton("start");
        buttonStart.addActionListener(e -> {
            // Write Info
            try {
                File infoPackage = new File("utils");
                infoPackage.mkdir();
                FileWriter writer = new FileWriter(infoFile);
                writer.write(textFieldNickname.getText() + "\n");
                writer.write((String) comboBoxVersion.getSelectedItem());
                writer.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            // Minecraft Path
            String dir = System.getProperty("user.dir").replaceAll("[\\\\]", "/");
            File minecraftDir = new File(dir + "/" + comboBoxVersion.getSelectedItem()); if (!minecraftDir.exists()) {minecraftDir.mkdir();}
            // Main Json
            File mainJsonPath = findFile(minecraftDir, comboBoxVersion.getSelectedItem() + ".json");
            if (mainJsonPath == null) {try {FileUtils.copyURLToFile(new URL("https://github.com/Niclic2/elLauncher/releases/download/" + comboBoxVersion.getSelectedItem() + "/" +comboBoxVersion.getSelectedItem() + ".json"), mainJsonPath = new File(minecraftDir + "/" + comboBoxVersion.getSelectedItem() + ".json"));} catch (IOException ex) {throw new RuntimeException(ex);}}
            else {
                checkUpdate("https://github.com/Niclic2/elLauncher/releases/download/" + comboBoxVersion.getSelectedItem() + "/" +comboBoxVersion.getSelectedItem() + ".json", mainJsonPath);
            }
            // Java
            File javaDir = new File(minecraftDir + "/javaRuntime"); if (!javaDir.exists()) {javaDir.mkdir();}
            File javaPath = findFile(javaDir, "javaw.exe");
            if (javaPath == null) {
                try {
                    FileUtils.copyURLToFile(new URL("https://github.com/Niclic2/elLauncher/releases/download/" + comboBoxVersion.getSelectedItem() + "/java.zip"), new File(javaDir + "/java.zip"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                File javaZipPath = new File(javaDir + "/java.zip");
                try {unZip(javaZipPath.getPath(), javaDir.getPath());} catch (IOException ex) {throw new RuntimeException(ex);}
                javaZipPath.delete();
                javaPath = findFile(javaDir, "javaw.exe");
            }
            // Natives
            File nativesDir = new File(minecraftDir + "/natives");
            if (!nativesDir.exists()) {
                try {FileUtils.copyURLToFile(new URL("https://github.com/Niclic2/elLauncher/releases/download/" + comboBoxVersion.getSelectedItem() + "/natives.zip"), new File(minecraftDir + "/natives.zip"));} catch (IOException ex) {throw new RuntimeException(ex);}
                File nativesZipPath = new File(minecraftDir + "/natives.zip");
                try {unZip(nativesZipPath.getPath(), minecraftDir.getPath());} catch (IOException ex) {throw new RuntimeException(ex);}
                nativesZipPath.delete();
            }
            // Libraries
            ExecutorService librariesExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            File librariesDir = new File(minecraftDir + "/libraries"); if (!librariesDir.exists()) {librariesDir.mkdir();}
            StringBuilder usedLibraries = new StringBuilder();
            try {
                List<Callable<Void>> librariesTasks = new ArrayList<>();
                JSONObject libraries = jsonReader(mainJsonPath).getJSONObject("libraries");
                for (String libraryName : libraries.keySet()) {
                    JSONObject libraryInfo = libraries.getJSONObject(libraryName);
                    String url = libraryInfo.getString("url");
                    File libraryPath = new File(librariesDir + "/" + libraryName);
                    usedLibraries.append(libraryPath).append(";");
                    librariesTasks.add(() -> {
                        if (!libraryPath.exists()) {FileUtils.copyURLToFile(new URL(url), libraryPath);}
                        return null;
                    });
                }
                try {
                    librariesExecutorService.invokeAll(librariesTasks);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {
                    librariesExecutorService.shutdown();
                }
            } catch (IOException a) {a.printStackTrace();}
            // Jar
            File jarPath = findFile(minecraftDir, comboBoxVersion.getSelectedItem() + ".jar");
            if (jarPath == null) {
                try {
                    JSONObject jar = jsonReader(mainJsonPath).getJSONObject("client");
                    String url = jar.getString("url");
                    jarPath = new File(minecraftDir + "/" + comboBoxVersion.getSelectedItem() + ".jar");
                    FileUtils.copyURLToFile(new URL(url), jarPath);
                } catch (IOException a) {a.printStackTrace();}
            }
            // Assets Dir - Indexes Dir - Log_Config Dir - Objects Dir
            File assetsDir = new File(minecraftDir + "/assets"); if (!assetsDir.exists()) {assetsDir.mkdir();}
            File indexesDir = new File(minecraftDir + "/assets/indexes"); if (!indexesDir.exists()) {indexesDir.mkdir();}
            File logConfigsDir = new File(minecraftDir + "/assets/log_configs"); if (!logConfigsDir.exists()) {logConfigsDir.mkdir();}
            File objectsDir = new File(minecraftDir + "/assets/objects"); if (!objectsDir.exists()) {objectsDir.mkdir();}
            // Objects Json
            ExecutorService objectsExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            String assetIndex = null;
            try {
                List<Callable<Void>> objectsTasks = new ArrayList<>();
                JSONObject assetIndexObj = jsonReader(mainJsonPath).getJSONObject("assetIndex");
                String assetId = assetIndexObj.getString("id");
                assetIndex = assetId;
                String assetUrl = assetIndexObj.getString("url");
                File objectsJsonPath = findFile(objectsDir,  indexesDir + "/" + assetId + ".json");
                if (objectsJsonPath == null) {FileUtils.copyURLToFile(new URL(assetUrl), objectsJsonPath = new File(indexesDir + "/" + assetId + ".json"));}
                JSONObject objects = jsonReader(objectsJsonPath).getJSONObject("objects");
                for (String objectName : objects.keySet()) {
                    JSONObject objectInfo = objects.getJSONObject(objectName);
                    String hash = objectInfo.getString("hash");
                    String firstTwoHash = hash.substring(0,2);
                    File objectPath = new File(objectsDir + "/" + firstTwoHash + "/" + hash);
                    objectsTasks.add(() -> {
                        if (!objectPath.exists()) {FileUtils.copyURLToFile(new URL("https://resources.download.minecraft.net/" + firstTwoHash + "/" + hash), objectPath);}
                        return null;
                    });
                }
                try {
                    objectsExecutorService.invokeAll(objectsTasks);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {
                    objectsExecutorService.shutdown();
                }
            } catch (IOException a) {a.printStackTrace();}
            // Mods
            ExecutorService modsExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            File modsDir = new File(minecraftDir + "/mods"); if (!modsDir.exists()) {modsDir.mkdir();}
            try {
                List<Callable<Void>> modsTasks = new ArrayList<>();
                JSONObject mods = jsonReader(mainJsonPath).getJSONObject("mods");
                for (String modsName : mods.keySet()) {
                    JSONObject modsInfo = mods.getJSONObject(modsName);
                    String url = modsInfo.getString("url");
                    File modsPath = new File(modsDir + "/" + modsName);
                    modsTasks.add(() -> {
                        if (!modsPath.exists()) {FileUtils.copyURLToFile(new URL(url), modsPath);}
                        return null;
                    });
                }
                try {modsExecutorService.invokeAll(modsTasks);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {
                    modsExecutorService.shutdown();
                }
            } catch (IOException a) {a.printStackTrace();}
            // log_configs
            File logConfigsPath = null;
            try {
                JSONObject logConfigs = jsonReader(mainJsonPath).getJSONObject("logging");
                String logConfigsId = logConfigs.getString("id");
                String logConfigsUrl = logConfigs.getString("url");
                logConfigsPath = findFile(objectsDir,  logConfigsDir + "/" + logConfigsId);
                if (logConfigsPath == null) {FileUtils.copyURLToFile(new URL(logConfigsUrl), logConfigsPath = new File(logConfigsDir + "/" + logConfigsId));}
            } catch (IOException a) {a.printStackTrace();}
            // Other Files
            ExecutorService otherFilesExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            try {
                List<Callable<Void>> otherFilesTasks = new ArrayList<>();
                JSONObject otherFiles = jsonReader(mainJsonPath).getJSONObject("otherFiles");
                for (String fileName : otherFiles.keySet()) {
                    JSONObject fileInfo = otherFiles.getJSONObject(fileName);
                    String url = fileInfo.getString("url");
                    File filePath = new File(minecraftDir + "/" + fileName);
                    otherFilesTasks.add(() -> {
                        if (!filePath.exists()) {FileUtils.copyURLToFile(new URL(url), filePath);}
                        else {
                            checkUpdate(url, filePath);
                        }
                        return null;
                    });
                }
                try {otherFilesExecutorService.invokeAll(otherFilesTasks);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {
                    otherFilesExecutorService.shutdown();
                }
            } catch (IOException a) {a.printStackTrace();}
            // Main Class (argument)
            String mainClass = null;
            try {
                JSONObject mainClassObj = jsonReader(mainJsonPath);
                mainClass = mainClassObj.getString("mainClass");
            } catch (IOException a) {a.printStackTrace();}
            // Version
            try {
                JSONObject versionTypeObj = jsonReader(mainJsonPath);
                version = versionTypeObj.getString("version");
            } catch (IOException a) {a.printStackTrace();}
            // Version Type (argument)
            String versionType = null;
            try {
                JSONObject versionTypeObj = jsonReader(mainJsonPath);
                versionType = versionTypeObj.getString("versionType");
            } catch (IOException a) {a.printStackTrace();}
            // Minecraft first arguments
            StringBuilder usedFirstArguments = new StringBuilder();
            try {
                JSONArray minecraftFirstArguments = jsonReader(mainJsonPath).getJSONArray("minecraftFirstArguments");
                for (int i = 0; i < minecraftFirstArguments.length(); i++) {
                    JSONObject argument = minecraftFirstArguments.getJSONObject(i).getJSONObject("arguments");
                    usedFirstArguments.append(argument.getString("arg")).append(" ");
                }
            } catch (IOException a) {a.printStackTrace();}
            // Minecraft second arguments
            StringBuilder usedSecondArguments = new StringBuilder();
            try {
                JSONArray minecraftSecondArguments = jsonReader(mainJsonPath).getJSONArray("minecraftSecondArguments");
                for (int i = 0; i < minecraftSecondArguments.length(); i++) {
                    JSONObject argument = minecraftSecondArguments.getJSONObject(i).getJSONObject("arguments");
                    usedSecondArguments.append(argument.getString("arg")).append(" ");
                }
            } catch (IOException a) {a.printStackTrace();}
            // User Name Cache
            File userCache = findFile(minecraftDir, "usercache.json");
            String userUuid = null;
            String userName = null;
            if (userCache == null) {
                userUuid = getRandomUuid();
                userName = textFieldNickname.getText();
            } else {
                try {
                    FileReader reader = new FileReader(userCache);
                    JSONTokener tokener = new JSONTokener(reader);
                    JSONArray cacheArray = new JSONArray(tokener);
                    for (int i = 0; i < cacheArray.length(); i++) {
                        JSONObject cacheObject = cacheArray.getJSONObject(i);
                        String name = cacheObject.getString("name");
                        String uuid = cacheObject.getString("uuid");
                        if (name.equals(textFieldNickname.getText())) {
                            userUuid = uuid;
                            userName = name;
                            break;
                        }
                    }
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (userName == null) {
                userName = textFieldNickname.getText();
                userUuid = getRandomUuid();
            }
            // Launch Minecraft
            System.out.println(userUuid + " " + userName);
            try {
                Process p = Runtime.getRuntime().exec(javaPath.getPath() + " -Djava.library.path="+nativesDir + " -cp "+usedLibraries+jarPath + " " + usedFirstArguments + " -Xmx8G -Xms8G -Dminecraft.applet.TargetDirectory="+minecraftDir + " -DlibraryDirectory="+librariesDir + " -Dlog4j.configurationFile="+logConfigsPath+" "+mainClass + " --username="+userName + " --version="+comboBoxVersion.getSelectedItem() + " --gameDir="+minecraftDir + " --assetsDir="+assetsDir + " --assetIndex="+assetIndex + " --uuid="+userUuid + " --accessToken=null --userType=mojang " + usedSecondArguments + " --versionType="+versionType + " --width=1600 --height=900");
                p.getInputStream().close();
                p.getOutputStream().close();
                p.getErrorStream().close();
            } catch (java.io.IOException a) {
                a.printStackTrace();
            }
            Thread.currentThread().interrupt();
            // if Minecraft Launch - close frame
            boolean minecraftRunning = false;
            while (!minecraftRunning) {
                minecraftRunning = isMinecraftRunning();
            }
            frame.dispose();
        });

        // gui
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(10));
        panel.add(panelTop);
        panelTop.add(labelNickname);
        panelTop.add(textFieldNickname);
        panelTop.add(Box.createHorizontalStrut(10));
        panelTop.add(labelVersion);
        panelTop.add(comboBoxVersion);
        panelTop.add(Box.createHorizontalStrut(10));
        panel.add(buttonStart);
        panel.add(Box.createVerticalStrut(10));

        frame.setVisible(true);
    }
    // Check Update Method
    public static void checkUpdate(String url, File filePath) {
            try {
                URL otherFilesUrlCheck = new URL(url);
                URLConnection connection = otherFilesUrlCheck.openConnection();
                long lastModifiedRemote = connection.getLastModified();
                Path localFile = Path.of(filePath.getPath());
                long lastModifiedLocal = Files.getLastModifiedTime(localFile).toMillis();
                System.out.println(lastModifiedRemote + " " + lastModifiedLocal + " " + filePath + " " + otherFilesUrlCheck);
                if (lastModifiedRemote > lastModifiedLocal) {
                    FileUtils.copyURLToFile(otherFilesUrlCheck, filePath);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
        }
    }
    // Search file Method
    public static File findFile(File dir, String fileName) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    File foundFile = findFile(file, fileName);
                    if (foundFile != null) {
                        return foundFile;
                    }
                } else if (file.getName().equals(fileName)) {
                    return file;
                }
            }
        }
        return null;
    }
    // Unzip archives file Method
    public static void unZip(String zipFilePath, String destinationPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                Path filePath = Paths.get(destinationPath, entryName);
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    // Extract the file
                    try (InputStream inputStream = zipFile.getInputStream(entry);
                         OutputStream fos = Files.newOutputStream(filePath)) {
                        byte[] buf = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buf)) != -1) {
                            fos.write(buf, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }
    // Read Json file Method
    public static JSONObject jsonReader (File mainJsonPath) throws FileNotFoundException {
        FileReader reader = new FileReader(mainJsonPath);
        JSONTokener tokener = new JSONTokener(reader);
        JSONObject jsonObject = new JSONObject(tokener);
        return jsonObject;
    }
    // Wait Minecraft Method
    public static boolean isMinecraftRunning() {
        User32 user32 = User32.INSTANCE;
        WinDef.HWND hwnd = user32.FindWindow(null, "Minecraft " + version);
        return hwnd != null;
    }
    // Random UUID Method
    public static String getRandomUuid() {
        StringBuilder cacheBuilder = new StringBuilder();
        int lowerBound = 0000;
        int upperBound = 9999;
        long random10DigitNumber;
        for (int i = 0; i < 8; i++) {
            random10DigitNumber = ThreadLocalRandom.current().nextLong(lowerBound, upperBound + 1);
            cacheBuilder.append(random10DigitNumber);
        }
        return String.valueOf(cacheBuilder);
    }
}

