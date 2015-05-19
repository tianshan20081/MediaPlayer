package com.weichuang.china.util;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class MyImageView extends View {
	private Drawable drawable;
	private Paint paint;
	public MyImageView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		 paint = new Paint(); 
		paint.setAntiAlias(true);
		paint.setShadowLayer(5f, 5.0f, 5.0f, Color.BLACK);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	}

	public MyImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		paint = new Paint(); 
		paint.setAntiAlias(true);
		paint.setShadowLayer(5f, 5.0f, 5.0f, Color.BLACK);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	}
	public MyImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		paint = new Paint(); 
		paint.setAntiAlias(true);
		paint.setShadowLayer(5f, 5.0f, 5.0f, Color.BLACK);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	}

	@Override
	public void draw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.draw(canvas);
		int PicWidth, PicHeight;
		if(drawable!=null){
			
			Drawable dbe = drawable.mutate();
//			 Bitmap bnp = BitmapFactory.decodeResource(getResources(), R.drawable.touxiang);  
			BitmapDrawable bd = (BitmapDrawable)drawable;
			Bitmap bnp = bd.getBitmap();
			int srcWidth = bnp.getWidth();   
            int srcHeight = bnp.getHeight();   
            ////////////////////////////////////////////
            float scaleWidth = ((float) 90) / srcWidth;   
            float scaleHeight = ((float)90) / srcHeight;   
               
            
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);    
            Bitmap bmp = Bitmap.createBitmap(bnp, 0, 0, srcWidth,   
            		srcWidth, matrix, true);
			PicWidth = bmp.getWidth();
			PicHeight = bmp.getHeight();
			
//			canvas.drawColor(Color.parseColor("#ebebeb"));
			canvas.drawColor(Color.parseColor("#00000000"));
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			dbe.setColorFilter(0x7f000000, PorterDuff.Mode.SRC_IN);
			canvas.translate(0 ,
					0);
			Rect rect = new Rect(  1, 2,  PicWidth - 3, PicHeight
					- 3);
			RectF rectF = new RectF(rect);
			canvas.drawRoundRect(rectF, 10f, 10f, paint);
			canvas.drawBitmap(bmp,  1,  1,
					null);
			canvas.restore();
		}
	}

	public void setImageDrawable(Drawable drawable) {
		// TODO Auto-generated method stub
		this.drawable = drawable;
		invalidate();
	}
	

}
