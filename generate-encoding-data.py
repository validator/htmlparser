#!/usr/bin/python

# Copyright (c) 2013-2015 Mozilla Foundation
#
# Permission is hereby granted, free of charge, to any person obtaining a 
# copy of this software and associated documentation files (the "Software"), 
# to deal in the Software without restriction, including without limitation 
# the rights to use, copy, modify, merge, publish, distribute, sublicense, 
# and/or sell copies of the Software, and to permit persons to whom the 
# Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in 
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
# DEALINGS IN THE SOFTWARE.

import json

class Label:
  def __init__(self, label, preferred):
    self.label = label
    self.preferred = preferred
  def __cmp__(self, other):
    return cmp(self.label, other.label)

# If a multi-byte encoding is on this list, it is assumed to have a
# non-generated decoder implementation class. Otherwise, the JDK default
# decoder is used as a placeholder.
MULTIBYTE_DECODER_IMPLEMENTED = [
  u"x-user-defined",
  u"replacement",
  u"big5",
]

preferred = []

labels = []

data = json.load(open("../encoding/encodings.json", "r"))

indexes = json.load(open("../encoding/indexes.json", "r"))

singleByte = []

multiByte = []

def toJavaClassName(name):
  if name == u"iso-8859-8-i":
    return u"Iso8I"
  if name.startswith(u"iso-8859-"):
    return name.replace(u"iso-8859-", u"Iso")
  return name.title().replace(u"X-", u"").replace(u"-", u"").replace(u"_", u"")

def toConstantName(name):
  return name.replace(u"-", u"_").upper()

# Encoding.java

for group in data:
  if group["heading"] == "Legacy single-byte encodings":
    singleByte = group["encodings"]
  else:
    multiByte.extend(group["encodings"])
  for encoding in group["encodings"]:
    preferred.append(encoding["name"])
    for label in encoding["labels"]:
      labels.append(Label(label, encoding["name"]))

preferred.sort()
labels.sort()

labelFile = open("src/nu/validator/encoding/Encoding.java", "w")

labelFile.write("""/*
 * Copyright (c) 2015 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package nu.validator.encoding;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.charset.spi.CharsetProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents an <a href="https://encoding.spec.whatwg.org/#encoding">encoding</a>
 * as defined in the <a href="https://encoding.spec.whatwg.org/">Encoding
 * Standard</a>, provides access to each encoding defined in the Encoding
 * Standard via a static constant and provides the 
 * "<a href="https://encoding.spec.whatwg.org/#concept-encoding-get">get an 
 * encoding</a>" algorithm defined in the Encoding Standard.
 * 
 * <p>This class inherits from {@link Charset} to allow the Encoding 
 * Standard-compliant encodings to be used in contexts that support
 * <code>Charset</code> instances. However, by design, the Encoding 
 * Standard-compliant encodings are not supplied via a {@link CharsetProvider}
 * and, therefore, are not available via and do not interfere with the static
 * methods provided by <code>Charset</code>. (This class provides methods of
 * the same name to hide each static method of <code>Charset</code> to help
 * avoid accidental calls to the static methods of the superclass when working
 * with Encoding Standard-compliant encodings.)
 * 
 * <p>When an application needs to use a particular encoding, such as utf-8
 * or windows-1252, the corresponding constant, i.e.
 * {@link #UTF_8 Encoding.UTF_8} and {@link #WINDOWS_1252 Encoding.WINDOWS_1252}
 * respectively, should be used. However, when the application receives an
 * encoding label from external input, the method {@link #forName(String) 
 * forName()} should be used to obtain the object representing the encoding 
 * identified by the label. In contexts where labels that map to the 
 * <a href="https://encoding.spec.whatwg.org/#replacement">replacement
 * encoding</a> should be treated as unknown, the method {@link
 * #forNameNoReplacement(String) forNameNoReplacement()} should be used instead.
 * 
 * 
 * @author hsivonen
 */
public abstract class Encoding extends Charset {

    private static final String[] LABELS = {
""")

for label in labels:
  labelFile.write("        \"%s\",\n" % label.label)

labelFile.write("""    };
    
    private static final Encoding[] ENCODINGS_FOR_LABELS = {
""")

for label in labels:
  labelFile.write("        %s.INSTANCE,\n" % toJavaClassName(label.preferred))

labelFile.write("""    };

    private static final Encoding[] ENCODINGS = {
""")

for label in preferred:
  labelFile.write("        %s.INSTANCE,\n" % toJavaClassName(label))
        
labelFile.write("""    };

""")

for label in preferred:
  labelFile.write("""    /**
     * The %s encoding.
     */
    public static final Encoding %s = %s.INSTANCE;

""" % (label, toConstantName(label), toJavaClassName(label)))
        
