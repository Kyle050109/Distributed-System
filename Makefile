JAVAC := javac
JAVA  := java

ifeq ($(wildcard src/*.java),)
  SRC_DIR := .
else
  SRC_DIR := src
endif

SOURCES := $(wildcard $(SRC_DIR)/*.java)
CLASSES := $(SOURCES:.java=.class)
.PHONY: all
all:
	@echo "Compiling Java sources in $(SRC_DIR)/"
	$(JAVAC) $(SOURCES)

$(SRC_DIR)/%.class: $(SRC_DIR)/%.java
	$(JAVAC) $<

.PHONY: run-server
run-server: all
	@echo "Starting CalculatorServer from $(SRC_DIR)/"
	cd $(SRC_DIR) && $(JAVA) CalculatorServer
.PHONY: run-client
run-client: all
	cd $(SRC_DIR) && $(JAVA) CalculatorClient

.PHONY: run-multiclient
run-multiclient: all
	cd $(SRC_DIR) && $(JAVA) MultiClientTest

.PHONY: run-main
run-main: all
	cd $(SRC_DIR) && $(JAVA) Main
.PHONY: clean
clean:
	@echo "Cleaning class files"
	@if [ -d src ]; then rm -f src/*.class; fi
	@if [ ! -d src ]; then rm -f *.class; fi
