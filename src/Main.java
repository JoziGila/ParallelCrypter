import java.io.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;
public class Main {
    public static void main(String[] args) {
        String filename = "C:/Users/Jozi/Desktop/testfile.bin";
        byte[] data = "12345678901234567801".getBytes();
        System.out.println("Bytes total:" + data.length);
        createBinary(filename, data);

        Chunk[] readBuffer = new Chunk[4]; // 8 bytes in total
        for (int i = 0; i < readBuffer.length; i++){
            readBuffer[i] = new Chunk();
        }

        try {
            new ReaderThread(filename, readBuffer).start();
            new ConsumerThread(readBuffer).start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void createBinary(String filename, byte[] data){
        try {
            OutputStream outputStream = new FileOutputStream(filename);
            for (int i = 0; i < data.length; i++) {
                outputStream.write(data[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ReaderThread extends Thread {
    Chunk[] readBuffer;
    BufferedInputStream in;

    public ReaderThread(String filename, Chunk[] readBuffer) throws FileNotFoundException {
        this.in = new BufferedInputStream(new FileInputStream(filename), 256);
        this.readBuffer = readBuffer;
        System.out.println(readBuffer[0].position);
    }

    @Override
    public void run() {
        int nextChunk = 0; // The chunk to be processed
        int totalBytes = 0; // Acts as a pointer to the last position

        int readBytes = 0; // Current bytes read
        while (readBytes != -1) {
            try {
                Chunk chunk = readBuffer[nextChunk];
                synchronized (chunk) {
                    // Write data to buffer chunk which is apparently locked now
                    readBytes = in.read(readBuffer[nextChunk].data);

                    // Its now ready to either stop the whole system or transfer some data
                    chunk.ready = true;

                    // IF READ BYTES IS -1 MARK CHUNK CURSED AND EXIT else normal business as always
                    if (readBytes == -1) {
                        chunk.position =  -1;
                    }

                    // The normal business
                    chunk.position =  totalBytes;
                    totalBytes += readBytes;
                }

                // Find next chunk to write and ready up
                // Can be refactored by not using int but straight up the object
                boolean isFound = false;
                while (!isFound) {
                    OptionalInt o = IntStream.range(0, readBuffer.length)
                            .filter(i -> (!readBuffer[i].ready))
                            .findFirst();
                    if (isFound = o.isPresent()) {
                        nextChunk = o.getAsInt();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class ConsumerThread extends Thread {
    Chunk[] readBuffer;
    public ConsumerThread(Chunk[] readBuffer){
        this.readBuffer = readBuffer;
    }

    @Override
    public void run() {
        while(true){
            Optional o = Arrays.stream(readBuffer).filter(x -> x.ready || x.position != -1).findAny();
            Chunk chunk;
            if (o.isPresent()){
                chunk = (Chunk)o.get();
                synchronized (chunk){
                    chunk.ready = false;
                }
            } else {
                o = Arrays.stream(readBuffer).filter(x -> x.position == -1).findAny();
                if (o.isPresent()){
                    return;
                }
            }
        }
    }
}

class Chunk {
    int position = 0;
    boolean ready = false;
    byte[] data = new byte[2];
}
