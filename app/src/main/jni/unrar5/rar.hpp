#ifndef _RAR_RARCOMMON_
#define _RAR_RARCOMMON_

#if 1	// COMITTON_MOD
#include <android/log.h>

#define  LOG_TAG    "comittonxt"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#endif

#include "raros.hpp"
#include "rartypes.hpp"
#include "os.hpp"

#if 0	// COMITTON_MOD
#ifdef RARDLL
#include "dll.hpp"
#endif
#endif

#include "version.hpp"
#include "rardefs.hpp"
#include "rarlang.hpp"
#include "unicode.hpp"
#if 0	// COMITTON_MOD
#include "errhnd.hpp"
#endif
#include "secpassword.hpp"
#include "array.hpp"
#include "timefn.hpp"
#include "sha1.hpp"
#include "sha256.hpp"
#include "blake2s.hpp"
#include "hash.hpp"
#if 0	// COMITTON_MOD
#include "options.hpp"
#endif
#include "rijndael.hpp"
#include "crypt.hpp"
#include "headers5.hpp"
#include "headers.hpp"
#if 0	// COMITTON_MOD
#include "pathfn.hpp"
#endif
#include "strfn.hpp"
#if 0	// COMITTON_MOD
#include "strlist.hpp"
#ifdef _WIN_ALL
#include "isnt.hpp"
#endif
#include "file.hpp"
#endif
#if 1	// COMITTON_MOD
#include "mem.hpp"	// add new file comitton
#endif
#include "crc.hpp"
#if 0	// COMITTON_MOD
#include "ui.hpp"
#include "filefn.hpp"
#include "filestr.hpp"
#include "find.hpp"
#include "scantree.hpp"
#include "savepos.hpp"
#endif
#include "getbits.hpp"
#include "rdwrfn.hpp"
#ifdef USE_QOPEN
#include "qopen.hpp"
#endif
#if 0	// COMITTON_MOD
#include "archive.hpp"
#include "match.hpp"
#include "cmddata.hpp"
#include "filcreat.hpp"
#include "consio.hpp"
#include "system.hpp"
#include "log.hpp"
#endif
#include "rawint.hpp"
#if 0	// COMITTON_MOD
#include "rawread.hpp"
#include "encname.hpp"
#include "resource.hpp"
#endif
#include "compress.hpp"

#include "rarvm.hpp"
#include "model.hpp"

#include "threadpool.hpp"

#include "unpack.hpp"



#if 0	// COMITTON_MOD
#include "extinfo.hpp"
#include "extract.hpp"



#include "list.hpp"


#include "rs.hpp"
#include "rs16.hpp"
#include "recvol.hpp"
#include "volume.hpp"
#include "smallfn.hpp"

#include "global.hpp"
#endif

#if 0
#include "benchmark.hpp"
#endif





#endif
