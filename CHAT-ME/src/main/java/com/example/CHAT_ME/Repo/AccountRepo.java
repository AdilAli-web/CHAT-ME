package com.example.CHAT_ME.Repo;

import com.example.CHAT_ME.Entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepo extends JpaRepository<Account,String> {


    List<Account> findTop20ByUserNameStartingWithAndUserNameNotOrderByUserName(String prefix,String exclude);

//    Optional<Account> findByUserName(String userName);;

    List<Account> findByUserNameIn(List<String> userNames);
}
