import java.util.concurrent.Semaphore;

public class TorreControl {

    private final Semaphore pistes;
    private final int pistesTotals;

    // Nombre de pistes que actualment es poden utilitzar (meteorologia pot reduir-ho)
    private volatile int pistesUtilitzables;

    private int avionsEsperantAterrament = 0;
    private final Object lockPrioritat = new Object();
    private int totalOperacions = 0;

    public TorreControl(int numPistes) {
        if (numPistes < 1) throw new IllegalArgumentException("Cal com a mínim 1 pista");
        this.pistesTotals = numPistes;
        this.pistes = new Semaphore(numPistes, true); // fair=true per intentar evitar fam
        this.pistesUtilitzables = numPistes; // per defecte totes les pistes són utilitzables
    }

    /**
     * Sol·licita pista per aterrament. Bloqueja en el semàfor però abans comprova
     * que la meteorologia permet utilitzar més pistes (pistesUtilitzables).
     */
    public void solicitarAterrament(Avio a) {
        synchronized (lockPrioritat) {
            avionsEsperantAterrament++;
        }

        // Espera activa curta si la meteorologia ha reduït el nombre de pistes utilitzables
        while (true) {
            // Si hi ha prou "espai utilitzable" intentem adquirir el semàfor
            if (getPistesOcupades() < getPistesUtilitzables()) {
                try {
                    pistes.acquire();
                    break; // adquirit
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // sortim si s'interromp
                    return;
                }
            } else {
                // no hi ha espai per ara segons meteorologia; esperem una mica i tornem a provar
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        synchronized (lockPrioritat) {
            avionsEsperantAterrament--;
            totalOperacions++;
        }
    }

    /**
     * Sol·licita pista per enlairament. Té prioritat inferior: espera fins que
     * no hi hagi avions esperant per aterrar i la meteorologia permet més pistes.
     */
    public void solicitarEnlairament(Avio a) {
        // Espera que no hi hagi avions esperant per aterrar (prioritat aterrament)
        while (true) {
            synchronized (lockPrioritat) {
                if (avionsEsperantAterrament == 0) {
                    // ara comprovar meteorologia / pistes utilitzables
                    if (getPistesOcupades() < getPistesUtilitzables()) {
                        try {
                            pistes.acquire();
                            break;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
            // si no pot entrar, dormir una mica i tornar a provar
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        synchronized (lockPrioritat) {
            totalOperacions++;
        }
    }

    /**
     * Notificar alliberament de pista quan l'avió acaba d'utilitzar-la.
     */
    public void notificarAlliberamentPista(Avio a) {
        pistes.release();
    }

    /**
     * Canviar el nombre de pistes utilitzables (per la meteorologia).
     * El valor ha d'estar entre 1 i pistesTotals.
     */
    public void setPistesUtilitzables(int nouValor) {
        if (nouValor < 1) nouValor = 1;
        if (nouValor > pistesTotals) nouValor = pistesTotals;
        this.pistesUtilitzables = nouValor;
        System.out.println("[TORRE] Meteorologia: pistes utilitzables = " + pistesUtilitzables + " / " + pistesTotals);
    }

    // --- Getters per monitorització ---
    public int getPistesTotals() {
        return pistesTotals;
    }

    public int getPistesUtilitzables() {
        return pistesUtilitzables;
    }

    public int getPistesOcupades() {
        return pistesTotals - pistes.availablePermits();
    }

    public int getEsperantAterrament() {
        synchronized (lockPrioritat) {
            return avionsEsperantAterrament;
        }
    }

    public int getTotalOperacions() {
        synchronized (lockPrioritat) {
            return totalOperacions;
        }
    }
}
