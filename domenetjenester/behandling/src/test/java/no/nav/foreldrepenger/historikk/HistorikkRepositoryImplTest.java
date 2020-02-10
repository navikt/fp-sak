package no.nav.foreldrepenger.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class HistorikkRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Repository repository = repoRule.getRepository();

    private BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(repoRule.getEntityManager());
    

    private final EntityManager entityManager = repoRule.getEntityManager();
    private final HistorikkRepository historikkRepository = new HistorikkRepository(entityManager);
    private Fagsak fagsak;

    @Before
    public void setup() {
        fagsak = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
    }
    
    @Test
    public void lagrerHistorikkinnslag() {
        repository.lagre(fagsak.getNavBruker());
        repository.lagre(fagsak);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repository.lagre(behandling);
        repository.flush();

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setBehandling(behandling);
        historikkinnslag.setType(HistorikkinnslagType.VEDTAK_FATTET);
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VEDTAK_FATTET)
            .medSkjermlenke(SkjermlenkeType.VEDTAK);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
        List<Historikkinnslag> historikk = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(1);

        Historikkinnslag lagretHistorikk = historikk.get(0);
        assertThat(lagretHistorikk.getAktør().getKode()).isEqualTo(historikkinnslag.getAktør().getKode());
        assertThat(lagretHistorikk.getType().getKode()).isEqualTo(historikkinnslag.getType().getKode());
        assertThat(lagretHistorikk.getHistorikkTid()).isNull();
    }

    @Test
    public void henterAlleHistorikkinnslagForBehandling() {
        repository.lagre(fagsak.getNavBruker());
        repository.lagre(fagsak);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repository.lagre(behandling);
        repository.flush();

        Historikkinnslag vedtakFattet = new Historikkinnslag();
        vedtakFattet.setAktør(HistorikkAktør.SØKER);
        vedtakFattet.setBehandling(behandling);
        vedtakFattet.setType(HistorikkinnslagType.VEDTAK_FATTET);
        HistorikkInnslagTekstBuilder vedtakFattetBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VEDTAK_FATTET)
            .medSkjermlenke(SkjermlenkeType.VEDTAK);
        vedtakFattetBuilder.build(vedtakFattet);
        historikkRepository.lagre(vedtakFattet);

        Historikkinnslag brevSent = new Historikkinnslag();
        brevSent.setBehandling(behandling);
        brevSent.setType(HistorikkinnslagType.BREV_SENT);
        brevSent.setAktør(HistorikkAktør.SØKER);
        HistorikkInnslagTekstBuilder mottattDokBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BREV_SENT);
        mottattDokBuilder.build(brevSent);
        historikkRepository.lagre(brevSent);

        List<Historikkinnslag> historikk = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(2);
        assertThat(historikk.stream().anyMatch(h -> HistorikkinnslagType.VEDTAK_FATTET.equals(h.getType()))).isTrue();
        assertThat(historikk.stream().anyMatch(h -> HistorikkinnslagType.BREV_SENT.equals(h.getType()))).isTrue();
    }
}
