package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.tilganger.AnsattInfoKlient;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOperasjonerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Bygger et sammensatt resultat av FagsakBehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */
@ApplicationScoped
public class FagsakBehandlingOperasjonerDtoTjeneste {

    private TotrinnTjeneste totrinnTjeneste;
    private VergeTjeneste vergeTjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private AnsattInfoKlient ansattInfoKlient;

    @Inject
    public FagsakBehandlingOperasjonerDtoTjeneste(TotrinnTjeneste totrinnTjeneste,
                                                  VergeTjeneste vergeTjeneste,
                                                  FagsakEgenskapRepository fagsakEgenskapRepository,
                                                  AnsattInfoKlient ansattInfoKlient) {
        this.ansattInfoKlient = ansattInfoKlient;
        this.totrinnTjeneste = totrinnTjeneste;
        this.vergeTjeneste = vergeTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    FagsakBehandlingOperasjonerDtoTjeneste() {
        // for CDI proxy
    }

    public BehandlingOperasjonerDto lovligeOperasjoner(Behandling b) {
        if (b.erSaksbehandlingAvsluttet()) {
            return BehandlingOperasjonerDto.avsluttet(b.getUuid()); // Skal ikke foreta menyvalg lenger
        }
        var erSaksbehandler = ansattInfoKlient.kanSaksbehandle();
        var kanMerkesHaster = !fagsakEgenskapRepository.harFagsakMarkering(b.getFagsakId(), FagsakMarkering.HASTER);
        if (!erSaksbehandler) {
            return BehandlingOperasjonerDto.veileder(b.getUuid(), kanMerkesHaster);
        }
        if (BehandlingStatus.FATTER_VEDTAK.equals(b.getStatus())) {
            var tilgokjenning = b.getAnsvarligSaksbehandler() != null && !b.getAnsvarligSaksbehandler().equalsIgnoreCase(
                KontekstHolder.getKontekst().getUid());
            return BehandlingOperasjonerDto.fatteVedtak(b.getUuid(), tilgokjenning, kanMerkesHaster);
        }
        var kanÅpnesForEndring = b.erRevurdering() && !b.isBehandlingPåVent() &&
            SpesialBehandling.erIkkeSpesialBehandling(b) && !b.erKøet() &&
            !FagsakYtelseType.ENGANGSTØNAD.equals(b.getFagsakYtelseType());
        var totrinnRetur = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(b.getId()).stream()
            .anyMatch(tt -> !tt.isGodkjent());
        var behandlingIkkeHosKlageInstans = !behandlingHosKlageInstans(b);
        return new BehandlingOperasjonerDto(b.getUuid(),
            !b.erKøet(), // Bytte enhet
            SpesialBehandling.kanHenlegges(b) && behandlingIkkeHosKlageInstans, // Henlegges
            b.isBehandlingPåVent() && !b.erKøet() && behandlingIkkeHosKlageInstans, // Gjenopptas
            kanÅpnesForEndring, // Åpnes for endring
            kanMerkesHaster, // Merkes med Haster
            !b.isBehandlingPåVent(), // Settes på vent
            true, // Sende melding
            !b.isBehandlingPåVent() && totrinnRetur, // Fra beslutter
            false, // Til godkjenning
            vergeTjeneste.utledBehandlingOperasjon(b));
    }

    private static boolean behandlingHosKlageInstans(Behandling behandling) {
        return Objects.equals(behandling.getBehandlendeOrganisasjonsEnhet(), BehandlendeEnhetTjeneste.getKlageInstans());
    }

}
