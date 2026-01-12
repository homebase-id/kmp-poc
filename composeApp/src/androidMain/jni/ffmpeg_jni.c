/*
 * FFmpeg JNI Wrapper for Android
 * Provides native FFmpeg operations: thumbnail, rotation, compression, HLS segmentation
 */
#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/avutil.h"
#include "libavutil/dict.h"
#include "libavutil/opt.h"
#include "libavutil/imgutils.h"
#include "libavutil/timestamp.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"

#define LOG_TAG "FFmpegJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define RESULT_SUCCESS 0
#define RESULT_ERROR -1

// ============================================================================
// INITIALIZATION
// ============================================================================

JNIEXPORT jint JNICALL
Java_id_homebase_homebasekmppoc_media_FFmpegNative_nativeInit(JNIEnv *env, jclass clazz) {
    LOGI("FFmpeg version: %s", av_version_info());
    LOGI("libavcodec version: %d.%d.%d", 
         LIBAVCODEC_VERSION_MAJOR, 
         LIBAVCODEC_VERSION_MINOR, 
         LIBAVCODEC_VERSION_MICRO);
    return RESULT_SUCCESS;
}

JNIEXPORT jstring JNICALL
Java_id_homebase_homebasekmppoc_media_FFmpegNative_getVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, av_version_info());
}

// ============================================================================
// MEDIA INFO FUNCTIONS
// ============================================================================

JNIEXPORT jlong JNICALL
Java_id_homebase_homebasekmppoc_media_FFmpegNative_getMediaDuration(JNIEnv *env, jclass clazz, jstring path) {
    const char *input_path = (*env)->GetStringUTFChars(env, path, NULL);
    if (!input_path) return RESULT_ERROR;
    
    AVFormatContext *fmt_ctx = NULL;
    if (avformat_open_input(&fmt_ctx, input_path, NULL, NULL) < 0) {
        LOGE("Could not open input: %s", input_path);
        (*env)->ReleaseStringUTFChars(env, path, input_path);
        return RESULT_ERROR;
    }
    
    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, path, input_path);
        return RESULT_ERROR;
    }
    
    jlong duration = fmt_ctx->duration;
    avformat_close_input(&fmt_ctx);
    (*env)->ReleaseStringUTFChars(env, path, input_path);
    return duration;
}

JNIEXPORT jint JNICALL
Java_id_homebase_homebasekmppoc_media_FFmpegNative_getVideoRotation(JNIEnv *env, jclass clazz, jstring path) {
    const char *input_path = (*env)->GetStringUTFChars(env, path, NULL);
    if (!input_path) return 0;
    
    AVFormatContext *fmt_ctx = NULL;
    if (avformat_open_input(&fmt_ctx, input_path, NULL, NULL) < 0) {
        (*env)->ReleaseStringUTFChars(env, path, input_path);
        return 0;
    }
    
    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, path, input_path);
        return 0;
    }
    
    int rotation = 0;
    for (unsigned int i = 0; i < fmt_ctx->nb_streams; i++) {
        if (fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            AVDictionaryEntry *tag = av_dict_get(fmt_ctx->streams[i]->metadata, "rotate", NULL, 0);
            if (tag) {
                rotation = atoi(tag->value);
            }
            break;
        }
    }
    
    avformat_close_input(&fmt_ctx);
    (*env)->ReleaseStringUTFChars(env, path, input_path);
    return rotation;
}

JNIEXPORT jintArray JNICALL
Java_id_homebase_homebasekmppoc_media_FFmpegNative_getVideoDimensions(JNIEnv *env, jclass clazz, jstring path) {
    const char *input_path = (*env)->GetStringUTFChars(env, path, NULL);
    if (!input_path) return NULL;
    
    AVFormatContext *fmt_ctx = NULL;
    if (avformat_open_input(&fmt_ctx, input_path, NULL, NULL) < 0) {
        (*env)->ReleaseStringUTFChars(env, path, input_path);
        return NULL;
    }
    
    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, path, input_path);
        return NULL;
    }
    
    int width = 0, height = 0;
    for (unsigned int i = 0; i < fmt_ctx->nb_streams; i++) {
        if (fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            width = fmt_ctx->streams[i]->codecpar->width;
            height = fmt_ctx->streams[i]->codecpar->height;
            break;
        }
    }
    
    avformat_close_input(&fmt_ctx);
    (*env)->ReleaseStringUTFChars(env, path, input_path);
    
    if (width == 0 || height == 0) return NULL;
    
    jintArray result = (*env)->NewIntArray(env, 2);
    if (result) {
        jint dims[2] = {width, height};
        (*env)->SetIntArrayRegion(env, result, 0, 2, dims);
    }
    return result;
}

