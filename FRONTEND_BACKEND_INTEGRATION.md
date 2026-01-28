# 🔗 Frontend-Backend Integration Guide

## 📋 Current Status

### **Frontend (react-mobile-pos-final)**
- ✅ Using **Zustand** for state management
- ✅ Using **Local Storage** for data persistence
- ✅ Complete UI with all features implemented
- ✅ Ready for backend integration

### **Backend (POS-Backend-Code-Clone)**
- ✅ Spring Boot REST API
- ✅ MySQL database configured
- ✅ Cloudinary image storage
- ⚠️ **Needs alignment with frontend data structures**

---

## 🎯 Integration Strategy

### **Phase 1: Data Structure Alignment** (Current)
Ensure backend models match frontend exactly

### **Phase 2: API Endpoint Creation**
Create REST APIs that frontend will call

### **Phase 3: Frontend Migration**
Replace local storage with API calls

---

## 📊 Frontend Data Structures (from store.js)

### **1. Products**
```javascript
{
  id: string,
  name: string,
  price: number,
  category: string,
  stock: number,
  image: string,  // URL
  barcode: string,
  composition: string  // JSON string
}
```

### **2. Orders**
```javascript
{
  id: string,
  note: string,
  items: [...],  // Cart items
  subtotal: number,
  discount: { type: 'amount'|'percent', value: number },
  taxRate: number,
  taxAmount: number,
  total: number,
  date: ISO string,
  paymentInfo: {
    method: string,  // 'Cash', 'UPI', 'Card', 'Pay Later'
    payments: [...],  // For split payments
    customer: {...},
    note: string
  },
  customer: {
    name: string,
    mobile: string,
    loyaltyPoints: number,
    walletBalance: number
  },
  user: string,  // username
  status: 'pending'|'completed',
  isRefunded: boolean,
  payLater: boolean,
  payLaterAmount: number,
  tableId: string,
  orderType: 'dine-in'|'takeaway'|'delivery',
  pointsEarned: number
}
```

### **3. Users**
```javascript
{
  id: string,
  username: string,
  password: string,  // hashed
  name: string,
  email: string,
  phone: string,
  role: 'admin'|'staff',
  createdAt: ISO string
}
```

### **4. Customers**
```javascript
{
  id: string,
  name: string,
  mobile: string,
  joinedAt: ISO string,
  loyaltyPoints: number,
  walletBalance: number
}
```

### **5. Expenses**
```javascript
{
  id: string,
  title: string,
  amount: number,
  category: string,
  createdAt: ISO string
}
```

### **6. Credit Notes**
```javascript
{
  id: string,
  customerName: string,
  mobile: string,
  amount: number,
  createdAt: ISO string,
  usedAt: ISO string,
  isUsed: boolean
}
```

### **7. Tables**
```javascript
{
  id: string,
  name: string,
  area: string,
  status: 'available'|'occupied',
  capacity: number,
  currentOrder: [...],  // Cart items
  guestCount: number,
  sessionStart: timestamp,
  kitchenStatus: string
}
```

### **8. Settings**
```javascript
{
  upiId: string,
  merchantName: string,
  gstNumber: string,
  notifications: boolean,
  language: string,
  currency: string,
  contactNumber: string,
  printerConnected: boolean,
  scannerType: 'Camera'|'HID',
  skipCustomerDetails: boolean
}
```

### **9. Stock Logs**
```javascript
{
  id: string,
  productId: string,
  qtyChange: number,  // +ve or -ve
  type: 'sale'|'restock'|'correction'|'return',
  reason: string,
  user: string,
  createdAt: ISO string
}
```

### **10. Held Orders (Parked Orders)**
```javascript
{
  id: string,
  items: [...],  // Cart items
  note: string,
  createdAt: ISO string
}
```

---

## 🔧 Backend Model Updates Needed

I'll now update the backend models to match the frontend exactly:

1. **Product** - ✅ Already matches
2. **Order** - ⚠️ Needs expansion (add discount, payLater, tableId, orderType, etc.)
3. **User** - ✅ Already matches
4. **Customer** - ⚠️ Needs loyaltyPoints, walletBalance
5. **Expense** - ⚠️ Needs category field
6. **CreditNote** - ⚠️ Needs mobile, isUsed, usedAt
7. **Table** - ❌ Missing entirely
8. **Settings** - ❌ Missing entirely
9. **StockLog** - ❌ Missing entirely
10. **HeldOrder** - ❌ Missing entirely

