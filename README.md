# BookShelf

Образовательное приложение для чтения учебников (PDF/EPUB/Web).

Форк [Mihon](https://github.com/mihonapp/mihon).

## Статус

- Phase 1: базовый форк, чистка telemetry
- Phase 2: модели данных manga → textbook
- Phase 3 (в работе): Reader (PDF/EPUB/Web)

## Сборка

```bash
./gradlew assembleDebug
```

Сборка и проверки (spotless, SQLDelight-миграции, unit-тесты) автоматически запускаются в GitHub Actions при пуше в `main`/`dev` — см. `.github/workflows/build.yml`. Готовые debug-APK доступны в артефактах сборки.
