# Client app design

Every screen of the client app, drawn as 390 × 844 phone screens on a design canvas:
https://claude.ai/artifact/SmLkxQGz3DuXWN59UPaUZb (private until its owner shares it). Most screens are
linked, so the canvas's Play button walks from the welcome screen through a booking.

This folder holds the canvas's sources: the screens are written as templates, and
[build.py](build.py) turns them into the files the canvas shows.

| File | What it is |
|---|---|
| `templates/*.html` | One template per screen, with `@@macro(...)@@` placeholders for repeated parts |
| `build.py` | Expands the placeholders into `out/project/*.dc.html`, the canvas's own format |
| `canvas.json` | Where each screen sits on the canvas, and the row titles |

## Screens

| Flow | Template | Screen |
|---|---|---|
| | `Foundations` | Colours, type, icons, corner sizes and components: the reference for the app's theme |
| Welcome & account | `Main` | Welcome: create an account, sign in, or browse without one |
| | `SignIn` | Sign in with email, or with Apple or Google |
| | `SignUp` | Create an account: name, email, mobile number, password, offers opt-in |
| Discover | `Home` | Search, categories, next appointment, free times today, book again, popular salons |
| | `Search` | Service and salon suggestions as you type, recent searches |
| | `Results` | Salons for a search, with the matched service, its price and the next free times |
| | `Map` | The same results as price pins on a map |
| | `Filters` | Sort, day, time of day and maximum price |
| Salon page | `Salon` | Photo, rating, address, opening state and the services, each with Book |
| | `SalonReviews` | Rating summary and reviews, with the salon's replies |
| | `SalonAbout` | About, the team, opening hours, location, contact and cancellation policy |
| Booking | `BookStaff` | Step 1: a professional, or anyone available |
| | `BookTime` | Step 2: day and time (works in Play) |
| | `BookReview` | Step 3: summary, note for the salon, your details, offers opt-in |
| | `BookDone` | Booked, awaiting the salon's confirmation |
| Appointments | `Bookings` | Upcoming and past bookings with their status |
| | `BookingDetail` | One appointment: status, details, salon, change time, cancel |
| | `CancelBooking` | Cancel confirmation with an optional reason |
| | `Review` | Rate a visit: stars, what stood out, a comment (works in Play) |
| Favourites, inbox & profile | `Favourites` | Saved salons with their next free time |
| | `Notifications` | Confirmations, review prompts, reminders and cancellations |
| | `Profile` | Account, privacy and help, sign out, delete account |
| | `EditProfile` | Account details, with an error on the phone field |

## Look

The Salonat brand (see [brand](../../brand)) on the admin panel's palette: rose `#9C5A65` for actions,
plum `#2C191D` for text and dark cards, glow `#E2B3A9` only on plum, warm sand neutrals, and Outfit
throughout. Appointment statuses show as Awaiting confirmation (`BOOKED`), Confirmed (`CONFIRMED`),
Completed (`COMPLETED`), Cancelled (`CANCELLED`) and Missed (`NO_SHOW`). `templates/Foundations.html`
has every value.

## Sample data

Glow Beauty Studio, Urban Cuts Barbershop, their services, prices, staff and opening hours, and the
client's email and phone come from the demo data in `salon-backend/src/main/resources/db/seed_data.sql`.
Made up for the mockup: the ratings, reviews and distances, the salons Lumen Nail Bar, Atelier Kiez Hair
and Ruhe Massage Studio, Glow's Blow-dry & Style and Gel Manicure, the client's surname, and the 24-hour
cancellation window. Photos and maps are placeholders.

## Changing a screen

Edit its template, then build:

```bash
python3 client-app/design/build.py
```

The 24 screens and `canvas.json` land in `client-app/design/out/project/`, which git ignores. Publish
all of them to the canvas in one go, with `out` as the root: from some sandboxes the canvas can't be read
back, so a publish built on an older version than the one live is refused and can't be merged.

## Macros

Each `@@name(arg|arg)@@` in a template is replaced by the markup `build.py` makes for it:

| Macro | Makes |
|---|---|
| `head(title)`, `foot(w\|h)`, `root(h)` | The page around a screen, and the screen's frame |
| `i(name\|size\|colour)` | A line icon from the set in `ICONS` |
| `btn(href\|text\|kind\|icon)` | A full-width button: `primary`, `secondary`, `dark` or `danger` |
| `chip(text\|selected\|icon)`, `slot(time\|href)` | A filter chip and a time slot |
| `badge(kind\|text)` | A status badge: `wait`, `ok`, `done` or `cancel` |
| `back(href)`, `iconbtn(href\|icon\|label)` | Round icon buttons |
| `ph(w\|h\|radius\|tone)`, `av(initials\|size\|tone)` | A photo placeholder and an initials avatar |
| `rating(score\|count)`, `stars(n\|size)` | A star rating |
| `tabbar(active)` | The bottom tab bar: `home`, `bookings`, `favourites` or `profile` |
| `lockup(h)`, `lockup_dark(h)`, `mark(h)` | The Salonat logo, read from [brand](../../brand) |
