# This script looks for exceptions in the Tomcat log
# Any exceptions it finds are emailed to selected email addresses

import sys
import os
import smtplib
from email.mime.text import MIMEText

# Constants
LAST_LINE_FILE = "lastLine.txt"
SEARCHING_MODE = 0
EXCEPTION_MODE = 1

# Global variables (from command line paramters)
logFile = None
emailHost = None
emailSender = None
emailRecipients = []


# Other top-level variables
lastLine = 0
exceptions = []

# Process the command line arguments
def processCommandLine ():
    global logFile, emailHost, emailSender, emailRecipients

    if len(sys.argv) < 5:
        print "Usage: extract_exception.py logFile smtpHost" + \
            " emailSenderAddress emailRecipient [emailRecipient...]"
        quit()
    else:
        logFile = sys.argv[1]
        if not os.access(logFile, os.R_OK):
            print "Log file does not exist or cannot be read"
            quit()

        emailHost = sys.argv[2]
        emailSender = sys.argv[3]

        for i in xrange(4, len(sys.argv)):
            emailRecipients.append(sys.argv[i])


# The last line we processed is stored in a file.
# These are two functions to load and store it
def loadLastLine ():
    lastLine = 0

    if os.path.isdir(LAST_LINE_FILE):
        print "Last line file is a directory!"
        quit()
    elif os.path.isfile(LAST_LINE_FILE):
        if not os.access(LAST_LINE_FILE, os.R_OK | os.W_OK):
            print "Cannot access last line file for read/write"
            quit()
        else:
            llf = None

            try:
                llf = open(LAST_LINE_FILE, "r")
                lastLine = int(llf.read())
                llf.close()
            except (TypeError, ValueError):
                print("Last line file does not contain an integer")
                quit()

    return lastLine

def saveLastLine (lastLine):
    if (os.path.isfile(LAST_LINE_FILE) 
            and not os.access(LAST_LINE_FILE, os.W_OK)):
        
        print "Cannot access last line file for writing"
        quit()
    else:
        llf = open(LAST_LINE_FILE, "w")
        llf.truncate()
        llf.write(str(lastLine))
        llf.close()

# Read the log file and return it as an array of lines
def getFileLines (fileName):
    contents = None
    lines = None

    if not os.access(fileName, os.R_OK):
        print "Log file does not exist or cannot be read"
        quit()
    else:
        file = open(fileName, "r")
        contents = file.read()
        file.close()

    if contents is not None:
        lines = contents.splitlines()

    return lines

#######################################################
# Here we go

# Initialise
processCommandLine()
lastLine = loadLastLine()

# Load the log file
logData = getFileLines(logFile)

# If the last line is larger than the file, we start again
if lastLine > len(logData):
    lastLine = -1

# Work our way through the file
currentLine = lastLine + 1
mode = SEARCHING_MODE
exception = None

while currentLine < len(logData):

    line = logData[currentLine]
    
    # Currently processing an exception
    if mode == EXCEPTION_MODE:

        # Continued exceptions start with "at" or
        # "Caused by" (trimmed strings)
        trimmed = line.lstrip()
        if trimmed.startswith("at") or trimmed.startswith("Caused by"):
            exception.append(line)
        else:
            exceptions.append("\n".join(exception))
            mode = SEARCHING_MODE

    else: # We are searching for an exception
        
        # If we see the word Exception, start a new Exception
        if line.find("Exception") > -1:
            exception = [line]
            mode = EXCEPTION_MODE



    # Go to the next line
    currentLine = currentLine + 1

# Tidy up the last exception, if that was the last thing in the file
if mode == EXCEPTION_MODE:
    exceptions.append("\n".join(exception))



if len(exceptions) > 0:

    # Send an email with the exceptions we found
    emailBody = "Exceptions found in " + logFile

    for exception in exceptions:
        emailBody = emailBody + "\n\n"
        emailBody = emailBody + exception

    email = smtplib.SMTP(emailHost)
    msg = MIMEText(emailBody)
    msg["Subject"] = "QuinCe Exceptions"
    msg["From"] = emailSender
    msg["To"] = ", ".join(emailRecipients)
    email.sendmail(emailSender, emailRecipients, msg.as_string())


# Store the last processed line ready for next time
saveLastLine(currentLine - 1)
