/// Answers shaped as salon-backend sends them, for the tests that read them.
library;

const glowJson = <String, Object?>{
  'id': 1,
  'name': 'Glow Beauty Studio',
  'description':
      'A calm, modern salon specialising in colour and precision cuts.',
  'image': 'https://picsum.photos/seed/glow-beauty-studio/800/500',
  'status': 'APPROVED',
  'currency': 'EUR',
  'timezone': 'Europe/Berlin',
  'created_at': '2026-01-05T09:00:00Z',
  'updated_at': null,
  'addresses': [
    {
      'id': 1,
      'street': '12 Rosenthaler Str.',
      'city': 'Berlin',
      'country': 'Germany',
      'postal_code': '10119',
      'latitude': 52.53,
      'longitude': 13.4,
    },
  ],
  'contacts': [
    {'id': 1, 'type': 'email', 'value': 'hello@glowbeauty.example.com'},
    {'id': 2, 'type': 'phone', 'value': '+49 30 1234501'},
  ],
  'services': [
    {
      'id': 1,
      'name': 'Signature Haircut',
      'description': 'Wash, cut and style.',
      'duration_minutes': 45,
      'price': 45,
      'is_active': true,
    },
    {
      'id': 2,
      'name': 'Balayage Colour',
      'description': null,
      'duration_minutes': 120,
      'price': 150.5,
      'is_active': true,
    },
  ],
};

const urbanHoursJson = <String, Object?>{
  'timezone': 'Europe/Berlin',
  'hours': [
    {'day_of_week': 'MONDAY', 'opens_at': '10:00', 'closes_at': '13:00'},
    {'day_of_week': 'MONDAY', 'opens_at': '14:00', 'closes_at': '20:00'},
    {'day_of_week': 'SATURDAY', 'opens_at': '10:00', 'closes_at': '16:00'},
  ],
};

const miaJson = <String, Object?>{
  'id': 1,
  'title': 'Senior Stylist',
  'is_active': true,
  'user_id': 3,
  'first_name': 'Mia',
  'last_name': 'Stylist',
  'email': null,
  'hired_at': '2021-03-15',
  'calendar_colour': null,
  'created_at': null,
  'updated_at': null,
};

const availabilityJson = <String, Object?>{
  'timezone': 'Europe/Berlin',
  'duration_minutes': 45,
  'staff': [
    {
      'id': 1,
      'first_name': 'Mia',
      'last_name': 'Stylist',
      'slots': [
        {'starts_at': '2026-11-10T09:00', 'ends_at': '2026-11-10T09:45'},
        {'starts_at': '2026-11-10T09:15', 'ends_at': '2026-11-10T10:00'},
      ],
    },
    {
      'id': 2,
      'first_name': 'Noah',
      'last_name': 'Colorist',
      'slots': <Object>[],
    },
  ],
};

const appointmentJson = <String, Object?>{
  'id': 7,
  'customer': {'id': 10, 'first_name': 'Olivia', 'last_name': 'Weber'},
  'staff': {'id': 1, 'first_name': 'Mia', 'last_name': 'Stylist'},
  'service': {'id': 1, 'name': 'Signature Haircut'},
  'starts_at': '2026-11-10T10:15',
  'ends_at': '2026-11-10T11:00',
  'status': 'BOOKED',
  'price': 45.00,
  'notes': 'Window seat, please',
  'created_at': '2026-11-08T09:00:00Z',
  'updated_at': null,
};
