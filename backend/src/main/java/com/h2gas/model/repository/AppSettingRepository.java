package com.h2gas.model.repository;

import com.h2gas.enums.AppSettingKey;
import com.h2gas.model.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, AppSettingKey> {
}
