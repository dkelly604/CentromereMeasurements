# CentromereMeasurements
ImageJ plugin to measure centromeres in fluorescently labelled 4 colour channel images from a Deltavision microscope.

INSTALL

1. Ensure that the ImageJ version is at least 1.5 and the installation has Java 1.8.0_60 (64bit) installed. If not download the latest version of ImageJ bundled with Java and install it.

2. The versions can be checked by opening ImageJ and clicking Help then About ImageJ.

3. Download the latest copy of Bio-Formats into the ImageJ plugin directory

4. Create a directory in the C: drive called Temp (case sensitive)

5. Using notepad save a blank .txt file called Results.txt into the Temp directory you previously created (also case sensitive).

6. Place CentromereMeasurements.jar into the plugins directory of your ImageJ installation.

7. If everything has worked Alba_AutoOpen_RegionGrow should be in the Plugins menu.

8. Alba_AutoOpen_RegionGrow.java is the editable code for the plugin should improvements or changes be required.

USAGE

1. You will be prompted to Open DV Images. The plugin was written for 4 channel deltavision images acquired Green channel, Red Channel, Far-Red Channel and Blue Channel. It will cause problems if the channel order is changed.

2. When the Bio-Formats dialogue opens make sure that the only tick is in Split Channels, nothing else should be ticked.

3. A dialogue box opens asking for a background oval to be drawn on the Green Channel, draw the region in an area of background, do not change channel. Click OK and you will be prompted to draw another region on the red channel.

4. The plugin will automatically threshold the blue channel to select the nucleus, you will be given the option to adjust the threshold if required.

5. If more than one region is found select the correct one from the open ROI manager.

6. The measurments will be made automatically and saved to the text file you should have created in C:\Temp
