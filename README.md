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
- **JUnit 5** & **Mockito** - do testów jednostkowych

---

## Struktura projektu

```
.
├── src
│   ├── main
│   │   ├── java
│   │   │   ├── config         // Konfiguracja Springa i inicjalizator danych
│   │   │   ├── main           // Główna klasa aplikacji
│   │   │   ├── model          // Modele danych
│   │   │   ├── repository     // Repozytoria (@Repository)
│   │   │   └── service        // Serwisy (@Service)
│   │   └── resources
│   │       └── schema.sql     // Skrypt SQL do inicjalizacji bazy danych
│   └── test
│       └── java
│           └── service        // Testy jednostkowe dla warstwy serwisowej
└── pom.xml                    // Plik konfiguracyjny Mavena
```

---

## Uruchomienie i Testowanie

### Wymagania
- **Java Development Kit (JDK)** w wersji 17 lub nowszej.
- **Apache Maven**.

### Uruchomienie Aplikacji
1. Sklonuj repozytorium lub pobierz pliki projektu.
2. Otwórz terminal lub wiersz poleceń w głównym katalogu projektu.
3. Uruchom aplikację za pomocą polecenia Mavena:
   ```bash
   mvn compile exec:java
   ```
4. Przy pierwszym uruchomieniu aplikacja automatycznie wypełni bazę danych zestawem danych testowych.

### Uruchomienie Testów
Aby uruchomić testy jednostkowe, użyj polecenia:
```bash
mvn test
```

### Domyślne konta

- **Administrator:** `admin` / `admin123`
- **Użytkownik 1:** `user` / `user123`
- **Użytkownik 2 (z wypożyczoną książką):** `user2` / `user123`
```