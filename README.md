# Flight Tracker: Real-Time Flight Insights & Data Analytics

Welcome to **Flight Tracker**, a powerful Android application designed for real-time flight tracking, top flights data analytics, and historical flight records. Built with modern Android technologies like **Jetpack Compose**, **Retrofit**, **Room**, and **WorkManager**, this app delivers a seamless and reactive user experience with a sleek "Batman" dark  theme.

---

## Overview
Flight Tracker integrates with an external aviation API to provide live flight information, analytics for top flights on specific routes, and a local database for historical flight data. The app's modern UI and robust architecture make it a go-to tool for aviation enthusiasts and data-driven travelers.

---

## Features

### Real-Time Flight Tracking
- Input a flight's **IATA code** to fetch and display live flight details, including:
  - Status, airline, departure, arrival
  - Location coordinates, altitude, and speed

### Top Flights Analytics
- Enter departure and arrival **IATA codes** to view insights on the **top 5 flights** for the route.
- Displays flight durations and calculates the **average duration** for the route.

### Flight History
- Stores flight records in a local **Room database**.
- View historical data sorted by date, including flight IATA, duration, and timestamp.

### Background Data Updates
- Uses **WorkManager** to periodically fetch flight data in the background.
- Supports both one-time and recurring tasks to keep data up-to-date.

### Modern UI with Jetpack Compose
- Built with **Jetpack Compose** for a declarative, responsive, and visually appealing UI.
- Features a custom **BatmanTheme** with dark  aesthetics.

---

## Tech Stack
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-1.5.0-4285F4.svg?style=flat-square&logo=android&logoColor=white)
![Retrofit](https://img.shields.io/badge/Retrofit-2.9.0-FFCA28.svg?style=flat-square)
![Room](https://img.shields.io/badge/Room-2.5.0-4CAF50.svg?style=flat-square)
![WorkManager](https://img.shields.io/badge/WorkManager-2.8.0-1976D2.svg?style=flat-square)

- **Kotlin**: Primary programming language.
- **Jetpack Compose**: Modern UI toolkit for declarative UI design.
- **Retrofit**: For API communication with aviation data endpoints.
- **Room**: Local database for persistent storage of flight records.
- **WorkManager**: For scheduling background tasks.
- **Gson**: For JSON parsing.
- **Coroutines**: For asynchronous programming.

---

## Architecture
The app follows a **layered architecture** to ensure maintainability and scalability:

### Network Layer
- **Retrofit API Integration**: Communicates with an external aviation API via two endpoints:
  - `getFlightByIATA`: Fetches details for a specific flight.
  - `getFlightsByRoute`: Retrieves data for flights on a specified route.
- **Data Models**: Structured classes like `AviationStackResponse` and `AviationFlightData` map API responses.

### Business Logic Layer
- **FlightViewModel**: Manages UI state and business logic, including real-time tracking with a coroutine-based polling loop (refreshes every minute).
- **FlightDataWorker**: A `CoroutineWorker` that periodically fetches and stores flight data for a specific route (e.g., LAX to JFK).

### Data Persistence Layer
- **Room Database**: Stores flight records using:
  - **Entity**: `FlightRecordEntity` for storing flight details.
  - **DAO**: `FlightRecordDao` for CRUD operations and querying historical data.
  - **Database Singleton**: Ensures thread-safe access to the database.

### Presentation Layer
- **Jetpack Compose UI**:
  - **Home Screen**: Navigation hub for app features.
  - **Track Flight Screen**: Input IATA code to view real-time flight data.
  - **Top Flights Screen**: Displays top 5 flights for a route with average duration.
  - **Flight History Screen**: Shows stored flight records in a scrollable list.
  - **Reusable Components**: `InfoRow` for consistent data display.
- **Navigation**: Uses **Compose Navigation** for seamless screen transitions.
- **Theming**: Custom `BatmanTheme` with dark  colors and edge-to-edge display support.

---

## Demo

https://github.com/user-attachments/assets/206202d9-8936-4095-a510-5a3f03b8a115


---

## Getting Started

### Prerequisites
- Android Studio (Latest stable version)
- Kotlin 1.9.0 or higher
- An API key from an aviation data provider (e.g., AviationStack)

### Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/flight-tracker.git
   ```
2. **Open in Android Studio**:
   - Import the project into Android Studio.
3. **Add API Key**:
   - Create a `local.properties` file in the project root.
   - Add your aviation API key: `AVIATION_API_KEY=your_api_key_here`
4. **Build and Run**:
   - Sync the project with Gradle.
   - Run the app on an emulator or physical device (API 21+).

---

## Usage
1. **Track a Flight**:
   - Navigate to the **Track Flight Screen**.
   - Enter a flight's IATA code (e.g., `AA123`).
   - Start tracking to view real-time data like altitude, speed, and coordinates.

2. **Analyze Top Flights**:
   - Go to the **Top Flights Screen**.
   - Input departure and arrival IATA codes (e.g., `LAX` to `JFK`).
   - View the top 5 flights and their average duration.

3. **View Flight History**:
   - Access the **Flight History Screen** to see stored flight records.
   - Records are sorted by date and include flight IATA, duration, and timestamp.

4. **Background Updates**:
   - The app automatically schedules background tasks to fetch and store flight data using **WorkManager**.

---

## Code Highlights
- **Retrofit Setup**: Configured in `RetrofitInstance` with Gson for JSON parsing.
- **FlightViewModel**: Handles real-time tracking with coroutines and state management.
- **Room Database**: Persists flight data with `FlightRecordEntity` and `FlightRecordDao`.
- **WorkManager**: Schedules periodic data fetching with `FlightDataWorker`.
- **Jetpack Compose**: Powers the UI with composable functions like `HomeScreen`, `TrackFlightScreen`, and reusable `InfoRow`.

---

## License
![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)  
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Contributing
Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/YourFeature`).
3. Commit your changes (`git commit -m 'Add YourFeature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a pull request.

---

## Acknowledgements
- [AviationStack](https://aviationstack.com/) for providing flight data APIs.
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI development.
- [Giphy](https://giphy.com/) for free dark -themed GIFs.
- [Freepik](https://freepik.com/) for -themed images.

---

*Fly high with Flight Tracker and explore the skies with real-time insights!*
