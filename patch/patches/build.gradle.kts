group = "tw.maps"

patches {
    about {
        name = "Ungoogled Maps AA (DutchPharaoh)"
        description = "Google Maps 26.44.06.1006895251: native Android Auto navigation with two-wheeler routes. " +
            "Includes the required Dutch App Patches (JohninNL, GPLv3) Maps patches. " +
            "Fork by DutchPharaoh with Android Auto scrolling and split-screen layout fixes."
        source = "https://github.com/DutchPharaoh/Ungoogled-Maps-AA-DutchPharaoh"
        author = "DutchPharaoh (fork of shuili1013); includes Dutch App Patches by JohninNL"
        contact = "https://github.com/DutchPharaoh/Ungoogled-Maps-AA-DutchPharaoh/issues"
        website = "https://github.com/DutchPharaoh/Ungoogled-Maps-AA-DutchPharaoh"
        license = "GPL-3.0"
    }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(11) }
