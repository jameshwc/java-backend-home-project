package com.test.bank.service;

import com.test.bank.db.tables.records.UserRecord;
import com.test.bank.initializer.DataSourceInitializer;
import com.test.bank.model.TransferResponse;
import com.test.bank.model.User;

import org.jooq.impl.DefaultConfiguration;
import org.jooq.types.UInteger;
import org.jooq.impl.DSL;
import org.jooq.DSLContext;
// import org.jooq.types.UInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.test.bank.db.Tables.USER;

@Singleton
public class TransactionService {

    DefaultConfiguration jooqConfiguration;

    @Inject
    public TransactionService(DataSourceInitializer dataSourceInitializer) {
        this.jooqConfiguration = dataSourceInitializer.getJooqConfiguration();
    }

    public TransferResponse transfer(int fromUserId, int toUserId, int amount) {
        DSLContext create = DSL.using(jooqConfiguration);
        UserRecord fromUserRecord = create.fetchOne(USER, USER.ID.eq(UInteger.valueOf(fromUserId)));
        UserRecord toUserRecord = create.fetchOne(USER, USER.ID.eq(UInteger.valueOf(toUserId)));
        
        if (fromUserRecord == null || toUserRecord == null) {
            return null;
        }

        if(amount < 0) {
            return null;
        }
        
        int fromDeposit = fromUserRecord.getWallet();
        if(fromDeposit < amount) { // ISSUE: what if the transfer amount is greater than the from-user's deposit?
            return null;
        }
        
        fromUserRecord.setWallet(fromDeposit - amount);
        fromUserRecord.store();
        toUserRecord.setWallet(toUserRecord.getWallet() + amount);
        toUserRecord.store();

        TransferResponse tran = new TransferResponse();
        tran.setFromUser(fromUserRecord.into(User.class));
        tran.setToUser(toUserRecord.into(User.class));
        return tran;
    }

}
