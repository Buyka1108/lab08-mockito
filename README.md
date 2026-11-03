# Лаборатори 8 — Mockito mock объектууд

- Оюутны нэр: Ж.Буянхишиг
- Оюутны код: B232270114
- Хичээл: F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест

## Ажлын орчин

Ubuntu орчныг Windows-ийн WSL2 дээр ашигласан.

### java -version

```text
openjdk version "17.0.20.1" 2026-08-18
OpenJDK Runtime Environment (build 17.0.20.1+1-1-26.04-Ubuntu)
OpenJDK 64-Bit Server VM (build 17.0.20.1+1-1-26.04-Ubuntu, mixed mode, sharing)
```

### mvn -version

```text
Apache Maven 3.9.12
Maven home: /usr/share/maven
Java version: 17.0.20.1, vendor: Ubuntu, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "6.6.114.1-microsoft-standard-wsl2", arch: "amd64", family: "unix"
```

## Зорилго

CourseRegistrationService классын бүртгэлийн логикийг өгөгдлийн сан,
бодит и-мэйл үйлчилгээнээс тусгаарлан тестэлсэн.
StudentRepository болон EmailSender хамаарлуудыг Mockito mock-оор орлуулсан.

## Төслийн бүтэц

```text
lab08-mockito/
├── .gitignore
├── pom.xml
├── README.md
├── src/
│   ├── main/java/mn/edu/must/sqat/
│   │   ├── Student.java
│   │   ├── StudentRepository.java
│   │   ├── EmailSender.java
│   │   └── CourseRegistrationService.java
│   └── test/java/mn/edu/must/sqat/
│       └── CourseRegistrationServiceTest.java
└── results/
    ├── mvn-test.txt
    ├── mvn-test-mutant-1.txt
    ├── mvn-test-mutant-2.txt
    └── mvn-test-mutant-3.txt
```

## Ажиллуулах

Java 17 буюу түүнээс дээш хувилбар болон Maven шаардлагатай.

```bash
mvn test
```

Тестийн гаралтыг хадгалах:

```bash
mkdir -p results
mvn test 2>&1 | tee results/mvn-test.txt
```

## Хэсэг A — Тестүүд

| Тест | Шалгасан нөхцөл | Гол хэрэгсэл |
|---|---|---|
| A1 | Идэвхтэй, өмнөх холбоо хичээлээ үзсэн оюутанд OK буцаж, бүртгэл хадгалагдан, и-мэйл илгээх дуудлага хийгдэнэ | when, verify |
| A2 | Оюутан олдохгүй бол ERROR_NO_STUDENT; хадгалалт, и-мэйл илгээлт хийгдэхгүй | never |
| A3 | Идэвхгүй оюутанд ERROR_INACTIVE_STUDENT; хадгалалт хийгдэхгүй, и-мэйлтэй харьцахгүй | never, verifyNoInteractions |
| A4 | Өмнөх холбоо хичээл дутуу бол ERROR_PREREQUISITES; хадгалалт, и-мэйл илгээлт хийгдэхгүй | when, never |
| A5 | И-мэйл илгээгч RuntimeException шидсэн ч OK буцаж, бүртгэл хадгалагдана | doThrow, verify |
| A6 | studentId болон courseId тус бүр null үед IllegalArgumentException шидэж, хамаарлуудтай харьцахгүй | assertThrows, verifyNoInteractions |
| A7 | И-мэйлийн биед бүртгүүлсэн хичээлийн код орсон байна | ArgumentCaptor |
| A8 | Оюутан байхгүй, идэвхгүй, өмнөх холбоо хичээл дутуу гэсэн гурван алдааны нөхцөл | ParameterizedTest, CsvSource |

Нийт 10 тест: A1–A7-ийн 7 энгийн тест, A8-ийн 3 параметртэй тохиолдол.

Эх кодыг сэргээсний дараах results/mvn-test.txt-ийн эцсийн үр дүн:

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### A8 дахь lenient() тайлбар

