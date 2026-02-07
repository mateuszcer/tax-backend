# 🎬 Demo Commands - Copy & Paste Ready

## Before Demo (Setup on EC2)

```bash
# 1. Install dependencies
sudo apt update
sudo apt install -y docker.io docker-compose git
sudo systemctl start docker
sudo usermod -aG docker ubuntu

# 2. Clone repo
git clone <your-repo-url>
cd tax-backend

# 3. Generate secure JWT secret
export JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=$JWT_SECRET" > .env
echo "SPRING_PROFILES_ACTIVE=simple-auth" >> .env

# 4. Start application
docker-compose up -d

# 5. Wait for startup (30-60 seconds)
docker-compose logs -f spring-boot-app
# Wait for: "Started TaxBackendApplication"

# 6. Test health
curl http://localhost:8080/actuator/health
```

---

## During Demo (Live Commands)

### 1. Show Health Check
```bash
curl http://YOUR_EC2_IP:8080/actuator/health
```
**Expected:** `{"status":"UP"}`

---

### 2. Create Demo Account
```bash
curl -X POST http://YOUR_EC2_IP:8080/auth/signUp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "investor@demo.com",
    "password": "SecureDemo123!"
  }'
```

**Expected:**
```json
{
  "success": true,
  "message": "User registered successfully...",
  "timestamp": "2026-02-07T..."
}
```

---

### 3. Login (Immediately - No Confirmation!)
```bash
curl -X POST http://YOUR_EC2_IP:8080/auth/signIn \
  -H "Content-Type: application/json" \
  -d '{
    "email": "investor@demo.com",
    "password": "SecureDemo123!"
  }'
```

**Expected:**
```json
{
  "success": true,
  "data": {
    "idToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOi...",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOi..."
  },
  "message": "User signed in successfully",
  "timestamp": "2026-02-07T..."
}
```

**Copy the idToken for next step!**

---

### 4. Access Protected Endpoint
```bash
# Replace YOUR_TOKEN with the idToken from previous response
curl http://YOUR_EC2_IP:8080/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:**
```json
{
  "success": true,
  "data": [],
  "message": "Orders retrieved successfully",
  "timestamp": "2026-02-07T..."
}
```

---

### 5. Show Invalid Credentials Protection
```bash
curl -X POST http://YOUR_EC2_IP:8080/auth/signIn \
  -H "Content-Type: application/json" \
  -d '{
    "email": "investor@demo.com",
    "password": "WrongPassword!"
  }'
```

**Expected:**
```json
{
  "success": false,
  "message": "Invalid email or password",
  "errorCode": "AUTHENTICATION_FAILED",
  "timestamp": "2026-02-07T..."
}
```

---

### 6. Show Duplicate Email Protection
```bash
curl -X POST http://YOUR_EC2_IP:8080/auth/signUp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "investor@demo.com",
    "password": "AnotherPassword123!"
  }'
```

**Expected:**
```json
{
  "success": false,
  "message": "Email already registered",
  "errorCode": "AUTHENTICATION_FAILED",
  "timestamp": "2026-02-07T..."
}
```

---

## Quick Test All Features

```bash
# Run automated test script
./test-auth.sh http://YOUR_EC2_IP:8080
```

This will test:
- ✅ Health check
- ✅ Sign up
- ✅ Sign in
- ✅ JWT token generation
- ✅ Protected endpoints
- ✅ Invalid credentials rejection
- ✅ Duplicate email rejection

---

## Troubleshooting During Demo

### Check if app is running
```bash
docker-compose ps
```

### Check logs
```bash
docker-compose logs -f spring-boot-app
```

### Restart if needed
```bash
docker-compose restart spring-boot-app
```

### Check database
```bash
docker-compose exec db psql -U postgres -d taxooldb -c "SELECT email, created_at FROM users;"
```

---

## Frontend Demo Commands

### Update Frontend Config
```typescript
// In your frontend .env or config
API_BASE_URL=http://YOUR_EC2_IP:8080
```

### Test Login from Frontend
```typescript
// 1. Sign up
const signUpResponse = await axios.post(`${API_BASE_URL}/auth/signUp`, {
  email: 'investor@demo.com',
  password: 'SecureDemo123!'
});

// 2. Sign in (immediately!)
const signInResponse = await axios.post(`${API_BASE_URL}/auth/signIn`, {
  email: 'investor@demo.com',
  password: 'SecureDemo123!'
});

const { idToken } = signInResponse.data.data;

// 3. Use token
axios.defaults.headers.common['Authorization'] = `Bearer ${idToken}`;

// 4. Call protected endpoint
const ordersResponse = await axios.get(`${API_BASE_URL}/api/orders`);
console.log(ordersResponse.data);
```

---

## Key Talking Points for Investors

1. **"We have full authentication with JWT tokens"**
   - Show sign up/sign in flow
   - Show token generation
   - Show protected endpoints

2. **"Security is built-in from day one"**
   - BCrypt password hashing
   - JWT token validation
   - 24-hour token expiry
   - Security headers (HSTS, X-Frame-Options)

3. **"Architecture is cloud-ready"**
   - Currently using simple-auth for demo
   - Easy migration to AWS Cognito later
   - Same API, just change profile
   - No frontend changes needed

4. **"Cost-effective during early stage"**
   - $5-10/month instead of $100+
   - PostgreSQL instead of Cognito
   - Can scale to Cognito when funded

---

## Emergency Contacts

- **If app crashes:** `docker-compose restart`
- **If database issues:** `docker-compose down -v && docker-compose up -d`
- **If token issues:** Check JWT_SECRET is set
- **If CORS issues:** Update CORS_ORIGINS in .env

---

## Pre-Demo Checklist

- [ ] App running: `docker-compose ps`
- [ ] Health check: `curl localhost:8080/actuator/health`
- [ ] Test script passes: `./test-auth.sh`
- [ ] Demo user created: investor@demo.com
- [ ] Frontend connected to EC2 IP
- [ ] Have EC2 IP written down
- [ ] Have demo credentials written down
- [ ] Browser tabs ready:
  - [ ] Swagger UI: `http://YOUR_EC2_IP:8080/swagger-ui.html`
  - [ ] Frontend URL
  - [ ] Terminal with curl commands

---

## Demo Flow (30 seconds)

1. **Show health:** `curl .../actuator/health` → UP ✅
2. **Create account:** Sign up via frontend or curl
3. **Login instantly:** No email confirmation needed
4. **Show token:** Point out JWT in response
5. **Access data:** Call protected endpoint with token
6. **Show security:** Try wrong password → rejected

**Done! Professional auth demo in 30 seconds.** 🎉

---

## Post-Demo

```bash
# Create investor user for continued access
curl -X POST http://YOUR_EC2_IP:8080/auth/signUp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "investor1@example.com",
    "password": "InvestorAccess123!"
  }'
```

Give them:
- Frontend URL
- Email: investor1@example.com
- Password: InvestorAccess123!
- Let them play with the system!

---

Good luck! 🚀
