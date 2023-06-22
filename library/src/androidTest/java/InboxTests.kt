package com.wultra.android.mtokensdk.test

import com.wultra.android.mtokensdk.inbox.*
import com.wultra.android.mtokensdk.operation.IOperationsService
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.*
import org.junit.Assert.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class InboxTests {

    lateinit var ops: IOperationsService
    lateinit var inbox: IInboxService
    private lateinit var pa: PowerAuthSDK
    val pin = "1234"

    @Before
    fun setup() {
        try {
            val result = IntegrationUtils.prepareActivation(pin)
            pa = result.first
            ops = result.second
            inbox = result.third
        } catch (e: Throwable) {
            fail("Activation preparation failed: $e")
        }
    }

    @After
    fun tearDown() {
        IntegrationUtils.removeRegistration(pa.activationIdentifier)
        pa.removeActivationLocal(IntegrationUtils.context)
    }

    @Test
    fun testInboxMessages() {
        val messagesToTest = 5
        assertEquals(0, fetchUnreadMessagesCount())

        // Now prepare messages
        val messages = IntegrationUtils.createInboxMessages(messagesToTest)
        assertEquals(messagesToTest, fetchUnreadMessagesCount())

        // Read first page
        val futureList = CompletableFuture<List<InboxMessage>>()
        inbox.getMessageList(0, 50, false) {
            it.onSuccess { futureList.complete(it) }
                .onFailure { futureList.complete(null) }
        }
        val messagesList = futureList.get(20, TimeUnit.SECONDS)
        assertNotNull(messagesList)
        compareMessages(messages, messagesList!!)

        // Now read first message's detail
        val firstMessage = messages.first()
        val futureDetail = CompletableFuture<InboxMessageDetail?>()
        inbox.getMessageDetail(firstMessage.id) {
            it.onSuccess { futureDetail.complete(it) }
                .onFailure { futureDetail.complete(null) }
        }
        val detail = futureDetail.get(20, TimeUnit.SECONDS)

        assertEquals(firstMessage.id, detail!!.id)
        assertEquals(firstMessage.subject, detail.subject)
        assertEquals(firstMessage.summary, detail.summary)
        assertEquals(firstMessage.body, detail.body)
        assertEquals(firstMessage.read, detail.read)
        assertEquals(firstMessage.type, detail.type.rawValue())
        assertEquals(firstMessage.timestamp.time / 1000, detail.timestampCreated.time / 1000)
    }

    @Test
    fun testGetAllInboxMessages() {
        val count = 11
        val messages = IntegrationUtils.createInboxMessages(count, "html")
        val futureList = CompletableFuture<List<InboxMessage>?>()
        inbox.getAllMessages {
            it.onSuccess { futureList.complete(it) }
                .onFailure { futureList.complete(null) }
        }
        val messagesList = futureList.get(20, TimeUnit.SECONDS)
        assertNotNull(messagesList)
        compareMessages(messages, messagesList!!)
    }

    @Test
    fun testMarkMessageRead() {
        val count = 4
        val messages = IntegrationUtils.createInboxMessages(count)
        val receivedMessages = fetchAllMessages()

        compareMessages(messages, receivedMessages)

        // Mark first as read and receive its detail
        val messageId = receivedMessages.first().id
        val markReadCompletion = CompletableFuture<Boolean>()
        inbox.markRead(messageId) {
            it.onSuccess { markReadCompletion.complete(true) }
                .onFailure { markReadCompletion.complete(false) }
        }
        val markRead = markReadCompletion.get(20, TimeUnit.SECONDS)
        assertTrue(markRead)

        // Now get message detail
        val messageDetailCompletion = CompletableFuture<InboxMessageDetail?>()
        inbox.getMessageDetail(messageId) {
            it.onSuccess { messageDetailCompletion.complete(it) }
                .onFailure { messageDetailCompletion.complete(null) }
        }
        val messageDetail = messageDetailCompletion.get(20, TimeUnit.SECONDS)
        assertNotNull(messageDetail)
        assertTrue(messageDetail?.read ?: false)
    }

    @Test
    fun testMarkAllMessagesRead() {
        val count = 4
        val messages = IntegrationUtils.createInboxMessages(count)
        val receivedMessages = fetchAllMessages()

        compareMessages(messages, receivedMessages)
        val markReadCompletion = CompletableFuture<Boolean>()
        inbox.markAllRead {
            it.onSuccess { markReadCompletion.complete(true) }
                .onFailure { markReadCompletion.complete(false) }
        }
        assertTrue(markReadCompletion.get(20, TimeUnit.SECONDS))

        val allReadMessages = fetchAllMessages(false)
        val allUnreadMessages = fetchAllMessages(true)
        assertEquals(0, allUnreadMessages.count())
        assertEquals(count, allReadMessages.count())
    }

    // Helper functions

    private fun fetchUnreadMessagesCount(): Int {
        val futureCount = CompletableFuture<InboxCount?>()
        inbox.getUnreadCount {
            it.onSuccess { futureCount.complete(it) }
                .onFailure { futureCount.complete(null) }
        }
        val count = futureCount.get(20, TimeUnit.SECONDS)
        assertNotNull(count)
        return count!!.countUnread
    }

    private fun fetchAllMessages(onlyUnread: Boolean = false): List<InboxMessage> {
        val futureMessages = CompletableFuture<List<InboxMessage>?>()
        inbox.getAllMessages(onlyUnread) {
            it.onSuccess { futureMessages.complete(it) }
                .onFailure { futureMessages.complete(null) }
        }
        val messages = futureMessages.get(20, TimeUnit.SECONDS)
        assertNotNull(messages)
        return messages!!
    }

    private fun compareMessages(expected: List<NewInboxMessage>, received: List<InboxMessage>) {
        assertEquals(expected.count(), received.count())
        for (detail in expected) {
            val message = received.firstOrNull { it.id == detail.id } ?: throw Exception("Message with ID ${detail.id} not found")
            assertEquals(detail.subject, message.subject)
            assertEquals(detail.read, message.read)
            assertEquals(detail.summary, message.summary)
            assertEquals(detail.type, message.type.rawValue())
            assertEquals(detail.timestamp.time / 1000, message.timestampCreated.time / 1000)
        }
    }
}

fun InboxContentType.rawValue(): String {
    return when (this) {
        InboxContentType.TEXT -> "text"
        InboxContentType.HTML -> "html"
    }
}
