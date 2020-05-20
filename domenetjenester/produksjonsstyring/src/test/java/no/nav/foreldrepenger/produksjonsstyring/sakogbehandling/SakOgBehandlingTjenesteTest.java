package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class SakOgBehandlingTjenesteTest {

    private SakOgBehandlingTjeneste tjeneste; // objektet vi tester
    private SakOgBehandlingAdapter mockAdapter;

    @Before
    public void setup() {
        mockAdapter = mock(SakOgBehandlingAdapter.class);
        tjeneste = new SakOgBehandlingTjeneste(mockAdapter, null);
    }

    @Test
    public void test_ctor0() {
        tjeneste = new SakOgBehandlingTjeneste();
    }

    @Test
    public void test_behandlingOpprettet() {

        OpprettetBehandlingStatus status = new OpprettetBehandlingStatus();
        tjeneste.behandlingOpprettet(status);

        verify(mockAdapter).behandlingOpprettet(same(status));
    }

    @Test
    public void test_behandlingAvsluttet() {

        AvsluttetBehandlingStatus status = new AvsluttetBehandlingStatus();
        tjeneste.behandlingAvsluttet(status);

        verify(mockAdapter).behandlingAvsluttet(same(status));
    }
}