labelFile.write("""
private static SortedMap<String, Charset> encodings = null;

    protected Encoding(String canonicalName, String[] aliases) {
        super(canonicalName, aliases);
    }

    private enum State {
        HEAD, LABEL, TAIL
    };

    public static Encoding forName(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Label must not be null.");
        }
        if (label.length() == 0) {
            throw new IllegalCharsetNameException(label);
        }
        // First try the fast path
        int index = Arrays.binarySearch(LABELS, label);
        if (index >= 0) {
            return ENCODINGS_FOR_LABELS[index];
        }
        // Else, slow path
        StringBuilder sb = new StringBuilder();
        State state = State.HEAD;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if ((c == ' ') || (c == '\\n') || (c == '\\r') || (c == '\\t')
                    || (c == '\\u000C')) {
                if (state == State.LABEL) {
                    state = State.TAIL;
                }
                continue;
            }
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                switch (state) {
                    case HEAD:
                        state = State.LABEL;
                        // Fall through
                    case LABEL:
                        sb.append(c);
                        continue;
                    case TAIL:
                        throw new IllegalCharsetNameException(label);
                }
            }
            if (c >= 'A' && c <= 'Z') {
                c += 0x20;
                switch (state) {
                    case HEAD:
                        state = State.LABEL;
                        // Fall through
                    case LABEL:
                        sb.append(c);
                        continue;
                    case TAIL:
                        throw new IllegalCharsetNameException(label);
                }
            }
            if ((c == '-') || (c == '+') || (c == '.') || (c == ':')
                    || (c == '_')) {
                switch (state) {
                    case LABEL:
                        sb.append(c);
                        continue;
                    case HEAD:
                    case TAIL:
                        throw new IllegalCharsetNameException(label);
                }
            }
            throw new IllegalCharsetNameException(label);
        }
        index = Arrays.binarySearch(LABELS, sb.toString());
        if (index >= 0) {
            return ENCODINGS_FOR_LABELS[index];
        }
        throw new UnsupportedCharsetException(label);
    }

    public static Encoding forNameNoReplacement(String label) {
        Encoding encoding = Encoding.forName(label);
        if (encoding == Encoding.REPLACEMENT) {
            throw new UnsupportedCharsetException(label);            
        }
        return encoding;
    }

    public static boolean isSupported(String label) {
        try {
            Encoding.forName(label);
        } catch (UnsupportedCharsetException e) {
            return false;
        }
        return true;
    }

    public static boolean isSupportedNoReplacement(String label) {
        try {
            Encoding.forNameNoReplacement(label);
        } catch (UnsupportedCharsetException e) {
            return false;
        }
        return true;
    }

    public static SortedMap<String, Charset> availableCharsets() {
        if (encodings == null) {
            TreeMap<String, Charset> map = new TreeMap<String, Charset>();
            for (Encoding encoding : ENCODINGS) {
                map.put(encoding.name(), encoding);
            }
            encodings = Collections.unmodifiableSortedMap(map);
        }
        return encodings;
    }

    public static Encoding defaultCharset() {
        return WINDOWS_1252;
    }

    @Override public boolean canEncode() {
        return false;
    }

    @Override public boolean contains(Charset cs) {
        return false;
    }

    @Override public CharsetEncoder newEncoder() {
        throw new UnsupportedOperationException("Encoder not implemented.");
    }
}
""")

labelFile.close()

# Single-byte encodings

for encoding in singleByte:
  name = encoding["name"]
  labels = encoding["labels"]
  labels.sort()
  className = toJavaClassName(name)
  mappingName = name
  if mappingName == u"iso-8859-8-i":
    mappingName = u"iso-8859-8"
  mapping = indexes[mappingName]
  classFile = open("src/nu/validator/encoding/%s.java" % className, "w")
  classFile.write('''/*
 * Copyright (c) 2013-2015 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

/*
 * THIS IS A GENERATED FILE. PLEASE DO NOT EDIT.
 * Instead, please regenerate using generate-encoding-data.py
 */

package nu.validator.encoding;

import java.nio.charset.CharsetDecoder;

class ''')
  classFile.write(className)
  classFile.write(''' extends Encoding {

    private static final char[] TABLE = {''')
  fallible = False
  comma = False
  for codePoint in mapping:
    # XXX should we have error reporting?
    if not codePoint:
      codePoint = 0xFFFD
      fallible = True
    if comma:
      classFile.write(",")
    classFile.write("\n        '\u%04x'" % codePoint);
    comma = True    
  classFile.write('''
    };
    
    private static final String[] LABELS = {''')

  comma = False
  for label in labels:
    if comma:
      classFile.write(",")
    classFile.write("\n        \"%s\"" % label);
    comma = True    
  classFile.write('''
    };
    
    private static final String NAME = "''')
  classFile.write(name)
  classFile.write('''";
    
    static final Encoding INSTANCE = new ''')
  classFile.write(className)
  classFile.write('''();
    
    private ''')
  classFile.write(className)
  classFile.write('''() {
        super(NAME, LABELS);
    }

    @Override public CharsetDecoder newDecoder() {
        return new ''')
  classFile.write("Fallible" if fallible else "Infallible")
  classFile.write('''SingleByteDecoder(this, TABLE);
    }

}
''')
  classFile.close()

