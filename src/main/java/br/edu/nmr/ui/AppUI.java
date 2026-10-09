package br.edu.nmr.ui;

import br.edu.nmr.io.ExperimentReader;
import br.edu.nmr.model.Experiment;
import br.edu.nmr.model.TimeSignal;
import br.edu.nmr.service.BatchProcessor;
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
import javafx.scene.control.ComboBox;

import java.io.File;

public class AppUI extends Application {

    private final ExperimentReader reader = new ExperimentReader();
    private LineChart<Number, Number> chart;
    private Experiment currentExperiment;
    private NumberAxis xAxis;
    private NumberAxis yAxis;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Plataforma de Análise RMN - Equipe 03");

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


        Label lblTipo = new Label("Tipo de Ensaio:");
        lblTipo.setStyle("-fx-font-weight: bold;");
        ComboBox<String> comboType = new ComboBox<>();
        comboType.getItems().addAll("Relaxação T1", "Relaxação T2 (CPMG)", "Difusão");
        comboType.setValue("Relaxação T1"); // Valor padrão

        btnProcess.setOnAction(e -> {
            if (currentExperiment == null) return;

            String tipoSelecionado = comboType.getValue();
            statusLabel.setText("A processar modo: " + tipoSelecionado + "...\n(Aguarde, calculando em background)");
            btnProcess.setDisable(true); // Desativa o botão para evitar cliques duplos durante o cálculo

            // Cria uma Thread separada para não congelar a Interface Gráfica
            new Thread(() -> {
                try {
                    if (tipoSelecionado.equals("Relaxação T1")) {
                        java.util.List<?> incList = (java.util.List<?>) currentExperiment.getParameters().increment().raw().get("inc");
                        double stepX = ((Number) incList.get(0)).doubleValue();
                        double startX = currentExperiment.getParameters().preAcquisition().getDouble("poltime", 0.0);

                        br.edu.nmr.service.BatchProcessor batch = new br.edu.nmr.service.BatchProcessor(currentExperiment);
                        br.edu.nmr.model.IntegratedSeries series = batch.processAll(currentExperiment, startX, stepX);

                        br.edu.nmr.analysis.RelaxationFit fitter = new br.edu.nmr.analysis.RelaxationFit();
                        double[] otimizados = fitter.fit(series.getXValues(), series.getYValues(), 1.0, 0.5, 0.0);

                        // Devolve o gráfico e os rótulos à Thread Principal da UI
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText(String.format("T1 Concluído!\nR1 = %.4f s⁻¹\nT1 = %.4f s", otimizados[1], (1.0 / otimizados[1])));
                            plotFitResult(series.getXValues(), series.getYValues(), otimizados);
                            btnProcess.setDisable(false);
                        });

                    } else if (tipoSelecionado.equals("Relaxação T2 (CPMG)")) {
                        System.out.println(">> INICIANDO T2 NA THREAD DE BACKGROUND...");

                        // 1. LER OS PARÂMETROS CORRETOS A PARTIR DO BLOCO "pinc"
                        java.util.List<?> incList = (java.util.List<?>) currentExperiment.getParameters().increment().raw().get("inc");
                        java.util.List<?> nList = (java.util.List<?>) currentExperiment.getParameters().increment().raw().get("n");

                        double tempoEco = ((Number) incList.get(0)).doubleValue();
                        int numEcos = ((Number) nList.get(0)).intValue();

                        br.edu.nmr.model.TimeSignal scanUnico = currentExperiment.getScans().get(0);
                        double[] amplitudes = scanUnico.channel(0);

                        int amostrasPorEco = amplitudes.length / numEcos;

                        System.out.println(">> Total de amostras no sinal: " + amplitudes.length);
                        System.out.println(">> Número de ecos extraídos (n): " + numEcos);
                        System.out.println(">> Amostras recortadas por eco: " + amostrasPorEco);

                        double[] xValues = new double[numEcos];
                        double[] yValues = new double[numEcos];

                        br.edu.nmr.signal.ButterworthFilter filter = new br.edu.nmr.signal.ButterworthFilter(currentExperiment.getParameters());
                        br.edu.nmr.signal.FourierTransform fft = new br.edu.nmr.signal.FourierTransform();
                        br.edu.nmr.analysis.PeakIntegrator integrator = new br.edu.nmr.analysis.PeakIntegrator();

                        // 2. TEMPO DE AMOSTRAGEM CORRETO (Evita o erro ArithmeticException do Nyquist)
                        double dwellTime = scanUnico.dx();

                        for (int i = 0; i < numEcos; i++) {
                            System.out.println(">> A processar eco " + (i + 1) + " de " + numEcos + "...");

                            xValues[i] = (i + 1) * tempoEco;
                            double[] ecoAmplitudes = new double[amostrasPorEco];
                            System.arraycopy(amplitudes, i * amostrasPorEco, ecoAmplitudes, 0, amostrasPorEco);

                            br.edu.nmr.model.TimeSignal ecoVirtual = new br.edu.nmr.model.TimeSignal(new double[][]{ecoAmplitudes}, dwellTime, 0.0);

                            br.edu.nmr.model.TimeSignal filtered = filter.apply(ecoVirtual);
                            br.edu.nmr.signal.Spectrum spectrum = fft.transform(filtered);
                            yValues[i] = integrator.integrate(spectrum, currentExperiment.getParameters());
                        }

                        System.out.println(">> Início do Ajuste de Curva (Fit)...");
                        br.edu.nmr.analysis.RelaxationFit fitter = new br.edu.nmr.analysis.RelaxationFit();
                        double[] otimizados = fitter.fit(xValues, yValues, yValues[0], 10.0, 0.0);

                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText(String.format("T2 Concluído!\nTaxa R2 = %.4f s⁻¹\nTempo T2 = %.4f s", otimizados[1], (1.0 / otimizados[1])));
                            plotFitResult(xValues, yValues, otimizados);
                            btnProcess.setDisable(false);
                            System.out.println(">> Gráfico enviado para o ecrã com sucesso!");
                        });


                    } else if (tipoSelecionado.equals("Difusão")) {
                        System.out.println(">> INICIANDO DIFUSÃO NA THREAD DE BACKGROUND...");

                        java.util.List<?> incList = (java.util.List<?>) currentExperiment.getParameters().increment().raw().get("inc");
                        double stepX = ((Number) incList.get(0)).doubleValue();
                        double startX = currentExperiment.getParameters().preAcquisition().getDouble("g0", 0.0);

                        br.edu.nmr.service.BatchProcessor batch = new br.edu.nmr.service.BatchProcessor(currentExperiment);
                        br.edu.nmr.model.IntegratedSeries series = batch.processAll(currentExperiment, startX, stepX);

                        double bigDelta = currentExperiment.getParameters().preAcquisition().getDouble("tau", 0.05);

                        br.edu.nmr.analysis.DiffusionFit fitter = new br.edu.nmr.analysis.DiffusionFit();

                        double[] otimizados = fitter.fit(series.getXValues(), series.getYValues(), bigDelta, series.getYValues()[0], 1.0);

                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText(String.format("Difusão Concluída!\nCoef. D = %.4e", otimizados[1]));
                            plotFitResult(series.getXValues(), series.getYValues(), otimizados);
                            btnProcess.setDisable(false);
                            System.out.println(">> Gráfico enviado para a tela com sucesso!");
                        });
                    }

                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Erro no processamento:\n" + ex.getMessage());
                        btnProcess.setDisable(false);
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

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
                    this.currentExperiment = exp;
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
                lblTipo, comboType,
                lblZoom, hboxX, hboxY, hboxZoomBtns,
                new Label(""),
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

    /**
     * Plota o sinal no gráfico nativo do JavaFX com decimação de segurança.
     */
    private void plotSignal(TimeSignal signal) {
        chart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Canal 0");

        double[] timeAxis = signal.timeAxis();
        double[] amplitudes = signal.channel(0);

        int totalPoints = amplitudes.length;
        int maxPointsToPlot = 4000;

        int step = Math.max(1, totalPoints / maxPointsToPlot);

        for (int i = 0; i < totalPoints; i += step) {
            series.getData().add(new XYChart.Data<>(timeAxis[i], amplitudes[i]));
        }

        chart.getData().add(series);
    }

    /**
     * Plota os pontos experimentais (integrais) e a linha de tendência (curva ajustada).
     */
    /**
     * Plota os pontos experimentais (integrais) e a linha de tendência (curva ajustada).
     */

    /**
     * Plota os pontos experimentais e a linha de tendência.
     * Identifica automaticamente se é Relaxação (3 params) ou Difusão (2 params).
     */
    private void plotFitResult(double[] x, double[] y, double[] params) {
        chart.getData().clear();

        xAxis.setLabel("Tempo / Duração do Gradiente (s)");
        yAxis.setLabel("Área Integrada do Pico");

        XYChart.Series<Number, Number> scatter = new XYChart.Series<>();
        scatter.setName("Dados Experimentais");

        int maxPointsToPlot = 1000;
        int step = Math.max(1, x.length / maxPointsToPlot);
        for (int i = 0; i < x.length; i += step) {
            scatter.getData().add(new XYChart.Data<>(x[i], y[i]));
        }

        XYChart.Series<Number, Number> line = new XYChart.Series<>();
        line.setName("Ajuste da Curva");

        double minX = x[0];
        double maxX = x[x.length - 1];
        double stepLine = (maxX - minX) / 200.0;

        double bigDelta = 0.0;
        if (currentExperiment != null && params.length == 2) {
            bigDelta = currentExperiment.getParameters().preAcquisition().getDouble("tau", 0.05);
        }

        for (double currentX = minX; currentX <= maxX; currentX += stepLine) {
            double fitY = 0.0;

            if (params.length == 3) {
                // Equação de Relaxação T1 / T2
                fitY = params[0] * Math.exp(-params[1] * currentX) + params[2];
            } else if (params.length == 2) {
                // Equação de Difusão (Stejskal-Tanner)
                fitY = params[0] * Math.exp(-params[1] * (currentX * currentX) * (bigDelta - currentX / 3.0));
            }

            line.getData().add(new XYChart.Data<>(currentX, fitY));
        }

        chart.getData().addAll(scatter, line);
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}