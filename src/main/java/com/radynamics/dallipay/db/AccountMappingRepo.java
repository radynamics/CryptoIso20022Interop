package com.radynamics.dallipay.db;

import com.radynamics.dallipay.cryptoledger.LedgerFactory;
import com.radynamics.dallipay.cryptoledger.LedgerId;
import com.radynamics.dallipay.cryptoledger.Wallet;
import com.radynamics.dallipay.iso20022.Account;
import com.radynamics.dallipay.iso20022.AccountFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Optional;

public class AccountMappingRepo implements AutoCloseable {
    private final Connection conn;

    private AccountMappingRepo(Connection conn) {
        if (conn == null) throw new IllegalArgumentException("Parameter 'conn' cannot be null");
        this.conn = conn;
    }

    public AccountMappingRepo() {
        this(Database.connect());
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public AccountMapping[] list(LedgerId ledgerId, Wallet wallet) throws SQLException {
        var ps = conn.prepareStatement("SELECT * FROM accountmapping WHERE ledgerId = ? AND walletPublicKey = ? LIMIT 100");
        ps.setInt(1, ledgerId.numericId());
        ps.setString(2, wallet.getPublicKey());

        return readList(ps);
    }

    public AccountMapping[] list(LedgerId ledgerId, Account account) throws SQLException {
        var ps = conn.prepareStatement("SELECT * FROM accountmapping WHERE ledgerId = ? AND bankAccount = ? LIMIT 100");
        ps.setInt(1, ledgerId.numericId());
        ps.setString(2, account.getUnformatted());

        return readList(ps);
    }

    private AccountMapping[] readList(PreparedStatement ps) throws SQLException {
        var rs = ps.executeQuery();
        var list = new ArrayList<AccountMapping>();
        while (rs.next()) {
            list.add(read(rs));
        }
        return list.toArray(new AccountMapping[0]);
    }

    private AccountMapping read(ResultSet rs) throws SQLException {
        var ledgerId = LedgerId.of(rs.getInt("ledgerId"));
        var o = new AccountMapping(ledgerId);
        o.setId(rs.getLong("id"));
        o.setAccount(AccountFactory.create(rs.getString("bankAccount")));
        var l = LedgerFactory.create(ledgerId);
        o.setWallet(l.createWallet(rs.getString("walletPublicKey"), ""));
        return o;
    }

    public Optional<AccountMapping> single(LedgerId ledgerId, Account account) throws SQLException {
        if (account == null) {
            return Optional.empty();
        }
        var ps = conn.prepareStatement("SELECT * FROM accountmapping WHERE ledgerId = ? AND bankAccount = ? LIMIT 1");
        ps.setInt(1, ledgerId.numericId());
        ps.setString(2, account.getUnformatted());

        var rs = ps.executeQuery();
        return rs.next() ? Optional.of(read(rs)) : Optional.empty();
    }

    public Optional<AccountMapping> single(LedgerId ledgerId, Wallet wallet) throws SQLException {
        if (wallet == null) {
            return Optional.empty();
        }
        var ps = conn.prepareStatement("SELECT * FROM accountmapping WHERE ledgerId = ? AND walletPublicKey = ? LIMIT 1");
        ps.setInt(1, ledgerId.numericId());
        ps.setString(2, wallet.getPublicKey());

        var rs = ps.executeQuery();
        return rs.next() ? Optional.of(read(rs)) : Optional.empty();
    }

    public void saveOrUpdate(AccountMapping value) throws SQLException {
        String sql = "INSERT OR REPLACE INTO accountmapping (id, ledgerId, bankAccount, walletPublicKey) \n"
                + "	    VALUES ((SELECT id FROM accountmapping WHERE id = ?), ?, ?, ?);";
        var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, value.getId());
        ps.setInt(2, value.getLedgerId().numericId());
        ps.setString(3, value.getAccount().getUnformatted());
        ps.setString(4, value.getWallet().getPublicKey());

        Database.executeUpdate(ps, 1);

        try (var generatedKeys = ps.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                value.setId(generatedKeys.getLong(1));
            } else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        }
    }

    private void delete(AccountMapping value) throws Exception {
        var ps = conn.prepareStatement("DELETE FROM accountmapping WHERE id = ?");
        ps.setLong(1, value.getId());

        Database.executeUpdate(ps, 1);
    }


    public void persistOrDelete(AccountMapping mapping) throws Exception {
        if (mapping.allPresent()) {
            // When user clicks into cell and predefined value (ex senderWallet) matches other one (ex senderAccount).
            // When user enters an invalid wallet remove mapping to prevent multiple wallets to same account after entering a valid wallet again.
            if (mapping.bothSame() || !mapping.isWalletPresentAndValid()) {
                if (mapping.isPersisted()) {
                    delete(mapping);
                }
            } else {
                if (mapping.isWalletPresentAndValid()) {
                    saveOrUpdate(mapping);
                }
            }
        } else if (mapping.isPersisted() && mapping.accountOrWalletMissing()) {
            // Interpret "" as removal. During creation values are maybe not yet defined.
            delete(mapping);
        }
    }

    public void commit() throws SQLException {
        conn.commit();
    }
}