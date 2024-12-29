# Android NFC-Player

This is just a little project I started for my two daughters. It works similarly to TonieBox but
relies completely on your android phone and nextcloud.

## Install

You have to compile and install it on your own. Just clone the repo and install it on your phone.
If you don't know how to do this please see androids documentation for it.

## Configure

Next you need to create an app password for your nextcloud user. Go to settings, security, devices
and sessions and create a password.

Then you start the app and enter the nextcloud server URL, your user name, your newly created
app password and the folder where you will store your content.

## Upload content

Now you can start uploading content to this nextcloud folder. Just add an audio file. If you want
to show a picture on the screen you can optionally add a picture file with the same name (except for
the file extension of course).

## NFC tags
You can make NFC tags that will trigger the app and open the audio as soon as they are scanned. The
tags should have following content:

- type: `text/storyname`
- data: *The file name of your audio without the extension*
