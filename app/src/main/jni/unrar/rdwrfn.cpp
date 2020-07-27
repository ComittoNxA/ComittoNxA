#include "rar.hpp"

ComprDataIO::ComprDataIO()
{
  Init();
}


void ComprDataIO::Init()
{
#if 0	// COMITTON_MOD
  UnpackFromMemory=false;
  UnpackToMemory=false;
#endif
  UnpPackedSize=0;
#if 0	// COMITTON_MOD
  ShowProgress=true;
#endif
  TestMode=false;
  SkipUnpCRC=false;
  PackVolume=false;
  UnpVolume=false;
  NextVolumeMissing=false;
#if 0	// COMITTON_MOD
  SrcFile=NULL;
  DestFile=NULL;
#endif
  UnpWrSize=0;
  Command=NULL;
  Encryption=0;
  Decryption=0;
  TotalPackRead=0;
  CurPackRead=CurPackWrite=CurUnpRead=CurUnpWrite=0;
  PackFileCRC=UnpFileCRC=PackedCRC=0xffffffff;
  LastPercent=-1;
  SubHead=NULL;
  SubHeadPos=NULL;
  CurrentCommand=0;
#if 0	// COMITTON_MOD
  ProcessedArcSize=TotalArcSize=0;
#endif
#if 1	// COMITTON_MOD
  OldFormat = false;
#endif
}

int ComprDataIO::UnpRead(byte *Addr,size_t Count)
{
  int RetCode=0,TotalRead=0;
  byte *ReadAddr;
  ReadAddr=Addr;
  while (Count > 0)
  {
#if 0	// COMITTON_MOD
    Archive *SrcArc=(Archive *)SrcFile;
#endif

    size_t ReadSize=((int64)Count>UnpPackedSize) ? (size_t)UnpPackedSize:Count;
#if 1	// COMITTON_MOD

	memcpy(Addr, UnpackFromMemoryAddr, ReadSize);
    RetCode = (int)ReadSize;
    UnpackFromMemorySize -= ReadSize;
	UnpackFromMemoryAddr += ReadSize;

#else
    if (UnpackFromMemory)
    {
      memcpy(Addr,UnpackFromMemoryAddr,UnpackFromMemorySize);
      RetCode=(int)UnpackFromMemorySize;
      UnpackFromMemorySize=0;
    }
    else
    {
      if (!SrcFile->IsOpened())
        return(-1);
      RetCode=SrcFile->Read(ReadAddr,ReadSize);
      FileHeader *hd=SubHead!=NULL ? SubHead:&SrcArc->NewLhd;
      if (hd->Flags & LHD_SPLIT_AFTER)
        PackedCRC=CRC(PackedCRC,ReadAddr,RetCode);
    }
#endif
	CurUnpRead+=RetCode;
    TotalRead+=RetCode;
#ifndef NOVOLUME
    // These variable are not used in NOVOLUME mode, so it is better
    // to exclude commands below to avoid compiler warnings.
    ReadAddr+=RetCode;
    Count-=RetCode;
#endif
    UnpPackedSize-=RetCode;
#if 0	// COMITTON_MOD
    if (UnpPackedSize == 0 && UnpVolume)
    {
#ifndef NOVOLUME
      if (!MergeArchive(*SrcArc,this,true,CurrentCommand))
#endif
      {
        NextVolumeMissing=true;
        return(-1);
      }
    }
    else
#endif
      break;
  }
#if 0	// COMITTON_MOD
  Archive *SrcArc=(Archive *)SrcFile;
  if (SrcArc!=NULL)
    ShowUnpRead(SrcArc->CurBlockPos+CurUnpRead,UnpArcSize);
#endif
  if (RetCode!=-1)
  {
    RetCode=TotalRead;
#ifndef RAR_NOCRYPT
    if (Decryption) {
#ifndef SFX_MODULE
        if (Decryption < 20) {
            Decrypt.Crypt(Addr, RetCode, (Decryption == 15) ? NEW_CRYPT : OLD_DECODE);
        } else {
            if (Decryption == 20) {
                for (int I = 0; I < RetCode; I += 16)
            Decrypt.DecryptBlock20(&Addr[I]);
            } else
#endif
        {
                int CryptSize = (RetCode & 0xf) == 0 ? RetCode : ((RetCode & ~0xf) + 16);
                Decrypt.DecryptBlock(Addr, CryptSize);
        }
#endif
  }
    }
  }
#if 0	// COMITTON_MOD
  Wait();
#endif
  return(RetCode);
}


#if defined(RARDLL) && defined(_MSC_VER) && !defined(_WIN_64)
// Disable the run time stack check for unrar.dll, so we can manipulate
// with ProcessDataProc call type below. Run time check would intercept
// a wrong ESP before we restore it.
#pragma runtime_checks( "s", off )
#endif

