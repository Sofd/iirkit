#!/bin/sh

java -cp "build/classes:build/test/classes:../de.sofd.util/build/classes:../de.sofd.viskit/build/classes:`echo lib/*jar | tr ' ' ':'`" org.mozilla.javascript.tools.shell.Main
