INSERT INTO courses (id, slug, title, subtitle, category, thumbnail_url, instructor_id, level, language, duration_hours, lesson_count, rating, rating_count, student_count, price, original_price, currency)
VALUES (
  '00000000-0000-0000-0000-000000000202'::uuid,
  'react-node-fullstack',
  'Full Stack Development with React & Node.js',
  'Master modern frontend architectures, microservices, GraphQL and state management',
  'Web Development',
  'https://images.unsplash.com/photo-1633356122544-f134324a6cee?auto=format&fit=crop&w=800&q=70',
  '00000000-0000-0000-0000-000000000002'::uuid,
  'Advanced',
  'English',
  60,
  18,
  4.9,
  1850,
  4200,
  19999,
  29999,
  'INR'
)
ON CONFLICT (slug) DO UPDATE SET
  title = EXCLUDED.title,
  subtitle = EXCLUDED.subtitle,
  category = EXCLUDED.category,
  thumbnail_url = EXCLUDED.thumbnail_url,
  level = EXCLUDED.level,
  language = EXCLUDED.language,
  duration_hours = EXCLUDED.duration_hours,
  lesson_count = EXCLUDED.lesson_count,
  rating = EXCLUDED.rating,
  rating_count = EXCLUDED.rating_count,
  student_count = EXCLUDED.student_count,
  price = EXCLUDED.price,
  original_price = EXCLUDED.original_price;

INSERT INTO modules (id, course_id, title, sequence_number)
VALUES 
  ('00000000-0000-0000-0000-000000000303'::uuid, '00000000-0000-0000-0000-000000000202'::uuid, 'Modern React & TypeScript', 1),
  ('00000000-0000-0000-0000-000000000304'::uuid, '00000000-0000-0000-0000-000000000202'::uuid, 'Node.js, Express & Database Design', 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO lessons (id, module_id, title, sequence_number, duration_seconds)
VALUES
  ('00000000-0000-0000-0000-000000000404'::uuid, '00000000-0000-0000-0000-000000000303'::uuid, 'Component Patterns & State', 1, 900),
  ('00000000-0000-0000-0000-000000000405'::uuid, '00000000-0000-0000-0000-000000000303'::uuid, 'Server State with React Query', 2, 1200),
  ('00000000-0000-0000-0000-000000000406'::uuid, '00000000-0000-0000-0000-000000000304'::uuid, 'Building Scalable APIs in Node', 1, 1500)
ON CONFLICT (id) DO NOTHING;

-- Make sure student 00000000-0000-0000-0000-000000000001 is NOT enrolled in react-node-fullstack
DELETE FROM enrollments 
WHERE student_id = '00000000-0000-0000-0000-000000000001'::uuid 
  AND course_id = '00000000-0000-0000-0000-000000000202'::uuid;
