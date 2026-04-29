package com.mycompany.laberintoswing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

public class LaberintoSwing extends JFrame {

    static final int PARED = 1;
    static final int CAMINO = 0;
    static final int ENTRADA = 2;
    static final int SALIDA = 3;

    static final Random random = new Random();

    private int[][] laberinto;
    private Celda entrada;
    private Celda salida;
    private Celda jugador;

    private PanelLaberinto panelLaberinto;

    public LaberintoSwing() {
        setTitle("Juego de Laberinto");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        String[] opciones = {
            "Por defecto",
            "Fácil",
            "Normal",
            "Difícil"
        };

        JComboBox<String> comboDificultad = new JComboBox<>(opciones);
        JButton botonGenerar = new JButton("Generar nuevo laberinto");

        JPanel panelSuperior = new JPanel();
        panelSuperior.add(new JLabel("Dificultad:"));
        panelSuperior.add(comboDificultad);
        panelSuperior.add(botonGenerar);

        panelLaberinto = new PanelLaberinto();

        add(panelSuperior, BorderLayout.NORTH);
        add(panelLaberinto, BorderLayout.CENTER);

        botonGenerar.addActionListener(e -> {
            int dificultad = comboDificultad.getSelectedIndex();
            generarNuevoLaberinto(dificultad);
            panelLaberinto.requestFocusInWindow();
        });

        generarNuevoLaberinto(0);

        setSize(850, 900);
        setLocationRelativeTo(null);
        setVisible(true);

        SwingUtilities.invokeLater(() -> panelLaberinto.requestFocusInWindow());
    }

    private void generarNuevoLaberinto(int dificultad) {
        int tamano = obtenerTamano(dificultad);

        laberinto = crearTerreno(tamano, tamano);
        excavarLaberinto(laberinto);
        colocarEntradaYSalida(laberinto);

        jugador = new Celda(entrada.fila, entrada.columna);

        panelLaberinto.setLaberinto(laberinto);
        panelLaberinto.repaint();
    }

    private int obtenerTamano(int dificultad) {
        switch (dificultad) {
            case 1:
                return 21; // Fácil
            case 2:
                return 31; // Normal
            case 3:
                return 41; // Difícil
            default:
                return 25; // Por defecto
        }
    }

