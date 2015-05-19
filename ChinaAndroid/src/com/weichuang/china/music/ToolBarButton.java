/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.weichuang.china.music;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;

import com.android.china.R;

/**
 * <p>
 * <code>ToolBarButton</code> represents a toolbar-button widget. toolbar-buttons can be
 * pressed, or clicked, by the user to perform an action. A typical use of a
 * toolbar-button in an activity would be the following:
 * </p>
 *
 * <pre class="prettyprint">
 * public class MyActivity extends Activity {
 *     protected void onCreate(Bundle icicle) {
 *         super.onCreate(icicle);
 *
 *         setContentView(R.layout.content_layout_id);
 *
 *         final ToolBarButton tbbutton = (Button) findViewById(R.id.button_id);
 *         button.setOnClickListener(new View.OnClickListener() {
 *             public void onClick(View v) {
 *                 // Perform action on click
 *             }
 *         });
 *     }
 * }
 * </pre>
 *
 * xml use as following:
 *
 * 	    <ToolBarButton 
 *	        android:drawableTop= "@drawable/star02"
 *	        android:text="Button06" 
 *	        android:id="@+id/Button06" 
 *	        android:layout_width="96dip" 
 *	        android:layout_height="70dip"
 *	        style="@style/toolbar_button_style">
 *	    </ToolBarButton>
 *
 * <p><strong>XML attributes</strong></p>
 * <p>
 * See {@link android.R.styleable#Button Button Attributes},
 * {@link android.R.styleable#TextView TextView Attributes},
 * {@link android.R.styleable#View View Attributes}
 * </p>
 */


public class ToolBarButton extends Button {

	public ToolBarButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public ToolBarButton(Context context, AttributeSet attrs) {
		this(context, attrs, R.style.ToolBarButtonStyle);
		// TODO Auto-generated constructor stub
	}

	public ToolBarButton(Context context) {
		this(context,null);
		// TODO Auto-generated constructor stub
	}
	
    @Override
    public void onDraw(Canvas canvas) {

        final Drawable background = getBackground();
        if (background != null) {


            final Bitmap b = ((BitmapDrawable)(background.getCurrent())).getBitmap();

            Rect src;
            Rect dst;
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            int w = getWidth();
            int h = getHeight();
            
            int bw = b.getWidth();
            int bh = b.getHeight();
            
            if(bw > w && bh > h){
                src = new Rect((bw-w)/2,(bh-h)/2,(bw+w)/2,(bh+h)/2);
            }else if(bw > w && bh <= h){
            	src = new Rect((bw-w)/2,0,(bw+w)/2,bh);
            }else{
            	src = new Rect(0,0,bw,bh);
            }
            
            dst = new Rect(0,0,w,h);
            if ((scrollX | scrollY) == 0) {
	        canvas.drawBitmap(b, src, dst, null);
	    }else{
		canvas.translate(scrollX, scrollY);
		canvas.drawBitmap(b, src, dst, null);
		canvas.translate(-scrollX, -scrollY);
	    }
        }
        
        super.onDraw(canvas);
    }
	
}
