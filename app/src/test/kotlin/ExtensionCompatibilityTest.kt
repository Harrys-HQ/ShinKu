import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import java.io.File

class ExtensionCompatibilityTest {

    @Test
    fun verifyFreezeZoneClassesExist() {
        val criticalClasses = listOf(
            "eu.kanade.tachiyomi.source.Source",
            "eu.kanade.tachiyomi.source.model.SManga",
            "eu.kanade.tachiyomi.source.model.SChapter",
            "eu.kanade.tachiyomi.source.model.Page",
            "eu.kanade.tachiyomi.network.NetworkHelper",
            "eu.kanade.tachiyomi.network.JavaScriptEngine"
        )

        for (className in criticalClasses) {
            try {
                val clazz = Class.forName(className)
                assertNotNull(clazz, "Class $className could not be loaded")
            } catch (e: ClassNotFoundException) {
                throw AssertionError("CRITICAL ERROR: Class '$className' was moved or renamed! This breaks extension compatibility.", e)
            }
        }
    }

    @Test
    fun verifyFrozenPackageConsistency() {
        var root = File(".").canonicalFile
        if (!File(root, "source-api").exists() && File(root, "../source-api").exists()) {
            root = File(root, "..").canonicalFile
        }
        val sourceApiDir = File(root, "source-api/src/commonMain/kotlin/eu/kanade/tachiyomi")
        val coreCommonDir = File(root, "core/common/src/main/kotlin/eu/kanade/tachiyomi")

        val directoriesToScan = listOf(sourceApiDir, coreCommonDir)

        for (dir in directoriesToScan) {
            if (!dir.exists()) {
                throw AssertionError("CRITICAL ERROR: Frozen directory '${dir.absolutePath}' does not exist! Package structure has been changed.")
            }
            dir.walkTopDown().forEach { file ->
                if (file.isFile && (file.extension == "kt" || file.extension == "java")) {
                    val content = file.readText()
                    val expectedSubPath = file.parentFile!!.absolutePath
                        .replace("\\", "/")
                        .substringAfter("/src/")
                        .substringAfter("/kotlin/")
                        .substringAfter("/java/")
                        .replace("/", ".")

                    val packageLine = content.lineSequence()
                        .map { it.trim() }
                        .find { it.startsWith("package ") }

                    assertNotNull(packageLine, "File '${file.name}' is missing a package declaration")
                    val declaredPackage = packageLine!!.substringAfter("package ").substringBefore(";").trim()

                    if (declaredPackage != expectedSubPath) {
                        throw AssertionError("CRITICAL ERROR: File '${file.name}' in directory '${file.parent}' declares package '$declaredPackage', but is expected to declare '$expectedSubPath'. Package moved or renamed!")
                    }
                }
            }
        }
    }
}
