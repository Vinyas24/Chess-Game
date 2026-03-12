# Premium Full-Stack Chess Game ♛

A modern, full-stack chess application built with a custom Java Spring Boot engine and a stunning React Vite frontend featuring a Glassmorphism design system. 

Provides full support for all traditional chess rules (Castling, En Passant, Pawn Promotion) along with a robust Artificial Intelligence (AI) opponent.

🌐 **Live Demo:** [Play Chess Now](https://chess-game-psi-dusky.vercel.app/)

---

## ✨ Features
- **Glassmorphism UI:** Premium aesthetic utilizing TailwindCSS with translucent panels, dynamic background orbs, and elegant Unicode piece typography.
- **Smart Move Engine:** Click a piece to highlight valid move paths. Attack squares are highlighted dynamically.
- **Game Modes:** Choose between **Pass and Play (Local 1v1)** or **Play vs AI** (playing as White or Black).
- **Backend AI Algorithm:** The integrated Java Engine calculates optimal moves using a recursive material evaluation heuristic, enabling a challenging PvE experience.
- **Dynamic Checkmate Alerts:** Visual pulsing animations for checks, and a full-screen React Confetti "Birthday Pop" celebration when checkmate is delivered!

## 🏗️ Architecture

The project is split into two distinct tiers:

1. **`backend/` - Spring Boot (Java 17+)**
   - Originally a terminal-based monolithic Java engine, refactored into a stateless REST API via `GameController`.
   - Manages games in a concurrent HashMap, storing state via UUIDs.
   - Live Deployed on: **[Render](https://chess-game-5l3v.onrender.com/)**

2. **`frontend/` - Vite + React**
   - High-performance UI rendering the board state provided by the backend.
   - State management is completely agnostic, acting purely as a beautiful presentation layer.
   - Live Deployed on: **[Vercel](https://chess-game-psi-dusky.vercel.app/)**

---

## 🚀 Running Locally

### Prerequisites
- Java 17 or higher
- Node.js v14+ and npm

### 1. Start the Java Backend
Navigate to the `backend` directory and run the Spring Boot server (listens on `http://localhost:8080`):
```bash
cd backend
./mvnw clean spring-boot:run
```

### 2. Start the React Frontend
Open a **new** terminal window, navigate to the `frontend` directory, install dependencies, and start Vite:
```bash
cd frontend
npm install
npm run dev
```

The application will be available at `http://localhost:5173`.

---

## ⚙️ Deployment Instructions

### Vercel (Frontend)
1. Import the repository into Vercel.
2. Ensure the "Framework Preset" is set to `Vite`.
3. Set the "Root Directory" to `frontend`.
4. Deploy.

### Render (Backend)
1. Create a "Web Service" in Render.
2. Select `Java` as the environment.
3. Set the "Root Directory" to `backend`.
4. **Build Command:** `./mvnw clean package -DskipTests`
5. **Start Command:** `java -jar target/chess-0.0.1-SNAPSHOT.jar`
6. Important: Ensure the free tier port dynamically maps to Spring Boot's internal `8080` wrapper, which happens automatically!

---
*Built with ❤️ for serious and casual chess players alike.*
