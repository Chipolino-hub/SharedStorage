package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    @FXML private VBox splitDropZone;
    @FXML private Label splitFileNameLabel;
    @FXML private Slider nSlider;
    @FXML private Slider kSlider;
    @FXML private Label nValueLabel;
    @FXML private Label kValueLabel;
    @FXML private Button splitButton;
    @FXML private ProgressBar splitProgressBar;
    @FXML private TextArea splitLog;

    @FXML private VBox restoreDropZone;
    @FXML private Label restoreStatusLabel;
    @FXML private Label restoreWarningLabel;
    @FXML private ListView<String> restoreFileList;
    @FXML private Button restoreButton;
    @FXML private ProgressBar restoreProgressBar;

    private File selectedFile;
    private final List<Share> loadedShares = new ArrayList<>();

    @FXML
    private void initialize() {
        setupSliders();
        setupDragAndDrop();
    }

    private void setupSliders() {
        nSlider.setMin(3);
        nSlider.setMax(10);
        nSlider.setValue(5);
        nSlider.setBlockIncrement(1);
        nSlider.setShowTickLabels(true);
        nSlider.setShowTickMarks(true);

        kSlider.setMin(2);
        kSlider.setMax(10);
        kSlider.setValue(3);
        kSlider.setBlockIncrement(1);
        kSlider.setShowTickLabels(true);
        kSlider.setShowTickMarks(true);

        nValueLabel.setText("5");
        kValueLabel.setText("3");

        nSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int n = newVal.intValue();
            nValueLabel.setText(String.valueOf(n));
            if (kSlider.getValue() > n) kSlider.setValue(n);
            kSlider.setMax(n);
        });

        kSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                kValueLabel.setText(String.valueOf(newVal.intValue())));
    }

    private void setupDragAndDrop() {
        splitDropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        splitDropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".txt")) {
                    selectedFile = file;
                    splitFileNameLabel.setText(file.getName());
                    splitLog.appendText("Выбран файл: " + file.getName() + "\n");
                } else {
                    splitFileNameLabel.setText("Только .txt файлы!");
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        restoreDropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        restoreDropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                db.getFiles().forEach(this::addShareFile);
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    @FXML
    private void selectFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"));
        File file = fc.showOpenDialog(splitDropZone.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            splitFileNameLabel.setText(file.getName());
            splitLog.appendText("Выбран файл: " + file.getName() + "\n");
        }
    }

    @FXML
    private void splitFile() {
        if (selectedFile == null) {
            showAlert("Ошибка", "Сначала выберите файл");
            return;
        }

        try {
            splitProgressBar.setVisible(true);
            splitButton.setDisable(true);

            String content = Files.readString(selectedFile.toPath(), StandardCharsets.UTF_8);
            int n = (int) nSlider.getValue();
            int k = (int) kSlider.getValue();

            splitLog.appendText("Разделяем на " + n + " долей (K=" + k + ")...\n");
            List<String> shares = ShamirCrypto.splitSecret(content, k, n);

            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Куда сохранить доли?");
            File dir = dc.showDialog(splitDropZone.getScene().getWindow());

            if (dir != null) {
                String hash = calculateHash(content);
                for (int i = 0; i < n; i++) {
                    new Share(i + 1, k, n, shares.get(i), selectedFile.getName(), hash)
                            .saveToFile(dir.getAbsolutePath());
                }
                splitLog.appendText("Готово! Доли сохранены в:\n" + dir.getAbsolutePath() + "\n\n");
            }
        } catch (Exception e) {
            showAlert("Ошибка при разделении", e.getMessage());
            e.printStackTrace();
        } finally {
            splitProgressBar.setVisible(false);
            splitButton.setDisable(false);
        }
    }

    @FXML
    private void addShareFiles() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Доли", "*.txt"));
        List<File> files = fc.showOpenMultipleDialog(restoreDropZone.getScene().getWindow());
        if (files != null) {
            files.forEach(this::addShareFile);
        }
    }

    private void addShareFile(File file) {
        try {
            Share share = Share.loadFromFile(file);
            
            // Новый алгоритм проверки: сначала проверяем по хешу файла, а потом по индексу
            boolean hasSameHashButDifferentIndex = loadedShares.stream()
                .anyMatch(s -> s.getHash().equals(share.getHash()) && 
                               s.getIndex() == share.getIndex());
            
            // Если есть доля с таким же хешем и таким же индексом - это дубликат
            if (hasSameHashButDifferentIndex) {
                showAlert("Дубликат", "Доля с номером " + share.getIndex() + " уже загружена.\n" +
                        "Пожалуйста, загрузите разные доли для восстановления.");
                return;
            }
            
            loadedShares.add(share);
            String displayText = "Доля " + share.getIndex() + " из " + share.getN();
            if (share.getDataHash() == null) {
                displayText += " (старый формат)";
            }
            restoreFileList.getItems().add(displayText);
            updateRestoreStatus();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Целостность доли нарушена")) {
                showAlert("Ошибка целостности", 
                    "Файл '" + file.getName() + "' был изменен или поврежден!\n" +
                    "Содержимое доли не соответствует ее контрольной сумме.\n" +
                    "Пожалуйста, используйте оригинальную, неизмененную долю.\n\n" +
                    "Техническая информация: " + errorMessage);
            } else {
                showAlert("Ошибка загрузки", 
                    "Не удалось загрузить долю: " + file.getName() + 
                    "\nПричина: " + (errorMessage != null ? errorMessage : "Неизвестная ошибка"));
            }
        }
    }

    @FXML
    private void clearRestoreList() {
        loadedShares.clear();
        restoreFileList.getItems().clear();
        updateRestoreStatus();
    }

    @FXML
    private void removeSelectedShare() {
        int selectedIndex = restoreFileList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < loadedShares.size()) {
            loadedShares.remove(selectedIndex);
            restoreFileList.getItems().remove(selectedIndex);
            updateRestoreStatus();
        }
    }

    private void updateRestoreStatus() {
        if (loadedShares.isEmpty()) {
            restoreStatusLabel.setText("Долей нет");
            restoreWarningLabel.setText("");
            restoreButton.setDisable(true);
            return;
        }

        // Шаг 1: Группируем доли по хешу файла
        Map<String, List<Share>> sharesByHash = loadedShares.stream()
            .collect(Collectors.groupingBy(Share::getHash));
        
        // Шаг 2: Если есть доли с разными хешами - это приоритетная ошибка
        if (sharesByHash.size() > 1) {
            restoreStatusLabel.setText("Обнаружены доли с разными хешами!");
            restoreWarningLabel.setText("Хеши исходных файлов не совпадают. Возможно, доли от разных файлов или были изменены.");
            restoreButton.setDisable(true);
            return;
        }
        
        // Шаг 3: Теперь проверяем индексы в пределах одной группы хешей (все хеши одинаковые)
        List<Share> sharesWithSameHash = sharesByHash.values().iterator().next();
        
        // Проверяем на дубликаты индексов
        Set<Integer> uniqueIndices = new HashSet<>();
        boolean hasDuplicates = false;
        for (Share share : sharesWithSameHash) {
            if (!uniqueIndices.add(share.getIndex())) {
                hasDuplicates = true;
                break;
            }
        }
        
        if (hasDuplicates) {
            restoreStatusLabel.setText("Обнаружены доли с одинаковыми номерами!");
            restoreWarningLabel.setText("Загружены несколько долей с одинаковым номером из одного файла.");
            restoreButton.setDisable(true);
            return;
        }

        // Шаг 4: Проверяем версии долей (новый/старый формат)
        boolean hasOldVersions = sharesWithSameHash.stream()
            .anyMatch(s -> s.getDataHash() == null);
        
        if (hasOldVersions) {
            restoreWarningLabel.setText("Внимание: некоторые доли старого формата без проверки целостности");
        } else {
            restoreWarningLabel.setText("");
        }

        // Шаг 5: Проверяем количество долей
        int k = sharesWithSameHash.get(0).getK();
        if (sharesWithSameHash.size() < k) {
            restoreStatusLabel.setText("Нужно ещё " + (k - sharesWithSameHash.size()) + " долей");
            restoreButton.setDisable(true);
        } else {
            restoreStatusLabel.setText("Готово к восстановлению!");
            restoreButton.setDisable(false);
        }
    }

    @FXML
    private void restoreFile() {
        // Берем первую группу долей (все с одинаковым хешем)
        Map<String, List<Share>> sharesByHash = loadedShares.stream()
            .collect(Collectors.groupingBy(Share::getHash));
        
        List<Share> sharesWithSameHash = sharesByHash.values().iterator().next();
        
        int k = sharesWithSameHash.get(0).getK();
        if (sharesWithSameHash.size() < k) return;

        try {
            restoreProgressBar.setVisible(true);
            restoreButton.setDisable(true);

            Set<Integer> indicesSet = new HashSet<>();
            for (Share share : sharesWithSameHash) {
                if (!indicesSet.add(share.getIndex())) {
                    showAlert("Ошибка", "Обнаружены дубликаты долей! Пожалуйста, загрузите разные доли.");
                    return;
                }
            }

            List<Share> toUse = sharesWithSameHash.subList(0, Math.min(k, sharesWithSameHash.size()));
            List<Integer> indices = toUse.stream().map(Share::getIndex).collect(Collectors.toList());
            List<String> data = toUse.stream().map(Share::getData).collect(Collectors.toList());

            String restored = ShamirCrypto.recoverSecret(indices, data, k);

            if (restored.isEmpty()) {
                throw new Exception("Восстановленные данные пусты. Возможно, доли повреждены.");
            }

            FileChooser saver = new FileChooser();
            saver.setInitialFileName("восстановленный_" + toUse.get(0).getOriginalFileName());
            File saveFile = saver.showSaveDialog(restoreDropZone.getScene().getWindow());

            if (saveFile != null) {
                Files.writeString(saveFile.toPath(), restored, StandardCharsets.UTF_8);
                
                String restoredHash = calculateHash(restored);
                String expectedHash = toUse.get(0).getHash();
                
                if (!restoredHash.equals(expectedHash)) {
                    showAlert("Предупреждение", 
                        "Файл восстановлен, но его хеш не соответствует оригиналу!\n" +
                        "Это означает, что некоторые доли были изменены.\n" +
                        "Сохраняемый файл может быть поврежденным.\n\n" +
                        "Хеш оригинала: " + expectedHash + "\n" +
                        "Хеш результата: " + restoredHash);
                } else {
                    showAlert("Успех", 
                        "Файл успешно восстановлен и проверен!\n" +
                        "Хеш файла соответствует оригиналу.\n" +
                        "Имя файла: " + saveFile.getName());
                    clearRestoreList();
                }
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("BigInteger not invertible")) {
                showAlert("Ошибка восстановления", 
                    "Обнаружены некорректные или дублирующиеся доли.\n" +
                    "Убедитесь, что все доли уникальны, не были изменены и принадлежат одному файлу.");
            } else if (errorMessage != null && errorMessage.contains("Восстановленные данные пусты")) {
                showAlert("Ошибка восстановления", 
                    "Не удалось восстановить данные. Возможные причины:\n" +
                    "1. Доли были изменены или повреждены\n" +
                    "2. Доли от разных файлов\n" +
                    "3. Недостаточно корректных долей");
            } else {
                showAlert("Ошибка восстановления", 
                    errorMessage != null ? errorMessage : "Неизвестная ошибка\n" +
                    "Проверьте, что все доли являются оригинальными и не были изменены.");
            }
            e.printStackTrace();
        } finally {
            restoreProgressBar.setVisible(false);
            restoreButton.setDisable(false);
        }
    }

    @FXML
    private void openAbout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("about.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("О программе");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String calculateHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "error";
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}