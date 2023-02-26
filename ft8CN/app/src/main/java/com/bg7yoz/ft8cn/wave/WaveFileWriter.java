package com.bg7yoz.ft8cn.wave;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WaveFileWriter {
	private String filename = null;
	private FileOutputStream fos = null;
	private BufferedOutputStream bos = null;

	private long chunksize = 0;
	private long subchunk1size = 0;
	private int audioformat = 0;
	private int numchannels = 0;
	private long samplerate = 0;
	private long byterate = 0;
	private int blockalign = 0;
	private int bitspersample = 0;
	private long subchunk2size = 0;

	public WaveFileWriter(String filename, int[][] data, long samplerate,int bitspersample) {
		this.initWriter(filename, data, 0, data[0].length, samplerate,bitspersample);
	}

	public WaveFileWriter(String filename, int[][] data, int offset, int len, long samplerate,int bitspersample) {
		this.initWriter(filename, data, offset, len, samplerate,bitspersample);
	}

	public void initWriter(String filename, int[][] data, int offset, int len, long samplerate,int bitspersample) {
		this.filename = filename;

		try {
			fos = new FileOutputStream(this.filename);
			bos = new BufferedOutputStream(fos);

			// int datalen = data[0].length;
			int datalen = len;

			this.samplerate = samplerate;
			// this.bitspersample = bitspersample;
			this.bitspersample = bitspersample;
			this.numchannels = data.length;
			this.subchunk2size = this.numchannels * (this.bitspersample / 8) * datalen;
			this.subchunk1size = 16;
			this.audioformat = 1; // PCM
			this.byterate = this.samplerate * this.bitspersample * this.numchannels / 8;
			this.blockalign = this.numchannels * this.bitspersample / 8;

			this.chunksize = this.subchunk2size + 8 + this.subchunk1size + 8 + 4;

			writeString(WaveConstants.CHUNKDESCRIPTOR, WaveConstants.LENCHUNKDESCRIPTOR);
			writeLong(this.chunksize);
			writeString(WaveConstants.WAVEFLAG, WaveConstants.LENWAVEFLAG);
			writeString(WaveConstants.FMTSUBCHUNK, WaveConstants.LENFMTSUBCHUNK);
			writeLong(this.subchunk1size);
			writeInt(this.audioformat);
			writeInt(this.numchannels);
			writeLong(this.samplerate);
			writeLong(this.byterate);
			writeInt(this.blockalign);
			writeInt(this.bitspersample);
			writeString(WaveConstants.DATASUBCHUNK, WaveConstants.LENDATASUBCHUNK);
			writeLong(this.subchunk2size);
			for (int i = 0; i < datalen; ++i) {
				for (int n = 0; n < this.numchannels; ++n) {
					if (this.bitspersample == 16) {
						writeInt(data[n][i + offset]);
					} else {
						writeByte((byte) data[n][i + offset]);
					}
				}
			}
			bos.flush();
			fos.flush();
			bos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
   // TODO document why this method is empty
 }

	private void writeString(String str, int len) {
		if (str.length() != len) {
			throw new IllegalArgumentException("length not match!!!");
		}
		byte[] bt = str.getBytes();
		try {
			bos.write(bt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeByte(byte data) {
		try {
			bos.write(new byte[] { data }, 0, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeInt(int data) {
		byte[] buf = new byte[2];
		buf[1] = (byte) (data >>> 8);
		buf[0] = (byte) (data & 0xFF);
		try {
			bos.write(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeLong(long data) {
		byte[] buf = new byte[4];
		buf[0] = (byte) (data & 0x00ff);
		buf[1] = (byte) ((data >> 8) & 0x00ff);
		buf[2] = (byte) ((data >> 16) & 0x00ff);
		buf[3] = (byte) ((data >> 24) & 0x00ff);
		try {
			bos.write(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean saveSingleChannel(String filename, int[] data, long samplerate,int bitspersample) {
		int[][] datar = new int[1][];
		datar[0] = data;
		WaveFileWriter writer = new WaveFileWriter(filename, datar, samplerate,bitspersample);
		writer.close();
		return true;
	}
}
