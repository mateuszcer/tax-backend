# 🚀 Quick Start - Tomorrow's Demo

## Option 1: Local Testing (5 minutes)

```bash
# 1. Build dependencies
./gradlew build -x test

# 2. Start with docker-compose (already configured for simple-auth)
docker-compose up --build -d

# 3. Check logs
docker-compose logs -f spring-boot-app

# Wait for: "Started TaxBackendApplication"
```

## Option 2: EC2 Deployment (15 minutes)

```bash
# On EC2 instance
sudo apt update && sudo apt install -y docker.io docker-compose git
sudo systemctl start docker
sudo usermod -aG docker ubuntu

# Clone and run
git clone <your-repo>
cd tax-backend

# Change JWT secret (important!)
export JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=$JWT_SECRET" >> .env

# Start
docker-compose up -d

# Check
curl http://localhost:8080/actuator/health
```

## Test Authentication

```bash
# 1. Sign up
curl -X POST http://localhost:8080/auth/signUp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@demo.com","password":"Test123!"}'

# 2. Sign in (immediately - no confirmation needed!)
curl -X POST http://localhost:8080/auth/signIn \
  -H "Content-Type: application/json" \
  -d '{"email":"test@demo.com","password":"Test123!"}'

# 3. Copy the idToken from response

# 4. Test protected endpoint
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_ID_TOKEN_HERE"
```

## Frontend Update

**Only change the API base URL:**

```typescript
// Change from Cognito endpoint to your EC2
const API_BASE_URL = 'http://your-ec2-ip:8080';

// Everything else stays the same!
```

## Current Configuration

✅ Profile: `simple-auth` (in docker-compose.yml line 25)  
✅ Auto-confirmed users (no email verification)  
✅ PostgreSQL for user storage  
✅ JWT tokens (24h expiry)  
✅ BCrypt password hashing  
✅ Same API as Cognito  

## Troubleshooting

**Can't connect to database?**
```bash
docker-compose logs db
docker-compose restart
```

**App won't start?**
```bash
docker-compose logs spring-boot-app
# Check for profile: should see "simple-auth"
```

**JWT secret error?**
```bash
# Make sure it's at least 32 characters
docker-compose exec spring-boot-app env | grep JWT_SECRET
```

## Ready? ✅

Your app is now running with real authentication, zero cloud costs, and the same API your frontend expects!

---

**See DEPLOYMENT_SIMPLE_AUTH.md for detailed production deployment guide.**
