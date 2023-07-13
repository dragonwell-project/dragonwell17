/*
 * Copyright (c) 2022, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "runtime/dynamicCDSCheck.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/quickStart.hpp"
#include "cds/filemap.hpp"
#include "classfile/classLoaderExt.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"

GrowableArray<const char*> *DynamicCDSCheck::fat_jars = NULL;
bool DynamicCDSCheck::initialized = false;

void DynamicCDSCheck::initialize(TRAPS) {
  // We only support using EagerAppCDSDynamicClassDiffCheck under QuickStart.
  Klass* klass = SystemDictionary::resolve_or_fail(vmSymbols::com_alibaba_cds_dynamic_DynamicCDSCheck(), true, CHECK);
  JavaValue result(T_VOID);
  JavaCallArguments args(2);
  char buf[JVM_MAXPATHLEN];
  sprintf(buf, "%s%s%s", QuickStart::cache_path(), os::file_separator(), QuickStart::jar_file_lst());
  args.push_oop(java_lang_String::create_from_str(buf, THREAD));
  sprintf(buf, "%s%s%s", QuickStart::cache_path(), os::file_separator(), QuickStart::cds_final_lst());
  args.push_oop(java_lang_String::create_from_str(buf, THREAD));
  JavaCalls::call_static(&result, klass, vmSymbols::initialize_name(),
                         vmSymbols::string_string_void_signature(), &args, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    // Initialize failed. We disable EagerAppCDSDynamicClassDiffCheck.
    warning("Disabling EagerAppCDSDynamicClassDiffCheck");
    EagerAppCDSDynamicClassDiffCheck = false;
    initialized = false;
  } else {
    initialized = true;
  }
}

void DynamicCDSCheck::record_dir_or_plain_jar(ClassPathEntry* e, const char *original_source, TRAPS) {
  assert(DumpSharedSpaces, "sanity");

  if (e == NULL) {
    return;
  }

  if (!e->is_jar_file() && !e->is_modules_image()) {
    // ClassPathDirEntry
    // record info of this dir.
    jar_record_file->print_cr("%s%c%lx", original_source, separator, 0L);
  } if (e->is_jar_file()) {
    // ClassPathZipEntry
    ClassPathZipEntry *entry = (ClassPathZipEntry*) e;
    // record info of this jar.
    Symbol *source_jar = SymbolTable::new_symbol(entry->name());
    if (SystemDictionary::jar2crc32_table()->lookup(source_jar) == NULL) {
      SystemDictionary::jar2crc32_table()->add(source_jar, entry->crc32());
      jar_record_file->print_cr("%s%c%lx", original_source, separator, entry->crc32());
    }
  }
}

void DynamicCDSCheck::record_dir_or_plain_jar(const char *original_source, TRAPS) {
  assert(DumpSharedSpaces, "sanity");

  if (::strncmp(original_source, "file:", 5) != 0) {   // a normal thin jar
    return;
  }

  ClassPathEntry* e = ClassLoaderExt::find_classpath_entry_from_cache(THREAD, original_source+5);

  DynamicCDSCheck::record_dir_or_plain_jar(e, original_source, THREAD);
}

void DynamicCDSCheck::record_fat_jar(const char *original_source, TRAPS) {
  assert(DumpSharedSpaces, "sanity");

  // if not initialized, then lazily initialize
  if (fat_jars == NULL) {
    fat_jars = new (ResourceObj::C_HEAP, mtClass) GrowableArray<const char*>(5, mtInternal);
  }
  // we are going to read the fat jar and record into the file
  if (::strncmp(original_source, "jar:file:", 9) != 0) {
    return;
  }

  // we start with "jar:file:".

  char buf[MAXPATHLEN];
  sprintf(buf, "%s", original_source);
  char *end = strstr(buf, "!/");
  if (end == NULL) {
    return;
  }

  *end = '\0';

  bool slowpath = true;
  for (int i = 0; i < fat_jars->length(); i++) {
    if (::strcmp(fat_jars->at(i), buf) == 0) {
      // this fat jar has been processed.
      slowpath = false;
      break;
    }
  }

  if (!slowpath) {
    return;
  }

  ClassPathEntry* e = ClassLoaderExt::find_classpath_entry_from_cache(THREAD, buf+9);
  if (e == NULL || !e->is_jar_file()) {
    return;
  }

  // ClassPathZipEntry
  ClassPathZipEntry *entry = (ClassPathZipEntry *) e;
  // record info of this jar.
  jar_record_file->print_cr("%s!/%c%lx", buf, separator, entry->crc32());

  // append to the list
  fat_jars->append(::strdup(buf));
}

bool DynamicCDSCheck::check_klass_validation(InstanceKlass *ik, TRAPS) {
  assert(UseSharedSpaces, "sanity");

  // check_klass_validation may be called before DynamicCDSCheck::initialize().
  // Or if DynamicCDSCheck initialization failed, we also return false.
  if (!initialized) {
    return false;
  }

  if (is_klass_in_jrt(ik, THREAD)) {
    return true;
  }

  Klass *klass = SystemDictionary::resolve_or_fail(vmSymbols::com_alibaba_cds_dynamic_DynamicCDSCheck(), true, CHECK_AND_CLEAR_false);
  JavaValue result(T_BOOLEAN);
  JavaCallArguments args(2);
  args.push_oop(java_lang_String::create_from_symbol(ik->source_file_path(), THREAD));
  args.push_oop(java_lang_String::create_from_symbol(ik->name(), THREAD));
  JavaCalls::call_static(&result, klass, vmSymbols::checkKlassValidation_name(),
                         vmSymbols::string_string_bool_signature(), &args, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    // print exception
    Handle ex(THREAD, PENDING_EXCEPTION);
    CLEAR_PENDING_EXCEPTION;
    java_lang_Throwable::print_stack_trace(ex, tty);
    return false;
  }
  return result.get_jboolean();
}

bool DynamicCDSCheck::is_klass_in_jrt(InstanceKlass *ik, TRAPS) {
  int path_index = ik->shared_classpath_index();
  SharedClassPathEntry* ent = FileMapInfo::shared_path(path_index);
  return ent != NULL && ent->is_modules_image();
}