void ComprDataIO::UnpWrite(byte *Addr,size_t Count)
{
#if 0	// COMITTON_MOD

#ifdef RARDLL
  RAROptions *Cmd=((Archive *)SrcFile)->GetRAROptions();
  if (Cmd->DllOpMode!=RAR_SKIP)
  {
    if (Cmd->Callback!=NULL &&
        Cmd->Callback(UCM_PROCESSDATA,Cmd->UserData,(LPARAM)Addr,Count)==-1)
      ErrHandler.Exit(USER_BREAK);
    if (Cmd->ProcessDataProc!=NULL)
    {
      // Here we preserve ESP value. It is necessary for those developers,
      // who still define ProcessDataProc callback as "C" type function,
      // even though in year 2001 we announced in unrar.dll whatsnew.txt
      // that it will be PASCAL type (for compatibility with Visual Basic).
#if defined(_MSC_VER)
#ifndef _WIN_64
      __asm mov ebx,esp
#endif
#elif defined(_WIN_ALL) && defined(__BORLANDC__)
      _EBX=_ESP;
#endif
      int RetCode=Cmd->ProcessDataProc(Addr,(int)Count);

      // Restore ESP after ProcessDataProc with wrongly defined calling
      // convention broken it.
#if defined(_MSC_VER)
#ifndef _WIN_64
      __asm mov esp,ebx
#endif
#elif defined(_WIN_ALL) && defined(__BORLANDC__)
      _ESP=_EBX;
#endif
      if (RetCode==0)
        ErrHandler.Exit(USER_BREAK);
    }
  }
#endif // RARDLL

#endif
	UnpWrAddr=Addr;
	UnpWrSize=Count;
#if 0	// COMITTON_MOD
  if (UnpackToMemory)
  {
#endif
	if (Count <= UnpackToMemorySize)
	{
	  memcpy(UnpackToMemoryAddr,Addr,Count);
	  UnpackToMemoryAddr+=Count;
	  UnpackToMemorySize-=Count;
	}
#if 0	// COMITTON_MOD
  }
  else
    if (!TestMode)
      DestFile->Write(Addr,Count);
#endif
	CurUnpWrite+=Count;
#if 1	// COMITTON_MOD
	if (!SkipUnpCRC) {
    if (OldFormat)
        UnpFileCRC = OldCRC((ushort) UnpFileCRC, Addr, Count);
    else
        UnpFileCRC = CRC(UnpFileCRC, Addr, Count);
    }
#else
  if (!SkipUnpCRC)
#ifndef SFX_MODULE
    if (((Archive *)SrcFile)->OldFormat)
      UnpFileCRC=OldCRC((ushort)UnpFileCRC,Addr,Count);
    else
#endif
      UnpFileCRC=CRC(UnpFileCRC,Addr,Count);
#endif
#if 0	// COMITTON_MOD
  ShowUnpWrite();
  Wait();
#endif
}

#if defined(RARDLL) && defined(_MSC_VER) && !defined(_WIN_64)
// Restore the run time stack check for unrar.dll.
#pragma runtime_checks( "s", restore )
#endif



#if 0	// COMITTON_MOD
void ComprDataIO::ShowUnpRead(int64 ArcPos,int64 ArcSize)
{
  if (ShowProgress && SrcFile!=NULL)
  {
    if (TotalArcSize!=0)
    {
      // important when processing several archives or multivolume archive
      ArcSize=TotalArcSize;
      ArcPos+=ProcessedArcSize;
    }

    Archive *SrcArc=(Archive *)SrcFile;
    RAROptions *Cmd=SrcArc->GetRAROptions();

    int CurPercent=ToPercent(ArcPos,ArcSize);
    if (!Cmd->DisablePercentage && CurPercent!=LastPercent)
    {
      mprintf("\b\b\b\b%3d%%",CurPercent);
      LastPercent=CurPercent;
    }
  }
}

void ComprDataIO::ShowUnpWrite()
{
}





void ComprDataIO::SetFiles(File *SrcFile,File *DestFile)
{
  if (SrcFile!=NULL)
    ComprDataIO::SrcFile=SrcFile;
  if (DestFile!=NULL)
    ComprDataIO::DestFile=DestFile;
  LastPercent=-1;
}
#endif


void ComprDataIO::GetUnpackedData(byte **Data,size_t *Size)
{
  *Data=UnpWrAddr;
  *Size=UnpWrSize;
}


#if 0	// COMITTON_MOD
void ComprDataIO::SetEncryption(int Method,const wchar *Password,const byte *Salt,bool Encrypt,bool HandsOffHash)
{
  if (Encrypt)
  {
    Encryption=*Password ? Method:0;
#ifndef RAR_NOCRYPT
    Crypt.SetCryptKeys(Password,Salt,Encrypt,false,HandsOffHash);
#endif
  }
  else
  {
    Decryption=*Password ? Method:0;
#ifndef RAR_NOCRYPT
    Decrypt.SetCryptKeys(Password,Salt,Encrypt,Method<29,HandsOffHash);
#endif
  }
}
#endif

#if !defined(SFX_MODULE) && !defined(RAR_NOCRYPT)
void ComprDataIO::SetAV15Encryption()
{
  Decryption=15;
  Decrypt.SetAV15Encryption();
}
#endif


#if !defined(SFX_MODULE) && !defined(RAR_NOCRYPT)
void ComprDataIO::SetCmt13Encryption()
{
  Decryption=13;
  Decrypt.SetCmt13Encryption();
}
#endif

void ComprDataIO::SetUnpackToMemory(byte *Addr,uint Size)
{
  UnpackToMemory=true;
  UnpackToMemoryAddr=Addr;
  UnpackToMemorySize=Size;
}

#if 1	// COMITTON_MOD
void ComprDataIO::SetUnpackFromMemory(byte *Addr,uint Size)
{
  UnpackFromMemory=true;
  UnpackFromMemoryAddr=Addr;
  UnpackFromMemorySize=Size;
}
#endif
