/*
 * This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */
package mp3;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

public class Decoder {
	public static final int BUFFER_SIZE = 2 * 1152;
	public static final int MAX_CHANNELS = 2;
	private static final boolean BENCHMARK = false;

	protected final int[] bufferPointer = new int[MAX_CHANNELS];
	protected int channels;
	private SynthesisFilter filter1;
	private SynthesisFilter filter2;
	private Layer3Decoder l3decoder;
	private boolean initialized;

	private SourceDataLine line;
	private final byte[] buffer = new byte[BUFFER_SIZE * 2];
	private boolean stop;
	private volatile boolean pause;

	//    public static void mainBits(String[] args) throws Exception {
	//        Decoder.testBits();
	//        Decoder.testBits();
	//    }

	//    public static void testBits() throws Exception {
	//        Random r = new Random();
	//        byte[] data = new byte[2000];
	//        r.nextBytes(data);
	//        BitReservoir bytes = new BitReservoir2();
	//        BitReservoir2 bytes2 = new BitReservoir2();
	//        for(int i=0; i<data.length; i++) {
	//            bytes.putByte(data[i] & 0xff);
	//            bytes2.putByte(data[i] & 0xff);
	//        }
	//        long time = System.currentTimeMillis();
	//        for(int a = 0; a<30000; a++) {
	//            for(int i=0; i<data.length * 8; i++) {
	////                int b = bytes.getOneBit();
	//                int b2 = bytes2.getOneBit();
	//            }
	//        }
	//        // old: 7188, 11625
	//        // int:
	//        // byte: 6219,4484
	//        System.out.println(System.currentTimeMillis() - time);
	//    }

	//    public static void main(String[] a) throws Exception {
	//        Decoder decoder = new Decoder();
	//        BENCHMARK = false;
	//        for (int i = 0; i < a.length; i++) {
	//            decoder.playAll(new File(a[i]));
	//        }
	//        BENCHMARK = false;
	//        decoder.mainTest();
	//        System.out.println(Layer3Decoder.count);
	//        mainTest();
	//        System.exit(0);
	//        mainTest();
	//        mainTest();
	//    }

	// private void mainTest() throws Exception {
	//        long time = System.currentTimeMillis();
	//        play(new File("C:/temp/01-Where I Stood.mp3"));
	//        play(new File("C:/music/Missy Higgins/On A Clear Night/05-Secret.mp3"));
	//        playAll(new File("C:/music/Missy Higgins/On A Clear Night"));
	//        playAll(new File("C:/music/Vanessa-Mae"));
	//        playAll(new File("C:/music/s"));
	//        playAll(new File("C:/music/s/Shakira/Shakira - Grandes Exitos - Ojos As�.mp3"));
	//        playAll(new File("C:/music/o/Orishas - El Kilo - El Kilo.mp3"));
	//        playAll(new File("C:/music/s/Shakira"));
	//        playAll(new File("C:/music"));
	//        System.out.println("done in " + (System.currentTimeMillis() - time));
	//    }

	//    private void playAll(File file) throws Exception {
	//        if (file.isDirectory()) {
	//            File[] list = file.listFiles();
	//            for (int i = 0; i < list.length; i++) {
	//                File temp = list[i];
	//                int x = (int) (Math.random() * list.length);
	//                list[i] = list[x];
	//                list[x] = temp;
	//            }
	//            for (int i = 0; i < list.length; i++) {
	//                playAll(list[i]);
	//            }
	//        } else {
	//            play(new BufferedInputStream(new FileInputStream(file), 4 * 1024));
	//        }
	//    }

	public void decodeFrame(Header header, Bitstream stream) throws IOException {
		if (!initialized) {
			double scaleFactor = 32700.0f;
			int mode = header.mode();
			int channels = mode == Header.MODE_SINGLE_CHANNEL ? 1 : 2;
			filter1 = new SynthesisFilter(0, scaleFactor);
			if (channels == 2) {
				filter2 = new SynthesisFilter(1, scaleFactor);
			}
			initialized = true;
		}
		if (l3decoder == null) {
			l3decoder = new Layer3Decoder(stream, header, filter1, filter2,
					this);
		}
		l3decoder.decodeFrame();
		writeBuffer();
	}

	protected void initOutputBuffer(SourceDataLine line, int numberOfChannels) {
		this.line = line;
		channels = numberOfChannels;
		for (int i = 0; i < channels; i++) {
			bufferPointer[i] = i + i;
		}
	}

	public void appendSamples(int channel, double[] f) {
		int p = bufferPointer[channel];
		for (int i = 0; i < 32; i++) {
			double sample = f[i];
			int s = (int) ((sample > 32767.0f) ? 32767 : ((sample < -32768.0f) ? -32768 : sample));
			buffer[p] = (byte) (s >> 8);
			buffer[p + 1] = (byte) (s & 0xff);
			p += 4;
		}
		bufferPointer[channel] = p;
	}

	protected void writeBuffer() throws IOException {
		if (line != null) {
			line.write(buffer, 0, bufferPointer[0]);
		}
		for (int i = 0; i < channels; i++) {
			bufferPointer[i] = i + i;
		}
	}

	public void loop(String name, InputStream in, Boolean shouldFade) throws IOException {
		stop=false;
		BufferedInputStream bin = new BufferedInputStream(in, 128 * 1024);
		play(name, bin, shouldFade);
		in.close();
		bin.close();
		if(stop)
			return;
		in=getClass().getResourceAsStream(name);
		while(!stop) {
			bin = new BufferedInputStream(in, 128 * 1024);
			play(name, bin, false);
			in.close();
			bin.close();
			if(stop)
				return;
			in=getClass().getResourceAsStream(name);
		}
	}

