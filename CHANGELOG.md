# Changelog

## [0.2.0](https://github.com/Nexters/ALIGNER-SERVER/compare/aligner-server-v0.1.0...aligner-server-v0.2.0) (2026-08-16)


### ✨ 신규 기능 (Features)

* Spring Boot Actuator 추가 및 완전 은닉 헬스체크 설정 ([#57](https://github.com/Nexters/ALIGNER-SERVER/issues/57)) ([e19ea24](https://github.com/Nexters/ALIGNER-SERVER/commit/e19ea24b5c89ab6524e0796a66fb12465ecbcaa2))

## 0.1.0 (2026-08-15)


### ✨ 신규 기능 (Features)

* bodyPartCode 를 API 경계에서 enum 으로 고정 ([#37](https://github.com/Nexters/ALIGNER-SERVER/issues/37)) ([4e51ba1](https://github.com/Nexters/ALIGNER-SERVER/commit/4e51ba134f41c7dd4194522fdc6e5d25c31be85e)), closes [#36](https://github.com/Nexters/ALIGNER-SERVER/issues/36)
* catalog 도메인 구축 — 운동·목표 자세·근육 ([#10](https://github.com/Nexters/ALIGNER-SERVER/issues/10)) ([dc17b36](https://github.com/Nexters/ALIGNER-SERVER/commit/dc17b3635e4926e085dae341b147fc4f6bb308f7))
* course 도메인 구축 — 코스 처방·오늘의 코스·진행도·도장 ([#26](https://github.com/Nexters/ALIGNER-SERVER/issues/26)) ([5ce43bf](https://github.com/Nexters/ALIGNER-SERVER/commit/5ce43bf1159f4ea72de53f2c82f9fd351b0181e3))
* K3s GitOps 배포 계약 연결 및 멀티 도큐먼트 프로필 체계 구축 ([#53](https://github.com/Nexters/ALIGNER-SERVER/issues/53)) ([aced7f0](https://github.com/Nexters/ALIGNER-SERVER/commit/aced7f0d08f31fe8fd25a5686f2cd74e1e9080d6))
* member 도메인과 카카오 로그인 구축 ([#6](https://github.com/Nexters/ALIGNER-SERVER/issues/6)) ([73b2ea3](https://github.com/Nexters/ALIGNER-SERVER/commit/73b2ea3432550cc39b3cd35d721b9ececad65ed3))
* member 에 온보딩 입력값과 회원탈퇴 추가 ([#22](https://github.com/Nexters/ALIGNER-SERVER/issues/22)) ([c41c176](https://github.com/Nexters/ALIGNER-SERVER/commit/c41c17690f29543f091c905a203e62d92631f1ea))
* screening 도메인 구축 — 부위·자세 체감 선택·원인 판별 ([#16](https://github.com/Nexters/ALIGNER-SERVER/issues/16)) ([7ab0a01](https://github.com/Nexters/ALIGNER-SERVER/commit/7ab0a018955a203e22ac7218ce33ee5b6bdb93c2))
* Swagger(OpenAPI) 문서 등록 — springdoc 동작시키기 ([#13](https://github.com/Nexters/ALIGNER-SERVER/issues/13)) ([63d2fd5](https://github.com/Nexters/ALIGNER-SERVER/commit/63d2fd53e7b63e931680c39c0db64591eb8f2da5))
* training 도메인 구축 — 세션 시작·수행 기록·완료 push ([#28](https://github.com/Nexters/ALIGNER-SERVER/issues/28)) ([d82b33a](https://github.com/Nexters/ALIGNER-SERVER/commit/d82b33aaebd982e2732da8686daa029c2b879fcd))
* YMove 연동 — 재생 URL·썸네일·음성 큐 대본 ([#47](https://github.com/Nexters/ALIGNER-SERVER/issues/47)) ([780d489](https://github.com/Nexters/ALIGNER-SERVER/commit/780d48935049e6c35888266928f871b79e18ab17))
* 근육맵을 앞·뒤로 나누고 운동에 분류 추가 ([#24](https://github.com/Nexters/ALIGNER-SERVER/issues/24)) ([d0a1a1c](https://github.com/Nexters/ALIGNER-SERVER/commit/d0a1a1c0a4e7083c9dc06a946b51d327bcdd0f6d))
* 디자인 스펙이 요구하는 계약을 실제 API 에 반영 ([#35](https://github.com/Nexters/ALIGNER-SERVER/issues/35)) ([cacec8b](https://github.com/Nexters/ALIGNER-SERVER/commit/cacec8bc6ebbb76500619700885ddd2a6b0207f4))
* 빌드 기반 골격 구축 ([#4](https://github.com/Nexters/ALIGNER-SERVER/issues/4)) ([cf78c33](https://github.com/Nexters/ALIGNER-SERVER/commit/cf78c33fd259e6874d4db9413ea24460977c298f))
* 오늘 코스를 완주하면 홈에 내일 운동 미리보기를 내린다 ([#49](https://github.com/Nexters/ALIGNER-SERVER/issues/49)) ([1b2c96a](https://github.com/Nexters/ALIGNER-SERVER/commit/1b2c96a000e221a064529517bd3ba0a50dfd821b))
* 콘텐츠 정본을 seed 로 옮긴다 — 자세·운동·부위·코스 템플릿 ([#44](https://github.com/Nexters/ALIGNER-SERVER/issues/44)) ([ef37282](https://github.com/Nexters/ALIGNER-SERVER/commit/ef3728235f380757f703a2b1ecfaef23972e1cb6)), closes [#43](https://github.com/Nexters/ALIGNER-SERVER/issues/43)
* 파이어로그 — 핀포즈별 완주 4회를 기록하고 완료 리포트에 싣는다 ([#42](https://github.com/Nexters/ALIGNER-SERVER/issues/42)) ([07fd2d4](https://github.com/Nexters/ALIGNER-SERVER/commit/07fd2d4e1605a4b86dc71637d433c49f5a33a51f))
* 프론트 오리진 CORS 허용 설정 추가 ([#18](https://github.com/Nexters/ALIGNER-SERVER/issues/18)) ([2f5d207](https://github.com/Nexters/ALIGNER-SERVER/commit/2f5d207a2219f3414e728d3068bc790e8e3cf683))


### 🐛 버그 수정 (Bug Fixes)

* 남의 코스 조회를 막고 세션 완료의 동시성 결함을 없앤다 ([#45](https://github.com/Nexters/ALIGNER-SERVER/issues/45)) ([52956f6](https://github.com/Nexters/ALIGNER-SERVER/commit/52956f63a327d18fb1beb40864bbb5ca9ca9da2f)), closes [#32](https://github.com/Nexters/ALIGNER-SERVER/issues/32)
* 온보딩 순서를 와이어프레임에 맞춰 부위 선입력 제거 ([#21](https://github.com/Nexters/ALIGNER-SERVER/issues/21)) ([17e72fd](https://github.com/Nexters/ALIGNER-SERVER/commit/17e72fd3e8f0b44acf9645022b1ec4040459a8a5))
* 카카오 로그인을 인가 코드 플로우로 전환 ([#14](https://github.com/Nexters/ALIGNER-SERVER/issues/14)) ([48c138c](https://github.com/Nexters/ALIGNER-SERVER/commit/48c138cad72cb83b44d7a5a7aab95499a4025f39))


### 📝 문서 (Documentation)

* readme 추가 ([94f9e4c](https://github.com/Nexters/ALIGNER-SERVER/commit/94f9e4ce2003ef5cb0ef5c5101ff0e0bbefa815c))
* 와이어프레임·콘텐츠 정본에 맞춰 도메인 설계 정합화 ([#9](https://github.com/Nexters/ALIGNER-SERVER/issues/9)) ([5b5deb4](https://github.com/Nexters/ALIGNER-SERVER/commit/5b5deb4256b5b83aae49631f1799e52706d6e242))


### 🔧 빌드 및 설정 (Maintenance)

* 실시간 컨테이너 로그 확인을 위한 Dozzle 연동 및 배포 워크플로우 반영 ([#51](https://github.com/Nexters/ALIGNER-SERVER/issues/51)) ([30b5f20](https://github.com/Nexters/ALIGNER-SERVER/commit/30b5f206ebee9eda9f538eaadbb857557bab2dbb))
* 첫 릴리즈 v0.1.0 생성을 위해 기준 버전을 0.0.0으로 조정 ([dd7e6e6](https://github.com/Nexters/ALIGNER-SERVER/commit/dd7e6e6d60fc1dce3d46351b78493a791febe2de))
* 프로젝트 초기 설정과 에이전트 하네스 구축 ([#2](https://github.com/Nexters/ALIGNER-SERVER/issues/2)) ([54c4c01](https://github.com/Nexters/ALIGNER-SERVER/commit/54c4c0173beb79183a784f0b37f895a96c25e9e6))
