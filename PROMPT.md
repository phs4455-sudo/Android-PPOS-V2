# Android POS VibeCoding Starter Prompt (PROMPT.md) — Kotlin + Compose + Room

> 목적: 이 문서는 AI(바이브코딩)가 Android POS 기능(테이블관리 + 푸드코트 주문)을
> **일관된 아키텍처/품질/안정성**으로 구현하도록 돕는 “마스터 요구사항 + 개발 규칙”이다.
> 산출물은 실제 운영 가능한 수준(오프라인/장시간 안정성)을 목표로 한다.

---

## 0. 프로젝트 정보
- 프로젝트명: Android POS (Table + FoodCourt)
- 대상 POS 유형: 레스토랑(테이블관리) + 푸드코트(메뉴 그리드 주문)
- 목표 릴리즈: MVP → v1
- 주요 사용자: 캐셔(주문/결제/출력), 매니저(테이블/주문상태 관리)
- 성공 기준(한 줄): 오프라인에서도 테이블 상태/주문/결제가 끊김 없이 동작하고, 장시간 실행에도 안정적이다.

---

## 1. 개발 환경 & 제약 (확정)
### 1.1 Android/빌드
- 언어: Kotlin
- UI: Jetpack Compose (Material3)
- Gradle: 8.5
- Gradle JVM: 17.0.18
- Compile SDK: 34
- Target SDK: 26
- 아키텍처: MVVM (+ UseCase optional)
- DB: SQLite + Room
- 비동기: Kotlin Coroutines + Flow
- Navigation: androidx.navigation:navigation-compose

> 주의: Target 26 제약에 맞게 런타임 권한/백그라운드 제한 등 동작 확인 필요.

### 1.2 데이터/오프라인
- 오프라인을 기본으로 동작한다.
- 주문/결제/출력 관련 데이터는 **DB가 단일 진실(Single Source of Truth)** 이다.
- 앱 재시작/프로세스 종료 후에도 복구되어야 한다.
- 데이터 보존 정책(초안): 주문/결제 로그 30일 보관(추후 수정 가능)

### 1.3 장비/연동(이번 범위에서는 인터페이스만)
- 프린터/카드리더/스캐너 연동은 실제 SDK가 없으면 임의 구현 금지.
- 대신 `Driver` 인터페이스 + `FakeDriver`로 동작 확인 가능하게 구성한다.

---

## 2. 범위(Scope)
### 2.1 이번 개발 범위 (Must)
- [ ] 테이블 관리
  - 구역/층(Area) 단위로 테이블 그룹화
  - 테이블 상태 표시(빈자리/주문중/결제대기 등)
  - 테이블 이동/합석/분리(최소: 이동/합석) — DB 기반 일관성
- [ ] 푸드코트 주문
  - 코트(브랜드/매장) 선택
  - 메뉴를 **바둑판(그리드)** 형태로 빠르게 선택
  - 옵션/수량 변경, 장바구니, 주문 생성
- [ ] 주문 상태/금액/경과시간 표시
  - “실제 주문(DB)” 기반으로 계산/표시
- [ ] 앱 재시작 후 복구

### 2.2 가능하면 포함 (Should)
- [ ] 드래그&드롭으로 테이블 이동 및 합석(Compose)
- [ ] 합석된 테이블일 경우, 해제 기능
- [ ] 주문 분할(아이템 단위)
- [ ] 간단 영수증 출력(FakeDriver)

### 2.3 제외 (Won’t) — MVP에서는 제외
- [ ] 실 결제 승인(실 카드리더 SDK)
- [ ] 서버 동기화/백오피스 API
- [ ] 복잡한 권한/계정/멀티매장

---

## 3. 핵심 도메인 정의(초안)
### 3.1 엔티티
- Area(구역/층)
- TableEntity(테이블)
- MenuCategory(코트/카테고리)
- MenuItem(메뉴)
- Order(주문)
- OrderItem(주문 항목)
- OrderItemOption(옵션)
- Payment(이번 MVP는 구조만)
- PrintJob(구조만)

### 3.2 테이블 상태(권장)
- EMPTY
- OCCUPIED   (주문 존재)
- BILLING    (결제 진행/대기)
- DISABLED   (사용불가)
- (합석 여부)

### 3.3 주문 상태(권장)
- CREATED
- SENT   (주방 전송 개념 — MVP는 선택)
- PAID
- VOID

---

## 4. UX/UI 요구사항
### 4.1 공통 레이아웃 규칙
- 상단 고정 탑바(필수):
  - 더현대 로고(또는 텍스트)
  - 현재 날짜/시간
  - 포스번호/거래번호(없으면 placeholder)
  - 주요 버튼: 점검 / 조회 / 영수증재출력 / 더보기
- 터치 영역: 최소 48dp
- 한 화면에서 “작업 완료까지 탭 수 최소화”

