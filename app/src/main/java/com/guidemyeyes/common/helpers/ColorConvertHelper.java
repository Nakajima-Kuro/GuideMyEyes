package com.guidemyeyes.common.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.Image;

import androidx.renderscript.Allocation;
import androidx.renderscript.Element;
import androidx.renderscript.RenderScript;
import androidx.renderscript.ScriptIntrinsicYuvToRGB;
import androidx.renderscript.Type;

import java.nio.ByteBuffer;

public class ColorConvertHelper {

    private final Context context;

    public ColorConvertHelper(Context context) {
        this.context = context;
    }

    public Bitmap YUV_420_888_toRGBIntrinsics(Image image) {
        final RenderScript rs = RenderScript.create(this.context);
        //Image to YUV
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer bufferY = planes[0].getBuffer();
        ByteBuffer bufferU = planes[1].getBuffer();
        ByteBuffer bufferV = planes[2].getBuffer();
        int lengthY = bufferY.remaining();
        int lengthU = bufferU.remaining();
        int lengthV = bufferV.remaining();
        byte[] dataYUV = new byte[lengthY + lengthU + lengthV];
        bufferY.get(dataYUV, 0, lengthY);
        bufferU.get(dataYUV, lengthY, lengthU);
        bufferV.get(dataYUV, lengthY + lengthU, lengthV);

        //YUV to RGB
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(dataYUV.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);


        Bitmap bmpOut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        in.copyFromUnchecked(dataYUV);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        out.copyTo(bmpOut);

        //Invert Red and Blue channel
        float[] colorTransform = {
                0, 0, 1f, 0, 0,
                0, 1f, 0, 0, 0,
                1f, 0, 0, 0, 0,
                0, 0, 0, 1f, 0};
        Canvas canvas = new Canvas(bmpOut);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(colorTransform);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bmpOut, 0, 0, paint);
        return bmpOut;
    }
}