	public void loop(String name, InputStream in, Integer numTimes, Boolean shouldFade) throws IOException {
		BufferedInputStream bin = new BufferedInputStream(in, 128 * 1024);
		play(name, bin, shouldFade);
		in.close();
		bin.close();
		if(stop)
			return;
		in=getClass().getResourceAsStream(name);
		for(int i=0; i<numTimes-1; i++) {
			bin = new BufferedInputStream(in, 128 * 1024);
			play(name, bin, false);
			in.close();
			bin.close();
			if(stop)
				return;
			in=getClass().getResourceAsStream(name);
		}
	}

	public void loop(String name, InputStream in, Integer numTimes, Long offset, Boolean shouldFade) throws IOException {
		BufferedInputStream bin = new BufferedInputStream(in, 128 * 1024);
		play(name, bin, shouldFade);
		in.close();
		bin.close();
		if(stop)
			return;
		for(int i=0; i<numTimes-1; i++) {
			in=getClass().getResourceAsStream(name);
			bin = new BufferedInputStream(in, 128 * 1024);
			bin.skip(offset);
			play(name, bin, false);
			in.close();
			bin.close();
			if(stop)
				return;
		}
	}

	public void loop(String name, InputStream in, Long offset, Boolean shouldFade) throws IOException {
		BufferedInputStream bin = new BufferedInputStream(in, 128 * 1024);
		play(name, bin, shouldFade);
		//in.close();
		//bin.close();
		if(stop)
			return;
		while(!stop) {
			in=getClass().getResourceAsStream(name);
			bin = new BufferedInputStream(in, 128 * 1024);
			bin.skip(offset);
			play(name, bin, false);
			//in.close();
			//bin.close();
			if(stop) {
				bin.close();
				return;
			}
		}
	}

	public void play(String name, InputStream in, Boolean shouldFade) throws IOException {
		stop = false;
		int frameCount = Integer.MAX_VALUE;

		// int testing;
		// frameCount = 100;
		
		if(shouldFade)
			try{Thread.sleep(250);}catch(InterruptedException e){e.printStackTrace();}

		Decoder decoder = new Decoder();
		Bitstream stream = new Bitstream(in);
		line = null;
		int error = 0;
		for (int frame = 0; !stop && frame < frameCount; frame++) {
			if (pause) {
				line.stop();
				while (pause && !stop) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// ignore
					}
				}
				line.flush();
				line.start();
			}
			try {
				Header header = stream.readFrame();
				if (header == null) {
					break;
				}
				if (decoder.channels == 0) {
					int channels = (header.mode() == Header.MODE_SINGLE_CHANNEL) ? 1 : 2;
					float sampleRate = header.frequency();
					int sampleSize = 16;
					AudioFormat format = new AudioFormat(
							AudioFormat.Encoding.PCM_SIGNED, sampleRate,
							sampleSize, channels, channels * (sampleSize / 8),
							sampleRate, true);
					// big endian
					SourceDataLine.Info info = new DataLine.Info(
							SourceDataLine.class, format);
					line = (SourceDataLine) AudioSystem.getLine(info);
					if (BENCHMARK) {
						decoder.initOutputBuffer(null, channels);
					} else {
						decoder.initOutputBuffer(line, channels);
					}
					// todo sometimes the line can not be opened (maybe not enough system resources?): display error message
					// System.out.println(line.getFormat().toString());
					line.open(format);
					line.start();
					if(shouldFade)
						fadeIn();
				}
				while (line.available() < 100) {
					Thread.yield();
					Thread.sleep(200);
				}
				decoder.decodeFrame(header, stream);
			} catch (Exception e) {
				System.out.println("Error at: " + name + " Frame: " + frame + " Error: " + e.toString());
				e.printStackTrace();
				break;
				/*if (error++ > 1000) {
                    break;
                }*/
				// todo should not write directly
			} finally {
				stream.closeFrame();
			}
		}
		if (error > 0) {
			System.out.println("errors: " + error);
		}
		in.close();
		if (line != null) {
			line.drain();
			/*while(line.isActive()&&!stop) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {e.printStackTrace();}
			}*/
			/*if(shouldFade)
				fadeOut();*/
			line.stop();
			line.close();
			line = null;
		}
	}

	public void stop(final boolean shouldFade) {
		new Thread() {
			public void run() {
		if(shouldFade)
			fadeOut();
		Decoder.this.stop = true;
			}
		}.start();
	}

	public boolean pause() {
		this.pause = !pause;
		return pause;
	}

	public void fadeIn() {
		if(line==null)
			return;
		new Thread() {
			public void run() {
				//try{sleep(200);}catch(InterruptedException e){e.printStackTrace();}
				FloatControl volume=(FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
				for(float i=-15; i<=0; i+=.15) {
					volume.setValue(i);
					try{sleep(2);}catch(InterruptedException e){e.printStackTrace();}
				}
			}
		}.start();
	}

	public void fadeOut() {
		if(line==null)
			return;
		//new Thread() {
			//public void run() {
				FloatControl volume=(FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
				for(float i=(float)-1.5; i>-15; i-=.15) {
					volume.setValue(i);
					try{Thread.sleep(2);}catch(InterruptedException e){e.printStackTrace();}
				}
			//}
		//}.start();
	}

}
