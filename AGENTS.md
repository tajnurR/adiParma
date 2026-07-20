# AGENTS.md

Repository guide for future AI coding agents working on AdiPharma.

## 1. Project Overview

AdiPharma is a Spring Boot based pharmacy point-of-sale/admin application. It supports pharmacy staff workflows around product catalog lookup, stock/pricing management, point-of-sale checkout, customer records, transaction history, invoice/receipt output, day open/close operations, and analytics reports.

Confirmed users/business purpose:

- Intended user is a pharmacy/admin operator using a browser-based POS dashboard.
- Core purpose is to sell medicines, reduce stock, record customer-linked sales, print invoices/thermal receipts, monitor stock alerts, and close a business day with sales totals.

Main technologies:

- Java 21
- Spring Boot 4.0.3
- Spring MVC/WebMVC
- Spring Data JPA/Hibernate
- PostgreSQL runtime driver
- Thymeleaf server templates
- OpenHTMLToPDF/PDFBox for PDF invoice generation
- Lombok for entity boilerplate
- Plain browser JavaScript, jQuery, Tailwind CDN, Google Material Symbols, Tom Select CDN, DataTables CDN, JsBarcode CDN
- Maven wrapper (`./mvnw`) with Maven 3.9.12

Major functional areas:

- POS sale creation: `PointSalesApiController` -> `PointSalesService`
- Product catalog, stock list, stock alerts, pricing batches: `MedicineStockPriceMappingApiController` -> `MedicineStockPriceMappingService`
- Customer CRUD/search/sales history: `CustomerApiController`, `CustomerSalesApiController`
- Transactions and reports: `TransactionApiController`, `ReportsApiController`
- Day ledger: `DayLedgerApiController`, `DayLedgerService`
- Invoice/receipt generation: `InvoiceController`, `InvoiceDataService`, `InvoicePdfService`, `PdfGenerationService`
- Settings/profile: `SystemSettingsApiController`, `SystemSettingsService`

## 2. Repository Structure

| Path | Responsibility |
| --- | --- |
| `pom.xml` | Maven build. Declares Spring Boot, JPA, Thymeleaf, WebMVC, PostgreSQL, Lombok, OpenHTMLToPDF, and test starters. No frontend build tooling is configured. |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | Maven wrapper scripts. Wrapper config uses Maven 3.9.12 with `distributionType=only-script`. |
| `src/main/java/com/adipharma/AdiPharmaApplication.java` | Spring Boot application entry point. |
| `src/main/java/com/adipharma/controller` | MVC/page and REST API controllers. Controllers are thin and delegate business logic to services. |
| `src/main/java/com/adipharma/service` | Main business logic, validation, query shaping, transaction boundaries, PDF rendering orchestration. |
| `src/main/java/com/adipharma/repository` | Spring Data `JpaRepository` interfaces plus JPQL query methods. |
| `src/main/java/com/adipharma/entity` | JPA entities that define the database schema Hibernate manages. |
| `src/main/java/com/adipharma/dto` | Simple public-field DTOs for sale create/response and invoice rendering. |
| `src/main/java/com/adipharma/common/enums/PaymentMethod.java` | Payment method code/label mapping. |
| `src/main/java/com/adipharma/exception` | `ResourceNotFoundException` and global HTML error-page handler. API controllers mostly handle errors locally instead. |
| `src/main/resources/application.yml` | Application name, server port, datasource, and JPA settings. Contains local database connection values; do not copy secrets/credentials into docs or logs. |
| `src/main/resources/templates` | Thymeleaf layout, sidebar, dashboard shell, error pages, and invoice/receipt templates. |
| `src/main/resources/static/business-pages` | Dynamically loaded page fragments (`.html`), page CSS, and page JS modules registered through `window.BusinessPages.register`. |
| `src/main/resources/static/dataScript` | CSV seed data and a standalone Python insertion script for generics, manufacturers, medicines, and stock mappings. |
| `src/main/resources/db/migration` | Empty. There are no migration files in the repository. |
| `src/test/java/com/adipharma/AdiPharmaApplicationTests.java` | Single Spring context-load test. |
| `target/` | Maven build output, ignored by Git. Do not edit. |

Empty placeholder directories currently exist under `src/main/java/com/adipharma/common/constants`, `common/helper`, `config`, `mapper`, `service/impl`, `util`, `src/main/java/com/mailum`, and some template/static folders. They do not contain implementation at the time of analysis.

## 3. Application Architecture

This is a modular monolith using a conventional layered Spring architecture:

`Browser UI -> Spring MVC Controller -> Service -> Spring Data Repository -> JPA Entity -> PostgreSQL`

Frontend flow:

