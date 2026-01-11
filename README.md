# 🔗 HealthAlert Notification System

> **Developed by Bekir Ata Yıldırım** 

This project implements a **Public Health Alert Notification System** designed to simulate real-time tracking of disease outbreaks, inspired by global organizations like WHO and CDC.

The system processes a stream of timestamped health incidents and user requests in a **discrete-time event simulation**, creating an automated early-warning mechanism for public health.

---

## 🛠️ How It Works

The simulation runs in chronological order, processing events in 1-hour time slices. It manages two main streams of data:

* **User Management (Watchers):** Users can register (add) or unregister (delete) from the system to receive alerts. They can also query the system for specific disease data or regional statistics.
* **Health Incidents:** Disease outbreaks are processed as they occur. The system maintains a "active window" of the past **6 hours**. Any incident older than 6 hours is automatically expired and removed from the system.
* **Proximity Alerts:** When a new incident occurs, the system calculates the Euclidean distance between the incident and all registered watchers. If a watcher is close enough (`Distance < 2 × Severity`), they receive an immediate notification.

---

## 🚀 How to Run

It uses standard Java, so no external libraries are required.

**How to compile and run it yourself:**

### 1. Download the code:
```bash
git clone https://github.com/BekirAtaYildirim/PublicHealthAlertNotificationSystem.git
```

### 2. Compile: Navigate to the source directory and compile:
```bash
javac *.java
```

### 3. Run: You can run the program with:
```bash
java HealthAlert Notification [--all] <watcherFile> <healthFile>
```
