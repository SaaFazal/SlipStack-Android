# SlipStack

![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=for-the-badge&logo=android)
![Machine Learning](https://img.shields.io/badge/Google_ML_Kit-OCR-4285F4?style=for-the-badge&logo=google)
![SQLite](https://img.shields.io/badge/SQLite-Local_DB-003B57?style=for-the-badge&logo=sqlite)

SlipStack is a full-stack Android mobile application that utilizes advanced Optical Character Recognition (OCR) and Regex pattern matching to seamlessly digitize and track physical receipts. 

## 🚀 Key Features

*   **On-Device OCR Scanning:** Fast, private, and secure receipt scanning utilizing Google ML Kit. All image processing happens locally on the device.
*   **Offline-First Architecture:** Engineered to work flawlessly without an internet connection, featuring intelligent background sync for when connectivity is restored.
*   **Dynamic Data Extraction:** Automated extraction of line items, prices, dates, and vendor names using highly robust and tested Regex patterns.
*   **Automated Categorization:** Smart grouping and analysis of extracted purchase data into standard expense categories.
*   **Interactive Data Visualization:** Visualizes monthly spending habits and category breakdowns with interactive charts using the MPAndroidChart library.

## 🛠️ Technology Stack

*   **Platform:** Android (Native Java)
*   **Machine Learning:** Google ML Kit (Text Recognition API)
*   **Local Database:** Room Persistence Library / SQLite
*   **Visualization:** MPAndroidChart
*   **UI/UX:** Material Design Components, XML Layouts

## ⚙️ Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/SaaFazal/SlipStack-Android.git
   ```
2. **Open in Android Studio:**
   Launch Android Studio and select `Open an existing project`. Navigate to the cloned `SlipStack-Android` folder.
3. **Sync Gradle:**
   Allow Android Studio to sync the Gradle files and download all necessary dependencies (ML Kit, Room, MPAndroidChart).
4. **Build and Run:**
   Connect an Android physical device or start an emulator, then click the `Run` button in Android Studio.

## 📸 Permissions Required
The application requires the following permissions to function correctly:
*   `CAMERA`: For capturing photos of physical receipts.
*   `READ_EXTERNAL_STORAGE`: For importing existing receipt images from the gallery.
