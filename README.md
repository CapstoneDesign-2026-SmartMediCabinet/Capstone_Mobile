# 안드로이드 스튜디오의 코드 clone(내려받기)하는 방법 :  

1. 안드로이드 스튜디오에서 Github 로그인 : file-setting-Verson Control-github 에서 로그인 가능
2. VCS(Version Control System)을 Git으로 설정, 즉 맨 위 VCS-Enable Version Control integration - Git 클릭
3. VCS버튼이 Git으로 바뀜, 이 때 Git- Github - clone repository 선택
4. Repository URL 칸에 아래 주소 복붙하고 Clone 버튼 누르기
- 주소: [https://github.com/Bibagyte/Capstone_Tablet.git](https://github.com/Bibagyte/Capstone_Tablet.git)


  4-1. 또는 Github 웹사이트에서 우상단 프로필아이콘 클릭 - 설정 - Applications - Authorized OAuth apps(3번째 탭) - JetBrains IDE integration 클릭, 하단의 Organization Access의 Grant 버튼 클릭

    
  4-2. 이후 동일하게 안드로이드 스튜디오에서 3번까지 진행, 조직 내 모든 Repository가 뜸.  
  거기서 /Capstone_Tablet 으로 끝나는 Repository 선택  


# 태블릿 어플리케이션 항목

- 초기 화면 : 사용자 로그인 화면(얼굴인식)
      - 로그인 실패 화면 별도로 확보 필요
- 관리자 페이지 : 약품장 내부 약품 보관정보, 약품 등록 화면으로의 버튼, 온습도 값, 환풍장치
- 환자 페이지 : 처방받은 약품 내역, 이용 가능한 약품장만 이용가능 표기
- 약품 등록 페이지 : 관리자 화면에서 이어짐, 카메라 떠 있고 화면에 카메라 보임
- 약품 등록 성공 : 등록 정보 표기

[코드 위치 바로가기](app/src/main/java/com/inu/capstone_mobile) 












<details> 
    <summary> ConstraintLayout의 설명 </summary>
    
### 새 배치 방식  

새로운 정렬 방식의 이름은 바로 **`ConstraintLayout` (제약 레이아웃)** 입니다. 안드로이드 실무에서 복잡한 화면을 짤 때 쓰는 가장 강력한 무기인데, 이게 뭔지 딱 이해하기 쉽게 설명해 드릴게요.

### 🧱 기존 방식 (`Column`, `Row`) vs 🕸️ 새로운 방식 (`ConstraintLayout`)

*   **기존 방식 (레고 블록):** `Column`(세로 쌓기)과 `Row`(가로 쌓기)를 계속 겹겹이 포개서 화면을 만듭니다. 박스 안에 박스, 그 안에 또 박스... 화면이 복잡해지면 코드가 엄청나게 깊어지고 들여쓰기가 끝도 없이 들어갑니다.
*   **새로운 방식 (고무줄 연결):** 화면 전체를 하나의 큰 '코르크 보드판'이라고 생각하는 겁니다. 그리고 UI 요소들을 **압정으로 꽂은 뒤, 서로의 상하좌우를 고무줄로 연결해서** 위치를 잡습니다. 박스를 겹겹이 쌀 필요 없이 1층 평면도 위에서 모든 걸 해결합니다. 

---

### 🤖 AI가 짜준 코드 해석하기 (고무줄 묶는 법)

AI가 짠 코드의 핵심 작동 원리는 딱 3단계입니다.

**1. 이름표 만들기 (`createRefs`)**
```kotlin
val (title, card, divider) = createRefs()
```
*   "자, 이제 화면에 올릴 부품들 이름표(압정) 3개만 미리 만들게!" 라고 선언하는 겁니다. 아까 IDE가 인식 못해서 빨간 줄이 났던 게 바로 이 녀석들입니다.

**2. 부품에 이름표 붙이기 (`constrainAs`)**
```kotlin
Text(
    "관리자 페이지",
    modifier = Modifier.constrainAs(title) { // <--- 요기!
...
```
*   "이 Text 부품의 이름표는 앞으로 'title'이야!" 라고 지정해 줍니다.

**3. 고무줄로 서로 묶기 (`linkTo`) - 핵심! ⭐**
```kotlin
top.linkTo(parent.top, margin = 50.dp)
centerHorizontallyTo(parent)
```
*   이 부분이 제일 중요합니다.
*   `top.linkTo(parent.top)`: "내 위쪽(`top`) 고무줄을 부모 화면(`parent`)의 위쪽(`top`)에 묶어! 그리고 50.dp만큼 띄워 줘!"
*   만약 `start.linkTo(card.end)`라고 쓴다면? "내 왼쪽 끝을 저 'card' 부품의 오른쪽 끝에 묶어!"가 됩니다. 즉, 카드 바로 옆에 나란히 배치하라는 뜻이죠.

</details>

