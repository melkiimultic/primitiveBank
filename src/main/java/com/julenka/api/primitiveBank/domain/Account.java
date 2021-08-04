package com.julenka.api.primitiveBank.domain;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.bind.DefaultValue;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "accounts")
@RequiredArgsConstructor
public class Account {
    @Id
    @GeneratedValue()
    private Long id;

    @NotNull
    @Column(name = "balance", columnDefinition="Decimal(10,2) default '0.00'" )
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

}
