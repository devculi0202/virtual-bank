package com.virtualbank.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.virtualbank.domain.AccountResponse;
import com.virtualbank.domain.UserResponse;
import com.virtualbank.dto.AccountDto;
import com.virtualbank.exceptions.AccountException;
import com.virtualbank.services.AccountService;
import com.virtualbank.services.UserService;

@RestController
@RequestMapping("/account")
public class AccountController {

  @Autowired
  private AccountService accountService;

  @Autowired
  private UserService userService;

  @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AccountResponse> createAccount(
      @RequestBody(required = true) AccountDto accountReq) throws AccountException {
    return ResponseEntity
        .ok(accountService.createAccount(userService.getCurrentUserId(), accountReq));
  }
  
  @GetMapping(value = "/{accountnumber}")
  public ResponseEntity<UserResponse> getUserByAccount(
      @PathVariable("accountnumber") String accountNumber) throws AccountException {
    return ResponseEntity.ok(accountService.getUserByAccount(accountNumber));
  }
  
  
  

}
