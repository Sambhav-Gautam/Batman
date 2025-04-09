# Flight Tracker: Real-Time Flight Insights & Data Analytics

Welcome to the **Flight Tracker** project—a robust Android application designed to deliver real-time flight tracking, top flights data analytics, and detailed historical flight records. This project is built with modern Android technologies including Jetpack Compose, Retrofit, Room, and WorkManager for a seamless and reactive experience.

---

## Overview

Batman Flight Tracker integrates with an external aviation API to fetch live flight information, allowing users to track individual flights, view top flights for specific routes, and access historical flight data stored locally. The app is themed with a "Batman" motif that enhances its visual appeal while providing utility.

---

## Features

- **Real-Time Flight Tracking:**  
  Fetch and display live details of a flight by inputting the flight's IATA code. Live flight parameters such as status, airline, departure, arrival, location coordinates, altitude, and speed are displayed.

- **Top Flights Analytics:**  
  Enter departure and arrival IATA codes to get insights on the top 5 flights on the route. This includes calculating the flight durations and providing an average duration for the route.

- **Flight History:**  
  A local Room database stores detailed records of flight data. Users can review these records sorted by date, which include flight IATA, duration in minutes, and the timestamp for when the data was recorded.

- **Background Data Updates:**  
  WorkManager is utilized to perform periodic background fetching of flight data. This ensures that the app maintains up-to-date information by scheduling both one-time and recurring work tasks.

- **Modern UI with Jetpack Compose:**  
  The user interface is implemented using Jetpack Compose, enabling a declarative approach to UI design that supports themes, responsive layouts, and smooth transitions through composable screens.

---

## Architecture

The application follows a layered architecture designed to separate concerns and enhance maintainability:

1. **Network Layer:**
   - **Retrofit API Integration:**  
     The app uses Retrofit to communicate with an external aviation API. Two main API endpoints are provided:
     - **`getFlightByIATA`:** Retrieves details for a flight based on its IATA code.
     - **`getFlightsByRoute`:** Fetches flight details for a specified departure and arrival route.
   - **Data Models:**  
     Data classes are defined to map the JSON responses from the API, such as `AviationStackResponse`, `AviationFlightData`, and nested models (`FlightInfo`, `LiveInfo`, `AirportInfo`, `AirlineInfo`, and `AircraftInfo`).

2. **Business Logic Layer:**
   - **ViewModel (`FlightViewModel`):**  
     This component handles UI state management and business logic. It provides methods to fetch flight data, start and stop real-time tracking, and update the UI accordingly. Continuous tracking is implemented through a coroutine loop that refreshes flight details every minute.
   - **Background Worker (`FlightDataWorker`):**  
     A dedicated worker class extends `CoroutineWorker` to periodically fetch flight data for a specific route (example: from LAX to JFK) and store the results in the local database. It parses flight times to calculate flight duration and handles error logging.

3. **Data Persistence Layer:**
   - **Room Database:**  
     The local database, implemented using Room, persists flight records. The key components here include:
     - **Entity (`FlightRecordEntity`):** Represents each flight record stored in the database.
     - **Data Access Object (DAO) (`FlightRecordDao`):** Contains SQL queries to insert flight records, compute weekly averages for a given route, and retrieve historical flight records in descending order.
   - **Database Singleton:**  
     A thread-safe singleton instance of the database is provided to ensure a single access point throughout the application lifecycle.

4. **Presentation Layer:**
   - **Jetpack Compose UI Components:**  
     The UI is organized into several composable functions and screens:
     - **Home Screen:**  
       Acts as the main navigation hub where users can choose to track a flight, view top flights, or see historical flight data.
     - **Track Flight Screen:**  
       Allows users to enter a flight IATA code and displays real-time data. Users can start or stop tracking based on the current state.
     - **Top Flights Screen:**  
       Provides inputs for departure and arrival IATA codes, fetches data for the top 5 flights of that route, displays each flight’s details, and computes the average flight duration.
     - **Flight History Screen:**  
       Displays a chronological list of stored flight records from the Room database, including details like the flight's IATA code, flight duration, and record timestamp.
   - **Reusable UI Components:**  
     Components like `InfoRow` are used across different screens to consistently display label-value pairs in a formatted manner.

5. **Navigation and Theming:**
   - The app leverages Compose Navigation to manage transitions between screens seamlessly.  
   - A custom theme (`BatmanTheme`) is used to style the application, including color palettes, typography, and overall visual design.
   - Edge-to-edge display support is enabled to maximize screen space, providing a modern, immersive experience.

---

## Detailed Explanation of Code Components

- **Retrofit and API Communication:**  
  The project defines a Retrofit instance under `RetrofitInstance` that sets up the base URL and a Gson converter. The `AviationStackApi` interface details the API endpoints required to fetch flight data. This design allows for clear separation between network communication and data processing.

- **Data Models:**  
  Models such as `AviationStackResponse` and `AviationFlightData` encapsulate the information received from the aviation API. Nested data models ensure that every aspect of flight information—from flight identifiers to live tracking data—is appropriately structured.

- **Flight Tracking ViewModel:**  
  The `FlightViewModel` is responsible for real-time updates. It fetches data from the API, manages error states, and maintains a flag for when tracking is active. The method `startRealTimeTracking` continuously polls the API using a one-minute delay loop to update flight data.

- **Room Database Integration:**  
  The app uses Room for persistent storage, with an entity class `FlightRecordEntity` that represents each stored flight record. The DAO (`FlightRecordDao`) provides methods not only for inserting records but also for querying historical data, ensuring that users can see long-term trends and statistics.

- **WorkManager Background Tasks:**  
  `FlightDataWorker` is defined as a coroutine-based worker that runs in the background. Its primary function is to periodically fetch flight data for a hardcoded route and save it to the local database. Two scheduling functions, `scheduleTestFlightDataWorker` and `schedulePeriodicFlightDataWorker`, are provided to manage one-time and recurring work requests, respectively.

- **User Interface Implementation:**  
  Multiple composable functions define the various screens:
  - **HomeScreen:**  
    Contains navigation buttons for different features.
  - **TrackFlightScreen:**  
    Features an input field for entering the flight IATA code, buttons to initiate or halt tracking, and displays flight details using a card layout.
  - **TopFlightsScreen:**  
    Uses inputs for departure and arrival codes to fetch and display the top flights for that route, along with error handling and average duration calculation.
  - **FlightHistoryScreen:**  
    Retrieves flight history data from the Room database and renders it in a scrollable list.
  - **Reusable Component (`InfoRow`):**  
    A helper composable used throughout the UI to consistently display individual pieces of information.

- **Main Activity and Navigation Setup:**  
  `MainActivity` serves as the entry point for the app. It initializes the edge-to-edge experience, schedules background workers immediately upon launch, and sets up the navigation graph using the composable `NavHost`. This structure ensures that users can seamlessly switch between screens while benefiting from automatic data updates in the background.

---

Flight Tracker seamlessly combines data fetching, persistent storage, and dynamic user interfaces to provide an engaging experience for real-time flight insights and historical analysis. Enjoy exploring flight data with this modern Android application!