// ============================================================================
// THUMBNAIL EXTRACTION
// ============================================================================

JNIEXPORT jint JNICALL
Java_id_homebase_homebasekmppoc_media_FFmpegNative_extractThumbnail(
    JNIEnv *env, jclass clazz, 
    jstring inputPath, jstring outputPath, jdouble timeSeconds) {
    
    const char *input = (*env)->GetStringUTFChars(env, inputPath, NULL);
    const char *output = (*env)->GetStringUTFChars(env, outputPath, NULL);
    
    if (!input || !output) {
        if (input) (*env)->ReleaseStringUTFChars(env, inputPath, input);
        if (output) (*env)->ReleaseStringUTFChars(env, outputPath, output);
        return RESULT_ERROR;
    }
    
    int result = RESULT_ERROR;
    AVFormatContext *ifmt_ctx = NULL;
    AVFormatContext *ofmt_ctx = NULL;
    AVCodecContext *dec_ctx = NULL;
    AVCodecContext *enc_ctx = NULL;
    struct SwsContext *sws_ctx = NULL;
    AVFrame *frame = NULL;
    AVFrame *rgb_frame = NULL;
    AVPacket *pkt = NULL;
    
    // Open input
    if (avformat_open_input(&ifmt_ctx, input, NULL, NULL) < 0) {
        LOGE("Could not open input: %s", input);
        goto cleanup;
    }
    
    if (avformat_find_stream_info(ifmt_ctx, NULL) < 0) {
        LOGE("Could not find stream info");
        goto cleanup;
    }
    
    // Find video stream
    int video_stream = -1;
    for (unsigned int i = 0; i < ifmt_ctx->nb_streams; i++) {
        if (ifmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream = i;
            break;
        }
    }
    
    if (video_stream < 0) {
        LOGE("No video stream found");
        goto cleanup;
    }
    
    // Setup decoder
    const AVCodec *decoder = avcodec_find_decoder(ifmt_ctx->streams[video_stream]->codecpar->codec_id);
    if (!decoder) {
        LOGE("Decoder not found");
        goto cleanup;
    }
    
    dec_ctx = avcodec_alloc_context3(decoder);
    if (!dec_ctx) {
        LOGE("Could not allocate decoder context");
        goto cleanup;
    }
    
    if (avcodec_parameters_to_context(dec_ctx, ifmt_ctx->streams[video_stream]->codecpar) < 0) {
        LOGE("Could not copy codec params");
        goto cleanup;
    }
    
    if (avcodec_open2(dec_ctx, decoder, NULL) < 0) {
        LOGE("Could not open decoder");
        goto cleanup;
    }
    
    // Setup MJPEG encoder for output
    const AVCodec *encoder = avcodec_find_encoder(AV_CODEC_ID_MJPEG);
    if (!encoder) {
        LOGE("MJPEG encoder not found");
        goto cleanup;
    }
    
    enc_ctx = avcodec_alloc_context3(encoder);
    if (!enc_ctx) {
        LOGE("Could not allocate encoder context");
        goto cleanup;
    }
    
    enc_ctx->width = dec_ctx->width;
    enc_ctx->height = dec_ctx->height;
    enc_ctx->pix_fmt = AV_PIX_FMT_YUVJ420P;
    enc_ctx->time_base = (AVRational){1, 25};
    enc_ctx->flags |= AV_CODEC_FLAG_QSCALE;
    enc_ctx->global_quality = 2 * FF_QP2LAMBDA; // High quality
    
    if (avcodec_open2(enc_ctx, encoder, NULL) < 0) {
        LOGE("Could not open encoder");
        goto cleanup;
    }
    
    // Seek to time
    int64_t timestamp = (int64_t)(timeSeconds * AV_TIME_BASE);
    av_seek_frame(ifmt_ctx, -1, timestamp, AVSEEK_FLAG_BACKWARD);
    
    frame = av_frame_alloc();
    rgb_frame = av_frame_alloc();
    pkt = av_packet_alloc();
    if (!frame || !rgb_frame || !pkt) {
        LOGE("Could not allocate frame/packet");
        goto cleanup;
    }
    
    // Setup scaler for pixel format conversion
    sws_ctx = sws_getContext(
        dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
        dec_ctx->width, dec_ctx->height, AV_PIX_FMT_YUVJ420P,
        SWS_BILINEAR, NULL, NULL, NULL);
    
    if (!sws_ctx) {
        LOGE("Could not create scaler");
        goto cleanup;
    }
    
    rgb_frame->format = AV_PIX_FMT_YUVJ420P;
    rgb_frame->width = dec_ctx->width;
    rgb_frame->height = dec_ctx->height;
    av_frame_get_buffer(rgb_frame, 0);
    
    // Read and decode one frame
    int got_frame = 0;
    while (!got_frame && av_read_frame(ifmt_ctx, pkt) >= 0) {
        if (pkt->stream_index == video_stream) {
            if (avcodec_send_packet(dec_ctx, pkt) == 0) {
                if (avcodec_receive_frame(dec_ctx, frame) == 0) {
                    got_frame = 1;
                }
            }
        }
        av_packet_unref(pkt);
    }
    
    if (!got_frame) {
        LOGE("Could not decode frame");
        goto cleanup;
    }
    
    // Convert pixel format
    sws_scale(sws_ctx, (const uint8_t * const *)frame->data, frame->linesize,
              0, dec_ctx->height, rgb_frame->data, rgb_frame->linesize);
    
    rgb_frame->pts = 0;
    
    // Encode to JPEG
    if (avcodec_send_frame(enc_ctx, rgb_frame) < 0) {
        LOGE("Error sending frame for encoding");
        goto cleanup;
    }
    
    AVPacket *enc_pkt = av_packet_alloc();
    if (avcodec_receive_packet(enc_ctx, enc_pkt) == 0) {
        // Write JPEG to file
        FILE *f = fopen(output, "wb");
        if (f) {
            fwrite(enc_pkt->data, 1, enc_pkt->size, f);
            fclose(f);
            result = RESULT_SUCCESS;
            LOGI("Thumbnail saved to %s", output);
        }
    }
    av_packet_free(&enc_pkt);
    
cleanup:
    if (sws_ctx) sws_freeContext(sws_ctx);
    if (pkt) av_packet_free(&pkt);
    if (frame) av_frame_free(&frame);
    if (rgb_frame) av_frame_free(&rgb_frame);
    if (enc_ctx) avcodec_free_context(&enc_ctx);
    if (dec_ctx) avcodec_free_context(&dec_ctx);
    if (ifmt_ctx) avformat_close_input(&ifmt_ctx);
    
    (*env)->ReleaseStringUTFChars(env, inputPath, input);
    (*env)->ReleaseStringUTFChars(env, outputPath, output);
    
    return result;
}

