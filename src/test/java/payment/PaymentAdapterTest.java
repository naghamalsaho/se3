package test.java.payment;


import org.junit.jupiter.api.Test;
import payment.PayPalAdapter;
import payment.PayPalApi;
import payment.PaymentGateway;
import transactions.Transaction;
import accounts.Account;
import accounts.factory.AccountFactory;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PayPalAdapterTest {
    @Test
    void adapterConvertsAmountToCentsAndCallsApi() {
        PayPalApi api = mock(PayPalApi.class);
        when(api.sendPayment(anyString(), anyString(), anyLong())).thenReturn(true);

        PaymentGateway adapter = new PayPalAdapter(api);

        Account from = AccountFactory.createChecking("s1","from", 1000.0);
        Account to = AccountFactory.createChecking("s2","to", 0.0);
        Transaction tx = new Transaction(Transaction.Type.TRANSFER, from, to, 12.34);

        boolean ok = adapter.process(tx);
        assertTrue(ok);

        // verify sendPayment called with cents 1234
        verify(api, times(1)).sendPayment(eq("s1"), eq("s2"), eq(1234L));
    }
}
