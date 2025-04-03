package com.esgdev.sparkpaint.io;

import com.esgdev.sparkpaint.engine.history.CompressedLayer;
import com.esgdev.sparkpaint.engine.history.CompressedLayerState;
import com.esgdev.sparkpaint.engine.history.LayerState;
import com.esgdev.sparkpaint.engine.layer.Layer;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/**
 * SparkPaintFileFormat is responsible for saving and loading SparkPaint files.
 * It uses a custom binary format with a header, payload, and CRC32 checksum.
 */
public class SparkPaintFileFormat {
    private static final byte[] MAGIC = "GR8A".getBytes(); // SparkPaint Image Format
    private static final int VERSION = 1;

    public static void saveToFile(File file, List<Layer> layers, int currentLayerIndex) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            // Write header
            dos.write(MAGIC);
            dos.writeInt(VERSION);

            // Serialize layers to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                List<CompressedLayer> compressedLayers = layers.stream()
                    .map(CompressedLayer::fromLayer)
                    .collect(Collectors.toList());
                oos.writeObject(new CompressedLayerState(compressedLayers, currentLayerIndex));
            }

            byte[] layerData = baos.toByteArray();

            // Write payload length
            dos.writeInt(layerData.length);

            // Write layer data
            dos.write(layerData);

            // Calculate and write CRC32
            CRC32 crc = new CRC32();
            crc.update(layerData);
            dos.writeLong(crc.getValue());
        }
    }

    public static LayerState loadFromFile(File file) throws IOException, ClassNotFoundException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())))) {
            // Verify magic number
            byte[] magicBuffer = new byte[4];
            dis.readFully(magicBuffer);
            if (!Arrays.equals(magicBuffer, MAGIC)) {
                throw new IOException("Invalid file format");
            }

            // Check version
            int version = dis.readInt();
            if (version > VERSION) {
                throw new IOException("Unsupported file version");
            }

            // Read payload length
            int dataLength = dis.readInt();

            // Read layer data
            byte[] layerData = new byte[dataLength];
            dis.readFully(layerData);

            // Verify CRC32
            long storedCrc = dis.readLong();
            CRC32 crc = new CRC32();
            crc.update(layerData);
            if (crc.getValue() != storedCrc) {
                throw new IOException("File data corrupted");
            }

            // Deserialize layer data
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(layerData))) {
                CompressedLayerState compressedState = (CompressedLayerState) ois.readObject();
                return compressedState.toLayerState();
            }
        }
    }
}