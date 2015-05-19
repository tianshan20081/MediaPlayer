package com.weichuang.china.music.coverflow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLUtils;

import com.weichuang.china.music.MusicUtils;

public class Cover {
	public static final int INVALID_TEXTURE = -1;
	public static final int STATE_UNLOADED = 0;
	public static final int STATE_QUEUED = 1;
	public static final int STATE_LOADING = 2;
	public static final int STATE_LOADED = 3;
	public static final int STATE_ERROR = 4;

	int mState = 0;
	int mTextureId = 0;
	Bitmap mBitmap = null;

	private long mAlbumeId = -1L;
	private Context mContext;

	public String artistName;
	public String albumName;

	public StringTexture artistTexture;
	public StringTexture albumTexture;

	public Cover(Context context, long album_id) {
		mAlbumeId = album_id;
		mContext = context;
	}

	public void setAlbumId(long albumeId) {
		mAlbumeId = albumeId;
	}

	public long getAlbumId() {
		return mAlbumeId;
	}

	public int getCoverTexture(GL10 gl) {
		int texture = mTextureId;
		if (texture != 0) {
			return texture;
		} else {
			genCoverTexture(gl);
			return mTextureId;
		}
	}

	public void genCoverTexture(GL10 gl) {

		if (mBitmap != null) {
			Bitmap bitmap = mBitmap;
			GL11 newGL = (GL11) gl;
			int textureId[] = new int[1];

			gl.glGenTextures(1, textureId, 0);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId[0]);

			/*
			 * for test
			 */
			int[] cropRect1 = { 0, 256, 256, -256 };
			newGL.glTexParameteriv(GL11.GL_TEXTURE_2D,
					GL11Ext.GL_TEXTURE_CROP_RECT_OES, cropRect1, 0);

			newGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
					GL11.GL_CLAMP_TO_EDGE);
			newGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
					GL11.GL_CLAMP_TO_EDGE);
			newGL.glTexParameterf(GL11.GL_TEXTURE_2D,
					GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			newGL.glTexParameterf(GL11.GL_TEXTURE_2D,
					GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

			mTextureId = textureId[0];
			// mBitmap = null;
			// bitmap.recycle();
		} else {
			if (mAlbumeId != -1) {
				genCoverBitmap(mAlbumeId);
				if (mBitmap != null) {
					genCoverTexture(gl);
				} else {
					mTextureId = INVALID_TEXTURE;
				}
			}
		}
	}

	public Bitmap genCoverBitmap(long album_id) {
		Bitmap bitmap = MusicUtils
				.getArtworkQuick(mContext, album_id, 257, 256);
		mBitmap = bitmap;
		return bitmap;

	}

	public Bitmap loadCoverBitmap() {
		if (mAlbumeId != -1) {
			if (mBitmap == null || mBitmap.isRecycled() == true) {
				Bitmap bitmap = MusicUtils.getArtworkQuick(mContext, mAlbumeId,
						257, 256);
				mBitmap = bitmap;
				return bitmap;
			} else {
				return mBitmap;
			}
		} else {
			return null;
		}
	}

	public void setAlbumInfo(String artistname, String albumname) {
		artistName = artistname;
		albumName = albumname;
	}

	public void bind(GL10 gl) {
		if (mTextureId != 0) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId);
		} else {
			if (mBitmap != null) {
				Bitmap bitmap = mBitmap;
				GL11 newGL = (GL11) gl;
				int textureId[] = new int[1];

				gl.glGenTextures(1, textureId, 0);
				gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId[0]);

				/*
				 * for test
				 */
				// int[] cropRect1 = { 0, 256, 256, -256 };
				// newGL.glTexParameteriv(GL11.GL_TEXTURE_2D,
				// GL11Ext.GL_TEXTURE_CROP_RECT_OES, cropRect1, 0);

				newGL.glTexParameteri(GL11.GL_TEXTURE_2D,
						GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE);
				newGL.glTexParameteri(GL11.GL_TEXTURE_2D,
						GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE);
				newGL.glTexParameterf(GL11.GL_TEXTURE_2D,
						GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				newGL.glTexParameterf(GL11.GL_TEXTURE_2D,
						GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

				mTextureId = textureId[0];
				mBitmap = null;
				bitmap.recycle();
			}
		}
	}

	public FloatBuffer mArtistVertex;
	public FloatBuffer mArtistTex;
	public FloatBuffer mAlbumVertex;
	public FloatBuffer mAlbumTex;
	private static final int VERTS = 4;
	public boolean isTitlteQuadInitialed = false;

	public void initTitleQuad() {
		if(!isTitlteQuadInitialed){
		ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mArtistVertex = vbb.asFloatBuffer();

		ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mArtistTex = tbb.asFloatBuffer();

		vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mAlbumVertex = vbb.asFloatBuffer();

		tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mAlbumTex = tbb.asFloatBuffer();
		
		isTitlteQuadInitialed= true;
		}else{
			mArtistVertex.clear();
			mArtistTex.clear();
			mAlbumVertex.clear();
			mAlbumTex.clear();
		}

	}

	public static void setTitleQuad(StringTexture texture, FloatBuffer vertex,
			FloatBuffer tex, float glToPix) {
		float edgeHor = texture.getWidth() / (2 * glToPix);
		float edgeVer = texture.getHeight() / (2 * glToPix);		
		float u = texture.getNormalizedWidth();
		float v = texture.getNormalizedHeight();
		vertex.clear();
		tex.clear();
		float[] coords = {
				// X, Y, Z
				edgeHor, edgeVer, 0, -edgeHor, edgeVer, 0, edgeHor, -edgeVer,
				0, -edgeHor, -edgeVer, 0 };

		float[] texcoords = { u, 0, 0, 0, u, v, 0, v };
		vertex.put(coords);
		tex.put(texcoords);

		vertex.position(0);
		tex.position(0);
	}
}
