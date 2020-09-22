package no.nav.foreldrepenger.økonomi.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class FastsettOppdragskontrollTest {
    private static final long BEHANDLING_ID = 1;
    private static final long PROSESS_TASK_ID = 100;
    private static final Saksnummer SAKSNUMMER = new Saksnummer("100000");

    /**
     * Første forsøk på å gjøre et førstegangsoppdrag.
     */
    @Test
    public void opprettOppdragskontrollNårIngenFinnesFraFør() {
        // Arrange
        List<Oppdragskontroll> tidligereOppdragListe = Collections.emptyList();

        // Act
        var oppdragskontroll = FastsettOppdragskontroll.finnEllerOpprett(tidligereOppdragListe, BEHANDLING_ID, PROSESS_TASK_ID, SAKSNUMMER);

        // Asssert
        assertThat(oppdragskontroll.getSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(oppdragskontroll.getBehandlingId()).isEqualTo(BEHANDLING_ID);
        assertThat(oppdragskontroll.getVenterKvittering()).isTrue();
        assertThat(oppdragskontroll.getProsessTaskId()).isEqualTo(PROSESS_TASK_ID);
    }

    /**
     * Her finnes oppdragskontroll for samme behandling. Gjelder rekjøring.
     */
    @Test
    public void finnOppdragskontrollNårFinnesForBehandling() {
        // Arrange
        long tidligereOppdragskontrollProsessTaskId = PROSESS_TASK_ID + 5;
        Oppdragskontroll tidligereOppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll(SAKSNUMMER, BEHANDLING_ID, tidligereOppdragskontrollProsessTaskId);
        tidligereOppdragskontroll.setVenterKvittering(Boolean.FALSE);
        List<Oppdragskontroll> tidligereOppdragListe = Collections.singletonList(tidligereOppdragskontroll);

        // Act
        var oppdragskontroll = FastsettOppdragskontroll.finnEllerOpprett(tidligereOppdragListe, BEHANDLING_ID, PROSESS_TASK_ID, SAKSNUMMER);

        // Asssert
        assertThat(oppdragskontroll.getSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(oppdragskontroll.getBehandlingId()).isEqualTo(BEHANDLING_ID);
        assertThat(oppdragskontroll.getVenterKvittering()).isTrue();
        assertThat(oppdragskontroll.getProsessTaskId()).isEqualTo(tidligereOppdragskontrollProsessTaskId);
    }

    /**
     * Her finnes oppdragskontroll for tidligere behandling. Første forsøk på å sende oppdrag for denne behandlingen.
     */
    @Test
    public void opprettOppdragskontrollNårFinnesForTidligereBehandling() {
        // Arrange
        Oppdragskontroll tidligereOppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll(SAKSNUMMER, BEHANDLING_ID-1);
        List<Oppdragskontroll> tidligereOppdragListe = Collections.singletonList(tidligereOppdragskontroll);

        // Act
        var oppdragskontroll = FastsettOppdragskontroll.finnEllerOpprett(tidligereOppdragListe, BEHANDLING_ID, PROSESS_TASK_ID, SAKSNUMMER);


        // Asssert
        assertThat(oppdragskontroll.getSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(oppdragskontroll.getBehandlingId()).isEqualTo(BEHANDLING_ID);
        assertThat(oppdragskontroll.getVenterKvittering()).isTrue();
        assertThat(oppdragskontroll.getProsessTaskId()).isEqualTo(PROSESS_TASK_ID);
    }
}
