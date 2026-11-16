# NESTA - Real-Estate Property Discovery & Listing Platform 🏠🇰🇪

> *"Find a place that feels like home."*

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)](#)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](#)

---

## 📌 Project Overview

**NESTA** is a native Android real-estate marketplace platform tailored for **Nairobi, Kenya**, designed with clean architecture and Material 3 design to deliver a visual, fast, and trustworthy property discovery experience for **Property Seekers**, **Property Owners & Agents**, and **Platform Administrators**.

---

## ✨ Key Features & Roles

### 🔍 1. Property Seekers
- **Smart Natural Language Search**: Query properties using natural language (e.g. *"2 bedroom apartment in Kilimani under 80k"*).
- **Interactive Nairobi Map Canvas**: View available listings as price pins (`KSh 75k`, `KSh 32M`) with callout preview cards.
- **Filter Bottom Sheet**: Filter by Listing Type (Rent/Sale), Property Type, Price Range, Bedrooms, Bathrooms, and 17+ Amenities.
- **Side-by-Side Property Comparison**: Compare up to 4 selected listings in a comparative matrix.
- **Saved Wishlist & Inquiries**: Save favorites, schedule viewing appointments, and send in-app messages to listing owners.

### 🏢 2. Property Owners & Agents
- **Agent Application & Admin Approval**: Agent accounts require both verified email and administrator approval (`approvalStatus == APPROVED`) before property creation.
- **Listing Creation Wizard**: 5-step wizard with image selection, amenity picker, and live Listing Quality Score Meter.
- **Agent Portal Dashboard**: Track active listings, total views, saves, and viewing inquiry appointments.

### 🛡️ 3. Platform Administrators
- **Moderation Console**: 3 moderation queues for **Property Listings**, **Agent Approvals**, and **Flagged Reports**.
- **Agent Approvals Queue**: Review applicant profile details and approve or reject agent applications with audit logging (`reviewedBy`, `reviewedAt`, `rejectionReason`).
- **Property Review Queue**: Approve (`VERIFIED`/`AVAILABLE`), reject with reason, or suspend listings.

---

## 🔐 Security & Backend Architecture

- **Firebase Authentication**: Email & password authentication with email verification link dispatch.
- **Cloud Firestore Database**: Persistent storage for `/users`, `/properties`, `/viewing_requests`, `/conversations`, `/messages`, `/reports`, `/notifications`, and `/property_alerts`.
- **Backend Security Rules (`firestore.rules`)**:
  - Non-administrators cannot modify `role`, `isAgentApproved`, or `approvalStatus`.
  - New agent listings are forced to `status = PENDING` and `verificationStatus = PENDING`.
  - Unapproved agents and unverified emails cannot create listing documents.
- **Cloudflare Worker Backend Signing (`backend/sign-upload.js`)**:
  - Authorizes Cloudinary image uploads using Firebase Auth ID token verification and Firestore agent approval checks.
  - Computes SHA-1 signatures for Cloudinary uploads without storing API secrets in the Android app.

---

## 🎨 High-Contrast Design System & Visual Palette

| Token | Hex Code | Role | Contrast Ratio |
| :--- | :--- | :--- | :---: |
| **Deep Navy** | `#17202A` | Primary Brand Accent & Headers | **14.8:1** (on Warm Off-White) |
| **Warm Off-White** | `#FAF9F6` | Screen Backgrounds | **14.8:1** |
| **Dark Charcoal** | `#1C1C1C` | Primary Body Text | **18.1:1** (on Card White) |
| **Muted Text** | `#4A5568` | Supporting Text & Placeholders | **7.1:1** (on Card White) |
| **Muted Terracotta** | `#B34A28` | Primary CTA Highlights & Badges | **5.6:1** (on Warm Ivory) |
| **Verified Green** | `#1B5E20` | Verified Badge Accent | **8.2:1** (on Verified Green Surface) |
| **Pending Orange** | `#D84315` | Approval Pending Warning Badge | **4.8:1** (on Pending Orange Surface) |
| **Rejected Red** | `#B71C1C` | Error & Rejection Banners | **7.5:1** (on Rejected Red Surface) |

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: androidx.navigation.compose
- **Image Loading**: Coil (`io.coil-kt:coil-compose`)
- **Backend**: Firebase Auth, Cloud Firestore, Cloudflare Worker (Node.js/JS)
- **Target SDK**: Android 15 / API 35 (Compile SDK 37, Min SDK 24)

---

## 🚀 Getting Started & Setup

### 1. Android App Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/AkhwaleLucy/NESTA-APP.git
   cd NESTA-APP
   ```
2. Open in Android Studio (2024.2+ or higher).
3. Sync Gradle and build the project (`./gradlew app:assembleDebug`).

### 2. Deploy Firestore Rules
To deploy Firestore security rules to live project `nesta-8f512`:
```bash
npx firebase-tools login
npx firebase-tools deploy --only firestore:rules --project nesta-8f512
```

### 3. Deploy Cloudflare Worker Signing Backend
To deploy the backend upload signing script in `backend/sign-upload.js`:
```bash
cd backend
npx wrangler login
npx wrangler secret put CLOUDINARY_API_SECRET
npx wrangler deploy
```

---

## 🧪 Current Verification Status

- **Android App Compilation**: Passed (`assembleDebug` builds cleanly with 0 errors).
- **Firestore Security Rules**: Deployed and live on Firebase project `nesta-8f512`.
- **Local Unit Tests**: Passed (`NestaRepositoryTest.kt` verifies role routing, seeker bounds, pending agent restrictions, and admin approvals).
- **Live Cloudflare Worker Deployment**: Requires user execution of `npx wrangler deploy` with Cloudinary account credentials.

---

## 📜 License

Distributed under the **MIT License**.
