/*
MD5C.C - RSA Data Security, Inc., MD5 message-digest algorithm

Copyright (C) 1991-2, RSA Data Security, Inc. Created 1991.
All rights reserved.

License to copy and use this software is granted provided that it
is identified as the "RSA Data Security, Inc. MD5 Message-Digest
Algorithm" in all material mentioning or referencing this software
or this function.

License is also granted to make and use derivative works provided
that such works are identified as "derived from the RSA Data
Security, Inc. MD5 Message-Digest Algorithm" in all material
mentioning or referencing the derived work.

RSA Data Security, Inc. makes no representations concerning either
the merchantability of this software or the suitability of this
software for any particular purpose. It is provided "as is"
without express or implied warranty of any kind.

These notices must be retained in any copies of any part of this
documentation and/or software.
*/

//#include "fitz-internal.h"
#include <string.h>
#include <android/log.h>
#include "PdfCrypt.h"

//#define DEBUG

/*
 * Compute an encryption key (PDF 1.7 algorithm 3.2)
 */

static const unsigned char padding[32] =
{
	0x28, 0xbf, 0x4e, 0x5e, 0x4e, 0x75, 0x8a, 0x41,
	0x64, 0x00, 0x4e, 0x56, 0xff, 0xfa, 0x01, 0x08,
	0x2e, 0x2e, 0x00, 0xb6, 0xd0, 0x68, 0x3e, 0x80,
	0x2f, 0x0c, 0xa9, 0xfe, 0x64, 0x53, 0x69, 0x7a
};

/*
 * PDF 1.7 algorithm 3.1 and ExtensionLevel 3 algorithm 3.1a
 *
 * Using the global encryption key that was generated from the
 * password, create a new key that is used to decrypt individual
 * objects and streams. This key is based on the object and
 * generation numbers.
 */

int computeObjectKey(int method, BYTE *crypt_key, int crypt_len, int num, int gen, BYTE *res_key)
{
	fz_md5 md5;
	unsigned char message[5];

	if (method == PDF_CRYPT_AESV3) {
		memcpy(res_key, crypt_key, crypt_len / 8);
		return crypt_len / 8;
	}

	fz_md5_init(&md5);
	fz_md5_update(&md5, crypt_key, crypt_len / 8);
	message[0] = (num) & 0xFF;
	message[1] = (num >> 8) & 0xFF;
	message[2] = (num >> 16) & 0xFF;
	message[3] = (gen) & 0xFF;
	message[4] = (gen >> 8) & 0xFF;
	fz_md5_update(&md5, message, 5);

	if (method == PDF_CRYPT_AESV2) {
		fz_md5_update(&md5, (unsigned char *)"sAlT", 4);
	}

	fz_md5_final(&md5, res_key);

	if (crypt_len / 8 + 5 > 16) {
		return 16;
	}
	return crypt_len / 8 + 5;
}

static void
pdf_compute_encryption_key(pdf_crypt *crypt, unsigned char *password, int pwlen, unsigned char *key)
{
#ifdef DEBUG
	LOGD("pdf_compute_encryption_key");
#endif
	unsigned char buf[32];
	unsigned int p;
	int i, n;
	fz_md5 md5;

	n = crypt->length / 8;

	/* Step 1 - copy and pad password string */
	if (pwlen > 32)
		pwlen = 32;
	memcpy(buf, password, pwlen);
	memcpy(buf + pwlen, padding, 32 - pwlen);
#ifdef DEBUG
	LOGD("pdf_compute_encryption_key: 1-buf=%d,%d,%d,%d,%d,%d..., n=%d", (int)buf[0], (int)buf[1], (int)buf[2], (int)buf[3], (int)buf[4], (int)buf[5], n);
#endif

	/* Step 2 - init md5 and pass value of step 1 */
	fz_md5_init(&md5);
	fz_md5_update(&md5, buf, 32);
#ifdef DEBUG
	LOGD("pdf_compute_encryption_key: 2-buf=%d,%d,%d,%d,%d,%d..., n=%d", (int)buf[0], (int)buf[1], (int)buf[2], (int)buf[3], (int)buf[4], (int)buf[5], n);
#endif

	/* Step 3 - pass O value */
	fz_md5_update(&md5, crypt->o, 32);

	/* Step 4 - pass P value as unsigned int, low-order byte first */
	p = (unsigned int) crypt->p;
	buf[0] = (p) & 0xFF;
	buf[1] = (p >> 8) & 0xFF;
	buf[2] = (p >> 16) & 0xFF;
	buf[3] = (p >> 24) & 0xFF;
	fz_md5_update(&md5, buf, 4);
#ifdef DEBUG
	LOGD("pdf_compute_encryption_key: 3-buf=%d,%d,%d,%d,%d,%d..., n=%d", (int)buf[0], (int)buf[1], (int)buf[2], (int)buf[3], (int)buf[4], (int)buf[5], n);
#endif

	/* Step 5 - pass first element of ID array */
	fz_md5_update(&md5, crypt->id, crypt->id_len);
#ifdef DEBUG
	LOGD("pdf_compute_encryption_key: id=%s,%d", crypt->id, crypt->id_len);
#endif

	/* Step 6 (revision 4 or greater) - if metadata is not encrypted pass 0xFFFFFFFF */
	if (crypt->r >= 4)
	{
		if (!crypt->encrypt_metadata)
		{
			buf[0] = 0xFF;
			buf[1] = 0xFF;
			buf[2] = 0xFF;
			buf[3] = 0xFF;
			fz_md5_update(&md5, buf, 4);
		}
	}

	/* Step 7 - finish the hash */
	fz_md5_final(&md5, buf);
#ifdef DEBUG
	LOGD("pdf_compute_encryption_key: 4-buf=%d,%d,%d,%d,%d,%d..., n=%d", (int)buf[0], (int)buf[1], (int)buf[2], (int)buf[3], (int)buf[4], (int)buf[5], n);
#endif

	/* Step 8 (revision 3 or greater) - do some voodoo 50 times */
	if (crypt->r >= 3)
	{
		for (i = 0; i < 50; i++)
		{
			fz_md5_init(&md5);
			fz_md5_update(&md5, buf, n);
			fz_md5_final(&md5, buf);
		}
	}

	/* Step 9 - the key is the first 'n' bytes of the result */
#ifdef DEBUG
	LOGD("pdf_compute_encryption_key: 5-buf=%d,%d,%d,%d,%d,%d..., n=%d", (int)buf[0], (int)buf[1], (int)buf[2], (int)buf[3], (int)buf[4], (int)buf[5], n);
#endif
	memcpy(key, buf, n);
}

