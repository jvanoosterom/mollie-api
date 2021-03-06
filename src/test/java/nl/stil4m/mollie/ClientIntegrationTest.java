package nl.stil4m.mollie;

import com.google.common.collect.Sets;
import nl.stil4m.mollie.domain.*;
import nl.stil4m.mollie.domain.subpayments.ideal.CreateIdealPayment;
import nl.stil4m.mollie.domain.subpayments.ideal.IdealPaymentOptions;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

public class ClientIntegrationTest {

    private Client client;

    private String VALID_API_KEY = "test_nVK7W2WFmZXUNWcntBtCwvgCAgZ3c5";

    @Before
    public void before() {
        client = new ClientBuilder().withApiKey(VALID_API_KEY).build();
    }

    @Test
    public void testCreatePayment() throws IOException {
        Date beforeTest = new Date();
        ResponseOrError<CreatedPayment> payment = client.payments().create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null));

        DynamicClientIntegrationTest.assertWithin(beforeTest, payment.getData().getCreatedDatetime(), new Date());
    }

    @Test
    public void testCreateIdealPayment() throws IOException, URISyntaxException {
        Date beforeTest = new Date();
        ResponseOrError<Page<Issuer>> all = client.issuers().all(Optional.empty(), Optional.empty());
        assertThat(all.getSuccess(), is(true));
        Issuer issuer = all.getData().getData().get(0);

        ResponseOrError<CreatedPayment> payment = client.payments().create(new CreateIdealPayment(1.00, "Some description", "http://example.com", null, new IdealPaymentOptions(issuer.getId())));

        assertThat(payment.getSuccess(), is(true));
        DynamicClientIntegrationTest.assertWithin(beforeTest, payment.getData().getCreatedDatetime(), new Date());
    }

    @Test
    public void testGetPayment() throws IOException {
        ResponseOrError<CreatedPayment> payment = client.payments().create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null));
        String id = payment.getData().getId();

        ResponseOrError<Payment> paymentStatus = client.payments().get(id);
        assertThat(paymentStatus.getData().getStatus(), is("open"));
    }

    @Test
    public void testGetPaymentWithRefunds() throws IOException {
        ResponseOrError<Payment> getResponse = client.payments().get("tr_3AdTKpQGii");

        getResponse.get(payment -> {
            assertThat(payment.getLinks().getRefunds().isPresent(), is(true));
        }, errorData -> {
            System.out.println();
        });

    }


    @Test
    public void testGetPaymentWithEmptyId() throws IOException {
        try {
            client.payments().get("");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Payment id may not be an empty string"));
        }
    }

    @Test
    public void testGetPaymentWithNullId() throws IOException {
        try {
            client.payments().get(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Payment id may not be null"));
        }
    }

    @Test
    public void testGetMethods() throws IOException, URISyntaxException {
        ResponseOrError<Page<Method>> allResponse = client.methods().all(Optional.empty(), Optional.empty());
        assertThat(allResponse.getSuccess(), is(true));

        Page<Method> all = allResponse.getData();
        assertThat(all.getCount(), is(10));
        assertThat(all.getOffset(), is(0));
        assertThat(all.getLinks(), is(notNullValue()));

        Set<String> identifiers = all.getData().stream().map(Method::getId).collect(Collectors.toSet());
        assertThat(identifiers.containsAll(Sets.newHashSet("ideal", "creditcard", "paypal")), is(true));
    }

    @Test
    public void testGetMethod() throws IOException, URISyntaxException {
        ResponseOrError<Method> methodResponse = client.methods().get("ideal");
        assertThat(methodResponse.getSuccess(), is(true));

        Method method = methodResponse.getData();
        assertThat(method.getId(), is("ideal"));
        assertThat(method.getDescription(), is("iDEAL"));
        assertThat(method.getAmount().getMinimum(), is(0.36));
        assertThat(method.getAmount().getMaximum(), is(50000.0));
        assertThat(method.getImage().getNormal(), is("https://www.mollie.com/images/payscreen/methods/ideal.png"));
        assertThat(method.getImage().getBigger(), is("https://www.mollie.com/images/payscreen/methods/ideal@2x.png"));
    }

    @Test
    public void testGetInvalidMethod() throws IOException {
        ResponseOrError<Method> methodResponse = client.methods().get("no-such-method");
        assertThat(methodResponse.getSuccess(), is(false));
        assertThat(methodResponse.getStatus(), is(404));
        assertThat(methodResponse.getError().keySet(), is(Sets.newHashSet("error")));
    }

    @Test
    public void testGetIssuers() throws IOException, URISyntaxException {
        ResponseOrError<Page<Issuer>> allResponse = client.issuers().all(Optional.empty(), Optional.empty());
        assertThat(allResponse.getSuccess(), is(true));

        Page<Issuer> all = allResponse.getData();
        assertThat(all.getCount(), is(1));
        assertThat(all.getTotalCount(), is(1));
        assertThat(all.getOffset(), is(0));
        assertThat(all.getLinks(), is(notNullValue()));
        assertThat(all.getLinks().getPrevious().isPresent(), is(false));
        assertThat(all.getLinks().getNext().isPresent(), is(false));

        Set<String> identifiers = all.getData().stream().map(Issuer::getId).collect(Collectors.toSet());
        assertThat(identifiers.containsAll(Sets.newHashSet("ideal_TESTNL99")), is(true));
    }

    @Test
    public void testGetIssuer() throws IOException, URISyntaxException {
        ResponseOrError<Issuer> allResponse = client.issuers().get("ideal_TESTNL99");
        assertThat(allResponse.getSuccess(), is(true));

        Issuer issuer = allResponse.getData();
        assertThat(issuer.getResource(), is("issuer"));
        assertThat(issuer.getId(), is("ideal_TESTNL99"));
        assertThat(issuer.getName(), is("TBM Bank"));
        assertThat(issuer.getMethod(), is("ideal"));
    }

    @Test
    public void testGetRefunds() throws IOException, URISyntaxException {
        ResponseOrError<CreatedPayment> payment = client.payments().create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null));
        String id = payment.getData().getId();

        ResponseOrError<Page<Refund>> all = client.refunds().all(id, Optional.empty(), Optional.empty());
        assertThat(all.getSuccess(), is(true));
        Page<Refund> refundPage = all.getData();
        assertThat(refundPage.getCount(), is(0));
        assertThat(refundPage.getData(), is(notNullValue()));
        assertThat(refundPage.getLinks(), is(notNullValue()));
    }

    @Test
    public void testListRefundsForExistingPayment() throws IOException, URISyntaxException {
        CreatedPayment createdPayment = client.payments().create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null)).getData();
        ResponseOrError<Page<Refund>> all = client.refunds().all(createdPayment.getId(), Optional.empty(), Optional.empty());

        assertThat(all.getSuccess(), is(true));

        Page<Refund> data = all.getData();
        assertThat(data.getData().size(), is(0));
    }


    @Test
    public void testCancelNonExistingRefund() throws IOException {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("type", "request");
        errorData.put("message", "The refund id is invalid");
        ResponseOrError<CreatedPayment> payment = client.payments().create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null));
        String id = payment.getData().getId();

        ResponseOrError<Void> cancel = client.refunds().cancel(id, "foo_bar");
        assertThat(cancel.getSuccess(), is(false));
        assertThat(cancel.getError().get("error"), is(errorData));
    }

    @Test
    public void testGetNonExistingRefund() throws IOException {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("type", "request");
        errorData.put("message", "The refund id is invalid");
        ResponseOrError<CreatedPayment> payment = client.payments().create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null));
        String id = payment.getData().getId();

        ResponseOrError<Refund> get = client.refunds().get(id, "foo_bar");
        assertThat(get.getSuccess(), is(false));
        assertThat(get.getError().get("error"), is(errorData));
    }

    @Test
    public void testCreateRefund() throws IOException, URISyntaxException {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("type", "request");
        errorData.put("message", "The payment is already refunded or has not been paid for yet");

        ResponseOrError<CreatedPayment> payment = client.payments().create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null));
        String id = payment.getData().getId();

        ResponseOrError<Refund> create = client.refunds().create(id, Optional.of(1.00));
        assertThat(create.getSuccess(), is(false));
        assertThat(create.getError().get("error"), is(errorData));
    }
}