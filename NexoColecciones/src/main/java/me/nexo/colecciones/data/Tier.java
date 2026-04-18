package me.nexo.colecciones.data;

import java.util.List;

public class Tier {
    private final int nivel;
    private final long requerido;
    private final List<String> recompensas;
    private final List<String> loreRecompensa;

    public Tier(int nivel, long requerido, List<String> recompensas, List<String> loreRecompensa) {
        this.nivel = nivel;
        this.requerido = requerido;
        this.recompensas = recompensas;
        this.loreRecompensa = loreRecompensa;
    }

    public int getNivel() { return nivel; }
    public long getRequerido() { return requerido; }
    public List<String> getRecompensas() { return recompensas; }
    public List<String> getLoreRecompensa() { return loreRecompensa; }
}