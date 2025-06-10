package com.example.nakenchat

enum class MessageType {
    NONE,
    SYSTEM,
    EMOTE,
    SENT,
    RECEIVED,
    PRIVATE_OUT,
    PRIVATE_IN
}

data class Message(
    val text: String,
    val from: String = "",
    val to: String = "",
    val fromId: Int = -1,
    val toId: Int = -1,
    val isSentByUser: Boolean = false,
    val type: MessageType = MessageType.NONE,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {

        /**
         * Parses a raw line from the server into a Message object.
         * @param line Raw input line from server.
         * @param currentUsername The username logged in on this client, to determine sent/received.
         */
        fun parseRawMessage(line: String, currentUsername: String): Message {
            val trimmed = line.trim()

            return when {
                // Private message received: <id>name:message
                trimmed.startsWith("<") -> {
                    // Format: <id>name:message body
                    val endId = trimmed.indexOf('>')
                    if (endId > 1) {
                        val fromId = trimmed.substring(1, endId).toIntOrNull() ?: -1
                        val remainder = trimmed.substring(endId + 1)
                        val splitIndex = remainder.indexOf(':')
                        if (splitIndex > 0) {
                            var fromName = remainder.substring(0, splitIndex).trim()
                            fromName = fromName.removeSuffix("(private)").trim()
                            val messageBody = remainder.substring(splitIndex + 1).trim()
                            val isFromUser = fromName.equals(currentUsername, ignoreCase = true)
                            Message(
                                text = messageBody,
                                from = fromName,
                                to = currentUsername,
                                fromId = fromId,
                                toId = -1,
                                isSentByUser = isFromUser,
                                type = MessageType.PRIVATE_IN
                            )
                        } else {
                            // fallback NONE type
                            Message(text = line, type = MessageType.NONE)
                        }
                    } else {
                        Message(text = line, type = MessageType.NONE)
                    }
                }

                // Private message sent: >> Message sent to [id]name: <id>name (private): message
                trimmed.startsWith(">> Message sent to [") -> {
                    // Extract recipient
                    val toStart = trimmed.indexOf('[')
                    val toEnd = trimmed.indexOf(']')
                    val toId = if (toStart >= 0 && toEnd > toStart) {
                        trimmed.substring(toStart + 1, toEnd).toIntOrNull() ?: -1
                    } else -1

                    val afterTo = trimmed.substring(toEnd + 1)
                    // Extract recipient name (before ':')
                    val colonIndex = afterTo.indexOf(':')
                    val toName = if (colonIndex > 0) {
                        afterTo.substring(0, colonIndex).trim()
                    } else ""

                    // Now extract sender id and name and message body after ":"
                    val privatePart = afterTo.substring(colonIndex + 1).trim()
                    // Expected: <id>name (private): message
                    val senderIdStart = privatePart.indexOf('<')
                    val senderIdEnd = privatePart.indexOf('>')
                    if (senderIdStart >= 0 && senderIdEnd > senderIdStart) {
                        val fromId = privatePart.substring(senderIdStart + 1, senderIdEnd).toIntOrNull() ?: -1
                        val afterSenderId = privatePart.substring(senderIdEnd + 1).trim()
                        // name is before "(private):"
                        val privateIndex = afterSenderId.indexOf("(private):")
                        if (privateIndex > 0) {
                            val fromName = afterSenderId.substring(0, privateIndex).trim()
                            val messageBody = afterSenderId.substring(privateIndex + "(private):".length).trim()
                            val isFromUser = fromName.equals(currentUsername, ignoreCase = true)
                            Message(
                                text = messageBody,
                                from = fromName,
                                to = toName,
                                fromId = fromId,
                                toId = toId,
                                isSentByUser = isFromUser,
                                type = MessageType.PRIVATE_OUT
                            )
                        } else {
                            Message(text = line, type = MessageType.NONE)
                        }
                    } else {
                        Message(text = line, type = MessageType.NONE)
                    }
                }

                // Emote message: (id)name message
                trimmed.startsWith("(") -> {
                    // Format: (id)name message
                    val idEnd = trimmed.indexOf(')')
                    if (idEnd > 1) {
                        val fromId = trimmed.substring(1, idEnd).toIntOrNull() ?: -1
                        val afterId = trimmed.substring(idEnd + 1).trim()
                        val spaceIndex = afterId.indexOf(' ')
                        if (spaceIndex > 0) {
                            val fromName = afterId.substring(0, spaceIndex)
                            val messageBody = afterId.substring(spaceIndex + 1).trim()
                            val isFromUser = fromName.equals(currentUsername, ignoreCase = true)
                            Message(
                                text = messageBody,
                                from = fromName,
                                fromId = fromId,
                                isSentByUser = isFromUser,
                                type = MessageType.EMOTE
                            )
                        } else {
                            Message(text = line, type = MessageType.NONE)
                        }
                    } else {
                        Message(text = line, type = MessageType.NONE)
                    }
                }

                // Sent or Received: [id]name: message
                trimmed.startsWith("[") -> {
                    val idEnd = trimmed.indexOf(']')
                    if (idEnd > 1) {
                        val fromId = trimmed.substring(1, idEnd).toIntOrNull() ?: -1
                        val afterId = trimmed.substring(idEnd + 1).trim()
                        val colonIndex = afterId.indexOf(':')
                        if (colonIndex > 0) {
                            val fromName = afterId.substring(0, colonIndex).trim()
                            val messageBody = afterId.substring(colonIndex + 1).trim()
                            val isFromUser = fromName.equals(currentUsername, ignoreCase = true)
                            val type = if (isFromUser) MessageType.SENT else MessageType.RECEIVED
                            Message(
                                text = messageBody,
                                from = fromName,
                                fromId = fromId,
                                isSentByUser = isFromUser,
                                type = type
                            )
                        } else {
                            Message(text = line, type = MessageType.NONE)
                        }
                    } else {
                        Message(text = line, type = MessageType.NONE)
                    }
                }

                // System message: >> message body
                trimmed.startsWith(">>") -> {
                    val messageBody = trimmed.substring(2).trim()
                    Message(text = messageBody, type = MessageType.SYSTEM)
                }

                // Default fallback NONE
                else -> {
                    Message(text = line, type = MessageType.NONE)
                }
            }
        }
    }
}