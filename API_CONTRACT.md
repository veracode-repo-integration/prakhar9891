# API Contract Documentation
This document outlines the JSON Request and Response structures for all backend endpoints in the POS system.

## 1. Authentication (`/api/auth`)

### Register
- **Endpoint**: `POST /api/register`
- **Request**:
```json
{
  "username": "admin",
  "password": "password123",
  "email": "admin@pos.com",
  "firstName": "Admin User",
  "contact": "9876543210",
  "role": "admin" // 'admin' or 'staff'
}
```
- **Response**:
```json
{
  "ok": true
}
```

### Login
- **Endpoint**: `POST /api/login`
- **Request**:
```json
{
  "identifier": "admin", // username
  "passwordHash": "password123" // raw password
}
```
- **Response**:
```json
{
  "ok": true,
  "token": "JWT_TOKEN_STRING",
  "user": {
    "dbId": 1,
    "username": "admin",
    "firstName": "Admin User",
    "email": "admin@pos.com",
    "contact": "9876543210",
    "role": "admin"
  }
}
```

---

## 2. Products (`/api/pos-products`)

### Get All Products
- **Endpoint**: `GET /api/pos-products`
- **Response**: Array of Product objects
```json
[
  {
    "id": "prod-123",
    "name": "Burger",
    "price": 150.0,
    "stock": 50,
    "category": "Food",
    "image": "url..."
  }
]
```

### Create Product
- **Endpoint**: `POST /api/pos-products`
- **Request**:
```json
{
  "name": "Burger",
  "price": 150.0,
  "stock": 50,
  "category": "Food"
}
```
- **Response**: **Direct Product Object** (Fixed to match Frontend `api.createProduct`)
```json
{
  "id": "prod-123",
  "name": "Burger",
  "price": 150.0,
  ...
}
```

### Update Product
- **Endpoint**: `PUT /api/pos-products/{id}`
- **Request**: Partial Product Object
```json
{
  "price": 160.0
}
```
- **Response**:
```json
{
  "ok": true,
  "product": { ...updatedProduct }
}
```

### Delete Product
- **Endpoint**: `DELETE /api/pos-products/{id}`
- **Response**:
```json
{ "success": true }
```

---

## 3. Orders & Checkout (`/api`)

### Checkout
- **Endpoint**: `POST /api/checkout`
- **Request**:
```json
{
  "items": [
    { "id": "p1", "qty": 2, "price": 100 }
  ],
  "paymentInfo": {
    "method": "cash",
    "customer": { "name": "John", "mobile": "9876543210" }
  },
  "user": { "username": "admin", "role": "admin" }, // Or string "admin"
  "discount": { "type": "percent", "value": 10 }
}
```
- **Response**:
```json
{
  "ok": true,
  "order": {
    "id": "ORD-123",
    "total": 180.0,
    "items": [...],
    "createdAt": "..."
  }
}
```

### Get Orders
- **Endpoint**: `GET /api/pos-orders`
- **Response**: Array of Order objects

---

## 4. Tables (`/api/tables`)

### Get Tables
- **Endpoint**: `GET /api/tables`
- **Response**: Array of Table objects

### Create Table
- **Endpoint**: `POST /api/tables`
- **Request**:
```json
{ "name": "T1", "capacity": 4, "area": "Main" }
```
- **Response**:
```json
{ "ok": true, "table": { ... } }
```

### Update Table (Status/Order)
- **Endpoint**: `PUT /api/tables/{id}`
- **Request**:
```json
{
  "status": "occupied",
  "currentOrder": [ { "id": "p1", "qty": 1 } ] // Can be Array OR String
}
```
- **Response**:
```json
{ "ok": true, "table": { ... } }
```

---

## 5. Customers (`/api/customers`)

### Get Customers
- **Endpoint**: `GET /api/customers`
- **Response**: Array of Customer objects

### Create Customer
- **Endpoint**: `POST /api/customers`
- **Request**:
```json
{ "name": "John Doe", "mobile": "9876543210" }
```
- **Response**:
```json
{ "ok": true, "customer": { ... } }
```

---

## 6. Settings (`/api/settings`)

### Get Settings
- **Endpoint**: `GET /api/settings`
- **Response**: Settings Object directly
```json
{
  "id": "default",
  "merchantName": "My Shop",
  "taxRate": 0.05
}
```

### Update Settings
- **Endpoint**: `PUT /api/settings`
- **Request**:
```json
{ "merchantName": "New Name" }
```
- **Response**:
```json
{ "ok": true, "settings": { ... } }
```

---

## 7. Held Orders (`/api/held-orders`)

### Park Order
- **Endpoint**: `POST /api/held-orders`
- **Request**: Same as Checkout request structure
- **Response**:
```json
{ "ok": true, "heldOrder": { ... }, "count": 5 }
```

---

## 8. Images (`/api`)

### Upload Image
- **Endpoint**: `POST /api/upload-image`
- **Request**: `multipart/form-data` with key `file`
- **Response**:
```json
{
  "success": true,
  "imageUrl": "http://cloudinary.com/..."
}
```

### Delete Image
- **Endpoint**: `DELETE /api/delete-image?imageUrl=...`
- **Response**:
```json
{ "success": true }
```

---

## UI Synchronization check
- **Products**: UI expects flat object on creation. **MATCH**.
- **Checkout**: UI expects `res.order` on success. **MATCH**.
- **Tables**: UI sends array for `currentOrder`. Backend fixed to accept Array/String. **MATCH**.
- **Settings**: UI expects object update. **MATCH**.
- **Customers**: UI creates customer and expects success. **MATCH**.
