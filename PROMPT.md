# PROMPT.md — THE HYUNDAI Android POS Master Prompt

> 목적: 이 문서는 Codex가 이 프로젝트를 수정/구현할 때 따라야 하는 **최상위 개발 지침서**다.  
> 이 프로젝트는 **기존 Android PDA/POS 비즈니스 로직을 최대한 재사용**하면서,  
> **Kotlin + Jetpack Compose + Room 기반의 신규 UI/앱 구조**로 확장하는 것을 목표로 한다.

---

## 0. 프로젝트 개요

### 0.1 프로젝트 목표
- 기존 Android PDA/POS 소스의 **검증된 비즈니스 로직**을 최대한 유지한다.
- UI는 Android POS에 맞는 **가로형 터치 친화적 Compose UI**로 재구성한다.
- 데이터 저장은 가능한 한 `Room` 기반으로 정리한다. 기존엔 'SQLite'기반 저장 중이고, 이 또한 활용해도 된다.
- 기존 로직과 신규 UI/데이터 계층은 **Adapter / Gateway / Wrapper** 로 연결한다.

### 0.2 현재 화면 범위
현재 프로젝트에서 다루는 주요 화면은 아래와 같다.
- `RESTAURANT`
- `FOOD`
- `PAYMENT`
- `SUPER_HOME`
- `PRODUCT_REGISTER`

### 0.3 중요한 의미 정의
- `PRODUCT_REGISTER` 는 **판매용 상품등록 화면** 이다.
  - 바코드 스캔 또는 상품 터치로 상품을 담고
  - 행사할인 / 보류 / 취소 / 시재 / 결제 수단 선택으로 이어지는
  **슈퍼 POS 판매 등록 화면**을 의미한다.

---

## 1. 고정 개발 환경

아래 조건은 프로젝트의 고정 전제이며, 임의로 변경하지 않는다.

- Language: `Kotlin`
- UI: `Jetpack Compose` + `Material3`
- Gradle: `8.5`
- Compile SDK: `34`
- Target SDK: `26`
- Gradle JVM / JDK: `17.0.18`
- Local DB: `SQLite + Room`
- Async: `Kotlin Coroutines + Flow`
- Navigation: `navigation-compose`
- Architecture: `MVVM`

---

## 2. 절대 규칙 (Non-negotiable Rules)

### 2.1 레거시 비즈니스 로직 재사용
- 기존 Android PDA/POS 비즈니스 로직은 **이미 검증된 자산**으로 간주한다.
- 해당 로직은 **가능한 한 수정하지 않는다**.
- 기존 로직을 Compose/UI에 직접 억지로 맞추지 말고, **Adapter / Wrapper / Gateway** 로 연결한다.
- 기존 로직을 새로 다시 작성하거나 추측으로 치환하지 않는다.

### 2.2 Vendor SDK / 장비 API 추측 금지
- 스캐너, 프린터, 결제 단말, 카드 리더기 등 외부 장비 SDK API를 **추측해서 만들지 않는다**.
- 확실하지 않으면 반드시 아래 방식으로 처리한다.
  - `TODO(VENDOR_SCANNER)`
  - `TODO(VENDOR_PRINTER)`
  - `TODO(VENDOR_PAYMENT)`
  - Interface / Gateway 로 추상화

### 2.3 Compose 내부에 비즈니스 로직 금지
- Composable 함수 안에는 비즈니스 로직을 넣지 않는다.
- Composable은 다음 역할만 담당한다.
  - UI 렌더링
  - 사용자 이벤트 전달
  - 상태 표시
- 비즈니스 로직 / 데이터 조합 / 검증 / 상태 전이는 ViewModel 및 domain 계층에서 처리한다.

### 2.4 빌드 안정성 유지
- 모든 변경 후 반드시:

```bash
./gradlew assembleDebug
```

를 실행하고, 컴파일/빌드 오류를 모두 해결한다.
- 깨진 상태로 작업을 끝내지 않는다.

### 2.5 디자인 일관성 유지
- 기존 `Restaurant` / `Food` 화면의 디자인 언어를 유지한다.
- Super POS 화면만 전혀 다른 스타일로 만들지 않는다.

---

## 3. 아키텍처 원칙

### 3.1 기본 계층 구조
프로젝트의 기본 구조는 아래를 따른다.

```text
Compose UI
  -> ViewModel
  -> UseCase / Legacy Business Logic
  -> Repository
  -> Room / Legacy Adapter / Gateway
```

### 3.2 데이터 원칙
- 가능한 경우 `Room` 을 Single Source of Truth 로 유지한다.
- 레거시 로직이 메모리/파일/기존 DB 구조를 사용하더라도, 신규 UI에서는 Adapter를 통해 일관성 있게 연결한다.
- 동일한 의미의 데이터를 UI마다 따로 들고 있지 않는다.

### 3.3 추천 Adapter / Gateway
필요 시 아래 인터페이스를 우선 고려한다.
- `BarcodeScannerGateway`
- `ProductLookupGateway`
- `SalesCartGateway`
- `PaymentGateway`
- `ReceiptGateway`
- `LegacyOrderGateway`

