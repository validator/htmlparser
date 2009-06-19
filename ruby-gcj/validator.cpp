#include <gcj/cni.h>

#include <java/io/FileInputStream.h>
#include <java/io/PrintStream.h>
#include <java/lang/System.h>
#include <java/lang/Throwable.h>
#include <javax/xml/parsers/DocumentBuilderFactory.h>
#include <javax/xml/parsers/DocumentBuilder.h>
#include <org/w3c/dom/Document.h>

#include "ruby.h"

extern "C" VALUE jaxp_parse(VALUE self, VALUE filename) {
  using namespace java::io;
  using namespace java::lang;
  using namespace javax::xml::parsers;
  using namespace org::w3c::dom;
     
  VALUE result = Qnil;

  JvCreateJavaVM(NULL);
  JvAttachCurrentThread(NULL, NULL);
  JvInitClass(&System::class$);
  JvInitClass(&DocumentBuilderFactory::class$);

  DocumentBuilderFactory *factory = DocumentBuilderFactory::newInstance();
  DocumentBuilder *parser = factory->newDocumentBuilder();
  
  try {
    String *name = JvNewStringUTF(RSTRING(filename)->ptr);
    InputStream *in = new FileInputStream(name);
    Document *doc = parser->parse(in);

    String *encoding = doc->getXmlEncoding();
    jint len = JvGetStringUTFLength(encoding);
    char buf[len+1];
    JvGetStringUTFRegion(encoding, 0, len, buf);
    buf[len] = '\0';
    result = rb_str_new2(buf);
  } catch (java::lang::Throwable *ex) {
    ex->printStackTrace();
  }

  JvDetachCurrentThread();

  return result;
}

typedef VALUE (ruby_method)(...);

extern "C" void Init_validator() {
  VALUE nu = rb_define_module("Nu");
  VALUE validator = rb_define_class_under(nu, "Validator", rb_cObject);
  rb_define_singleton_method(validator, "parse", (ruby_method*)&jaxp_parse, 1);
}
