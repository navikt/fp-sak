package no.nav.foreldrepenger.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class HistorikkRepositoryTest {

    private HistorikkRepository historikkRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingLåsRepository behandlingLåsRepository;
    private BasicBehandlingBuilder basicBehandlingBuilder;

    @BeforeEach
    void setup(EntityManager entityManager) {
        historikkRepository = new HistorikkRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        basicBehandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    private Fagsak opprettFagsak() {
        return basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void lagrerHistorikkinnslag() {
        var behandling = opprettBehandling();

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

    private Behandling opprettBehandling() {
        var fagsak = opprettFagsak();
        return opprettBehandling(fagsak);
    }

    private Behandling opprettBehandling(Fagsak fagsak) {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var behandlingLåsRepository = this.behandlingLåsRepository;
        behandlingRepository.lagre(behandling, behandlingLåsRepository.taLås(behandling.getId()));
        return behandling;
    }

    @Test
    public void henterAlleHistorikkinnslagForBehandling() {
        var behandling = opprettBehandling();

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
