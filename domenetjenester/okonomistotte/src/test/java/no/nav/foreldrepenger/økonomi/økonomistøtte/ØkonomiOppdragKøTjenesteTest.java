package no.nav.foreldrepenger.økonomi.økonomistøtte;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomi.økonomistøtte.queue.producer.ØkonomioppdragJmsProducer;

public class ØkonomiOppdragKøTjenesteTest {
    private final Long behandlingId = 1L;

    private ØkonomiOppdragKøTjeneste oppdragKøTjeneste;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ØkonomioppdragRepository økonomiRepository;
    @Mock
    private ØkonomioppdragJmsProducer økonomiJmsProducer;


    @Before
    public void setUp() {
        oppdragKøTjeneste = new ØkonomiOppdragKøTjeneste(økonomiRepository, økonomiJmsProducer);
    }

    @Test
    public void skal_sende_økonomi_oppdrag() {
        Oppdragskontroll oppdrag = OppdragTestDataHelper.buildOppdragskontroll();
        ØkonomiOppdragUtils.setupOppdrag110(oppdrag, OppdragTestDataHelper.buildAvstemming115(), true);
        when(økonomiRepository.finnOppdragForBehandling(behandlingId)).thenReturn(Optional.of(oppdrag));

        oppdragKøTjeneste.leggOppdragPåKø(behandlingId);

        verify(økonomiJmsProducer, times(2)).sendØkonomiOppdrag(any());
    }

    @Test
    public void skal_ikke_sende_økonomi_oppdrag() {
        when(økonomiRepository.finnOppdragForBehandling(behandlingId)).thenReturn(Optional.empty());

        oppdragKøTjeneste.leggOppdragPåKø(behandlingId);

        verify(økonomiJmsProducer, never()).sendØkonomiOppdrag(any());
    }
}
