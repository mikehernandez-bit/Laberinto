package com.mycompany.NReinas;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Random;

public class NReinasSwing extends JFrame {

    private final int N = 8;
    private JLabel[][] casillas;
    private JLabel lblEstado;
    private JTextArea areaRegistro;
    private JButton btnResolver;
    private Random rand = new Random();

    // 1. Definir un color diferente para cada una de las 8 reinas
    private final Color[] coloresReinas = {
        new Color(220, 20, 60),   // Rojo Carmesí
        new Color(0, 102, 204),   // Azul Fuerte
        new Color(34, 139, 34),   // Verde Bosque
        new Color(255, 140, 0),   // Naranja Oscuro
        new Color(138, 43, 226),  // Púrpura
        new Color(0, 139, 139),   // Verde Azulado (Teal)
        new Color(255, 20, 147),  // Rosa Profundo
        new Color(100, 100, 100)  // Gris Oscuro
    };

    // Matriz para guardar por dónde ha pasado cada reina [fila][columna]
    private boolean[][] estelas = new boolean[N][N];

    public NReinasSwing() {
        setTitle("N-Reinas: Escalada de Colina Animada con Estelas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel del Tablero
        JPanel panelTablero = new JPanel(new GridLayout(N, N));
        casillas = new JLabel[N][N];
        Font fuenteReina = new Font("SansSerif", Font.BOLD, 40);

        for (int fila = 0; fila < N; fila++) {
            for (int col = 0; col < N; col++) {
                casillas[fila][col] = new JLabel("", SwingConstants.CENTER);
                casillas[fila][col].setFont(fuenteReina);
                casillas[fila][col].setOpaque(true);
                
                // Fondo de tablero de ajedrez
                if ((fila + col) % 2 == 0) {
                    casillas[fila][col].setBackground(new Color(240, 217, 181)); 
                } else {
                    casillas[fila][col].setBackground(new Color(181, 136, 99)); 
                }
                
                panelTablero.add(casillas[fila][col]);
            }
        }

        // Panel de Registro
        areaRegistro = new JTextArea(15, 32);
        areaRegistro.setEditable(false);
        areaRegistro.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollRegistro = new JScrollPane(areaRegistro);
        scrollRegistro.setBorder(BorderFactory.createTitledBorder("Historial de Movimientos"));

        // Panel Inferior
        JPanel panelControl = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnResolver = new JButton("▶ Iniciar Búsqueda Animada");
        btnResolver.setFont(new Font("Arial", Font.BOLD, 14));
        
        lblEstado = new JLabel("Presiona el botón para comenzar.");
        lblEstado.setFont(new Font("Arial", Font.PLAIN, 14));

        btnResolver.addActionListener(e -> iniciarAnimacion());

        panelControl.add(btnResolver);
        panelControl.add(lblEstado);

        add(panelTablero, BorderLayout.CENTER);
        add(scrollRegistro, BorderLayout.EAST);
        add(panelControl, BorderLayout.SOUTH);

        setSize(950, 650);
        setLocationRelativeTo(null); 
    }

    // --- LÓGICA HEURÍSTICA Y ANIMACIÓN (SWING WORKER) ---

    private void iniciarAnimacion() {
        btnResolver.setEnabled(false); // Desactivar botón mientras corre
        areaRegistro.setText("--- INICIO DE BÚSQUEDA ---\n");

        // SwingWorker permite ejecutar el ciclo while en un hilo separado
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            int[] tablero = new int[N];
            int reinicios = 0;
            int movimientos = 0;

            @Override
            protected Void doInBackground() throws Exception {
                generarTableroAleatorio(tablero);
                limpiarEstelas();
                publish("Conflictos iniciales: " + calcularConflictos(tablero) + "\n\n");
                actualizarTableroVisual(tablero);
                Thread.sleep(500); // Pausa inicial

                while (true) {
                    int conflictosActuales = calcularConflictos(tablero);
                    
                    if (conflictosActuales == 0) {
                        publish("\n¡SOLUCIÓN ENCONTRADA!\n");
                        break;
                    }

                    int[] mejorTablero = tablero.clone();
                    int mejoresConflictos = conflictosActuales;
                    boolean huboMejora = false;
                    
                    int colMovida = -1;
                    int filaAntigua = -1;
                    int filaNueva = -1;

                    // Evaluar vecinos
                    for (int col = 0; col < N; col++) {
                        for (int fila = 0; fila < N; fila++) {
                            if (tablero[col] == fila) continue;
                            
                            int[] nuevoTablero = tablero.clone();
                            nuevoTablero[col] = fila;
                            int nuevosConflictos = calcularConflictos(nuevoTablero);

                            if (nuevosConflictos < mejoresConflictos) {
                                mejoresConflictos = nuevosConflictos;
                                mejorTablero = nuevoTablero.clone();
                                colMovida = col;
                                filaAntigua = tablero[col];
                                filaNueva = fila;
                                huboMejora = true;
                            }
                        }
                    }

                    if (huboMejora) {
                        movimientos++;
                        // Dejar la estela en la posición que la reina abandona
                        estelas[filaAntigua][colMovida] = true; 
                        tablero = mejorTablero;
                        
                        publish(String.format("Paso %d: Reina %d (Col) -> Fila %d | Conflictos: %d\n", 
                                              movimientos, colMovida, filaNueva, mejoresConflictos));
                        
                        actualizarTableroVisual(tablero);
                        Thread.sleep(250); // PAUSA DE ANIMACIÓN (250 milisegundos)
                        
                    } else {
                        // Atrapado: Reinicio Aleatorio
                        reinicios++;
                        publish(String.format("\n[!] Óptimo local atascado en %d conflictos.\n", conflictosActuales));
                        publish(String.format(">>> REINICIO ALEATORIO #%d <<<\n\n", reinicios));
                        
                        generarTableroAleatorio(tablero);
                        limpiarEstelas(); // Al reiniciar, borramos los rastros viejos
                        
                        actualizarTableroVisual(tablero);
                        Thread.sleep(500); // Pausa larga para notar el reinicio
                    }
                }
                return null;
            }

            // Este método actualiza el área de texto en tiempo real
            @Override
            protected void process(List<String> chunks) {
                for (String texto : chunks) {
                    areaRegistro.append(texto);
                }
                areaRegistro.setCaretPosition(areaRegistro.getDocument().getLength());
            }

            // Se ejecuta cuando el algoritmo termina
            @Override
            protected void done() {
                btnResolver.setEnabled(true);
                lblEstado.setText("¡Éxito! Reinicios: " + reinicios + " | Movimientos: " + movimientos);
                lblEstado.setForeground(new Color(0, 150, 0));
            }
        };
        
        worker.execute(); 
    }

