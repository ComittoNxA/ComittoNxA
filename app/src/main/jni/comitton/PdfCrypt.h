#define  LOG_TAG    "comitton_pdf"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define BUFFER_SIZE		(10 * 1024)
// #define NULL			0

#define PDF_CRYPT_NONE		0
#define PDF_CRYPT_RC4		1
#define PDF_CRYPT_AESV2		2
#define PDF_CRYPT_AESV3		3
#define PDF_CRYPT_UNKNOWN	4

struct pdf_crypt
{
	unsigned char id[48];	//[48];
	int id_len;

	int v;
	int length;
	// pdf_obj *cf;
	// pdf_crypt_filter stmf;
	// pdf_crypt_filter strf;

	int r;
	unsigned char o[48];	//[48];
	unsigned char u[48];	//[48];
	unsigned char oe[32];	//[32];
	unsigned char ue[32];	//[32];
	int p;
	int encrypt_metadata;

	unsigned char key[32];	//[32]; /* decryption key generated from password */
};

struct fz_md5
{
	unsigned int state[4];
	unsigned int count[2];
	unsigned char buffer[64];
};

struct fz_arc4
{
	unsigned x;
	unsigned y;
	unsigned char state[256];
};

typedef	unsigned char	BYTE;

typedef struct _data_nest {
	BYTE	data[BUFFER_SIZE];
	int		size;
	struct	_data_nest *next;
} DATA_NEST;

BYTE *init_buffer(void);
int set_buffer(int size);
BYTE *add_buffer(void);
BYTE *copy_buffer(int *size);
int size_buffer(void);
DATA_NEST *alloc_buffer(void);
int free_buffer(void);

int FlateDecompress(unsigned char *inbuf, int insize);
int PredictDecode(BYTE *inbuf, int insize, int predictor, int columns, int colors, int bpc);
int Arc4Decode(BYTE *inbuf, int insize, unsigned char *key, unsigned keylen);
int AesDecode(BYTE *inbuf, int insize, unsigned char *key, unsigned keylen);
int computeObjectKey(int method, BYTE *crypt_key, int crypt_len, int num, int gen, BYTE *res_key);

void fz_md5_init(fz_md5 *state);
void fz_md5_update(fz_md5 *state, const unsigned char *input, unsigned inlen);
void fz_md5_final(fz_md5 *state, unsigned char digest[16]);

/* arc4 crypto */
void fz_arc4_init(fz_arc4 *arc4, const unsigned char *key, unsigned keylen);
void fz_arc4_encrypt(fz_arc4 *state, unsigned char *dest, const unsigned char *src, unsigned len);

/* sha-256 digests */

typedef struct fz_sha256_s fz_sha256;

struct fz_sha256_s
{
	unsigned int state[8];
	unsigned int count[2];
	union {
		unsigned char u8[64];
		unsigned int u32[16];
	} buffer;
};

void fz_sha256_init(fz_sha256 *state);
void fz_sha256_update(fz_sha256 *state, const unsigned char *input, unsigned int inlen);
void fz_sha256_final(fz_sha256 *state, unsigned char digest[32]);

/* AES block cipher implementation from XYSSL */

typedef struct fz_aes_s fz_aes;

#define AES_DECRYPT 0
#define AES_ENCRYPT 1

struct fz_aes_s
{
	int nr; /* number of rounds */
	unsigned long *rk; /* AES round keys */
	unsigned long buf[68]; /* unaligned data */
};

void aes_setkey_enc( fz_aes *ctx, const unsigned char *key, int keysize );
void aes_setkey_dec( fz_aes *ctx, const unsigned char *key, int keysize );
void aes_crypt_cbc( fz_aes *ctx, int mode, int length,
	unsigned char iv[16],
	const unsigned char *input,
	unsigned char *output );


int pdf_authenticate_password(pdf_crypt *crypt, char *password);

static inline int fz_absi(int i)
{
	return (i < 0 ? -i : i);
}
