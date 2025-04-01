# SparkPaint File Format Specification

## Overview

The SparkPaint file format (`.spp`) is a binary format used to store layered image data. It consists of three main parts:
1. A header for format identification
2. The serialized data payload
3. A CRC32 checksum for data integrity validation

## File Structure

| Section | Size (bytes) | Description |
|---------|--------------|-------------|
| Magic Number | 4 | "GR8A" in ASCII |
| Version | 4 | Integer version (currently 1) |
| Data Length | 4 | Length of the serialized data in bytes |
| Serialized Data | Variable | Java-serialized data containing compressed layers |
| CRC32 Checksum | 8 | 64-bit CRC32 checksum of the serialized data |

## Detailed Specification

### Header
- **Magic Number**: 4 bytes containing ASCII "GR8A" to identify the file format
- **Version**: 4-byte integer (current version is 1)
- **Data Length**: 4-byte integer indicating the length of the serialized data in bytes

### Serialized Data
The serialized data consists of a Java-serialized `CompressedLayerState` object containing:
- A list of `CompressedLayer` objects
- The index of the currently active layer

Each `CompressedLayer` contains:
- Compressed image data (DEFLATE compression of ARGB pixel data)
- Image width (pixels)
- Image height (pixels)
- Visibility flag (boolean)
- Layer name (String)

### CRC32 Checksum
An 8-byte (long) CRC32 checksum calculated from the serialized data section to verify data integrity.

## Compression Method

Image data is compressed using the DEFLATE algorithm:
1. Original ARGB image data (4 bytes per pixel) is serialized
2. Data is compressed using DEFLATE (Java's Deflater)
3. Decompression is performed using Java's Inflater when loading

## Import/Export Implementation Guide

To implement SparkPaint file format support:

### Reading Files
1. Read and verify the 4-byte magic number "GR8A"
2. Read the 4-byte version number and ensure compatibility
3. Read the 4-byte data length
4. Read the serialized data (byte array of specified length)
5. Read the 8-byte CRC32 checksum
6. Calculate CRC32 of the data and verify against the stored checksum
7. Deserialize the data into layer information and images

### Writing Files
1. Write the 4-byte magic number "GR8A"
2. Write the 4-byte version number (1)
3. Compress and serialize all layer data
4. Write the 4-byte length of the serialized data
5. Write the serialized data
6. Calculate and write the 8-byte CRC32 checksum of the serialized data

## Error Handling

The format requires validation of:
- Magic number for format identification
- Version compatibility
- CRC32 checksum for data integrity

An IOException should be thrown if any validation fails.