    // --- MÉTODOS AUXILIARES ---

    private int calcularConflictos(int[] tablero) {
        int conflictos = 0;
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                if (tablero[i] == tablero[j] || Math.abs(tablero[i] - tablero[j]) == Math.abs(i - j)) {
                    conflictos++;
                }
            }
        }
        return conflictos;
    }

    private void generarTableroAleatorio(int[] tablero) {
        for (int i = 0; i < N; i++) {
            tablero[i] = rand.nextInt(N);
        }
    }

    private void limpiarEstelas() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                estelas[i][j] = false;
            }
        }
    }

    private void actualizarTableroVisual(int[] tablero) {
        SwingUtilities.invokeLater(() -> {
            // Limpiar todo el texto primero
            for (int f = 0; f < N; f++) {
                for (int c = 0; c < N; c++) {
                    casillas[f][c].setText("");
                }
            }
            
            // Dibujar las estelas (Rastros)
            for (int f = 0; f < N; f++) {
                for (int c = 0; c < N; c++) {
                    if (estelas[f][c]) {
                        casillas[f][c].setText("•"); 
                        casillas[f][c].setForeground(coloresReinas[c]); 
                    }
                }
            }
            
            // Dibujar las Reinas Actuales
            for (int c = 0; c < N; c++) {
                int f = tablero[c];
                casillas[f][c].setText("♛");
                casillas[f][c].setForeground(coloresReinas[c]); 
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NReinasSwing app = new NReinasSwing();
            app.setVisible(true);
        });
    }
}