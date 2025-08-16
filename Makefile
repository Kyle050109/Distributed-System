JAVAC = javac
JAVA = java

SOURCES = Calculator.java CalculatorImplementation.java CalculatorServer.java CalculatorClient.java MultiClientTest.java
CLASSES = $(SOURCES:.java=.class)

all: $(CLASSES)
%.class: %.java
	$(JAVAC) $<
run-server: CalculatorServer.class
	$(JAVA) CalculatorServer
run-client: CalculatorClient.class
	$(JAVA) CalculatorClient
run-multiclient: MultiClientTest.class
	$(JAVA) MultiClientTest
clean:
	rm -f *.class
