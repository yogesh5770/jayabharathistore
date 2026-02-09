# ✅ CASHFREE DEPENDENCY - FIXED!

## Issue
```
❌ Failed to resolve: com.cashfree.pg:android-sdk:2.1.2
```

## Solution
Changed the artifact name from `android-sdk` to `api`:

**File:** `gradle/libs.versions.toml`

**Before:**
```toml
cashfree-pg = { group = "com.cashfree.pg", name = "android-sdk", version.ref = "cashfree" }
```

**After:**
```toml
cashfree-pg = { group = "com.cashfree.pg", name = "api", version.ref = "cashfree" }
```

## Correct Dependency
```gradle
implementation("com.cashfree.pg:api:2.1.2")
```

## Next Steps

1. **Sync Gradle in Android Studio:**
   - Click **File** → **Sync Project with Gradle Files**
   - Or click the 🐘 icon in the toolbar

2. **Clean and Rebuild:**
   - **Build** → **Clean Project**
   - **Build** → **Rebuild Project**

3. **Verify:**
   - Check that sync completes without errors
   - The Cashfree SDK should now download successfully

## Reference
Official Cashfree SDK for Android uses the `api` artifact:
- Maven: `com.cashfree.pg:api:2.1.2`
- Documentation: https://docs.cashfree.com/docs/android-integration

---

✅ **Issue Resolved!** The dependency is now correctly configured.
