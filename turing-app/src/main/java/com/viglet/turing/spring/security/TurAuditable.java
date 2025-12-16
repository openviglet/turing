package com.viglet.turing.spring.security;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class TurAuditable<U> {

    @CreatedBy
    protected U createdBy;

    @CreatedDate
    protected Instant creationDate;

    @LastModifiedBy
    protected U lastModifiedBy;

    @LastModifiedDate
    protected Instant lastModifiedDate;

}