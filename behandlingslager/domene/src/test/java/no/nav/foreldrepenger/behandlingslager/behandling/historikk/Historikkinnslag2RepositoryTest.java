package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.DATE_FORMATTER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

public class Historikkinnslag2RepositoryTest extends EntityManagerAwareTest {
    private BasicBehandlingBuilder basicBehandlingBuilder;
    private Historikkinnslag2Repository repository;
    private Historikkinnslag2 historikkinnslag;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        basicBehandlingBuilder = new BasicBehandlingBuilder(entityManager);
        repository = new Historikkinnslag2Repository(entityManager);
    }

    @Test
    void skal_kunne_lagre_og_hentet_opp_historikkinnslag_basert_på_behandlingid() {
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var behandlingId = behandling.getId();

        historikkinnslag = lagHistorikkinnslag(behandlingId, behandling.getFagsakId());
        repository.lagre(historikkinnslag);

        var hentet = repository.hent(behandlingId).getFirst();
        assertThat(hentet).isNotNull();
        assertThat(hentet.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(hentet.getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(hentet.getTittel()).isEqualTo("Fakta endret");
    }

    @Test
    void skal_hente_historikkinnslag_basert_på_saksnummer() {
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var behandlingId = behandling.getId();

        historikkinnslag = lagHistorikkinnslag(behandlingId, behandling.getFagsakId());
        repository.lagre(historikkinnslag);

        var hentet = repository.hent(behandling.getSaksnummer()).getFirst();
        assertThat(hentet).isNotNull();
        assertThat(hentet.getBehandlingId()).isEqualTo(behandlingId);
    }

    @Test
    void skal_sette_på_punktum_mot_slutten_av_en_tekstlinje() {
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var behandlingId = behandling.getId();
        var arbeidsforholdInfo = "INTERESSANT INTUITIV KATT DIAMETER (315853370)";
        var idag = LocalDate.now();
        var punktum = ".";

        var tekstlinje = HistorikkinnslagLinjeBuilder.fraTilEquals("Startdato for refusjon til " + arbeidsforholdInfo, null, idag);

        var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.FAKTA_OM_FORDELING)
            .addLinje(tekstlinje)
            .build();
        repository.lagre(historikkinnslag);

        var hentet = repository.hent(behandling.getSaksnummer());
        assertThat(hentet).isNotNull();
        assertThat(hentet.getFirst().getLinjer().getFirst().getTekst()).isEqualTo(
            "__Startdato for refusjon til " + arbeidsforholdInfo + "__ er satt til __" + DATE_FORMATTER.format(idag) + "__" + punktum);
    }

    @Test
    void skal_ikke_legge_på_sluttpunktum_hvis_siste_tegn_er_spørsmålstegn() {
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var behandlingId = behandling.getId();
        var punktum = ".";
        var tekstlinje = "Er dette riktig?";

        var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .addLinje(tekstlinje)
            .build();
        repository.lagre(historikkinnslag);

        var hentet = repository.hent(behandling.getSaksnummer());
        assertThat(hentet).isNotNull();
        assertThat(hentet.getFirst().getLinjer().getFirst().getTekst())
            .doesNotContain(punktum)
            .isEqualTo(tekstlinje);
    }

    private Historikkinnslag2 lagHistorikkinnslag(long behandlingId, long fagsakId) {
        return new Historikkinnslag2.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medTittel("Fakta endret")
            .addLinje("begrunnelsestekst")
            .build();
    }
}