---

## 4. 현재 내비게이션 구조

### 4.1 기존 POS 흐름
기존 레스토랑/푸드 흐름은 아래를 유지/확장한다.

```text
RESTAURANT
 -> FOOD
 -> PAYMENT
 -> RESTAURANT
```

### 4.2 Super POS 흐름
Super POS는 아래 흐름을 기본으로 한다.

```text
SUPER_HOME
 -> PRODUCT_REGISTER()
 -> PAYMENT (future or optional)
```

또는

```text
SUPER_HOME
 -> PRODUCT_REGISTER(barcode)
```

### 4.3 바코드 스캔 진입 규칙
- `SUPER_HOME` 에서 바코드 스캔 성공 시:
  - `PRODUCT_REGISTER(barcode=...)` 로 이동
- `SUPER_HOME` 에서 `상품등록` 버튼/타일 선택 시:
  - `PRODUCT_REGISTER()` 로 이동

---

## 5. 공통 디자인 언어

### 5.1 시각 스타일
- 상단 고정 `PosTopBar`
- 배경: 라이트 그레이 / 라이트 베이지 / 아이보리 톤
- 강조색:
  - `PRIMARY_GREEN` = 딥그린
  - `ACCENT_BEIGE` = 베이지/샌드 톤 CTA
  - `ACCENT_SLATE` = 블루그레이/슬레이트 보조 강조
- 카드/버튼/패널은 둥근 모서리와 충분한 여백 사용
- 고정 영역은 명확히 분리:
  - 상단 탑바
  - 우측 패널 / 하단 액션바

### 5.2 UX 원칙
- 가로형 Android POS/PDA 기준
- 터치 중심 UX
- 버튼은 크고 명확해야 한다
- 리스트/그리드/액션 영역을 시각적으로 명확히 분리한다
- 스캔/등록/결제까지의 주요 플로우는 탭 수가 적어야 한다

---

## 6. 화면 정의

### 6.1 RESTAURANT
- 2-pane 레이아웃
  - 좌: 테이블 배치/그리드
  - 우: 선택 테이블 주문 패널
- 테이블 정보:
  - 테이블명
  - 금액
  - 경과시간
  - 인원
- 우측 패널:
  - 선택 테이블 헤더
  - 주문 목록
  - 총 주문금액
  - 결제 버튼

### 6.2 FOOD
- 2-pane 레이아웃
  - 좌: 현재 주문 리스트/카트
  - 우: 카테고리 탭 + 메뉴 그리드
- 하단 고정 액션바:
  - `반품/환불`
  - `주문 보류`
  - `결제 진행`

### 6.3 PAYMENT
- 2-pane 레이아웃
  - 좌: 주문/결제 요약
  - 우: 결제 수단 / 키패드 / 결제 옵션
- 큰 터치형 결제 수단 버튼 사용
- 숫자 키패드는 POS 친화적 형태 유지

### 6.4 SUPER_HOME
- 상단: 공통 `PosTopBar`
- 그 아래: 바코드 스캔/입력 바
- 본문 2-pane
  - 좌 65%:
    - Hero Tile Row (`상품판매`, `상품등록`, `통합조회`)
    - Secondary Action Grid
  - 우 35%:
    - 직전결제내역 / 최근 거래 요약 패널

### 6.5 PRODUCT_REGISTER (판매용 상품등록 화면)
- 상단: 공통 `PosTopBar`
- 본문 2-pane
  - 좌 45%: 장바구니/등록 리스트 + 좌측 하단 액션 + 금액 요약
  - 우 55%: 카테고리 탭 + 상품 그리드 + 하단 2x3 시재/결제 액션 버튼
- 이 화면은 **판매용 상품 등록/선택/결제 진입 화면**이다

---

## 7. PRODUCT_REGISTER 상세 요구사항

### 7.1 좌측 패널 — 장바구니 리스트
리스트 헤더 컬럼은 다음을 사용한다.
- `No.`
- `상품`
- `단가`
- `수량`
- `금액`

각 행은 다음을 포함한다.
- 순번
- 상품명
- 필요시 2번째 줄 메타정보(예: 바코드)
- 단가
- 수량 Stepper (`-`, 현재수량, `+`)
- 금액
- 삭제 버튼 (`X`)

### 7.2 좌측 하단 액션 버튼
좌측 하단에는 아래 버튼 3개를 가로 배치한다.
- `행사적용`
- `주문 보류`
- `전체취소`

### 7.3 금액 요약
좌측 최하단에는 다음 수치를 표시한다.
- `총 매출`
- `할인금액`
- `받을 금액`

규칙:
- `받을 금액`은 가장 크게 강조한다.
- 총액/할인/최종 금액은 즉시 갱신되어야 한다.

### 7.4 우측 카테고리 탭
기본 카테고리 예시:
- `즐겨찾기`
- `채소`
- `과일`
- `생수`
- `종량제 봉투`
- `기타`

선택된 탭은 인디케이터와 강조색으로 구분한다.

