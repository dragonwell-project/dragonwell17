#ifndef SHARE_VM_RUNTIME_QUICKSTART_HPP
#define SHARE_VM_RUNTIME_QUICKSTART_HPP

#include "memory/allocation.hpp"
#include "utilities/growableArray.hpp"

#define OPT_TAG_LIST_EXT

#define OPT_TAG_LIST \
  OPT_TAG_LIST_EXT

class QuickStart : AllStatic {
public:
  enum opt {
#define OPT_TAG(name) _##name,
    OPT_TAG_LIST
#undef OPT_TAG
    Count
  };

  typedef enum {
    Normal,
    Tracer,
    Replayer
  } QuickStartRole;

  ~QuickStart();
  static const char* cache_path()       { return _cache_path; }
  static bool is_enabled()              { return _is_enabled; }
  static bool parse_command_line_arguments(const char* opts = NULL);
  static void post_process_arguments();
  static void initialize(TRAPS);
  static bool is_tracer()               { return _role == Tracer; }
  static bool is_replayer()             { return _role == Replayer; }
  static bool is_starting()             { return is_enabled() && _is_starting; }

  static int remove_dir(const char* dir);

private:
  static const char* _cache_path;
  static const char* _image_env;
  static const char* _lock_path;
  static const char* _temp_metadata_file_path;
  static const char* _metadata_file_path;

  static FILE *_METADATA_FILE;
  static FILE *_TEMP_METADATA_FILE;

  static int _lock_file_fd;

  static QuickStartRole _role;

  static bool _is_starting;
  static bool _is_enabled;
  static bool _verbose;
  static bool _print_stat_enabled;
  static bool _need_destroy;
  static const char* _opt_name[];
  static bool _opt_enabled[];

  static bool set_optimization(const char* option, bool enabled);
  static bool determine_tracer_or_replayer();
  static void calculate_cache_path();
  static void destroy_cache_folder();
  static void process_argument_for_optimaztion();
  static bool check_integrity();
  static void generate_metadata_file();
  static bool match_option(const char* option, const char* name, const char** tail);
  static void print_command_line_help(outputStream* out);
  static void log(const char* msg, ...) ATTRIBUTE_PRINTF(1, 2);

public:
  static void notify_dump();
};

#endif
