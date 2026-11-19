package com.campus.lostfound.service;

import com.campus.lostfound.common.PageQuery;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.dto.PasswordDTO;
import com.campus.lostfound.dto.ProfileDTO;
import com.campus.lostfound.vo.ClaimVO;
import com.campus.lostfound.vo.CommentVO;
import com.campus.lostfound.vo.ItemListVO;
import com.campus.lostfound.vo.ProfileVO;
import com.campus.lostfound.vo.UserVO;

/** 个人中心服务（详细设计 4.1.4 ~ 4.1.6）。 */
public interface UserService {

    /** 个人资料 + 四项统计。 */
    ProfileVO getProfile();

    /** 修改昵称与联系方式，学号/角色/状态不可改。 */
    UserVO updateProfile(ProfileDTO dto);

    /** 修改密码：原密码错误 1005，新旧相同/两次不一致 1001。 */
    void changePassword(PasswordDTO dto);

    /** 我的发布（可按审核状态、物品状态筛选）。 */
    PageResult<ItemListVO> myItems(PageQuery query, Integer auditStatus, Integer itemStatus);

    /** 我的认领（可按申请状态筛选）。 */
    PageResult<ClaimVO> myClaims(PageQuery query, Integer status);

    /** 我的留言。 */
    PageResult<CommentVO> myComments(PageQuery query);
}
