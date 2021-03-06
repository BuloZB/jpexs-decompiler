/*
 *  Copyright (C) 2010-2015 JPEXS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.jpexs.decompiler.flash.helpers;

import com.jpexs.decompiler.flash.tags.base.ImageTag;
import com.jpexs.decompiler.flash.tags.enums.ImageFormat;
import com.jpexs.helpers.Helper;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.monte.media.jpeg.CMYKJPEGImageReader;
import org.monte.media.jpeg.CMYKJPEGImageReaderSpi;

/**
 *
 * @author JPEXS
 */
public class ImageHelper {

    public static BufferedImage read(byte[] data) throws IOException {
        return read(new ByteArrayInputStream(data));
    }

    public static BufferedImage read(InputStream input) throws IOException {
        BufferedImage in;
        byte data[] = Helper.readStream(input);
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            CMYKJPEGImageReader r = new CMYKJPEGImageReader(new CMYKJPEGImageReaderSpi());
            r.setInput(iis);
            in = r.read(0);
        } catch (IOException ex) {
            try {
                in = ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(data)));
            } catch (IOException ex1) {
                return null;
            }
        }

        int type = in.getType();
        if (type != BufferedImage.TYPE_INT_ARGB && type != BufferedImage.TYPE_INT_RGB) {
            // convert to ARGB
            int width = in.getWidth();
            int height = in.getHeight();
            int[] imgData = in.getRGB(0, 0, width, height, null, 0, width);
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            newImage.getRaster().setDataElements(0, 0, width, height, imgData);
            return newImage;
        }

        return in;
    }

    public static void write(BufferedImage image, ImageFormat format, File output) throws IOException {
        String formatName = getImageFormatString(format).toUpperCase(Locale.ENGLISH);
        if (format == ImageFormat.JPEG) {
            image = fixImageIOJpegBug(image);
        }

        ImageIO.write(image, formatName, output);
    }

    public static void write(BufferedImage image, ImageFormat format, OutputStream output) throws IOException {
        String formatName = getImageFormatString(format).toUpperCase(Locale.ENGLISH);
        if (format == ImageFormat.JPEG) {
            image = fixImageIOJpegBug(image);
        }

        ImageIO.write(image, formatName, output);
    }

    public static void write(BufferedImage image, ImageFormat format, ByteArrayOutputStream output) {
        String formatName = getImageFormatString(format).toUpperCase(Locale.ENGLISH);
        if (format == ImageFormat.JPEG) {
            image = fixImageIOJpegBug(image);
        }

        try {
            ImageIO.write(image, formatName, output);
        } catch (IOException ex) {
            Logger.getLogger(ImageHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static BufferedImage fixImageIOJpegBug(BufferedImage image) {
        int type = image.getType();
        if (type != BufferedImage.TYPE_INT_RGB) {
            // convert to RGB without alpha channel
            int width = image.getWidth();
            int height = image.getHeight();
            int[] imgData = image.getRGB(0, 0, width, height, null, 0, width);
            ImageTag.divideAlpha(imgData);
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            newImage.getRaster().setDataElements(0, 0, width, height, imgData);
            return newImage;
        }

        return image;
    }

    public static String getImageFormatString(ImageFormat format) {
        switch (format) {
            case UNKNOWN:
                return "unk";
            case JPEG:
                return "jpg";
            case GIF:
                return "gif";
            case PNG:
                return "png";
            case BMP:
                return "bmp";
        }

        throw new Error("Unsuported image format: " + format);
    }

    public static Dimension getDimesion(InputStream input) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(input)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException ex) {
        }

        BufferedImage image = read(input);
        return new Dimension(image.getWidth(), image.getHeight());
    }
}
