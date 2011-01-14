#!/bin/sh

java -cp "build/classes:build/test/classes:`echo lib/*jar | tr ' ' ':'`" org.mozilla.javascript.tools.shell.Main
