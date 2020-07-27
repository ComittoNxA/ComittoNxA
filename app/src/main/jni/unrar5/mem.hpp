#ifndef _RAR_MEM_
#define _RAR_MEM_

class Mem
{
	private:
		FILE *fp;
		byte	*wbuff;
		int		pos;

	public:
		Mem();
		virtual ~Mem();
		void Init(int size);
		int MemRead(byte *buf, size_t size);
		int MemWrite(byte *buf, size_t size);
};

#endif
