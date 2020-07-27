#ifndef _RAR_RARCOMMON_
#define _RAR_RARCOMMON_

#if 1	// COMITTON_MOD
#include <android/log.h>

#define  LOG_TAG    "comitton"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#endif

#include "raros.hpp"
#include "os.hpp"

#if 0	// COMITTON_MOD
#ifdef RARDLL
#include "dll.hpp"
#endif
#endif


#ifndef _WIN_CE
#include "version.hpp"
#endif
#include "rartypes.hpp"
#include "rardefs.hpp"
#include "rarlang.hpp"
#if 0	// COMITTON_MOD
#include "unicode.hpp"
#include "errhnd.hpp"
#endif
#include "array.hpp"
#include "timefn.hpp"
#if 0	// COMITTON_MOD
#include "options.hpp"
#endif
#include "headers.hpp"
#if 0	// COMITTON_MOD
#include "pathfn.hpp"
#include "strfn.hpp"
#include "strlist.hpp"
#include "file.hpp"
#else
#include "mem.hpp"	// add new file comitton
#endif
#include "sha1.hpp"
#include "crc.hpp"
#include "rijndael.hpp"
#include "crypt.hpp"
#if 0	// COMITTON_MOD
#include "filefn.hpp"
#include "filestr.hpp"
#include "find.hpp"
#include "scantree.hpp"
#include "savepos.hpp"
#endif
#include "getbits.hpp"
#include "rdwrfn.hpp"
#if 0	// COMITTON_MOD
#include "archive.hpp"
#include "match.hpp"
#include "cmddata.hpp"
#include "filcreat.hpp"
#include "consio.hpp"
#include "system.hpp"
#ifdef _WIN_ALL
#include "isnt.hpp"
#endif
#include "log.hpp"
#include "rawread.hpp"
#include "encname.hpp"
#include "resource.hpp"
#endif
#include "compress.hpp"


#include "rarvm.hpp"
#include "model.hpp"


#include "unpack.hpp"


#if 0	// COMITTON_MOD
#include "extinfo.hpp"
#include "extract.hpp"



#include "list.hpp"


#include "rs.hpp"
#include "recvol.hpp"
#include "volume.hpp"
#include "smallfn.hpp"
#include "ulinks.hpp"

#include "global.hpp"
#endif


#endif
