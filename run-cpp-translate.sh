#!/bin/sh

# ***** BEGIN LICENSE BLOCK *****
# Version: MPL 1.1/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Mozilla Public License Version
# 1.1 (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
# http://www.mozilla.org/MPL/
#
# Software distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
# for the specific language governing rights and limitations under the
# License.
#
# The Original Code is HTML Parser C++ Translator code.
#
# The Initial Developer of the Original Code is
# Mozilla Foundation.
# Portions created by the Initial Developer are Copyright (C) 2009
# the Initial Developer. All Rights Reserved.
#
# Contributor(s):
#   Henri Sivonen <hsivonen@iki.fi>
#
# Alternatively, the contents of this file may be used under the terms of
# either the GNU General Public License Version 2 or later (the "GPL"), or
# the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the MPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the MPL, the GPL or the LGPL.
#
# ***** END LICENSE BLOCK *****

if [ $# -ne 3 ]
then
  echo "Usage: run-cpp-translate.sh /absolute/path/to/javaparser.jar /absolute/path/to/htmlparser /absolute/path/to/mozilla"
  exit 1
fi


cd "$2/translator-src"
javac -d "$2/classes" -cp "$1" nu/validator/htmlparser/cpptranslate/CppTypes.java nu/validator/htmlparser/cpptranslate/CppVisitor.java nu/validator/htmlparser/cpptranslate/GkAtomParser.java nu/validator/htmlparser/cpptranslate/HVisitor.java nu/validator/htmlparser/cpptranslate/LabelVisitor.java nu/validator/htmlparser/cpptranslate/LicenseExtractor.java nu/validator/htmlparser/cpptranslate/Main.java nu/validator/htmlparser/cpptranslate/NoCppInputStream.java nu/validator/htmlparser/cpptranslate/StringLiteralParser.java nu/validator/htmlparser/cpptranslate/SymbolTable.java nu/validator/htmlparser/generator/GenerateNamedCharacters.java nu/validator/htmlparser/generator/GenerateNamedCharactersCpp.java 
java -cp "$1:$2/classes" nu.validator.htmlparser.cpptranslate.Main "$2/src/nu/validator/htmlparser/impl/" "$3/content/html/parser/src/" "$3/content/html/parser/src/nsHtml5AtomList.h"
