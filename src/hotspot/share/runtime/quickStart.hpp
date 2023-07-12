#ifndef SHARE_VM_RUNTIME_QUICKSTART_HPP
#define SHARE_VM_RUNTIME_QUICKSTART_HPP

#include "memory/allocation.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/ostream.hpp"

#define OPT_TAG_LIST_EXT

#define OPT_TAG_LIST \
  OPT_TAG(appcds) \
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

  typedef enum {
    Normal,
    Tracer,
    Replayer,
  } QuickStartRole;

  static const char* cache_path()       { return _cache_path; }
  static bool is_enabled()              { return _is_enabled; }
  static bool verbose()                 { return _verbose; }
  static bool parse_command_line_arguments(const char* opts = NULL);
  static void post_process_arguments(JavaVMInitArgs* options_args);
  static void initialize(TRAPS);
  static bool is_tracer()               { return _role == Tracer; }
  static bool is_replayer()             { return _role == Replayer; }
  static bool is_normal()               { return _role == Normal; }
  static bool is_starting()             { return is_enabled() && _is_starting; }

  static int remove_dir(const char* dir);
  static const char* image_id()         { return _image_id; }
  static const char* vm_version()       { return _vm_version; }

private:
  static const char* _cache_path;
  static const char* _image_id;
  static const char* _vm_version;
  static const char* _lock_path;
  static const char* _temp_metadata_file_path;
  static const char* _metadata_file_path;

  static FILE*       _metadata_file;
  static fileStream* _temp_metadata_file;

  static int _lock_file_fd;

  static QuickStartRole _role;

  static bool _is_starting;
  static bool _is_enabled;
  static bool _verbose;
  static bool _print_stat_enabled;
  static bool _need_destroy;
  static const char* _opt_name[];
  static const char* _identifier_name[];
  static bool _opt_enabled[];
  static bool _opt_passed[];
  static int _jvm_option_count;

  static bool set_optimization(const char* option, bool enabled);
  static bool determine_tracer_or_replayer(JavaVMInitArgs* options_args);
  static void calculate_cache_path();
  static void destroy_cache_folder();
  static void setenv_for_roles();
  static void process_argument_for_optimization();
  static bool check_integrity(JavaVMInitArgs* options_args);
  static void generate_metadata_file();
  static bool match_option(const char* option, const char* name, const char** tail);
  static void print_command_line_help(outputStream* out);
  static bool dump_cached_info(JavaVMInitArgs* options_args);
  static bool load_and_validate(JavaVMInitArgs* options_args);
  static void check_features(const char* &str);
  static void print_stat(bool isReplayer);
  static void log(const char* msg, ...) ATTRIBUTE_PRINTF(1, 2);
  static void settle_opt_pass_table();

public:
  static void set_opt_passed(opt feature);
  static void notify_dump();

  // cds stuff
private:
  static const char *_origin_class_list;
  static const char *_final_class_list;
  static const char *_jsa;
  static const char *_eagerappcds_agentlib;
  static const char *_eagerappcds_agent;
private:
  static void enable_eagerappcds();
  static void enable_appcds();
  static void add_CDSDumpHook(TRAPS);
};

#endif
