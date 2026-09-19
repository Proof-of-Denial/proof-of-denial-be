plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "pod"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("com.anthropic:anthropic-java:2.34.0")
	implementation(platform("org.xrpl:xrpl4j-bom:5.0.0"))
	implementation("org.xrpl:xrpl4j-client")
	implementation("org.xrpl:xrpl4j-core")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

springBoot {
	mainClass.set("pod.ProofOfDenialApplicationKt")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	// XRPL 테스트넷 faucet(faucet.altnet.rippletest.net)이 TLS 핸드셰이크에서 중간 인증서를 보내지 않는다.
	// curl/브라우저는 OS가 캐시해둔 중간 인증서로 넘어가지만 JVM 기본 검증기는 그게 없으면 막힌다.
	// AIA(Authority Info Access)로 빠진 인증서를 받아오게 허용 — 검증 자체를 끄는 게 아니라 체인을 완성해줄 뿐이다.
	jvmArgs("-Dcom.sun.security.enableAIAcaIssuers=true")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<JavaExec>("keygen") {
	group = "pod"
	description = "Ed25519 키 한 쌍 생성 → keys/"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("pod.app.interfaces.cli.KeygenCliKt")
	args = listOf("keys")
}

tasks.register<JavaExec>("verifyLedger") {
	group = "pod"
	description = "장부 검증. --args=\"data/ledger.jsonl keys/ed25519.public [--seq N] [--expect-head HASH]\""
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("pod.app.interfaces.cli.VerifyCliKt")
	// CLI는 문제를 찾으면 exit 1로 끝난다. Gradle이 그걸 태스크 실패로 보고 "FAILURE: Build failed" 블록을
	// 결과 줄 아래에 덧붙이는데, 그러면 데모 화면에서 정작 봐야 할 "결과: 문제 N건"이 묻힌다.
	isIgnoreExitValue = true
	// --anchor-tx로 XRPL 공개 RPC(s.altnet.rippletest.net)를 조회할 때도 bootRun과 같은 TLS 중간 인증서
	// 문제가 난다. 이유는 위 bootRun 설정 주석과 동일 — 검증을 끄는 게 아니라 체인을 완성해줄 뿐이다.
	jvmArgs("-Dcom.sun.security.enableAIAcaIssuers=true")
}
