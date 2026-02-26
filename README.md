# Terminalowy Menedżer Biblioteki z bazą danych

Projekt aplikacji konsolowej do zarządzania biblioteką, zrealizowany w ramach zadania akademickiego. Aplikacja wykorzystuje JDBC do komunikacji z relacyjną bazą danych H2 i implementuje kompletny zestaw funkcjonalności systemu bibliotecznego.

---

## Funkcjonalności

### 1. System Uwierzytelniania i Role
- **System logowania** z podziałem na role: `USER` i `ADMIN`.
- **Hashowanie haseł** przy użyciu biblioteki **jBCrypt** dla bezpiecznego przechowywania danych.
- Domyślne konta `admin`, `user` i `user2` tworzone przy pierwszym uruchomieniu w celu łatwego testowania.

### 2. Zarządzanie Książkami i Kategoriami
- **Pełne operacje CRUD na książkach** (dodawanie, przeglądanie, edycja, usuwanie) dostępne dla administratora.
- **Zarządzanie kategoriami** (dodawanie, edycja, usuwanie) przez administratora.
- Książki są przypisane do kategorii, co umożliwia ich logiczne grupowanie.

### 3. System Wypożyczeń i Zwrotów
- Użytkownicy mogą **wypożyczać dostępne książki**.
- Użytkownicy mogą **przeglądać swoje aktywne wypożyczenia** i **zwracać książki**.
- Status książki (`AVAILABLE`, `LOANED`, `RESERVED`) jest automatycznie zarządzany przez system.

### 4. System Rezerwacji
- Użytkownik może **zarezerwować książkę, która jest aktualnie wypożyczona**.
- **Kolejka rezerwacji**: system obsługuje wielu użytkowników rezerwujących tę samą książkę, działając na zasadzie "kto pierwszy, ten lepszy".
- **Powiadomienia**: po zwrocie książki, jest ona automatycznie rezerwowana dla następnej osoby w kolejce, a użytkownik jest o tym informowany po zalogowaniu.

### 5. Rozbudowana Wyszukiwarka
- **Wyszukiwanie po wielu kryteriach jednocześnie**: tytuł, autor, rok wydania, kategoria.
- **Sortowanie wyników**: możliwość sortowania alfabetycznie (po tytule, autorze) lub numerycznie (po roku wydania), w porządku rosnącym lub malejącym.
- **Paginacja**: wyniki wyszukiwania są dzielone na strony, co ułatwia przeglądanie dużych zbiorów danych.

### 6. Statystyki dla Administratora
- **Liczba wszystkich książek** w systemie.
- **Liczba aktualnie wypożyczonych książek**.
- **Najpopularniejsze książki** (TOP 5 najczęściej wypożyczanych).
- **Liczba aktywnych użytkowników** (mających co najmniej jedno wypożyczenie).

### 7. Bezpieczeństwo i Dobre Praktyki
- **Ochrona przed SQL Injection**: wszystkie zapytania do bazy danych wykorzystują `PreparedStatement`.
- **Transakcje bazodanowe**: operacje modyfikujące wiele tabel (np. wypożyczenie, zwrot) są opakowane w transakcje, co zapewnia spójność danych.
- **Zarządzanie zasobami**: poprawne zamykanie połączeń i zasobów JDBC za pomocą `try-with-resources`.

---

## Technologie

- **Java 11+**
- **Maven** - do zarządzania zależnościami i budowania projektu
- **JDBC** - do komunikacji z bazą danych
- **H2 Database** - lekka, plikowa baza danych
- **Lombok** - do redukcji boilerplate'u w modelach danych
- **jBCrypt** - do bezpiecznego hashowania haseł

---

## Struktura projektu

```
.
├── src
│   ├── main
│   │   ├── java
│   │   │   ├── config         // Konfiguracja połączenia z bazą danych
│   │   │   ├── main           // Główna klasa aplikacji
│   │   │   ├── model          // Modele danych (Book, User, Loan, Category, Reservation, SearchCriteria)
│   │   │   ├── repository     // Interfejsy i implementacje repozytoriów (JDBC)
│   │   │   └── service        // Serwisy (np. AuthService do logowania)
│   │   └── resources
│   │       └── schema.sql     // Skrypt SQL do inicjalizacji bazy danych
│   └── test
└── pom.xml                    // Plik konfiguracyjny Mavena
```

---

## Konfiguracja Bazy Danych

Aplikacja jest skonfigurowana do pracy z plikową bazą danych **H2**. Wszystkie dane konfiguracyjne znajdują się w klasie `src/main/java/config/DatabaseConfig.java`.

- **URL:** `jdbc:h2:./library_db;AUTO_SERVER=TRUE`
- **Użytkownik:** `sa`
- **Hasło:** `(puste)`

Plik bazy danych (`library_db.mv.db`) jest tworzony automatycznie w głównym katalogu projektu przy pierwszym uruchomieniu.

---

## Uruchomienie i Dane Testowe

### Wymagania
- **Java Development Kit (JDK)** w wersji 11 lub nowszej.
- **Apache Maven**.

### Kroki
1. Sklonuj repozytorium lub pobierz pliki projektu.
2. Otwórz terminal lub wiersz poleceń w głównym katalogu projektu.
3. Uruchom aplikację za pomocą polecenia Mavena:
   ```bash
   mvn compile exec:java
   ```
4. Przy pierwszym uruchomieniu aplikacja automatycznie wypełni bazę danych zestawem danych testowych, aby umożliwić natychmiastowe przetestowanie wszystkich funkcjonalności.

### Domyślne konta

Dostępne są następujące konta do testowania aplikacji:

- **Administrator:**
  - **Login:** `admin`
  - **Hasło:** `admin123`

- **Użytkownik 1:**
  - **Login:** `user`
  - **Hasło:** `user123`

- **Użytkownik 2 (z wypożyczoną książką):**
  - **Login:** `user2`
  - **Hasło:** `user123`

### Dane początkowe
- **Książki i kategorie**: System tworzy 4 kategorie i 6 książek.
- **Historia wypożyczeń**: System tworzy kilka archiwalnych wypożyczeń, aby statystyki popularności nie były puste.
- **Aktywne wypożyczenie**: Książka "Folwark zwierzęcy" jest automatycznie wypożyczona przez `user2`, co pozwala od razu przetestować funkcję zwrotu.
```