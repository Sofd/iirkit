#!/bin/sh

java -cp "build/classes:`echo lib/*jar | tr ' ' ':'`" org.mozilla.javascript.tools.shell.Main
