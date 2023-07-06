#ifndef SHARE_VM_RUNTIME_QUICKSTART_HPP
#define SHARE_VM_RUNTIME_QUICKSTART_HPP

#include "memory/allocation.hpp"
#include "utilities/growableArray.hpp"

#define OPT_TAG_LIST_EXT

#define OPT_TAG_LIST \
  OPT_TAG(cds) \
  OPT_TAG(eagerappcds) \
  OPT_TAG_LIST_EXT

class QuickStart : AllStatic {
public:
  enum opt {
#define OPT_TAG(name) _##name,
    OPT_TAG_LIST
#undef OPT_TAG
    Count
  };

  ~QuickStart();
  static const char* cache_path()       { return _cache_path; }
  static bool is_enabled()              { return _is_enabled; }
  static bool parse_command_line_arguments(const char* opts = NULL);
  static void post_process_arguments();
  static void initialize(TRAPS);
  static bool is_producer()             { return _is_enabled && _is_tracer; }
  static bool is_consumer()             { return _is_enabled && _is_replayer; }
  static bool is_starting()             { return _is_enabled && _is_starting; }

private:
  static const char* _cache_path;
  static const char* _image_env;
  static bool _is_enabled;
  static bool _is_tracer;
  static bool _is_replayer;
  static bool _is_starting;
  static bool _verbose;
  static bool _print_stat_enabled;
  static bool _need_destroy;
  static bool _need_finish_check;
  static const char* _opt_name[];
  static bool _opt_enabled[];

  static bool set_optimization(const char* option, bool enabled);
  static void determine_tracer_or_replayer();
  static void calculate_cache_path();
  static void process_argument_for_optimaztion();
  static void check_consistency();
  static bool match_option(const char* option, const char* name, const char** tail);
  static void print_command_line_help(outputStream* out);
};

#endif
