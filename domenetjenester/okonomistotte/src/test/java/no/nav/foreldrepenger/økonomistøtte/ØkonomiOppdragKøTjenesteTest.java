package no.nav.foreldrepenger.økonomistøtte;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.queue.producer.ØkonomioppdragJmsProducer;

@ExtendWith(MockitoExtension.class)
public class ØkonomiOppdragKøTjenesteTest {
    private final Long behandlingId = 1L;

    private ØkonomiOppdragKøTjeneste oppdragKøTjeneste;

    @Mock
    private ØkonomioppdragRepository økonomiRepository;
    @Mock
    private ØkonomioppdragJmsProducer økonomiJmsProducer;


    @BeforeEach
    public void setUp() {
        oppdragKøTjeneste = new ØkonomiOppdragKøTjeneste(økonomiRepository, økonomiJmsProducer);
    }

    @Test
    public void skal_sende_økonomi_oppdrag() {
        var oppdrag = OppdragTestDataHelper.buildOppdragskontroll();
        ØkonomiOppdragUtils.setupOppdrag110(oppdrag, true);
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