// ============================================================================
// VIDEO COMPRESSION
// ============================================================================

JNIEXPORT jint JNICALL
Java_id_homebase_homebasekmppoc_media_FFmpegNative_compressVideo(
    JNIEnv *env, jclass clazz,
    jstring inputPath, jstring outputPath,
    jint targetBitrate, jint maxWidth) {
    
    const char *input = (*env)->GetStringUTFChars(env, inputPath, NULL);
    const char *output = (*env)->GetStringUTFChars(env, outputPath, NULL);
    
    if (!input || !output) {
        if (input) (*env)->ReleaseStringUTFChars(env, inputPath, input);
        if (output) (*env)->ReleaseStringUTFChars(env, outputPath, output);
        return RESULT_ERROR;
    }
    
    LOGI("Compressing video: %s -> %s (bitrate=%d, maxWidth=%d)", input, output, targetBitrate, maxWidth);
    
    int result = RESULT_ERROR;
    AVFormatContext *ifmt_ctx = NULL;
    AVFormatContext *ofmt_ctx = NULL;
    AVCodecContext *dec_ctx = NULL;
    AVCodecContext *enc_ctx = NULL;
    AVCodecContext *audio_dec_ctx = NULL;
    AVCodecContext *audio_enc_ctx = NULL;
    struct SwsContext *sws_ctx = NULL;
    struct SwrContext *swr_ctx = NULL;
    AVFrame *frame = NULL;
    AVFrame *scaled_frame = NULL;
    AVPacket *pkt = NULL;
    
    int video_stream_idx = -1;
    int audio_stream_idx = -1;
    int out_video_idx = -1;
    int out_audio_idx = -1;
    
    // Open input
    if (avformat_open_input(&ifmt_ctx, input, NULL, NULL) < 0) {
        LOGE("Could not open input");
        goto cleanup;
    }
    
    if (avformat_find_stream_info(ifmt_ctx, NULL) < 0) {
        LOGE("Could not find stream info");
        goto cleanup;
    }
    
    // Find video and audio streams
    for (unsigned int i = 0; i < ifmt_ctx->nb_streams; i++) {
        if (ifmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO && video_stream_idx < 0) {
            video_stream_idx = i;
        } else if (ifmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO && audio_stream_idx < 0) {
            audio_stream_idx = i;
        }
    }
    
    if (video_stream_idx < 0) {
        LOGE("No video stream found");
        goto cleanup;
    }
    
    // Setup video decoder
    AVCodecParameters *in_codecpar = ifmt_ctx->streams[video_stream_idx]->codecpar;
    const AVCodec *decoder = avcodec_find_decoder(in_codecpar->codec_id);
    if (!decoder) {
        LOGE("Decoder not found");
        goto cleanup;
    }
    
    dec_ctx = avcodec_alloc_context3(decoder);
    avcodec_parameters_to_context(dec_ctx, in_codecpar);
    if (avcodec_open2(dec_ctx, decoder, NULL) < 0) {
        LOGE("Could not open decoder");
        goto cleanup;
    }
    
    // Calculate output dimensions
    int out_width = dec_ctx->width;
    int out_height = dec_ctx->height;
    if (maxWidth > 0 && dec_ctx->width > maxWidth) {
        out_width = maxWidth;
        out_height = (int)((double)dec_ctx->height * maxWidth / dec_ctx->width);
        // Ensure even dimensions for H.264
        out_height = (out_height / 2) * 2;
    }
    
    // Setup output format
    if (avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, output) < 0) {
        LOGE("Could not create output context");
        goto cleanup;
    }
    
    // Setup video encoder (H.264)
    const AVCodec *encoder = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!encoder) {
        LOGE("H.264 encoder not found");
        goto cleanup;
    }
    
    AVStream *out_stream = avformat_new_stream(ofmt_ctx, NULL);
    if (!out_stream) {
        LOGE("Could not create output stream");
        goto cleanup;
    }
    out_video_idx = out_stream->index;
    
    enc_ctx = avcodec_alloc_context3(encoder);
    enc_ctx->width = out_width;
    enc_ctx->height = out_height;
    enc_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
    enc_ctx->time_base = ifmt_ctx->streams[video_stream_idx]->time_base;
    enc_ctx->framerate = av_guess_frame_rate(ifmt_ctx, ifmt_ctx->streams[video_stream_idx], NULL);
    enc_ctx->bit_rate = targetBitrate;
    enc_ctx->gop_size = 30;
    enc_ctx->max_b_frames = 2;
    
    if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER) {
        enc_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    }
    
    // Set H.264 preset
    AVDictionary *opts = NULL;
    av_dict_set(&opts, "preset", "fast", 0);
    av_dict_set(&opts, "tune", "fastdecode", 0);
    
    if (avcodec_open2(enc_ctx, encoder, &opts) < 0) {
        LOGE("Could not open encoder");
        av_dict_free(&opts);
        goto cleanup;
    }
    av_dict_free(&opts);
    
    avcodec_parameters_from_context(out_stream->codecpar, enc_ctx);
    out_stream->time_base = enc_ctx->time_base;
    
    // Copy audio stream if present
    if (audio_stream_idx >= 0) {
        AVStream *audio_out = avformat_new_stream(ofmt_ctx, NULL);
        if (audio_out) {
            out_audio_idx = audio_out->index;
            avcodec_parameters_copy(audio_out->codecpar, ifmt_ctx->streams[audio_stream_idx]->codecpar);
            audio_out->time_base = ifmt_ctx->streams[audio_stream_idx]->time_base;
        }
    }
    
    // Setup scaler
    sws_ctx = sws_getContext(
        dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
        out_width, out_height, AV_PIX_FMT_YUV420P,
        SWS_BILINEAR, NULL, NULL, NULL);
    
    if (!sws_ctx) {
        LOGE("Could not create scaler");
        goto cleanup;
    }
    
    // Open output file
    if (!(ofmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        if (avio_open(&ofmt_ctx->pb, output, AVIO_FLAG_WRITE) < 0) {
            LOGE("Could not open output file");
            goto cleanup;
        }
    }
    
    if (avformat_write_header(ofmt_ctx, NULL) < 0) {
        LOGE("Could not write header");
        goto cleanup;
    }
    
    frame = av_frame_alloc();
    scaled_frame = av_frame_alloc();
    pkt = av_packet_alloc();
    
    scaled_frame->format = AV_PIX_FMT_YUV420P;
    scaled_frame->width = out_width;
    scaled_frame->height = out_height;
    av_frame_get_buffer(scaled_frame, 0);
    
    int64_t frame_count = 0;
    AVPacket *in_pkt = av_packet_alloc();
    
    // Read, decode, encode, write loop
    while (av_read_frame(ifmt_ctx, in_pkt) >= 0) {
        if (in_pkt->stream_index == video_stream_idx) {
            // Decode video
            if (avcodec_send_packet(dec_ctx, in_pkt) == 0) {
                while (avcodec_receive_frame(dec_ctx, frame) == 0) {
                    // Scale frame
                    sws_scale(sws_ctx, (const uint8_t * const *)frame->data, frame->linesize,
                              0, dec_ctx->height, scaled_frame->data, scaled_frame->linesize);
                    
                    scaled_frame->pts = frame->pts;
                    
                    // Encode
                    if (avcodec_send_frame(enc_ctx, scaled_frame) == 0) {
                        AVPacket *enc_pkt = av_packet_alloc();
                        while (avcodec_receive_packet(enc_ctx, enc_pkt) == 0) {
                            enc_pkt->stream_index = out_video_idx;
                            av_packet_rescale_ts(enc_pkt, enc_ctx->time_base, ofmt_ctx->streams[out_video_idx]->time_base);
                            av_interleaved_write_frame(ofmt_ctx, enc_pkt);
                        }
                        av_packet_free(&enc_pkt);
                    }
                    frame_count++;
                }
            }
        } else if (in_pkt->stream_index == audio_stream_idx && out_audio_idx >= 0) {
            // Copy audio packets
            in_pkt->stream_index = out_audio_idx;
            av_packet_rescale_ts(in_pkt, 
                ifmt_ctx->streams[audio_stream_idx]->time_base,
                ofmt_ctx->streams[out_audio_idx]->time_base);
            av_interleaved_write_frame(ofmt_ctx, in_pkt);
        }
        av_packet_unref(in_pkt);
    }
    av_packet_free(&in_pkt);
    
    // Flush encoder
    avcodec_send_frame(enc_ctx, NULL);
    AVPacket *flush_pkt = av_packet_alloc();
    while (avcodec_receive_packet(enc_ctx, flush_pkt) == 0) {
        flush_pkt->stream_index = out_video_idx;
        av_packet_rescale_ts(flush_pkt, enc_ctx->time_base, ofmt_ctx->streams[out_video_idx]->time_base);
        av_interleaved_write_frame(ofmt_ctx, flush_pkt);
    }
    av_packet_free(&flush_pkt);
    
    av_write_trailer(ofmt_ctx);
    
    LOGI("Compression complete: %lld frames processed", (long long)frame_count);
    result = RESULT_SUCCESS;
    
cleanup:
    if (sws_ctx) sws_freeContext(sws_ctx);
    if (pkt) av_packet_free(&pkt);
    if (frame) av_frame_free(&frame);
    if (scaled_frame) av_frame_free(&scaled_frame);
    if (enc_ctx) avcodec_free_context(&enc_ctx);
    if (dec_ctx) avcodec_free_context(&dec_ctx);
    if (ofmt_ctx) {
        if (!(ofmt_ctx->oformat->flags & AVFMT_NOFILE))
            avio_closep(&ofmt_ctx->pb);
        avformat_free_context(ofmt_ctx);
    }
    if (ifmt_ctx) avformat_close_input(&ifmt_ctx);
    
    (*env)->ReleaseStringUTFChars(env, inputPath, input);
    (*env)->ReleaseStringUTFChars(env, outputPath, output);
    
    return result;
}

