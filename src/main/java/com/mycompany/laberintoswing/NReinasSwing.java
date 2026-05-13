
package com.mycompany.laberintoswing;

/**
 *
 * @author jhoan
 */
import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class NReinasSwing extends JFrame {

    private final int N = 8; // Tamaño del tablero
    private JLabel[][] casillas;
    private JLabel lblEstado;
    private Random rand = new Random();

    public NReinasSwing() {
        // Configuración principal de la ventana
        setTitle("Problema de las N-Reinas - Escalada de la Colina");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel del tablero
        JPanel panelTablero = new JPanel(new GridLayout(N, N));
        casillas = new JLabel[N][N];
        Font fuenteReina = new Font("SansSerif", Font.BOLD, 40);

        for (int fila = 0; fila < N; fila++) {
            for (int col = 0; col < N; col++) {
                casillas[fila][col] = new JLabel("", SwingConstants.CENTER);
                casillas[fila][col].setFont(fuenteReina);
                casillas[fila][col].setOpaque(true);
                casillas[fila][col].setForeground(Color.BLACK);
                
                // Colores intercalados del tablero
                if ((fila + col) % 2 == 0) {
                    casillas[fila][col].setBackground(new Color(240, 217, 181)); // Color claro
                } else {
                    casillas[fila][col].setBackground(new Color(181, 136, 99)); // Color oscuro
                }
                
                panelTablero.add(casillas[fila][col]);
            }
        }

        // Panel de controles inferiores
        JPanel panelControl = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnResolver = new JButton("Resolver N-Reinas");
        btnResolver.setFont(new Font("Arial", Font.BOLD, 14));
        
        lblEstado = new JLabel("Presiona el botón para iniciar la búsqueda heurística.");
        lblEstado.setFont(new Font("Arial", Font.PLAIN, 14));

        btnResolver.addActionListener(e -> resolverYActualizarUI());

        panelControl.add(btnResolver);
        panelControl.add(lblEstado);

        // Agregar paneles a la ventana
        add(panelTablero, BorderLayout.CENTER);
        add(panelControl, BorderLayout.SOUTH);

        setSize(600, 650);
        setLocationRelativeTo(null); // Centrar en la pantalla
    }

    // --- LÓGICA DEL ALGORITMO HEURÍSTICO ---

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

    private void resolverYActualizarUI() {
        int[] tablero = new int[N];
        generarTableroAleatorio(tablero);
        
        int reinicios = 0;
        int movimientos = 0;

        while (true) {
            int conflictosActuales = calcularConflictos(tablero);
            
            if (conflictosActuales == 0) {
                // Se encontró la solución, actualizar la interfaz
                dibujarTablero(tablero);
                lblEstado.setText("¡Solución! Reinicios: " + reinicios + " | Movimientos: " + movimientos);
                break;
            }

            int[] mejorTablero = tablero.clone();
            int mejoresConflictos = conflictosActuales;
            boolean huboMejora = false;

            for (int col = 0; col < N; col++) {
                for (int fila = 0; fila < N; fila++) {
                    if (tablero[col] == fila) continue;
                    
                    int[] nuevoTablero = tablero.clone();
                    nuevoTablero[col] = fila;
                    int nuevosConflictos = calcularConflictos(nuevoTablero);

                    if (nuevosConflictos < mejoresConflictos) {
                        mejoresConflictos = nuevosConflictos;
                        mejorTablero = nuevoTablero.clone();
                        huboMejora = true;
                    }
                }
            }

            if (huboMejora) {
                tablero = mejorTablero;
                movimientos++;
            } else {
                // Atrapado en óptimo local, aplicar reinicio aleatorio
                generarTableroAleatorio(tablero);
                reinicios++;
            }
        }
    }

    // --- MÉTODOS VISUALES ---

    private void dibujarTablero(int[] tablero) {
        // Limpiar tablero previo
        for (int fila = 0; fila < N; fila++) {
            for (int col = 0; col < N; col++) {
                casillas[fila][col].setText(""); 
            }
        }
        
        // Colocar las reinas según el arreglo solución
        for (int col = 0; col < N; col++) {
            int fila = tablero[col];
            casillas[fila][col].setText("♛");
        }
    }

    public static void main(String[] args) {
        // Asegurar que la interfaz se ejecute en el Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            NReinasSwing app = new NReinasSwing();
            app.setVisible(true);
        });
    }
}