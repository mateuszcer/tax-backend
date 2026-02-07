# Development Authentication

## Overview

In development mode, the application uses a **mock authentication service** that accepts any email/password combination and returns fake JWT tokens. This eliminates the need for AWS Cognito in local development.

## Architecture

```
┌─────────────────────────────────────────────────┐
│              AuthController                      │
│              (Application Layer)                 │
└───────────────────┬─────────────────────────────┘
                    │
                    │ uses
                    ▼
┌─────────────────────────────────────────────────┐
│              AuthService                         │
│              (Domain Interface)                  │
└─────────┬──────────────────────┬────────────────┘
          │                      │
          │ implements           │ implements
          ▼                      ▼
┌──────────────────────┐  ┌─────────────────────┐
│  DevAuthService      │  │ CognitoAuthService  │
│  (@Profile "dev")    │  │ (@Profile "!dev")   │
│                      │  │                      │
│  - Any password OK   │  │ - Real AWS Cognito  │
│  - Fake JWT tokens   │  │ - Real JWT tokens   │
└──────────────────────┘  └─────────────────────┘
```

### DDD Principles

✅ **Domain Interface** - `AuthService` defines authentication contract  
✅ **Multiple Implementations** - Dev, Test, and Production adapters  
✅ **Profile-Based Selection** - Spring automatically picks the right implementation  
✅ **No Production Impact** - Dev code never runs in production  

## How It Works

### Development Mode (Profile: `dev`)

**Service:** `DevAuthService`

```java
@Service
@Profile("dev")
public class DevAuthService implements AuthService {
    // Accepts ANY email/password
    // Returns fake JWT tokens
}
```

**Features:**
- ✅ Accepts any email/password combination
- ✅ Returns fake but valid-looking JWT tokens
- ✅ No AWS Cognito connection needed
- ✅ No real user database
- ✅ Instant login (no network calls)

### Production Mode (Profile: `prod`)

**Service:** `CognitoAuthService`

```java
@Service
@Profile("!dev & !test")
public class CognitoAuthService implements AuthService {
    // Uses real AWS Cognito
}
```

**Features:**
- ✅ Real AWS Cognito authentication
- ✅ Real JWT tokens with proper claims
- ✅ Full security validation
- ✅ User pool management

### Test Mode (Profile: `test`)

**Service:** `TestAuthService`

```java
@Service
@Profile("test")
public class TestAuthService implements AuthService {
    // Similar to dev, but for tests
}
```

## Usage Examples

### 1. Sign Up (Dev Mode)

```bash
curl -X POST http://localhost:8080/auth/signUp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "any@email.com",
    "password": "any_password_works"
  }'

# Response:
{
  "success": true,
  "message": "User registered successfully. Please check your email for confirmation code."
}
```

**Dev Behavior:** Auto-approves signup, no email sent

### 2. Confirm Sign Up (Dev Mode)

```bash
curl -X POST http://localhost:8080/auth/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "email": "any@email.com",
    "code": "any_code"
  }'

# Response:
{
  "success": true,
  "message": "User confirmed successfully. You can now sign in."
}
```

**Dev Behavior:** Auto-confirms, any code works

### 3. Sign In (Dev Mode)

```bash
curl -X POST http://localhost:8080/auth/signIn \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "literally_anything"
  }'

# Response:
{
  "success": true,
  "data": {
    "idToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZXZfYWJjZDEyMzQiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJpYXQiOjE3MDY2NDAwMDAsImV4cCI6MTcwNjcyNjQwMH0.fake-signature",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "message": "User signed in successfully"
}
```

**Dev Behavior:** 
- ✅ Any email works
- ✅ Any password works
- ✅ Returns fake JWT with `sub` claim
- ✅ Token contains user email

## Fake JWT Structure

Dev mode generates JWT-like tokens with this structure:

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "dev_abc123456789",    // Fake user ID
    "email": "user@example.com",   // Your provided email
    "iat": 1706640000,             // Current timestamp
    "exp": 1706726400              // 24h expiry
  },
  "signature": "fake-signature-abc123"
}
```

**Note:** These tokens work with `AuthUserIdResolver` which extracts the `sub` claim for `@AuthUserId` parameters.

## Frontend Integration

### Development

```typescript
// In dev mode, ANY credentials work!
const response = await axios.post('http://localhost:8080/auth/signIn', {
  email: 'dev@test.com',
  password: 'password123'  // Or literally anything
});

