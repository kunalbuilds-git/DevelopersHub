# DevelopersHub: v1 Scope

## Problem
Student developers miss hackathons, internships, GSoC deadlines, and beginner open-source issues because they're scattered across many sites. DevelopersHub is one place to find, filter, save, and track them.

**Target user:** first- and second-year CS students in India.

## v1 must-haves
1. Browse opportunities in a paginated list.
2. Filter by type (hackathon, internship, program, OSS issue) and by tag.
3. Search by keyword.
4. Open a detail page (title, organization, deadline, link, description).
5. Register and log in.
6. Bookmark an opportunity.
7. See my saved list, sorted by deadline.
8. Admin can create, edit, and delete opportunities (curated by hand).

## v1.1
- Highlight deadlines that are close.
- Users can submit an opportunity for admin approval.

## Later
- Email reminders.
- Automatic import from the GitHub API.

## Data model (draft)
- Opportunity: id, title, organization, type, description, url, deadline, createdAt
- Tag: id, name (many-to-many with Opportunity)
- User: id, email, passwordHash, role
- Bookmark: user, opportunity, createdAt
