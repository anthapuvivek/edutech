# Edutech flatform

Absolutely. For Lovable, I recommend not asking it to build the entire application in one prompt. Give it a strong master frontend specification first, make it establish the design system and architecture, and then build each portal/module incrementally.

Since your platform is intended to feel premium, enterprise-grade, modern, and scalable, here is the master prompt I’d give Lovable. our organization name is learntrix accordingly follow and built it

Build an Enterprise-Grade EdTech Learning Platform Frontend

Build a production-quality, premium EdTech web application frontend for a modern technology education company.

The platform should combine the best concepts of a professional Learning Management System (LMS), Udemy-style course learning, LeetCode/HackerRank-style coding practice, AI-powered learning, student career development, teacher course management, and an enterprise admin portal.

1. Important Development Rules

Build the FRONTEND FIRST.

Do not build the backend, database, payment processing, video processing infrastructure, or code execution engine yet.

The frontend must be designed so that it can later connect cleanly to a separate REST API backend built with Python FastAPI.

Use:

Next.js

React

TypeScript

Tailwind CSS

shadcn/ui

Responsive design

Component-based architecture

Clean reusable components

Proper loading states

Proper empty states

Proper error states

Accessible UI

Mobile, tablet and desktop responsiveness

Use realistic mock data for now.

Do NOT hard-code the UI around mock data in a way that makes future API integration difficult.

Create clear service/API abstraction layers so the backend can later replace mock data without rewriting the UI.

Use Git-friendly, maintainable code.

2. Brand Positioning

The company is a premium technology education platform.

The visual identity should communicate:

Luxury

Trust

Technology

Academic excellence

Career growth

Innovation

Professionalism

Enterprise quality

Do NOT make it look like a generic school website.

Do NOT make it look childish.

Do NOT overuse gradients, glowing effects, excessive animations, or neon colors.

The design should feel closer to a premium technology company combined with a world-class university and modern SaaS platform.

Use a sophisticated typography system.

Use generous whitespace.

Use strong visual hierarchy.

Use subtle borders, shadows and depth.

Animations should be subtle and purposeful.

3. Design System

Create a centralized design system.

Include:

Primary color

Secondary color

Accent color

Background colors

Surface colors

Text colors

Border colors

Success

Warning

Error

Information

Create reusable components for:

Buttons

Inputs

Selects

Dropdowns

Modal

Dialog

Drawer

Tabs

Cards

Tables

Badges

Toast notifications

Tooltips

Breadcrumbs

Pagination

Search

Filters

Progress bars

Skeleton loaders

Empty states

Error states

Confirmation dialogs

Use shadcn/ui wherever appropriate.

The entire application must use the same design language.

4. Application Architecture

Organize the frontend into clear modules.

Suggested structure:

app/

components/

features/

layouts/

lib/

services/

hooks/

types/

mock/

utils/

Separate:

Authentication

Public website

Student portal

Teacher portal

Admin portal

Coding platform

AI learning

Course learning

Notifications

Payments UI

Analytics

Use reusable layouts rather than duplicating entire pages.

5. User Roles

The platform must support role-based interfaces.

Primary roles:

Student

Teacher

Admin

Design the application so additional roles can be added later:

Mentor

Content Manager

Reviewer

Support Agent

Corporate Client

Do not assume that every user sees the same dashboard.

6. Public Website

Create a premium public-facing website.

Navigation:

Home

Courses

Programs

Coding Practice

AI Learning

About Us

Contact

Login

Get Started

Homepage sections:

Hero

Headline focused on transforming students into technology professionals.

Include:

Strong headline

Supporting description

Primary CTA: Explore Courses

Secondary CTA: Start Learning

Professional hero visual

Trust indicators

Featured Courses

Display:

Course image

Course title

Instructor

Level

Duration

Rating

Number of students

Price

Discount

CTA

Learning Categories

Examples:

Programming

Full Stack Development

Data Science

Artificial Intelligence

Machine Learning

Cloud Computing

AWS

DevOps

Cyber Security