1. `HomeController` serves `/` as `templates/home/index.html`.
2. `PageController` routes `/pos`, `/customers`, `/products`, `/stock`, `/stock-alerts`, `/reports`, `/transactions`, `/settings`, `/profile`, and `/customers/{id}/sales` back to the same shell.
3. `templates/layouts/main.html` loads jQuery, Tailwind CDN, Material Symbols, shared page assets, and a business-page fragment based on `window.location.pathname`.
4. Each business page registers an initializer in `window.BusinessPages`, then uses `fetch` or jQuery DataTables to call `/api/...`.

Backend flow:

- Controllers validate missing request bodies or parse simple query params.
- Services perform most validation, entity construction, response-map construction, and transactional writes.
- Repositories use Spring Data derived methods and JPQL. `@EntityGraph` is used where nested lazy relationships must be returned to the browser.
- Entities define table names, indexes, uniqueness, FK behavior, precision/scale, and some default timestamps.

Important exceptions to the normal flow:

- `MedicineStockPriceMappingApiController.getMedicineStockDetailsWithLimit` returns `List<AdiMedicineStockPriceMapping>` entities directly; this relies on `@EntityGraph` and open-in-view/lazy serialization behavior.
- Most API responses are ad hoc `Map<String,Object>` payloads rather than typed response DTOs.
- `GlobalExceptionHandler` returns Thymeleaf HTML error views for uncaught exceptions. REST controllers commonly catch `IllegalArgumentException` themselves and return JSON `{ "message": "..." }`.
- Invoice PDFs use Thymeleaf template rendering inside `PdfGenerationService`, then OpenHTMLToPDF, not a separate reporting engine.
- There is no Spring Security configuration found. The sidebar has a `/logout` link, but no authentication/authorization code was found.

## 4. Business Domain and Rules

### Confirmed Entities and Relationships

| Entity | Table | Key fields and relationships |
| --- | --- | --- |
| `AdiMedicineGeneric` | `adi_medicine_generic` | Generic code/name/slug. `generic_code` and `slug` are unique. |
| `AdiMedicineManufacturals` | `adi_medicine_manufacturals` | Manufacturer code/name/slug plus count columns. `manufacturer_code` and `slug` are unique. |
| `AdiMedicineDetails` | `adi_medicine_details` | Medicine/product details. Unique `brand_code` and `slug`. Optional many-to-one generic and manufacturer. |
| `AdiMedicineStockPriceMapping` | `adi_medicine_stock_price_mapping` | Stock batch/pricing row for one medicine. Holds `qty`, `price`, `costPrice`, `expireDate`, `addDate`, `addedBy`. Required many-to-one medicine. |
| `AdiCustomar` | `adi_customar` | Customer profile. Name, contact, age, address, added timestamp. Name spelling is `Customar` in code/table. |
| `AdiPointSalesMaster` | `adi_point_sales_master` | Sale/invoice header. Unique `invoiceNo`, payment info, totals, optional customer, sale/created timestamps. |
| `AdiPointSalesDetails` | `adi_point_sales_details` | Sale line items. Required stock batch and sale master, quantity, total price, discount, discount type. |
| `AdiDayLedger` | `adi_day_ledger` | Business day open/close state and daily totals by payment method. |
| `AdiSystemSettings` | `adi_system_settings` | Singleton-style pharmacy/profile/invoice settings row. Service uses the first row by ascending ID and creates defaults if none exists. Also stores invoice/receipt titles, labels, currency symbol, fallback customer text, and footer notes. |

Foreign-key behavior declared in entities:

- Medicine generic/manufacturer references use `ON DELETE SET NULL`.
- Stock mapping medicine reference uses `ON DELETE CASCADE`.
- Sale detail stock mapping uses `ON DELETE RESTRICT`.
- Sale detail sale master uses `ON DELETE CASCADE`.
- Sale master customer uses `ON DELETE SET NULL`.

### Confirmed Business Rules

