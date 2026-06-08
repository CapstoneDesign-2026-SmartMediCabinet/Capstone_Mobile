## ㅣ폰트 설정
---
- ui.theme 속 type.kt에서 설정
- res에 폰트 삽입, 실제 폴더 하나 만들어서 넣게 됨. 안드스튜에서 경로 만들고, 폴더로 가서 거기에 파일 넣기
- 폰트명 반드시 소문자, fontname_extrabold.otf 이런식. otf보단 ttf가 권장된다함
- val (폰트명) = FontFamily() 만들어서 여기에 실제 폰트파일,fontweight = normal 이런거 지정
- val typography 해서 여기에 일종의 템플릿 지정. 즉 제목 글꼴, 본문 글꼴 등으로 해서 bodylarge, titlelarge
이런 식으로 지정해서 진행함.

-예시 코드 : 
```kotlin
  val Typography = Typography(
   bodyLarge = TextStyle(
   fontFamily = PretendardFamily,
   fontWeight = FontWeight.Normal,
   fontSize = 16.sp,
   lineHeight = 24.sp,
   letterSpacing = 0.5.sp
   )
  ) 
  ```
## ㅣ동작 원리 : Recomposible
---
기본적으로, 안드로이드는 함수가 남아있지 않음. 객체를 그리고, 바로 사라짐.  
그래서 함수 내부 변수들은 전부 일종의 지역변수 처럼 동작하기 때문에, 이걸 기억하는 두 가지 특징이 존재함 :  
1. remember 달아서 휘발되지 않게
2. 그 바로 뒤에 {mutableStateOf(state)}달아서, 상태(state)가 변하면 값을 업데이트함. '그 부분만' 화면이 바뀜.  


## ㅣ간단한 문법 정리 : 까먹을만한 거  
---
 함수를 선언할 때, 예를 들어 C언어라면
 ```C
 int alpha = 10;  
```
 이라면, 변수타입(int) 변수명(Val) = 값(10) 이런식으로 선언하는데, 코틀린에서는
 ```kotlin
 var alpha : Int = 10;
```
 이런식으로, 변수명(Val) : 변수타입(Int) = 값(10) 이런식으로 선언함.
 그래서 "Modifier"라는 이름을 가진(note : "카오스 혹은 도타는 워크래프트3의    
 mod에서 출발했다"의 mod의 약어가 사실 modification의 약어라고 함. modify의 의미를 첨언)타입이 존재함.역할은 padding,  
 fillmaxwidth같은 수정. 이 경우 :  
 ```kotlin
var modifer : Modifier = Modifier  
```  
라고 하는데, 여기서 기본값 Modifier는 '아무것도 없음'을 나타낸다. NULL과 비슷한 용도라나 뭐라나  
뭐가 됐든 modifier는 변수명일 뿐이라서 맘대로 수정해도 되지만, 저게 국룰인 것도 있고  
무엇보다 Card()같은 곳 내부의 메서드인 modifier 같은 얘들도 있어서 주의할 필요가 있다. 구분은 색깔(파란색이면 원형 그대로)  

## ㅣ Scaffold 정리  
---
1. TopAppBar (상단 바) 고도화
   현재는 제목만 덜렁 있지만, 실전 앱이라면 여기에 여러 기능을 넣어야 합니다.
 - 상태 표시: "라즈베리 파이 연결됨/끊김" 같은 아이콘.
 - 로그아웃/설정: 사용자나 관리자가 내비게이션으로 돌아갈 수 있는 버튼.
 - 뒤로 가기: MedRegi 같은 서브 화면에서 메인으로 돌아오는 화살표.
2. Navigation 연동 (Drawer vs Rail vs BottomBar)  
   태블릿 환경이라면 화면이 넓기 때문에 일반적인 하단 바(BottomAppBar)보다는 옆에 세로로 붙는 
Navigation Rail이나 Navigation Drawer가 훨씬 프로다워 보입니다.  
관리자 메뉴 / 사용자 메뉴 / 설정 / 로그아웃 등을 전환하는 리모컨을 어디에 둘지 정해야 합니다.
3. FloatingActionButton (FAB, 플로팅 버튼)  
가장 중요한 '핵심 액션' 하나를 정해서 띄워주세요.  
예: 관리자 페이지라면 "알약 추가 등록" 버튼을 우측 하단에 띄우는 식입니다.
4. Snackbar (알림창)
   서버나 하드웨어에서 오류가 났을 때 사용자에게 살짝 띄워줄 알림 창입니다. 
스캐폴드에는 이 기능이 이미 내장되어 있어서 SnackbarHost만 설정해두면 나중에 편하게 쓸 수 있습니다. 

---
문서의 마지막.

