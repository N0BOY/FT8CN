package com.bg7yoz.ft8cn.wave;

/**
 * Wave文件操作所用的常量。
 * 已经弃用。FT8CN目前不采用文件方式处理音频。
 * @author BGY70Z
 * @date 2023-03-20
 */
public final class WaveConstants {
	static public int LENCHUNKDESCRIPTOR = 4;
	static public int LENCHUNKSIZE = 4;
	static public int LENWAVEFLAG = 4;
	static public int LENFMTSUBCHUNK = 4;
	static public int LENSUBCHUNK1SIZE = 4;
	static public int LENAUDIOFORMAT = 2;
	static public int LENNUMCHANNELS = 2;
	static public int LENSAMPLERATE = 2;
	static public int LENBYTERATE = 4;
	static public int LENBLOCKLING = 2;
	static public int LENBITSPERSAMPLE = 2;
	static public int LENDATASUBCHUNK = 4;
	static public int LENSUBCHUNK2SIZE = 4;
	
	public static String CHUNKDESCRIPTOR = "RIFF";
	public static String WAVEFLAG = "WAVE";
	public static String FMTSUBCHUNK = "fmt ";
	public static String DATASUBCHUNK = "data";
}