/*
 * Compute an encryption key (PDF 1.7 ExtensionLevel 3 algorithm 3.2a)
 */

static void
pdf_compute_encryption_key_r5(pdf_crypt *crypt, unsigned char *password, int pwlen, int ownerkey, unsigned char *validationkey)
{
	unsigned char buffer[128 + 8 + 48];
	fz_sha256 sha256;
	fz_aes aes;

	/* Step 2 - truncate UTF-8 password to 127 characters */

	if (pwlen > 127)
		pwlen = 127;

	/* Step 3/4 - test password against owner/user key and compute encryption key */

	memcpy(buffer, password, pwlen);
	if (ownerkey)
	{
		memcpy(buffer + pwlen, crypt->o + 32, 8);
		memcpy(buffer + pwlen + 8, crypt->u, 48);
	}
	else
		memcpy(buffer + pwlen, crypt->u + 32, 8);

	fz_sha256_init(&sha256);
	fz_sha256_update(&sha256, buffer, pwlen + 8 + (ownerkey ? 48 : 0));
	fz_sha256_final(&sha256, validationkey);

	/* Step 3.5/4.5 - compute file encryption key from OE/UE */

	memcpy(buffer + pwlen, crypt->u + 40, 8);

	fz_sha256_init(&sha256);
	fz_sha256_update(&sha256, buffer, pwlen + 8);
	fz_sha256_final(&sha256, buffer);

	/* clear password buffer and use it as iv */
	memset(buffer + 32, 0, sizeof(buffer) - 32);
	aes_setkey_dec(&aes, buffer, crypt->length);
	aes_crypt_cbc(&aes, AES_DECRYPT, 32, buffer + 32, ownerkey ? crypt->oe : crypt->ue, crypt->key);
}

/*
 * Computing the user password (PDF 1.7 algorithm 3.4 and 3.5)
 * Also save the generated key for decrypting objects and streams in crypt->key.
 */

static void
pdf_compute_user_password(pdf_crypt *crypt, unsigned char *password, int pwlen, unsigned char *output)
{
#ifdef DEBUG
	LOGD("pdf_compute_user_password: r=%d", crypt->r);
#endif
	if (crypt->r == 2)
	{
		fz_arc4 arc4;

#ifdef DEBUG
		LOGD("r==2", crypt->r);
#endif
		pdf_compute_encryption_key(crypt, password, pwlen, crypt->key);
		fz_arc4_init(&arc4, crypt->key, crypt->length / 8);
		fz_arc4_encrypt(&arc4, output, padding, 32);
	}

	if (crypt->r == 3 || crypt->r == 4)
	{
		unsigned char xors[32];
		unsigned char digest[16];
		fz_md5 md5;
		fz_arc4 arc4;
		int i, x, n;

		n = crypt->length / 8;

		pdf_compute_encryption_key(crypt, password, pwlen, crypt->key);

		fz_md5_init(&md5);
		fz_md5_update(&md5, padding, 32);
		fz_md5_update(&md5, crypt->id, crypt->id_len);
		fz_md5_final(&md5, digest);

		fz_arc4_init(&arc4, crypt->key, n);
		fz_arc4_encrypt(&arc4, output, digest, 16);

		for (x = 1; x <= 19; x++)
		{
			for (i = 0; i < n; i++)
				xors[i] = crypt->key[i] ^ x;
			fz_arc4_init(&arc4, xors, n);
			fz_arc4_encrypt(&arc4, output, output, 16);
		}

		memcpy(output + 16, padding, 16);
	}

	if (crypt->r == 5)
	{
		pdf_compute_encryption_key_r5(crypt, password, pwlen, 0, output);
	}
}

