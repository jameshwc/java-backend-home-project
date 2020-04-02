package com.test.bank.service;

import com.test.bank.db.tables.records.AdminRecord;
import com.test.bank.initializer.DataSourceInitializer;
import com.test.bank.model.AdminUser;
import com.test.bank.tool.PasswordUtils;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.types.UInteger;
import org.jooq.Record1;
import org.jooq.Result;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.SecureRandom;
import java.util.Base64;

import static com.test.bank.db.Tables.ADMIN;
import static com.test.bank.db.Tables.TOKEN;

@Singleton
public class AdminService {

    DefaultConfiguration jooqConfiguration;

    @Inject
    public AdminService(DataSourceInitializer dataSourceInitializer) {
        this.jooqConfiguration = dataSourceInitializer.getJooqConfiguration();
    }

    public String login(String account, String password) {
        String token = null;
        AdminUser adminUser = DSL.using(jooqConfiguration).fetchOne(ADMIN, ADMIN.ACCOUNT.eq(account)).into(AdminUser.class);
        if (PasswordUtils.verifyUserPassword(password, adminUser.getPassword(), adminUser.getSalt())) {
            token = generateToken();
            DSL.using(jooqConfiguration).insertInto(TOKEN, TOKEN.ADMINID, TOKEN.TOKEN_)
                    .values(UInteger.valueOf(adminUser.getId()), token)
                    .onDuplicateKeyUpdate()
                    .set(TOKEN.TOKEN_, token)
                    .execute();
        }
        return token;
    }

    public boolean authenticate(String token) {
        Result<Record1<UInteger>> queryResult = DSL.using(jooqConfiguration).select(TOKEN.ADMINID).from(TOKEN).where(TOKEN.TOKEN_.eq(token)).fetch();
        if(queryResult.isEmpty() || queryResult.size() > 1) { // is there any chance to get multiple result due to some error?
            return false; 
        }
        UInteger queryAdminId = queryResult.get(0).value1();
        AdminRecord adminUser = DSL.using(jooqConfiguration).fetchOne(ADMIN, ADMIN.ID.eq(queryAdminId));
        if (adminUser != null) {
            return true;
        }
        return false;
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

}
