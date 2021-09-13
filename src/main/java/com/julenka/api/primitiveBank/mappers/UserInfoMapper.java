package com.julenka.api.primitiveBank.mappers;

import com.julenka.api.primitiveBank.domain.UserInfo;
import com.julenka.api.primitiveBank.dto.UserInfoDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserInfoMapper {

    UserInfo toUserInfo(UserInfoDTO dto);

    UserInfoDTO fromUserInfo(UserInfo userInfo);
}
