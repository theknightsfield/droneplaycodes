# DUNI Codes

## 경로설명
+ [/api/web](https://developer.duni.io/examples/api/readlatestposition/map.html) : Open API를 활용하는 웹 기반의 예제 코드.
+ [/api/android](https://github.com/AplyPlatform/du_developer_codes/tree/master/api/android) : Open API를 활용하는 안드로이드 기반의 예제 코드.

# 안드로이드 기반 코드 - DUNI PILOT 앱.
<img src="https://code.duni.io/api/android_main.png" alt="DUNI PILOT" width="300">
<img src="https://code.duni.io/api/android_mission.png" alt="DUNI PILOT" width="300">

+ 지도를 터치하여 마크가 된 모든 장소로 드론이 방문하는 예제 앱 입니다.
+ 각 장소를 방문할때 마다 해당 장소의 좌표를 DUNI API를 호출하여 기록합니다.
+ 개발환경 - Android Studio
+ 반드시 넓은 공터에서 현재위치와 고도를 충분히 확인하고 확보한 후 테스트를 진행하세요.
+ 이 코드로 인해 발생하는 모든 책임은 사용자에게 있습니다. (드론이 남의 벤틀리 차량으로 돌격 할 수도 있습니다)

+ 코드의 수행을 위해 Manifest 파일에 Mapbox 지도 ACCESS TOKEN, DJI SDK API KEY 정보를 입력해야 합니다.        
> AndroidManifest.xml
<pre>
<code>
... meta-data
           android:name="mapbox.sdk.ACCESSTOKEN"
           android:value="MAPBOX-SDK-ACCESS-TOKEN" ...
... meta-data
           android:name="com.dji.sdk.API_KEY"
           android:value="DJI-SDK-API-KEY" ...
</code>
</pre>

# 웹 
> [DUNI Samples](http://developer.duni.io/examples/index.html) : Open API를 활용한 웹 예제가 실행되는 모습을 볼 수 있습니다.
