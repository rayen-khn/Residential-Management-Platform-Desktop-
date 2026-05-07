# Syndicati Java - Security Implementation Guide

Comprehensive security measures implemented to match the web version (Symfony).

---

## 1. **Rate Limiting & Brute Force Protection**

**File**: `LoginRateLimiter.java`

### Features:
- Max 5 failed login attempts per 15 minutes
- Account lockout after exceeding limit
- Automatic unlock after 15-minute window
- Clear attempts on successful login

### Usage in AuthController:
```java
// Check if user is rate-limited
if (LoginRateLimiter.isRateLimited(email)) {
    long secondsRemaining = LoginRateLimiter.getLockoutTimeRemaining(email);
    return AuthResult.failure("Too many attempts. Try again in " + secondsRemaining + " seconds");
}

// Record failed attempt
LoginRateLimiter.recordFailedAttempt(email);

// Clear on successful login
LoginRateLimiter.clearAttempts(email);
```

### Configuration:
```
MAX_ATTEMPTS: 5
LOCKOUT_MINUTES: 15
```

---

## 2. **Two-Factor Authentication (2FA/MFA)**

### Email OTP Flow:
1. User enables Email 2FA
2. On login, 6-digit code sent to email
3. User enters code within 15 minutes
4. Session granted after verification

### TOTP (Authenticator App):
- Google Authenticator compatible
- Authy compatible
- Microsoft Authenticator compatible
- Base32-encoded secrets
- QR code generation for setup

**File**: `TOTPService.java`

### Methods:
```java
// Generate new secret
String secret = TOTPService.generateSecret();

// Get QR code URI
String uri = TOTPService.generateOTPAuthURI(email, secret);

// Verify code
boolean valid = TOTPService.verifyCode(secret, "123456");

// With custom leeway (±30 seconds per step)
boolean valid = TOTPService.verifyCode(secret, "123456", 1);
```

### Database Fields (User Entity):
```
twoFactorEnabled: boolean (default false)
totpSecret: string (Base32 encoded)
authCode: string (6-digit OTP)
authCodeExpiresAt: datetime (15-minute expiry)
```

---

## 3. **Account Verification by Admin**

**Field**: `isVerified` (boolean, default false)

### Rules:
- New signups create unverified accounts
- Unverified users CANNOT login
- Admin sets `isVerified = true` to enable login
- OAuth users auto-verified on signup

### Error Message:
```
"Your account is not yet verified by an administrator. 
You cannot log in until an admin verifies your account."
```

---

## 4. **Account Disabling**

**Fields**:
- `isDisabled`: boolean
- `disabledAt`: datetime
- `disabledReason`: string

### Rules:
- Disabled accounts cannot login
- Reason displayed to user

---

## 5. **CSRF Token Protection**

**File**: `CSRFTokenManager.java`

### Features:
- Stateless CSRF tokens
- Per-session tokens
- Constant-time comparison (timing attack resistant)
- Auto-generated Base64 tokens

### Usage:
```java
// Generate token for session
String token = CSRFTokenManager.generateToken(sessionId);

// Validate token from header
boolean valid = CSRFTokenManager.validateToken(sessionId, headerToken);

// Clear on logout
CSRFTokenManager.clearToken(sessionId);
```

### Implementation in Frontend:
```javascript
// Add CSRF token to all POST/PUT/DELETE requests
fetch('/api/endpoint', {
    method: 'POST',
    headers: {
        'X-CSRF-Token': csrfToken
    }
});
```

---

## 6. **Password Security**

### Hashing:
- bcrypt (cost 10, ~100ms per hash)
- Support for legacy plaintext passwords
- Automatic migration on login

### Validation Rules (Signup):
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 special character: `!@#$%^&*(),.?":{}|<>`
- Confirmation must match

### Password Reset:
- 12-character random password generated
- 6-digit verification code (15-min expiry)
- User enumeration protection (generic error)

---

## 7. **Role-Based Access Control**

**Roles**:
- `RESIDENT`: Default role for new users
- `SYNDIC`: Syndicate member
- `OWNER`: Property owner
- `ADMIN`: Administrator
- `SUPERADMIN`: Super administrator

### Protected Routes:
```
/admin/* → requires ADMIN, SYNDIC, OWNER, or SUPERADMIN
/profile/* → requires login verification
```

---

## 8. **Session Security**

### Configuration:
```
Session Timeout: 24 minutes
Cookie Secure: auto (HTTPS in production)
SameSite: lax (CSRF protection)
HttpOnly: true (XSS protection)
```

### Session Data:
```java
{
    'is_logged_in': true,
    'user': {
        'id': userId,
        'name': firstName + lastName,
        'email': emailUser,
        'role': roleUser,
        'avatar': profileImagePath,
        'settings': userSettings
    },
    '2fa_user_id': userId,  // During 2FA
    '2fa_email': email      // During 2FA
}
```

