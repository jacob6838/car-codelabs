package com.example.places

import android.Manifest
import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.android.cars.places.R
import java.util.Calendar

private const val ACTION_REPLY = "com.example.REPLY"
private const val ACTION_MARK_AS_READ = "com.example.MARK_AS_READ"
private const val ACTION_NOTIFY = "com.example.NOTIFY"

private const val EXTRA_CONVERSATION_ID_KEY = "conversation_id"
private const val REMOTE_INPUT_RESULT_KEY = "reply_input"

private val channel_id = "jacob_testing";

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.

 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.

 */
class MessagingService : IntentService("MessagingService") {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MessagingService = this@MessagingService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onHandleIntent(intent: Intent?) {
            // Fetches internal data.
//                val conversationId = intent!!.getIntExtra(EXTRA_CONVERSATION_ID_KEY, -1)



            // Handles the action that was requested in the intent. The TODOs
            // are addressed in a later section.
            when (intent?.action) {
                ACTION_REPLY -> {
                    // Extracts reply response from the intent using the same key that the
                    // RemoteInput uses.
                    val results: Bundle = RemoteInput.getResultsFromIntent(intent)!!
                    val message = results.getString(REMOTE_INPUT_RESULT_KEY)


                    // This conversation object comes from the MessagingService.
                    print("ACTION_REPLY: $message")
                }
                ACTION_MARK_AS_READ -> print("ACTION_MARK_AS_READ")
                ACTION_NOTIFY -> notify(this, 1)
            }
    }

    /**
     * Creates a [RemoteInput] that lets remote apps provide a response string
     * to the underlying [Intent] within a [PendingIntent].
     */
    fun createReplyRemoteInput(context: Context): RemoteInput {
        // RemoteInput.Builder accepts a single parameter: the key to use to store
        // the response in.
        return RemoteInput.Builder(REMOTE_INPUT_RESULT_KEY).build()
        // Note that the RemoteInput has no knowledge of the conversation. This is
        // because the data for the RemoteInput is bound to the reply Intent using
        // static methods in the RemoteInput class.
    }

    /** Creates an [Intent] that handles replying to the given [appConversation]. */
    fun createReplyIntent(
        context: Context, notificationId: Int): Intent {
        // Creates the intent backed by the MessagingService.
        val intent = Intent(context, MessagingService::class.java)

        // Lets the MessagingService know this is a reply request.
        intent.action = ACTION_REPLY

        // Provides the ID of the conversation that the reply applies to.
        intent.putExtra(EXTRA_CONVERSATION_ID_KEY, notificationId)

        return intent
    }

    /** Creates an [Intent] that handles marking the [appConversation] as read. */
    fun createMarkAsReadIntent(
        context: Context, notificationId: Int): Intent {
        val intent = Intent(context, MessagingService::class.java)
        intent.action = ACTION_MARK_AS_READ
        intent.putExtra(EXTRA_CONVERSATION_ID_KEY, notificationId)
        return intent
    }

    fun createMarkAsReadAction(
        context: Context, notificationId: Int): NotificationCompat.Action {
        val markAsReadIntent = createMarkAsReadIntent(context, notificationId)
        val markAsReadPendingIntent = PendingIntent.getService(
            context,
            12345, // Method explained below.
            markAsReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT  or PendingIntent.FLAG_IMMUTABLE)
        val markAsReadAction = NotificationCompat.Action.Builder(
            R.drawable.baseline_cancel_24, "Mark as Read", markAsReadPendingIntent)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
        return markAsReadAction
    }

    fun createReplyAction(
        context: Context, notificationId: Int): NotificationCompat.Action {
        val replyIntent: Intent = createReplyIntent(context, notificationId)

        val replyPendingIntent = PendingIntent.getService(
            context,
            12345, // Method explained later.
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val replyAction = NotificationCompat.Action.Builder(R.drawable.baseline_reply_24, "Reply", replyPendingIntent)
            // Provides context to what firing the Action does.
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)

            // The action doesn't show any UI, as required by Android Auto.
            .setShowsUserInterface(false)

            // Don't forget the reply RemoteInput. Android Auto will use this to
            // make a system call that will add the response string into
            // the reply intent so it can be extracted by the messaging app.
            .addRemoteInput(createReplyRemoteInput(context))
            .build()

        return replyAction
    }

    fun createMessagingStyle(
        context: Context, notificationId: Int): NotificationCompat.MessagingStyle {
        // Method defined by the messaging app.
        val appDeviceUser: Int = 1234

        val devicePerson = Person.Builder()
            // The display name (also the name that's read aloud in Android auto).
            .setName(appDeviceUser.toString())

            // The icon to show in the notification shade in the system UI (outside
            // of Android Auto).
            .setIcon(IconCompat.createWithResource(context, R.drawable.baseline_verified_user_24))

            // A unique key in case there are multiple people in this conversation with
            // the same name.
            .setKey(appDeviceUser.toString())
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(devicePerson)

        // Sets the conversation title. If the app's target version is lower
        // than P, this will automatically mark the conversation as a group (to
        // maintain backward compatibility). Use `setGroupConversation` after
        // setting the conversation title to explicitly override this behavior. See
        // the documentation for more information.
        messagingStyle.setConversationTitle(appDeviceUser.toString())

        // Group conversation means there is more than 1 recipient, so set it as such.
        messagingStyle.setGroupConversation(false)

        val senderPerson = Person.Builder()
            .setName(appDeviceUser.toString())
            .setIcon(IconCompat.createWithResource(context, R.drawable.baseline_verified_user_24))
            .setKey(appDeviceUser.toString())
            .build()

        // Adds the message. More complex messages, like images,
        // can be created and added by instantiating the MessagingStyle.Message
        // class directly. See documentation for details.
        messagingStyle.addMessage(
            "Test Message", Calendar.getInstance().time.toInstant().toEpochMilli(), senderPerson)

        return messagingStyle
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun notify(context: Context, notificationId: Int) {
        // Creates the actions and MessagingStyle.
        val replyAction = createReplyAction(context, notificationId)
        val markAsReadAction = createMarkAsReadAction(context, notificationId)
        val messagingStyle = createMessagingStyle(context, notificationId)

        // Creates the notification.
        val notification = NotificationCompat.Builder(context, channel_id)
            // A required field for the Android UI.
            .setSmallIcon(R.drawable.baseline_notification_important_24)
            .setCategory(Notification.CATEGORY_MESSAGE)

            // Shows in Android Auto as the conversation image.
            .setLargeIcon(drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.baseline_donut_large_24)!!))

            // Adds MessagingStyle.
            .setStyle(messagingStyle)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)

            // Adds reply action.
            .addAction(replyAction)

            // Makes the mark-as-read action invisible, so it doesn't appear
            // in the Android UI but the app satisfies Android Auto's
            // mark-as-read Action requirement. Both required actions can be made
            // visible or invisible; it is a stylistic choice.
            .addInvisibleAction(markAsReadAction)

            .build()

        // Posts the notification for the user to see.
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManagerCompat.notify(notificationId, notification)
    }

}