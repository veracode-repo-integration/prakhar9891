# MySQL Database Setup Guide

## 🎯 Goal: Multi-User Access with Centralized Database

This guide helps you set up MySQL for your POS application where multiple users on different devices can access the same data.

---

## ✅ Changes Made

1. **Updated `pom.xml`**: Replaced H2 with MySQL connector
2. **Updated `application.properties`**: Configured MySQL connection with environment variable support
3. **Created `.env.example`**: Template for database credentials

---

## 📋 Setup Options

### **Option 1: Free Cloud MySQL (RECOMMENDED for Multi-User)** 🌐

#### A. Using Aiven (Easiest - No Credit Card)

1. **Sign up**: Go to https://aiven.io/
2. **Create MySQL Service**:
   - Choose "MySQL"
   - Select "Free Plan" (1GB storage)
   - Choose region closest to you
   - Click "Create Service"
3. **Get Connection Details**:
   - Host: `your-service-name.aivencloud.com`
   - Port: `12345` (example)
   - User: `avnadmin`
   - Password: (shown in dashboard)
   - Database: `defaultdb`

4. **Update Your Application**:
   ```bash
   # Create .env file
   cp .env.example .env
   ```
   
   Edit `.env`:
   ```properties
   MYSQL_URL=jdbc:mysql://your-service.aivencloud.com:12345/defaultdb?useSSL=true
   MYSQL_USER=avnadmin
   MYSQL_PASSWORD=your_password_from_aiven
   ```

#### B. Using Railway.app

1. **Sign up**: Go to https://railway.app/
2. **Create New Project** → **Provision MySQL**
3. **Get Connection Details** from "Variables" tab
4. **Update `.env`** with Railway credentials

---

### **Option 2: Local MySQL (For Testing Only)** 🏠

**Note**: Your laptop must be running 24/7 for others to access.

#### Step 1: Create Database

```bash
# Login to MySQL
mysql -u root -p

# Create database
CREATE DATABASE posdb;

# Create user for remote access (optional)
CREATE USER 'posuser'@'%' IDENTIFIED BY 'SecurePassword123!';
GRANT ALL PRIVILEGES ON posdb.* TO 'posuser'@'%';
FLUSH PRIVILEGES;
EXIT;
```

#### Step 2: Configure MySQL for Remote Access

Edit MySQL config file:
- Windows: `C:\ProgramData\MySQL\MySQL Server 9.2\my.ini`

Add/Update:
```ini
[mysqld]
bind-address = 0.0.0.0
```

Restart MySQL service:
```powershell
Restart-Service MySQL82
```

#### Step 3: Configure Firewall

```powershell
# Allow MySQL port
New-NetFirewallRule -DisplayName "MySQL" -Direction Inbound -Protocol TCP -LocalPort 3306 -Action Allow
```

#### Step 4: Update Application

Create `.env`:
```properties
MYSQL_URL=jdbc:mysql://localhost:3306/posdb?createDatabaseIfNotExist=true&useSSL=false
MYSQL_USER=root
MYSQL_PASSWORD=your_mysql_password
```

---

## 🚀 Running the Application

### Method 1: Using Maven (Recommended)

```bash
# Install dependencies
mvn clean install

# Run with environment variables
mvn spring-boot:run
```

### Method 2: Using JAR

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/server-0.0.1-SNAPSHOT.jar
```

---

## 🔐 Security Best Practices

1. **Never commit `.env` file** (already in `.gitignore`)
2. **Use strong passwords** for database
3. **Enable SSL** for cloud databases
4. **Limit database user permissions** to only what's needed
5. **Use environment variables** in production

---

## 🧪 Testing the Setup

1. **Start the application**
2. **Check logs** for successful database connection:
   ```
   HikariPool-1 - Start completed.
   ```
3. **Test API**:
   ```bash
   curl http://localhost:4000/api/register -X POST -H "Content-Type: application/json" -d '{"username":"admin","passwordHash":"admin123","firstName":"Admin","lastName":"User","email":"admin@pos.com","role":"admin"}'
   ```

---

## 📱 Accessing from Multiple Devices

### For Cloud Database (Aiven/Railway):
- ✅ **No additional setup needed**
- All devices connect to your deployed backend
- Backend connects to cloud MySQL

### For Local MySQL:
1. **Get your public IP**: https://whatismyipaddress.com/
2. **Configure port forwarding** on router (port 3306)
3. **Use Dynamic DNS** (optional): https://www.noip.com/
4. **Share connection details** with other users

---

## 🆘 Troubleshooting

### Error: "Access denied for user"
- Check username/password in `.env`
- Verify user has correct permissions

### Error: "Communications link failure"
- Check if MySQL is running
- Verify host and port are correct
- Check firewall settings

### Error: "Unknown database"
- Add `?createDatabaseIfNotExist=true` to MYSQL_URL
- Or manually create database

---

## 📊 Next Steps

1. Choose your database option (Cloud recommended)
2. Set up the database
3. Update `.env` file
4. Run the application
5. Test with multiple devices

**Need help?** Let me know which option you choose and I'll guide you through the specific steps!
