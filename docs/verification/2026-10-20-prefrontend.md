# Pre-frontend verification: 2026-10-20

## Scope and Isolation

- Exercised the deployed backend and AI images on EC2 using a separate database,
  synthetic accounts, independent JWT/internal callback secrets, and isolated containers.
- Used the real AI provider for ten student analyses, initial team generation, and regeneration.
- Disabled Firebase delivery in the isolated environment to avoid sending test notifications
  to real devices. Production user/team data was not used for destructive scenarios.
- No frontend source changes.

## Verified Flows

| Area | Checks |
| --- | --- |
| Authentication | Admin/student login, invalid credentials/JWT, role enforcement, HttpOnly refresh cookie, reissue/logout, revoked refresh token, password change/reset |
| Student search | Korean name and student-ID substring, six-result limit, own-grade filtering, self exclusion, admin name/ID/role search |
| Survey | Preferred teammates: max three, duplicate/self/nonexistent/admin/other-grade rejection; valid preferences persisted; invalid input does not complete the survey |
| Real AI analysis | Ten survey submissions produced ten successful persisted analyses; admin analysisStatus and result fields verified |
| Real AI matching | Ten students, two teams, no duplicate assignment, max five members, leaders, strengths/weaknesses/reasons, DTO parsing and stored version |
| Matching progress | Real AI stage callbacks and batch progress: 1/2 to 2/2, 50% to 100% |
| Regeneration | Real regeneration with baseVersionId/origin/versionId, version details/diff, discard and apply |
| Version replacement | Existing teams can switch to regenerated version and back; reapplying the current version preserves team IDs |
| Manual teams | Frontend memberUserIds/leaderUserId contract, max-five and duplicate-assignment validation, automatic chat room/channel creation |
| Chat REST | Send/list/edit/delete, author-only mutation, pin/unpin, readCount, unread summaries, presence, channel CRUD, cross-team access rejection |
| Chat WebSocket | JWT CONNECT, actual STOMP SEND delivery, personal/global/admin unread notifications; 17 normal/negative scenarios passed after fixes |
| Files | UTF-8 upload, byte-identical download, download URL contract, unauthenticated/cross-team rejection, missing/empty files, encoded path traversal rejection |
| Project and journal | Project CRUD, journal create/update, duplicate rejection, team authorization, completion after all members submit, completed-entry edit rejection, admin views |
| Notices | CRUD, student permissions, read marker, notification log API |
| FCM API | Token registration/deletion only; not proof of actual push delivery |
| Infrastructure | Valid HTTPS certificate, health endpoint, allowed Vercel HTTP origin, disallowed arbitrary origin; production deployment rechecked separately |

## Bugs Found and Fixed

### WebSocket Destination Authorization

Before the fix, JWT CONNECT was authenticated but SUBSCRIBE destinations were not
authorized. A student from another team actually received a test chat message.
Other team events/presence and admin unread topics could also be subscribed to.

Changes:

- Apply existing team/channel/room access checks to subscriptions and chat SEND.
- Restrict admin unread subscription to administrators.
- Allow established frontend notice and personal queues explicitly.
- Reject wildcard subscriptions, raw user queues, other users' unread topics,
  and direct publication to broker destinations.
- Keep repository authorization reads transactional for WebSocket execution outside HTTP OSIV.

The same real WebSocket test confirmed normal delivery and rejection of all
unauthorized destinations after the fix.

### Team Version Replacement Unique-Key Failure

Replacing already assigned teams failed with a duplicate team_members.user_id key.
Hibernate had queued old membership deletions while new identity-generated inserts
were executed first.

Changes:

- Flush existing team/member deletions before creating the replacement teams,
  in the same transaction.
- Return the current version without rebuilding teams when it is already APPLIED.
- Add deletion/flush/insertion ordering and idempotency regression tests.

Actual MySQL verification switched regenerated -> previous versions successfully,
with all ten students assigned once. Reapplying the same version preserved IDs.

## Automated Tests

- Backend: `./gradlew --no-daemon test bootJar`, 166 tests, zero failures/errors/skips.
- AI deployed image: `pytest -q -p no:cacheprovider /app/test`, 62 passed,
  one skipped, 16 subtests passed. External provider keys were overridden with
  placeholders for this regression suite; real-provider checks are separate above.
- Backend live auth/survey/AI-generation phase: 45 scenarios passed.
- An initial traversal test assumed JSON for a Tomcat HTML 400 response; raw-response
  retest confirmed rejection. This was a test-harness parsing issue.

## Remaining Limits

- Actual FCM delivery requires a real registered device/browser token, permission,
  and service worker. Token API success and mocked scheduler tests do not establish delivery.
- AI does not expose `DELETE /matching/jobs/{jobId}` (404). Backend cancellation
  and result rejection do not guarantee an already-started external LLM call stops.
- Once all students are assigned, starting another matching job currently reports
  that no unassigned students remain. Test regeneration before applying the final version.
- The configured production WebSocket origin is
  `https://cap-team-web-frontend.vercel.app`. A new frontend domain requires its own
  HTTP CORS and WebSocket handshake check. A different Vercel domain being accepted
  by HTTP CORS does not imply its WebSocket origin is accepted.
- Browser UI rendering, frontend routing/cookies, actual browser push permission,
  and end-to-end Vercel interaction remain frontend-integration checks.
- Synthetic tests are not a production-scale load or security audit.

## Test Interpretation

Do not equate health=UP with full feature verification. Record real request outcomes,
business failures, and untested dependencies separately. Do not count an arbitrary
400 response as success if it arose from an unexpected database exception.