    private int[][] crearTerreno(int filas, int columnas) {
        int[][] terreno = new int[filas][columnas];

        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                terreno[fila][columna] = PARED;
            }
        }

        return terreno;
    }

    private void excavarLaberinto(int[][] laberinto) {
        int filas = laberinto.length;
        int columnas = laberinto[0].length;

        int filaInicio = obtenerNumeroImparAleatorio(filas);
        int columnaInicio = obtenerNumeroImparAleatorio(columnas);

        laberinto[filaInicio][columnaInicio] = CAMINO;

        Stack<Celda> lista = new Stack<>();
        lista.push(new Celda(filaInicio, columnaInicio));

        while (!lista.isEmpty()) {
            Celda actual = lista.peek();

            List<Celda> vecinosValidos = obtenerVecinosValidos(laberinto, actual);

            if (vecinosValidos.isEmpty()) {
                lista.pop();
            } else {
                Celda vecino = vecinosValidos.get(random.nextInt(vecinosValidos.size()));

                int filaIntermedia = (actual.fila + vecino.fila) / 2;
                int columnaIntermedia = (actual.columna + vecino.columna) / 2;

                laberinto[filaIntermedia][columnaIntermedia] = CAMINO;
                laberinto[vecino.fila][vecino.columna] = CAMINO;

                lista.push(vecino);
            }
        }
    }

    private List<Celda> obtenerVecinosValidos(int[][] laberinto, Celda actual) {
        List<Celda> vecinos = new ArrayList<>();

        int[][] direcciones = {
            {-2, 0}, // arriba
            {2, 0},  // abajo
            {0, -2}, // izquierda
            {0, 2}   // derecha
        };

        for (int[] direccion : direcciones) {
            int nuevaFila = actual.fila + direccion[0];
            int nuevaColumna = actual.columna + direccion[1];

            if (estaDentro(laberinto, nuevaFila, nuevaColumna)
                    && laberinto[nuevaFila][nuevaColumna] == PARED) {
                vecinos.add(new Celda(nuevaFila, nuevaColumna));
            }
        }

        return vecinos;
    }

    private boolean estaDentro(int[][] laberinto, int fila, int columna) {
        return fila > 0
                && fila < laberinto.length - 1
                && columna > 0
                && columna < laberinto[0].length - 1;
    }

    private int obtenerNumeroImparAleatorio(int limite) {
        int numero;

        do {
            numero = random.nextInt(limite);
        } while (numero % 2 == 0 || numero <= 0 || numero >= limite - 1);

        return numero;
    }

    private void colocarEntradaYSalida(int[][] laberinto) {
        List<Celda> caminos = obtenerCaminos(laberinto);

        do {
            entrada = caminos.get(random.nextInt(caminos.size()));
            salida = caminos.get(random.nextInt(caminos.size()));
        } while (
                mismaCelda(entrada, salida)
                || !existeRuta(laberinto, entrada, salida)
                || distanciaManhattan(entrada, salida) < laberinto.length / 2
        );

        laberinto[entrada.fila][entrada.columna] = ENTRADA;
        laberinto[salida.fila][salida.columna] = SALIDA;
    }

    private int distanciaManhattan(Celda a, Celda b) {
        return Math.abs(a.fila - b.fila) + Math.abs(a.columna - b.columna);
    }

    private boolean mismaCelda(Celda a, Celda b) {
        return a.fila == b.fila && a.columna == b.columna;
    }

    private List<Celda> obtenerCaminos(int[][] laberinto) {
        List<Celda> caminos = new ArrayList<>();

        for (int fila = 0; fila < laberinto.length; fila++) {
            for (int columna = 0; columna < laberinto[0].length; columna++) {
                if (laberinto[fila][columna] == CAMINO) {
                    caminos.add(new Celda(fila, columna));
                }
            }
        }

        return caminos;
    }

    private boolean existeRuta(int[][] laberinto, Celda entrada, Celda salida) {
        boolean[][] visitado = new boolean[laberinto.length][laberinto[0].length];
        Queue<Celda> cola = new LinkedList<>();

        cola.add(entrada);
        visitado[entrada.fila][entrada.columna] = true;

        int[][] direcciones = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
        };

        while (!cola.isEmpty()) {
            Celda actual = cola.poll();

            if (mismaCelda(actual, salida)) {
                return true;
            }

            for (int[] direccion : direcciones) {
                int nuevaFila = actual.fila + direccion[0];
                int nuevaColumna = actual.columna + direccion[1];

                if (nuevaFila >= 0
                        && nuevaFila < laberinto.length
                        && nuevaColumna >= 0
                        && nuevaColumna < laberinto[0].length
                        && !visitado[nuevaFila][nuevaColumna]
                        && laberinto[nuevaFila][nuevaColumna] != PARED) {

                    visitado[nuevaFila][nuevaColumna] = true;
                    cola.add(new Celda(nuevaFila, nuevaColumna));
                }
            }
        }

        return false;
    }

    private void moverJugador(int cambioFila, int cambioColumna) {
        int nuevaFila = jugador.fila + cambioFila;
        int nuevaColumna = jugador.columna + cambioColumna;

        if (nuevaFila < 0
                || nuevaFila >= laberinto.length
                || nuevaColumna < 0
                || nuevaColumna >= laberinto[0].length) {
            return;
        }

        if (laberinto[nuevaFila][nuevaColumna] == PARED) {
            return;
        }

        jugador.fila = nuevaFila;
        jugador.columna = nuevaColumna;

        panelLaberinto.repaint();

        if (jugador.fila == salida.fila && jugador.columna == salida.columna) {
            JOptionPane.showMessageDialog(
                    this,
                    "¡Ganaste! Llegaste a la salida.",
                    "Laberinto completado",
                    JOptionPane.INFORMATION_MESSAGE
            );

            generarNuevoLaberinto(0);
            panelLaberinto.requestFocusInWindow();
        }
    }

    static class Celda {

        int fila;
        int columna;

        Celda(int fila, int columna) {
            this.fila = fila;
            this.columna = columna;
        }
    }

    class PanelLaberinto extends JPanel {

        private int[][] laberinto;

        public PanelLaberinto() {
            setFocusable(true);
            setBackground(Color.WHITE);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP:
                            moverJugador(-1, 0);
                            break;

                        case KeyEvent.VK_DOWN:
                            moverJugador(1, 0);
                            break;

                        case KeyEvent.VK_LEFT:
                            moverJugador(0, -1);
                            break;

                        case KeyEvent.VK_RIGHT:
                            moverJugador(0, 1);
                            break;
                    }
                }
            });
        }

        public void setLaberinto(int[][] laberinto) {
            this.laberinto = laberinto;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (laberinto == null) {
                return;
            }

            int filas = laberinto.length;
            int columnas = laberinto[0].length;

            int anchoCelda = getWidth() / columnas;
            int altoCelda = getHeight() / filas;
            int tamanoCelda = Math.min(anchoCelda, altoCelda);

            int offsetX = (getWidth() - columnas * tamanoCelda) / 2;
            int offsetY = (getHeight() - filas * tamanoCelda) / 2;

            for (int fila = 0; fila < filas; fila++) {
                for (int columna = 0; columna < columnas; columna++) {
                    int x = offsetX + columna * tamanoCelda;
                    int y = offsetY + fila * tamanoCelda;

                    if (laberinto[fila][columna] == PARED) {
                        g.setColor(Color.DARK_GRAY);
                    } else if (laberinto[fila][columna] == CAMINO) {
                        g.setColor(Color.WHITE);
                    } else if (laberinto[fila][columna] == ENTRADA) {
                        g.setColor(Color.GREEN);
                    } else if (laberinto[fila][columna] == SALIDA) {
                        g.setColor(Color.RED);
                    }

                    g.fillRect(x, y, tamanoCelda, tamanoCelda);

                    g.setColor(Color.BLACK);
                    g.drawRect(x, y, tamanoCelda, tamanoCelda);
                }
            }

            dibujarJugador(g, tamanoCelda, offsetX, offsetY);
            dibujarTexto(g);
        }

        private void dibujarJugador(Graphics g, int tamanoCelda, int offsetX, int offsetY) {
            if (jugador == null) {
                return;
            }

            int x = offsetX + jugador.columna * tamanoCelda;
            int y = offsetY + jugador.fila * tamanoCelda;

            g.setColor(Color.BLUE);
            g.fillOval(
                    x + 3,
                    y + 3,
                    tamanoCelda - 6,
                    tamanoCelda - 6
            );
        }

        private void dibujarTexto(Graphics g) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 14));

            g.drawString(
                    "Usa las flechas del teclado para mover el jugador azul hasta la meta roja.",
                    20,
                    25
            );

            int y = getHeight() - 20;

            g.setColor(Color.GREEN);
            g.fillRect(20, y - 12, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString("Entrada", 45, y);

            g.setColor(Color.RED);
            g.fillRect(130, y - 12, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString("Salida", 155, y);

            g.setColor(Color.BLUE);
            g.fillOval(230, y - 12, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString("Jugador", 255, y);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(340, y - 12, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString("Pared", 365, y);

            g.setColor(Color.WHITE);
            g.fillRect(430, y - 12, 15, 15);
            g.setColor(Color.BLACK);
            g.drawRect(430, y - 12, 15, 15);
            g.drawString("Camino", 455, y);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LaberintoSwing::new);
    }
}