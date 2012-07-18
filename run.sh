#!/bin/bash

# ABOUT THIS SCRIPT
#
# This script runs leafChat from the developer install if you have
# it set up in Eclipse or a similar editor. That means you can just
# edit the code and run it again, rather than having to build the
# distributable code.

# It is usually easier to run it directly from your IDE, but this
# script can be useful if running on a secondary computer or
# something. (Also, if you're having trouble with running from your
# IDE, maybe this script will help you figure out what you're
# missing.)
#
# PREREQUISITES:
# 1. This script only works on a Unix-like system.
# 2. You must use an IDE that has already built the code from all
#    leafChat source into the 'bin' folder, including copies of any
#    data files from within the source folders.
#
# FIRST RUN:
# 1. If you run this and it complains about not being able to find
#    a theme, find your leafChat settings directory (.leafChat on
#    Linux, Library/Application Data/leafChat on Mac); it should
#    have a 'themes' subdirectory. Grab *.leafChatTheme from the
#    program distribution.
#
# REQUIRED SETTINGS:
# 1. Set LEAFCHAT_INSTALLATION to the location of a leafChat installation
#    (one way to get this is to unpack the normal distribution zip file).
#    export LEAFCHAT_INSTALLATION=/Applications/leafChat.app

if [ ! -e "src/leafchat/startup/IDEStartupHandler.java" ]
then
	echo "You must run this script from the project root. See script."
	exit
fi

if [ ! -e "bin/leafchat/startup/IDEStartupHandler.class" ]
then
	echo "You must use an IDE to compile to the 'bin' folder. See script."
	exit
fi

if [ ! -e "bin/leafchat/startup/splash.jpg" ]
then
	echo "You must use an IDE that copies data files to 'bin' folder. See script."
	exit
fi

if [ ! -d "$LEAFCHAT_INSTALLATION" ]
then
	echo "Please set the environment variable LEAFCHAT_INSTALLATION. See script."
	exit
fi

# Get list of all library files to include in classpath.
LIBS=$(find lib -name "*.jar" | perl -e "while(<>) {chomp; print $_; print ':'; }") 

# Run Java with given options and leafChat's IDE startup class.
java -cp ${LIBS}bin -Dleafchat.installation=${LEAFCHAT_INSTALLATION} leafchat.startup.IDEStartupHandler