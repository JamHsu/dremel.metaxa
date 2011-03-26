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
import dremel.tableton.ColumnWriter;

public class SimpleIntColumnWriter implements ColumnWriter {
	
	final static byte logTable[] = new byte[] { 0, 1, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4 };
	final static byte compTable[] = new byte[] { 0, 8, 4, 2, 2, 1, 1, 1, 1 };
	
	private class MappedFileData {
		public String fileName;
		public FileChannel channel;
		public MappedByteBuffer buffer;
		public int size; //in bytes, won't be bigger than max chunk anyway
		
		public MappedFileData(String fileName) {
			this.fileName = fileName;
			try {
				this.channel = new RandomAccessFile(new File(fileName), "rw").getChannel();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			try {
				this.buffer = channel.map(MapMode.READ_WRITE, 0L, 128 * 1024 * 1024);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.size = 0;
			buffer.rewind();
			buffer.putInt(0).putInt(0);
		}

//		public void close() {
//			size = buffer.position();
//			buffer.rewind();
//			buffer.putInt(size);
//			buffer.force();
//			try {
//				channel.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		
		public void close(int remainder) {
			size = buffer.position();
			buffer.rewind();
			buffer.putInt(size);
			buffer.putInt(remainder);
			buffer.force();
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
//	private byte[] defBitBuffer;
//	private byte[] repBitBuffer;
	private byte repIndex;
	private byte repByte;
	private byte defIndex;
	private byte defByte;
	private IntBuffer intBuffer;
	
	private static int bufSize = 2048;
	
	private byte[] repBuffer;
	private int repBufPos;
	
	private byte[] defBuffer;
	private int defBufPos;
	
	private int[] tempBuffer;
	private int bufPos;

	public SimpleIntColumnWriter(ColumnMetaData columnMeta) {
		
		this.columnMeta = columnMeta;
		
		this.maxDef = columnMeta.getMaxDefinitionLevel();
		this.defBits = 8;
		if (maxDef < 16)
			this.defBits = logTable[maxDef];
		this.defComp = compTable[defBits];
		//this.defBitBuffer = new byte[defComp];
		
		this.maxRep = columnMeta.getMaxRepetitionLevel();
		this.repBits = 8;
		if (maxRep < 16)
			this.repBits = logTable[maxRep];
		this.repComp = compTable[repBits];
		//this.repBitBuffer = new byte[repComp];
		
		this.repFile = new MappedFileData(columnMeta.getFileSet().getRepFileName());
		this.defFile = new MappedFileData(columnMeta.getFileSet().getDefFileName());
		this.dataFile = new MappedFileData(columnMeta.getFileSet().getDataFileName());
		
		this.intBuffer = dataFile.buffer.asIntBuffer();
		
		tempBuffer = new int[bufSize];
		bufPos = 0;
		
		repBuffer = new byte[bufSize];
		repBufPos = 0;
		defBuffer = new byte[bufSize];
		defBufPos = 0;
	}
	
	@Override
	public void addStringDataTriple(String data, boolean isNull, byte repLevel,
			byte defLevel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addIntDataTriple(int data, boolean isNull, byte repLevel, byte defLevel) {
		
		if (! isNull) {
			if (bufPos == bufSize) {
				intBuffer.put(tempBuffer);
				bufPos = 0;
			}
			tempBuffer[bufPos] = data;
			++bufPos;
		}
		if (repComp > 1) {
			if (repIndex < repComp) { 
				repByte |= repLevel << (repIndex * repBits);;
			} else {
				if (repBufPos == bufSize) {
					repFile.buffer.put(repBuffer);
					repBufPos = 0;
				}
				repBuffer[repBufPos] = repByte;
				++repBufPos;
				//repFile.buffer.put(repByte);
				repByte = repLevel;
				repIndex = 0;
			}
			++repIndex;
		} else {
			if (repBufPos == bufSize) {
				repFile.buffer.put(repBuffer);
				repBufPos = 0;
			}
			repBuffer[repBufPos] = repByte;
			++repBufPos;
			//repFile.buffer.put(repLevel);
		}
		if (defComp > 1) {
			if (defIndex < defComp) { 
				defByte |= defLevel << (defIndex * defBits);
			} else {
				if (defBufPos == bufSize) {
					defFile.buffer.put(defBuffer);
					defBufPos = 0;
				}
				defBuffer[defBufPos] = defByte;
				++defBufPos;
				//defFile.buffer.put(defByte);
				defByte = defLevel;
				defIndex = 0;
			}
			++defIndex;
		} else {
			defFile.buffer.put(defLevel);
		}
	}

	public void addIntDataTriple(int[] dataA, boolean[] isNullA, byte[] repLevelA, byte[] defLevelA) {

		for (int i = 0; i < dataA.length; ++i) {
			
			if (! isNullA[i]) {
				if (bufPos == bufSize) {
					intBuffer.put(tempBuffer);
					bufPos = 0;
				}
				tempBuffer[bufPos] = dataA[i];
				++bufPos;
			}
			if (repComp > 1) {
				if (repIndex < repComp) { 
					repByte |= repLevelA[i] << (repIndex * repBits);
				} else {
					if (repBufPos == bufSize) {
						repFile.buffer.put(repBuffer);
						repBufPos = 0;
					}
					repBuffer[repBufPos] = repByte;
					++repBufPos;
					//repFile.buffer.put(repByte);
					repByte = repLevelA[i];
					repIndex = 0;
				}
				++repIndex;
			} else {
				if (repBufPos == bufSize) {
					repFile.buffer.put(repBuffer);
					repBufPos = 0;
				}
				repBuffer[repBufPos] = repByte;
				++repBufPos;
				//repFile.buffer.put(repLevel);
			}
			if (defComp > 1) {
				if (defIndex < defComp) { 
					defByte |= defLevelA[i] << (defIndex * defBits);
				} else {
					if (defBufPos == bufSize) {
						defFile.buffer.put(defBuffer);
						defBufPos = 0;
					}
					defBuffer[defBufPos] = defByte;
					++defBufPos;
					//defFile.buffer.put(defByte);
					defByte = defLevelA[i];
					defIndex = 0;
				}
				++defIndex;
			} else {
				defFile.buffer.put(defLevelA[i]);
			}
		}
	}

	@Override
	public void setNullValue(byte repLevel, byte defLevel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		intBuffer.put(tempBuffer);
		this.dataFile.buffer.position(this.dataFile.buffer.position() + intBuffer.position() * 4);
		this.dataFile.close(bufPos);
		
		if (repBufPos == bufSize) {
			repFile.buffer.put(repBuffer);
			repBufPos = 0;
		}
		repBuffer[repBufPos] = repByte;
		//++repBufPos;
		repFile.buffer.put(repBuffer);
		this.repFile.close(repBufPos * repComp + repIndex);
		
		if (defBufPos == bufSize) {
			defFile.buffer.put(defBuffer);
			defBufPos = 0;
		}
		defBuffer[defBufPos] = defByte;
		//++defBufPos;
		defFile.buffer.put(defBuffer);
		this.defFile.close(defBufPos * defComp + defIndex);
		
	}
/*
	private void writeDefLevels() {
		byte result = 0;
		for (byte i = 0; i < defComp; ++i) {
			result |= defBitBuffer[i] << (i * defBits);
		}
		defFile.buffer.put(result);
	}
	
	private void writeRepLevels() {
		byte result = 0;
		for (byte i = 0; i < repComp; ++i) {
			result |= repBitBuffer[i] << (i * repBits);
		}
		repFile.buffer.put(result);
	}*/
}
