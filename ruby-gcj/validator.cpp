#include <gcj/cni.h>

#include <java/io/ByteArrayInputStream.h>
#include <java/lang/System.h>
#include <java/lang/Throwable.h>
#include <javax/xml/parsers/DocumentBuilderFactory.h>
#include <javax/xml/parsers/DocumentBuilder.h>
#include <org/w3c/dom/Document.h>

#include "DomUtils.h"

#include "ruby.h"

using namespace java::io;
using namespace java::lang;
using namespace javax::xml::parsers;
using namespace org::w3c::dom;

static VALUE vnu_Document;
static ID ID_read;

static void vnu_document_free(Document *doc) {
  DomUtils::unpin(doc);
}

static VALUE vnu_parse(VALUE self, VALUE input) {
  DocumentBuilderFactory *factory = DocumentBuilderFactory::newInstance();
  DocumentBuilder *parser = factory->newDocumentBuilder();
  
  /* read file-like objects into memory.  TODO: buffer such objects */
  if (rb_respond_to(input, ID_read))
    input = rb_funcall(input, ID_read, 0);

  try {
    jbyteArray bytes = JvNewByteArray(RSTRING(input)->len);
    memcpy(elements(bytes), RSTRING(input)->ptr, RSTRING(input)->len);
    Document *doc = parser->parse(new ByteArrayInputStream(bytes));
    DomUtils::pin(doc);
    return Data_Wrap_Struct(vnu_Document, NULL, vnu_document_free, doc);
  } catch (java::lang::Throwable *ex) {
    ex->printStackTrace();
    return Qnil;
  }

}

static VALUE vnu_document_encoding(VALUE rdoc) {
  Document *jdoc;
  Data_Get_Struct(rdoc, Document, jdoc);

  String *encoding = jdoc->getXmlEncoding();
  jint len = JvGetStringUTFLength(encoding);
  char buf[len];
  JvGetStringUTFRegion(encoding, 0, len, buf);
  return rb_str_new(buf, len);
}

typedef VALUE (ruby_method)(...);

extern "C" void Init_validator() {
  JvCreateJavaVM(NULL);
  JvAttachCurrentThread(NULL, NULL);
  JvInitClass(&System::class$);
  JvInitClass(&DocumentBuilderFactory::class$);
  JvInitClass(&DomUtils::class$);

  VALUE nu = rb_define_module("Nu");
  VALUE validator = rb_define_class_under(nu, "Validator", rb_cObject);
  rb_define_singleton_method(validator, "parse", (ruby_method*)&vnu_parse, 1);

  vnu_Document = rb_define_class_under(validator, "Document", rb_cObject);
  rb_define_method(vnu_Document, "encoding", 
    (ruby_method*)&vnu_document_encoding, 0);

  ID_read = rb_intern("read");
}