---

## 9. **Login Flow with All Security Measures**

```
1. User enters email + password
   ↓
2. Check rate limiting
   ├─ If locked: return "Too many attempts"
   └─ If allowed: continue
   ↓
3. Find user by email
   ├─ If not found: record attempt + return generic error
   ├─ If disabled: return "Account disabled"
   ├─ If not verified: return "Awaiting admin verification"
   ↓
4. Verify password (bcrypt)
   ├─ If wrong: record attempt + show attempts remaining
   └─ If correct: clear attempts
   ↓
5. Check 2FA status
   ├─ If 2FA enabled: return MFA_REQUIRED
   │   └─ User verifies code separately
   └─ If no 2FA: full access granted
   ↓
6. Session created with user data
   ↓
7. User logged in ✓
```

---

## 10. **Email Verification Codes (AuthCode)**

**Purpose**: Used for verification in multiple flows
- Password reset
- Email 2FA
- Account recovery

### Features:
- 6-digit code
- 15-minute expiration
- Stored in `authCode` field
- Expiry in `authCodeExpiresAt` field

---

## 11. **OAuth 2.0 Security (Google)**

**Planned Implementation**:
- Google OAuth 2.0 integration
- Secure redirect URIs
- Token management
- Auto-verification for OAuth users
- Email account linking

---

## 12. **Security Best Practices Implemented**

✓ Constant-time comparison for tokens  
✓ Brute force rate limiting  
✓ Account lockout mechanism  
✓ CSRF token protection  
✓ Secure password hashing (bcrypt)  
✓ Account verification requirement  
✓ Account disabling capability  
✓ Admin approval workflow  
✓ 2FA/MFA support (Email OTP + TOTP)  
✓ Generic error messages for failed login  
✓ Session timeout (24 minutes)  
✓ Secure cookie flags  
✓ Role-based access control  

---

## 13. **Usage in LoginView**

### Check Login Result:
```java
AuthController controller = new AuthController();
AuthResult result = controller.login(email, password);

if (!result.isSuccess()) {
    showError(result.getMessage());
    return;
}

if (result.isMfaRequired()) {
    showMFAVerificationScreen(result.getUser());
    return;
}

// User logged in
User user = result.getUser();
SessionManager.getInstance().setUser(user);
```

### Setup TOTP:
```java
// Generate secret
AuthController.TOTPSetupResult setup = controller.setupTOTP(user);
String secret = setup.getSecret();
String qrUri = setup.getOtpUri();

// Display QR code to user (convert URI to QR)
// User scans with authenticator app

// Verify and enable after user confirms
AuthResult verify = controller.enableTOTP(user, userConfirmedSecret);
```

### 2FA Verification:
```java
// Verify email OTP
AuthResult result = controller.verifyLoginOtp(email, "123456");
if (result.isSuccess()) {
    // Grant full access
    SessionManager.getInstance().setUser(result.getUser());
}

// Or verify TOTP
AuthResult result = controller.verifyTOTPCode(user, "123456");
if (result.isSuccess()) {
    // Grant full access
}
```

---

## 14. **Configuration Summary**

| Setting | Value | Matches Web |
|---------|-------|---|
| Max Login Attempts | 5 | ✓ |
| Lockout Duration | 15 minutes | ✓ |
| 2FA Code Duration | 15 minutes | ✓ |
| Password Min Length | 8 | ✓ |
| TOTP Time Step | 30 seconds | ✓ |
| TOTP Leeway | 1 step | ✓ |
| Session Timeout | 24 minutes | ✓ |
| CSRF Token Length | 32 bytes | ✓ |
| Password Reset Code | 6 digits | ✓ |

---

## 15. **Security Headers (Recommended for Frontend)**

```
X-CSRF-Token: [token]
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

---

## Testing Security Features

### Rate Limiting Test:
```
1. Attempt login 5 times with wrong password
2. 6th attempt should return "Too many attempts"
3. Wait 15 minutes (or code can reset manually)
4. Should be able to login again
```

### 2FA Test:
```
1. Enable 2FA for test account
2. Login with correct credentials
3. Should see "2FA required" message
4. Enter code sent to email → success
```

### TOTP Test:
```
1. Generate TOTP secret
2. Scan QR code with Google Authenticator
3. Enter code from app → should work
4. Expired codes should fail
```

---

## Next Steps After Implementation

1. **Implement in UI**: Update LoginView to handle MFA flows
2. **Admin Panel**: Add verification/disable controls
3. **User Settings**: Add 2FA management page
4. **Email Templates**: Verify codes look professional
5. **Logging**: Log all authentication attempts
6. **Monitoring**: Alert on suspicious activity

---

**Version**: 1.0  
**Last Updated**: April 1, 2026  
**Matches**: Web Version (Symfony) Security Implementation
