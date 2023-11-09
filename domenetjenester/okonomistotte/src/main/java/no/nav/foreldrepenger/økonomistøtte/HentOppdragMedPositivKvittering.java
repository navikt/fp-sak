package no.nav.foreldrepenger.økonomistøtte;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class HentOppdragMedPositivKvittering {
    private ØkonomioppdragRepository økonomioppdragRepository;

    HentOppdragMedPositivKvittering() {
        // for CDI proxy
    }

    @Inject
    public HentOppdragMedPositivKvittering(ØkonomioppdragRepository økonomioppdragRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
    }

    public List<Oppdrag110> hentOppdragMedPositivKvittering(Behandling behandling) {
        var oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandling.getId());
        var oppdrag110List = oppdragskontroll.map(Oppdragskontroll::getOppdrag110Liste)
            .orElse(Collections.emptyList());
        return oppdrag110List.stream()
            .filter(OppdragKvitteringTjeneste::harPositivKvittering)
            .toList();
    }

    public List<Oppdrag110> hentOppdragMedPositivKvitteringFeilHvisVenter(Behandling behandling) {
        var oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandling.getId());
        var oppdrag110List = oppdragskontroll.map(Oppdragskontroll::getOppdrag110Liste)
            .orElse(Collections.emptyList());
        if (oppdrag110List.stream().anyMatch(Oppdrag110::venterKvittering))
            throw new IllegalStateException("Utviklerfeil har ikke ventet på at oppdrag er kvittert");
        return oppdrag110List.stream()
            .filter(OppdragKvitteringTjeneste::harPositivKvittering)
            .toList();
    }

    public List<Oppdrag110> hentOppdragMedPositivKvittering(Saksnummer saksnummer) {
        var oppdragskontrollList = økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);
        return oppdragskontrollList.stream().map(Oppdragskontroll::getOppdrag110Liste)
            .flatMap(List::stream)
            .filter(OppdragKvitteringTjeneste::harPositivKvittering)
            .toList();
    }
}
