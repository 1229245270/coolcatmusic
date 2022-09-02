package com.hzc.coolCatMusic.utils;

import static com.hzc.coolCatMusic.utils.TagUtils.LocalUtilsTAG;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.provider.MediaStore;

import com.hzc.coolCatMusic.entity.LocalSongEntity;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.goldze.mvvmhabit.utils.KLog;

public class LocalUtils {

    public static List<LocalSongEntity> getAllMediaList(Context context, int minDuration,double minSize,int... searchSize) {
        Cursor cursor = null;
        List<LocalSongEntity> mediaList = new ArrayList<LocalSongEntity>();
        try {
            cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,
                    null, null, MediaStore.Audio.Media.DATE_ADDED + " DESC");
            if(cursor == null) {
                KLog.d(LocalUtilsTAG, "The getMediaList cursor is null.");
                return mediaList;
            }
            int count= cursor.getCount();
            if(count <= 0) {
                KLog.d(LocalUtilsTAG, "The getMediaList cursor count is 0.");
                return mediaList;
            }
            mediaList = new ArrayList<LocalSongEntity>();
            LocalSongEntity mediaEntity = null;
            while (cursor.moveToNext()) {
                int duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                if(artist.equals("<unknown>") || duration <= minDuration * 1000 || size <= minSize * 1024L * 1024L) {
                    continue;
                }
                mediaEntity = new LocalSongEntity();
                mediaEntity.setId(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                mediaEntity.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                mediaEntity.setDisplay_name(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)));
                mediaEntity.setDuration(duration);
                mediaEntity.setSize(size);
                mediaEntity.setArtist(artist);
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                mediaEntity.setPath(data);
                mediaEntity.setAlbums(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));

                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(data);

                byte[] cover = mediaMetadataRetriever.getEmbeddedPicture();
                if(cover != null){
                    Bitmap bitmap = BitmapFactory.decodeByteArray(cover,0,cover.length);
                    mediaEntity.setImage(bitmap);
                }
                mediaList.add(mediaEntity);
                if(searchSize != null && searchSize.length >= 1 && mediaList.size() >= searchSize[0]){
                    return mediaList;
                }
            }
            String destFileDir = context.getCacheDir().getPath();
            File destFile = new File(destFileDir);
            File[] files = destFile.listFiles();
            if(files != null){
                for(File file : files){
                    if(file.isFile()){
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        String str = file.getPath();
                        mmr.setDataSource(str);
                        String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION); // 播放时长单位为毫秒
                        byte[] pic = mmr.getEmbeddedPicture();  // 图片，可以通过BitmapFactory.decodeByteArray转换为bitmap图片

                        mediaEntity = new LocalSongEntity();
                        String fileName = file.getName();
                        mediaEntity.setDisplay_name(fileName);
                        mediaEntity.setDuration(Integer.parseInt(duration));
                        mediaEntity.setPath(str);
                        if(album == null || artist == null){
                            try {
                                album = fileName.substring(fileName.indexOf(" - ") + 3,fileName.lastIndexOf(".mp3"));
                                artist = fileName.substring(0,fileName.indexOf(" - "));
                            }catch (Exception e){
                                KLog.e(e.toString());
                            }
                        }
                        mediaEntity.setArtist(artist);
                        mediaEntity.setAlbums(album);
                        mediaEntity.setTitle(title);
                        if(pic != null){
                            Bitmap bitmap = BitmapFactory.decodeByteArray(pic,0,pic.length);
                            mediaEntity.setImage(bitmap);
                        }
                        mediaList.add(mediaEntity);

                        if(searchSize != null && searchSize.length >= 1 && mediaList.size() >= searchSize[0]){
                            return mediaList;
                        }
                    }
                }
            }
        } catch (Exception e) {
            KLog.d(LocalUtilsTAG,e.toString());
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return mediaList;
    }

}
