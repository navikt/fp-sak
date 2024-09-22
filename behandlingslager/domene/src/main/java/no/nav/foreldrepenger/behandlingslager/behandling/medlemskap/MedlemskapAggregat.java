package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable aggregat som samler informasjon fra ulike kilder for Medlemskap informasjon fra registere, søknad, og slik
 * det er vurdert av Saksbehandler (evt. automatisk vurdert).
 */
public class MedlemskapAggregat {

    private final VurdertMedlemskap vurdertMedlemskap;
    private final Set<MedlemskapPerioderEntitet> registrertMedlemskapPeridoer;
    private final MedlemskapOppgittTilknytningEntitet oppgittTilknytning;
    private final VurdertMedlemskapPeriodeEntitet vurderingLøpendeMedlemskap;

    public MedlemskapAggregat(VurdertMedlemskap medlemskap, Set<MedlemskapPerioderEntitet> medlemskapPerioder,
                              MedlemskapOppgittTilknytningEntitet oppgittTilknytning, VurdertMedlemskapPeriodeEntitet vurderingLøpendeMedlemskap) {
        this.vurdertMedlemskap = medlemskap;
        this.registrertMedlemskapPeridoer = medlemskapPerioder;
        this.oppgittTilknytning = oppgittTilknytning;
        this.vurderingLøpendeMedlemskap = vurderingLøpendeMedlemskap;
    }

    /** Hent Medlemskap slik det er vurdert (hvis eksisterer). */
    public Optional<VurdertMedlemskap> getVurdertMedlemskap() {
        return Optional.ofNullable(vurdertMedlemskap);
    }

    /** Hent Registrert medlemskapinformasjon (MEDL) slik det er innhentet. */
    public Set<MedlemskapPerioderEntitet> getRegistrertMedlemskapPerioder() {
        return registrertMedlemskapPeridoer;
    }

    public List<MedlemskapPerioderEntitet> getRegistrertMedlemskapPerioderList() {
        return registrertMedlemskapPeridoer.stream().sorted(MedlemskapPerioderEntitet.COMP_MEDLEMSKAP_PERIODER).toList();
    }

    /** Hent Oppgitt tilknytning slik det er oppgitt av Søker. */
    public Optional<MedlemskapOppgittTilknytningEntitet> getOppgittTilknytning() {
        return Optional.ofNullable(oppgittTilknytning);
    }

    /** Hent Løpende medlemskap (hvis eksisterer)*/
    public Optional<VurdertMedlemskapPeriodeEntitet> getVurderingLøpendeMedlemskap() {
        return Optional.ofNullable(vurderingLøpendeMedlemskap);
    }
}
