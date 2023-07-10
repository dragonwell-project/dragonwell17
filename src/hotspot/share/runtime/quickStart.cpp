#include "precompiled.hpp"
#include "classfile/vmSymbols.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/vmClasses.hpp"
#include "runtime/arguments.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/quickStart.hpp"
#include "utilities/defaultStream.hpp"

bool QuickStart::_is_starting = true;
bool QuickStart::_is_enabled = false;
bool QuickStart::_verbose = false;
bool QuickStart::_print_stat_enabled = false;
bool QuickStart::_need_destroy = false;

QuickStart::QuickStartRole QuickStart::_role = QuickStart::Normal;

const char* QuickStart::_cache_path = NULL;
const char* QuickStart::_image_env = NULL;
const char* QuickStart::_lock_path = NULL;
const char* QuickStart::_temp_metadata_file_path = NULL;
const char* QuickStart::_metadata_file_path = NULL;
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

FILE *QuickStart::_METADATA_FILE = NULL;
FILE *QuickStart::_TEMP_METADATA_FILE = NULL;

int QuickStart::_lock_file_fd = 0;

#define DEFAULT_SHARED_DIRECTORY      "alibaba.quickstart.sharedcache"
#define METADATA_FILE                 "metadata"
// metadata file stores some information for invalid check, and generate it after the startup is complete.
// Before the content of the file is completely generated, use the file name metadata.tmp temporarily.
// After the startup is complete, rename metadata.tmp to metadata.
#define TEMP_METADATA_FILE            "metadata.tmp"
#define LOCK_FILE                     "LOCK"

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
    } else if (match_option(cur, "destroy", &tail)) {
      _need_destroy = true;
    } else if (match_option(cur, "path=", &tail)) {
      _cache_path = os::strdup_check_oom(tail, mtArguments);
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
  Klass* klass = vmClasses::com_alibaba_util_QuickStart_klass();
  JavaValue result(T_VOID);
  JavaCallArguments args(2);
  args.push_int(is_tracer());
  args.push_oop(java_lang_String::create_from_str(QuickStart::cache_path(), THREAD));

  JavaCalls::call_static(&result, klass, vmSymbols::initialize_name(),
                         vmSymbols::boolean_String_void_signature(), &args, CHECK);
}

void QuickStart::post_process_arguments() {
  // Prepare environment
  calculate_cache_path();
  // destroy the cache directory
  destroy_cache_folder();
  // Determine the role
  if (!determine_tracer_or_replayer()) {
    _role = Normal;
    return;
  }
  // Process argument for each optimization
  process_argument_for_optimaztion();

  if (_role == Tracer) {
    // Temporarily put here to ensure the integrity of the test
    generate_metadata_file();
  }
}

bool QuickStart::check_integrity() {
  // check the integrity
  return true;
}

void QuickStart::calculate_cache_path() {
  if (_cache_path != NULL) {
    log("cache path is set from -Xquickstart:path=%s", _cache_path);
    return;
  }

  const char *buffer = ::getenv("QUICKSTART_CACHE_PATH");
  if (buffer != NULL && (_cache_path = os::strdup_check_oom(buffer)) != NULL) {
    log("cache path is set from env with %s", _cache_path);
    return;
  }
  const char* home = ::getenv("HOME");
  char buf[PATH_MAX];
  jio_snprintf(buf, PATH_MAX, "%s%s%s", home, os::file_separator(), DEFAULT_SHARED_DIRECTORY);
  _cache_path = os::strdup_check_oom(buf);
  log("cache path is set as default with %s", _cache_path);
}

void QuickStart::destroy_cache_folder() {
  if (_need_destroy && _cache_path != NULL) {
    if (remove_dir(_cache_path) < 0) {
      log("failed to destory the cache folder: %s", _cache_path);
    } else {
      log("destory the cache folder: %s", _cache_path);
    }
    vm_exit(0);
  }
}

void QuickStart::process_argument_for_optimaztion() {
  switch(_role) {
    case Replayer:
      break;
    case Tracer:
      break;
    default:
      break;
  }
}

