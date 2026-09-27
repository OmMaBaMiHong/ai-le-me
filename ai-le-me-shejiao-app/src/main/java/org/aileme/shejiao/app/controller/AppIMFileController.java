/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.controller;

import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.AffineTransImage;
import org.aileme.shejiao.common.utils.R;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * IM文件上传下载至本地
 * @author Jl.Yu
 * @date 2022/11/16 13:41
 */
@RestController
@RequestMapping("/app/im")
@Tag(name = "移动端——IM文件")
public class AppIMFileController {

    @Value("${shejiao.linfeng.path.image}")
    private String imagePath;

    @Value("${shejiao.linfeng.path.file}")
    private String filePath;

    @Value("${shejiao.linfeng.path.video}")
    private String videoPath;

    /**
     * 上传图片
     */
    @PostMapping(path = "/image/upload", consumes = "multipart/form-data")
    @Operation(summary = "上传图片")
    public R uploadImage(MultipartFile image) {

        String fileName = image.getOriginalFilename();
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            throw new LinfengException("图片格式错误");
        }
        String suffixName = fileName.substring(index).toLowerCase(Locale.ROOT);
        fileName = UUID.randomUUID() + suffixName;
        File file = new File(imagePath + fileName);

        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new LinfengException("文件夹创建失败");
            }
        }
        try {
            image.transferTo(file);
        } catch (IOException e) {
            e.printStackTrace();
            return R.error("文件上传失败");
        }

        Map<String, String> map = new HashMap<>(2);
        if (image.getSize() > 1024 * 500) {
            AffineTransImage.minImage(imagePath, fileName);
            map.put("compressUrl", "resource/image/min_" + fileName);
        } else {
            map.put("compressUrl", "resource/image/" + fileName);
        }
        map.put("url", "resource/image/" + fileName);
        return R.ok().put("data", map);
    }

    /**
     * 上传文件
     */
    @PostMapping(path = "/file/upload",consumes = "multipart/form-data")
    @Operation(summary = "上传文件")
    public R uploadFile(MultipartFile file){

        String fileName = file.getOriginalFilename();
        String suffixName = fileName.substring(fileName.indexOf(".")).toLowerCase(Locale.ROOT);//后缀名

        ///给文件改名  更换成唯一id方便下载
        String download = UUID.randomUUID() + suffixName;
        File files = new File(filePath + download);

        if(!files.getParentFile().exists()){
            if(!files.getParentFile().mkdirs()){
                throw new LinfengException("文件夹创建失败");
            }
        }
        try {
            file.transferTo(files);
        }catch (IOException e){
            e.printStackTrace();
            return R.error("文件上传失败");
        }
        Map<String, String> map = new HashMap<>(3);
        map.put("name",fileName);
        map.put("download",download);
        map.put("size",String.format("%.4f", file.getSize()/1048576.0)+"MB");
        return R.ok().put("data", map);
    }

    /**
     * 下载文件
     */
    @GetMapping("/file/download")
    @ResponseBody
    @Operation(summary = "下载文件")
    public R download(HttpServletRequest request, HttpServletResponse response) {
        String filePathName = filePath + request.getParameter("url");
        String fileRealName = request.getParameter("name");
        File file = new File(filePathName);
        if (!file.exists()) {
            return R.error("文件已丢失");
        }
        //文件名字乱码问题
        try {
            fileRealName = new String(fileRealName.getBytes(StandardCharsets.UTF_8),"ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.reset();
        response.setContentType("multipart/x-download");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;fileName=" + fileRealName);
        response.addHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));

        try {
            InputStream inStream = new FileInputStream(filePathName);
            OutputStream os = response.getOutputStream();

            byte[] buff = new byte[1024];
            int len ;
            while ((len = inStream.read(buff)) > 0) {
                os.write(buff, 0, len);
            }
            os.flush();
            os.close();

            inStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("文件下载失败");
        }
        return R.ok("文件下载成功");
    }

    /**
     * 上传视频
     */
    @PostMapping(path = "/video/upload",consumes = "multipart/form-data")
    @Operation(summary = "上传视频")
    public R uploadVideo(MultipartFile video){
        String fileName = video.getOriginalFilename();
        int index = fileName.lastIndexOf(".");
        if(index==-1){
            throw new LinfengException("视频格式错误");
        }
        String suffixName = fileName.substring(index).toLowerCase(Locale.ROOT);
        fileName = UUID.randomUUID() + suffixName;
        File file = new File(videoPath + fileName);

        if(!file.getParentFile().exists()){
            if(!file.getParentFile().mkdirs()){
                throw new LinfengException("文件夹创建失败");
            }
        }
        try {
            video.transferTo(file);
        }catch (IOException e){
            e.printStackTrace();
            return R.error("图片上传失败");
        }
        Map<String, String> map = new HashMap<>(1);
        map.put("url","resource/video/"+fileName);
        return R.ok().put("data",map);
    }

}
