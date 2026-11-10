package com.h2gas.model.entity;

import com.h2gas.enums.AppSettingKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// HU26: configuração do sistema mudada pelo admin
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
public class AppSetting {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_key", length = 60)
    private AppSettingKey key;

    @Column(nullable = false)
    private boolean enabled;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public AppSetting(AppSettingKey key, boolean enabled) {
        this.key = key;
        this.enabled = enabled;
    }
}
