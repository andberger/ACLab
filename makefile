JCC = javac
JFLAGS = -g
JAR = jar
JARFLAGS = -cvf

default: all

all:
	$(JCC) $(JFLAGS) PrintClient.java
	$(JCC) $(JFLAGS) PrintServer.java
	$(JCC) $(JFLAGS) Printerface.java
	$(JAR) $(JARFLAGS) Printerface.jar Printerface.class

run-client:
	java -Djava.security.policy=client.policy PrintClient $(username) $(password)

run-server:
	java -Djava.rmi.server.codebase=Printerface.jar -Djava.rmi.server.hostname=127.0.0.1 -Djava.security.policy=server.policy PrintServer

clean: 
	$(RM) *.class

