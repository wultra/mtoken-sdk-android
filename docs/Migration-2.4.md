# Migration from 2.3.x to 2.4.x (Android)

This guide provides instructions for migrating from **Wultra Mobile Token SDK for Android** version `2.3.x` to version `2.4.x`.

Version `2.4.x` introduces a new, extensible **Pre‑approval UI Template** and replaces the legacy singular screen with a plural model. The goal is to make the UI configurable (images/icons/buttons/alerts) while keeping app code simple.

---

### Added Functionality

1. **Multiple Pre‑approval Screens**  
   The singular `preApprovalScreen` is replaced by **`preApprovalScreens: List<PreApprovalScreen>`** in `OperationUIData`.

2. **New Screen Fields**  
   Each screen may now specify:
    - `id: String?` — unique identifier for the screen
    - `backButton: Boolean?` — when true, show a back button in the UI instead of using decline as “back”
    - `image: String?` — asset identifier to be mapped in the app

3. **Structured Screen Content**  
   Each screen can include **typed `elements`** (`LIST_ITEM`, `ALERT`, `BUTTON`) and **configurable `controls`** (approve/decline UI).

4. **Configurable Controls**
    - `decline` is optional (`type: BACK | REJECT`, optional `text`)
    - `approve` is optional (`type: SLIDER | BUTTON`, optional `text`, optional `counter`). Apps should provide a default approve control if missing. The counter defines how long (in seconds) the approve control remains disabled after the screen appears.
    - `axis: HORIZONTAL | VERTICAL` and `flip: Boolean` control layout order/stacking

5. **Configurable Visuals**
    - Screen `image` and list item `icon` are resolved from app assets.

6. **Reject method**
    - The `OperationsService` reject method accepts `mobileTokenData`, just like confirm. If applicable, these values should be attached to the operation when calling confirm/reject methods.

7. **New RejectionReason**
    - New reject reason `PREAPPROVAL` indicates the user cancelled the operation during the Pre-Approval flow.

---

### Removed / Changed Functionality

1. **Singular Pre‑approval Removed from `OperationUIData`**
    - **Removed:** `preApprovalScreen` (object with `items` and `approvalType`)
    - **Replaced by:** `preApprovalScreens: List<PreApprovalScreen>`

2. **Ad‑hoc Fields Removed**
    - **Removed:** `items: [String]` → use `elements` with `type: "LIST_ITEM"` and `text`
    - **Removed:** `approvalType` → use `controls.approve.type` (`SLIDER` or `BUTTON`)

3. **Legacy Fallback Behavior**
    - If plural is present, singular is ignored.
    - If plural is absent, the SDK still converts the legacy singular via the deserializer (maps `items` → `elements`, `approvalType` → `controls.approve.type`), but your app’s model must be updated.

---

### Server / JSON Migration

**Before (2.3.x – singular):**

```json
{
  "flipButtons": true,
  "blockApprovalOnCall": false,
  "preApprovalScreen": {
    "type": "WARNING",
    "heading": "Watch out!",
    "message": "You may become a victim of an attack.",
    "items": [
      "You activate a new app and allow access to your accounts",
      "Make sure the activation takes place on your device",
      "If you have been prompted for this operation in connection with a payment, decline it"
    ],
    "approvalType": "SLIDER"
  }
}
```

**After (2.4.x – plural):**

```json
{
  "flipButtons": true,
  "blockApprovalOnCall": false,
  "preApprovalScreens": [
    {
      "id": "custom_id",
      "type": "WARNING",
      "backButton": true,
      "image": "image-label",
      "heading": "Watch out!",
      "message": "You may become a victim of an attack.",
      "elements": [
        { "id": "e1", "type": "ALERT", "style": "INFO", "text": "Make sure the activation takes place on your device" },
        { "id": "e2", "type": "BUTTON", "action": "PHONE", "text": "Call center", "href": "+42012345678" },
        { "id": "e3", "type": "LIST_ITEM", "icon": "icon-label", "text": "You activate a new app and allow access to your accounts" }
      ],
      "controls": {
        "flip": true,
        "axis": "VERTICAL",
        "decline": { "type": "REJECT", "text": "Reject Payment" },
        "approve": { "type": "BUTTON", "text": "Approve Payment", "counter": 10 }
      }
    },
    {
      "id": "qr1",
      "type": "QR_SCAN",
      "heading": "Watch out!",
      "message": "You may become a victim of an attack."
    }
  ]
}
```

---

### Migration Checklist

- Replace `preApprovalScreen` → **`preApprovalScreens`**.
- Replace `items` → **`elements` of type `LIST_ITEM`**.
- Map `approvalType` → **`controls.approve.type`**.
- Use `id`, `backButton`, `image` when present.
- For ease of use, consider using new RejectionReason **`PREAPPROVAL`** when rejecting operations during pre-approval screens flow.

---