// ============================================================================
// HLS SEGMENTATION
// ============================================================================

JNIEXPORT jint JNICALL
Java_id_homebase_homebasekmppoc_media_FFmpegNative_segmentToHLS(
    JNIEnv *env, jclass clazz,
    jstring inputPath, jstring playlistPath, jint segmentDuration) {
    
    const char *input = (*env)->GetStringUTFChars(env, inputPath, NULL);
    const char *playlist = (*env)->GetStringUTFChars(env, playlistPath, NULL);
    
    if (!input || !playlist) {
        if (input) (*env)->ReleaseStringUTFChars(env, inputPath, input);
        if (playlist) (*env)->ReleaseStringUTFChars(env, playlistPath, playlist);
        return RESULT_ERROR;
    }
    
    LOGI("Creating HLS: %s -> %s (segment=%ds)", input, playlist, segmentDuration);
    
    int result = RESULT_ERROR;
    AVFormatContext *ifmt_ctx = NULL;
    AVFormatContext *ofmt_ctx = NULL;
    AVPacket *pkt = NULL;
    
    // Open input
    if (avformat_open_input(&ifmt_ctx, input, NULL, NULL) < 0) {
        LOGE("Could not open input");
        goto cleanup;
    }
    
    if (avformat_find_stream_info(ifmt_ctx, NULL) < 0) {
        LOGE("Could not find stream info");
        goto cleanup;
    }
    
    // Create HLS output
    if (avformat_alloc_output_context2(&ofmt_ctx, NULL, "hls", playlist) < 0) {
        LOGE("Could not create HLS output context");
        goto cleanup;
    }
    
    // Set HLS options
    AVDictionary *opts = NULL;
    char seg_time[16];
    snprintf(seg_time, sizeof(seg_time), "%d", segmentDuration);
    av_dict_set(&opts, "hls_time", seg_time, 0);
    av_dict_set(&opts, "hls_list_size", "0", 0);
    av_dict_set(&opts, "hls_flags", "single_file", 0);
    
    // Copy streams
    int *stream_mapping = calloc(ifmt_ctx->nb_streams, sizeof(int));
    int stream_idx = 0;
    
    for (unsigned int i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *in_stream = ifmt_ctx->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;
        
        if (in_codecpar->codec_type != AVMEDIA_TYPE_VIDEO &&
            in_codecpar->codec_type != AVMEDIA_TYPE_AUDIO) {
            stream_mapping[i] = -1;
            continue;
        }
        
        stream_mapping[i] = stream_idx++;
        
        AVStream *out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (!out_stream) {
            LOGE("Could not create output stream");
            free(stream_mapping);
            av_dict_free(&opts);
            goto cleanup;
        }
        
        if (avcodec_parameters_copy(out_stream->codecpar, in_codecpar) < 0) {
            LOGE("Could not copy codec params");
            free(stream_mapping);
            av_dict_free(&opts);
            goto cleanup;
        }
        out_stream->codecpar->codec_tag = 0;
        out_stream->time_base = in_stream->time_base;
    }
    
    // Open output
    if (!(ofmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        if (avio_open(&ofmt_ctx->pb, playlist, AVIO_FLAG_WRITE) < 0) {
            LOGE("Could not open output");
            free(stream_mapping);
            av_dict_free(&opts);
            goto cleanup;
        }
    }
    
    if (avformat_write_header(ofmt_ctx, &opts) < 0) {
        LOGE("Could not write header");
        free(stream_mapping);
        av_dict_free(&opts);
        goto cleanup;
    }
    av_dict_free(&opts);
    
    // Copy packets
    pkt = av_packet_alloc();
    while (av_read_frame(ifmt_ctx, pkt) >= 0) {
        if (pkt->stream_index >= (int)ifmt_ctx->nb_streams || stream_mapping[pkt->stream_index] < 0) {
            av_packet_unref(pkt);
            continue;
        }
        
        int out_idx = stream_mapping[pkt->stream_index];
        pkt->stream_index = out_idx;
        
        AVStream *in_stream = ifmt_ctx->streams[pkt->stream_index];
        AVStream *out_stream = ofmt_ctx->streams[out_idx];
        
        av_packet_rescale_ts(pkt, in_stream->time_base, out_stream->time_base);
        pkt->pos = -1;
        
        if (av_interleaved_write_frame(ofmt_ctx, pkt) < 0) {
            LOGE("Error writing packet");
            break;
        }
        av_packet_unref(pkt);
    }
    
    av_write_trailer(ofmt_ctx);
    free(stream_mapping);
    
    LOGI("HLS segmentation complete");
    result = RESULT_SUCCESS;
    
cleanup:
    if (pkt) av_packet_free(&pkt);
    if (ofmt_ctx) {
        if (!(ofmt_ctx->oformat->flags & AVFMT_NOFILE))
            avio_closep(&ofmt_ctx->pb);
        avformat_free_context(ofmt_ctx);
    }
    if (ifmt_ctx) avformat_close_input(&ifmt_ctx);
    
    (*env)->ReleaseStringUTFChars(env, inputPath, input);
    (*env)->ReleaseStringUTFChars(env, playlistPath, playlist);
    
    return result;
}
