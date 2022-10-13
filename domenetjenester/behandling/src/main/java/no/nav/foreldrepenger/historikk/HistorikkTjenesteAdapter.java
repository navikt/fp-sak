package no.nav.foreldrepenger.historikk;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

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

    private static final String HENT_DOK_PATH = "/dokument/hent-dokument";

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
        var journalPosterForSak = dokumentArkivTjeneste.hentAlleJournalposterForSak(saksnummer).stream()
                .map(ArkivJournalPost::getJournalpostId)
                .collect(Collectors.toList());
        return historikkinnslagList.stream()
                .map(historikkinnslag -> HistorikkInnslagKonverter.mapFra(historikkinnslag, journalPosterForSak, behandlingRepository, dokumentPath))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * IKKE BRUK DENNE. Kall på tekstBuilder() for å få
     * HistorikkInnslagTekstBuilder. Deretter opprettHistorikkInslag når ferdig
     *
     * @param historikkinnslag
     */
    @Deprecated
    public void lagInnslag(Historikkinnslag historikkinnslag) {
        historikkRepository.lagre(historikkinnslag);
    }

    public HistorikkInnslagTekstBuilder tekstBuilder() {
        return builder;
    }

    public void opprettHistorikkInnslag(Long behandlingId, HistorikkinnslagType hisType) {
        opprettHistorikkInnslag(behandlingId, hisType, HistorikkAktør.SAKSBEHANDLER);
    }

    public void opprettHistorikkInnslag(Long behandlingId, HistorikkinnslagType hisType, HistorikkAktør historikkAktør) {
        if (!builder.getHistorikkinnslagDeler().isEmpty() || (builder.antallEndredeFelter() > 0) ||
                builder.getErBegrunnelseEndret() || builder.getErGjeldendeFraSatt()) {

            var innslag = new Historikkinnslag();

            builder.medHendelse(hisType);
            innslag.setAktør(historikkAktør);
            innslag.setType(hisType);
            innslag.setBehandlingId(behandlingId);
            builder.build(innslag);

            resetBuilder();

            lagInnslag(innslag);
        }
    }

    private void resetBuilder() {
        builder = new HistorikkInnslagTekstBuilder();
    }

    public URI getRequestPath(HttpServletRequest request) {
        // FIXME XSS valider requestURL eller bruk relativ URL
        if (request == null) {
            return null;
        }
        var stringBuilder = new StringBuilder();

        stringBuilder.append(request.getScheme())
            .append("://")
            .append(request.getLocalName())
            .append(":") // NOSONAR
            .append(request.getLocalPort());

        stringBuilder.append(request.getContextPath())
            .append(request.getServletPath());
        return UriBuilder.fromUri(stringBuilder.toString()).path(HENT_DOK_PATH).build();
    }
}
