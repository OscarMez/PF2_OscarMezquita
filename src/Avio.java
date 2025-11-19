import java.util.Random;

public class Avio extends Thread {

    public enum Estat {
        EN_VOL,
        ESPERANT,
        ATERRANT,
        EN_TERMINAL,
        ENLAIRANT
    }

    private final String codi;
    private Estat estat;
    private final TorreControl torre;
    private final boolean primerAterrament;
    private boolean fetAterrament = false;
    private boolean fetEnlairament = false;
    private Random rand = new Random();

    public Avio(String codi, TorreControl torre, boolean primerAterrament) {
        this.codi = codi;
        this.torre = torre;
        this.primerAterrament = primerAterrament;
        this.estat = primerAterrament ? Estat.EN_VOL : Estat.EN_TERMINAL;
    }

    public String getCodi() { return codi; }
    public Estat getEstat() { return estat; }

    private void dormir(int min, int max) {
        try {
            Thread.sleep((rand.nextInt(max - min + 1) + min) * 1000L);
        } catch (InterruptedException e) {}
    }

    @Override
    public void run() {
        if (primerAterrament) {
            // Aterrar primer
            estat = Estat.ESPERANT;
            torre.solicitarAterrament(this);
            estat = Estat.ATERRANT;
            dormir(2,5);
            torre.notificarAlliberamentPista(this);
            estat = Estat.EN_TERMINAL;
            fetAterrament = true;

            dormir(1,4);

            // Enlairar-se després
            estat = Estat.ESPERANT;
            torre.solicitarEnlairament(this);
            estat = Estat.ENLAIRANT;
            dormir(2,5);
            torre.notificarAlliberamentPista(this);
            estat = Estat.EN_VOL;
            fetEnlairament = true;

        } else {
            // Enlairar-se primer
            estat = Estat.ESPERANT;
            torre.solicitarEnlairament(this);
            estat = Estat.ENLAIRANT;
            dormir(2,5);
            torre.notificarAlliberamentPista(this);
            estat = Estat.EN_VOL;
            fetEnlairament = true;

            dormir(1,4);

            // Aterrar després
            estat = Estat.ESPERANT;
            torre.solicitarAterrament(this);
            estat = Estat.ATERRANT;
            dormir(2,5);
            torre.notificarAlliberamentPista(this);
            estat = Estat.EN_TERMINAL;
            fetAterrament = true;
        }
    }

    public boolean haCompletatCicle() {
        return fetAterrament && fetEnlairament;
    }

    @Override
    public String toString() {
        return "[" + codi + "] " + estat;
    }
}
