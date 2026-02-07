# Simple Auth Deployment Guide

## For Investor Demo / Small Deployments (No Cloud Costs)

This guide shows how to deploy the application with **simple-auth** profile - JWT authentication using PostgreSQL, **no AWS Cognito needed**.

---

## ✅ What You Get

- ✅ **Real authentication** with email/password (looks professional for investors)
- ✅ **JWT tokens** compatible with your existing frontend
- ✅ **Zero cloud costs** - just EC2 + PostgreSQL
- ✅ **Same API endpoints** - frontend code doesn't change
- ✅ **Auto-confirmed users** - no email verification needed

---

## 🚀 Quick Deployment on EC2

### 1. SSH into your EC2 instance

```bash
ssh -i your-key.pem ubuntu@your-ec2-ip
```

### 2. Install Docker & Docker Compose

```bash
sudo apt update
sudo apt install -y docker.io docker-compose git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
# Log out and back in for group changes
```

### 3. Clone your repository

```bash
git clone https://github.com/your-username/tax-backend.git
cd tax-backend
```

### 4. Set environment variables

```bash
# Create .env file
cat > .env << 'EOF'
SPRING_PROFILES_ACTIVE=simple-auth
JWT_SECRET=change-this-to-random-32-char-string-for-production
DB_HOST=db
DB_PORT=5432
DB_NAME=taxooldb
DB_USERNAME=postgres
DB_PASSWORD=your-secure-password
CORS_ORIGINS=https://your-frontend-domain.com,http://localhost:3000
FRONTEND_URL=https://your-frontend-domain.com
EOF
```

### 5. Build and run

```bash
# Build the Docker image
docker-compose build

# Start the services
docker-compose up -d

# Check logs
docker-compose logs -f spring-boot-app
```

### 6. Verify it's running

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

---

## 🔑 Testing Authentication

### Sign Up a Test User

```bash
curl -X POST http://your-ec2-ip:8080/auth/signUp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "investor@demo.com",
    "password": "DemoPassword123!"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully. Please check your email for confirmation code.",
  "timestamp": "2026-02-07T..."
}
```

### Sign In (No Confirmation Needed!)

```bash
curl -X POST http://your-ec2-ip:8080/auth/signIn \
  -H "Content-Type: application/json" \
  -d '{
    "email": "investor@demo.com",
    "password": "DemoPassword123!"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "idToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4NmY3ZTIxMS0uLi4",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4NmY3ZTIxMS0uLi4"
  },
  "message": "User signed in successfully",
  "timestamp": "2026-02-07T..."
}
```

### Use Token in Protected Endpoint

```bash
TOKEN="<idToken from sign in response>"

curl http://your-ec2-ip:8080/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🌐 Frontend Configuration

Your frontend doesn't need to change! Just update the API base URL:

```typescript
// Example: axios config
const API_BASE_URL = 'http://your-ec2-ip:8080';

// Sign up
await axios.post(`${API_BASE_URL}/auth/signUp`, {
  email: 'user@example.com',
  password: 'SecurePassword123!'
});

// Sign in (no confirmation needed!)
const response = await axios.post(`${API_BASE_URL}/auth/signIn`, {
  email: 'user@example.com',
  password: 'SecurePassword123!'
});

const { idToken, accessToken } = response.data.data;

// Use token in requests
axios.defaults.headers.common['Authorization'] = `Bearer ${idToken}`;
```

---

## 🔧 Configuration Options

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | - | **Must be `simple-auth`** |
| `JWT_SECRET` | (default) | Secret key for JWT signing (32+ chars) |
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | taxooldb | Database name |
| `DB_USERNAME` | postgres | Database username |
| `DB_PASSWORD` | password | Database password |
| `CORS_ORIGINS` | localhost:3000 | Allowed CORS origins (comma-separated) |
| `FRONTEND_URL` | localhost:3000 | Frontend URL for redirects |

### Generate Secure JWT Secret

```bash
# Generate random 32-character secret
openssl rand -base64 32
```

---

## 🔒 Security Checklist for Demo

- ✅ Change default `JWT_SECRET` to random string
- ✅ Change default database password
- ✅ Update `CORS_ORIGINS` to your frontend domain
- ✅ Enable HTTPS if using a domain (Let's Encrypt)
- ✅ Configure EC2 security group (allow 80, 443, 22 only)

---

## 📊 Database Migrations

Flyway migrations run automatically on startup. The `users` table will be created with:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) UNIQUE NOT NULL,
    confirmed BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🐛 Troubleshooting

### "Connection refused" to database

```bash
# Check if db container is running
docker-compose ps

# Check db logs
docker-compose logs db

# Restart services
docker-compose restart
```

### "Invalid credentials" even with correct password

```bash
# Check which profile is active
docker-compose logs spring-boot-app | grep "Active profile"

# Should see: "The following profiles are active: simple-auth"
```

### JWT token not working

```bash
# Check JWT_SECRET is set
docker-compose exec spring-boot-app env | grep JWT_SECRET

# Restart to apply changes
docker-compose restart spring-boot-app
```

### Database migration failed

```bash
# Connect to database and check migrations
docker-compose exec db psql -U postgres -d taxooldb

# Check migration history
SELECT * FROM flyway_schema_history;

# If needed, drop database and restart (DEV ONLY!)
docker-compose down -v
docker-compose up -d
```

---

## 🚀 Upgrading to Cognito Later

When you have budget for AWS Cognito:

1. Update environment variable: `SPRING_PROFILES_ACTIVE=prod`
2. Set Cognito environment variables
3. Restart application
4. **Frontend doesn't change!** Same endpoints, same response format

---

## 📝 Differences from Dev Profile

| Feature | dev | simple-auth |
|---------|-----|-------------|
| Any password works | ✅ | ❌ |
| Database required | ❌ | ✅ |
| Real JWT tokens | ❌ | ✅ |
| Password hashing | ❌ | ✅ (BCrypt) |
| User persistence | ❌ | ✅ (PostgreSQL) |
| Security headers | ❌ | ✅ |
| For investors | ❌ | ✅ |

---

## 💰 Cost Comparison

| Solution | Monthly Cost | Setup Time |
|----------|--------------|------------|
| **simple-auth** | **$5-10** (EC2 t2.micro) | **15 min** |
| Cognito + ECS | $50-100+ | 2-4 hours |
| Cognito + Fargate | $100-200+ | 3-6 hours |

---

## ✅ Ready for Demo!

Your application is now running with professional-looking authentication that works exactly like your Cognito setup would, but without the cloud costs. Perfect for investor demos!

**Test user for demo:**
- Email: `investor@demo.com`
- Password: `DemoPassword123!`

Good luck with your demo tomorrow! 🚀
