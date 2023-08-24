package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.queue.producer.ØkonomioppdragJmsProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.lagOppdrag110;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ØkonomiOppdragKøTjenesteTest {
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
    void skal_sende_økonomi_oppdrag() {
        var oppdrag = OppdragTestDataHelper.oppdragskontrollUtenOppdrag();
        lagOppdrag110(oppdrag, 1L, KodeFagområde.FP, true, true, false);
        lagOppdrag110(oppdrag, 1L, KodeFagområde.FP, true, true, false);
        when(økonomiRepository.finnOppdragForBehandling(behandlingId)).thenReturn(Optional.of(oppdrag));

        oppdragKøTjeneste.leggOppdragPåKø(behandlingId);

        verify(økonomiJmsProducer, times(2)).sendØkonomiOppdrag(any());
    }

    @Test
    void skal_ikke_sende_økonomi_oppdrag() {
        when(økonomiRepository.finnOppdragForBehandling(behandlingId)).thenReturn(Optional.empty());

        oppdragKøTjeneste.leggOppdragPåKø(behandlingId);

        verify(økonomiJmsProducer, never()).sendØkonomiOppdrag(any());
    }
}