- Payment methods are integer codes in `PaymentMethod`: `1=Cash`, `2=Card`, `3=Mobile`, `4=Other`.
- A POS sale requires an open day. `PointSalesService.create` calls `DayLedgerService.ensureDayOpen`.
- A POS sale requires a customer ID even though invoice display has a "Walk-in Customer" fallback.
- A POS sale requires at least one item, valid payment method, nonnegative cash received/change amount, and positive total amount.
- Each sale item must include a stock mapping ID, quantity greater than zero, nonnegative total price, and discount type `PERCENT`/`PERCENTAGE` or `BDT`/`AMOUNT`.
- Invoice numbers use `IyyyyMMdd-#####`, generated from the highest invoice with today's prefix. `PointSalesService` uses an in-process lock and retries unique constraint violations up to 3 times.
- Stock is reduced after sale detail rows are saved. If requested quantity exceeds available quantity server-side, stock is clamped to zero rather than rejecting the sale.
- Product create requires name, code, generic, manufacturer, category, selling price, cost price, quantity, and expire date.
- Product codes must match `M######`.
- Product brand codes must be unique.
- Product create creates both `AdiMedicineDetails` and an initial `AdiMedicineStockPriceMapping`.
- Product edit updates medicine details (`brandName`, `brandCode`, `type`, `description`, generic, manufacturer, `requiresRx`, `trackExpiry`) but does not update stock price/quantity fields even though the frontend sends them.
- Pricing add requires medicine ID, nonnegative selling/cost price, nonnegative quantity, and expire date. If an existing stock mapping has same medicine, selling price, cost price, and expire date, quantity is incremented; otherwise a new mapping is created.
- Customer create/update requires name, phone, age, and address. Age must match 1 to 3 digits.
- Day close is blocked before 7:00 PM server local time. It totals same-day sales from midnight to next midnight.
- Day open is idempotent for an already open current day and returns current status.
- Stock alert thresholds are hardcoded by controller calls: low stock limit `10`, expiring soon window `30` days.
- Alert status priority is out of stock first, then expiring soon, then low stock, then normal.
- Reports default to last 30 days if dates are omitted or invalid.
- System settings use a cached singleton-style row; defaults are created in code when missing.
- Invoice/receipt presentation text is database-backed through `AdiSystemSettings`: invoice title, receipt title, currency symbol, bill-to/customer labels, invoice metadata labels, table column labels, total labels, walk-in customer label, and footer notes.

### Inferred or Needs Verification

- Inferred: This application targets Bangladesh pharmacy sales because UI/PDF currency formatting uses the taka symbol and medicine seed data appears Bangladesh-specific. No explicit locale/business jurisdiction policy was found.
- Needs verification: There is no authentication, role, branch, organization, tenant, tax, refund, return, prescription enforcement, or audit trail implementation in the repository.
- Needs verification: No stock reservation/locking was found for concurrent sale requests. Server-side oversell behavior should be confirmed with the product owner before changing it.
- Needs verification: The seed script is not idempotent in a strict sense; it relies on database constraints and skips rows on insert errors. Confirm intended usage before running against non-disposable data.

## 5. Main Workflows

### Browser Shell and Page Loading

- Entry: `HomeController.home()` for `/`, `PageController.pages()` for major app paths.
- Template: `templates/layouts/main.html`.
- Frontend dispatch: `getPageKeyFromPath()` maps path to business page key and `renderPage(key)` loads `/business-pages/.../{page}.html`, CSS, and JS.
- Shared assets: `business-pages/common/common.js`, `customer-dropdown-service.js`, `toast-service.js`, `common.css`.
- Header status: layout fetches `/api/day/status` and `/api/system-settings` to populate day status and pharmacy branding.

### POS Sale

- UI entry: `static/business-pages/point-of-sale/point-of-sale.html` and `point-of-sale.js`.
- Product lookup: `fetchProducts()` calls `GET /api/medicine-stock-price-mappings?q=...&searchBy=...`.
- POS medicine search requires at least 3 typed characters. Search scopes are `all`, `medicine`, `generic`, `company`, `code`, and `category`; each new search clears current results and resets the product grid scroll to the top.
- The POS product result grid is intentionally constrained by `point-of-sale.css` to scroll inside the left panel; do not let search results grow the page height or push the cart/billing panel down.
- Customer lookup/create: `CustomerDropdownService` calls `GET /api/customers/search` and `POST /api/customers`.
- Client validation: customer selected, day not closed when `window.AppDayState.isOpen === false`, cart nonempty, payment selected, item quantity not above client-known stock, cash enough for cash payments, discount cannot reduce a line below `0.01`.
- API entry: `PointSalesApiController.create()`, `POST /api/point-sales`.
- Service: `PointSalesService.create()`.
- Database operations: verifies customer and stock IDs, creates `AdiPointSalesMaster`, creates `AdiPointSalesDetails`, decrements `AdiMedicineStockPriceMapping.qty`, all inside `@Transactional`.
- Response: `PointSaleCreateResponse` with invoice number, totals, customer, payment, sale date, and line items.
- Invoice actions: POS modal can fetch `/api/invoices/{id}/pdf` or open `/invoices/{id}/thermal`.

### Product Catalog, Product Create/Edit, Pricing

