
rootProject.name = "hexagon_extra"

include(
    // Utility modules
    "args",
    "converters",
    "scheduler",
    "models",
    "injection",
    "terminal",

    // Ports
    "store",

    // Adapters
    "store_mongodb",

    // Tools
    "dokka_json",
    "processor",

    // Testing
    "application_test",
)
