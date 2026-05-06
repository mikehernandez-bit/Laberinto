package com.mycompany.laberintoswing;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class LaberintoSwing extends JFrame {

    static final int CAMINO = 0;
    static final int PARED = 1;
    static final int ENTRADA = 2;
    static final int SALIDA = 3;
    static final int EXPLORADO = 4;
    static final int SOLUCION = 5;

    static final Random random = new Random();

    private int[][] laberinto;
    private Celda entrada;
    private Celda salida;
    private Celda jugador;

    private PanelLaberinto panelLaberinto;
    private JComboBox<String> comboDificultad;
    private JButton botonGenerar;
    private JButton botonResolver;
    private JButton botonLimpiar;

    private javax.swing.Timer timer;
    private ResultadoBusqueda resultadoActual;
    private int indiceExplorado;
    private int indiceSolucion;
    private boolean mostrandoSolucion;

    public LaberintoSwing() {
        setTitle("Laberinto Autónomo - Prim + Caminos Extra + A*");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        String[] opciones = {
                "Por defecto",
                "Fácil",
                "Normal",
                "Difícil"
        };

        comboDificultad = new JComboBox<>(opciones);
        botonGenerar = new JButton("Generar nuevo laberinto");
        botonResolver = new JButton("Resolver automáticamente");
        botonLimpiar = new JButton("Limpiar recorrido");

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSuperior.add(new JLabel("Dificultad:"));
        panelSuperior.add(comboDificultad);
        panelSuperior.add(botonGenerar);
        panelSuperior.add(botonResolver);
        panelSuperior.add(botonLimpiar);

        panelLaberinto = new PanelLaberinto();

        add(panelSuperior, BorderLayout.NORTH);
        add(panelLaberinto, BorderLayout.CENTER);

        botonGenerar.addActionListener(e -> {
            int dificultad = comboDificultad.getSelectedIndex();
            generarNuevoLaberinto(dificultad);
        });

        botonResolver.addActionListener(e -> resolverAutomaticamente());

        botonLimpiar.addActionListener(e -> {
            detenerTimer();
            limpiarRecorridoAnterior();
            jugador = new Celda(entrada.fila, entrada.columna);
            panelLaberinto.repaint();
        });

        generarNuevoLaberinto(0);

        setSize(1100, 900);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void generarNuevoLaberinto(int dificultad) {
        detenerTimer();

        int tamano = obtenerTamano(dificultad);
        laberinto = crearTerreno(tamano, tamano);

        /*
         * ALGORITMO 1: Prim aleatorizado.
         * Este algoritmo genera el laberinto base desde paredes completas.
         * Es diferente al DFS de la versión anterior.
         */
        generarConPrimAleatorizado(laberinto);

        /*
         * ALGORITMO 2: Braid maze / apertura de ciclos.
         * Rompe paredes estratégicamente para crear varios caminos alternativos.
         * Esto hace que el laberinto no tenga una resolución tan lineal.
         */
        crearCaminosAlternativos(laberinto, dificultad);

        /*
         * ALGORITMO 3: BFS.
         * Busca dos puntos muy alejados para colocar una sola entrada y una sola
         * salida.
         */
        colocarEntradaYSalidaLejanas(laberinto);

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

    private void generarConPrimAleatorizado(int[][] laberinto) {
        int filaInicio = obtenerNumeroImparAleatorio(laberinto.length);
        int columnaInicio = obtenerNumeroImparAleatorio(laberinto[0].length);

        laberinto[filaInicio][columnaInicio] = CAMINO;

        List<ParedCandidata> paredes = new ArrayList<>();
        agregarParedesCandidatas(laberinto, filaInicio, columnaInicio, paredes);

        while (!paredes.isEmpty()) {
            int indice = random.nextInt(paredes.size());
            ParedCandidata candidata = paredes.remove(indice);

            Celda destino = candidata.destino;

            if (!estaDentroDelAreaInterna(laberinto, destino.fila, destino.columna)) {
                continue;
            }

            if (laberinto[destino.fila][destino.columna] == PARED) {
                laberinto[candidata.pared.fila][candidata.pared.columna] = CAMINO;
                laberinto[destino.fila][destino.columna] = CAMINO;

                agregarParedesCandidatas(laberinto, destino.fila, destino.columna, paredes);
            }
        }
    }

    private void agregarParedesCandidatas(
            int[][] laberinto,
            int fila,
            int columna,
            List<ParedCandidata> paredes) {
        int[][] direcciones = {
                { -2, 0 },
                { 2, 0 },
                { 0, -2 },
                { 0, 2 }
        };

        for (int[] direccion : direcciones) {
            int filaDestino = fila + direccion[0];
            int columnaDestino = columna + direccion[1];

            if (!estaDentroDelAreaInterna(laberinto, filaDestino, columnaDestino)) {
                continue;
            }

            if (laberinto[filaDestino][columnaDestino] == PARED) {
                int filaPared = fila + direccion[0] / 2;
                int columnaPared = columna + direccion[1] / 2;

                paredes.add(new ParedCandidata(
                        new Celda(filaPared, columnaPared),
                        new Celda(filaDestino, columnaDestino)));
            }
        }
    }

    private boolean estaDentroDelAreaInterna(int[][] laberinto, int fila, int columna) {
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

    private void crearCaminosAlternativos(int[][] laberinto, int dificultad) {
        int cantidadObjetivo;

        switch (dificultad) {
            case 1:
                cantidadObjetivo = laberinto.length / 2;
                break;
            case 2:
                cantidadObjetivo = laberinto.length;
                break;
            case 3:
                cantidadObjetivo = laberinto.length * 2;
                break;
            default:
                cantidadObjetivo = laberinto.length;
                break;
        }

        int abiertos = 0;
        int intentos = 0;
        int maxIntentos = cantidadObjetivo * 40;

        while (abiertos < cantidadObjetivo && intentos < maxIntentos) {
            intentos++;

            int fila = 1 + random.nextInt(laberinto.length - 2);
            int columna = 1 + random.nextInt(laberinto[0].length - 2);

            if (laberinto[fila][columna] != PARED) {
                continue;
            }

            int vecinosCamino = contarVecinosCamino(laberinto, fila, columna);

            if (vecinosCamino >= 2) {
                laberinto[fila][columna] = CAMINO;
                abiertos++;
            }
        }

        reducirCallejonesSinSalida(laberinto, dificultad);
    }

    private void reducirCallejonesSinSalida(int[][] laberinto, int dificultad) {
        int porcentaje;

        switch (dificultad) {
            case 1:
                porcentaje = 20;
                break;
            case 2:
                porcentaje = 35;
                break;
            case 3:
                porcentaje = 50;
                break;
            default:
                porcentaje = 30;
                break;
        }

        for (int fila = 1; fila < laberinto.length - 1; fila++) {
            for (int columna = 1; columna < laberinto[0].length - 1; columna++) {
                if (laberinto[fila][columna] == CAMINO
                        && contarVecinosCamino(laberinto, fila, columna) == 1
                        && random.nextInt(100) < porcentaje) {

                    Celda paredParaAbrir = elegirParedVecinaParaAbrir(laberinto, fila, columna);

                    if (paredParaAbrir != null) {
                        laberinto[paredParaAbrir.fila][paredParaAbrir.columna] = CAMINO;
                    }
                }
            }
        }
    }

    private Celda elegirParedVecinaParaAbrir(int[][] laberinto, int fila, int columna) {
        List<Celda> paredes = new ArrayList<>();

        int[][] direcciones = {
                { -1, 0 },
                { 1, 0 },
                { 0, -1 },
                { 0, 1 }
        };

        for (int[] direccion : direcciones) {
            int nuevaFila = fila + direccion[0];
            int nuevaColumna = columna + direccion[1];

            if (estaDentroDelAreaInterna(laberinto, nuevaFila, nuevaColumna)
                    && laberinto[nuevaFila][nuevaColumna] == PARED) {
                paredes.add(new Celda(nuevaFila, nuevaColumna));
            }
        }

        if (paredes.isEmpty()) {
            return null;
        }

        return paredes.get(random.nextInt(paredes.size()));
    }

    private int contarVecinosCamino(int[][] laberinto, int fila, int columna) {
        int contador = 0;

        int[][] direcciones = {
                { -1, 0 },
                { 1, 0 },
                { 0, -1 },
                { 0, 1 }
        };

        for (int[] direccion : direcciones) {
            int nuevaFila = fila + direccion[0];
            int nuevaColumna = columna + direccion[1];

            if (nuevaFila >= 0
                    && nuevaFila < laberinto.length
                    && nuevaColumna >= 0
                    && nuevaColumna < laberinto[0].length
                    && laberinto[nuevaFila][nuevaColumna] == CAMINO) {
                contador++;
            }
        }

        return contador;
    }

    private void colocarEntradaYSalidaLejanas(int[][] laberinto) {
        List<Celda> caminos = obtenerCaminos(laberinto);

        Celda puntoAleatorio = caminos.get(random.nextInt(caminos.size()));
        Celda extremo1 = encontrarCeldaMasLejana(laberinto, puntoAleatorio);
        Celda extremo2 = encontrarCeldaMasLejana(laberinto, extremo1);

        entrada = extremo1;
        salida = extremo2;

        laberinto[entrada.fila][entrada.columna] = ENTRADA;
        laberinto[salida.fila][salida.columna] = SALIDA;
    }

    private Celda encontrarCeldaMasLejana(int[][] laberinto, Celda inicio) {
        boolean[][] visitado = new boolean[laberinto.length][laberinto[0].length];
        int[][] distancia = new int[laberinto.length][laberinto[0].length];

        Queue<Celda> cola = new LinkedList<>();
        cola.add(inicio);
        visitado[inicio.fila][inicio.columna] = true;

        Celda masLejana = inicio;

        int[][] direcciones = {
                { -1, 0 },
                { 1, 0 },
                { 0, -1 },
                { 0, 1 }
        };

        while (!cola.isEmpty()) {
            Celda actual = cola.poll();

            if (distancia[actual.fila][actual.columna] > distancia[masLejana.fila][masLejana.columna]) {
                masLejana = actual;
            }

            for (int[] direccion : direcciones) {
                int nuevaFila = actual.fila + direccion[0];
                int nuevaColumna = actual.columna + direccion[1];

                if (esPosicionValidaParaCaminar(laberinto, nuevaFila, nuevaColumna)
                        && !visitado[nuevaFila][nuevaColumna]) {

                    visitado[nuevaFila][nuevaColumna] = true;
                    distancia[nuevaFila][nuevaColumna] = distancia[actual.fila][actual.columna] + 1;

                    cola.add(new Celda(nuevaFila, nuevaColumna));
                }
            }
        }

        return masLejana;
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

    private void resolverAutomaticamente() {
        detenerTimer();
        limpiarRecorridoAnterior();

        resultadoActual = resolverConAEstrella(laberinto, entrada, salida);

        if (resultadoActual.camino.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se encontró una ruta hacia la salida.",
                    "Sin solución",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        jugador = new Celda(entrada.fila, entrada.columna);
        indiceExplorado = 0;
        indiceSolucion = 0;
        mostrandoSolucion = false;

        timer = new javax.swing.Timer(25, e -> animarBusquedaYSolucion());
        timer.start();
    }

    private void animarBusquedaYSolucion() {
        if (!mostrandoSolucion) {
            if (indiceExplorado < resultadoActual.explorados.size()) {
                Celda actual = resultadoActual.explorados.get(indiceExplorado);

                if (!mismaCelda(actual, entrada) && !mismaCelda(actual, salida)) {
                    laberinto[actual.fila][actual.columna] = EXPLORADO;
                }

                indiceExplorado++;
                panelLaberinto.repaint();
                return;
            }

            mostrandoSolucion = true;
            indiceSolucion = 0;
            timer.setDelay(70);
        }

        if (indiceSolucion < resultadoActual.camino.size()) {
            Celda paso = resultadoActual.camino.get(indiceSolucion);

            jugador.fila = paso.fila;
            jugador.columna = paso.columna;

            if (!mismaCelda(paso, entrada) && !mismaCelda(paso, salida)) {
                laberinto[paso.fila][paso.columna] = SOLUCION;
            }

            indiceSolucion++;
            panelLaberinto.repaint();
            return;
        }

        detenerTimer();

        JOptionPane.showMessageDialog(
                this,
                "El laberinto fue resuelto automáticamente con A*.",
                "Solución completada",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private ResultadoBusqueda resolverConAEstrella(int[][] laberinto, Celda inicio, Celda meta) {
        PriorityQueue<Nodo> abiertos = new PriorityQueue<>(
                Comparator.comparingInt((Nodo n) -> n.f)
                        .thenComparingInt(n -> n.h));

        boolean[][] cerrado = new boolean[laberinto.length][laberinto[0].length];
        int[][] costoG = new int[laberinto.length][laberinto[0].length];
        Celda[][] padre = new Celda[laberinto.length][laberinto[0].length];
        List<Celda> explorados = new ArrayList<>();

        for (int fila = 0; fila < costoG.length; fila++) {
            Arrays.fill(costoG[fila], Integer.MAX_VALUE);
        }

        costoG[inicio.fila][inicio.columna] = 0;

        abiertos.add(new Nodo(
                inicio,
                0,
                calcularHeuristica(inicio, meta)));

        int[][] direcciones = {
                { -1, 0 },
                { 1, 0 },
                { 0, -1 },
                { 0, 1 }
        };

        while (!abiertos.isEmpty()) {
            Nodo nodoActual = abiertos.poll();
            Celda actual = nodoActual.celda;

            if (cerrado[actual.fila][actual.columna]) {
                continue;
            }

            cerrado[actual.fila][actual.columna] = true;
            explorados.add(new Celda(actual.fila, actual.columna));

            if (mismaCelda(actual, meta)) {
                List<Celda> camino = reconstruirCamino(padre, inicio, meta);
                return new ResultadoBusqueda(explorados, camino);
            }

            for (int[] direccion : direcciones) {
                int nuevaFila = actual.fila + direccion[0];
                int nuevaColumna = actual.columna + direccion[1];

                if (!esPosicionValidaParaCaminar(laberinto, nuevaFila, nuevaColumna)) {
                    continue;
                }

                if (cerrado[nuevaFila][nuevaColumna]) {
                    continue;
                }

                int nuevoCostoG = costoG[actual.fila][actual.columna] + 1;

                if (nuevoCostoG < costoG[nuevaFila][nuevaColumna]) {
                    costoG[nuevaFila][nuevaColumna] = nuevoCostoG;
                    padre[nuevaFila][nuevaColumna] = new Celda(actual.fila, actual.columna);

                    Celda vecino = new Celda(nuevaFila, nuevaColumna);
                    int h = calcularHeuristica(vecino, meta);

                    abiertos.add(new Nodo(vecino, nuevoCostoG, h));
                }
            }
        }

        return new ResultadoBusqueda(explorados, new ArrayList<>());
    }

    private int calcularHeuristica(Celda a, Celda b) {
        return Math.abs(a.fila - b.fila) + Math.abs(a.columna - b.columna);
    }

    private List<Celda> reconstruirCamino(Celda[][] padre, Celda inicio, Celda meta) {
        List<Celda> camino = new ArrayList<>();
        Celda actual = new Celda(meta.fila, meta.columna);

        camino.add(actual);

        while (!mismaCelda(actual, inicio)) {
            actual = padre[actual.fila][actual.columna];

            if (actual == null) {
                return new ArrayList<>();
            }

            camino.add(new Celda(actual.fila, actual.columna));
        }

        Collections.reverse(camino);
        return camino;
    }

    private boolean esPosicionValidaParaCaminar(int[][] laberinto, int fila, int columna) {
        if (fila < 0
                || fila >= laberinto.length
                || columna < 0
                || columna >= laberinto[0].length) {
            return false;
        }

        return laberinto[fila][columna] != PARED;
    }

    private void limpiarRecorridoAnterior() {
        if (laberinto == null || entrada == null || salida == null) {
            return;
        }

        for (int fila = 0; fila < laberinto.length; fila++) {
            for (int columna = 0; columna < laberinto[0].length; columna++) {
                if (laberinto[fila][columna] == EXPLORADO
                        || laberinto[fila][columna] == SOLUCION) {
                    laberinto[fila][columna] = CAMINO;
                }
            }
        }

        laberinto[entrada.fila][entrada.columna] = ENTRADA;
        laberinto[salida.fila][salida.columna] = SALIDA;
    }

    private void detenerTimer() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    private boolean mismaCelda(Celda a, Celda b) {
        return a != null
                && b != null
                && a.fila == b.fila
                && a.columna == b.columna;
    }

    static class Celda {

        int fila;
        int columna;

        Celda(int fila, int columna) {
            this.fila = fila;
            this.columna = columna;
        }
    }

    static class ParedCandidata {

        Celda pared;
        Celda destino;

        ParedCandidata(Celda pared, Celda destino) {
            this.pared = pared;
            this.destino = destino;
        }
    }

    static class Nodo {

        Celda celda;
        int g;
        int h;
        int f;

        Nodo(Celda celda, int g, int h) {
            this.celda = celda;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }
    }

    static class ResultadoBusqueda {

        List<Celda> explorados;
        List<Celda> camino;

        ResultadoBusqueda(List<Celda> explorados, List<Celda> camino) {
            this.explorados = explorados;
            this.camino = camino;
        }
    }

    class PanelLaberinto extends JPanel {

        private int[][] laberintoPanel;

        public PanelLaberinto() {
            setBackground(Color.WHITE);
        }

        public void setLaberinto(int[][] laberinto) {
            this.laberintoPanel = laberinto;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (laberintoPanel == null) {
                return;
            }

            int filas = laberintoPanel.length;
            int columnas = laberintoPanel[0].length;

            int margenSuperior = 45;
            int margenInferior = 80;

            int anchoDisponible = getWidth();
            int altoDisponible = getHeight() - margenSuperior - margenInferior;

            int anchoCelda = anchoDisponible / columnas;
            int altoCelda = altoDisponible / filas;
            int tamanoCelda = Math.max(4, Math.min(anchoCelda, altoCelda));

            int offsetX = (getWidth() - columnas * tamanoCelda) / 2;
            int offsetY = margenSuperior;

            for (int fila = 0; fila < filas; fila++) {
                for (int columna = 0; columna < columnas; columna++) {
                    int x = offsetX + columna * tamanoCelda;
                    int y = offsetY + fila * tamanoCelda;

                    switch (laberintoPanel[fila][columna]) {
                        case PARED:
                            g.setColor(Color.DARK_GRAY);
                            break;
                        case CAMINO:
                            g.setColor(Color.WHITE);
                            break;
                        case ENTRADA:
                            g.setColor(Color.GREEN);
                            break;
                        case SALIDA:
                            g.setColor(Color.RED);
                            break;
                        case EXPLORADO:
                            g.setColor(Color.ORANGE);
                            break;
                        case SOLUCION:
                            g.setColor(Color.CYAN);
                            break;
                        default:
                            g.setColor(Color.WHITE);
                            break;
                    }

                    g.fillRect(x, y, tamanoCelda, tamanoCelda);

                    g.setColor(Color.LIGHT_GRAY);
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

            int margen = Math.max(2, tamanoCelda / 5);

            g.setColor(Color.BLUE);
            g.fillOval(
                    x + margen,
                    y + margen,
                    tamanoCelda - margen * 2,
                    tamanoCelda - margen * 2);
        }

        private void dibujarTexto(Graphics g) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 14));

            g.drawString(
                    "Laberinto autónomo: genera varios caminos y presiona Resolver automáticamente para que el jugador azul llegue solo a la salida.",
                    20,
                    25);

            int y = getHeight() - 30;

            dibujarLeyenda(g, 20, y, Color.GREEN, "Entrada");
            dibujarLeyenda(g, 130, y, Color.RED, "Salida");
            dibujarLeyendaCircular(g, 230, y, Color.BLUE, "Jugador");
            dibujarLeyenda(g, 340, y, Color.DARK_GRAY, "Pared");
            dibujarLeyenda(g, 430, y, Color.ORANGE, "Explorado A*");
            dibujarLeyenda(g, 570, y, Color.CYAN, "Solución final");
        }

        private void dibujarLeyenda(Graphics g, int x, int y, Color color, String texto) {
            g.setColor(color);
            g.fillRect(x, y - 12, 15, 15);
            g.setColor(Color.BLACK);
            g.drawRect(x, y - 12, 15, 15);
            g.drawString(texto, x + 25, y);
        }

        private void dibujarLeyendaCircular(Graphics g, int x, int y, Color color, String texto) {
            g.setColor(color);
            g.fillOval(x, y - 12, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString(texto, x + 25, y);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LaberintoSwing::new);
    }
}