- UI entry: `product-catalog.html` and `product-catalog.js`.
- Browse endpoint: `GET /api/products/catalog` -> `MedicineStockPriceMappingService.getCatalog`.
- Categories/options: `GET /api/products/catalog/categories`, `/api/products/options/generics`, `/api/products/options/manufacturers`.
- Create endpoint: `POST /api/products` -> `createProduct`.
- Edit endpoint: `GET /api/products/{id}` and `PUT /api/products/{id}` -> `getProductDetails`, `updateProduct`; `{id}` is stock mapping ID for get/update.
- Pricing endpoint: `POST /api/products/{id}/pricing` -> `addPricing`; frontend passes the medicine ID from cached catalog data, not the stock mapping ID.
- Barcode labels: frontend-only with JsBarcode CDN and a print window. No backend barcode service exists.

### Stock and Stock Alerts

- UI entries: `stock.html`/`stock.js`, `stock-alerts.html`/`stock-alerts.js`.
- Stock list: `GET /api/stock` -> `getStockList`.
- Stock summary: `GET /api/stock/summary` -> `getStockSummary`.
- Alert list: `GET /api/stock-alerts` -> `getStockAlerts`.
- Alert summary: `GET /api/stock-alerts/summary` -> `getStockAlertSummary`.
- Backend loads up to 5000 mappings, maps rows, filters/sorts/paginates in memory for stock/alert lists.
- Detail modal in stock calls `GET /api/products/{id}` using stock mapping ID.

### Customer Management and Sales History

- UI entry: `customer.html`/`customer.js`.
- DataTables endpoint: `GET /api/customers/datatable` -> `CustomerService.datatable`.
- Search endpoint for dropdowns: `GET /api/customers/search` -> `CustomerService.search`; blank query returns no items.
- Create/update: `POST /api/customers`, `PUT /api/customers/{id}` -> `CustomerService.create/update`.
- Sales history page: `/customers/{id}/sales` shell route loads `customer-sales.html`/`customer-sales.js`.
- Sales endpoints: `GET /api/customers/{id}/sales` and `GET /api/customers/{id}/sales/{saleId}/details`.
- If a sale does not belong to the requested customer, `CustomerSalesService.getSaleDetails` returns an empty items payload rather than an error.

### Transactions

- UI entry: `transactions.html`/`transactions.js`.
- List endpoint: `GET /api/transactions` with optional `q`, `payment`, `start`, `end`, `page`, `size`, `sort`, `dir`.
- Service: `TransactionService.listTransactions`.
- Date behavior: invalid/blank dates become unbounded in the controller/service (`1970-01-01` to `2999-12-31` fallback).
- Detail endpoint: `GET /api/transactions/{id}` -> `TransactionService.getTransaction`.
- Invoice button opens `/api/invoices/{id}/pdf`.

### Reports

- UI entry: `reports.html`/`reports.js`.
- Summary endpoint: `GET /api/reports/summary`.
- Export endpoint: `GET /api/reports/export`, returns CSV named `analytics-report.csv`.
- Service: `ReportsService.getReport`.
- Metrics: sale count, total revenue, average order value, top 5 products by quantity sold, total products, stock value, low stock count, expiring soon count.

### Day Ledger

- UI entry: `settings.html`/`settings.js`.
- Status: `GET /api/day/status`.
- Recent records: `GET /api/day/records?limit=10`; service clamps limit to `1..30`.
- Open: `POST /api/day/open`.
- Close: `POST /api/day/close`; blocked before 7:00 PM and totals current-day sales by payment type.
- POS sale creation depends on `ensureDayOpen()`.

### Invoice and Receipt Rendering

- PDF endpoint: `GET /api/invoices/{id}/pdf?download=false`.
- Thermal page: `GET /invoices/{id}/thermal`.
- Data assembly: `InvoiceDataService.getInvoiceData` reads sale master/details and settings.
- Presentation labels/currency/footer text come from `SystemSettingsService.getSettingsPayload()` and are copied into `InvoiceData`.
- PDF rendering: `InvoicePdfService.generateInvoicePdf` -> `PdfGenerationService.generateFromTemplate("invoice-a4", Map.of("invoice", invoiceData))`.
- Templates: `templates/invoice-a4.html` and `templates/receipt-thermal.html`.

## 6. Coding Style and Conventions

Backend conventions:

