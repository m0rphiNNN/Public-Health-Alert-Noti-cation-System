import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

public class HealthAlertNotification {

    private final LinkedPositionalList<Watcher> watcherList;
    private final LinkedQueue<HealthIncident> incidentQueue;
    private final LinkedPositionalList<HealthIncident> severityOrderedList;

    public HealthAlertNotification() {
        this.watcherList = new LinkedPositionalList<>();
        this.incidentQueue = new LinkedQueue<>();
        this.severityOrderedList = new LinkedPositionalList<>();
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java HealthAlertNotification [--all] <watcherFile> <healthFile>");
            return;
        }

        boolean showAll = false;
        String watcherFileName;
        String healthFileName;

        if (args.length == 3) {
            if (!args[0].equalsIgnoreCase("--all")) {
                System.out.println("Invalid flag. Use --all or omit.");
                return;
            }
            showAll = true;
            watcherFileName = args[1];
            healthFileName = args[2];
        } else {
            watcherFileName = args[0];
            healthFileName = args[1];
        }

        HealthAlertNotification simulation = new HealthAlertNotification();
        try {
            simulation.runSimulation(watcherFileName, healthFileName, showAll);
        } catch (FileNotFoundException e) {
            System.err.println("Error: Input file not found. " + e.getMessage());
        }
    }

    /**
     * DÜZELTME: Simülasyon döngüsü, olay sırasını garanti altına almak için
     * basit ve güvenilir "currentTime++" modeline geri döndürüldü.
     */
    public void runSimulation(String watcherFileName, String healthFileName, boolean showAll) throws FileNotFoundException {
        Scanner watcherScanner = new Scanner(new File(watcherFileName));
        Scanner healthScanner = new Scanner(new File(healthFileName));

        String nextWatcherLine = watcherScanner.hasNextLine() ? watcherScanner.nextLine() : null;
        String nextHealthLine = healthScanner.hasNextLine() ? healthScanner.nextLine() : null;

        int currentTime = 0;

        while (nextWatcherLine != null || nextHealthLine != null || !incidentQueue.isEmpty()) {

            int nextWatcherTime = (nextWatcherLine != null) ? Integer.parseInt(nextWatcherLine.split(" ")[0]) : Integer.MAX_VALUE;
            int nextHealthTime = (nextHealthLine != null) ? Integer.parseInt(nextHealthLine.split(" ")[0]) : Integer.MAX_VALUE;

            // Önce kullanıcı olaylarını işle (öncelik kuralı)
            while (nextWatcherTime == currentTime) {
                processWatcherEvent(nextWatcherLine);
                nextWatcherLine = watcherScanner.hasNextLine() ? watcherScanner.nextLine() : null;
                nextWatcherTime = (nextWatcherLine != null) ? Integer.parseInt(nextWatcherLine.split(" ")[0]) : Integer.MAX_VALUE;
            }

            // Sonra sağlık olaylarını işle
            while (nextHealthTime == currentTime) {
                processHealthEvent(nextHealthLine, showAll);
                nextHealthLine = healthScanner.hasNextLine() ? healthScanner.nextLine() : null;
                nextHealthTime = (nextHealthLine != null) ? Integer.parseInt(nextHealthLine.split(" ")[0]) : Integer.MAX_VALUE;
            }

            // 6 saatten eski olayları temizle
            purgeOldIncidents(currentTime);

            // Eğer işlenecek olay kalmadıysa ve kuyruk boşsa döngüyü sonlandır
            if (nextWatcherTime == Integer.MAX_VALUE && nextHealthTime == Integer.MAX_VALUE && incidentQueue.isEmpty()) {
                break;
            }
            
            currentTime++; // Zamanı bir saat ilerlet
        }

        watcherScanner.close();
        healthScanner.close();
    }

    private void processWatcherEvent(String line) {
        String[] parts = line.split(" ");
        String command = parts[1];

        switch (command) {
            case "add": {
                double lat = Double.parseDouble(parts[2]);
                double lon = Double.parseDouble(parts[3]);
                String name = parts[4];
                watcherList.addLast(new Watcher(name, lat, lon));
                System.out.println(name + " is added to the watcher-list");
                break;
            }
            case "delete": {
                String name = parts[2];
                Position<Watcher> toRemove = null;
                for (Position<Watcher> pos : watcherList.positions()) {
                    if (pos.getElement().getName().equals(name)) {
                        toRemove = pos;
                        break;
                    }
                }
                if (toRemove != null) {
                    watcherList.remove(toRemove);
                    System.out.println(name + " is removed from the watcher-list");
                }
                break;
            }
            case "query-highest":
                System.out.println("Most severe health incident in past 6 hours:");
                if (severityOrderedList.isEmpty()) {
                    System.out.println("No records");
                } else {
                    HealthIncident highest = severityOrderedList.first().getElement();
                    System.out.printf(Locale.US, "(Disease: %s) Severity: %.1f at %s\n",
                            highest.getDisease(), highest.getSeverity(), highest.getLocation());
                }
                break;
            case "query-disease":
            case "query-region":
                processQueueQuery(command, parts);
                break;
        }
    }
    
    /**
     * DÜZELTME: "No records" mantığı ve "found" bayrağı tamamen kaldırıldı.
     * Bu sorgular sonuç bulamazsa hiçbir şey yazdırmamalıdır.
     */
    private void processQueueQuery(String queryType, String[] params) {
        LinkedPositionalList<HealthIncident> tempList = new LinkedPositionalList<>();

        boolean found = false;

        while (!incidentQueue.isEmpty()) {
            HealthIncident incident = incidentQueue.dequeue();
            tempList.addLast(incident);

            if (queryType.equals("query-disease")) {
                String diseaseToFind = params[2];
                if (incident.getDisease().equals(diseaseToFind)) {
                    System.out.printf(Locale.US, "(Disease: %s) Severity: %.1f at %s\n",
                            incident.getDisease(), incident.getSeverity(), incident.getLocation());
                    found = true;
                }
            } else if (queryType.equals("query-region")) {
                double qlat = Double.parseDouble(params[2]);
                double qlon = Double.parseDouble(params[3]);
                double radius = Double.parseDouble(params[4]);
                double distance = calculateDistance(qlat, qlon, incident.getLatitude(), incident.getLongitude());
                if (distance < radius) {
                    System.out.printf(Locale.US, "(Disease: %s) Severity: %.1f at %s\n",
                            incident.getDisease(), incident.getSeverity(), incident.getLocation());
                    found = true;
                }
            }
        }

        for (HealthIncident incident : tempList) {
            incidentQueue.enqueue(incident);
        }

        // Eğer sorgu için hiçbir kayıt bulunmadıysa "No records" yaz
        if (!found) {
            System.out.println("No records");
        }
    }

    private void processHealthEvent(String line, boolean showAll) {
        String[] parts = line.split(" ");
        int time = Integer.parseInt(parts[0]);
        String disease = parts[1];
        double lat = Double.parseDouble(parts[2]);
        double lon = Double.parseDouble(parts[3]);
        String location = parts[4];
        double severity = Double.parseDouble(parts[7]);

        HealthIncident incident = new HealthIncident(time, disease, lat, lon, location, severity);
        addIncidentToStructures(incident);

        if (showAll) {
            System.out.printf("(Disease: %s) at %s is inserted into incident-queue\n",
                incident.getDisease(), incident.getLocation());
        }
        checkForNotifications(incident);
    }

    private void addIncidentToStructures(HealthIncident incident) {
        incidentQueue.enqueue(incident);
        Position<HealthIncident> current = severityOrderedList.first();
        while (current != null && current.getElement().getSeverity() > incident.getSeverity()) {
            current = severityOrderedList.after(current);
        }
        Position<HealthIncident> newPosition;
        if (current == null) {
            newPosition = severityOrderedList.addLast(incident);
        } else {
            newPosition = severityOrderedList.addBefore(current, incident);
        }
        incident.setSeverityPosition(newPosition);
    }

    private void checkForNotifications(HealthIncident incident) {
        for (Watcher watcher : watcherList) {
            double distance = calculateDistance(watcher.getLatitude(), watcher.getLongitude(),
                                                incident.getLatitude(), incident.getLongitude());
            if (distance < 2 * incident.getSeverity()) {
                 System.out.printf("(Disease: %s) at %s is close to %s\n",
                    incident.getDisease(), incident.getLocation(), watcher.getName());
            }
        }
    }

    private void purgeOldIncidents(int currentTime) {
        int expiryTime = currentTime - 6;
        while (!incidentQueue.isEmpty() && incidentQueue.first().getTime() < expiryTime) {
            HealthIncident toRemove = incidentQueue.dequeue();
            if (toRemove.getSeverityPosition() != null) {
                severityOrderedList.remove(toRemove.getSeverityPosition());
            }
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2));
    }
}