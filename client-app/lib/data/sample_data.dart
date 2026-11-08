/// The fakes' data: the demo salons, people and hours from
/// `salon-backend/src/main/resources/db/seed_data.sql`, as the design uses them.
library;

import 'auth/auth_user.dart';
import 'hours/opening_hours.dart';
import 'salons/salon.dart';
import 'staff/staff_member.dart';

abstract final class SampleData {
  static const glow = Salon(
    id: 1,
    name: 'Glow Beauty Studio',
    description:
        'A calm, modern salon specialising in colour and precision cuts.',
    image: 'https://picsum.photos/seed/glow-beauty-studio/800/500',
    currency: 'EUR',
    timezone: 'Europe/Berlin',
    addresses: [
      Address(
        id: 1,
        street: '12 Rosenthaler Str.',
        city: 'Berlin',
        country: 'Germany',
        postalCode: '10119',
      ),
    ],
    contacts: [
      Contact(id: 1, type: 'phone', value: '+49 30 1234501'),
      Contact(id: 2, type: 'email', value: 'hello@glowbeauty.example.com'),
    ],
    services: [
      SalonService(
        id: 1,
        name: 'Signature Haircut',
        description: 'Wash, cut and style.',
        durationMinutes: 45,
        price: 45,
        isActive: true,
      ),
      SalonService(
        id: 2,
        name: 'Balayage Colour',
        description: 'Hand-painted colour with gloss finish.',
        durationMinutes: 120,
        price: 150,
        isActive: true,
      ),
      SalonService(
        id: 3,
        name: 'Classic Manicure',
        description: 'Shape, cuticle care and polish.',
        durationMinutes: 30,
        price: 25,
        isActive: true,
      ),
    ],
  );

  static const urban = Salon(
    id: 2,
    name: 'Urban Cuts Barbershop',
    description: 'No-frills fades, beard work and hot towel shaves.',
    image: 'https://picsum.photos/seed/urban-cuts-barbershop/800/500',
    currency: 'EUR',
    timezone: 'Europe/Berlin',
    addresses: [
      Address(
        id: 2,
        street: '45 Kastanienallee',
        city: 'Berlin',
        country: 'Germany',
        postalCode: '10435',
      ),
    ],
    contacts: [
      Contact(id: 3, type: 'phone', value: '+49 30 1234502'),
      Contact(id: 4, type: 'email', value: 'hello@urbancuts.example.com'),
    ],
    services: [
      SalonService(
        id: 4,
        name: 'Classic Fade',
        description: 'Skin fade with a straight-razor finish.',
        durationMinutes: 30,
        price: 28,
        isActive: true,
      ),
      SalonService(
        id: 5,
        name: 'Beard Trim',
        description: 'Shape and line-up.',
        durationMinutes: 20,
        price: 15,
        isActive: true,
      ),
      SalonService(
        id: 6,
        name: 'Hot Towel Shave',
        description: 'Traditional straight-razor shave.',
        durationMinutes: 40,
        price: 35,
        isActive: true,
      ),
    ],
  );

  static const salons = [glow, urban];

  static final hours = <int, OpeningHours>{
    // Tuesday to Friday 09:00 to 18:00, Saturday 09:00 to 14:00.
    glow.id: OpeningHours(
      timezone: glow.timezone,
      intervals: [
        for (final day in [
          DayOfWeek.tuesday,
          DayOfWeek.wednesday,
          DayOfWeek.thursday,
          DayOfWeek.friday,
        ])
          OpeningInterval(day: day, opensAt: '09:00', closesAt: '18:00'),
        const OpeningInterval(
          day: DayOfWeek.saturday,
          opensAt: '09:00',
          closesAt: '14:00',
        ),
      ],
    ),
    // Monday to Friday 10:00 to 20:00 with a lunch break, Saturday 10:00 to 16:00.
    urban.id: OpeningHours(
      timezone: urban.timezone,
      intervals: [
        for (final day in DayOfWeek.values.take(5)) ...[
          OpeningInterval(day: day, opensAt: '10:00', closesAt: '13:00'),
          OpeningInterval(day: day, opensAt: '14:00', closesAt: '20:00'),
        ],
        const OpeningInterval(
          day: DayOfWeek.saturday,
          opensAt: '10:00',
          closesAt: '16:00',
        ),
      ],
    ),
  };

  static const staff = <int, List<StaffMember>>{
    1: [
      StaffMember(
        id: 1,
        firstName: 'Mia',
        lastName: 'Stylist',
        title: 'Senior Stylist',
        isActive: true,
      ),
      StaffMember(
        id: 2,
        firstName: 'Noah',
        lastName: 'Colorist',
        title: 'Colorist',
        isActive: true,
      ),
    ],
    2: [
      StaffMember(
        id: 3,
        firstName: 'Leo',
        lastName: 'Barber',
        title: 'Barber',
        isActive: true,
      ),
      StaffMember(
        id: 4,
        firstName: 'Sam',
        lastName: 'Barber',
        title: 'Barber',
        isActive: true,
      ),
    ],
  };

  /// The client. The surname is the design's; the seed's is Customer.
  static const client = AuthUser(
    id: 10,
    firstName: 'Olivia',
    lastName: 'Weber',
    email: 'olivia@example.com',
    role: Role.customer,
  );

  /// The client's password in the fakes.
  static const clientPassword = 'password123';
}