bool QuickStart::determine_tracer_or_replayer() {
  struct stat st;
  char buf[PATH_MAX];
  int ret = os::stat(_cache_path, &st);
  if (ret != 0) {
    ret = ::mkdir(_cache_path, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
    if (ret != 0) {
      log("Could not mkdir [%s] because [%s]", _cache_path, os::strerror(errno));
      return false;
    }
  } else if (!S_ISDIR(st.st_mode)) {
    log("Cache path [%s] is not a directory, "
        "please use -Xquickstart:path=<path> or environment variable "
        "QUICKSTART_CACHE_PATH to specify.\n",
        _cache_path);
    return false;
  }

  // check whether the metadata file exists.
  jio_snprintf(buf, PATH_MAX, "%s%s%s", _cache_path, os::file_separator(), METADATA_FILE);
  _metadata_file_path = os::strdup_check_oom(buf, mtArguments);
  ret = os::stat(_metadata_file_path, &st);
  if (ret < 0 && errno == ENOENT) {
    // Create a LOCK file
    jio_snprintf(buf, PATH_MAX, "%s%s%s", _cache_path, os::file_separator(), LOCK_FILE);
    _lock_path = os::strdup_check_oom(buf, mtArguments);
    // if the lock exists, it returns -1.
    _lock_file_fd = os::create_binary_file(_lock_path, false);
    if (_lock_file_fd == -1) {
      log("Fail to create LOCK file");
      return false;
    }
    jio_snprintf(buf, PATH_MAX, "%s%s%s", _cache_path, os::file_separator(), TEMP_METADATA_FILE);
    _temp_metadata_file_path = os::strdup_check_oom(buf, mtArguments);
    ret = os::stat(buf, &st);
    if (ret == 0) {
      // error: A file exists, determine failed. Maybe this is a user's file.
      jio_fprintf(defaultStream::error_stream(), "[QuickStart] The %s file exists\n", TEMP_METADATA_FILE);
      return false;
    }
    _TEMP_METADATA_FILE = os::fopen(buf, "w");
    if (!_TEMP_METADATA_FILE) {
      jio_fprintf(defaultStream::error_stream(), "[QuickStart] Failed to create %s file\n", TEMP_METADATA_FILE);
      return false;
    }
    _role = Tracer;
    log("Running as tracer");
    return true;
  } else if (ret == 0 && check_integrity()) {
    _role = Replayer;
    log("Running as replayer");
    return true;
  }
  return false;
}

void QuickStart::generate_metadata_file() {
  // mv metadata to metadata.tmp
  ::fclose(_TEMP_METADATA_FILE);
  int ret = ::rename(_temp_metadata_file_path, _metadata_file_path);
  if (ret != 0) {
    jio_fprintf(defaultStream::error_stream(),
                "[QuickStart] Could not mv [%s] to [%s] because [%s]\n",
                TEMP_METADATA_FILE,
                METADATA_FILE,
                os::strerror(errno));
  }

  // remove lock file
  ret = ::close(_lock_file_fd);
  if (ret != 0) {
    jio_fprintf(defaultStream::error_stream(),
                "[QuickStart] Could not close [%s] because [%s]\n",
                LOCK_FILE,
                os::strerror(errno));
  }

  ret = ::remove(_lock_path);
  if (ret != 0) {
    jio_fprintf(defaultStream::error_stream(),
                "[QuickStart] Could not delete [%s] because [%s]\n",
                LOCK_FILE,
                os::strerror(errno));
  }
}

void QuickStart::log(const char* msg, ...) {
  if (_verbose) {
    va_list ap;
    va_start(ap, msg);
    tty->print("[QuickStart(%d)] ", os::current_process_id());
    tty->vprint(msg, ap);
    tty->print_cr("");
    va_end(ap);
  }
}

int QuickStart::remove_dir(const char* dir) {
  char cur_dir[] = ".";
  char up_dir[]  = "..";
  char dir_name[PATH_MAX];
  DIR *dirp = NULL;
  struct dirent *dp;
  struct stat dir_stat;

  int ret = os::stat(dir, &dir_stat);
  if (ret < 0) {
    jio_fprintf(defaultStream::error_stream(),
                "[QuickStart] Fail to get the stat for directory %s\n",
                dir);
    return ret;
  }

  if (S_ISREG(dir_stat.st_mode)) {
    ret = ::remove(dir);
  } else if (S_ISDIR(dir_stat.st_mode)) {
    dirp = os::opendir(dir);
    while ((dp = os::readdir(dirp)) != NULL) {
      if ((strcmp(cur_dir, dp->d_name) == 0) || (strcmp(up_dir, dp->d_name) == 0)) {
        continue;
      }
      jio_snprintf(dir_name, PATH_MAX, "%s%s%s", dir, os::file_separator(), dp->d_name);
      ret = remove_dir(dir_name);
      if (ret != 0) {
        break;
      }
    }

    os::closedir(dirp);
    if (ret != 0) {
      return -1;
    }
    ret = ::rmdir(dir);
  } else {
    jio_fprintf(defaultStream::error_stream(), "[QuickStart] unknow file type\n");
  }
  return ret;
}

void QuickStart::notify_dump() {
  log("startup finishes");
}
