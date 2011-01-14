#!/bin/sh

java -cp "build/classes:build/test/classes:../de.sofd.util/build/classes:../de.sofd.viskit/build/classes:`find lib/ -name '*jar' -print0 | tr '\0' ':'`" org.mozilla.javascript.tools.shell.Main
