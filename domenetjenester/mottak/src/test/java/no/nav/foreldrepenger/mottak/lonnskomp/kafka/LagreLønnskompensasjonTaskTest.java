package no.nav.foreldrepenger.mottak.lonnskomp.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.ytelse.LønnskompensasjonVedtak;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.lonnskomp.domene.LønnskompensasjonRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
public class LagreLønnskompensasjonTaskTest extends EntityManagerAwareTest {

    private static final String SAK = "3028155d-c556-4a8a-a38d-a526b1129bf2";
    private static final PersonIdent FNR = new PersonIdent("19575903667");
    private static final AktørId AKTØR_ID = new AktørId("1957590366736");

    private LønnskompensasjonRepository repository;

    @Mock
    private AktørTjeneste aktørTjeneste;

    @BeforeEach
    public void setup() {
        repository= new LønnskompensasjonRepository(getEntityManager());
    }

    @Test
    public void skal_oppdatere_lagret_vedtak() {
        when(aktørTjeneste.hentAktørIdForPersonIdent(eq(FNR))).thenReturn(Optional.of(AKTØR_ID));

        LønnskompensasjonVedtak vedtak = new LønnskompensasjonVedtak();
        vedtak.setFnr(FNR.getIdent());
        vedtak.setSakId(SAK);
        vedtak.setBeløp(new Beløp(new BigDecimal(18000L)));
        vedtak.setOrgNummer(new OrgNummer("999999999"));
        vedtak.setPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020,4,20), LocalDate.of(2020,5,15)));

        repository.lagre(vedtak);

        assertThat(vedtak.getAktørId()).isNull();

        ProsessTaskData data = new ProsessTaskData(LagreLønnskompensasjonTask.TASKTYPE);
        data.setProperty(LagreLønnskompensasjonTask.SAK, SAK);

        LagreLønnskompensasjonTask task = new LagreLønnskompensasjonTask(repository, aktørTjeneste);
        task.doTask(data);

        getEntityManager().clear();

        var oppdatertVedtak = repository.hentSak(SAK, FNR.getIdent());
        assertThat(oppdatertVedtak.get().getAktørId()).isEqualTo(AKTØR_ID);
    }
}
