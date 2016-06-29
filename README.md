# SilentOnFlip
Flip device to switch to silent-mode.

# Operation
To detect if the device is flipped over, we manipulate the Accelerometer z-axis.
Therefore, when the accelerometer's z-axis reading reaches its lowest value (in the region of (~ -9, although I chose -8), "setRingerMode" is set to silent.
Additionally, to indicate that silent-mode has been invoked, a multitude of indicators have been used to notify the user including camera flash(for a brif instance), Vibrator, and notification.
A good idea would be to offoad part of the camera component algorithm onto a seperate thread to avoid frame skipping due to the resource-heavy nature of dealing with the camera.
Since all of this is handled in the service, for the activity to display the "flipped-status" (if the device is flipped over or not), desired data can be added to an intent and broadcasted over to an activity, which in turn receives and retrieves the data from the intent and displays it using a textview.

![alt tag] (http://i.imgur.com/iqi2YA9.png "App Screenshot")