# Multi-byte encodings

for encoding in multiByte:
  name = encoding["name"]
  labels = encoding["labels"]
  labels.sort()
  className = toJavaClassName(name)
  classFile = open("src/nu/validator/encoding/%s.java" % className, "w")
  classFile.write('''/*
 * Copyright (c) 2013-2015 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

/*
 * THIS IS A GENERATED FILE. PLEASE DO NOT EDIT.
 * Instead, please regenerate using generate-encoding-data.py
 */

package nu.validator.encoding;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

class ''')
  classFile.write(className)
  classFile.write(''' extends Encoding {

    private static final String[] LABELS = {''')

  comma = False
  for label in labels:
    if comma:
      classFile.write(",")
    classFile.write("\n        \"%s\"" % label);
    comma = True    
  classFile.write('''
    };
    
    private static final String NAME = "''')
  classFile.write(name)
  classFile.write('''";
    
    static final ''')
  classFile.write(className)
  classFile.write(''' INSTANCE = new ''')
  classFile.write(className)
  classFile.write('''();
    
    private ''')
  classFile.write(className)
  classFile.write('''() {
        super(NAME, LABELS);
    }

    @Override public CharsetDecoder newDecoder() {
        ''')
  if name == "gbk":
    classFile.write('''return Charset.forName("gb18030").newDecoder();''')    
  elif name in MULTIBYTE_DECODER_IMPLEMENTED:
    classFile.write("return new %sDecoder(this);" % className)
  else:
    classFile.write('''return Charset.forName(NAME).newDecoder();''')
  classFile.write('''
    }

}
''')
  classFile.close()

# Big5

def nullToZero(codePoint):
  if not codePoint:
    codePoint = 0
  return codePoint

index = []

for codePoint in indexes["big5"]:
  index.append(nullToZero(codePoint))  

# There are four major gaps consisting of more than 4 consecutive invalid pointers
gaps = []
consecutive = 0
consecutiveStart = 0
offset = 0
for codePoint in index:
  if codePoint == 0:
    if consecutive == 0:
      consecutiveStart = offset
    consecutive +=1
  else:
    if consecutive > 4:
      gaps.append((consecutiveStart, consecutiveStart + consecutive))
    consecutive = 0
  offset += 1

def invertRanges(ranges, cap):
  inverted = []
  invertStart = 0
  for (start, end) in ranges:
    if start != 0:
      inverted.append((invertStart, start))
    invertStart = end
  inverted.append((invertStart, cap))
  return inverted

cap = len(index)
ranges = invertRanges(gaps, cap)

# Now compute a compressed lookup table for astralness

gaps = []
consecutive = 0
consecutiveStart = 0
offset = 0
for codePoint in index:
  if codePoint <= 0xFFFF:
    if consecutive == 0:
      consecutiveStart = offset
    consecutive +=1
  else:
    if consecutive > 40:
      gaps.append((consecutiveStart, consecutiveStart + consecutive))
    consecutive = 0
  offset += 1

astralRanges = invertRanges(gaps, cap)

classFile = open("src/nu/validator/encoding/Big5Data.java", "w")
classFile.write('''/*
 * Copyright (c) 2015 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

/*
 * THIS IS A GENERATED FILE. PLEASE DO NOT EDIT.
 * Instead, please regenerate using generate-encoding-data.py
 */

package nu.validator.encoding;

final class Big5Data {
    
    private static boolean readBit(String str, int i) {
        return (str.charAt(i >> 4) & (1 << (i & 0xF))) != 0;
    }

    static char lowBits(int pointer) {
''')

for (low, high) in ranges:
  classFile.write('''        if (pointer < %d) {
            return '\\u0000';
        }
        if (pointer < %d) {
            return "''' % (low, high))
  for i in xrange(low, high):
    classFile.write('\\u%04X' % (index[i] & 0xFFFF))
  classFile.write('''".charAt(pointer - %d);
        }
''' % low)

classFile.write('''        return '\\u0000';
    }

    static boolean isAstral(int pointer) {
''')

for (low, high) in astralRanges:
  if high - low == 1:
    classFile.write('''        if (pointer < %d) {
            return false;
        }
        if (pointer == %d) {
            return true;
        }
''' % (low, low))
  else:
    classFile.write('''        if (pointer < %d) {
            return false;
        }
        if (pointer < %d) {
            return readBit("''' % (low, high))
    bits = []
    for i in xrange(low, high):
      bits.append(1 if index[i] > 0xFFFF else 0)
    # pad length to multiple of 16
    for i in xrange(16 - (len(bits) % 16)):
      bits.append(0)
    i = 0
    while i < len(bits):
      accu = 0
      for j in xrange(16):
        accu |= bits[i + j] << j
      if accu == 0x22:
        classFile.write('\\"')
      else:
        classFile.write('\\u%04X' % accu)
      i += 16
    classFile.write('''", pointer - %d);
        }
''' % low)

classFile.write('''        return false;
    }

}
''')
classFile.close()
