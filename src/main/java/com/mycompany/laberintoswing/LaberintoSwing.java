package com.mycompany.laberintoswing;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

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

    private Stack<PasoDFS> pilaExploracion;
    private boolean[][] visitado;
    private List<Celda> caminoActual;
    private List<Celda> caminoFinal;
    private boolean solucionEncontrada;

    public LaberintoSwing() {
        setTitle("Laberinto Autónomo - Exploración DFS con Backtracking");
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
        botonResolver = new JButton("Explorar automáticamente");
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

        botonResolver.addActionListener(e -> iniciarExploracionAutomatica());

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
         * ALGORITMO DE CREACIÓN:
         * Prim aleatorizado.
         * El laberinto se interpreta como un grafo:
         * celdas = nodos, conexiones abiertas = aristas.
         */
        generarConPrimAleatorizado(laberinto);

        /*
         * ALGORITMO EXTRA:
         * Se abren paredes internas para crear ciclos y caminos alternativos.
         * Se evita abrir paredes que formen espacios grandes 2x2.
         */
        crearCaminosAlternativos(laberinto, dificultad);

        /*
         * BFS se usa para colocar una entrada y una salida alejadas.
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
        int maxIntentos = cantidadObjetivo * 60;

        while (abiertos < cantidadObjetivo && intentos < maxIntentos) {
            intentos++;

            int fila = 1 + random.nextInt(laberinto.length - 2);
            int columna = 1 + random.nextInt(laberinto[0].length - 2);

            if (laberinto[fila][columna] != PARED) {
                continue;
            }

            int vecinosCamino = contarVecinosCamino(laberinto, fila, columna);

            /*
             * Solo se abre si:
             * 1. La pared conecta caminos.
             * 2. No crea espacios 2x2.
             */
            if (vecinosCamino >= 2 && puedeAbrirSinCrearBloque2x2(laberinto, fila, columna)) {
                laberinto[fila][columna] = CAMINO;
                abiertos++;
            }
        }

        reducirCallejonesSinSalida(laberinto, dificultad);
    }

    /*
     * Este método evita que se formen huecos grandes de 2x2.
     * Simula abrir la pared y revisa si alrededor se formaría
     * un cuadrado de 4 caminos juntos.
     */
    private boolean puedeAbrirSinCrearBloque2x2(int[][] laberinto, int fila, int columna) {
        if (laberinto[fila][columna] != PARED) {
            return false;
        }

        for (int df = -1; df <= 0; df++) {
            for (int dc = -1; dc <= 0; dc++) {
                int f = fila + df;
                int c = columna + dc;

                if (f < 0 || f + 1 >= laberinto.length
                        || c < 0 || c + 1 >= laberinto[0].length) {
                    continue;
                }

                int caminos = 0;

                for (int i = f; i <= f + 1; i++) {
                    for (int j = c; j <= c + 1; j++) {
                        if (i == fila && j == columna) {
                            caminos++;
                        } else if (laberinto[i][j] != PARED) {
                            caminos++;
                        }
                    }
                }

                if (caminos == 4) {
                    return false;
                }
            }
        }

        return true;
    }

    private void reducirCallejonesSinSalida(int[][] laberinto, int dificultad) {
        int porcentaje;

        switch (dificultad) {
            case 1:
                porcentaje = 10;
                break;
            case 2:
                porcentaje = 20;
                break;
            case 3:
                porcentaje = 30;
                break;
            default:
                porcentaje = 15;
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
                    && laberinto[nuevaFila][nuevaColumna] == PARED
                    && puedeAbrirSinCrearBloque2x2(laberinto, nuevaFila, nuevaColumna)) {
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
        boolean[][] visitadoBFS = new boolean[laberinto.length][laberinto[0].length];
        int[][] distancia = new int[laberinto.length][laberinto[0].length];

        Queue<Celda> cola = new LinkedList<>();
        cola.add(inicio);
        visitadoBFS[inicio.fila][inicio.columna] = true;

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
                        && !visitadoBFS[nuevaFila][nuevaColumna]) {

                    visitadoBFS[nuevaFila][nuevaColumna] = true;
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

    /*
     * RESOLUCIÓN AUTÓNOMA EXPLORANDO:
     * El autómata NO resuelve instantáneamente.
     * Usa DFS con Backtracking.
     * El jugador azul prueba caminos, marca lo explorado,
     * retrocede si llega a un callejón sin salida
     * y finalmente muestra la ruta correcta.
     */
    private void iniciarExploracionAutomatica() {
        detenerTimer();
        limpiarRecorridoAnterior();

        visitado = new boolean[laberinto.length][laberinto[0].length];
        pilaExploracion = new Stack<>();
        caminoActual = new ArrayList<>();
        caminoFinal = new ArrayList<>();
        solucionEncontrada = false;

        jugador = new Celda(entrada.fila, entrada.columna);
        visitado[entrada.fila][entrada.columna] = true;
        caminoActual.add(new Celda(entrada.fila, entrada.columna));
        pilaExploracion.push(new PasoDFS(
                new Celda(entrada.fila, entrada.columna),
                obtenerDireccionesAleatorias()));

        timer = new javax.swing.Timer(90, e -> avanzarExploracionDFS());
        timer.start();
    }

    private void avanzarExploracionDFS() {
        if (solucionEncontrada) {
            animarCaminoFinal();
            return;
        }

        if (pilaExploracion.isEmpty()) {
            detenerTimer();
            JOptionPane.showMessageDialog(
                    this,
                    "El autómata exploró el laberinto, pero no encontró salida.",
                    "Sin solución",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        PasoDFS pasoActual = pilaExploracion.peek();
        Celda actual = pasoActual.celda;

        jugador.fila = actual.fila;
        jugador.columna = actual.columna;

        if (mismaCelda(actual, salida)) {
            solucionEncontrada = true;
            caminoFinal = new ArrayList<>(caminoActual);
            limpiarSoloExploradoParaMostrarSolucion();
            panelLaberinto.repaint();
            return;
        }

        Celda siguiente = obtenerSiguienteVecinoNoVisitado(pasoActual);

        if (siguiente != null) {
            visitado[siguiente.fila][siguiente.columna] = true;

            if (!mismaCelda(siguiente, entrada) && !mismaCelda(siguiente, salida)) {
                laberinto[siguiente.fila][siguiente.columna] = EXPLORADO;
            }

            jugador.fila = siguiente.fila;
            jugador.columna = siguiente.columna;

            caminoActual.add(new Celda(siguiente.fila, siguiente.columna));
            pilaExploracion.push(new PasoDFS(
                    new Celda(siguiente.fila, siguiente.columna),
                    obtenerDireccionesAleatorias()));
        } else {
            /*
             * Backtracking:
             * No hay vecinos disponibles.
             * El autómata retrocede al punto anterior.
             */
            Celda retroceso = pilaExploracion.pop().celda;

            if (!mismaCelda(retroceso, entrada) && !mismaCelda(retroceso, salida)) {
                laberinto[retroceso.fila][retroceso.columna] = EXPLORADO;
            }

            if (!caminoActual.isEmpty()) {
                caminoActual.remove(caminoActual.size() - 1);
            }

            if (!pilaExploracion.isEmpty()) {
                Celda anterior = pilaExploracion.peek().celda;
                jugador.fila = anterior.fila;
                jugador.columna = anterior.columna;
            }
        }

        laberinto[entrada.fila][entrada.columna] = ENTRADA;
        laberinto[salida.fila][salida.columna] = SALIDA;
        panelLaberinto.repaint();
    }

    private Celda obtenerSiguienteVecinoNoVisitado(PasoDFS pasoActual) {
        while (pasoActual.indiceDireccion < pasoActual.direcciones.size()) {
            int[] direccion = pasoActual.direcciones.get(pasoActual.indiceDireccion);
            pasoActual.indiceDireccion++;

            int nuevaFila = pasoActual.celda.fila + direccion[0];
            int nuevaColumna = pasoActual.celda.columna + direccion[1];

            if (esPosicionValidaParaCaminar(laberinto, nuevaFila, nuevaColumna)
                    && !visitado[nuevaFila][nuevaColumna]) {
                return new Celda(nuevaFila, nuevaColumna);
            }
        }

        return null;
    }

    private List<int[]> obtenerDireccionesAleatorias() {
        List<int[]> direcciones = new ArrayList<>();
        direcciones.add(new int[] { -1, 0 });
        direcciones.add(new int[] { 1, 0 });
        direcciones.add(new int[] { 0, -1 });
        direcciones.add(new int[] { 0, 1 });

        Collections.shuffle(direcciones, random);

        return direcciones;
    }

    private void limpiarSoloExploradoParaMostrarSolucion() {
        for (int fila = 0; fila < laberinto.length; fila++) {
            for (int columna = 0; columna < laberinto[0].length; columna++) {
                if (laberinto[fila][columna] == EXPLORADO) {
                    laberinto[fila][columna] = CAMINO;
                }
            }
        }

        laberinto[entrada.fila][entrada.columna] = ENTRADA;
        laberinto[salida.fila][salida.columna] = SALIDA;
    }

    private void animarCaminoFinal() {
        if (caminoFinal.isEmpty()) {
            detenerTimer();
            JOptionPane.showMessageDialog(
                    this,
                    "El autómata exploró y encontró la salida.",
                    "Solución completada",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Celda paso = caminoFinal.remove(0);

        jugador.fila = paso.fila;
        jugador.columna = paso.columna;

        if (!mismaCelda(paso, entrada) && !mismaCelda(paso, salida)) {
            laberinto[paso.fila][paso.columna] = SOLUCION;
        }

        laberinto[entrada.fila][entrada.columna] = ENTRADA;
        laberinto[salida.fila][salida.columna] = SALIDA;
        panelLaberinto.repaint();
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

    static class PasoDFS {

        Celda celda;
        List<int[]> direcciones;
        int indiceDireccion;

        PasoDFS(Celda celda, List<int[]> direcciones) {
            this.celda = celda;
            this.direcciones = direcciones;
            this.indiceDireccion = 0;
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
                    "Autómata explorador: prueba caminos, marca lo explorado, retrocede en callejones y luego muestra la ruta final.",
                    20,
                    25);

            int y = getHeight() - 30;

            dibujarLeyenda(g, 20, y, Color.GREEN, "Entrada");
            dibujarLeyenda(g, 130, y, Color.RED, "Salida");
            dibujarLeyendaCircular(g, 230, y, Color.BLUE, "Autómata");
            dibujarLeyenda(g, 340, y, Color.DARK_GRAY, "Pared");
            dibujarLeyenda(g, 430, y, Color.ORANGE, "Explorado");
            dibujarLeyenda(g, 550, y, Color.CYAN, "Ruta final");
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