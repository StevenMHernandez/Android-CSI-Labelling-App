# Android CSI Labelling App

Channel State Information (CSI) is often used to perform "Wi-Fi Sensing" and "Localization" prediction tasks.
Most often, prediction is performed through some supervised machine learning algorithms.
Supervised machine learning model require each sample to have a specific label (or classification) for training.
To facilitate training such a model with CSI data collected from the [ESP32 CSI Toolkit](https://stevenmhernandez.github.io/ESP32-CSI-Tool/), this Android app allows the user a number of methods to label CSI samples in real-time while also recording the CSI data.

## Features

* Collect and Parse CSI from an ESP32 running the [ESP32 CSI Toolkit](https://stevenmhernandez.github.io/ESP32-CSI-Tool/) (this feature uses the [Android ESP32 CSI Library](https://github.com/StevenMHernandez/Android-ESP32-CSI))
* Accurately keep track of timestamps for samples
* Real-time sample labelling (important for supervised machine learning algorithms)

## Labelling Methods

* Timer (Action changes every _t_ seconds)
* Press and Hold (Action is only recorded when the user presses a button on screen)
* Toggle (Action switches to the next state after each press-and-release)
* Manual Input (Manually type the current action into the text-input box)

## Usage

`git clone --recurse-submodules https://github.com/StevenMHernandez/Android-CSI-Labelling-App.git`

### Select Labelling Method

In `./app/src/main/AndroidManifest.xml`, uncomment the desired labelling method.

For example the below sample would select the "PressAndHold" labelling method:

```
<!-- <activity android:name=".Experiments.TimerMainActivity"> -->
<activity android:name=".Experiments.PressAndHoldMainActivity">
<!-- <activity android:name=".Experiments.ToggleMainActivity"> -->
<!-- <activity android:name=".Experiments.ManualInputMainActivity"> -->
```

### Set Labelling Parameters

In the MainActivity selected in the previous step, edit the `./app/src/main/java/com/stevenmhernandez/csi_labelling_app/.java file`

### Install App on Phone

Plug in your android device into computer, then press `run` in Android Studio. After installation, unplug smartphone and plug ESP32 into smartphone with a USB OTG cable.
