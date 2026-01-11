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

        boolean flagAll = false;
        String watcherFileName;
        String healthFileName;

        if (args.length == 3) {
            flagAll = true;
            watcherFileName = args[1];
            healthFileName = args[2];

        } else {

            watcherFileName = args[0];
            healthFileName = args[1];
        }

        HealthAlertNotification simulation = new HealthAlertNotification();
        try {
            simulation.runSimulation(watcherFileName, healthFileName, flagAll);
        } catch (FileNotFoundException e) {
            System.err.println("Input file not found. " + e.getMessage());
        }
    }

    public void runSimulation(String watcherFileName, String healthFileName, boolean showAll) throws FileNotFoundException {
        Scanner watcherScanner = new Scanner(new File(watcherFileName));
        Scanner healthScanner = new Scanner(new File(healthFileName));

        String nextWatcherLine = watcherScanner.hasNextLine() ? watcherScanner.nextLine() : null;
        String nextHealthLine = healthScanner.hasNextLine() ? healthScanner.nextLine() : null;

        int currentTime = 0;
        while (nextWatcherLine != null || nextHealthLine != null || !incidentQueue.isEmpty()) {
            int nextWatcherTime = nextWatcherLine != null ? Integer.parseInt(nextWatcherLine.split(" ")[0]) : -1;
            int nextHealthTime = nextHealthLine != null ? Integer.parseInt(nextHealthLine.split(" ")[0]) : -1;

            while (nextWatcherTime == currentTime) {
                WatcherEvent(nextWatcherLine);
                nextWatcherLine = watcherScanner.hasNextLine() ? watcherScanner.nextLine() : null;
                nextWatcherTime = nextWatcherLine != null ? Integer.parseInt(nextWatcherLine.split(" ")[0]) : -1;
            }

            while (nextHealthTime == currentTime) {
                HealthEvent(nextHealthLine, showAll);
                nextHealthLine = healthScanner.hasNextLine() ? healthScanner.nextLine() : null;
                nextHealthTime = (nextHealthLine != null) ? Integer.parseInt(nextHealthLine.split(" ")[0]) : Integer.MAX_VALUE;
            }

            deleteOldIncidents(currentTime);

            if (nextWatcherTime == -1 && nextHealthTime == -1 && incidentQueue.isEmpty()) {
                break;
            }

            currentTime++;
        }

        watcherScanner.close();
        healthScanner.close();
    }

    private void WatcherEvent(String line) {
        String[] words = line.split(" ");
        String command = words[1];

        if(command.equals("add")) {
            String name = words[4];
                double lon = Double.parseDouble(words[2]);
                double lat = Double.parseDouble(words[3]);
                watcherList.addLast(new Watcher(name, lat, lon));
                System.out.println(name + " is added to the watcher-list");
        }

        else if(command.equals("delete")) {
            String name = words[2];
                Position<Watcher> watcher = null;
                for (Position<Watcher> walk : watcherList.positions()) {
                    if (walk.getElement().getName().equals(name)) {
                        watcher = walk;
                        break;
                    }
                }
                if (watcher != null) {
                    watcherList.remove(watcher);
                    System.out.println(name + " is removed from the watcher-list");
                }
        }

        else if(command.equals("query-highest")) {
            System.out.println("Most severe health incident in past 6 hours:");
                if (severityOrderedList.isEmpty()) {
                    System.out.println("No records");
                } else {
                    HealthIncident highest = severityOrderedList.first().getElement();
                    System.out.printf(Locale.US, "(Disease: %s) Severity: %.1f at %s\n",highest.getDisease(), highest.getSeverity(), highest.getLocation());
                }
        }

        else if(command.equals("query-disease")) {
            searchQueue(command, words);
        }
        else if(command.equals("query-region")) {
            searchQueue(command, words);
        }

    }

    private void searchQueue(String queryType, String[] params) {
        LinkedPositionalList<HealthIncident> tempList = new LinkedPositionalList<>();
        boolean found = false;

        while (!incidentQueue.isEmpty()) {
            HealthIncident incident = incidentQueue.dequeue();
            tempList.addLast(incident);

            if (queryType.equals("query-disease")) {
                String diseaseToFind = params[2];
                if (incident.getDisease().equals(diseaseToFind)) {
                    System.out.printf(Locale.US, "(Disease: %s) Severity: %.1f at %s\n",incident.getDisease(), incident.getSeverity(), incident.getLocation());
                    found = true;
                }

            } 
            else if (queryType.equals("query-region")) {
                double lon = Double.parseDouble(params[2]);
                double lat = Double.parseDouble(params[3]);
                double radius = Double.parseDouble(params[4]);
                double distance = calculateDistance(lat, lon, incident.getLatitude(), incident.getLongitude());
                if (distance < radius) {
                    System.out.printf(Locale.US, "(Disease: %s) Severity: %.1f at %s\n",incident.getDisease(), incident.getSeverity(), incident.getLocation());
                    found = true;
                }
            }
        }

        for (HealthIncident incident : tempList) {
            incidentQueue.enqueue(incident);
        }

        if (!found) {
            System.out.println("No records");
        }
    }

    private void HealthEvent(String line, boolean showAll) {
        String[] words = line.split(" ");
        int time = Integer.parseInt(words[0]);
        String disease = words[1];
        double lon = Double.parseDouble(words[2]);
        double lat = Double.parseDouble(words[3]);
        String location = words[4];
        double severity = Double.parseDouble(words[7]);

        HealthIncident incident = new HealthIncident(time, disease, lat, lon, location, severity);
        insertIncident(incident);

        if (showAll) {
            System.out.printf("(Disease: %s) at %s is inserted into incident-queue\n",incident.getDisease(), incident.getLocation());
        }
        checkNotifications(incident);
    }

    private void insertIncident(HealthIncident incident) {
        incidentQueue.enqueue(incident);
        Position<HealthIncident> curr = severityOrderedList.first();
        while (curr != null && curr.getElement().getSeverity() > incident.getSeverity()) {
            curr = severityOrderedList.after(curr);
        }
        Position<HealthIncident> newPosition;
        if (curr == null) {
            newPosition = severityOrderedList.addLast(incident);
        } else {
            newPosition = severityOrderedList.addBefore(curr, incident);
        }
        incident.setSeverityPosition(newPosition);
    }

    private void checkNotifications(HealthIncident incident) {
        for (Watcher watcher : watcherList) {
            double distance = calculateDistance(watcher.getLatitude(), watcher.getLongitude(),incident.getLatitude(), incident.getLongitude());
            if (distance < 2 * incident.getSeverity()) {
                System.out.printf("(Disease: %s) at %s is close to %s\n",incident.getDisease(), incident.getLocation(), watcher.getName());
            }
        }
    }

    private void deleteOldIncidents(int currentTime) {
        int expiryTime = currentTime -6;
        while (!incidentQueue.isEmpty() && incidentQueue.first().getTime() <= expiryTime) {    
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