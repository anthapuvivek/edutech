import type { Category, Course, PlatformStat, Testimonial } from "@/types";

/** Centralized mock data. Delete this folder once the FastAPI backend is live. */

export const mockCategories: Category[] = [
  { id: "c1", name: "Full Stack Development", slug: "full-stack", courseCount: 24, description: "React, Node, APIs and modern web architecture." },
  { id: "c2", name: "Data Science", slug: "data-science", courseCount: 18, description: "Statistics, analytics and production data workflows." },
  { id: "c3", name: "Artificial Intelligence", slug: "ai", courseCount: 15, description: "LLMs, RAG systems and applied deep learning." },
  { id: "c4", name: "Machine Learning", slug: "machine-learning", courseCount: 16, description: "Model development from theory to deployment." },
  { id: "c5", name: "Cloud Computing", slug: "cloud", courseCount: 12, description: "Distributed systems and cloud-native design." },
  { id: "c6", name: "AWS", slug: "aws", courseCount: 11, description: "Certification tracks and hands-on AWS engineering." },
  { id: "c7", name: "DevOps", slug: "devops", courseCount: 10, description: "CI/CD, containers, observability and SRE practice." },
  { id: "c8", name: "Cyber Security", slug: "cyber-security", courseCount: 9, description: "Offensive and defensive security fundamentals." },
  { id: "c9", name: "Java", slug: "java", courseCount: 14, description: "Core Java, Spring Boot and enterprise backends." },
  { id: "c10", name: "Python", slug: "python", courseCount: 21, description: "From language foundations to FastAPI services." },
  { id: "c11", name: "Data Analytics", slug: "data-analytics", courseCount: 13, description: "SQL, BI tooling and decision analytics." },
  { id: "c12", name: "Programming", slug: "programming", courseCount: 28, description: "Problem solving, DSA and engineering craft." },
];

const instructors = [
  { id: "i1", name: "Dr. Ananya Rao", title: "Principal Engineer, ex-Google", rating: 4.9, students: 18400 },
  { id: "i2", name: "Vikram Menon", title: "Staff Data Scientist", rating: 4.8, students: 12700 },
  { id: "i3", name: "Sarah Whitfield", title: "Cloud Architect, AWS Hero", rating: 4.9, students: 15200 },
  { id: "i4", name: "Rahul Iyer", title: "Security Lead, CISSP", rating: 4.7, students: 8600 },
  { id: "i5", name: "Meera Krishnan", title: "AI Research Engineer", rating: 4.9, students: 9900 },
  { id: "i6", name: "Daniel Okafor", title: "Distinguished SRE", rating: 4.8, students: 7400 },
];

const thumbs = [
  "https://images.unsplash.com/photo-1517180102446-f3ece451e9d8?auto=format&fit=crop&w=800&q=70",
  "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=800&q=70",
  "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?auto=format&fit=crop&w=800&q=70",
  "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=70",
  "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=800&q=70",
  "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=800&q=70",
  "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70",
  "https://images.unsplash.com/photo-1516116216624-53e697fedbea?auto=format&fit=crop&w=800&q=70",
];

type Seed = {
  title: string;
  subtitle: string;
  category: string;
  level: Course["level"];
  skills: string[];
  hours: number;
  lessons: number;
  rating: number;
  ratings: number;
  students: number;
  price: number;
  original?: number;
  badges?: Course["badges"];
};

