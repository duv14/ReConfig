/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
plugins {
    kotlin("jvm")
}

configurations.create("modApi") {
    configurations.named("api") {
        extendsFrom(this@create)
    }
}
configurations.create("modImplementation") {
    configurations.named("implementation") {
        extendsFrom(this@create)
    }
}
configurations.create("modRuntimeOnly") {
    configurations.named("runtimeOnly") {
        extendsFrom(this@create)
    }
}
configurations.create("modCompileOnly") {
    configurations.named("compileOnly") {
        extendsFrom(this@create)
    }
}
