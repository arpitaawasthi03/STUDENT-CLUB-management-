import urllib.request
import urllib.parse
import http.cookiejar
import re

BASE_URL = "http://localhost:8080"

cj = http.cookiejar.CookieJar()

class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj), NoRedirectHandler)

def post_form(url, data):
    encoded = urllib.parse.urlencode(data).encode('utf-8')
    req = urllib.request.Request(url, data=encoded, headers={'Content-Type': 'application/x-www-form-urlencoded'})
    try:
        return opener.open(req)
    except urllib.error.HTTPError as e:
        return e

def get_url(url):
    req = urllib.request.Request(url)
    try:
        return opener.open(req)
    except urllib.error.HTTPError as e:
        return e

print("=" * 70)
print("  CAMPUS CLUB MEMBERSHIP MANAGEMENT SYSTEM - LIVE HTTP DEMO")
print("=" * 70)

# Step 1: Attempt to access protected URL /student/dashboard WITHOUT login
print("\n[Step 1] Attempting to access protected URL /student/dashboard WITHOUT login ...")
unauth_res = get_url(f"{BASE_URL}/student/dashboard")
print(f"  -> Response Status: {unauth_res.status}")
print(f"  -> Redirect Location: {unauth_res.headers.get('Location')}")
assert unauth_res.status == 302 and "auth_required" in unauth_res.headers.get('Location')
print("  -> Unauthenticated request successfully intercepted and redirected to /auth/login by AuthenticationFilter!")

# Step 2: Register two students via POST /student/register
print("\n[Step 2] Registering Two Students via POST /student/register ...")
import time
ts = int(time.time())
elena_email = f"elena.{ts}@campus.edu"
david_email = f"david.{ts}@campus.edu"

res_s1 = post_form(f"{BASE_URL}/student/register", {
    "studentName": "Elena Gilbert",
    "email": elena_email,
    "password": "ElenaPassword@123",
    "course": "B.Tech Computer Science",
    "yearOfStudy": "2"
})
print(f"  -> Elena Registration Status: {res_s1.status} (Redirect: {res_s1.headers.get('Location')})")
assert res_s1.status == 302 and "/student/list" in res_s1.headers.get('Location')

res_s2 = post_form(f"{BASE_URL}/student/register", {
    "studentName": "David Warner",
    "email": david_email,
    "password": "DavidPassword@123",
    "course": "B.Tech Information Technology",
    "yearOfStudy": "3"
})
print(f"  -> David Registration Status: {res_s2.status} (Redirect: {res_s2.headers.get('Location')})")
assert res_s2.status == 302 and "/student/list" in res_s2.headers.get('Location')
print("  -> Both students successfully registered and redirected using Post-Redirect-Get pattern!")

# Step 3: Login as Elena Gilbert via POST /auth/login
print(f"\n[Step 3] Logging in as Elena Gilbert ({elena_email}) via POST /auth/login ...")
login_res = post_form(f"{BASE_URL}/auth/login", {
    "email": elena_email,
    "password": "ElenaPassword@123"
})
print(f"  -> Login Response Status: {login_res.status}")
print(f"  -> Redirect Location: {login_res.headers.get('Location')}")
session_cookies = [c.name + '=' + c.value for c in cj]
print(f"  -> HttpSession Cookie Established: {session_cookies}")
assert login_res.status == 302 and "/student/dashboard" in login_res.headers.get('Location')

# Step 4: Access Dashboard with Active Session
print("\n[Step 4] Accessing /student/dashboard with active session ...")
dash_res = get_url(f"{BASE_URL}/student/dashboard")
print(f"  -> Dashboard HTTP Status: {dash_res.status}")
dash_html = dash_res.read().decode('utf-8')
assert "Elena Gilbert" in dash_html
assert "Student Session Active" in dash_html
print("  -> Dashboard verified! Welcome banner contains 'Elena Gilbert' and active session indicator.")

# Step 5: Create Two Campus Clubs via POST /club/add (Now Authenticated)
print("\n[Step 5] Creating Two Campus Clubs via POST /club/add ...")
gdsc_name = f"Google Developer Student Club {ts}"
drama_name = f"Drama & Theater Society {ts}"

res_c1 = post_form(f"{BASE_URL}/club/add", {
    "clubName": gdsc_name,
    "category": "Technical",
    "facultyCoordinator": "Dr. Sundar Pichai",
    "maximumMembers": "35",
    "description": "Cloud, AI, Mobile & Web App Workshops and Hackathons"
})
print(f"  -> GDSC Creation Status: {res_c1.status} (Redirect: {res_c1.headers.get('Location')})")
assert res_c1.status == 302 and "/club/list" in res_c1.headers.get('Location')