Java

Python

Data Analytics

Why Choose Us

Show:

Industry-focused curriculum

Hands-on projects

Coding practice

AI-powered learning

Expert instructors

Career support

Certificates

Progress tracking

Coding Practice

Create a section introducing the integrated coding platform.

Mention:

Practice problems

Multiple programming languages

Difficulty levels

Contests

Leaderboards

Real-time code execution

AI Learning

Introduce:

AI Tutor

AI Coding Coach

AI Course Assistant

Personalized learning

Career section

Show:

Projects

Skill development

Interview preparation

Resume support

Career guidance

Testimonials

Create premium student testimonial cards.

Statistics

Example:

10K+ Students

100+ Courses

50+ Expert Instructors

1M+ Problems Solved

Use realistic placeholder values and make them easy to replace later.

CTA

Strong final call-to-action.

Footer

Include:

Company

Courses

Programs

Resources

Coding Practice

AI Learning

Contact

Privacy Policy

Terms

Social links

7. Course Catalogue

Create a powerful course discovery page.

Features:

Search

Category filter

Skill filter

Difficulty filter

Duration filter

Price filter

Rating filter

Sort by

Pagination

Course cards should show:

Thumbnail

Course name

Instructor

Rating

Students

Duration

Level

Price

Discount

Bestseller/New badge

8. Course Details Page

Create a premium Udemy-style course page.

Include:

Course title

Subtitle

Instructor

Rating

Student count

Course duration

Level

Last updated

Language

Course description

Learning outcomes

Skills you’ll gain

Complete curriculum

Modules

Lessons

Preview lessons

Projects

Assignments

Coding problems

Requirements

Target audience

Instructor profile

Reviews

FAQs

Pricing card

Enroll button

Enquire button

Create an expandable curriculum interface.

9. Course Enquiry System

Students should be able to enquire about courses.

Create an enquiry form:

Name

Email

Phone

Course

Experience level

Preferred learning mode

Message

After submission show a professional success state.

The UI should be ready to connect to a backend enquiry API later.

10. Authentication

Create:

Login

Register

Forgot Password

Reset Password

Email verification

OAuth buttons

Logout

Session states

Authentication UI must support role-based redirection.

Example:

Student → Student Dashboard

Teacher → Teacher Dashboard

Admin → Admin Dashboard

Use mock authentication for now.

Do not implement insecure client-side authorization as the final security mechanism.

11. Student Dashboard

Create a premium SaaS-style dashboard.

Sidebar:

Dashboard

My Learning

Courses

Coding Practice

AI Tutor

Assignments

Quizzes

Projects

Certificates

Leaderboard

Notifications

Payments

Profile

Settings

Dashboard widgets:

Courses enrolled

Courses completed

Learning streak

Hours learned

Problems solved

Current rank

Certificates

Overall progress

Add:

Continue Learning

Recommended Courses

Upcoming Tasks

Recent Activity

Learning Progress

Coding Performance

12. Student Course Player

Build a professional learning interface.

Layout:

Left:

Course curriculum

Center:

Video player

Right/bottom:

Lesson information

Features:

Video player placeholder

Play/pause

Progress bar

Playback speed

Fullscreen

Captions

Quality selector

Lesson navigation

Previous/Next

Mark complete

Notes

Resources

Discussion

The video player must be architected so it can later connect to HLS streaming through CloudFront.

Do not implement actual video infrastructure now.

13. Student Coding Platform

Create a LeetCode/HackerRank-inspired coding practice interface.

Pages:

Coding Dashboard

Problem List

Problem Details

Code Editor

Submission History

Leaderboard

Contests

Achievements

Problem list:

Problem title

Difficulty

Acceptance rate

Topics

Solved status

Filters:

Difficulty

Topic

Language

Status

Problem page:

Problem description

Examples

Constraints

Input/output

Hints

Solutions section

Discussion

Code editor

Use Monaco Editor or a suitable code editor component.

Support UI for:

Python

Java

C++

JavaScript

C#

Create Run Code and Submit buttons.

