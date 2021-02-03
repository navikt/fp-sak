package no.nav.foreldrepenger.økonomistøtte;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class FinnNyesteOppdragForSak {

    private static final Comparator<Oppdragskontroll> SISTE_OPPDRAG_COMPARATOR = Comparator
        .comparing(Oppdragskontroll::getOpprettetTidspunkt, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(Oppdragskontroll::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private ØkonomioppdragRepository økonomioppdragRepository;

    FinnNyesteOppdragForSak() {
        // for CDI proxy
    }

    @Inject
    public FinnNyesteOppdragForSak(ØkonomioppdragRepository økonomioppdragRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
    }

    /**
     * Finn liste med {@link Oppdrag110} med positiv kvittering som tilhører nyeste {@link Oppdragskontroll} som inneholder {@link Oppdrag110}
     * med positiv kvittering.
     *
     * @param saksnummer et {@link Saksnummer}
     * @return liste med {@link Oppdrag110} med positiv kvittering som tilhører nyeste {@link Oppdragskontroll} som inneholder
     *         {@link Oppdrag110} med positiv kvittering, tom liste hvis det ikke finnes noen Oppdrag110 med positiv kvittering.
     */
    public List<Oppdrag110> finnNyesteOppdragForSak(Saksnummer saksnummer) {
        List<Oppdragskontroll> alleOppdragskontroll = økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);
        Optional<Oppdragskontroll> sisteOpprettedeOppdragskontrollMedPositivKvittering = alleOppdragskontroll.stream()
            .filter(oppdragskontroll -> oppdragskontroll.getOppdrag110Liste().stream()
                .anyMatch(OppdragKvitteringTjeneste::harPositivKvittering))
            .max(SISTE_OPPDRAG_COMPARATOR);

        return sisteOpprettedeOppdragskontrollMedPositivKvittering.map(Oppdragskontroll::getOppdrag110Liste)
            .orElse(Collections.emptyList())
            .stream()
            .filter(OppdragKvitteringTjeneste::harPositivKvittering)
            .collect(Collectors.toList());
    }
}