const { idToken, accessToken } = response.data.data;

// Store tokens
localStorage.setItem('idToken', idToken);
localStorage.setItem('accessToken', accessToken);

// Use in requests
axios.defaults.headers.common['Authorization'] = `Bearer ${idToken}`;
```

### Production

```typescript
// In prod, real Cognito validation
const response = await axios.post('https://api.taxool.com/auth/signIn', {
  email: 'real@user.com',
  password: 'SecurePassword123!'
});

// Same response structure, but real tokens
```

## Logs

Dev mode logs all auth operations with 🔓 emoji for easy identification:

```
🔓 DEV MODE: Auto-approving login for email: test@example.com
🔓 DEV MODE: Password accepted (any password works in dev)
✅ DEV MODE: Generated fake token for test@example.com

🔓 DEV MODE: Auto-approved signup for email: new@user.com
✅ DEV MODE: User new@user.com registered (no real AWS call)

🔓 DEV MODE: Auto-confirmed signup for email: new@user.com with code: 123456
✅ DEV MODE: User new@user.com confirmed (no real AWS call)
```

## Security Notes

### ⚠️ IMPORTANT

- **NEVER deploy dev mode to production!**
- Dev mode is controlled by `@Profile("dev")` - it cannot run in prod
- Fake tokens are not cryptographically secure
- No password hashing or validation in dev mode

### Profile Safety

Production deployment checklist:
- ✅ `SPRING_PROFILES_ACTIVE=prod` (NOT dev)
- ✅ Real AWS Cognito configured
- ✅ Real JWT validation enabled
- ✅ `CognitoAuthService` active (not `DevAuthService`)

## Switching Between Modes

### Run in Dev Mode

```bash
# Using Spring Boot
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Using JAR
java -jar -Dspring.profiles.active=dev target/tax-backend.jar

# Using Docker Compose
# Already configured in docker-compose.yml:
# environment:
#   - SPRING_PROFILES_ACTIVE=dev
docker-compose up
```

### Run in Prod Mode

```bash
# Using environment variable
export SPRING_PROFILES_ACTIVE=prod
./mvnw spring-boot:run

# Using system property
java -jar -Dspring.profiles.active=prod target/tax-backend.jar
```

## Testing

All auth endpoints work exactly the same in dev mode:

```bash
# Sign up
curl -X POST http://localhost:8080/auth/signUp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123"}'

# Confirm (any code)
curl -X POST http://localhost:8080/auth/confirm \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","code":"000000"}'

# Sign in (any password)
curl -X POST http://localhost:8080/auth/signIn \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"anything"}'

# Use token in protected endpoints
TOKEN="<idToken from sign in>"
curl http://localhost:8080/api/broker/coinbase/orders \
  -H "Authorization: Bearer $TOKEN"
```

## Troubleshooting

### "Authentication failed" in Dev Mode

**Cause:** Wrong profile active

**Solution:**
```bash
# Check active profile
curl http://localhost:8080/actuator/info

# Ensure dev profile is active in application-dev.properties
spring.profiles.active=dev
```

### Logs Show Cognito Errors in Dev

**Cause:** `CognitoAuthService` is active instead of `DevAuthService`

**Solution:**
```bash
# Verify profile
echo $SPRING_PROFILES_ACTIVE  # Should be "dev"

# Check which service is loaded
curl http://localhost:8080/actuator/beans | grep AuthService
# Should show: DevAuthService (not CognitoAuthService)
```

### Frontend Gets 401 Unauthorized

**Cause:** Token not included in request headers

**Solution:**
```typescript
// Include token in Authorization header
axios.defaults.headers.common['Authorization'] = `Bearer ${idToken}`;

// Or per-request
axios.get('/api/endpoint', {
  headers: { Authorization: `Bearer ${idToken}` }
});
```

## Benefits

✅ **Fast Development** - No AWS setup needed  
✅ **Offline Work** - No internet required  
✅ **Easy Testing** - Any credentials work  
✅ **Clean Architecture** - DDD principles maintained  
✅ **Safe Production** - Dev code never runs in prod  
✅ **Consistent API** - Same endpoints in dev/prod  

## Related Files

- `AuthService.java` - Domain interface
- `DevAuthService.java` - Dev implementation
- `CognitoAuthService.java` - Prod implementation
- `TestAuthService.java` - Test implementation
- `AuthController.java` - REST endpoints
- `DevSecurityConfiguration.java` - Dev security config