For now, create mock submission behavior.

Do NOT execute code in the frontend.

The future backend will provide the secure code execution service.

14. Coding Dashboard

Show:

Problems solved

Easy/Medium/Hard

Current streak

Coding activity

Recent submissions

Accuracy

Ranking

Badges

Recommended problems

Create a GitHub-style coding activity visualization.

15. AI Tutor Interface

Create a premium AI learning assistant.

UI should include:

Chat interface

Conversation history

New conversation

Course context

Suggested questions

Message streaming UI

Code blocks

Markdown

Copy code

Regenerate

Feedback buttons

Student should be able to select:

General AI Tutor

Course AI Tutor

Coding Coach

Interview Coach

The frontend must be ready for future streaming responses from the FastAPI backend.

Do not hard-code an AI provider into the frontend.

Use an abstract API service.

16. Teacher Dashboard

Create a separate teacher experience.

Sidebar:

Dashboard

My Courses

Course Builder

Lessons

Assignments

Quizzes

Coding Problems

Students

Analytics

Reviews

Announcements

Profile

Dashboard:

Total students

Active students

Course completion

Revenue

Average rating

Engagement

Recent enrollments

17. Teacher Course Builder

Create a visual course builder.

Structure:

Course

→ Modules

→ Lessons

→ Video

→ Resources

→ Quiz

→ Assignment

→ Coding Problem

Features:

Create course

Edit course

Reorder modules

Reorder lessons

Draft/publish

Upload video UI

Upload PDF/resource UI

Add quiz

Add coding problem

Preview course

Video upload UI should support:

Drag and drop

Upload progress

File size

Processing status

Thumbnail

Video duration

Resolution

Processing state

Prepare the frontend for future S3 presigned uploads.

18. Admin Dashboard

Create an enterprise admin portal.

Sidebar:

Overview

Users

Students

Teachers

Courses

Categories

Enquiries

Payments

Subscriptions

Coding Problems

Contests

Certificates

Analytics

Notifications

CMS

Audit Logs

Settings

Admin overview:

Total users

Students

Teachers

Active courses

Revenue

Enrollments

Course completion

Enquiries

Coding submissions

AI usage

Use charts and tables.

19. Admin User Management

Create:

User table

Search

Filters

Role

Status

Registration date

Last active

Actions

Actions:

View

Edit

Suspend

Activate

Delete

Add confirmation dialogs.

20. Admin Course Management

Show:

Course

Instructor

Category

Students

Status

Rating

Revenue

Created date

Actions:

View

Edit

Approve

Reject

Publish

Unpublish

21. Enquiry Management

Admin should see:

Student name

Course

Phone

Email

Status

Assigned staff

Created date

Last contacted

Statuses:

New

Contacted

Interested

Follow-up

Converted

Not Interested

Closed

Create a professional CRM-style interface.

22. Payments UI

Create frontend screens for:

Checkout

Order summary

Payment status

Payment success

Payment failure

Payment history

Invoices

Subscription

Refund status

Do not implement real payment processing.

Prepare interfaces for future Razorpay/Stripe integration.

23. Notifications

Create:

Notification center

Read/unread

Course notifications

Assignment notifications

Coding notifications

Payment notifications

System notifications

24. Analytics

Create analytics dashboards for:

Student:

Learning progress

Course completion

Coding performance

Time spent

Quiz performance

Teacher:

Student engagement

Course completion

Revenue

Ratings

Lesson performance

Admin:

Users

Revenue

Enrollments

Course performance

Student retention

Coding activity

AI usage

Use professional charts.

25. Responsive Design

The application must work perfectly on:

Desktop

Laptop

Tablet

Mobile

For mobile:

Use bottom navigation or a responsive sidebar where appropriate.

The coding platform should adapt intelligently to smaller screens.

The course player should also be mobile friendly.

26. Accessibility

Follow accessibility best practices.

Include:

Keyboard navigation

Proper labels

Focus states

ARIA where appropriate

Good contrast

Screen-reader friendly structure

