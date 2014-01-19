# All you really need is a .jar file containing all the class files.
default: StenoTray.jar

# You may not need the -D, but they are needed for some users.
run: StenoTray.jar
	java -Djava.ext.dirs=. -Djava.library.path=. StenoTray

.SUFFIXES: .java .class

# Build jar files from class files.
StenoTray.jar: *.class
	jar cfm StenoTray.jar StenoTray.manifest *.class

# No special options should be necessary.
.java.class: $<
	javac $<
