# ParcelWala — Email login API

As of 24 Oct 2026. Customers can log in with a 4-digit code sent to their
email, as well as by SMS. The backend needs the existing `send-otp` and
`verify-otp` endpoints to accept an email, and `complete-profile` to accept a
mobile number. No new endpoints, and phone login does not change.

## What changes

Every request carries **either** `phone_number` **or** `email`, never both.
Requests with `phone_number` behave exactly as today.

| Endpoint | Change | Phone login affected? |
| --- | --- | --- |
| `POST auth/customer/send-otp` | New field `email`. When present, send the 4-digit code by email instead of SMS. | No |
| `POST auth/customer/verify-otp` | New field `email`. Verify the code against that email and return the same login response. | No |
| `PUT customer/complete-profile` | New multipart part `phone_number`. Required when the account was created by email. | No |
| `user` object in every response | `email` set for email sign-ups; `phone_number` null until they complete their profile. | No |

Field names follow the snake_case `send-otp` already uses. The app also reads
camelCase responses.

## Flow

1. Customer picks **Email** on the Mobile number | Email toggle, types their address, ticks the terms, taps **Send code to email**.
2. App calls `send-otp` with `email`. Backend emails a 4-digit code.
3. App opens the code screen ("Check your email"). Customer enters the code.
4. App calls `verify-otp` with `email` + `otp`. Backend returns tokens and the `user`.
5. If `user.is_new_user` is true, the app opens **Complete profile**: name, **mobile number** (required for email sign-ups — drivers call it), optional referral code. App calls `complete-profile`.
6. Customer lands on Home. Existing customers skip step 5.

## 1. Send OTP — `POST auth/customer/send-otp`

Request — email (new):

```json
{ "email": "name@example.com", "purpose": "login" }
```

Request — phone (unchanged):

```json
{ "phone_number": "9876543210", "country_code": "+91", "purpose": "login" }
```

Response — 200, same shape for both:

```json
{
  "success": true,
  "message": "OTP sent to name@example.com",
  "data": { "otp_id": "8f14e45f-…", "expires_in": 300, "can_resend_after": 30, "otp": null }
}
```

| Field | Rule |
| --- | --- |
| `email` | Trim and lowercase before storing or comparing. Valid format, max 254 characters. |
| `phone_number` / `email` | Exactly one. Both or neither → 400. |
| `purpose` | `login` for sign-up and login, as today. New vs existing is decided at verify. |
| `expires_in`, `can_resend_after` | Seconds. |
| `otp` | Testing only. Must be `null` in production. |

The response must not reveal whether an account exists for the email.

## 2. Verify OTP — `POST auth/customer/verify-otp`

```json
{ "email": "name@example.com", "otp": "4821", "device_token": "<FCM token>", "device_type": "android" }
```

Response — same `LoginData` as phone login. A new email customer:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "user": {
      "user_id": 1042, "customer_id": 877, "phone_number": null, "full_name": null,
      "email": "name@example.com", "profile_image": null, "is_new_user": true,
      "wallet_balance": 0, "referral_code": "PW8K2Q"
    },
    "tokens": { "access_token": "<JWT>", "refresh_token": "<token>", "expires_in": 3600 }
  }
}
```

Which account the email logs into:

1. An account whose **verified** email is this address → log into it.
2. No such account → create one with this email marked verified, `phone_number` null, `is_new_user: true`.
3. Do **not** auto-link to an account that only has this email as an unverified profile email (typed during phone sign-up). Nobody proved they own it, so linking would let whoever owns the inbox into someone else's account.

## 3. Complete profile — `PUT customer/complete-profile`

Still multipart, same response (`user`). New part `phone_number`.

| Part | Phone sign-up (today) | Email sign-up (new) |
| --- | --- | --- |
| `full_name` | Required | Required |
| `phone_number` | Not sent | **Required.** 10 digits, India (+91 implied), no spaces |
| `email` | Optional | Not sent; the login email cannot be changed here |
| `referral_code` | Optional | Optional |
| profile photo | Optional | Optional |

- Store the number so bookings, rider calls and SMS updates work as for phone customers.
- Number already used by another customer → 409 (see Errors). Don't merge accounts silently.
- Return the updated `user` with `phone_number` set and `is_new_user: false`.
- Not SMS-verified in this version.

## Rules

Use the SMS values wherever SMS login already has one.

| Rule | Value |
| --- | --- |
| Code | 4 digits, numeric |
| Expiry | 5 minutes (`expires_in: 300`) |
| Resend | After 30 s (`can_resend_after: 30`); a new code cancels the previous one |
| Wrong attempts | 5 per code, then a new code is needed |
| Sends per email | Max 5 per hour |
| Sends per device / IP | Max 20 per hour |
| Sender | `ParcelWala <no-reply@[YOUR SENDING DOMAIN]>` with SPF, DKIM, DMARC |
| Subject | `Your ParcelWala code is 4821` |
| Body | The code, "valid for 5 minutes", "If you didn't ask for this, ignore this email." No links. |

## Errors

The app shows `message` to the customer as written. Envelope:
`{"success": false, "message": "…", "data": null}`.

| Case | Endpoint | HTTP | `message` |
| --- | --- | --- | --- |
| Both or neither of `phone_number` / `email` | send-otp, verify-otp | 400 | Enter a mobile number or an email. |
| Email format invalid | send-otp | 400 | Enter a valid email address. |
| Resend before `can_resend_after` | send-otp | 429 | Please wait 30 seconds before asking for a new code. |
| Over the hourly send limit | send-otp | 429 | Too many codes requested. Try again in an hour. |
| Email could not be sent | send-otp | 503 | We couldn't send the email right now. Try again or use your mobile number. |
| Wrong code | verify-otp | 400 | That code is incorrect. 3 tries left. |
| Code expired | verify-otp | 400 | This code has expired. Tap Resend for a new one. |
| Too many wrong tries | verify-otp | 429 | Too many wrong tries. Request a new code. |
| Email account without `phone_number` | complete-profile | 400 | Mobile number is required. |
| `phone_number` belongs to another customer | complete-profile | 409 | This mobile number is already registered. Log in with it or use a different number. |

## Open questions

- [ ] Which service sends the emails, and from which domain?
- [ ] Confirm the SMS values for expiry, resend wait and attempt limits.
- [ ] Should email sign-ups verify their mobile number by SMS before the first booking?
- [ ] Should phone customers be able to add and verify an email in their profile, to log in with it later?
- [ ] A fixed test email + code for Play Store review?
