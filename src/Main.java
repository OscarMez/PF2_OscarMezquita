import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Simulador aeroport");

        int numAvions = demanarEnter(sc, "Nombre total d'avions (mínim 10): ", 10);
        int numPistes = demanarEnter(sc, "Nombre de pistes disponibles (mínim 2): ", 2);

        TorreControl torre = new TorreControl(numPistes);

        List<Avio> avions = new ArrayList<>();
        Random rand = new Random();
        for (int i = 1; i <= numAvions; i++) {
            boolean aterramentPrimer = rand.nextBoolean();
            String codi = generarCodi(i);
            Avio a = new Avio(codi, torre, aterramentPrimer);
            avions.add(a);
        }

        long inici = System.currentTimeMillis();

        // Iniciar tots els fils
        for (Avio a : avions) a.start();

        // Thread meteorologia (daemon): cada 8..14 s canvia condició i ajusta pistes utilitzables
        Thread meteorologia = new Thread(() -> {
            String[] conds = {"NORMAL", "BOIRA", "VENT", "TEMPESTA"};
            while (true) {
                try {
                    Thread.sleep((rand.nextInt(7) + 8) * 1000L); // 8..14 s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                String cond = conds[rand.nextInt(conds.length)];
                int reduccio;
                switch (cond) {
                    case "BOIRA": reduccio = 1; break;
                    case "VENT": reduccio = 1; break;
                    case "TEMPESTA": reduccio = 2; break;
                    default: reduccio = 0; break;
                }

                if (cond.equals("NORMAL")) {
                    torre.setPistesUtilitzables(torre.getPistesTotals());
                    System.out.println("[METEO] Condició NORMAL: totes les pistes disponibles.");
                } else {
                    int nou = Math.max(1, torre.getPistesTotals() - reduccio);
                    torre.setPistesUtilitzables(nou);
                    System.out.println("[METEO] Condició " + cond + ": reduint pistes a " + nou + " / " + torre.getPistesTotals());
                }
            }
        }, "Meteorologia");
        meteorologia.setDaemon(true);
        meteorologia.start();

        // Monitor que imprimeix l'estat cada segon amb emojis
        Thread monitor = new Thread(() -> {
            boolean totsFets = false;
            while (!totsFets) {
                System.out.println("--------------------------------------------------");
                System.out.println("Pistes ocupades: " + torre.getPistesOcupades() + " / " + torre.getPistesTotals()
                        + " | Utilitzables: " + torre.getPistesUtilitzables()
                        + " | Esperant aterrar: " + torre.getEsperantAterrament());
                for (Avio a : avions) {
                    String emoji = switch (a.getEstat()) {
                        case EN_VOL -> "🛫";
                        case ESPERANT -> "😴";
                        case ATERRANT -> "🛬";
                        case EN_TERMINAL -> "🏢";
                        case ENLAIRANT -> "🚀";
                    };
                    System.out.printf("%-8s %s %s%n", a.getCodi(), emoji, a.getEstat().name());
                }
                System.out.println("--------------------------------------------------");

                totsFets = true;
                for (Avio a : avions) {
                    if (!a.haCompletatCicle()) {
                        totsFets = false;
                        break;
                    }
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Monitor");
        monitor.start();

        // Esperar a que acabin tots els avions
        for (Avio a : avions) {
            try { a.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Esperar que el monitor acabi
        try { monitor.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        long fi = System.currentTimeMillis();
        double tempsSegons = (fi - inici) / 1000.0;

        // Resum final
        System.out.println();
        System.out.println("SIMULACIÓ FINALITZADA");
        System.out.println("Temps total: " + String.format("%.2f", tempsSegons) + " s");
        System.out.println("Total operacions (aterratges + enlairaments): " + torre.getTotalOperacions());
        System.out.println("Detall per avió:");
        for (Avio a : avions) {
            System.out.printf("%-8s - estat final: %s%n", a.getCodi(), a.getEstat().name());
        }

        System.out.println("Fi de la simulació. Adeu!");
    }

    private static String generarCodi(int i) {
        return String.format("A%03d", i);
    }

    private static int demanarEnter(Scanner sc, String missatge, int minim) {
        int valor = 0;
        while (true) {
            System.out.print(missatge);
            try {
                valor = Integer.parseInt(sc.nextLine().trim());
                if (valor < minim) {
                    System.out.println("El valor mínim és " + minim);
                } else {
                    return valor;
                }
            } catch (NumberFormatException e) {
                System.out.println("Introdueix un nombre vàlid.");
            }
        }
    }
}
