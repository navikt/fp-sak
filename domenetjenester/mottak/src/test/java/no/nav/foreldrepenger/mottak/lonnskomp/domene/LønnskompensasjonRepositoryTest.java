package no.nav.foreldrepenger.mottak.lonnskomp.domene;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.ytelse.LønnskompensasjonVedtak;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.PersonIdent;


public class LønnskompensasjonRepositoryTest extends EntityManagerAwareTest {

    private static final AktørId AKTØR_ID = new AktørId("1231231231234");
    private static final PersonIdent FNR = new PersonIdent("12312312312");


    private LønnskompensasjonRepository repository;

    @BeforeEach
    public void setup() {
         repository= new LønnskompensasjonRepository(getEntityManager());
    }

    @Test
    public void skal_håndtere_lagring_rett() {
        var vedtak = new LønnskompensasjonVedtak();
        vedtak.setAktørId(AKTØR_ID);
        vedtak.setFnr(FNR.getIdent());
        vedtak.setSakId("1234");
        vedtak.setPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(10), LocalDate.now().minusMonths(9)));
        vedtak.setBeløp(new Beløp(new BigDecimal(10000L)));
        vedtak.setOrgNummer(new OrgNummer("999999999"));

        repository.lagre(vedtak);

        final var vedtakFraRepo = repository.hentLønnskompensasjonForIPeriode(AKTØR_ID, LocalDate.now().minusMonths(17), LocalDate.now());
        assertThat(vedtakFraRepo).hasSize(1);

        final var vedtakForSakId = repository.hentSak("1234", AKTØR_ID);
        assertThat(vedtakForSakId).isPresent();
        assertThat(vedtakForSakId.get().getOrgNummer()).isEqualTo(new OrgNummer("999999999"));

        var nyVedtak = new LønnskompensasjonVedtak(vedtak);
        nyVedtak.setBeløp(new Beløp(new BigDecimal(10001L)));

        if (repository.skalLagreVedtak(vedtak, nyVedtak)) {
            repository.lagre(nyVedtak);
        }

        final var oppdatertVedtattVedtak = repository.hentLønnskompensasjonForIPeriode(AKTØR_ID, LocalDate.now().minusMonths(17), LocalDate.now())
            .stream().findFirst().orElse(null);

        assertThat(oppdatertVedtattVedtak).isNotNull();
        assertThat(oppdatertVedtattVedtak.getId()).isNotEqualTo(vedtak.getId());
        assertThat(oppdatertVedtattVedtak.getBeløp().getVerdi().longValue()).isEqualTo(10001L);
    }

    @Test
    public void skal_forkaste_vedtak_som_likt() {
        var vedtak = new LønnskompensasjonVedtak();
        vedtak.setAktørId(AKTØR_ID);
        vedtak.setFnr(FNR.getIdent());
        vedtak.setSakId("1234");
        vedtak.setPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(10), LocalDate.now().minusMonths(9)));
        vedtak.setBeløp(new Beløp(new BigDecimal(10000L)));
        vedtak.setOrgNummer(new OrgNummer("999999999"));

        repository.lagre(vedtak);

        final var vedtakFraRepo = repository.hentLønnskompensasjonForIPeriode(AKTØR_ID, LocalDate.now().minusYears(1), LocalDate.now());
        assertThat(vedtakFraRepo).hasSize(1);

        var nyVedtak = new LønnskompensasjonVedtak(vedtak);

        if (repository.skalLagreVedtak(vedtak, nyVedtak)) {
            repository.lagre(nyVedtak);
        }

        final var oppdatertVedtattVedtak = repository.hentLønnskompensasjonForIPeriode(AKTØR_ID, LocalDate.now().minusYears(1), LocalDate.now())
            .stream().findFirst().orElse(null);

        assertThat(oppdatertVedtattVedtak).isNotNull();
        assertThat(oppdatertVedtattVedtak.getId()).isEqualTo(vedtak.getId());
    }
}