---

## 🚀 Next Steps

1. **Update Backend Models** - Add missing fields and entities
2. **Create Missing Entities** - Table, Settings, StockLog, HeldOrder
3. **Update Controllers** - Match frontend expectations
4. **Add New Endpoints** - For tables, settings, stock logs
5. **Test Integration** - Ensure data flows correctly

---

## 📝 API Endpoints Needed

### **Authentication**
- `POST /api/register` - ✅ Exists
- `POST /api/login` - ✅ Exists
- `POST /api/logout` - ⚠️ Add

### **Products**
- `GET /api/pos-products` - ✅ Exists
- `POST /api/pos-products` - ✅ Exists
- `PUT /api/pos-products/{id}` - ⚠️ Add
- `DELETE /api/pos-products/{id}` - ✅ Exists

### **Orders**
- `POST /api/checkout` - ✅ Exists (needs update)
- `GET /api/pos-orders` - ✅ Exists
- `PUT /api/pos-orders/{id}` - ⚠️ Add (for status updates)
- `POST /api/pos-orders/{id}/refund` - ⚠️ Add

### **Customers**
- `GET /api/customers` - ✅ Exists
- `POST /api/customers` - ✅ Exists
- `PUT /api/customers/{id}` - ⚠️ Add
- `DELETE /api/customers/{id}` - ✅ Exists

### **Expenses**
- `GET /api/expenses` - ✅ Exists
- `POST /api/expenses` - ✅ Exists
- `DELETE /api/expenses/{id}` - ✅ Exists

### **Credit Notes**
- `GET /api/credit-notes` - ✅ Exists
- `POST /api/credit-notes` - ✅ Exists
- `PUT /api/credit-notes/{id}` - ⚠️ Add (for marking as used)

### **Tables** ❌ NEW
- `GET /api/tables` - Create
- `POST /api/tables` - Create
- `PUT /api/tables/{id}` - Create
- `DELETE /api/tables/{id}` - Create
- `PUT /api/tables/{id}/status` - Create (occupy/clear)

### **Settings** ❌ NEW
- `GET /api/settings` - ✅ Exists
- `PUT /api/settings` - ✅ Exists

### **Stock Logs** ❌ NEW
- `GET /api/stock-logs` - Create
- `POST /api/stock-logs` - Create

### **Held Orders** ❌ NEW
- `GET /api/held-orders` - Create
- `POST /api/held-orders` - Create (park order)
- `DELETE /api/held-orders/{id}` - Create

### **Images**
- `POST /api/upload-image` - ✅ Exists
- `DELETE /api/delete-image` - ✅ Exists

---

## 🎯 Implementation Priority

### **High Priority** (Core Features)
1. Update Order model (discount, payLater, tableId, orderType)
2. Create Table entity and endpoints
3. Update Customer model (loyaltyPoints, walletBalance)
4. Add Product update endpoint

### **Medium Priority** (Important Features)
5. Create StockLog entity and endpoints
6. Create HeldOrder entity and endpoints
7. Update CreditNote model
8. Add order refund endpoint

### **Low Priority** (Nice to Have)
9. Settings management (already exists)
10. Expense category field

---

## 📦 Data Migration Strategy

### **Option 1: Fresh Start** (Recommended for Development)
- Start with empty database
- Frontend creates data via API
- Clean, no migration needed

### **Option 2: Import Existing Data**
- Export from local storage
- Import to MySQL via API
- Useful if you have test data

---

## 🔄 Frontend Migration Steps

After backend is ready:

1. **Create API Service** (`src/services/api.js`)
2. **Update Store** (Replace storage calls with API calls)
3. **Add Error Handling** (Network errors, auth errors)
4. **Add Loading States** (Show spinners during API calls)
5. **Test Thoroughly** (All CRUD operations)

---

## 🎯 Current Task

**I will now update the backend to match the frontend exactly.**

This includes:
1. Updating existing models
2. Creating missing entities
3. Adding new endpoints
4. Ensuring data structures match 100%

**Ready to proceed?**