### 4.2 화면 목록 & 플로우(필수)
- TableHome
  - Area 탭/필터(층/구역)
  - 테이블 그리드/리스트
  - 테이블 카드에: 상태/금액/경과시간 표시
- TableDetail(또는 OrderSheet)
  - 테이블 주문 목록, 합계, 주요 액션(추가주문/이동/합석/결제)
- FoodCourtHome
  - 코트/브랜드 선택
  - 카테고리 선택
  - 메뉴 그리드(바둑판)
- Cart/OrderConfirm
  - 수량/옵션/삭제
  - 주문 생성(= DB 저장)

### 4.3 예외 UX
- 앱이 장시간 켜져 있어도 크래시/멈춤 없이 동작(메모리 누수 방지)
- DB 작업 중 실패 시 사용자에게 재시도 안내 + 로그 기록

---

## 5. 비기능 요구사항(품질 기준)
### 5.1 성능
- TableHome/ FoodCourtHome 진입 시 1초 내 초기 렌더 목표(기기 사양에 따라 조정)
- 대량 메뉴(예: 300개)에서도 스크롤/탭 지연 최소화

### 5.2 안정성
- 주문 생성/이동/합석은 트랜잭션으로 보장한다(Room @Transaction).
- 중복 클릭 방지(결제/주문 확정 등 중요 액션)

### 5.3 로깅
- 중요한 이벤트(주문 생성/이동/합석/오류)는 구조화 로그로 남긴다.

---

## 6. “바이브코딩” 진행 규칙 (엄격)
### 6.1 출력/산출물 규칙
1) 먼저 설계(패키지/모듈/데이터흐름) 제시 후 구현
2) 코드는 완결된 파일 단위로 제공(생략 금지)
3) 파일 목록 + 역할 설명 → 코드 순서
4) 기존 파일 수정이 필요하면 diff로 제공
5) 외부 SDK 클래스/메서드명 임의 생성 금지
   - 모르면 `TODO(VENDOR_SDK)` + 인터페이스 추상화

### 6.2 아키텍처 규칙(MVVM)
- UI(Compose) ↔ ViewModel ↔ Repository ↔ DAO(Room)
- 모든 화면 상태는 `UiState`(data class)로 관리
- 이벤트는 `UiEvent`/`Action`으로 분리
- 비동기는 Coroutines/Flow만 사용

---

## 7. 데이터 스키마(초안)
> MVP 기준 최소 스키마. 필요시 확장.

- areas(id, name, sortOrder)
- tables(id, areaId, name, status, capacity, sortOrder)
- menu_categories(id, courtId, name, sortOrder)
- menu_items(id, categoryId, name, price, isSoldOut, imageUrl?, sortOrder)
- orders(id, tableId?, createdAt, status, totalAmount)
- order_items(id, orderId, menuItemId, nameSnapshot, priceSnapshot, qty)
- order_item_options(id, orderItemId, name, priceDelta)
- payments(id, orderId, method, amount, status, createdAt)  // MVP 구조만

---

## 8. 완료 정의(DoD)
- [ ] Must 항목 구현
- [ ] 앱 재시작 후 테이블/주문 상태 복구
- [ ] 테이블 이동/합석 후 금액/경과시간/상태가 DB 기반으로 일관되게 표시
- [ ] 푸드코트 메뉴 그리드에서 빠른 주문 생성 가능
- [ ] 주요 실패 케이스(DB 오류 등)에서 크래시 없이 안내 + 로그 남김
- [ ] Gradle 8.5 + JVM 17.0.18 환경에서 빌드/실행 가능

---

## 9. 이번 작업 지시(지금 구현할 것)
### 9.1 기능: 테이블관리 + 푸드코트 주문(MVP)
- 사용자 스토리:
  - 캐셔는 TableHome에서 구역별 테이블 상태를 보고, 테이블을 선택해 주문을 추가한다.
  - 푸드코트 화면에서 코트/카테고리별 메뉴를 그리드로 빠르게 선택해 주문을 만든다.
- 상세 요구사항:
  - 테이블 상태 표시: EMPTY/OCCUPIED/BILLING
  - 테이블 카드: 상태, 현재 주문 합계, 경과시간(주문 생성 시각 기준) 표시
  - 주문 생성 시: Order/OrderItem을 Room에 저장, Table 상태를 OCCUPIED로 갱신
  - 테이블 이동/합석: (MVP 최소)
    - 이동: 주문(Order)의 tableId 변경
    - 합석: 두 테이블 주문을 하나로 합치고(아이템 merge), 한쪽 테이블 비우기
- 예외 케이스:
  - 합석 대상 테이블이 DISABLED면 불가
  - 빈 테이블 이동 시 noop
  - DB 실패 시 롤백(트랜잭션)