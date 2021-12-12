package com.virtualbank.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
   private Long id;
   private String fullName;
   private String username;
   private AccountResponse accountResponse;
}
