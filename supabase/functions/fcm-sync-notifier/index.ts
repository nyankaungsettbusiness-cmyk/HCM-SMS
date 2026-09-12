// Supabase Edge Function: fcm-sync-notifier
// Serves as the secure server-side broadcaster for Firebase Cloud Messaging (FCM) sync hints.
// Adheres strictly to security guidelines: Uses Firebase HTTP v1 API with Server-side OAuth2 Service Account.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

interface WebhookPayload {
  type: "INSERT" | "UPDATE" | "DELETE";
  table: string;
  schema: string;
  record: Record<string, any>;
  old_record?: Record<string, any>;
}

// Generate Google OAuth2 Access Token from Service Account JSON
async function getGoogleOAuth2AccessToken(serviceAccount: any): Promise<string> {
  const iat = Math.floor(Date.now() / 1000);
  const exp = iat + 3600;

  const header = {
    alg: "RS256",
    typ: "JWT",
  };

  const claimSet = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: exp,
    iat: iat,
  };

  const encodeBase64Url = (obj: any) =>
    btoa(JSON.stringify(obj))
      .replace(/=/g, "")
      .replace(/\+/g, "-")
      .replace(/\//g, "_");

  const unsignedJwt = `${encodeBase64Url(header)}.${encodeBase64Url(claimSet)}`;

  // Import private key for signing
  const pemHeader = "-----BEGIN PRIVATE KEY-----";
  const pemFooter = "-----END PRIVATE KEY-----";
  const pemContents = serviceAccount.private_key
    .replace(pemHeader, "")
    .replace(pemFooter, "")
    .replace(/\s+/g, "");

  const binaryDerString = atob(pemContents);
  const binaryDer = new Uint8Array(binaryDerString.length);
  for (let i = 0; i < binaryDerString.length; i++) {
    binaryDer[i] = binaryDerString.charCodeAt(i);
  }

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer.buffer,
    {
      name: "RSASSA-PKCS1-v1_5",
      hash: "SHA-256",
    },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(unsignedJwt)
  );

  const signatureBase64Url = btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");

  const signedJwt = `${unsignedJwt}.${signatureBase64Url}`;

  // Exchange JWT for OAuth2 Access Token
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: signedJwt,
    }),
  });

  if (!tokenResponse.ok) {
    const errorText = await tokenResponse.text();
    throw new Error(`Failed to exchange token with Google OAuth2: ${errorText}`);
  }

  const tokenData = await tokenResponse.json();
  return tokenData.access_token;
}

serve(async (req: Request) => {
  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const serviceAccountJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_KEY");

    if (!serviceAccountJson) {
      return new Response(
        JSON.stringify({ error: "Missing FIREBASE_SERVICE_ACCOUNT_KEY secret in Supabase environment" }),
        { status: 500, headers: { "Content-Type": "application/json" } }
      );
    }

    const serviceAccount = JSON.parse(serviceAccountJson);
    const projectId = serviceAccount.project_id;
    const supabase = createClient(supabaseUrl, supabaseServiceRoleKey);

    const payload: WebhookPayload = await req.json();
    const { table, record } = payload;
    const schoolId = record?.school_id || "default_school";
    const version = String(record?.version || "1");

    // Query active device tokens for the target school
    const { data: devices, error: dbError } = await supabase
      .from("device_tokens")
      .select("token, user_id")
      .eq("school_id", schoolId);

    if (dbError || !devices || devices.length === 0) {
      return new Response(
        JSON.stringify({ message: "No registered device tokens for school", schoolId }),
        { status: 200, headers: { "Content-Type": "application/json" } }
      );
    }

    const accessToken = await getGoogleOAuth2AccessToken(serviceAccount);
    const fcmEndpoint = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

    let successCount = 0;
    let failureCount = 0;

    for (const device of devices) {
      const fcmMessage = {
        message: {
          token: device.token,
          // Thin sync hint - NEVER contains PII, payment amounts, or sensitive records
          data: {
            type: "sync_hint",
            entity: table,
            school_id: schoolId,
            version: version,
          },
          android: {
            priority: "high",
            collapse_key: `sync_${table}`,
          },
        },
      };

      const res = await fetch(fcmEndpoint, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(fcmMessage),
      });

      if (res.ok) {
        successCount++;
      } else {
        failureCount++;
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        schoolId,
        entity: table,
        tokensTargeted: devices.length,
        dispatched: successCount,
        failed: failureCount,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  } catch (error: any) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
});
