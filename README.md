# AI Tokens

Android-застосунок для перегляду використання токенів/квот у AI-провайдерів в одному місці.

## Підтримувані провайдери

| Провайдер | Джерело даних | Що показує |
|---|---|---|
| **DeepSeek** | `GET https://api.deepseek.com/user/balance` | topped-up баланс, total, granted, валюта |
| **MiniMax** | `GET https://www.minimax.io/v1/token_plan/remains` (fallback `.../v1/coding_plan/remains`) | 5-годинне rolling + тижневе вікна, час до скидання |
| **GitHub Copilot** | `GET https://api.github.com/users/{user}/settings/billing/ai_credit/usage` (fallback `.../premium_request/usage`) | AI credits за місяць, витрачені $, розбивка по моделях |
| **Z.ai** | `GET https://api.z.ai/api/monitor/usage/quota/limit` | `TOKENS_LIMIT` (5 год) + `TIME_LIMIT` (MCP, місяць) |

> Частина ендпоінтів неофіційна й може змінитися. На кожній картці є «Показати відповідь API» для звірки сирого JSON.

## Можливості

- Список акаунтів із прогрес-барами квот, балансом, часом останнього оновлення та станом помилок.
- Додавання/видалення акаунтів.
- Безпечне локальне зберігання ключів (`EncryptedSharedPreferences`, Android Keystore).
- Ручне оновлення; паралельні запити до всіх акаунтів.

## Вимоги

- **Android Studio** (AGP 9.4.1, Gradle 9.6.0 — використовується wrapper).
- **JDK** — Android Studio постачає вбудований (JBR 25).
- **Android SDK**: `compileSdk 37`, `minSdk 24`, `targetSdk 37`.
- Доступ в інтернет (усі запити тільки до API провайдерів).

## Збірка

### Android Studio

1. Відкрити теку проєкту через `File → Open`.
2. Дочекатися Gradle Sync.
3. `Run ▶` для запуску на емуляторі/пристрої або `Build → Build Bundle(s) / APK(s) → Build APK(s)`.

### Командний рядок

Linux / macOS / WSL:

```bash
./gradlew :app:assembleDebug
```

Windows:

```bat
gradlew.bat :app:assembleDebug
```

Готовий APK лежить у `app/build/outputs/apk/debug/app-debug.apk`.

### Тести

```bash
./gradlew :app:testDebugUnitTest
```

Форсувати повторний запуск тестів (коли задача `UP-TO-DATE`):

```bash
./gradlew :app:testDebugUnitTest --rerun
```

## Важливо для Windows/WSL: шляхи без кирилиці

Інструменти Android/Gradle на Windows не переносять **не-ASCII символи у шляхах**, якщо системне кодування JVM — `Cp1251` (типово для української/російської локалі). Це ламає запуск тестових процесів (`ClassNotFoundException: GradleWorkerMain` або тестових класів).

Тому:

- **Шлях проєкту має бути ASCII**, напр. `C:\AndroidProjects\my_tokens` (а не `C:\Users\ПК\...`).
- **Gradle user home теж має бути ASCII.** Задайте змінну середовища:
  - `GRADLE_USER_HOME=C:\gradle-home`
  - або `Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle user home = C:\gradle-home`.

`local.properties` містить машинно-специфічний шлях до SDK і **не комітиться** — Android Studio створює його автоматично.

## Налаштування акаунтів

Додайте акаунт кнопкою `+` у застосунку:

- **DeepSeek** — API key.
- **MiniMax** — Subscription Key (Token Plan); виберіть регіон Global/China.
- **GitHub Copilot** — fine-grained PAT із правом `Account permissions → Plan: Read-only`, GitHub username і план. У 2026 GitHub вимірює Copilot у **AI credits** (`ai_credit/usage`); місячні ліміти: Pro 1500 / Pro+ 7000 / Max 20000 / Business 1900 / Enterprise 3900 / Free — обмежений allowance. Ліміт задається вручну, бо API не повертає поле ліміту. Картка показує загальне споживання (`grossQuantity`) та суму понад ліміт (`netQuantity`, додатковий бюджет) з вартістю і розбивкою по моделях.
- **Z.ai** — API key.

Ключі зберігаються лише на пристрої, шифруються й нікуди не надсилаються (окрім запитів до відповідного API провайдера).

## Архітектура

- Kotlin + Jetpack Compose (Material 3), MVVM (`StateFlow`), ручний DI (`AppContainer`).
- Мережа: OkHttp + `kotlinx.serialization`.
- Кожен провайдер реалізує `UsageProvider` і повертає уніфікований `ProviderUsage` (вікна квот + баланс).

```
data/model      Account, Provider, QuotaWindow, Balance, ProviderUsage
data/local      CredentialStore (зашифровані ключі)
data/remote     Http, UsageProvider, клієнти провайдерів + парсери
data/repository AccountRepository, UsageRepository
ui/home         HomeScreen, ProviderCard, HomeViewModel
ui/accounts     AddAccountScreen, ManageScreen, AccountsViewModel
```
