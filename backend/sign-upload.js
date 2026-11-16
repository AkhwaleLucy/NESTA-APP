// Cloudflare Worker for Cloudinary Upload & Deletion Signing
// Verifies Firebase Auth ID Token, project identity, email verification, and agent approval in Firestore.

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, DELETE, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, Authorization",
        },
      });
    }

    if (request.method !== "POST" && request.method !== "DELETE") {
      return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405 });
    }

    try {
      const authHeader = request.headers.get("Authorization") || "";
      const idToken = authHeader.replace("Bearer ", "").trim();

      if (!idToken) {
        return new Response(JSON.stringify({ error: "Authorization token required" }), { status: 401 });
      }

      // Step 1: Verify Firebase Token & Project Identity via Identity Toolkit REST API
      const authResp = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${env.FIREBASE_API_KEY}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken }),
      });

      const authData = await authResp.json();
      if (!authData.users || authData.users.length === 0) {
        return new Response(JSON.stringify({ error: "Invalid or expired Firebase authentication token" }), { status: 401 });
      }

      const user = authData.users[0];
      const uid = user.localId;
      const emailVerified = user.emailVerified === true;

      if (!emailVerified) {
        return new Response(JSON.stringify({ error: "Email verification required before uploading or managing listing photos" }), { status: 403 });
      }

      // Step 2: Query Firestore user profile using authorized user ID token
      const firestoreUserResp = await fetch(`https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/users/${uid}`, {
        headers: { "Authorization": `Bearer ${idToken}` }
      });
      const userDoc = await firestoreUserResp.json();

      if (!userDoc.fields) {
        return new Response(JSON.stringify({ error: "User profile not found in database" }), { status: 403 });
      }

      const role = userDoc.fields.role?.stringValue || "SEEKER";
      const approvalStatus = userDoc.fields.approvalStatus?.stringValue || "PENDING";

      if (role !== "ADMIN" && (role !== "AGENT" || approvalStatus !== "APPROVED")) {
        return new Response(JSON.stringify({ error: "Only approved agents or administrators are authorized to manage listing photos" }), { status: 403 });
      }

      const body = await request.json();
      const propertyId = body.propertyId;

      if (!propertyId || typeof propertyId !== "string" || propertyId.trim().length === 0) {
        return new Response(JSON.stringify({ error: "Property ID required" }), { status: 400 });
      }

      // Step 3: Require Existing Firestore Property Document & Verify OwnerId using authorized user ID token
      const propResp = await fetch(`https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/properties/${propertyId}`, {
        headers: { "Authorization": `Bearer ${idToken}` }
      });

      if (propResp.status !== 200) {
        return new Response(JSON.stringify({ error: "Property document does not exist in Firestore. An initial draft property must be saved before uploading photos." }), { status: 404 });
      }

      const propDoc = await propResp.json();
      const ownerId = propDoc.fields?.ownerId?.stringValue;
      const status = propDoc.fields?.status?.stringValue;

      if (status === "SUSPENDED") {
        return new Response(JSON.stringify({ error: "Property listing is suspended. Photo modifications are prohibited." }), { status: 403 });
      }

      if (role !== "ADMIN" && ownerId !== uid) {
        return new Response(JSON.stringify({ error: "Unauthorized: You are not the recorded owner of this property document in Firestore." }), { status: 403 });
      }

      const timestamp = Math.floor(Date.now() / 1000);

      // Handle Asset Deletion / Destroy Action
      if (request.method === "DELETE" || body.action === "destroy") {
        const publicId = body.publicId;
        if (!publicId || !publicId.startsWith(`properties/${propertyId}/`)) {
          return new Response(JSON.stringify({ error: "Invalid public ID for asset deletion" }), { status: 400 });
        }

        // Verify public ID is recorded in property's imagePublicIds list in Firestore
        const recordedPublicIds = propDoc.fields?.imagePublicIds?.arrayValue?.values?.map(v => v.stringValue) || [];
        if (role !== "ADMIN" && !recordedPublicIds.includes(publicId)) {
          return new Response(JSON.stringify({ error: "Unauthorized: Asset public ID is not recorded for this property listing." }), { status: 403 });
        }

        // Official Cloudinary SHA-1 Destroy Signature: public_id=...&timestamp=...<API_SECRET>
        const stringToSign = `public_id=${publicId}&timestamp=${timestamp}${env.CLOUDINARY_API_SECRET}`;
        const encoder = new TextEncoder();
        const data = encoder.encode(stringToSign);
        const hashBuffer = await crypto.subtle.digest("SHA-1", data);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const signature = hashArray.map(b => b.toString(16).padStart(2, "0")).join("");

        return new Response(JSON.stringify({
          action: "destroy",
          signature,
          timestamp,
          publicId,
          apiKey: env.CLOUDINARY_API_KEY,
          cloudName: env.CLOUDINARY_CLOUD_NAME,
        }), {
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
          },
        });
      }

      // Step 4: Generate Server-Side Asset Identifiers & Parameters for Upload
      const randomHex = Array.from(crypto.getRandomValues(new Uint8Array(4)))
        .map(b => b.toString(16).padStart(2, "0")).join("");
      const folder = `properties/${propertyId}`;
      const publicId = `properties/${propertyId}/img_${timestamp}_${randomHex}`;
      const overwrite = "false";

      // Official Cloudinary SHA-1 Upload Signature Algorithm:
      // Sorted parameters alphabetically: folder, overwrite, public_id, timestamp
      const stringToSign = `folder=${folder}&overwrite=${overwrite}&public_id=${publicId}&timestamp=${timestamp}${env.CLOUDINARY_API_SECRET}`;

      const encoder = new TextEncoder();
      const data = encoder.encode(stringToSign);
      const hashBuffer = await crypto.subtle.digest("SHA-1", data);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      const signature = hashArray.map(b => b.toString(16).padStart(2, "0")).join("");

      return new Response(JSON.stringify({
        action: "upload",
        signature,
        timestamp,
        folder,
        publicId,
        overwrite: false,
        apiKey: env.CLOUDINARY_API_KEY,
        cloudName: env.CLOUDINARY_CLOUD_NAME,
      }), {
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      });
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message || "Signing server error" }), { status: 500 });
    }
  }
};
