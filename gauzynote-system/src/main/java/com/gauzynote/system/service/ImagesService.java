package com.gauzynote.system.service;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.Images;
import com.gauzynote.system.mapper.ImagesMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.List;

import static com.gauzynote.common.utils.file.FileUtils.deleteFile;

/**
 * 图片表(Images)表服务实现类
 *
 */
@Service("imagesService")
public class ImagesService {
    @Resource
    private ImagesMapper imagesDao;

    /**
     * 通过ID查询单条数据
     *
     * @param imageId 主键
     * @return 实例对象
     */
    public Images selectById(Long imageId) {
        Long userId = SecurityUtils.getUserId();
        return this.imagesDao.selectById(imageId, userId);
    }

    /**
     * 新增数据
     *
     * @param images 实例对象
     * @return 实例对象
     */
    public int insert(Images images) {
        images.setUserId(SecurityUtils.getUserId());
        int insert = this.imagesDao.insert(images);
        if (insert <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("image.creation.failed"));
        }
        return insert;
    }

    public Images insertByFile(MultipartFile file, Path filePath, String fileName) {
        Images images = new Images();
        images.setImageName(fileName);
        images.setImagePath(filePath.toString());
        images.setImageType(file.getContentType());
        images.setImageSize(file.getSize());
        insert(images);
        return images;
    }

    /**
     * 修改数据
     *
     * @param images 实例对象
     * @return 实例对象
     */
    public int update(Images images) {
        images.setUserId(SecurityUtils.getUserId());
        int update = this.imagesDao.update(images);
        if (update <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("image.modification.failed"));
        }
        return update;
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateImageName(Long imageId, String imageName) {
        Long userId = SecurityUtils.getUserId();
        Images oldImage = this.imagesDao.selectById(imageId, userId);
        if (oldImage ==null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, MessageUtils.message("resource.not.exists"));
        }
//        String newName = imageName;
//        String newName = imageName + oldImage.getImageName().substring(oldImage.getImageName().lastIndexOf("."));

        Images images = new Images();
        images.setImageId(imageId);
        images.setImageName(imageName);

        //原始文件的绝对路径
        // path 不变
//        File oldFile = new File(oldImage.getImagePath());

//        rename(oldImage.getImagePath(), newName);

//        images.setImagePath(oldFile.getParent() + File.separator + newName);
        return this.imagesDao.update(images);
    }

    /**
     * 通过主键删除数据
     *
     * @param imageId 主键
     * @return 是否成功
     */
    public int deleteById(Long imageId) {
        Long userId = SecurityUtils.getUserId();
        Images image = this.imagesDao.selectById(imageId, userId);
        if (image == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, MessageUtils.message("resource.not.exists"));
        }
        boolean b = deleteFile(image.getImagePath());
        if (!b) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("image.deletion.failed"));
        }
        int i = this.imagesDao.deleteById(imageId, userId);
        if (i <= 0){
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("image.deletion.failed"));
        }
        return i;
    }

    /**
     * 通过主键删除数据
     *
     * @param imageIds 主键
     */
    public void deleteByIds(List<Long> imageIds) {
        Long userId = SecurityUtils.getUserId();
        int i = this.imagesDao.deleteByIds(imageIds, userId);
        if (i <= 0){
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("image.deletion.failed"));
        }
    }
}
