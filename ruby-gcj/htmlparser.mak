deps    := ../../dependencies
icu4j   := $(deps)/icu4j-4_0.jar
chardet := $(deps)/mozilla/intl/chardet/java/dist/lib/chardet.jar
libgcj  := /usr/share/java/libgcj.jar
jaxp    := /usr/share/java/jaxp-1.3.jar

htmlparser_sources :=  DomUtils.java \
  $(shell find ../src -name '*.java' | grep -v 'xom')

all: Makefile headers lib validator.so

lib: lib/libnu-chardet.so lib/libnu-htmlparser.so lib/libnu-icu.so

# htmlparser itself (as a shared library)
lib/libnu-htmlparser.so: $(htmlparser_sources)
	@mkdir -p lib
	gcj -shared --classpath=$(icu4j):$(chardet):$(jaxp) -fPIC -o $@ $^

lib/libnu-chardet.so: $(chardet)
	@mkdir -p lib
	gcj -shared -fPIC -o $@ $<

lib/libnu-icu.so: $(icu4j)
	@mkdir -p lib
	gcj -shared -fPIC -o $@ $<

tests: all
	LD_LIBRARY_PATH=lib ruby test/domencoding.rb test/greek.xml

headers: headers/DomUtils.h \
  headers/nu/validator/htmlparser/dom/HtmlDocumentBuilder.h

headers/DomUtils.h: DomUtils.java
	@mkdir -p classes headers
	javac -d classes $<
	gcjh -o $@ -cp $(libgcj):classes DomUtils

headers/nu/validator/htmlparser/dom/HtmlDocumentBuilder.h: \
 ../src/nu/validator/htmlparser/dom/HtmlDocumentBuilder.java
	@mkdir -p classes headers
	javac -cp $(icu4j):$(chardet) -d classes -sourcepath ../src $<
	gcjh -cp $(icu4j):$(chardet):$(jaxp):$(libgcj):classes -o $@ \
 	  nu.validator.htmlparser.dom.HtmlDocumentBuilder

nu/validator.so: validator.so
	@mkdir -p nu
	ln -s -t nu ../validator.so
	
Makefile: lib
	ruby extconf.rb --with-gcj=${libgcj} --with-jaxp=${jaxp}

validator.so: validator.cpp headers/DomUtils.h
	make

clean:
	rm -rf classes headers lib nu Makefile mkmf.log *.o *.so
