package com.virtualbank.services.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.virtualbank.domain.AccountSavedResponse;
import com.virtualbank.domain.TransactionHistoryResponse;
import com.virtualbank.dto.InfoTranferDto;
import com.virtualbank.dto.TransactionInvoicesDto;
import com.virtualbank.dto.TransactionTypeDto;
import com.virtualbank.entity.Account;
import com.virtualbank.entity.Invoices;
import com.virtualbank.entity.TransactionHistory;
import com.virtualbank.entity.TransactionType;
import com.virtualbank.entity.User;
import com.virtualbank.enums.ErrorsEnum;
import com.virtualbank.enums.InvoicesTypesEnum;
import com.virtualbank.enums.Providers;
import com.virtualbank.enums.TransTypesEnum;
import com.virtualbank.exceptions.AccountException;
import com.virtualbank.exceptions.ResourceNotFoundException;
import com.virtualbank.exceptions.TransactionException;
import com.virtualbank.mapper.TransactionTypeMapper;
import com.virtualbank.repositories.AccountRepository;
import com.virtualbank.repositories.TransactionHistoryRepository;
import com.virtualbank.repositories.TransactionTypeRepository;
import com.virtualbank.services.InvoicesService;
import com.virtualbank.services.TransactionService;
import com.virtualbank.services.UserService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

  public static final String CONTENT_BILL = "The payment for client's bill %s";
  BigDecimal amountWaterReq = null;
  BigDecimal amountElectricReq = null;
  BigDecimal amountInternetReq = null;
  BigDecimal totalAmount;

  @Autowired
  private TransactionTypeRepository transactionTypeRepository;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private TransactionHistoryRepository transactionHistoryRepository;

  @Autowired
  private UserService userService;

  @Autowired
  private InvoicesService invoicesService;

  @Override
  public void saveTransactionType(TransactionTypeDto transType) {
    try {
      transactionTypeRepository.save(TransactionTypeMapper.convertToEntity(transType));
    } catch (Exception e) {
      log.error(e.getMessage());
    }

  }

  @Override
  @Transactional
  public void tranferMoneny(Long userId, InfoTranferDto infoTranfer) throws AccountException {
    log.info("Starting tranfer money for receiver {}", infoTranfer.getUsername());
    User sender = userService.getUserByUserId(userId);
    Optional<Account> receiverAccountOpt =
        accountRepository.findByAccNumber(infoTranfer.getAccounReceiver());
    Optional<Account> senderAccountOpt = accountRepository.findByUser(sender);
    if (receiverAccountOpt.isPresent() && senderAccountOpt.isPresent()) {
      if (infoTranfer.getAmount().compareTo(sender.getAccount().getAccBalance()) == 1) {
        log.error(ErrorsEnum.MONEY_NOT_ENOUGHT.getErrorMessage());
        throw new AccountException(ErrorsEnum.MONEY_NOT_ENOUGHT.getErrorMessage());
      }
      // add money for receiver
      receiverAccountOpt.get()
          .setAccBalance(receiverAccountOpt.get().getAccBalance().add(infoTranfer.getAmount()));
      accountRepository.save(receiverAccountOpt.get());

      // build transaction history - receiver
      Optional<TransactionType> receiverTransTypeOpt =
          transactionTypeRepository.findByTransType(TransTypesEnum.ADD.name());
      TransactionHistory receiverTransHistory = TransactionHistory.builder()
          .account(receiverAccountOpt.get()).user(senderAccountOpt.get().getUser())
          .transType(receiverTransTypeOpt.get()).transNote(infoTranfer.getContent())
          .transAmount(infoTranfer.getAmount()).transDate(new Date()).build();
      transactionHistoryRepository.save(receiverTransHistory);


      // minus for sender;
      senderAccountOpt.get()
          .setAccBalance(senderAccountOpt.get().getAccBalance().subtract(infoTranfer.getAmount()));
      accountRepository.save(senderAccountOpt.get());

      // build transaction history - sender
      Optional<TransactionType> senderTransTypeOpt =
          transactionTypeRepository.findByTransType(TransTypesEnum.MINUS.name());
      TransactionHistory senderTransHistory =
          TransactionHistory.builder().account(senderAccountOpt.get())
              .user(receiverAccountOpt.get().getUser()).transType(senderTransTypeOpt.get())
              .transNote(infoTranfer.getContent()).transAmount(infoTranfer.getAmount())
              .transDate(new Date()).isSave(infoTranfer.getIsSave()).build();
      transactionHistoryRepository.save(senderTransHistory);
      log.info("Tranfer money for receiver  {} success", infoTranfer.getUsername());

    } else {
      log.error(ErrorsEnum.ACCOUNT_NON_EXIST.getErrorMessage());
      throw new AccountException(ErrorsEnum.ACCOUNT_NON_EXIST.getErrorMessage());
    }

  }

  @Override
  public List<TransactionHistoryResponse> getTransHistory(Long userId) throws AccountException {
    List<TransactionHistoryResponse> transactionHistoryResponses = new ArrayList<>();
    User user = userService.getUserByUserId(userId);
    Optional<Account> accountOpt = accountRepository.findByUser(user);
    if (accountOpt.isPresent()) {
      List<TransactionHistory> transHistories =
          transactionHistoryRepository.findByAccount(accountOpt.get());
      transHistories.forEach(history -> {
        TransactionHistoryResponse transHistory = TransactionHistoryResponse.builder()
            .id(history.getId()).accountNumber(history.getUser().getAccount().getAccNumber())
            .fullName(history.getUser().getFullName()).transType(history.getTransType())
            .transNote(history.getTransNote()).transDate(history.getTransDate())
            .transAmount(history.getTransAmount()).build();
        transactionHistoryResponses.add(transHistory);
      });
    } else {
      log.error(ErrorsEnum.ACCOUNT_NON_EXIST.getErrorMessage());
      throw new AccountException(ErrorsEnum.ACCOUNT_NON_EXIST.getErrorMessage());
    }
    return transactionHistoryResponses;
  }

  @Override
  public List<AccountSavedResponse> getAccountsSaved(Long userId) throws AccountException {
    List<AccountSavedResponse> accountsSavedResponses = new ArrayList<>();
    User user = userService.getUserByUserId(userId);
    Optional<Account> accountOpt = accountRepository.findByUser(user);
    if (accountOpt.isPresent()) {
      List<TransactionHistory> transHistories =
          transactionHistoryRepository.findByAccountAndIsSave(accountOpt.get(), true);
      List<User> users = transHistories.stream().map(history -> history.getUser())
          .collect(Collectors.toList()).stream().distinct().collect(Collectors.toList());
      users.stream().forEach(userFiltered -> {
        AccountSavedResponse accountSaved =
            AccountSavedResponse.builder().accountNumber(userFiltered.getAccount().getAccNumber())
                .fullName(userFiltered.getFullName()).username(userFiltered.getUserName()).build();
        accountsSavedResponses.add(accountSaved);
      });
    } else {
      log.error(ErrorsEnum.ACCOUNT_NON_EXIST.getErrorMessage());
      throw new AccountException(ErrorsEnum.ACCOUNT_NON_EXIST.getErrorMessage());
    }
    return accountsSavedResponses;
  }

  @Override
  public void payBills(Long userId, TransactionInvoicesDto transactionInvoicesDto)
      throws AccountException, TransactionException {
    removeInvoices(userId, transactionInvoicesDto);
    for (InfoTranferDto info : transactionInvoicesDto.getInfoTranferDtos()) {
      tranferMoneny(userId, info);
    }
  }

  @Transactional
  private void removeInvoices(Long userId, TransactionInvoicesDto transactionInvoicesDto)
      throws TransactionException, AccountException {
    log.info("Start remove bill was tranfered");
    amountWaterReq = new BigDecimal(0);
    amountElectricReq = new BigDecimal(0);
    amountInternetReq = new BigDecimal(0);
    List<Invoices> invoices =
        invoicesService.getInvoicesEntity(userId, transactionInvoicesDto.getMonth());
    isBalanceEnough(userId, transactionInvoicesDto.getInfoTranferDtos());
    transactionInvoicesDto.getInfoTranferDtos().stream().forEach(info -> {
      if (info.getUsername().equalsIgnoreCase(Providers.NHA_MAY_NUOC_DN.name())) {
        try {
          amountWaterReq = info.getAmount();
          updateAmountInVoices(userId, invoices, amountWaterReq, InvoicesTypesEnum.WATER);
        } catch (TransactionException e) {
          log.error(e.getMessage());
        }
      } else if (info.getUsername().equalsIgnoreCase(Providers.EVN.name())) {
        try {
          amountElectricReq = info.getAmount();
          updateAmountInVoices(userId, invoices, amountElectricReq, InvoicesTypesEnum.ELECTRICITY);
        } catch (TransactionException e) {
          log.error(e.getMessage());
        }
      } else if (info.getUsername().equalsIgnoreCase(Providers.VNPT.name())) {
        try {
          amountInternetReq = info.getAmount();
          updateAmountInVoices(userId, invoices, amountInternetReq, InvoicesTypesEnum.INTERNET);
        } catch (TransactionException e) {
          log.error(e.getMessage());
        }

      }
    });

  }

  private void isBalanceEnough(Long userId, List<InfoTranferDto> tranferRequests)
      throws AccountException {
    totalAmount = new BigDecimal(0);
    User sender = userService.getUserByUserId(userId);
    tranferRequests.stream().forEach(tranfer -> {
      totalAmount = totalAmount.add(tranfer.getAmount());
    });
    if (sender.getAccount().getAccBalance().compareTo(totalAmount) == -1) {
      throw new AccountException(ErrorsEnum.MONEY_NOT_ENOUGHT.getErrorMessage());
    }
  }

  private void updateAmountInVoices(Long userId, List<Invoices> invoices,
      BigDecimal amountMoneyWantPay, InvoicesTypesEnum invoicesTypes) throws TransactionException {
    int counter = 0;
    for (Invoices invoice : invoices) {
      if (invoice.getInvoicesType().getInvoicesType().equalsIgnoreCase(invoicesTypes.name())) {
        if (isValidTranferBill(amountElectricReq, invoice.getBillAmount())) {
          invoicesService.updateAmountInvoices(userId, invoice,
              invoice.getBillAmount().subtract(amountMoneyWantPay));
        }
      }
      else {
        counter++;
      }
    }
    if (counter == invoices.size()) {
      throw new ResourceNotFoundException("Invoices", invoicesTypes.name());
    }
  }

  private boolean isValidTranferBill(BigDecimal amountWantPay, BigDecimal amountInvoices)
      throws TransactionException {
    if (amountWantPay != null && (amountWantPay.compareTo(amountInvoices) == -1
        || amountWantPay.compareTo(amountInvoices) == 0))
      return true;
    else {
      throw new TransactionException(ErrorsEnum.AMOUNT_MONEY_INVALID.getErrorMessage());
    }
  }


}