/*
 * Authenticating the user password (PDF 1.7 algorithm 3.6
 * and ExtensionLevel 3 algorithm 3.11)
 * This also has the side effect of saving a key generated
 * from the password for decrypting objects and streams.
 */

static int
pdf_authenticate_user_password(pdf_crypt *crypt, unsigned char *password, int pwlen)
{
#ifdef DEBUG
	LOGD("pdf_authenticate_user_password");
#endif
	unsigned char output[32];
	pdf_compute_user_password(crypt, password, pwlen, output);
	if (crypt->r == 2 || crypt->r == 5)
		return memcmp(output, crypt->u, 32) == 0;
	if (crypt->r == 3 || crypt->r == 4)
		return memcmp(output, crypt->u, 16) == 0;
	return 0;
}

/*
 * Authenticating the owner password (PDF 1.7 algorithm 3.7
 * and ExtensionLevel 3 algorithm 3.12)
 * Generates the user password from the owner password
 * and calls pdf_authenticate_user_password.
 */

static int
pdf_authenticate_owner_password(pdf_crypt *crypt, unsigned char *ownerpass, int pwlen)
{
	unsigned char pwbuf[32];
	unsigned char key[32];
	unsigned char xors[32];
	unsigned char userpass[32];
	int i, n, x;
	fz_md5 md5;
	fz_arc4 arc4;

	if (crypt->r == 5)
	{
		/* PDF 1.7 ExtensionLevel 3 algorithm 3.12 */

		pdf_compute_encryption_key_r5(crypt, ownerpass, pwlen, 1, key);

		return !memcmp(key, crypt->o, 32);
	}

	n = crypt->length / 8;

	/* Step 1 -- steps 1 to 4 of PDF 1.7 algorithm 3.3 */

	/* copy and pad password string */
	if (pwlen > 32)
		pwlen = 32;
	memcpy(pwbuf, ownerpass, pwlen);
	memcpy(pwbuf + pwlen, padding, 32 - pwlen);

	/* take md5 hash of padded password */
	fz_md5_init(&md5);
	fz_md5_update(&md5, pwbuf, 32);
	fz_md5_final(&md5, key);

	/* do some voodoo 50 times (Revision 3 or greater) */
	if (crypt->r >= 3)
	{
		for (i = 0; i < 50; i++)
		{
			fz_md5_init(&md5);
			fz_md5_update(&md5, key, 16);
			fz_md5_final(&md5, key);
		}
	}

	/* Step 2 (Revision 2) */
	if (crypt->r == 2)
	{
		fz_arc4_init(&arc4, key, n);
		fz_arc4_encrypt(&arc4, userpass, crypt->o, 32);
	}

	/* Step 2 (Revision 3 or greater) */
	if (crypt->r >= 3)
	{
		memcpy(userpass, crypt->o, 32);
		for (x = 0; x < 20; x++)
		{
			for (i = 0; i < n; i++)
				xors[i] = key[i] ^ (19 - x);
			fz_arc4_init(&arc4, xors, n);
			fz_arc4_encrypt(&arc4, userpass, userpass, 32);
		}
	}

	return pdf_authenticate_user_password(crypt, userpass, 32);
}

int 
pdf_authenticate_password(pdf_crypt *crypt, char *password)
{
#ifdef DEBUG
	LOGD("pdf_authenticate_password");
#endif
	if (crypt)
	{
		if (!password)
			password = (char*)"";
		if (pdf_authenticate_user_password(crypt, (unsigned char *)password, strlen(password)))
			return 1;
		if (pdf_authenticate_owner_password(crypt, (unsigned char *)password, strlen(password)))
			return 1;
		return 0;
	}
	return 1;
}

int
pdf_needs_password(pdf_crypt *crypt)
{
	if (!crypt)
		return 0;
	if (pdf_authenticate_password(crypt, (char*)""))
		return 0;
	return 1;
}

int
pdf_has_permission(pdf_crypt *crypt, int p)
{
	if (!crypt)
		return 1;
	return crypt->p & p;
}

unsigned char *
pdf_crypt_key(pdf_crypt *crypt)
{
	if (crypt)
		return crypt->key;
	return NULL;
}

int
pdf_crypt_version(pdf_crypt *crypt)
{
	if (crypt)
		return crypt->v;
	return 0;
}

int pdf_crypt_revision(pdf_crypt *crypt)
{
	if (crypt)
		return crypt->r;
	return 0;
}

int
pdf_crypt_length(pdf_crypt *crypt)
{
	if (crypt)
		return crypt->length;
	return 0;
}
