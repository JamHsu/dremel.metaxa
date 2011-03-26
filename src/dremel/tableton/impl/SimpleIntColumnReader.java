package dremel.tableton.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnReader;
import dremel.tableton.ColumnMetaData.ColumnType;

public class SimpleIntColumnReader implements ColumnReader {

	final static byte logTable[] = new byte[] { 0, 1, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4 };
	final static byte maskTable[] = new byte[] { 0, 0x1, 0x3, 0x3, 0x7, 0x7, 0x7, 0x7, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF };
	final static byte compTable[] = new byte[] { 0, 8, 4, 2, 2, 1, 1, 1, 1 };
	
	private class MappedFileData {
		public String fileName;
		public FileChannel channel;
		public MappedByteBuffer buffer;
		public int size; //in bytes, won't be bigger than max chunk anyway
		public int remainder; // how many entities left in last buffer
		
		public MappedFileData(String fileName) {
			this.fileName = fileName;
			try {
				this.channel = new RandomAccessFile(new File(fileName), "r").getChannel();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			try {
				this.buffer = channel.map(MapMode.READ_ONLY, 0L, 128 * 1024 * 1024);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.size = buffer.getInt();
			this.remainder = buffer.getInt();
			//System.out.println("Opened column: "+fileName+", size = "+size+" bytes, remainder = "+remainder+" items");
		}

		public void close() {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private ColumnMetaData columnMeta;
	private byte maxDef;
	private byte maxRep;
	private MappedFileData repFile;
	private MappedFileData defFile;
	private MappedFileData dataFile;
	private byte defBits;
	private byte repBits;
	private byte defComp;
	private byte repComp;
	
	private IntBuffer intBuffer;
	
	private static int bufSize = 2048;
	
	private byte[] repBuffer;
	private int repBufPos;
	
	private byte[] defBuffer;
	private int defBufPos;
	
	private int[] tempBuffer;
	private int bufPos;
	
	private byte[] repDictionary = null;
	private byte[] defDictionary = null;
	private byte defBytePos;
	private byte repBytePos;
	private byte currentRep;
	
	public SimpleIntColumnReader(ColumnMetaData columnMeta) {
		this.columnMeta = columnMeta;
		
		this.maxDef = columnMeta.getMaxDefinitionLevel();
		this.defBits = 8;
		if (maxDef < 16)
			this.defBits = logTable[maxDef];
		this.defComp = compTable[defBits];
		
		this.maxRep = columnMeta.getMaxRepetitionLevel();
		this.repBits = 8;
		if (maxRep < 16)
			this.repBits = logTable[maxRep];
		this.repComp = compTable[repBits];
		
		this.repFile = new MappedFileData(columnMeta.getFileSet().getRepFileName());
		this.defFile = new MappedFileData(columnMeta.getFileSet().getDefFileName());
		this.dataFile = new MappedFileData(columnMeta.getFileSet().getDataFileName());
		
		this.intBuffer = dataFile.buffer.asIntBuffer();
		
		tempBuffer = new int[bufSize];
		bufPos = 0;
		repBuffer = new byte[bufSize];
		repBufPos = 0;
		repBytePos= 0;
		defBuffer = new byte[bufSize];
		defBufPos = 0;
		defBytePos = 0;
		
		buildRepDict();
		buildDefDict();
		
		this.intBuffer.get(tempBuffer);
		this.repFile.buffer.get(repBuffer);
		this.defFile.buffer.get(defBuffer);
		
		if (repDictionary == null) {
			currentRep =  repBuffer[repBufPos];
		} else {
			currentRep = repDictionary[repBuffer[repBufPos] * repComp + repBytePos];
		}
	}
	
	private void buildRepDict() {
		if (repComp < 2)
			return; // no need in dictionary, direct storage of bytes
		byte mask = maskTable[maxRep];
		repDictionary = new byte[(int) 256 * repComp];
		for (int i = 0; i < 256; ++i) {
			for (byte j = 0; j < repComp; ++j) {
				byte shift = (byte) (j * repBits);
				repDictionary[i * repComp + j] = (byte) ((i & (mask << shift)) >> shift);
			}
		}
	}
	
	private void buildDefDict() {
		if (defComp < 2)
			return; // no need in dictionary, direct storage of bytes
		byte mask = maskTable[maxDef];
		defDictionary = new byte[256 * defComp];
		for (int i = 0; i < 256; ++i) {
			for (byte j = 0; j < defComp; ++j) {
				byte shift = (byte) (j * defBits);
				defDictionary[i * defComp + j] = (byte) ((i & (mask << shift)) >> shift);
			}
		}
	}

	@Override
	public ColumnType getDataType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isNull() {
		if (defDictionary == null) {
			if (defBuffer[defBufPos] < maxDef)
				return true;
			else
				return false;
		}
		if (defDictionary[(0xFF & defBuffer[defBufPos]) * defComp + defBytePos] < maxDef)
			return true;
		return false;
	}

	@Override
	public int getRepetitionLevel() {
		return currentRep;
	}

	public byte getDefLvl() {
		if (defDictionary == null) {
			return defBuffer[defBufPos];
		}
		return defDictionary[(0xFF & defBuffer[defBufPos]) * defComp + defBytePos];
	}
	
	@Override
	public int nextRepetitionLevel() {
		if (repDictionary == null) {
			if (repBufPos == bufSize - 1) {
				if (this.repFile.buffer.position() == repFile.size)
					return EOF_REPETITION_LEVEL;
				if (this.repFile.buffer.remaining() == 0)
					return EOF_REPETITION_LEVEL;
				this.repFile.buffer.get(repBuffer);
				repBufPos = -1;
			}
			if (repFile.buffer.position() == repFile.size && repFile.remainder == repBufPos + 1)
				return EOF_REPETITION_LEVEL;
			return repBuffer[repBufPos + 1];
		} else {
			if (repBufPos == bufSize - 1 && repBytePos == repComp - 1) {
				if (repFile.buffer.position() == repFile.size)
					return EOF_REPETITION_LEVEL;
				if (repFile.buffer.remaining() == 0)
					return EOF_REPETITION_LEVEL;
				this.repFile.buffer.get(repBuffer);
				repBufPos = 0;
				repBytePos = -1;
			}
			if (repBytePos == repComp - 1) {
				++repBufPos;
				repBytePos = -1;
			}
			if (repFile.buffer.position() == repFile.size && repFile.remainder == repBufPos * repComp + repBytePos + 1)
				return EOF_REPETITION_LEVEL;
			return repDictionary[(0xFF & repBuffer[repBufPos]) * repComp + repBytePos + 1];
		}
	}

	@Override
	public boolean next() {
		if (defDictionary == null) {
			if (defBufPos == bufSize - 1) {
				if (defFile.buffer.position() == defFile.size)
					return false; //no more def levels to read
				if (defFile.buffer.remaining() == 0)
					return false; //no more def levels to read
				defFile.buffer.get(defBuffer);
				defBufPos = -1;
			}
			if (defFile.buffer.position() == defFile.size && defFile.remainder == defBufPos + 1)
				return false; //no more def levels to read
			++defBufPos;
		} else {
			if (defBufPos == bufSize - 1 && defBytePos == defComp - 1) {
				if (defFile.buffer.position() == defFile.size)
					return false;
				if (defFile.buffer.remaining() == 0)
					return false;
				defFile.buffer.get(defBuffer);
				defBufPos = 0;
				defBytePos = -1;
			}
			if (defBytePos == defComp - 1) {
				++defBufPos;
				defBytePos = -1;
			}
			if (defFile.buffer.position() == defFile.size && defFile.remainder == defBufPos * defComp + defBytePos + 1)
				return false;
			++defBytePos;
		}
		
		if (repDictionary == null) {
			if (repBufPos == bufSize - 1) {
				if (repFile.buffer.position() == repFile.size)
					return false; //no more def levels to read
				if (repFile.buffer.remaining() == 0)
					return false; //no more def levels to read
				repFile.buffer.get(repBuffer);
				repBufPos = -1;
			}
			if (repFile.buffer.position() == repFile.size && repFile.remainder == repBufPos + 1)
				return false; //no more def levels to read
			++repBufPos;
			currentRep = repBuffer[repBufPos];
		} else {
			if (repBufPos == bufSize - 1 && repBytePos == repComp - 1) {
				if (repFile.buffer.position() == repFile.size)
					return false;
				if (repFile.buffer.remaining() == 0)
					return false;
				repFile.buffer.get(repBuffer);
				repBufPos = 0;
				repBytePos = -1;
			}
			if (repBytePos == repComp - 1) {
				++repBufPos;
				repBytePos = -1;
			}
			if (repFile.buffer.position() == repFile.size && repFile.remainder == repBufPos * repComp + repBytePos + 1)
				return false;
			++repBytePos;
			currentRep = repDictionary[(0xFF & repBuffer[repBufPos]) * repComp + repBytePos];
		}
		if (! isNull()) {
			if (bufPos == bufSize - 1) {
				if (dataFile.buffer.position() == dataFile.size)
					return false; //no more data to read, wtf?
				if (dataFile.buffer.remaining() == 0)
					return false; //no more data to read, wtf??
				intBuffer.get(tempBuffer);
				bufPos = -1;
			}
			if (dataFile.buffer.position() == dataFile.size && dataFile.remainder == bufPos + 1)
				return false; //no more data to read, wtf???
			++bufPos;
		}
		return true;
	}

	@Override
	public int getIntValue() {
		return tempBuffer[bufPos];
	}

	@Override
	public int getLongValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getByteValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getStringValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int fillByteValues(byte[] dataBuffer, byte[] repetitionBuffer,
			boolean[] isNullBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int fillIntValues(int[] dataBuffer, byte[] repetitionBuffer,
			boolean[] isNullBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDictionaryDataSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int fillStringDictionary(byte[] dataBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int fillStringOffsets(int[] dataBuffer, byte[] repetitionBuffer,
			boolean[] isNullBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int fillStringRowData(byte[] dataBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int filllevelData(byte[] repetitionBuffer, boolean[] isNullBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

}
