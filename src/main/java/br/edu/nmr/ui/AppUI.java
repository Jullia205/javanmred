package br.edu.nmr.ui;

import br.edu.nmr.io.ExperimentReader;
import br.edu.nmr.model.Experiment;
import br.edu.nmr.model.TimeSignal;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AppUI extends Application {

    private final ExperimentReader reader = new ExperimentReader();
    private LineChart<Number, Number> chart;
    private NumberAxis xAxis;
    private NumberAxis yAxis;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Plataforma de Análise RMN - Equipa 03");

        // 1. Área Central: Gráfico nativo do JavaFX
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        xAxis.setLabel("Tempo (s)");
        yAxis.setLabel("Amplitude");
        xAxis.setForceZeroInRange(false); // Permite que o eixo X dê zoom livremente sem prender no zero

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Sinal Bruto no Domínio do Tempo");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        // 2. Painel Lateral: Controlos de Importação e Processamento
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(15));
        controlPanel.setStyle("-fx-background-color: #f4f4f4;");
        controlPanel.setPrefWidth(280);

        Label statusLabel = new Label("Nenhum ficheiro carregado.");
        Button btnLoad = new Button("Importar Ficheiro RMN...");
        Button btnProcess = new Button("Processar em Lote");
        btnProcess.setDisable(true);

        // 3. Controlos de Zoom do Gráfico
        Label lblZoom = new Label("Controlos de Zoom (Eixos):");
        lblZoom.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        TextField txtXMin = new TextField(); txtXMin.setPromptText("X Mín");
        TextField txtXMax = new TextField(); txtXMax.setPromptText("X Máx");
        HBox hboxX = new HBox(5, txtXMin, txtXMax);

        TextField txtYMin = new TextField(); txtYMin.setPromptText("Y Mín");
        TextField txtYMax = new TextField(); txtYMax.setPromptText("Y Máx");
        HBox hboxY = new HBox(5, txtYMin, txtYMax);

        Button btnApplyZoom = new Button("Aplicar Zoom");
        Button btnResetZoom = new Button("Restaurar Eixos (Auto)");
        HBox hboxZoomBtns = new HBox(5, btnApplyZoom, btnResetZoom);

        // Ação: Aplicar Zoom
        btnApplyZoom.setOnAction(e -> {
            try {
                if (!txtXMin.getText().isEmpty() && !txtXMax.getText().isEmpty()) {
                    xAxis.setAutoRanging(false);
                    xAxis.setLowerBound(Double.parseDouble(txtXMin.getText()));
                    xAxis.setUpperBound(Double.parseDouble(txtXMax.getText()));
                }
                if (!txtYMin.getText().isEmpty() && !txtYMax.getText().isEmpty()) {
                    yAxis.setAutoRanging(false);
                    yAxis.setLowerBound(Double.parseDouble(txtYMin.getText()));
                    yAxis.setUpperBound(Double.parseDouble(txtYMax.getText()));
                }
            } catch (NumberFormatException ex) {
                statusLabel.setText("Erro no zoom: Insira apenas números.");
            }
        });

        // Ação: Restaurar Eixos
        btnResetZoom.setOnAction(e -> {
            xAxis.setAutoRanging(true);
            yAxis.setAutoRanging(true);
            txtXMin.clear(); txtXMax.clear();
            txtYMin.clear(); txtYMax.clear();
        });

        // 4. Ação do Botão de Importação
        btnLoad.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Abrir ficheiro de metadados (.json)");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            if (selectedFile != null) {
                try {
                    Experiment exp = reader.load(selectedFile.toPath());
                    statusLabel.setText("Carregado: " + selectedFile.getName() + "\n(" + exp.getScans().size() + " scans)");
                    btnProcess.setDisable(false);

                    // Restaura os eixos ao carregar um novo ficheiro
                    btnResetZoom.fire();
                    plotSignal(exp.getScans().get(0));

                } catch (Exception ex) {
                    statusLabel.setText("Erro ao carregar:\n" + ex.getMessage());
                }
            }
        });

        controlPanel.getChildren().addAll(
                btnLoad, statusLabel,
                lblZoom, hboxX, hboxY, hboxZoomBtns,
                new Label(""), // Espaçador
                btnProcess
        );

        // 5. Montagem do Layout Principal (BorderPane)
        BorderPane root = new BorderPane();
        root.setLeft(controlPanel);
        root.setCenter(chart);

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void plotSignal(TimeSignal signal) {
        chart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Canal 0");

        double[] timeAxis = signal.timeAxis();
        double[] amplitudes = signal.channel(0);

        for (int i = 0; i < amplitudes.length; i++) {
            series.getData().add(new XYChart.Data<>(timeAxis[i], amplitudes[i]));
        }

        chart.getData().add(series);
    }

    public static void main(String[] args) {
        launch(args);
    }
}