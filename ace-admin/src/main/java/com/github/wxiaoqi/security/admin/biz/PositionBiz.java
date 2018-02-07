package com.github.wxiaoqi.security.admin.biz;

import com.github.wxiaoqi.security.admin.entity.Group;
import com.github.wxiaoqi.security.admin.entity.Position;
import com.github.wxiaoqi.security.admin.entity.User;
import com.github.wxiaoqi.security.admin.mapper.PositionMapper;
import com.github.wxiaoqi.security.admin.vo.GroupTree;
import com.github.wxiaoqi.security.common.biz.BusinessBiz;
import com.github.wxiaoqi.security.common.util.UUIDUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 *
 * @author Mr.AG
 * @email 463540703@qq.com
 * @version 2018-02-04 19:06:43
 */
@Service
public class PositionBiz extends BusinessBiz<PositionMapper,Position> {
    /**
     * 更改岗位用户
     * @param positionId
     * @param users
     */
    public void modifyPositionUsers(String positionId, String users) {
        if(StringUtils.isNotBlank(users)){
            mapper.deletePositionUsers(positionId);
            for (String uId : users.split(",")) {
                mapper.insertPositionUser(UUIDUtils.generateUuid(),positionId,uId);
            }
        }
    }

    /**
     * 获取岗位用户
     * @param positionId
     * @return
     */
    public List<User> getPositionUsers(String positionId) {
        return mapper.selectPositionUsers(positionId);
    }

    public void modifyPositionGroups(String positionId, String groups) {
        if(StringUtils.isNotBlank(groups)) {
            mapper.deletePositionGroups(positionId);
            for (String groupId : groups.split(",")) {
                mapper.insertPositionGroup(UUIDUtils.generateUuid(),positionId,groupId);
            }
        }
    }

    public List<GroupTree> getPositionGroups(String positionId) {
        List<Group> groups = mapper.selectPositionGroups(positionId);
        List<GroupTree> trees = new ArrayList<GroupTree>();
        GroupTree node = null;
        for (Group group : groups) {
            node = new GroupTree();
            node.setLabel(group.getName());
            BeanUtils.copyProperties(group, node);
            trees.add(node);
        }
        return trees;
    }
}