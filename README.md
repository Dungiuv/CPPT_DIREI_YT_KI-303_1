# Обробка даних автопарку

Java-програма читає записи про поїздки автопарку з CSV-файлу, перевіряє
кожен запис, обчислює підсумкові показники й виводить звіт у консоль та файл.
Некоректні рядки не враховуються у статистиці; звіт містить їхні номери й
причини відхилення.

## Формат вхідних даних

Файл кодується в UTF-8. Кожен непорожній рядок містить рівно п'ять полів,
розділених крапкою з комою, у такому порядку:

```text
vehicle:String;driver:String;km:double;fuelLiters:double;date:String
```

Дата має бути коректною датою ISO у форматі `YYYY-MM-DD`. Назви транспортного
засобу й водія не можуть бути порожніми; кілометраж і кількість палива мають
бути скінченними невід'ємними числами. Порожні рядки ігноруються.

Приклад файлу [data/input.csv](./data/input.csv):

```text
Skoda;Гослінг Р.К.;1488;80;2026-09-11
Mercedes;Шевченко А.Т.;228;9;2026-09-12
Volvo;Коваленко І.П.;676;40;2026-09-10
Toyota;Гайдуляк М.І.;-52;3;2026-09-09
BMW;Духнович Д.А.;345;15;2026-09-28

;Любка Е.М.;42;7;2026-09-16
Suzuki;Мельник В.В.;956;65;2026-09-07
MAN;Зварич А.В.;765;52;20;2026-09-01
```

## Збірка і запуск

Потрібна Java 21. Maven Wrapper завантажує зафіксовану версію Maven, якщо її
ще немає в локальному середовищі.

Linux або macOS:

```bash
./mvnw clean verify
./mvnw package
java -jar target/labs-1.0.0.jar
```

Windows:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd package
java -jar target\labs-1.0.0.jar
```

За замовчуванням програма читає [data/input.csv](./data/input.csv) і записує
результат у [out/report.txt](./out/report.txt). Шляхи можна змінити через CLI:

```bash
java -jar target/labs-1.0.0.jar --input data/input.csv --output out/report.txt
```

Підтримуються параметри `-h` / `--help`, `--version`, `-i` / `--input PATH`
і `-o` / `--output PATH`. Для шляхів також доступні форми
`--input=PATH` і `--output=PATH`. Приклад:

```bash
java -jar target/labs-1.0.0.jar --input=data/input.csv --output=out/report.txt
```

## Перевірки й документація

`./mvnw verify` запускає JUnit 5 і статичний аналізатор SpotBugs. GitHub Actions
виконує перевірку та пакує JAR на Ubuntu, Windows і macOS.

Щоб згенерувати Javadoc:

```bash
./mvnw javadoc:javadoc
```

На Windows використовуйте `.\mvnw.cmd javadoc:javadoc`. HTML-документація
з'явиться у `target/reports/apidocs/index.html`.

Повний [звіт лабораторної роботи](./REPORT.md) містить опис реалізації,
приклади, результати тестування, історію GitHub Issues / Pull Requests і
відповіді на контрольні питання.
