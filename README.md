# 🏢 Workmate - Employee Attendance Management System

Workmate is a comprehensive Android application built with Kotlin, designed to streamline employee attendance tracking for organizations. The app features two distinct interfaces: one for **Admins** to manage employees and monitor attendance, and another for **Employees** to clock in/out and view their records.

---

## ✨ Features

### For Admins
* **📈 Dashboard:** A real-time dashboard showing today's attendance statistics (Present, Absent, Late, Half-Day) with a dynamic pie chart.
* **👥 Employee Management:** Seamlessly add, view, and update employee details.
* **📧 Automated Welcome Emails:** Automatically sends a welcome email with credentials to newly added employees using the SendGrid API.
* **🔍 Search & Filter:** Easily search for specific employees and manage their status (active/inactive).
* **📊 Reports:** View a list of all employees to access individual attendance reports.

### For Employees
* **⏰ Clock In/Out:** Simple one-tap clock-in and clock-out functionality with a live work duration timer.
* **⏸️ Break Tracking:** Functionality to start and resume breaks, which are accurately deducted from the total work time.
* **📖 Attendance History:** A detailed, scrollable log of all past attendance records, including status and work duration.
* **👤 Account Management:** A dedicated screen to view personal profile details.
* **🔒 Secure Login:** Authentication powered by Firebase for secure access.

---

## 🏛️ Architecture

This application is built following the modern **MVVM (Model-View-ViewModel)** architecture, ensuring a clean separation of concerns and a scalable codebase.

* **ViewModel:** Manages and stores UI-related data, surviving configuration changes.
* **LiveData:** Used to observe data changes and update the UI reactively.
* **Repository Pattern (implied):** The ViewModels handle the logic for fetching data from Firestore, acting as a single source of truth for the UI.

---

## 🛠️ Tech Stack & Libraries

* **Language:** [Kotlin](https://kotlinlang.org/)
* **Backend & Database:** [Firebase](https://firebase.google.com/) (Firestore, Authentication)
* **Architecture:** Android Architecture Components (ViewModel, LiveData)
* **Asynchronous Programming:** Coroutines
* **UI:** XML with Material Design Components
* **Charting:** [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
* **API Integration:** [OkHttp](https://square.github.io/okhttp/) for making API calls to SendGrid.

---

## 🚀 Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

* Android Studio (latest version recommended)
* A Google account to create a Firebase project.
* A SendGrid account to get an API key for sending emails.

### Setup Steps

1.  **Clone the repository:**
    ```sh
    git clone [https://github.com/Nishantptl/WorkMate.git]
    ```

2.  **Open in Android Studio:**
    * Open Android Studio and select `Open an existing project`.
    * Navigate to the cloned repository folder.

3.  **Connect to Firebase:**
    * Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
    * Add a new Android app to the project with the package name `com.example.workmate`.
    * Download the `google-services.json` file and place it in the `app/` directory of your project.
    * In the Firebase console, enable **Authentication (Email/Password)** and **Firestore Database**.

4.  **API Key Configuration (Important!)**
    This project uses a SendGrid API key to send emails. To keep it secure, it is **not** stored in the source code.

    * In the root directory of the project, create a new file named `gradle.properties`.
    * Add your SendGrid API key to this file like so:
        ```properties
        API_KEY="YOUR_SUPER_SECRET_SENDGRID_API_KEY"
        ```
    * The project's `.gitignore` file is already configured to ignore `gradle.properties`, ensuring your key will not be committed to Git.

5.  **Build and Run:**
    * Sync the project with Gradle files.
    * Build and run the app on an emulator or a physical device.

---

