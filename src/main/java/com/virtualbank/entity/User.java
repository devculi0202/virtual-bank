package com.virtualbank.entity;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "full_name", nullable = false)
  private String fullName;
  @Column(name = "user_name", unique = true, nullable = false)
  private String userName;
  @Column(name = "password", nullable = false)
  private String password;
  @Column(name = "date_create", nullable = false)
  private Date dateCreate;
  @OneToOne(mappedBy = "user")
  private Account account;
}
