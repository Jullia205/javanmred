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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AppUI extends Application {

    private final ExperimentReader reader = new ExperimentReader();
    private LineChart<Number, Number> chart;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Plataforma de Análise RMN - Equipa 03");

        // 1. Área Central: Gráfico nativo do JavaFX para visualização dos traços
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tempo (s)");
        yAxis.setLabel("Amplitude");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Sinal Bruto no Domínio do Tempo");
        chart.setCreateSymbols(false); // Remove os pontos da linha para otimizar a renderização de milhares de amostras
        chart.setAnimated(false); // Desativa animações para melhorar a performance com dados densos

        // 2. Painel Lateral: Controlos de Importação e Processamento
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(15));
        controlPanel.setStyle("-fx-background-color: #f4f4f4;");

        Label statusLabel = new Label("Nenhum ficheiro carregado.");
        Button btnLoad = new Button("Importar Ficheiro RMN...");
        Button btnProcess = new Button("Processar em Lote");
        btnProcess.setDisable(true); // Desativado até carregar um ficheiro

        // 3. Ação do Botão de Importação
        btnLoad.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Abrir ficheiro de metadados (.json)");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            if (selectedFile != null) {
                try {
                    // Carrega o par .json e .bin utilizando o vosso leitor IO
                    Experiment exp = reader.load(selectedFile.toPath());

                    // CORREÇÃO: Utilizando selectedFile.getName() em vez de exp.getName()
                    statusLabel.setText("Carregado: " + selectedFile.getName() + "\n(" + exp.getScans().size() + " scans)");
                    btnProcess.setDisable(false);

                    // Atualiza o gráfico com o primeiro traço do experimento
                    plotSignal(exp.getScans().get(0));

                } catch (Exception ex) {
                    statusLabel.setText("Erro ao carregar:\n" + ex.getMessage());
                }
            }
        });

        controlPanel.getChildren().addAll(btnLoad, statusLabel, btnProcess);

        // 4. Montagem do Layout Principal (BorderPane)
        BorderPane root = new BorderPane();
        root.setLeft(controlPanel);
        root.setCenter(chart);

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Plota o sinal no gráfico nativo do JavaFX.
     */
    private void plotSignal(TimeSignal signal) {
        chart.getData().clear(); // Limpa gráficos anteriores

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Canal 0");

        double[] timeAxis = signal.timeAxis();
        double[] amplitudes = signal.channel(0);

        // Adiciona os pontos à série do JavaFX
        for (int i = 0; i < amplitudes.length; i++) {
            series.getData().add(new XYChart.Data<>(timeAxis[i], amplitudes[i]));
        }

        chart.getData().add(series);
    }

    public static void main(String[] args) {
        launch(args);
    }
}