const seeds: Seed[] = [
  { title: "Full Stack Engineering Program", subtitle: "React, TypeScript, FastAPI and PostgreSQL end to end", category: "Full Stack Development", level: "Intermediate", skills: ["React", "TypeScript", "FastAPI", "PostgreSQL"], hours: 96, lessons: 214, rating: 4.9, ratings: 3421, students: 12840, price: 24999, original: 44999, badges: ["Bestseller", "Career Track"] },
  { title: "Applied Machine Learning", subtitle: "From regression to production model serving", category: "Machine Learning", level: "Intermediate", skills: ["Python", "scikit-learn", "MLOps"], hours: 72, lessons: 168, rating: 4.8, ratings: 2190, students: 8420, price: 21999, original: 36999, badges: ["Bestseller"] },
  { title: "Generative AI & LLM Systems", subtitle: "RAG, embeddings, agents and evaluation", category: "Artificial Intelligence", level: "Advanced", skills: ["LLMs", "RAG", "Vector Search"], hours: 58, lessons: 132, rating: 4.9, ratings: 1480, students: 6120, price: 27999, original: 45999, badges: ["New"] },
  { title: "AWS Solutions Architect", subtitle: "Design resilient cloud architectures with confidence", category: "AWS", level: "Intermediate", skills: ["AWS", "IAM", "VPC", "S3"], hours: 64, lessons: 158, rating: 4.8, ratings: 3050, students: 11230, price: 19999, original: 32999, badges: ["Bestseller"] },
  { title: "Data Science Career Track", subtitle: "Statistics, SQL, Python and storytelling with data", category: "Data Science", level: "Beginner", skills: ["Python", "SQL", "Pandas", "Statistics"], hours: 88, lessons: 196, rating: 4.7, ratings: 2760, students: 10410, price: 22999, original: 39999, badges: ["Career Track"] },
  { title: "DevOps & Platform Engineering", subtitle: "Docker, Kubernetes, Terraform and CI/CD pipelines", category: "DevOps", level: "Advanced", skills: ["Kubernetes", "Terraform", "CI/CD"], hours: 70, lessons: 149, rating: 4.8, ratings: 1620, students: 5980, price: 23999, original: 37999 },
  { title: "Cyber Security Essentials", subtitle: "Threat modelling, network defence and secure coding", category: "Cyber Security", level: "Beginner", skills: ["Security", "Networking", "OWASP"], hours: 46, lessons: 112, rating: 4.6, ratings: 940, students: 4210, price: 17999, original: 28999 },
  { title: "Java Backend with Spring Boot", subtitle: "Enterprise-grade services, testing and deployment", category: "Java", level: "Intermediate", skills: ["Java", "Spring Boot", "JPA"], hours: 62, lessons: 141, rating: 4.7, ratings: 1870, students: 7640, price: 18999, original: 30999 },
  { title: "Python for Engineers", subtitle: "Language mastery, tooling and clean architecture", category: "Python", level: "Beginner", skills: ["Python", "Testing", "Async"], hours: 40, lessons: 104, rating: 4.8, ratings: 4120, students: 16880, price: 12999, original: 21999, badges: ["Bestseller"] },
  { title: "Data Structures & Algorithms Intensive", subtitle: "Interview-grade problem solving with 500+ problems", category: "Programming", level: "Intermediate", skills: ["DSA", "Problem Solving"], hours: 80, lessons: 220, rating: 4.9, ratings: 5240, students: 21400, price: 15999, original: 26999, badges: ["Bestseller"] },
  { title: "Cloud Native Microservices", subtitle: "Event-driven design, service meshes and scaling", category: "Cloud Computing", level: "Advanced", skills: ["Microservices", "Kafka", "gRPC"], hours: 54, lessons: 126, rating: 4.7, ratings: 810, students: 3410, price: 21999, original: 34999 },
  { title: "Business Data Analytics", subtitle: "SQL, dashboards and decision-ready insight", category: "Data Analytics", level: "Beginner", skills: ["SQL", "Power BI", "Excel"], hours: 38, lessons: 96, rating: 4.6, ratings: 1320, students: 6740, price: 11999, original: 19999, badges: ["New"] },
];

function slugify(value: string) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
}

export const mockCourses: Course[] = seeds.map((seed, index) => ({
  id: `course-${index + 1}`,
  slug: slugify(seed.title),
  title: seed.title,
  subtitle: seed.subtitle,
  category: seed.category,
  skills: seed.skills,
  level: seed.level,
  language: "English",
  durationHours: seed.hours,
  lessonCount: seed.lessons,
  rating: seed.rating,
  ratingCount: seed.ratings,
  studentCount: seed.students,
  price: seed.price,
  originalPrice: seed.original,
  currency: "INR",
  thumbnailUrl: thumbs[index % thumbs.length]!,
  instructor: instructors[index % instructors.length]!,
  badges: seed.badges,
  updatedAt: new Date(2026, 6 - (index % 6), 12).toISOString(),
}));

export const mockTestimonials: Testimonial[] = [
  { id: "t1", name: "Priya Sharma", role: "Software Engineer", company: "Razorpay", rating: 5, quote: "The Full Stack track was the most structured program I have taken. The project reviews were as rigorous as a real code review at work." },
  { id: "t2", name: "Arjun Nair", role: "Data Scientist", company: "Swiggy", rating: 5, quote: "Learntrix combined theory with real datasets. I moved from support engineering into a data science role in nine months." },
  { id: "t3", name: "Fatima Khan", role: "Cloud Engineer", company: "Thoughtworks", rating: 5, quote: "The AWS labs and the coding practice platform together made certification feel like a by-product rather than the goal." },
  { id: "t4", name: "Rohit Deshmukh", role: "SDE II", company: "Atlassian", rating: 5, quote: "500+ curated problems with the AI coding coach explaining my failed submissions is what finally got me through interviews." },
];

export const mockStats: PlatformStat[] = [
  { id: "s1", label: "Learners enrolled", value: "10K+" },
  { id: "s2", label: "Expert-led courses", value: "100+" },
  { id: "s3", label: "Industry instructors", value: "50+" },
  { id: "s4", label: "Problems solved", value: "1M+" },
];
