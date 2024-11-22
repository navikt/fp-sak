package no.nav.foreldrepenger.historikk;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagDto;

/**
 * RequestScoped fordi HistorikkInnslagTekstBuilder inneholder state og denne
 * deles på tvers av AksjonspunktOppdaterere.
 */
@RequestScoped
public class HistorikkTjenesteAdapter {

    private HistorikkRepository historikkRepository;
    private HistorikkInnslagTekstBuilder builder;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public HistorikkTjenesteAdapter(HistorikkRepository historikkRepository,
                                    DokumentArkivTjeneste dokumentArkivTjeneste,
                                    BehandlingRepository behandlingRepository) {
        this.historikkRepository = historikkRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.builder = new HistorikkInnslagTekstBuilder();
    }

    HistorikkTjenesteAdapter() {
        // for CDI proxy
    }

    public List<HistorikkinnslagDto> hentAlleHistorikkInnslagForSak(Saksnummer saksnummer, URI dokumentPath) {
        var historikkinnslagList = Optional.ofNullable(historikkRepository.hentHistorikkForSaksnummer(saksnummer)).orElse(List.of());
        var journalPosterForSak = dokumentArkivTjeneste.hentAlleJournalposterForSakCached(saksnummer).stream()
                .map(ArkivJournalPost::getJournalpostId)
                .toList();
        return historikkinnslagList.stream()
                .map(historikkinnslag -> HistorikkInnslagKonverter.mapFra(historikkinnslag, journalPosterForSak, behandlingRepository, dokumentPath))
                .sorted()
                .toList();
    }

    public HistorikkInnslagTekstBuilder tekstBuilder() {
        return builder;
    }

    public void opprettHistorikkInnslag(Long behandlingId, HistorikkinnslagType hisType) {
        opprettHistorikkInnslag(behandlingId, hisType, HistorikkAktør.SAKSBEHANDLER);
    }

    private void opprettHistorikkInnslag(Long behandlingId, HistorikkinnslagType hisType, HistorikkAktør historikkAktør) {
        if (!builder.getHistorikkinnslagDeler().isEmpty() || builder.antallEndredeFelter() > 0 || builder.getErBegrunnelseEndret()
            || builder.getErGjeldendeFraSatt()) {

            var innslag = new Historikkinnslag();

            builder.medHendelse(hisType);
            innslag.setAktør(historikkAktør);
            innslag.setType(hisType);
            innslag.setBehandlingId(behandlingId);
            builder.build(innslag);

            resetBuilder();

            historikkRepository.lagre(innslag);
        }
    }

    private void resetBuilder() {
        builder = new HistorikkInnslagTekstBuilder();
    }

}