Оюутан байхгүй эсвэл идэвхгүй үед register() эрт буцдаг тул
courseExists болон prerequisites stub-ууд тухайн мөрд ашиглагдахгүй.
Иймээс зөвхөн эдгээр хоёр stub-д lenient() хэрэглэж,
Mockito-ийн strict stubs горимын UnnecessaryStubbingException-оос сэргийлсэн.

## Хэсэг B — Мутацийн туршилт

Мутаци бүрийг эх кодод тусад нь оруулж тестийг ажиллуулаад,
дараагийн мутацийн өмнө эх кодыг сэргээсэн.

| № | Түр оруулсан өөрчлөлт | Унасан тестүүд | Failures | Errors | Үр дүн | Нотолгоо |
|---|---|---|---:|---:|---|---|
| 1 | Идэвхгүй оюутны шалгалтыг коммент болгосон | A3, A8-ийн INACTIVE тохиолдол | 2 | 0 | BUILD FAILURE | results/mvn-test-mutant-1.txt |
| 2 | saveRegistration дуудлагыг коммент болгосон | A1, A5 | 2 | 0 | BUILD FAILURE | results/mvn-test-mutant-2.txt |
| 3 | Эхний if-ээс courseId == null шалгалтыг хассан | A6 | 1 | 0 | BUILD FAILURE | results/mvn-test-mutant-3.txt |

### Унасан тестийн методын нэрс

- A1: register_whenStudentActiveAndPrerequisitesMet_returnsOkAndSavesAndEmails
- A3: register_whenStudentInactive_returnsErrorAndDoesNotEmail
- A5: register_whenEmailSendingFails_returnsOkAndSaves
- A6: register_whenEitherInputNull_throwsAndDoesNotUseDependencies
- A8: register_whenInvalidRegistration_returnsExpectedError

### Мутацийн дүгнэлт

Гурван мутаци бүгд дор хаяж нэг тест унагасан.
Мутаци 1 дээр идэвхгүй оюутны алдааны кодыг шалгасан assertion-ууд,
мутаци 2 дээр хадгалалтын дуудлагыг шалгасан verify-ууд алдааг илрүүлсэн.
A6-г анхнаас нь хоёр аргументын null тохиолдлыг тус тус шалгадаг бичсэн тул
мутаци 3-ыг барихын тулд нэмэлт засвар хийх шаардлага гараагүй.
Эцэст нь бүх мутацийг буцааж, src/main-ийг starter хувилбарт сэргээн,
10 тест амжилттай ажилласныг results/mvn-test.txt файлд хадгалсан.

## Дүгнэлт

Mockito mock ашигласнаар бүртгэлийн логикийг бодит өгөгдлийн сан,
и-мэйл сервергүйгээр тусгаарлан шалгаж чадсан.
Mock-ийн буцаах утга болон алдааг тест бүрд тохируулж,
шаардлагатай дуудлагууд хийгдсэн эсэхийг verify-гаар шалгасан.
Харин гараар бичсэн fake нь интерфейсийг хэрэгжүүлж,
санах ойд өгөгдөл хадгалах зэрэг хялбаршуулсан бодит ажиллагаатай байдаг.
Student зэрэг энгийн өгөгдлийн объект болон тестийн бай болсон
CourseRegistrationService-ийг mock хийхгүй, бодит объектоор ашигласан.
verify-г дотоод хэрэгжүүлэлтийн жижиг дуудлага бүрд хэт хэрэглэвэл
зан төлөв өөрчлөгдөөгүй засварт ч тестүүд амархан эвдрэх эрсдэлтэй.
Иймээс буцаах үр дүн болон хадгалалт, мэдэгдэл зэрэг шаардлагатай
гадаад үйлдлүүдэд голлон шалгалт хийсэн.
Мутацийн туршилт нь зөвхөн ногоон тесттэй байхаас гадна
эдгээр тест зориуд оруулсан гурван алдааг илрүүлж байгааг баталсан.