27. Performance

Optimize the frontend for production.

Use:

Lazy loading

Code splitting

Image optimization

Proper caching

Skeleton loading

Pagination

Virtualized long lists where appropriate

Do not load huge datasets into the browser unnecessarily.

28. API Architecture

Although the backend is not being built yet, prepare the frontend for this future architecture:

Frontend:

Next.js

↓

REST API

↓

FastAPI

↓

PostgreSQL / Redis / AWS S3 / AWS services

Create a clean API client abstraction.

Example conceptual structure:

services/

auth.service.ts

course.service.ts

student.service.ts

teacher.service.ts

admin.service.ts

coding.service.ts

ai.service.ts

payment.service.ts

enquiry.service.ts

notification.service.ts

Use mock implementations initially.

Make replacing mock APIs with real APIs straightforward.

29. Environment Variables

Prepare environment variable architecture.

Example:

NEXT_PUBLIC_API_URL

NEXT_PUBLIC_APP_URL

NEXT_PUBLIC_CDN_URL

Do not expose secrets in frontend code.

Never hard-code API keys.

30. Security

Frontend security must follow production best practices.

Do not rely on frontend role checks for actual authorization.

Do not expose:

AWS credentials

Database credentials

AI API keys

Payment secrets

Private S3 credentials

All sensitive operations will be performed by the backend.

31. Future AWS Video Architecture

The frontend should be designed for this eventual infrastructure:

Teacher

→ FastAPI

→ S3 presigned upload

→ S3

→ AWS MediaConvert

→ HLS

→ CloudFront

→ Student Video Player

The course player should eventually accept a secure HLS manifest URL.

Do not build the AWS infrastructure now.

32. Future AI Architecture

The frontend should be provider-agnostic.

Future architecture:

Student

→ FastAPI AI API

→ AI Gateway

→ OpenAI / Claude / Gemini

→ RAG / Vector Search

→ Response

The frontend should never directly call OpenAI, Anthropic or Gemini using secret API keys.

33. Mock Data

Create realistic mock data for:

Courses

Students

Teachers

Lessons

Coding problems

Submissions

Enquiries

Payments

Notifications

Analytics

Reviews

Make mock data centralized and easy to remove.

34. Navigation

Implement role-aware navigation.

Public user:

Public website

Student:

Student application

Teacher:

Teacher application

Admin:

Admin application

Do not expose admin pages through normal student navigation.

35. Overall Product Quality

The final frontend should feel like a combination of:

Premium EdTech platform

Enterprise SaaS

Modern LMS

Coding practice platform

AI learning platform

It should feel production-ready rather than like an AI-generated demo.

Prioritize:

UX

Consistency

Performance

Accessibility

Scalability

Maintainability

Clean architecture

36. Important Instruction

Do NOT attempt to build the entire backend or infrastructure.

First establish the complete frontend architecture and design system.

Then implement the public website and authentication UI.

Then implement the Student Portal.

Then Teacher Portal.

Then Admin Portal.

Then Coding Platform UI.

Then AI Tutor UI.

Then analytics and remaining screens.

Keep the application modular so backend integration can be performed later without rebuilding the frontend.

Before creating large amounts of code, establish the reusable component system, routing structure, layouts, design tokens and application architecture.

The final result should be a premium enterprise EdTech frontend that is ready to connect to a Python FastAPI backend and AWS infrastructure.

One important recommendation

Don’t paste another giant prompt immediately after this. Let Lovable first establish the frontend architecture/design system.

Then build it in this order:

1. Design system → 2. Public website → 3. Authentication → 4. Student portal → 5. Teacher portal → 6. Admin portal → 7. Course player → 8. Coding platform → 9. AI Tutor → 10. Analytics.

Once Lovable generates the first version, bring the generated project/code here and I can help you review the architecture before you start building the FastAPI backend.

This project was built with [Lovable](https://lovable.dev).

**Live app**: https://strata-educate-core.lovable.app

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/5b5eef6c-0432-4ec4-b5da-e71c62dfad2d).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
