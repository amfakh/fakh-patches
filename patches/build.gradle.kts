group = "app.fakh"

patches {
    about {
        name = "Fakh Patches"
        description = "Material You colors and a pitch black dark mode for GitHub, ad removal, downloads, privacy and Material You for LinkedIn, premium unlock and tracker removal for Radio Streaming Indonesia, premium unlock for DoDo Music, and premium unlock for Windy."
        source = "git@github.com:amfakh/fakh-patches.git"
        author = "amfakh"
        contact = "na"
        website = "na"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn("buildAndroid")

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }
}