### 7.5 우측 상품 그리드
- 기본 4열 카드 그리드
- 카드 표시:
  - 상품명
  - 가격
- 카드 클릭 시 장바구니에 상품 1개 추가
- 이미 존재하면 수량 증가를 기본 규칙으로 한다
  - 단, 레거시 로직이 다른 규칙을 강제하면 그것을 따른다

### 7.6 우측 하단 액션 버튼 (2행 x 3열)
기본 액션 버튼:
- 1행:
  - `장바구니할인`
  - `상품권`
  - `현금`
- 2행:
  - `기타시재`
  - `H.Point 사용`
  - `카드/모바일`

색상 그룹:
- `장바구니할인`, `기타시재`: `ACCENT_SLATE`
- `상품권`, `H.Point 사용`: `PRIMARY_GREEN`
- `현금`, `카드/모바일`: `ACCENT_BEIGE`

이 버튼 영역은 하단에 고정되어야 한다.

---

## 8. 바코드 처리 규칙

### 8.1 SUPER_HOME 에서 스캔
- 바코드 입력 완료 시 `PRODUCT_REGISTER(barcode)` 로 이동한다.

### 8.2 PRODUCT_REGISTER 에서 스캔
- 화면이 열린 상태에서도 추가 스캔을 받을 수 있어야 한다.
- 처리 순서:
  1) 바코드 입력 수신
  2) 상품 조회
  3) 상품 존재 시 장바구니에 추가
  4) 기존 행이 있으면 수량 증가
  5) 미존재 시 사용자에게 피드백 표시

### 8.3 스캐너 처리 방식
- 기존 레거시 스캐너 입력 로직이 있으면 우선 재사용한다.
- 없으면 `BarcodeScannerGateway` 로 추상화한다.
- Vendor API를 임의로 만들지 않는다.

---

## 9. 상태 관리 규칙

### 9.1 ViewModel 필수
각 화면은 필요한 ViewModel / UiState 를 사용한다.
- `SuperHomeViewModel`
- `ProductRegisterViewModel`
- 필요 시 기존 `RestaurantViewModel`, `FoodViewModel`, `PaymentViewModel` 유지/확장

### 9.2 PRODUCT_REGISTER 핵심 상태
- `cartItems`
- `selectedCategory`
- `categoryProducts`
- `totalAmount`
- `discountAmount`
- `receivedAmount`
- `barcodeInputState`
- `lastScannedBarcode`
- `snackbarMessage`

### 9.3 상태 전이
- 수량 증가/감소/삭제/전체취소/보류/할인/시재 선택은 모두 ViewModel 이벤트로 처리한다.

---

## 10. Route / 명명 규칙

### 10.1 Route
- `RESTAURANT`
- `FOOD`
- `PAYMENT`
- `SUPER_HOME`
- `PRODUCT_REGISTER`

### 10.2 ProductRegister 관련 의미
- `PRODUCT_REGISTER` = 판매용 상품등록 화면
- 절대 상품 마스터 등록 폼으로 오해하지 않는다

### 10.3 추천 UI State 이름
- `SuperHomeUiState`
- `ProductRegisterUiState`
- `CartItemUiModel`
- `TenderAction`

---

## 11. Codex 작업 규칙

Codex는 작업 시 항상 다음 순서를 따른다.

1. `PROMPT.md`, `UI_SPEC.md` 를 먼저 읽는다.
2. 관련 파일/구조를 먼저 분석한다.
3. 최소 변경으로 원하는 기능을 구현한다.
4. 기존 레거시 로직은 직접 재작성하지 않는다.
5. 필요한 경우 Adapter / Gateway 를 추가한다.
6. 변경 후 반드시 아래 명령을 실행한다.

```bash
./gradlew assembleDebug
```

7. 빌드 오류가 있으면 모두 수정한다.
8. 기존 화면(Restaurant / Food / Payment)의 디자인 언어를 깨지 않는다.

---

## 12. 완료 기준 (Definition of Done)

아래 조건을 만족하면 해당 작업은 완료로 본다.

- 빌드가 성공한다 (`assembleDebug` 통과)
- Navigation 이 정상 동작한다
- `SUPER_HOME -> PRODUCT_REGISTER()` 동작한다
- `SUPER_HOME -> PRODUCT_REGISTER(barcode)` 동작한다
- `PRODUCT_REGISTER` 에서 상품 추가/수량변경/삭제가 즉시 반영된다
- 금액 요약이 즉시 갱신된다
- UI가 기존 Restaurant / Food 디자인 언어와 일관된다
- 레거시 비즈니스 로직을 무분별하게 재작성하지 않았다

---

## 13. 모호할 때의 처리 원칙

- 레거시 소스 구조가 애매하면 먼저 검색/분석 후 재사용 가능한 진입점을 찾는다.
- 안전하게 알 수 없는 경우:
  - 직접 추측 구현하지 않는다
  - Interface / Gateway + TODO 로 남긴다
- 우선순위는 다음과 같다.

1. 기존 로직 재사용
2. Adapter 추가
3. 신규 최소 구현
4. 추측 구현 금지
