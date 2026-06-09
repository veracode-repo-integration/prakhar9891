# 🐘 PostgreSQL & 🖼️ Image Storage Setup

Since you have many images, storing them directly in the database will make it slow. The professional way is to store images in **Google Cloud Storage (GCS)** and store the **URLs** in **PostgreSQL**.

---

## 1. Setup PostgreSQL on Google Cloud
1.  Go to **Cloud SQL** in your Google Cloud Console.
2.  Click **Create Instance** and select **PostgreSQL**.
3.  **Instance ID**: `pos-postgres-instance`
4.  **Version**: PostgreSQL 15 (or latest).
5.  **Region**: `us-central1`.
6.  **Connections**: Ensure "Public IP" is checked.
7.  **Create Instance** (Takes 3-5 mins).
8.  Go to the **Databases** tab and create a database named `posdb`.
9.  Go to the **Users** tab and create a user (e.g., `postgres`).

---

## 2. Setup Image Storage (Google Cloud Storage)
1.  Go to **Cloud Storage > Buckets**.
2.  Click **Create**.
3.  **Name**: `pos-system-images-99` (Must be unique globally).
4.  **Location**: `us-central1`.
5.  **Permissions**: 
    - Uncheck "Enforce public access prevention on this bucket".
    - Set Access Control to **Uniform**.
6.  **Make Public (Optional)**: If you want everyone to see images without a signed URL:
    - Go to the **Permissions** tab of your bucket.
    - Click **Grant Access**.
    - New Principal: `allUsers`
    - Role: `Cloud Storage > Storage Object Viewer`.

---

## 3. Update GitHub Secrets
Go to your GitHub repo **Settings > Secrets > Actions** and add:

| Secret | Value Example |
| :--- | :--- |
| `DB_URL` | `jdbc:postgresql:///posdb?cloudSqlInstance=PROJECT_ID:us-central1:INSTANCE_ID&socketFactory=com.google.cloud.sql.postgres.SocketFactory` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `Billing@2026` |
| `GCP_PROJECT_ID` | `your-google-project-id` |
| `GCP_BUCKET_NAME` | `pos-system-images-yourproject` |

---

## 4. How Image Saving Works
Your backend is now configured to:
1.  Receive an image from the mobile app.
2.  Upload it to your GCS Bucket.
3.  Get the Public URL (e.g., `https://storage.googleapis.com/.../image.jpg`).
4.  Save that **URL** string into your PostgreSQL `products` or `orders` table.

This keeps your database small and your image loading extremely fast!
