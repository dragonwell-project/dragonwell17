#include "precompiled.hpp"
#include <stdlib.h>
#include <strings.h>
#include "classfile/vmSymbols.hpp"
#include "runtime/java.hpp"
#include "runtime/quickStart.hpp"
#include "utilities/defaultStream.hpp"

bool QuickStart::_is_enabled = false;
bool QuickStart::_is_tracer = false;
bool QuickStart::_is_replayer = false;
bool QuickStart::_is_starting = true;
bool QuickStart::_verbose = false;
bool QuickStart::_print_stat_enabled = false;
bool QuickStart::_need_destroy = false;
bool QuickStart::_need_finish_check = true;

const char* QuickStart::_cache_path = NULL;
const char* QuickStart::_image_env = NULL;
const char* QuickStart::_opt_name[] = {
#define OPT_TAG(name) #name,
  OPT_TAG_LIST
#undef OPT_TAG
};

bool QuickStart::_opt_enabled[] = {
#define OPT_TAG(name) true,
  OPT_TAG_LIST
#undef OPT_TAG
};

#define DEFAULT_DIRECTORY "sharedcache"
#define MAX_PATH (2 * K)

bool QuickStart::parse_command_line_arguments(const char* options) {
  _is_enabled = true;
  if (options == NULL) {
    return true;
  }
  char* copy = os::strdup_check_oom(options, mtArguments);

  // Split string on commas
  bool success = true;
  for (char *comma_pos = copy, *cur = copy; success && comma_pos != NULL; cur = comma_pos + 1) {
    comma_pos = strchr(cur, ',');
    if (comma_pos != NULL) {
      *comma_pos = '\0';
    }
    const char* tail = NULL;
    if (*cur == '+') {
      success = set_optimization(cur + 1, true);
    } else if (*cur == '-') {
      success = set_optimization(cur + 1, false);
    } else if (match_option(cur, "help", &tail)) {
      fileStream stream(defaultStream::output_stream());
      print_command_line_help(&stream);
      vm_exit(0);
    } else if (match_option(cur, "verbose", &tail)) {
      _verbose = true;
    } else if (match_option(cur, "printStat", &tail)) {
      _print_stat_enabled = true;
    } else if (match_option(cur, "destory", &tail)) {
      _need_destroy = true;
    } else if (match_option(cur, "path=", &tail)) {
      _cache_path = os::strdup_check_oom(tail, mtArguments);
    } else if (match_option(cur, "dumpPolicy=", &tail)) {
      size_t len = strlen(tail);
      if (strncmp(tail, "api", len) == 0) {
        _need_finish_check = false;
      } else if (strncmp(tail, "auto", len) == 0) {
        _need_finish_check = true;
      } else {
        success = false;
        tty->print_cr("[QuickStart] Invalid -Xquickstart option '%s'", cur);
      }
    } else if (match_option(cur, "dockerImageEnv=", &tail)) {
      _image_env = os::strdup_check_oom(tail, mtArguments);
    } else {
      success = false;
      tty->print_cr("[QuickStart] Invalid -Xquickstart option '%s'", cur);
    }
  }

  os::free(copy);
  return success;
}

bool QuickStart::set_optimization(const char* option, bool enabled) {
  for (int i = 0; i < QuickStart::Count; i++) {
    if (strcasecmp(option, _opt_name[i]) == 0) {
      _opt_enabled[i] = enabled;
      return true;
    }
  }

  tty->print_cr("[QuickStart] Invalid -Xquickstart optimization option '%s'", option);
  return false;
}

bool QuickStart::match_option(const char* option, const char* name, const char** tail) {
  size_t len = strlen(name);
  if (strncmp(option, name, len) == 0) {
    *tail = option + len;
    return true;
  } else {
    return false;
  }
}

void QuickStart::print_command_line_help(outputStream* out) {
  out->print_cr("-Xquickstart Usage: -Xquickstart[:option,...]");
  out->cr();

  out->print_cr("Available option:");
  out->cr();

  out->print_cr("  help                 Print general quick start help");
  out->print_cr("  verbose              Enable verbose output");
  out->print_cr("  printStat            List all the elements in the cache");
  out->print_cr("  path=<path>          Specify the location of the cache files");
  out->print_cr("  destroy              Destroy the cache files (use specified path or default)");
  out->print_cr("  +/-<opt>             Enable/disable the specific optimization");
  out->print_cr("  dockerImageEnv=<env> Specify the environment variable to get the unique identifier of the container");
  out->cr();

  out->print_cr("Available optimization:");
  out->cr();
}

// initialize JDK part for QuickStart
void QuickStart::initialize(TRAPS) {
}

void QuickStart::post_process_arguments() {
  // Prepare environment
  calculate_cache_path();
  // Determine the role
  determine_tracer_or_replayer();
  // check the consistency
  check_consistency();
  // Process argument for each optimization
  process_argument_for_optimaztion();
}

void QuickStart::check_consistency() {
  // check the consistency
}

void QuickStart::calculate_cache_path() {
  if (_cache_path != NULL) {
    if (_verbose) tty->print_cr("[QuickStart] cache path is set from -Xquickstart:path=%s", _cache_path);
    return;
  }

  const char *buffer = ::getenv("QUICKSTART_CACHE_PATH");
  if (buffer != NULL && (_cache_path = os::strdup_check_oom(buffer)) != NULL) {
    if (_verbose) tty->print_cr("[QuickStart] cache path is set from env with %s", _cache_path);
    return;
  }
  const char* home = ::getenv("HOME");
  char buf[MAX_PATH];
  jio_snprintf(buf, MAX_PATH, "%s%s%s", home, os::file_separator, DEFAULT_DIRECTORY);
  _cache_path = os::strdup_check_oom(buf);
  if (_verbose) tty->print_cr("[QuickStart] cache path is set as default with %s", _cache_path);
}

void QuickStart::process_argument_for_optimaztion() {
  if (_is_replayer) {
  } else if (_is_tracer) {
  }
}

void QuickStart::determine_tracer_or_replayer() {
}
