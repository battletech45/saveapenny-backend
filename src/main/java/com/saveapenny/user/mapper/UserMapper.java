package com.saveapenny.user.mapper;

import com.saveapenny.user.dto.UserProfileResponse;
import com.saveapenny.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileResponse toUserProfileResponse(User user);
}
