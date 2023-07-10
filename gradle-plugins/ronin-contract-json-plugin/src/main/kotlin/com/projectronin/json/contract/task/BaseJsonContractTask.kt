package com.projectronin.json.contract.task

import org.gradle.api.DefaultTask

/**
 * Base task for tasks that ensures a common group and provides additional helper functions.
 */
abstract class BaseJsonContractTask : DefaultTask() {
    init {
        group = "contracts"
    }
}
