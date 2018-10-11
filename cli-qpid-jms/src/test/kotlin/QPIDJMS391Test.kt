/*
 * Copyright (c) 2018 Red Hat, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.truth.Truth.assertThat
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.spi.LoggingEvent
import org.apache.qpid.jms.transports.TransportSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.Session

@Tags(Tag("issue"), Tag("external"))
class QPIDJMS391Test {
    val prefix: String = "QPIDJMS357Test_"
    lateinit var randomSuffix: String
    val address: String
        get() = prefix + randomSuffix

    val random = Random()

    @BeforeEach
    fun setUp() {
        // https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
        randomSuffix = BigInteger(130, random).toString(32)
    }

    class Client {
        val INDIVIDUAL_ACKNOWLEDGE = 101

        lateinit var f: ConnectionFactory
        lateinit var c: Connection
        lateinit var s: Session

        fun start() {
            f = org.apache.qpid.jms.JmsConnectionFactory(
                "amqp://127.0.0.1:5672")
            c = f.createConnection()
            c.start()
            s = c.createSession(false, INDIVIDUAL_ACKNOWLEDGE)
        }

        fun stop() {
            s.close()
            c.stop()
            c.close()
        }
    }

    @Test
    fun `xxx`() {
        val ala = ArrayListAppender()
        LogManager.getLogger(TransportSupport::class.java).let {
            it.level = Level.DEBUG
            it.addAppender(ala)
        }

        TransportSupport::class.java
        // the config option is only used when we create actual ssl connection
        val f: ConnectionFactory = org.apache.qpid.jms.JmsConnectionFactory(
            "amqps://127.0.0.1:5673?transport.useOpenSSL=true&transport.trustAll=true&transport.verifyHost=false")
        val c: Connection = f.createConnection()
        c.start()
        val s: Session = c.createSession(Session.AUTO_ACKNOWLEDGE)
        s.close()
        c.close()

        assertThat(ala.loggingEvents.any {
            // println(it.renderedMessage)
            it.renderedMessage.matches(
                Regex.fromLiteral("OpenSSL Enabled: Version .* of OpenSSL will be used"))
        }).isTrue()
    }
}

class ArrayListAppender : AppenderSkeleton() {
    val loggingEvents = ArrayList<LoggingEvent>()

    override fun requiresLayout(): Boolean = false

    override fun append(loggingEvent: LoggingEvent) {
        loggingEvents.add(loggingEvent)
    }

    override fun close() = Unit
}
