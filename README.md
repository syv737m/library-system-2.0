# Terminalowy Menedżer Biblioteki z bazą danych

Projekt aplikacji konsolowej do zarządzania biblioteką, zrealizowany w ramach zadania akademickiego. Aplikacja została zbudowana w oparciu o **Spring Framework**, który zarządza cyklem życia komponentów i zależnościami. Wykorzystuje **JDBC** do komunikacji z relacyjną bazą danych H2 i implementuje kompletny zestaw funkcjonalności systemu bibliotecznego.

---

## Funkcjonalności

### 1. Architektura oparta na Springu
- **Kontener IoC**: Aplikacja wykorzystuje `AnnotationConfigApplicationContext` do zarządzania beanami.
- **Wstrzykiwanie Zależności (DI)**: Wszystkie komponenty (repozytoria, serwisy) są zarządzane przez Springa i wstrzykiwane przez konstruktor.
- **Automatyczna Inicjalizacja Bazy Danych**: Spring `DataSourceInitializer` automatycznie tworzy schemat bazy danych przy starcie.
- **Zarządzanie Cyklem Życia**: Adnotacja `@PostConstruct` jest używana do automatycznego wypełniania bazy danych danymi testowymi.

### 2. System Uwierzytelniania i Role
- **System logowania** z podziałem na role: `USER` i `ADMIN`.
- **Hashowanie haseł** przy użyciu biblioteki **jBCrypt**.

### 3. Zarządzanie Książkami i Kategoriami
- **"Soft Delete"**: Książki nie są usuwane fizycznie, a jedynie oznaczane jako usunięte, co chroni historię wypożyczeń.
- **Pełne operacje CRUD** na książkach i kategoriach dostępne dla administratora.

### 4. System Wypożyczeń i Rezerwacji
- Użytkownicy mogą **wypożyczać dostępne książki** oraz **rezerwować te, które są aktualnie wypożyczone**.
- **Kolejka rezerwacji**: System automatycznie zarządza kolejką i przypisuje zwróconą książkę do pierwszego oczekującego użytkownika.
- **Powiadomienia**: Użytkownik jest informowany po zalogowaniu o książkach, które na niego czekają.

### 5. Rozbudowana Wyszukiwarka
- **Wyszukiwanie po wielu kryteriach jednocześnie**: tytuł, autor, rok wydania, kategoria.
- **Sortowanie wyników** w porządku rosnącym lub malejącym.
- **Paginacja**: Wyniki wyszukiwania są dzielone na strony, co ułatwia przeglądanie.

### 6. Statystyki dla Administratora
- Liczba wszystkich i wypożyczonych książek.
- Najpopularniejsze książki (TOP 5).
- Liczba aktywnych użytkowników.

---

## Technologie

- **Java 17**
- **Spring Framework 6** (`spring-context`, `spring-jdbc`)
- **Maven**
- **JDBC** & **H2 Database**
- **Lombok**
- **jBCrypt**
- **Jakarta Annotation API** (`@PostConstruct`)

---

## Struktura projektu

```
.
├── src
│   ├── main
│   │   ├── java
│   │   │   ├── config         // Konfiguracja Springa (AppConfig) i inicjalizator danych (DataInitializer)
│   │   │   ├── main           // Główna klasa aplikacji (Main)
│   │   │   ├── model          // Modele danych (Book, User, etc.)
│   │   │   ├── repository     // Repozytoria (@Repository)
│   │   │   └── service        // Serwisy (@Service)
│   │   └── resources
│   │       └── schema.sql     // Skrypt SQL do inicjalizacji bazy danych
│   └── test
└── pom.xml                    // Plik konfiguracyjny Mavena
```

---

## Konfiguracja Bazy Danych

Aplikacja jest skonfigurowana do pracy z plikową bazą danych **H2**. Konfiguracja `DataSource` znajduje się w klasie `src/main/java/config/AppConfig.java`. Inicjalizacja schematu bazy danych odbywa się automatycznie przez Springa przy użyciu pliku `schema.sql`.

---

## Uruchomienie i Dane Testowe

### Wymagania
- **Java Development Kit (JDK)** w wersji 17 lub nowszej.
- **Apache Maven**.

### Kroki
1. Sklonuj repozytorium lub pobierz pliki projektu.
2. Otwórz terminal lub wiersz poleceń w głównym katalogu projektu.
3. Uruchom aplikację za pomocą polecenia Mavena:
   ```bash
   mvn compile exec:java
   ```
4. Przy pierwszym uruchomieniu aplikacja automatycznie wypełni bazę danych zestawem danych testowych.

### Domyślne konta

- **Administrator:** `admin` / `admin123`
- **Użytkownik 1:** `user` / `user123`
- **Użytkownik 2 (z wypożyczoną książką):** `user2` / `user123`

### Dane początkowe
- **Książki i kategorie**: System tworzy 4 kategorie i 6 książek.
- **Historia wypożyczeń**: System tworzy kilka archiwalnych wypożyczeń, aby statystyki popularności nie były puste.
- **Aktywne wypożyczenie**: Książka "Folwark zwierzęcy" jest automatycznie wypożyczona przez `user2`.
```