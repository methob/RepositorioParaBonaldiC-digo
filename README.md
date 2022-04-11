# RepositorioParaBonaldiC-digo

Thanks for taking the time for reporting an issue!

**Describe what happened**
Include any error message or stack trace if available.

i'm getting this error:

Fatal Exception: java.lang.RuntimeException: Unable to resume activity {mypackage/o.setVerticalOffset}: java.lang.NullPointerException: Attempt to invoke virtual method 'int android.os.Parcel.dataSize()' on a null object reference

Caused by java.lang.NullPointerException: Attempt to invoke virtual method 'int android.os.Parcel.dataSize()' on a null object reference
       at android.os.BaseBundle.<init>(BaseBundle.java:164)
       at android.os.Bundle.<init>(Bundle.java:106)
       at android.content.Intent.getExtras(Intent.java:6622)
       at com.datadog.android.rum.tracking.ActivityViewTrackingStrategy.onActivityResumed(ActivityViewTrackingStrategy.java:56)
       at com.datadog.android.rum.tracking.MixedViewTrackingStrategy.onActivityResumed(MixedViewTrackingStrategy.java:71)
       at android.app.Application.dispatchActivityResumed(Application.java:219)
       at android.app.Activity.onResume(Activity.java:1264)
       at androidx.fragment.app.FragmentActivity.onResume(FragmentActivity.java:434)

in my project we use dexguard

mapping file line: mypackage.presentation.HomeCardsChartAdapter -> o.setVerticalOffset:

**Steps to reproduce the issue:**

I don't know how to reproduce it, it's a recurring error that occurs with few users on some devices

**Describe what you expected:**

**Additional context**
 - Android OS version: Android 5,6,7
 - Device Model: Galaxy J5, LG K11
 - Datadog SDK version: 1.11.0
 - Versions of any other relevant dependencies (OkHttp, …): 
 - Proguard configuration: 
 - Gradle Plugins:

