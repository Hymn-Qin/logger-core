/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.slf4j.impl

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.util.ContextInitializer
import ch.qos.logback.classic.util.ContextSelectorStaticBinder
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.status.StatusUtil
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.ILoggerFactory
import org.slf4j.helpers.Util
import org.slf4j.spi.LoggerFactoryBinder

/**
 * The binding of [org.slf4j.LoggerFactory] class with an actual instance of
 * [ILoggerFactory] is performed using information returned by this class.
 *
 * @author Ceki Glc
 */
class StaticLoggerBinder private constructor() : LoggerFactoryBinder {

    override fun getLoggerFactory(): ILoggerFactory {
        if (!initialized) {
            return defaultLoggerContext
        }

        checkNotNull(contextSelectorBinder.contextSelector) { "contextSelector cannot be null. See also $NULL_CS_URL" }
        return contextSelectorBinder.contextSelector.loggerContext
    }

    override fun getLoggerFactoryClassStr(): String {
        return contextSelectorBinder.javaClass.name
    }

    companion object {

        /**
         * The unique instance of this class.
         *
         */
        /**
         * Return the singleton of this class.
         *
         * @return the StaticLoggerBinder singleton
         */
        private val singleton = StaticLoggerBinder()

        @JvmStatic
        fun getSingleton(): StaticLoggerBinder {
            return singleton
        }

        const val NULL_CS_URL = CoreConstants.CODES_URL + "#null_CS"

        private val KEY = Any()

    }
    //android-logback

    private var initialized = false
    private val defaultLoggerContext = LoggerContext()
    private val contextSelectorBinder = ContextSelectorStaticBinder.getSingleton()

    init {
        defaultLoggerContext.name = CoreConstants.DEFAULT_CONTEXT_NAME
        try {
            try {
                ContextInitializer(defaultLoggerContext).autoConfig()
            } catch (je: JoranException) {
                Util.report("Failed to auto configure default logger context", je)
            }
            // logback-292
            if (!StatusUtil.contextHasStatusListener(defaultLoggerContext)) {
                StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext)
            }
            contextSelectorBinder.init(defaultLoggerContext, KEY)
            initialized = true
        } catch (t: Exception) { // see LOGBACK-1159
            Util.report(
                "Failed to instantiate [" + LoggerContext::class.java.name
                        + "]", t
            )
        }
    }


}

/**
 * Declare the version of the SLF4J API this implementation is compiled against.
 * The value of this field is usually modified with each release.
 */
// to avoid constant folding by the compiler, this field must *not* be final
var REQUESTED_API_VERSION = "1.6.99" // !final