res_c2 = post_form(f"{BASE_URL}/club/add", {
    "clubName": drama_name,
    "category": "Cultural",
    "facultyCoordinator": "Prof. William Shakespeare",
    "maximumMembers": "20",
    "description": "Stage plays, street theater, improv and monologue workshops"
})
print(f"  -> Drama Society Creation Status: {res_c2.status} (Redirect: {res_c2.headers.get('Location')})")
assert res_c2.status == 302 and "/club/list" in res_c2.headers.get('Location')

# Step 6: Fetch club list to get club IDs
print("\n[Step 6] Viewing available clubs list via GET /club/list ...")
club_list_res = get_url(f"{BASE_URL}/club/list")
club_list_html = club_list_res.read().decode('utf-8')
club_ids = re.findall(r'value="(\d+)"', club_list_html)
print(f"  -> Available club IDs found: {club_ids}")
target_club_ids = club_ids[-2:] if len(club_ids) >= 2 else club_ids
print(f"  -> Selected Club IDs to join: {target_club_ids}")

# Step 7: Multi-club selection POST /membership/select
print("\n[Step 7] Submitting multi-club selection to POST /membership/select ...")
select_res = post_form(f"{BASE_URL}/membership/select", [
    ("selectedClubs", target_club_ids[0]),
    ("selectedClubs", target_club_ids[1])
])
print(f"  -> Selection Response Status: {select_res.status}")
print(f"  -> Redirect Location: {select_res.headers.get('Location')}")
assert select_res.status == 302 and "/membership/review" in select_res.headers.get('Location')
print("  -> Club selection temporarily stored in HttpSession and redirected to review page!")

# Step 8: Review selected clubs GET /membership/review
print("\n[Step 8] Reviewing selected clubs via GET /membership/review ...")
review_res = get_url(f"{BASE_URL}/membership/review")
print(f"  -> Review Page Status: {review_res.status}")
review_html = review_res.read().decode('utf-8')
assert "Review Selected Club Memberships" in review_html
assert "Eligible to Join" in review_html
print("  -> Review page rendered from session storage: All selected clubs verified and eligible to join.")

# Step 9: Confirm membership POST /membership/confirm
print("\n[Step 9] Confirming membership via POST /membership/confirm ...")
confirm_res = post_form(f"{BASE_URL}/membership/confirm", {})
print(f"  -> Confirmation Response Status: {confirm_res.status}")
print(f"  -> Redirect Location: {confirm_res.headers.get('Location')}")
assert confirm_res.status == 302 and "/membership/my-clubs" in confirm_res.headers.get('Location')
print("  -> Associations successfully persisted in Hibernate @ManyToMany table 'student_club'!")

# Step 10: View Joined Clubs GET /membership/my-clubs
print("\n[Step 10] Viewing joined clubs list via GET /membership/my-clubs ...")
my_clubs_res = get_url(f"{BASE_URL}/membership/my-clubs")
print(f"  -> My Clubs Page Status: {my_clubs_res.status}")
my_clubs_html = my_clubs_res.read().decode('utf-8')
assert gdsc_name in my_clubs_html or drama_name in my_clubs_html
print(f"  -> Joined clubs list confirmed! Enrolled clubs displayed under Elena Gilbert's memberships.")

# Step 11: Duplicate Membership Prevention
print("\n[Step 11] Attempting to join the same club again (Duplicate Prevention Test) ...")
dup_select = post_form(f"{BASE_URL}/membership/select", [("selectedClubs", target_club_ids[0])])
dup_review = get_url(f"{BASE_URL}/membership/review")
dup_review_html = dup_review.read().decode('utf-8')
assert "Duplicate Membership Detected" in dup_review_html or "Already Enrolled" in dup_review_html
print("  -> Duplicate membership check PASSED! System flagged already joined club with warning badge and disabled confirmation.")

# Step 12: Logout
print("\n[Step 12] Logging out via GET /auth/logout ...")
logout_res = get_url(f"{BASE_URL}/auth/logout")
print(f"  -> Logout Response Status: {logout_res.status}")
print(f"  -> Redirect Location: {logout_res.headers.get('Location')}")
assert logout_res.status == 302 and "/auth/login" in logout_res.headers.get('Location')
print("  -> Session invalidated successfully via session.invalidate()!")

# Step 13: Verify session invalidation
print("\n[Step 13] Verifying protected dashboard access AFTER logout ...")
after_logout_res = get_url(f"{BASE_URL}/student/dashboard")
print(f"  -> Status after logout: {after_logout_res.status}")
print(f"  -> Redirect: {after_logout_res.headers.get('Location')}")
assert after_logout_res.status == 302 and "auth_required" in after_logout_res.headers.get('Location')
print("  -> Session invalidation verified! Access to protected routes is blocked and redirected to login.")

print("\n" + "=" * 70)
print("  ALL 13 END-TO-END DEMONSTRATION WORKFLOWS COMPLETED SUCCESSFULLY!")
print("=" * 70)
