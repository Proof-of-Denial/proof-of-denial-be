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
}