- Package root is `com.adipharma`.
- Classes use constructor injection with `final` fields; no field injection found.
- Controllers are named `*ApiController` for REST endpoints and `HomeController`/`PageController`/`InvoiceController` for page/PDF routes.
- Services are concrete `@Service` classes in `service`; no service interfaces are currently used.
- Repositories extend `JpaRepository<Entity, IdType>`.
- Entities use Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@Builder`.
- Table names use snake_case with `adi_` prefix.
- Entity names preserve existing spelling, including `AdiCustomar` and `adi_customar`; do not rename casually.
- DTOs use public fields instead of getters/setters.
- Money uses `BigDecimal` in Java entities/services and `Number` in frontend display/payloads.
- Validation often uses `IllegalArgumentException`; controllers translate it to HTTP 400 or 404 JSON with a `message` key.
- Read-only service methods use `@Transactional(readOnly = true)` where lazy relation traversal is expected (`InvoiceDataService`, `CustomerSalesService`).
- Write transactions are explicit where multi-step writes occur (`PointSalesService.create`, `SystemSettingsService.saveSettings`).
- Null handling uses helper methods such as `isBlank`, `fallback`, and `blankToNull` locally inside services rather than shared utilities.
- Logging is not application-specific; only framework logs appear.

Frontend conventions:

- Each page script calls `window.BusinessPages.register("key", function (root) { ... return root; });`.
- Page scripts query within `root` and early-return if a required page element is missing.
- API calls use `fetch`, except customer table uses jQuery DataTables AJAX.
- Shared global utilities are `window.BusinessPages`, `window.CustomerDropdownService`, `window.ToastService`, `window.AppSettings`, and `window.AppDayState`.
- Page CSS is colocated under the same business page folder.
- Third-party browser libraries are loaded by inserting CDN `<script>`/`<link>` tags at runtime.
- User-visible errors are normally shown with `ToastService.show(message, "error")`.

Formatting/style:

- Java uses 4-space indentation in most files, with multiline constructor parameters and fluent builder chains.
- JPQL uses Java text blocks.
- Frontend JS uses semicolons, `const`/`let`, template literals for rows/modals, and local helper functions per page.
- Comments are sparse and mostly explain failure-tolerant asset loading or script sections.

Tests:

- Only one test exists: `AdiPharmaApplicationTests.contextLoads()`.
- No unit tests, repository tests, controller tests, or frontend tests were found.

## 7. Reusable Implementation Patterns

Adding a new REST feature:

1. Add a method to a `*ApiController` under `src/main/java/com/adipharma/controller`.
2. Keep request parsing/body-null checks in the controller.
3. Put validation, entity lookup, writes, and response shaping in a service.
4. Use `IllegalArgumentException` for validation failures and return `{ "message": ex.getMessage() }` from the controller.
5. Add repository methods only for query/persistence details.

Reference implementations:

- `CustomerApiController` + `CustomerService` for CRUD with simple validation and map payloads.
- `PointSalesApiController` + `PointSalesService` for transactional multi-table writes.
- `MedicineStockPriceMappingApiController` + `MedicineStockPriceMappingService` for pagination/filter/sort response maps.

Adding a new page:

1. Add an HTML/CSS/JS folder under `src/main/resources/static/business-pages/{feature}`.
2. Register the JS initializer through `window.BusinessPages.register("{key}", function(root) { ... })`.
3. Add the page asset paths and route mapping in `templates/layouts/main.html`.
4. Add a route to `PageController` if it should be directly addressable.
5. Add sidebar navigation in `templates/fragments/sidebar.html` if needed.

Adding a database-backed table/model:

1. Create a JPA entity under `entity` with explicit `@Table`, `@Column`, indexes/constraints, and FK behavior where needed.
2. Create a `JpaRepository`.
3. Add service methods for validation and response payloads.
4. Remember that Hibernate `ddl-auto: update` currently manages schema changes; there is no migration process yet.

Adding frontend API consumption:

- Follow existing page-local `fetch(...).then(response => response.json())` patterns.
- For mutating APIs, wrap non-OK responses with `{ ok, data }` and throw `data.message` where present.
- Use `ToastService` for feedback.
- Keep DOM selectors scoped to `root`.

Adding invoice fields:

- Update `InvoiceData`.
- Populate fields in `InvoiceDataService`.
- Update `invoice-a4.html` and/or `receipt-thermal.html`.
- Verify `/api/invoices/{id}/pdf` and `/invoices/{id}/thermal`.

## 8. Data Model and Database

Database configuration:

- PostgreSQL is configured in `application.yml`.
- Hibernate `spring.jpa.hibernate.ddl-auto: update` is enabled.
- SQL logging is enabled with `spring.jpa.show-sql: true`.
- There are no migration files. `src/main/resources/db/migration` is empty.

Primary key patterns:

- Most entities use `Long` IDs with `GenerationType.IDENTITY`.
- `AdiCustomar` uses `Integer` ID.
- `AdiSystemSettings` uses `Long` ID and is treated as a singleton-style table by `SystemSettingsService`.
- `AdiSystemSettings` includes invoice/receipt display settings, so Hibernate `ddl-auto:update` adds columns when the app starts against an older schema.

Audit/default fields:

- `AdiCustomar.added` defaults to `LocalDateTime.now()` in builder and `@PrePersist`.
- `AdiMedicineStockPriceMapping.addDate` defaults to `LocalDateTime.now()` in builder and `@PrePersist`.
- `AdiPointSalesMaster.createdOn` defaults to `LocalDateTime.now()` in builder and `@PrePersist`.
- `createdBy`, `addedBy`, `openedBy`, and `closedBy` exist but are hardcoded to `"admin"` or often null; no authenticated principal is wired.

Important query constraints:

- Stock/product lookups often use entity graphs to load `medicine`, `medicine.generic`, and `medicine.manufacturer`.
- Transaction list uses `coalesce(m.saleDate, m.createdOn)` in date filtering.
- Report/day ledger totals use `saleDate >= start` and `< end`.
- Stock value is `sum(price * qty)`, not cost-price value.
- Stock/alert list methods fetch up to 5000 rows, then sort/filter/page in memory.

Tenant/branch scoping:

- No organization, branch, tenant, project, account, or voucher scoping was found.
- Needs verification before adding multi-branch or multi-tenant behavior.

Data integrity assumptions:

- Unique product brand codes are enforced in entity metadata and service validation.
- Unique invoice numbers are enforced in entity metadata and retried in service logic.
- Sale detail rows restrict deletion of referenced stock mappings.
- There is no soft-delete field in any entity.

Seed data:

- `static/dataScript/generic.csv`, `manufacturer.csv`, and `medicine.csv` contain large medicine catalog seed data.
- `static/dataScript/insert_data.py` inserts generics, manufacturers, medicines, then random stock price mappings.
- Do not run the seed script against important data without reviewing it; it has hardcoded local DB config and random price/quantity generation.

## 9. API and Integration Conventions

API base style:

- REST endpoints are under `/api/...`.
- Page/PDF browser routes are plain paths such as `/`, `/pos`, `/customers/{id}/sales`, and `/invoices/{id}/thermal`.
- Most JSON response bodies are `Map<String,Object>`.
- Error JSON shape is usually `{ "message": "..." }`.

Endpoint summary:

| Endpoint | Controller/service |
| --- | --- |
| `GET /api/medicine-stock-price-mappings?q=&searchBy=` | `MedicineStockPriceMappingApiController.getMedicineStockDetailsWithLimit`; POS medicine search supports `all`, `medicine`, `generic`, `company`, `code`, `category` |
| `GET /api/products/catalog` | `MedicineStockPriceMappingService.getCatalog` |
| `GET /api/products/catalog/categories` | `getCatalogCategories` |
| `GET /api/products/options/generics` | `getGenerics` |
| `GET /api/products/options/manufacturers` | `getManufacturers` |
| `POST /api/products` | `createProduct` |
| `GET /api/products/{id}` | `getProductDetails` with stock mapping ID |
| `PUT /api/products/{id}` | `updateProduct` with stock mapping ID |
| `POST /api/products/{id}/pricing` | `addPricing` with medicine ID in current frontend usage |
| `GET /api/stock`, `GET /api/stock/summary` | stock management |
| `GET /api/stock-alerts`, `GET /api/stock-alerts/summary` | stock alerts |
| `GET /api/customers/search` | customer dropdown search |
| `GET /api/customers/datatable` | DataTables server-side customer list |
| `GET/POST/PUT /api/customers...` | customer CRUD and sales history |
| `POST /api/point-sales` | POS sale creation |
| `GET /api/transactions`, `GET /api/transactions/{id}` | transaction list/detail |
| `GET /api/reports/summary`, `GET /api/reports/export` | analytics and CSV export |
| `GET/PUT /api/system-settings` | settings/profile, including invoice/receipt labels and currency |
| `GET /api/day/status`, `GET /api/day/records`, `POST /api/day/open`, `POST /api/day/close` | day ledger |
| `GET /api/invoices/{id}/pdf` | PDF invoice bytes |
| `GET /invoices/{id}/thermal` | auto-print thermal receipt page |

Authentication/authorization:

- No Spring Security dependency/configuration/controllers were found.
- No permission checks or roles were found.
- Sidebar includes `/logout`, but no matching security implementation was found.

External integrations:

- PostgreSQL database.
- Browser CDNs: Tailwind, Google Fonts/Material Symbols, jQuery, DataTables, Tom Select, JsBarcode.
- OpenHTMLToPDF is an internal library dependency for PDF output.
- No message queues, scheduled jobs, webhooks, email/SMS, payment gateways, or external APIs were found.

Pagination/filtering:

- JSON list endpoints generally return `items`, `page`, `size`, `total`, `totalPages`.
- Customer DataTables endpoint returns `data`, `recordsTotal`, `recordsFiltered`, plus controller-added `draw`.
- Page sizes are clamped server-side: customer search max 10, customer datatable/transactions max 50, product/stock lists max 100.

## 10. Configuration and Environments

Main configuration files:

- `pom.xml`: dependencies and build plugins.
- `src/main/resources/application.yml`: Spring app name, datasource, Hibernate, SQL logging, server port.
- `.mvn/wrapper/maven-wrapper.properties`: Maven wrapper version/distribution.
- `.gitignore`: ignores `target/`, IDE files, wrapper jar, build outputs.
- `.gitattributes`: line endings for Maven scripts.

Environment behavior:

- No Spring profiles were found.
- No environment-variable placeholders were found in `application.yml`.
- No feature flags were found.
- No Docker, compose, CI, deployment, or production config files were found.
- Server port is configured in `application.yml`.

Local development:

- Requires Java 21.
- Requires PostgreSQL database matching local datasource configuration.
- `./mvnw spring-boot:run` starts the app.
- Browser pages expect internet access for CDN-hosted frontend libraries.

Security note:

- `application.yml` and `static/dataScript/insert_data.py` contain local database connection values. Treat them as configuration, not documentation content; do not copy credentials into `AGENTS.md`, tickets, logs, or chat responses.

## 11. Testing and Verification

Verified command:

```bash
./mvnw test
```

Observed result during analysis:

- Build succeeded.
- One test ran: `AdiPharmaApplicationTests.contextLoads`.
- The test started a full Spring context and connected to the configured PostgreSQL database.
- Hibernate SQL logging was emitted.
- Maven compiler reported unchecked/unsafe operations in `MedicineStockPriceMappingService`; no compilation failure.

Other useful commands from repository structure:

```bash
./mvnw spring-boot:run
./mvnw package
```

These are standard Maven/Spring Boot commands supported by the build, but only `./mvnw test` was executed during this analysis.

Testing limitations:

- No isolated unit tests for services.
- No controller/API tests.
- No repository tests with an isolated test database.
- No frontend tests.
- No linting/formatting/static-analysis plugins found in `pom.xml`.
- Because tests use the configured datasource, they may fail on machines without the expected local PostgreSQL database.

Recommended verification before completing changes:

- Always run `./mvnw test` for backend changes when a local database is available.
- For frontend changes, manually run the app and exercise the affected page because there is no automated frontend test harness.
- For sale/product/stock changes, verify API responses and database side effects.
- For invoice changes, verify both PDF and thermal receipt outputs.

## 12. Change Guidelines for Future Agents

- Read `AGENTS.md` before editing code.
- Inspect the specific affected files before changing them; this document is a map, not a replacement for current code.
- Follow the existing controller -> service -> repository pattern unless the request explicitly calls for a different design.
- Keep changes scoped. Avoid broad refactors, renames, package moves, or generated-output edits.
- Preserve public endpoint paths, JSON keys, payment codes, invoice number format, and table/column names unless the user explicitly requests a breaking change.
- Trace callers and frontend consumers before changing service methods, endpoint payloads, entity fields, or config.
- Use the existing `IllegalArgumentException` -> `{ "message": ... }` API error pattern for similar endpoints.
- Add or update tests when changing behavior. If automated coverage is missing, document manual verification.
- Do not modify `target/`.
- Do not expose or copy database credentials or other secrets.
- Be careful with `application.yml`; it was already modified in the working tree during analysis, and future agents should avoid overwriting user changes.
- Verify the final diff before responding.

## 13. High-Risk Areas

| Area | Why risky | Check before editing |
| --- | --- | --- |
| POS sale creation (`PointSalesService`) | Multi-table transactional write, invoice generation, stock decrement, financial totals. | Validate day-open rule, item validation, invoice uniqueness, stock side effects, and response contract. |
| Stock/pricing (`MedicineStockPriceMappingService`) | Product details and stock batches are split across two entities; frontend IDs differ by action. | Confirm whether a path ID is stock mapping ID or medicine ID. Test create/edit/pricing/stock detail flows. |
| Product edit | Frontend sends price/quantity, backend currently ignores these fields on edit. | Do not "fix" without confirming intended behavior; update frontend/backend/tests together if changed. |
| Day close (`DayLedgerService.closeDay`) | Aggregates official daily totals and blocks close before 7:00 PM. | Verify server timezone, business date definition, and payment-code totals. |
| Reports (`ReportsService`) | Financial metrics and stock value calculations. | Confirm date windows are inclusive/exclusive as intended and whether stock value should use selling price or cost. |
| Invoice/PDF generation | Customer-facing financial documents. | Verify `InvoiceDataService`, both templates, PDF byte response headers, and thermal print behavior. |
| System settings cache | First settings row is cached in memory. | Ensure updates refresh cache and defaults remain compatible. |
| Invoice settings | Invoice templates rely on DB-backed labels/currency from `AdiSystemSettings`. | Keep `InvoiceData`, `InvoiceDataService`, `SystemSettingsService`, profile form, and both templates in sync. |
| Database schema | Hibernate `ddl-auto:update` without migrations. | Avoid risky entity changes unless schema impact is understood. Consider adding migrations only as an explicit project decision. |
| Authentication/permissions | No actual security layer found. | Do not assume user identity, roles, or authorization exists. |
| Seed data script | Writes large data sets and random prices/quantities using local DB config. | Never run against important data without explicit approval and review. |

## 14. Known Technical Debt and Uncertainty

Confirmed technical debt:

- No migration files; schema is managed by Hibernate update.
- Minimal tests: only one context-load test.
- API responses rely heavily on raw `Map<String,Object>` and direct entity serialization.
- Frontend has repeated pagination, sorting, formatting, modal, and fetch patterns across page scripts.
- Stock/alert endpoints fetch up to 5000 rows and then sort/filter/page in memory.
- Hardcoded operator values such as `"admin"` are used for `addedBy`, `openedBy`, and `closedBy`.
- Product update accepts price/quantity parameters but does not persist them.
- POS server-side sale creation does not reject overselling; it clamps stock to zero.
- Application config and seed script include local DB credentials.
- No centralized validation framework (`jakarta.validation`) is used.
- No application-level logging around business events.
- No frontend build/dependency pinning beyond CDN URLs.

Uncertainty / needs verification:

- Whether customer is truly required for every sale or whether walk-in sales should be allowed.
- Whether stock should be rejected when sale quantity exceeds available stock.
- Whether product edit should update the current stock mapping price/quantity.
- Whether `requiresRx` should enforce prescription validation.
- Whether reports should use selling price or cost price for stock valuation.
- Whether day close at 7:00 PM is a fixed rule for all deployments.
- Whether the project intends to add authentication/authorization.
- Whether the empty migration/config/mapper/util packages are placeholders for planned architecture or leftovers.

## 15. Quick Reference

Important commands:

```bash
./mvnw test
./mvnw spring-boot:run
./mvnw package
```

Main entry points:

- App: `src/main/java/com/adipharma/AdiPharmaApplication.java`
- Shell layout: `src/main/resources/templates/layouts/main.html`
- Route-to-page mapping: `src/main/java/com/adipharma/controller/PageController.java`
- Sidebar: `src/main/resources/templates/fragments/sidebar.html`

Important config:

- Build: `pom.xml`
- Runtime: `src/main/resources/application.yml`
- Maven wrapper: `.mvn/wrapper/maven-wrapper.properties`

Key reference implementations:

- Transactional sale: `src/main/java/com/adipharma/service/PointSalesService.java`
- Inventory/product API: `src/main/java/com/adipharma/service/MedicineStockPriceMappingService.java`
- Customer CRUD: `src/main/java/com/adipharma/service/CustomerService.java`
- Day ledger: `src/main/java/com/adipharma/service/DayLedgerService.java`
- Invoice data/PDF: `src/main/java/com/adipharma/service/InvoiceDataService.java`, `InvoicePdfService.java`, `PdfGenerationService.java`
- Frontend page registration: `src/main/resources/static/business-pages/product-catalog/product-catalog.js`

Common change locations:

- POS UI/calculations: `src/main/resources/static/business-pages/point-of-sale/point-of-sale.js`
- Product catalog UI: `src/main/resources/static/business-pages/product-catalog/product-catalog.js`
- Stock UI: `src/main/resources/static/business-pages/stock/stock.js`
- Customer UI: `src/main/resources/static/business-pages/customer/customer.js`
- API controllers: `src/main/java/com/adipharma/controller`
- Business logic: `src/main/java/com/adipharma/service`
- Database models: `src/main/java/com/adipharma/entity`

Essential rules to remember:

- Do not rename `AdiCustomar`/`adi_customar` casually.
- Preserve payment codes `1..4`.
- Preserve invoice format `IyyyyMMdd-#####` unless explicitly changing it.
- POS sales require an open day in current backend logic.
- Product codes must match `M######`.
- Stock alert thresholds are currently hardcoded as low `10`, expiring within `30` days.
- There is no auth/permission layer to rely on.
- There are no migrations; entity changes directly affect Hibernate-managed schema.
- Do not copy credential values from config files.
