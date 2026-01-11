public class HealthIncident {
    private final int time;
    private final String disease;
    private final double latitude;
    private final double longitude;
    private final String location;
    private final double severity;
    private Position<HealthIncident> severityPosition; 

    public HealthIncident(int time, String disease, double latitude, double longitude, String location, double severity) {
        this.time = time;
        this.disease = disease;
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = location;
        this.severity = severity;
        this.severityPosition = null;
    }

    public int getTime() { 
        return time; 
    }

    public String getDisease() { 
        return disease;
    }

    public double getLatitude() {
         return latitude; 
        }

    public double getLongitude() {
         return longitude; 
        }

    public String getLocation() {
         return location; 
        }

    public double getSeverity() {
         return severity; 
        }

    public Position<HealthIncident> getSeverityPosition() {
         return severityPosition; 
        }
        
    public void setSeverityPosition(Position<HealthIncident> severityPosition) {
         this.severityPosition = severityPosition; 
        }
}