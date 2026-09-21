package com.vyro.app.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class UserModel(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatar: String? = null,
    val bio: String? = null,
    val coins: Long = 0,
    val followers: Long = 0,
    val following: Long = 0,
    val isFollowing: Boolean = false,
    val verified: Boolean = false
)

@Serializable
data class VideoModel(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatar: String? = null,
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val caption: String = "",
    val musicName: String? = null,
    val likes: Long = 0,
    val comments: Long = 0,
    val shares: Long = 0,
    val views: Long = 0,
    val liked: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class LiveStreamModel(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatar: String? = null,
    val title: String = "",
    val streamUrl: String? = null,
    val playbackUrl: String? = null,
    val rtmpsUrl: String? = null,
    val streamKey: String? = null,
    val viewerCount: Long = 0,
    val likes: Long = 0,
    val status: String = "active",
    val startedAt: String? = null
)

@Serializable
data class GiftModel(
    val id: String = "",
    val name: String = "",
    val price: Long = 0,
    val icon: String = "",
    val imageUrl: String? = null,
    val animationUrl: String? = null,
    val enabled: Boolean = true
)

@Serializable
data class GiftSendRequest(
    val liveId: String,
    val giftId: String,
    val quantity: Int = 1
)

@Serializable
data class GiftSendResponse(
    val success: Boolean = false,
    val message: String? = null,
    val remainingCoins: Long = 0,
    val gift: GiftModel? = null
)

@Serializable
data class CoinBalanceModel(
    val userId: String = "",
    val coins: Long = 0
)

@Serializable
data class WalletDepositRequest(
    val amount: Long,
    val walletNumber: String,
    val transactionReference: String? = null
)

@Serializable
data class WalletDepositResponse(
    val success: Boolean = false,
    val message: String? = null,
    val coins: Long = 0,
    val pending: Boolean = false
)

@Serializable
data class CreateLiveRequest(
    val title: String,
    val description: String? = null
)

@Serializable
data class CreateLiveResponse(
    val success: Boolean = false,
    val message: String? = null,
    val live: LiveStreamModel? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val displayName: String
)

@Serializable
data class AuthResponse(
    val success: Boolean = false,
    val message: String? = null,
    val token: String? = null,
    val user: UserModel? = null
)

@Serializable
data class LikeRequest(
    val videoId: String
)

@Serializable
data class FollowRequest(
    val userId: String
)

@Serializable
data class CommentRequest(
    val videoId: String,
    val text: String
)

@Serializable
data class CommentModel(
    val id: String = "",
    val videoId: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatar: String? = null,
    val text: String = "",
    val createdAt: String? = null
)

@Serializable
data class FeedResponse(
    val success: Boolean = false,
    val videos: List<VideoModel> = emptyList(),
    val message: String? = null
)

@Serializable
data class LiveListResponse(
    val success: Boolean = false,
    val lives: List<LiveStreamModel> = emptyList(),
    val message: String? = null
)

@Serializable
data class GiftsResponse(
    val success: Boolean = false,
    val gifts: List<GiftModel> = emptyList(),
    val message: String? = null
)

@Serializable
data class ProfileResponse(
    val success: Boolean = false,
    val user: UserModel? = null,
    val message: String? = null
)

@Serializable
data class GenericResponse(
    val success: Boolean = false,
    val message: String? = null
)
