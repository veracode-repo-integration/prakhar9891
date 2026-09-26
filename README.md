# Java POS Backend API Documentation

This backend is built using **Spring Boot 2.7.x** (Java 11) and follows a **Three-Layered Architecture** (Controller, Service, Repository).

## Base URL
`http://localhost:4000/api`


## Security Protocols (Zero Trust)
-   **Authentication**: All endpoints (except Auth) require a valid JWT Token.
-   **Header**: `Authorization: Bearer <your_jwt_token>`
-   **CSRF**: Disabled (Stateless API).
-   **Security Headers**: X-Frame-Options (DENY), X-XSS-Protection (Enabled).

---


## 1. Authentication

### Register
-   **URL**: `/register`
-   **Method**: `POST`
-   **Access**: Public
-   **Request Body**:
    ```json
    {
      "username": "admin",
      "passwordHash": "securePass123",  // Mapped to password
      "firstName": "John",
      "lastName": "Doe",
      "email": "admin@pos.com",
      "role": "admin" // or "staff"
    }
    ```
-   **Response**:
    ```json
    { "ok": true }
    ```

### Login
-   **URL**: `/login`
-   **Method**: `POST`
-   **Access**: Public
-   **Request Body**:
    ```json
    {
      "identifier": "admin", // Username or Email
      "passwordHash": "securePass123"
    }
    ```
-   **Response**:
    ```json
    {
      "ok": true,
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "username": "admin",
        "name": "John",
        "email": "admin@pos.com",
        "role": "admin"
      }
    }
    ```

---

## 2. Products

### Get All Products
-   **URL**: `/pos-products`
-   **Method**: `GET`
-   **Access**: Authenticated
-   **Response**: Array of Product objects.

### Create Product
-   **URL**: `/pos-products`
-   **Method**: `POST`
-   **Access**: Authenticated
-   **Request Body**:
    ```json
    {
      "name": "Apple",
      "price": 100,
      "stock": 50,
      "category": "Fruits",
      "barcode": "112233"
    }
    ```
-   **Response**:
    ```json
    { "success": true, "product": { ... } }
    ```

### Delete Product
-   **URL**: `/pos-products/{id}`
-   **Method**: `DELETE`
-   **Access**: Authenticated
-   **Response**: `{ "success": true }`

---

## 3. Orders

### Checkout (Place Order)
-   **URL**: `/checkout`
-   **Method**: `POST`
-   **Access**: Authenticated
-   **Request Body**:
    ```json
    {
      "cart": [
        { "id": "prod-1", "name": "Apple", "price": 100, "qty": 2 }
      ],
      "paymentInfo": {
        "method": "cash",
        "note": "Optional note",
        "customer": { "id": "cust-1", "name": "Jane" } // Optional
      },
      "user": { "username": "staff1", "name": "Staff Member" }
    }
    ```
-   **Response**:
    ```json
    { "ok": true, "order": { "id": "ORD-123...", "total": 200, ... } }
    ```

### Get Order History
-   **URL**: `/pos-orders`
-   **Method**: `GET`
-   **Access**: Authenticated
-   **Response**: Array of Order objects.

---

## 4. Common Data

### Customers
-   `GET /customers`: List all customers.
-   `POST /customers`: Create/Update customer. Body: `{ "name": "...", "mobile": "..." }`
-   `DELETE /customers/{id}`: Delete customer.

### Expenses
-   `GET /expenses`: List expenses.
-   `POST /expenses`: Create expense. Body: `{ "title": "...", "amount": 100 }`
-   `DELETE /expenses/{id}`: Delete expense.

### Settings
-   `GET /settings`: Get global settings.
-   `PUT /settings`: Update settings. Body: `{ "taxRate": 0.1, "storeName": "My POS" }`

### Credit Notes
-   `GET /credit-notes`: List credit notes.
-   `POST /credit-notes`: Create note. Body: `{ "customerName": "...", "amount": 50 }`

---

## 5. Admin (User Management)
**Requires `ROLE_ADMIN`**

### List Users
-   **URL**: `/users`
-   **Method**: `GET`
-   **Response**: List of users (masked).

### Change Role
-   **URL**: `/users/{id}/role`
-   **Method**: `PUT`
-   **Request Body**: `{ "role": "admin" }` (or "staff")

