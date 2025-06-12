package com.example

import com.google.common.collect.Lists
import org.apache.commons.lang3.StringUtils

class GradleService {
    List<String> processItems(List<String> items) {
        // Using Guava library
        def reversedList = Lists.reverse(items)

        // Using Commons Lang
        return reversedList.collect { item ->
            StringUtils.capitalize(item.toLowerCase())
        }
    }

    String getVersion() {
        return '1.0.0'
    }
}
